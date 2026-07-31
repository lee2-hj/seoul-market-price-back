package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.member.dto.request.AdminCreateRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.AdminCreateResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.AdminListResponse;
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
    public List<AdminListResponse> getAdmins() {
        return adminCreationRepository.findAll();
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
