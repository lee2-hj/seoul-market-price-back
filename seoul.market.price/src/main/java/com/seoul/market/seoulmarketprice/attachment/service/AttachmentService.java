package com.seoul.market.seoulmarketprice.attachment.service;

import com.seoul.market.seoulmarketprice.attachment.dto.AttachmentDownloadResponse;
import com.seoul.market.seoulmarketprice.attachment.dto.AttachmentResponse;
import com.seoul.market.seoulmarketprice.attachment.entity.Attachment;
import com.seoul.market.seoulmarketprice.attachment.entity.AttachmentTargetType;
import com.seoul.market.seoulmarketprice.attachment.repository.AttachmentRepository;
import com.seoul.market.seoulmarketprice.config.AttachmentProperties;
import lombok.RequiredArgsConstructor;
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

/** 게시글 첨부파일의 정책 검증, 메타데이터 저장 및 객체 스토리지 연동을 처리한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttachmentService {
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

    /** 파일 정책을 검증한 뒤 MinIO와 DB에 여러 첨부파일을 저장한다. */
    @Transactional
    public List<AttachmentResponse> upload(AttachmentTargetType type, Long targetId,
                                           List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("첨부할 파일을 선택해 주세요.");
        }
        List<Attachment> existing = active(type, targetId);
        if (existing.size() + files.size() > properties.maxFileCount()) {
            throw new IllegalArgumentException("첨부파일은 게시글당 최대 5개까지 등록할 수 있습니다.");
        }

        long existingSize = existing.stream().mapToLong(Attachment::getFileSize).sum();
        long newSize = files.stream().mapToLong(MultipartFile::getSize).sum();
        if (existingSize + newSize > properties.maxTotalSize()) {
            throw new IllegalArgumentException("첨부파일 전체 크기는 30MB를 초과할 수 없습니다.");
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
            // DB 저장을 포함한 후속 단계가 실패하면 이미 업로드한 객체를 보상 삭제한다.
            uploadedKeys.forEach(key -> {
                try { objectStorageService.delete(key); } catch (RuntimeException ignored) { }
            });
            throw exception;
        }
    }

    /** 특정 게시글의 활성 첨부파일 메타데이터 목록을 반환한다. */
    public List<AttachmentResponse> list(AttachmentTargetType type, Long targetId) {
        return active(type, targetId).stream().map(AttachmentResponse::from).toList();
    }

    /** 접근 권한 확인을 마친 첨부파일의 5분짜리 다운로드 URL을 발급한다. */
    public AttachmentDownloadResponse download(AttachmentTargetType type, Long targetId,
                                                 Long attachmentId) {
        Attachment attachment = find(type, targetId, attachmentId);
        return new AttachmentDownloadResponse(
                objectStorageService.createDownloadUrl(attachment.getObjectKey(),
                        attachment.getOriginalName(), DOWNLOAD_EXPIRY_SECONDS),
                DOWNLOAD_EXPIRY_SECONDS
        );
    }

    /** MinIO 객체를 제거한 뒤 DB 메타데이터를 소프트 삭제한다. */
    @Transactional
    public void delete(AttachmentTargetType type, Long targetId, Long attachmentId) {
        Attachment attachment = find(type, targetId, attachmentId);
        objectStorageService.delete(attachment.getObjectKey());
        attachment.softDelete();
    }

    /** 특정 게시글에 연결된 활성 첨부파일 엔티티를 조회한다. */
    private List<Attachment> active(AttachmentTargetType type, Long targetId) {
        return repository.findAllByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByIdAsc(type, targetId);
    }

    /** 대상 정보까지 모두 일치하는 활성 첨부파일을 조회한다. */
    private Attachment find(AttachmentTargetType type, Long targetId, Long attachmentId) {
        return repository.findByIdAndTargetTypeAndTargetIdAndDeletedAtIsNull(
                attachmentId, type, targetId
        ).orElseThrow(() -> new IllegalArgumentException("첨부파일을 찾을 수 없습니다."));
    }

    /** 빈 파일, 용량, 파일명, 확장자 및 MIME 타입 정책을 검증한다. */
    private ValidatedFile validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new IllegalArgumentException("빈 파일은 첨부할 수 없습니다.");
        }
        if (file.getSize() > properties.maxFileSize()) {
            throw new IllegalArgumentException("첨부파일 하나의 크기는 10MB를 초과할 수 없습니다.");
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

    /** 확장자 계열에 맞는 MIME 타입인지 추가로 확인한다. */
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

    /** 게시판 접두사, 연월, UUID 및 검증된 확장자로 충돌 없는 객체 키를 생성한다. */
    private String objectKey(AttachmentTargetType type, String extension) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        return type.objectPrefix() + "/" + date + "/" + UUID.randomUUID() + "." + extension;
    }

    /** 검증과 정규화를 마친 업로드 파일 정보를 내부에서 전달한다. */
    private record ValidatedFile(String originalName, String extension, String contentType) { }
}
