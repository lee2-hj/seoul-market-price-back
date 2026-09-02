package com.seoul.market.seoulmarketprice.board.comment.service;

import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.auth.repository.AdminRepository;
import com.seoul.market.seoulmarketprice.board.entity.Board;
import com.seoul.market.seoulmarketprice.board.exception.BoardNotFoundException;
import com.seoul.market.seoulmarketprice.board.repository.BoardQueryRepository;
import com.seoul.market.seoulmarketprice.board.repository.BoardRepository;
import com.seoul.market.seoulmarketprice.board.comment.dto.condition.MyCommentSearchCondition;
import com.seoul.market.seoulmarketprice.board.comment.dto.request.CommentCreateRequest;
import com.seoul.market.seoulmarketprice.board.comment.dto.request.CommentUpdateRequest;
import com.seoul.market.seoulmarketprice.board.comment.dto.response.CommentResponse;
import com.seoul.market.seoulmarketprice.board.comment.dto.response.MyCommentPageResponse;
import com.seoul.market.seoulmarketprice.board.comment.dto.response.MyCommentResponse;
import com.seoul.market.seoulmarketprice.board.comment.entity.BoardComment;
import com.seoul.market.seoulmarketprice.board.comment.entity.BoardType;
import com.seoul.market.seoulmarketprice.board.comment.entity.WriterType;
import com.seoul.market.seoulmarketprice.board.comment.exception.CommentAccessDeniedException;
import com.seoul.market.seoulmarketprice.board.comment.exception.CommentNotFoundException;
import com.seoul.market.seoulmarketprice.board.comment.repository.CommentRepository;
import com.seoul.market.seoulmarketprice.member.repository.MemberManagementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 댓글과 대댓글의 조회, 작성, 권한 검증 및 관리자 기능을 처리한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final String DELETED_MESSAGE = "삭제된 댓글입니다.";
    private static final String HIDDEN_MESSAGE = "관리자에 의해 숨김 처리된 댓글입니다.";

    private final CommentRepository commentRepository;
    private final BoardQueryRepository boardQueryRepository;
    private final BoardRepository boardRepository;
    private final MemberManagementRepository memberRepository;
    private final AdminRepository adminRepository;

    /** 공개 게시글의 댓글을 조회하고 최상위 댓글과 대댓글 구조로 조립한다. */
    public List<CommentResponse> getComments(Long boardId) {
        requirePublicBoard(boardId);
        List<BoardComment> comments = commentRepository.findAllByPost(
                BoardType.GENERAL,
                boardId
        );
        Map<String, String> writerNames = loadWriterNames(comments);
        return buildTree(comments, writerNames);
    }

    /** 로그인 사용자가 작성한 활성 댓글을 마이페이지용 목록으로 최신순 조회한다. */
    public MyCommentPageResponse getMyComments(
            Long userId,
            MyCommentSearchCondition condition
    ) {
        Page<BoardComment> page = commentRepository.findMyComments(
                WriterType.USER,
                userId,
                PageRequest.of(condition.getPage(), condition.getSize())
        );
        Map<Long, String> boardTitles = loadBoardTitles(page.getContent());
        String name = userName(userId);

        List<MyCommentResponse> content = page.getContent().stream()
                .map(comment -> toMyCommentResponse(comment, name, boardTitles))
                .toList();

        return new MyCommentPageResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /** 로그인한 일반 사용자의 최상위 댓글을 생성한다. */
    @Transactional
    public CommentResponse createUserComment(
            Long boardId,
            Long userId,
            CommentCreateRequest request
    ) {
        requirePublicBoard(boardId);
        BoardComment comment = BoardComment.create(
                BoardType.GENERAL,
                boardId,
                WriterType.USER,
                userId,
                null,
                request.content().trim()
        );
        BoardComment savedComment = commentRepository.save(comment);
        return toResponse(savedComment, userName(userId), List.of());
    }

    /** 활성 상태의 최상위 댓글에 한 단계 대댓글을 생성한다. */
    @Transactional
    public CommentResponse createUserReply(
            Long boardId,
            Long parentId,
            Long userId,
            CommentCreateRequest request
    ) {
        requirePublicBoard(boardId);
        BoardComment parent = findComment(boardId, parentId);
        validateReplyParent(parent);

        BoardComment reply = BoardComment.create(
                BoardType.GENERAL,
                boardId,
                WriterType.USER,
                userId,
                parent,
                request.content().trim()
        );
        BoardComment savedReply = commentRepository.save(reply);
        return toResponse(savedReply, userName(userId), List.of());
    }

    /** 일반 사용자가 본인 소유의 활성 댓글만 수정하게 한다. */
    @Transactional
    public CommentResponse updateUserComment(
            Long boardId,
            Long commentId,
            Long userId,
            CommentUpdateRequest request
    ) {
        requirePublicBoard(boardId);
        BoardComment comment = findActiveComment(boardId, commentId);
        requireOwner(comment, userId);
        comment.update(request.content().trim());
        return toResponse(comment, userName(userId), List.of());
    }

    /** 일반 사용자가 본인 소유의 활성 댓글만 소프트 삭제하게 한다. */
    @Transactional
    public void deleteUserComment(
            Long boardId,
            Long commentId,
            Long userId
    ) {
        requirePublicBoard(boardId);
        BoardComment comment = findActiveComment(boardId, commentId);
        requireOwner(comment, userId);
        comment.softDelete();
    }

    /** 관리자가 활성 게시글에 관리자 명의의 댓글을 생성한다. */
    @Transactional
    public CommentResponse createAdminComment(
            Long boardId,
            Long adminId,
            CommentCreateRequest request
    ) {
        if (boardQueryRepository.findActiveById(boardId).isEmpty()) {
            throw new BoardNotFoundException();
        }

        BoardComment comment = BoardComment.create(
                BoardType.GENERAL,
                boardId,
                WriterType.ADMIN,
                adminId,
                null,
                request.content().trim()
        );
        BoardComment savedComment = commentRepository.save(comment);
        return toResponse(savedComment, adminName(adminId), List.of());
    }

    /** 관리자가 활성 댓글의 공개 또는 숨김 상태를 변경한다. */
    @Transactional
    public CommentResponse changeVisibility(
            Long commentId,
            boolean visible
    ) {
        BoardComment comment = commentRepository.findById(commentId)
                .filter(BoardComment::isActive)
                .orElseThrow(CommentNotFoundException::new);

        comment.changeVisibility(visible);
        String name = visible ? writerName(comment) : null;
        return toResponse(comment, name, List.of());
    }

    /** 관리자가 지정한 활성 댓글을 소프트 삭제한다. */
    @Transactional
    public void deleteByAdmin(Long commentId) {
        BoardComment comment = commentRepository.findById(commentId)
                .filter(BoardComment::isActive)
                .orElseThrow(CommentNotFoundException::new);
        comment.softDelete();
    }

    /** 댓글 작업 대상 게시글이 공개 상태인지 확인한다. */
    private void requirePublicBoard(Long boardId) {
        if (!boardQueryRepository.existsPublicById(boardId)) {
            throw new BoardNotFoundException();
        }
    }

    /** URL의 게시글과 실제 댓글 소속이 일치하는지 함께 검증한다. */
    private BoardComment findComment(Long boardId, Long commentId) {
        return commentRepository.findByIdAndPost(
                        commentId,
                        BoardType.GENERAL,
                        boardId
                )
                .orElseThrow(CommentNotFoundException::new);
    }

    /** 삭제되지 않은 댓글만 수정 및 삭제 대상으로 반환한다. */
    private BoardComment findActiveComment(Long boardId, Long commentId) {
        BoardComment comment = findComment(boardId, commentId);
        if (!comment.isActive()) {
            throw new CommentNotFoundException();
        }
        return comment;
    }

    /** 대댓글은 활성·공개 상태의 최상위 댓글에만 작성할 수 있다. */
    private void validateReplyParent(BoardComment parent) {
        if (!parent.isActive() || !parent.isVisible() || !parent.isRoot()) {
            throw new IllegalArgumentException("대댓글을 작성할 수 없는 댓글입니다.");
        }
    }

    /** 일반 사용자가 댓글 작성자인지 확인한다. */
    private void requireOwner(BoardComment comment, Long userId) {
        if (!comment.isOwnedBy(WriterType.USER, userId)) {
            throw new CommentAccessDeniedException();
        }
    }

    /** 평면 조회 결과를 최상위 댓글과 한 단계 대댓글 목록으로 변환한다. */
    private List<CommentResponse> buildTree(
            List<BoardComment> comments,
            Map<String, String> writerNames
    ) {
        Map<Long, List<BoardComment>> repliesByParent = comments.stream()
                .filter(comment -> !comment.isRoot())
                .collect(Collectors.groupingBy(
                        comment -> comment.getParent().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<CommentResponse> result = new ArrayList<>();
        for (BoardComment root : comments) {
            if (!root.isRoot()) {
                continue;
            }

            List<BoardComment> replies = repliesByParent.getOrDefault(
                    root.getId(),
                    List.of()
            );
            if (!root.isActive() && replies.isEmpty()) {
                continue;
            }

            List<CommentResponse> replyResponses = replies.stream()
                    .map(reply -> maskedResponse(reply, writerNames, List.of()))
                    .toList();
            result.add(maskedResponse(root, writerNames, replyResponses));
        }
        return result;
    }

    /** 삭제 또는 숨김 댓글의 내용과 작성자 정보를 마스킹한다. */
    private CommentResponse maskedResponse(
            BoardComment comment,
            Map<String, String> writerNames,
            List<CommentResponse> replies
    ) {
        if (!comment.isActive()) {
            return masked(comment, DELETED_MESSAGE, replies);
        }
        if (!comment.isVisible()) {
            return masked(comment, HIDDEN_MESSAGE, replies);
        }

        String writerName = writerNames.get(
                writerKey(comment.getWriterType(), comment.getWriterId())
        );
        return toResponse(comment, writerName, replies);
    }

    /** 작성자 정보를 제외한 마스킹 응답을 생성한다. */
    private CommentResponse masked(
            BoardComment comment,
            String message,
            List<CommentResponse> replies
    ) {
        return new CommentResponse(
                comment.getId(),
                parentId(comment),
                null,
                null,
                null,
                message,
                comment.isVisible(),
                !comment.isActive(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                replies
        );
    }

    /** 댓글 엔티티를 API 응답으로 변환한다. */
    private CommentResponse toResponse(
            BoardComment comment,
            String writerName,
            List<CommentResponse> replies
    ) {
        return new CommentResponse(
                comment.getId(),
                parentId(comment),
                comment.getWriterType(),
                comment.getWriterId(),
                writerName,
                comment.getContent(),
                comment.isVisible(),
                !comment.isActive(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                replies
        );
    }

    private Long parentId(BoardComment comment) {
        return comment.getParent() == null
                ? null
                : comment.getParent().getId();
    }

    /** 내 댓글 엔티티를 원문 이동 정보가 포함된 마이페이지 응답으로 변환한다. */
    private MyCommentResponse toMyCommentResponse(
            BoardComment comment,
            String name,
            Map<Long, String> boardTitles
    ) {
        return new MyCommentResponse(
                comment.getId(),
                parentId(comment),
                comment.getBoardType(),
                comment.getPostId(),
                boardTitles.get(comment.getPostId()),
                name,
                comment.getContent(),
                comment.isVisible(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    /** 일반 게시판 댓글의 게시글 제목을 한 번에 조회해 댓글별 제목 맵을 만든다. */
    private Map<Long, String> loadBoardTitles(List<BoardComment> comments) {
        Set<Long> boardIds = comments.stream()
                .filter(comment -> comment.getBoardType() == BoardType.GENERAL)
                .map(BoardComment::getPostId)
                .collect(Collectors.toSet());

        return boardRepository.findAllById(boardIds).stream()
                .collect(Collectors.toMap(Board::getId, Board::getTitle));
    }

    /** 댓글 작성자 ID를 유형별로 모아 이름을 일괄 조회한다. */
    private Map<String, String> loadWriterNames(List<BoardComment> comments) {
        Set<Long> userIds = writerIds(comments, WriterType.USER);
        Set<Long> adminIds = writerIds(comments, WriterType.ADMIN);
        Map<String, String> names = new HashMap<>();

        memberRepository.findAllById(userIds).forEach(member ->
                names.put(
                        writerKey(WriterType.USER, member.getId()),
                        member.getName()
                )
        );
        adminRepository.findAllById(adminIds).forEach(admin ->
                names.put(
                        writerKey(WriterType.ADMIN, admin.getId()),
                        admin.getName()
                )
        );
        return names;
    }

    /** 이름 조회가 필요한 활성·공개 댓글의 작성자 ID만 추출한다. */
    private Set<Long> writerIds(
            List<BoardComment> comments,
            WriterType writerType
    ) {
        return comments.stream()
                .filter(BoardComment::isActive)
                .filter(BoardComment::isVisible)
                .filter(comment -> comment.getWriterType() == writerType)
                .map(BoardComment::getWriterId)
                .collect(Collectors.toSet());
    }

    private String writerName(BoardComment comment) {
        return comment.getWriterType() == WriterType.USER
                ? userName(comment.getWriterId())
                : adminName(comment.getWriterId());
    }

    private String userName(Long id) {
        return memberRepository.findById(id)
                .map(Member::getName)
                .orElse(null);
    }

    private String adminName(Long id) {
        return adminRepository.findById(id)
                .map(Admin::getName)
                .orElse(null);
    }

    private String writerKey(WriterType writerType, Long id) {
        return writerType.name() + ':' + id;
    }
}
