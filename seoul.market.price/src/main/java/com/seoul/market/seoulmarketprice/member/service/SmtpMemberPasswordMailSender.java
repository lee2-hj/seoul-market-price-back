package com.seoul.market.seoulmarketprice.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmtpMemberPasswordMailSender implements MemberPasswordMailSender {
    private final JavaMailSender mailSender;

    @Override
    public void send(String email, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("서울마켓 임시 비밀번호 안내");
        message.setText("임시 비밀번호는 " + temporaryPassword + " 입니다. 로그인 후 비밀번호를 변경해 주세요.");
        mailSender.send(message);
    }
}
