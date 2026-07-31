package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.member.dto.request.admin.AdminCreateRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminCreateResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminListResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminPageResponse;
import com.seoul.market.seoulmarketprice.member.exception.DuplicateAdminException;
import com.seoul.market.seoulmarketprice.member.repository.AdminCreationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 계정 생성 로직을 구현하는 서비스.
 *
 * <p>
 * auth 패키지의 관리자 코드는 변경하지 않고,
 * 평문 비밀번호를 BCrypt로 암호화한 뒤 저장한다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminManagementServiceImpl implements AdminManagementService {

    private final AdminCreationRepository adminCreationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AdminPageResponse getAdmins(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("페이지 크기는 1 이상 100 이하여야 합니다.");
        }

        long offset = (long) page * size;
        long totalElements = adminCreationRepository.countAll();
        List<AdminListResponse> content =
                adminCreationRepository.findAll(size, offset);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new AdminPageResponse(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                totalPages == 0 || page >= totalPages - 1
        );
    }

    /**
     * 중복 아이디를 확인하고 새 관리자 계정을 저장한다.
     *
     * @param request 관리자 생성 요청
     * @return 생성된 관리자 기본 정보
     * @throws DuplicateAdminException 이미 사용 중인 아이디인 경우
     */
    @Override
    @Transactional
    public AdminCreateResponse createAdmin(AdminCreateRequest request) {
        if (adminCreationRepository.existsByAdminId(request.userId())) {
            throw new DuplicateAdminException();
        }

        try {
            Long adminId = adminCreationRepository.save(
                    request.userId(),
                    passwordEncoder.encode(request.password()),
                    request.name()
            );
            return new AdminCreateResponse(
                    adminId,
                    request.userId(),
                    request.name()
            );
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateAdminException();
        }
    }

}
