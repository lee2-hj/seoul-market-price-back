package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.member.dto.request.admin.AdminCreateRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.admin.AdminUpdateRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminCreateResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminListResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminPageResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminUpdateResponse;
import com.seoul.market.seoulmarketprice.member.exception.AdminDeletionException;
import com.seoul.market.seoulmarketprice.member.exception.AdminNotFoundException;
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

    /**
     * 요청에 포함된 관리자 정보만 수정한다.
     * 새 비밀번호는 평문으로 저장하지 않고 BCrypt로 암호화한다.
     */
    @Override
    @Transactional
    public AdminUpdateResponse updateAdmin(Long id, AdminUpdateRequest request) {
        validateUpdateRequest(request);
        String encodedPassword = request.password() == null
                ? null : passwordEncoder.encode(request.password());

        return adminCreationRepository.update(
                        id,
                        encodedPassword,
                        request.name(),
                        request.phone(),
                        request.email()
                )
                .orElseThrow(AdminNotFoundException::new);
    }

    /** 자기 자신 또는 마지막 활성 관리자가 아닌 계정을 소프트 삭제한다. */
    @Override
    @Transactional
    public void deleteAdmin(Long id, Long currentAdminId) {
        if (!adminCreationRepository.existsActiveById(id)) {
            throw new AdminNotFoundException();
        }
        if (id.equals(currentAdminId)) {
            throw new AdminDeletionException("현재 로그인한 관리자 계정은 삭제할 수 없습니다.");
        }
        if (adminCreationRepository.countAll() <= 1) {
            throw new AdminDeletionException("마지막 활성 관리자 계정은 삭제할 수 없습니다.");
        }
        if (adminCreationRepository.softDelete(id) == 0) {
            throw new AdminNotFoundException();
        }
    }

    /** 빈 수정 요청과 공백 문자열이 저장되는 것을 차단한다. */
    private void validateUpdateRequest(AdminUpdateRequest request) {
        if (request.password() == null && request.name() == null
                && request.phone() == null && request.email() == null) {
            throw new IllegalArgumentException("수정할 관리자 정보를 하나 이상 입력해야 합니다.");
        }
        if (isBlank(request.password()) || isBlank(request.name())
                || isBlank(request.phone()) || isBlank(request.email())) {
            throw new IllegalArgumentException("관리자 수정 항목은 공백일 수 없습니다.");
        }
    }

    /** 값이 전달된 경우에만 공백 여부를 확인한다. */
    private boolean isBlank(String value) {
        return value != null && value.isBlank();
    }

}
