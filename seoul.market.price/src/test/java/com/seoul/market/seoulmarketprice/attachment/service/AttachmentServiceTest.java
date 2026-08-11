package com.seoul.market.seoulmarketprice.attachment.service;

import com.seoul.market.seoulmarketprice.attachment.entity.Attachment;
import com.seoul.market.seoulmarketprice.attachment.entity.AttachmentTargetType;
import com.seoul.market.seoulmarketprice.attachment.repository.AttachmentRepository;
import com.seoul.market.seoulmarketprice.config.AttachmentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 첨부파일 개수·형식 정책과 MinIO 객체 키 생성을 검증한다. */
@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {
    /** 첨부파일 메타데이터 저장소 대역. */
    @Mock AttachmentRepository repository;
    /** 실제 MinIO 호출을 대체하는 객체 스토리지 대역. */
    @Mock ObjectStorageService storage;

    /** 테스트 대상 첨부파일 서비스. */
    private AttachmentService service;

    /** 운영 기본값과 같은 파일 제한으로 서비스를 구성한다. */
    @BeforeEach
    void setUp() {
        service = new AttachmentService(repository, storage,
                new AttachmentProperties(5, 10 * 1024 * 1024, 30 * 1024 * 1024));
        when(repository.findAllByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByIdAsc(
                any(), any())).thenReturn(Collections.emptyList());
    }

    /** 일반 게시판 파일이 board 접두사의 객체 키로 업로드되는지 확인한다. */
    @Test
    void uploadsValidatedFileUsingBoardObjectPrefix() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "notice.pdf", "application/pdf", "pdf-data".getBytes()
        );
        when(repository.save(any(Attachment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.upload(AttachmentTargetType.BOARD, 1L, List.of(file));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().originalName()).isEqualTo("notice.pdf");
        verify(storage).upload(startsWith("board/"), any());
        verify(repository).flush();
    }

    /** 한 번의 요청에서 최대 첨부 개수를 초과하면 저장 전 거절하는지 확인한다. */
    @Test
    void rejectsMoreThanFiveFiles() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "a.pdf", "application/pdf", "x".getBytes()
        );

        assertThatThrownBy(() -> service.upload(
                AttachmentTargetType.QNA_BOARD, 2L,
                List.of(file, file, file, file, file, file)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 5개");

        verify(storage, never()).upload(any(), any());
    }

    /** 실행파일 확장자는 MinIO 업로드 전에 차단하는지 확인한다. */
    @Test
    void rejectsExecutableExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "malware.exe", "application/x-msdownload", "x".getBytes()
        );

        assertThatThrownBy(() -> service.upload(
                AttachmentTargetType.BOARD, 1L, List.of(file)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("허용되지 않는 첨부파일 형식");

        verify(storage, never()).upload(any(), any());
    }
}
