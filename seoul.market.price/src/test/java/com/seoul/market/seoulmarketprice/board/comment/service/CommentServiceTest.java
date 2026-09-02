package com.seoul.market.seoulmarketprice.board.comment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.auth.repository.AdminRepository;
import com.seoul.market.seoulmarketprice.board.entity.Board;
import com.seoul.market.seoulmarketprice.board.exception.BoardNotFoundException;
import com.seoul.market.seoulmarketprice.board.repository.BoardQueryRepository;
import com.seoul.market.seoulmarketprice.board.repository.BoardRepository;
import com.seoul.market.seoulmarketprice.board.comment.dto.condition.MyCommentSearchCondition;
import com.seoul.market.seoulmarketprice.board.comment.dto.request.CommentCreateRequest;
import com.seoul.market.seoulmarketprice.board.comment.dto.request.CommentUpdateRequest;
import com.seoul.market.seoulmarketprice.board.comment.entity.BoardComment;
import com.seoul.market.seoulmarketprice.board.comment.entity.BoardType;
import com.seoul.market.seoulmarketprice.board.comment.entity.WriterType;
import com.seoul.market.seoulmarketprice.board.comment.exception.CommentAccessDeniedException;
import com.seoul.market.seoulmarketprice.board.comment.repository.CommentRepository;
import com.seoul.market.seoulmarketprice.member.repository.MemberManagementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 댓글 서비스의 게시글 검증, 계층 제한 및 작성자 권한을 검증한다. */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private BoardQueryRepository boardQueryRepository;
    @Mock
    private BoardRepository boardRepository;
    @Mock
    private MemberManagementRepository memberRepository;
    @Mock
    private AdminRepository adminRepository;

    private CommentService service;

    @BeforeEach
    void setUp() {
        service = new CommentService(
                commentRepository,
                boardQueryRepository,
                boardRepository,
                memberRepository,
                adminRepository
        );
    }

    /** 공개 게시글에는 로그인 사용자의 댓글을 생성할 수 있다. */
    @Test
    void userCreatesCommentOnPublicBoard() throws Exception {
        Member member = mock(Member.class);
        when(boardQueryRepository.existsPublicById(1L)).thenReturn(true);
        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));
        when(member.getName()).thenReturn("홍길동");
        when(commentRepository.save(any(BoardComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createUserComment(
                1L,
                7L,
                new CommentCreateRequest(" 댓글 ")
        );

        assertThat(response.writerType()).isEqualTo(WriterType.USER);
        assertThat(response.writerId()).isEqualTo(7L);
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(new ObjectMapper().writeValueAsString(response))
                .contains("\"name\":\"홍길동\"");
        assertThat(response.content()).isEqualTo("댓글");
    }

    /** 없거나 숨겨진 게시글에는 댓글을 생성할 수 없다. */
    @Test
    void creationRejectsHiddenBoard() {
        when(boardQueryRepository.existsPublicById(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.createUserComment(
                1L,
                7L,
                new CommentCreateRequest("댓글")
        )).isInstanceOf(BoardNotFoundException.class);
        verify(commentRepository, never()).save(any());
    }

    /** 대댓글 아래에 다시 대댓글을 생성하는 요청을 거부한다. */
    @Test
    void replyRejectsReplyAsParent() {
        BoardComment root = BoardComment.create(
                BoardType.GENERAL,
                1L,
                WriterType.USER,
                7L,
                null,
                "댓글"
        );
        BoardComment reply = BoardComment.create(
                BoardType.GENERAL,
                1L,
                WriterType.USER,
                8L,
                root,
                "답글"
        );
        when(boardQueryRepository.existsPublicById(1L)).thenReturn(true);
        when(commentRepository.findByIdAndPost(2L, BoardType.GENERAL, 1L))
                .thenReturn(Optional.of(reply));

        assertThatThrownBy(() -> service.createUserReply(
                1L,
                2L,
                7L,
                new CommentCreateRequest("중첩")
        )).isInstanceOf(IllegalArgumentException.class);
    }

    /** 다른 사용자가 댓글을 수정하려는 요청을 거부한다. */
    @Test
    void updateRejectsDifferentUser() {
        BoardComment comment = BoardComment.create(
                BoardType.GENERAL,
                1L,
                WriterType.USER,
                7L,
                null,
                "댓글"
        );
        when(boardQueryRepository.existsPublicById(1L)).thenReturn(true);
        when(commentRepository.findByIdAndPost(2L, BoardType.GENERAL, 1L))
                .thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.updateUserComment(
                1L,
                2L,
                8L,
                new CommentUpdateRequest("수정")
        )).isInstanceOf(CommentAccessDeniedException.class);
    }

    /** 관리자는 노출 여부와 관계없이 활성 게시글에 댓글을 작성할 수 있다. */
    @Test
    void adminCanCommentOnActiveBoard() {
        Board board = Board.createGeneral(7L, "제목", "내용");
        when(boardQueryRepository.findActiveById(1L)).thenReturn(Optional.of(board));
        when(commentRepository.save(any(BoardComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createAdminComment(
                1L,
                3L,
                new CommentCreateRequest("안내")
        );

        assertThat(response.writerType()).isEqualTo(WriterType.ADMIN);
    }

    /** 내 댓글 조회는 활성 사용자 댓글을 페이징 응답으로 반환한다. */
    @Test
    void getsMyCommentsAsPagedResponse() {
        BoardComment comment = BoardComment.create(
                BoardType.GENERAL,
                11L,
                WriterType.USER,
                7L,
                null,
                "내 댓글"
        );
        MyCommentSearchCondition condition = new MyCommentSearchCondition();
        condition.setPage(0);
        condition.setSize(20);

        when(commentRepository.findMyComments(
                WriterType.USER,
                7L,
                PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(
                List.of(comment),
                PageRequest.of(0, 20),
                1
        ));
        when(boardRepository.findAllById(any())).thenReturn(List.of());

        var response = service.getMyComments(7L, condition);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).singleElement().satisfies(item -> {
            assertThat(item.postId()).isEqualTo(11L);
            assertThat(item.content()).isEqualTo("내 댓글");
        });
    }
}
