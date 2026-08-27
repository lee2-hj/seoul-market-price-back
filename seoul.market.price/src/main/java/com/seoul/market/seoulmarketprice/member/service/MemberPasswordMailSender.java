package com.seoul.market.seoulmarketprice.member.service;

public interface MemberPasswordMailSender {
    void send(String email, String temporaryPassword);
}
