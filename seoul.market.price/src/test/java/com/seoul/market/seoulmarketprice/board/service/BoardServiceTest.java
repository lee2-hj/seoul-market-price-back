package com.seoul.market.seoulmarketprice.board.service;

import com.seoul.market.seoulmarketprice.board.dto.condition.BoardSearchCondition;
import com.seoul.market.seoulmarketprice.board.dto.condition.BoardSearchType;
import com.seoul.market.seoulmarketprice.board.dto.request.AdminBoardUpdateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.BoardCreateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.BoardUpdateRequest;
import com.seoul.market.seoulmarketprice.board.entity.Board;
import com.seoul.market.seoulmarketprice.board.entity.PostType;
import com.seoul.market.seoulmarketprice.board.exception.BoardAccessDeniedException;
import com.seoul.market.seoulmarketprice.board.exception.BoardNotFoundException;
import com.seoul.market.seoulmarketprice.board.repository.BoardRepository;
import com.seoul.market.seoulmarketprice.board.repository.BoardQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 일반 게시판 서비스의 생성, 권한, 삭제 및 조회수 증가 규칙을 검증한다. */
@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository repository;

    @Mock
    private BoardQueryRepository queryRepository;

    private BoardService service;

    @BeforeEach
    void setUp() {
        service = new BoardService(repository, queryRepository);
    }

    /** 검색 타입과 정규화된 검색어를 QueryDSL 저장소에 전달한다. */
    @Test
    void getBoardsPassesModelAttributeSearchCondition() {
        BoardSearchCondition condition = new BoardSearchCondition();
        condition.setSearchType(BoardSearchType.TITLE);
        condition.setKeyword(" 시장 ");

        when(queryRepository.findPublicPage(any(BoardSearchCondition.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getBoards(condition);

        assertThat(condition.getKeyword()).isEqualTo("시장");
        verify(queryRepository).findPublicPage(any(BoardSearchCondition.class), any(Pageable.class));
    }

    /** 일반 게시글 생성 시 사용자 작성자와 기본 공개 상태가 설정된다. */
    @Test
    void createBoardCreatesVisibleGeneralPostOwnedByUser() {
        when(repository.save(any(Board.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createBoard(
                7L,
                "user7",
                new BoardCreateRequest(" 제목 ", " 내용 ")
        );

        assertThat(response.postType()).isEqualTo(PostType.GENERAL);
        assertThat(response.userId()).isEqualTo("user7");
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.visible()).isTrue();
    }

    /** 다른 사용자가 게시글을 수정하려는 요청을 거부한다. */
    @Test
    void updateBoardRejectsDifferentUser() {
        Board board = Board.createGeneral(7L, "제목", "내용");
        when(queryRepository.findActiveById(1L)).thenReturn(Optional.of(board));

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
        when(queryRepository.findActiveById(1L)).thenReturn(Optional.of(board));

        service.deleteBoard(1L, 7L);

        assertThat(board.getDeletedAt()).isNotNull();
    }

    /** 상세 조회 전에 조회수를 원자적으로 증가시킨다. */
    @Test
    void getBoardIncrementsViewCountBeforeReading() {
        Board board = Board.createGeneral(7L, "제목", "내용");
        when(queryRepository.incrementViewCount(1L)).thenReturn(1L);
        when(queryRepository.findPublicById(1L)).thenReturn(Optional.of(board));

        service.getBoard(1L);

        verify(queryRepository).incrementViewCount(1L);
        verify(queryRepository).findPublicById(1L);
    }

    /** 공개 상태가 아닌 게시글 상세 조회는 존재하지 않는 것으로 처리한다. */
    @Test
    void getBoardReturnsNotFoundWhenNotPublic() {
        when(queryRepository.incrementViewCount(1L)).thenReturn(0L);

        assertThatThrownBy(() -> service.getBoard(1L))
                .isInstanceOf(BoardNotFoundException.class);
        verify(queryRepository, never()).findPublicById(1L);
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
