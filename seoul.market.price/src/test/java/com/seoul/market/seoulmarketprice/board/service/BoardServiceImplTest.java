package com.seoul.market.seoulmarketprice.board.service;

import com.seoul.market.seoulmarketprice.board.dto.request.AdminBoardUpdateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.BoardCreateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.BoardUpdateRequest;
import com.seoul.market.seoulmarketprice.board.entity.Board;
import com.seoul.market.seoulmarketprice.board.entity.PostType;
import com.seoul.market.seoulmarketprice.board.exception.BoardAccessDeniedException;
import com.seoul.market.seoulmarketprice.board.exception.BoardNotFoundException;
import com.seoul.market.seoulmarketprice.board.repository.BoardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 일반 게시판 서비스의 생성, 권한, 삭제 및 조회수 증가 규칙을 검증한다. */
@ExtendWith(MockitoExtension.class)
class BoardServiceImplTest {

    @Mock
    private BoardRepository repository;

    private BoardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BoardServiceImpl(repository);
    }

    /** 일반 게시글 생성 시 사용자 작성자와 기본 공개 상태가 설정된다. */
    @Test
    void createBoardCreatesVisibleGeneralPostOwnedByUser() {
        when(repository.save(any(Board.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createBoard(
                7L,
                new BoardCreateRequest(" 제목 ", " 내용 ")
        );

        assertThat(response.postType()).isEqualTo(PostType.GENERAL);
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.visible()).isTrue();
    }

    /** 다른 사용자가 게시글을 수정하려는 요청을 거부한다. */
    @Test
    void updateBoardRejectsDifferentUser() {
        Board board = Board.createGeneral(7L, "제목", "내용");
        when(repository.findActiveById(1L)).thenReturn(Optional.of(board));

        assertThatThrownBy(() -> service.updateBoard(
                1L,
                8L,
                new BoardUpdateRequest("변경", "내용")
        )).isInstanceOf(BoardAccessDeniedException.class);
    }

    /** 작성자 본인의 삭제 요청은 삭제 시각을 기록한다. */
    @Test
    void deleteBoardSoftDeletesOwnedPost() {
        Board board = Board.createGeneral(7L, "제목", "내용");
        when(repository.findActiveById(1L)).thenReturn(Optional.of(board));

        service.deleteBoard(1L, 7L);

        assertThat(board.getDeletedAt()).isNotNull();
    }

    /** 상세 조회 전에 조회수를 원자적으로 증가시킨다. */
    @Test
    void getBoardIncrementsViewCountBeforeReading() {
        Board board = Board.createGeneral(7L, "제목", "내용");
        when(repository.incrementViewCount(1L)).thenReturn(1);
        when(repository.findPublicById(1L)).thenReturn(Optional.of(board));

        service.getBoard(1L);

        verify(repository).incrementViewCount(1L);
        verify(repository).findPublicById(1L);
    }

    /** 공개 상태가 아닌 게시글 상세 조회는 존재하지 않는 것으로 처리한다. */
    @Test
    void getBoardReturnsNotFoundWhenNotPublic() {
        when(repository.incrementViewCount(1L)).thenReturn(0);

        assertThatThrownBy(() -> service.getBoard(1L))
                .isInstanceOf(BoardNotFoundException.class);
        verify(repository, never()).findPublicById(1L);
    }

    /** 변경 항목이 없는 관리자 수정 요청을 거부한다. */
    @Test
    void adminUpdateRejectsEmptyRequest() {
        AdminBoardUpdateRequest request = new AdminBoardUpdateRequest(
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> service.updateByAdmin(1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
