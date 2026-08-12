package com.seoul.market.seoulmarketprice.board.service;

import com.seoul.market.seoulmarketprice.board.dto.request.AdminBoardUpdateRequest;
import com.seoul.market.seoulmarketprice.board.dto.condition.BoardSearchCondition;
import com.seoul.market.seoulmarketprice.board.dto.request.BoardCreateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.BoardUpdateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.NoticeCreateRequest;
import com.seoul.market.seoulmarketprice.board.dto.response.BoardDetailResponse;
import com.seoul.market.seoulmarketprice.board.dto.response.BoardListResponse;
import com.seoul.market.seoulmarketprice.board.dto.response.BoardPageResponse;
import com.seoul.market.seoulmarketprice.board.entity.Board;
import com.seoul.market.seoulmarketprice.board.exception.BoardAccessDeniedException;
import com.seoul.market.seoulmarketprice.board.exception.BoardNotFoundException;
import com.seoul.market.seoulmarketprice.board.repository.BoardRepository;
import com.seoul.market.seoulmarketprice.board.repository.BoardQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 일반 게시판의 조회, 작성, 권한 검증 및 관리자 기능을 처리한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    /** 게시글 저장과 조회를 담당하는 저장소이다. */
    private final BoardRepository boardRepository;

    /** 공개 게시판의 동적 검색과 페이징을 담당하는 QueryDSL 저장소이다. */
    private final BoardQueryRepository boardQueryRepository;

    /** 공개 게시글을 공지 고정 우선, 최신순으로 페이징 조회한다. */
    public BoardPageResponse getBoards(
            BoardSearchCondition condition
    ) {
        validatePage(condition.getPage(), condition.getSize());
        condition.setKeyword(normalizeKeyword(condition.getKeyword()));

        PageRequest pageable = PageRequest.of(
                condition.getPage(),
                condition.getSize()
        );

        Page<Board> result = boardQueryRepository.findPublicPage(condition, pageable);

        return new BoardPageResponse(
                result.getContent().stream().map(this::toListResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    /** 조회수를 원자적으로 증가시킨 뒤 공개 게시글 상세 정보를 반환한다. */
    @Transactional
    public BoardDetailResponse getBoard(Long id) {
        if (boardQueryRepository.incrementViewCount(id) == 0) {
            throw new BoardNotFoundException();
        }

        Board board = boardQueryRepository.findPublicById(id)
                .orElseThrow(BoardNotFoundException::new);
        return toDetailResponse(board);
    }

    /** 로그인 사용자의 일반 게시글을 생성한다. */
    @Transactional
    public BoardDetailResponse createBoard(
            Long userId,
            String loginId,
            BoardCreateRequest request
    ) {
        Board board = Board.createGeneral(
                userId,
                request.title().trim(),
                request.content().trim()
        );
        return toDetailResponse(boardRepository.save(board), loginId);
    }

    /** 작성자 본인의 일반 게시글만 수정한다. */
    @Transactional
    public BoardDetailResponse updateBoard(
            Long id,
            Long userId,
            BoardUpdateRequest request
    ) {
        Board board = findActive(id);
        requireOwner(board, userId);
        board.updateGeneral(
                request.title().trim(),
                request.content().trim()
        );
        return toDetailResponse(board);
    }

    /** 작성자 본인의 일반 게시글만 소프트 삭제한다. */
    @Transactional
    public void deleteBoard(Long id, Long userId) {
        Board board = findActive(id);
        requireOwner(board, userId);
        board.softDelete();
    }

    /** 관리자 명의의 공지사항을 생성한다. */
    @Transactional
    public BoardDetailResponse createNotice(
            Long adminId,
            NoticeCreateRequest request
    ) {
        boolean visible = request.visible() == null || request.visible();
        boolean pinned = Boolean.TRUE.equals(request.pinned());

        Board board = Board.createNotice(
                adminId,
                request.title().trim(),
                request.content().trim(),
                visible,
                pinned
        );
        return toDetailResponse(boardRepository.save(board));
    }

    /** 관리자가 전달한 필드만 선택적으로 수정한다. */
    @Transactional
    public BoardDetailResponse updateByAdmin(
            Long id,
            AdminBoardUpdateRequest request
    ) {
        validateAdminUpdate(request);
        Board board = findActive(id);
        board.updateByAdmin(
                trim(request.title()),
                trim(request.content()),
                request.visible(),
                request.pinned()
        );
        return toDetailResponse(board);
    }

    /** 관리자가 지정한 활성 게시글을 소프트 삭제한다. */
    @Transactional
    public void deleteByAdmin(Long id) {
        findActive(id).softDelete();
    }

    /** 삭제되지 않은 게시글을 조회한다. */
    private Board findActive(Long id) {
        return boardQueryRepository.findActiveById(id)
                .orElseThrow(BoardNotFoundException::new);
    }

    /** 일반 사용자가 게시글 작성자인지 확인한다. */
    private void requireOwner(Board board, Long userId) {
        if (!board.isOwnedBy(userId)) {
            throw new BoardAccessDeniedException();
        }
    }

    /** 첨부파일 변경 전에 활성 게시글의 작성자 본인인지 확인한다. */
    public void requireOwner(Long id, Long userId) {
        requireOwner(findActive(id), userId);
    }

    /** 첨부파일 공개 조회 전에 게시글이 공개 상태인지 확인한다. */
    public void requirePublicAccess(Long id) {
        boardQueryRepository.findPublicById(id)
                .orElseThrow(BoardNotFoundException::new);
    }

    /** 관리자 첨부파일 작업 전에 삭제되지 않은 게시글인지 확인한다. */
    public void requireActive(Long id) {
        findActive(id);
    }

    /** 페이지 번호와 크기의 허용 범위를 검증한다. */
    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("페이지 크기는 1 이상 100 이하여야 합니다.");
        }
    }

    /** 검색어의 앞뒤 공백을 제거하고 길이를 검증한다. */
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String normalized = keyword.trim();
        if (normalized.length() > 200) {
            throw new IllegalArgumentException("검색어는 200자 이하여야 합니다.");
        }
        return normalized;
    }

    /** 관리자 수정 요청에 실제 변경 항목이 존재하는지 검증한다. */
    private void validateAdminUpdate(AdminBoardUpdateRequest request) {
        if (request.title() == null
                && request.content() == null
                && request.visible() == null
                && request.pinned() == null) {
            throw new IllegalArgumentException("수정할 항목을 하나 이상 입력해야 합니다.");
        }

        if ((request.title() != null && request.title().isBlank())
                || (request.content() != null && request.content().isBlank())) {
            throw new IllegalArgumentException("제목과 내용은 공백일 수 없습니다.");
        }
    }

    /** null은 유지하고 값이 있으면 앞뒤 공백을 제거한다. */
    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** 게시글 엔티티를 목록 응답으로 변환한다. */
    private BoardListResponse toListResponse(Board board) {
        return new BoardListResponse(
                board.getId(),
                board.getPostType(),
                board.getTitle(),
                board.getWriterUserId(),
                board.getWriterName(),
                board.getMemberId(),
                board.getViewCount(),
                board.isPinned(),
                board.getCreatedAt()
        );
    }

    /** 게시글 엔티티를 상세 응답으로 변환한다. */
    private BoardDetailResponse toDetailResponse(Board board) {
        return toDetailResponse(board, board.getWriterUserId());
    }

    /** 작성자 로그인 아이디를 지정하여 게시글 상세 응답으로 변환한다. */
    private BoardDetailResponse toDetailResponse(Board board, String writerUserId) {
        return new BoardDetailResponse(
                board.getId(),
                board.getPostType(),
                board.getTitle(),
                board.getContent(),
                writerUserId,
                board.getWriterName(),
                board.getMemberId(),
                board.getViewCount(),
                board.isVisible(),
                board.isPinned(),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}
