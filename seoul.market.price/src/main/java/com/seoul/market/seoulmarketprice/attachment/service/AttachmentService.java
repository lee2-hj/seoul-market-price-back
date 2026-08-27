package com.seoul.market.seoulmarketprice.attachment.service;

import com.seoul.market.seoulmarketprice.attachment.dto.AttachmentDownloadResponse;
import com.seoul.market.seoulmarketprice.attachment.dto.AttachmentResponse;
import com.seoul.market.seoulmarketprice.attachment.entity.Attachment;
import com.seoul.market.seoulmarketprice.attachment.entity.AttachmentTargetType;
import com.seoul.market.seoulmarketprice.attachment.repository.AttachmentRepository;
import com.seoul.market.seoulmarketprice.config.AttachmentProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 게시글 첨부파일의 정책 검증과 저장 흐름을 총괄한다.
 *
 * <p>실제 파일은 MinIO에 저장하고, 파일명·객체 키·용량 등의 메타데이터만 MySQL에 저장한다.
 * MinIO 업로드 이후 DB 저장이 실패하면 이미 업로드된 객체를 삭제해 두 저장소의 불일치를
 * 최소화한다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttachmentService {
    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);
    /** 발급한 MinIO 다운로드 URL의 유효시간인 5분을 초로 표현한 값. */
    private static final int DOWNLOAD_EXPIRY_SECONDS = 300;
    /** 일반 게시판에서 허용하는 안전한 문서·이미지·압축파일 확장자 목록. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "txt",
            "hwp", "hwpx", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip"
    );
    /** 확장자와 관계없이 업로드를 차단하는 실행 가능 콘텐츠 MIME 목록. */
    private static final Set<String> BLOCKED_CONTENT_TYPES = Set.of(
            "application/x-msdownload", "application/x-sh", "application/x-bat",
            "text/html", "application/javascript", "text/javascript"
    );

    /** 첨부파일 메타데이터 저장소. */
    private final AttachmentRepository repository;
    /** MinIO 저장 연산을 제공하는 객체 스토리지 추상화. */
    private final ObjectStorageService objectStorageService;
    /** 최대 파일 개수와 용량 제한 설정. */
    private final AttachmentProperties properties;

    /**
     * 파일 개수와 전체 용량을 먼저 확인한 뒤 각 파일을 검증하여 MinIO와 DB에 저장한다.
     *
     * <p>MinIO는 DB 트랜잭션에 참여하지 않으므로 처리 도중 예외가 발생하면 이번 요청에서
     * 업로드한 객체 키를 추적해 보상 삭제한다.</p>
     *
     * @param type 첨부 대상 게시판 유형
     * @param targetId 첨부 대상 게시글의 기본 키
     * @param files multipart 요청으로 전달된 파일 목록
     * @return 저장이 완료된 첨부파일의 공개 메타데이터 목록
     * @throws IllegalArgumentException 파일이 없거나 개수·용량·확장자·MIME 정책을 위반한 경우
     * @throws IllegalStateException MinIO 업로드 또는 보상 삭제를 제외한 저장 연동에 실패한 경우
     */
    @Transactional
    public List<AttachmentResponse> upload(AttachmentTargetType type, Long targetId,
                                           List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("첨부할 파일을 선택해 주세요.");
        }
        List<Attachment> existing = active(type, targetId);
        if (existing.size() + files.size() > properties.maxFileCount()) {
            throw new IllegalArgumentException("첨부파일은 게시글당 최대 "
                    + properties.maxFileCount() + "개까지 등록할 수 있습니다.");
        }

        long existingSize = existing.stream().mapToLong(Attachment::getFileSize).sum();
        long newSize = files.stream().mapToLong(MultipartFile::getSize).sum();
        if (existingSize + newSize > properties.maxTotalSize()) {
            throw new IllegalArgumentException("첨부파일 전체 크기는 "
                    + formatSize(properties.maxTotalSize()) + "를 초과할 수 없습니다.");
        }

        List<String> uploadedKeys = new ArrayList<>();
        try {
            List<Attachment> saved = new ArrayList<>();
            for (MultipartFile file : files) {
                ValidatedFile validated = validate(file);
                String objectKey = objectKey(type, validated.extension());
                objectStorageService.upload(objectKey, file);
                uploadedKeys.add(objectKey);
                saved.add(repository.save(Attachment.create(type, targetId, objectKey,
                        validated.originalName(), validated.contentType(), file.getSize())));
            }
            repository.flush();
            return saved.stream().map(AttachmentResponse::from).toList();
        } catch (RuntimeException exception) {
            // MinIO는 DB 롤백 대상이 아니므로 이번 요청에서 생성한 객체를 직접 보상 삭제한다.
            // 보상 삭제 실패는 경고 로그로 남기되 최초 예외를 가리지 않는다.
            uploadedKeys.forEach(key -> {
                try {
                    objectStorageService.delete(key);
                } catch (RuntimeException cleanupException) {
                    log.warn("첨부파일 보상 삭제에 실패했습니다. objectKey={}", key, cleanupException);
                }
            });
            throw exception;
        }
    }

    /**
     * 특정 게시글에 연결된 소프트 삭제되지 않은 첨부파일을 등록 순서대로 조회한다.
     *
     * @param type 첨부 대상 게시판 유형
     * @param targetId 첨부 대상 게시글의 기본 키
     * @return 객체 키를 제외한 첨부파일 공개 메타데이터 목록
     */
    public List<AttachmentResponse> list(AttachmentTargetType type, Long targetId) {
        return active(type, targetId).stream().map(AttachmentResponse::from).toList();
    }

    /**
     * 대상 게시글과 첨부파일의 소속을 검증한 뒤 5분 동안 유효한 다운로드 URL을 발급한다.
     *
     * <p>게시글 공개 여부나 작성자 권한은 호출하는 컨트롤러·게시판 서비스에서 먼저 검증하며,
     * 이 메서드는 첨부파일이 요청한 게시글에 실제로 속하는지를 추가로 확인한다.</p>
     *
     * @param type 첨부 대상 게시판 유형
     * @param targetId 첨부 대상 게시글의 기본 키
     * @param attachmentId 다운로드할 첨부파일의 기본 키
     * @return presigned URL과 초 단위 유효시간
     * @throws IllegalArgumentException 활성 첨부파일을 찾지 못하거나 소속이 일치하지 않는 경우
     */
    public AttachmentDownloadResponse download(AttachmentTargetType type, Long targetId,
                                                 Long attachmentId) {
        Attachment attachment = find(type, targetId, attachmentId);
        return new AttachmentDownloadResponse(
                objectStorageService.createDownloadUrl(attachment.getObjectKey(),
                        attachment.getOriginalName(), DOWNLOAD_EXPIRY_SECONDS),
                DOWNLOAD_EXPIRY_SECONDS
        );
    }

    /**
     * MinIO 객체를 먼저 제거하고 성공한 경우에만 DB 메타데이터를 소프트 삭제한다.
     *
     * <p>객체 삭제가 실패했는데 DB만 삭제 상태가 되는 상황을 방지하기 위해 이 순서를 사용한다.</p>
     *
     * @param type 첨부 대상 게시판 유형
     * @param targetId 첨부 대상 게시글의 기본 키
     * @param attachmentId 삭제할 첨부파일의 기본 키
     * @throws IllegalArgumentException 활성 첨부파일을 찾지 못하거나 소속이 일치하지 않는 경우
     * @throws IllegalStateException MinIO 객체 삭제에 실패한 경우
     */
    @Transactional
    public void delete(AttachmentTargetType type, Long targetId, Long attachmentId) {
        Attachment attachment = find(type, targetId, attachmentId);
        objectStorageService.delete(attachment.getObjectKey());
        attachment.softDelete();
    }

    /** 소프트 삭제된 행을 제외하고 특정 게시글에 연결된 첨부파일 엔티티를 조회한다. */
    private List<Attachment> active(AttachmentTargetType type, Long targetId) {
        return repository.findAllByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByIdAsc(type, targetId);
    }

    /** 게시판 유형·게시글 ID·첨부파일 ID가 모두 일치하는 활성 첨부파일을 조회한다. */
    private Attachment find(AttachmentTargetType type, Long targetId, Long attachmentId) {
        return repository.findByIdAndTargetTypeAndTargetIdAndDeletedAtIsNull(
                attachmentId, type, targetId
        ).orElseThrow(() -> new IllegalArgumentException("첨부파일을 찾을 수 없습니다."));
    }

    /**
     * 빈 파일, 단일 파일 용량, 파일명, 확장자와 MIME 타입을 검증하고 파일명을 정규화한다.
     *
     * <p>클라이언트가 경로를 포함한 파일명을 보내더라도 마지막 이름만 남겨 경로 조작 문자열이
     * 메타데이터에 저장되지 않도록 한다. 확장자 허용 목록과 MIME 계열 검사는 서로 보완하며,
     * 파일 내용 자체를 악성코드 검사하는 기능은 아니다.</p>
     *
     * @param file 검증할 multipart 파일
     * @return 정규화된 파일명, 소문자 확장자와 MIME 타입
     * @throws IllegalArgumentException 첨부파일 정책을 하나라도 위반한 경우
     */
    private ValidatedFile validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new IllegalArgumentException("빈 파일은 첨부할 수 없습니다.");
        }
        if (file.getSize() > properties.maxFileSize()) {
            throw new IllegalArgumentException("첨부파일 하나의 크기는 "
                    + formatSize(properties.maxFileSize()) + "를 초과할 수 없습니다.");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("첨부파일 이름을 확인할 수 없습니다.");
        }
        originalName = originalName.replace('\\', '/');
        originalName = originalName.substring(originalName.lastIndexOf('/') + 1).trim();
        if (originalName.isBlank() || originalName.length() > 255) {
            throw new IllegalArgumentException("첨부파일 이름은 255자 이하여야 합니다.");
        }
        int dot = originalName.lastIndexOf('.');
        String extension = dot < 0 ? "" : originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 첨부파일 형식입니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()
                || BLOCKED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("허용되지 않는 첨부파일 MIME 형식입니다.");
        }
        validateContentType(extension, contentType.toLowerCase(Locale.ROOT));
        return new ValidatedFile(originalName, extension, contentType);
    }

    /**
     * 허용 확장자가 기대하는 MIME 계열과 클라이언트가 전달한 MIME 타입이 일치하는지 확인한다.
     *
     * @param extension 소문자로 정규화된 확장자
     * @param contentType 소문자로 정규화된 MIME 타입
     * @throws IllegalArgumentException 확장자와 MIME 계열이 일치하지 않는 경우
     */
    private void validateContentType(String extension, String contentType) {
        boolean valid = switch (extension) {
            case "jpg", "jpeg", "png", "gif", "webp" -> contentType.startsWith("image/");
            case "pdf" -> contentType.equals("application/pdf");
            case "txt" -> contentType.startsWith("text/plain");
            case "zip" -> contentType.equals("application/zip")
                    || contentType.equals("application/x-zip-compressed")
                    || contentType.equals("application/octet-stream");
            default -> contentType.startsWith("application/");
        };
        if (!valid) {
            throw new IllegalArgumentException("파일 확장자와 MIME 형식이 일치하지 않습니다.");
        }
    }

    /**
     * 게시판 접두사, 업로드 연월, UUID와 검증된 확장자로 외부 입력과 독립적인 객체 키를 만든다.
     *
     * @param type 객체 키 최상위 경로를 결정하는 게시판 유형
     * @param extension 검증을 통과한 소문자 확장자
     * @return {@code board/yyyy/MM/UUID.ext} 또는 {@code qna_board/yyyy/MM/UUID.ext} 형식의 키
     */
    private String objectKey(AttachmentTargetType type, String extension) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        return type.objectPrefix() + "/" + date + "/" + UUID.randomUUID() + "." + extension;
    }

    /** 설정된 바이트 제한을 사용자에게 읽기 쉬운 단위로 표현한다. */
    private static String formatSize(long bytes) {
        long megabyte = 1024L * 1024L;
        long kilobyte = 1024L;
        if (bytes % megabyte == 0) {
            return (bytes / megabyte) + "MB";
        }
        if (bytes % kilobyte == 0) {
            return (bytes / kilobyte) + "KB";
        }
        return bytes + "바이트";
    }

    /** 검증과 정규화를 마친 파일명·확장자·MIME 타입을 업로드 흐름 내부에서 전달한다. */
    private record ValidatedFile(String originalName, String extension, String contentType) { }
}
