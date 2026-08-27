package com.seoul.market.seoulmarketprice.config;

import co.elastic.clients.transport.DefaultTransportOptions;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.rest5_client.Rest5ClientOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchClientConfig {

    /*
     * elasticsearch-java 9.x 클라이언트는 기본적으로 Content-Type/Accept 헤더에
     * "compatible-with=9"를 붙여 보내는데, 접속 대상 엘라스틱서치 서버는 8.14.3이라
     * 이 값을 거부한다(media_type_header_exception: Accept version must be either
     * version 8 or 7, but found 9). 두 헤더를 일반 application/json으로 덮어써서
     * 8.x 서버와 통신할 수 있게 한다.
     *
     * Spring Boot가 Rest5ClientTransport를 만들 때 이 타입의 빈이 있으면
     * ObjectProvider로 가져다 써서 자동으로 적용된다.
     */
    @Bean
    public Rest5ClientOptions rest5ClientOptions() {
        TransportOptions transportOptions = new DefaultTransportOptions().toBuilder()
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json")
                .build();

        return Rest5ClientOptions.of(transportOptions);
    }
}
