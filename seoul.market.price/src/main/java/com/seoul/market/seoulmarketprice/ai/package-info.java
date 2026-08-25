/**
 * 자연어 기반 부동산 가격 검색 기능을 제공하는 패키지다.
 *
 * <p>컨트롤러는 AI 검색 및 장소·주변 아파트 도구 API를 외부에 공개하고,
 * 서비스 계층은 질문 의도 분석, 지역/지표 해석, 통계 조회, AI 설명 생성을
 * 기능별로 분리해 처리한다. DTO는 각 기능의 요청·응답과 검색 해석 결과를
 * 전달하며, repository 계층은 아파트 위치 데이터셋을 조회한다.</p>
 *
 * <ul>
 *   <li>{@code controller}: AI 관련 HTTP 진입점</li>
 *   <li>{@code service}: 자연어 검색, 순위·추이·비교·주변 아파트 조회 및 설명 생성</li>
 *   <li>{@code repository}: 아파트 위치 데이터셋 접근 및 비활성 구현체</li>
 *   <li>{@code dto}: 검색 조건, 해석 결과, 통계 사실과 API 응답 모델</li>
 *   <li>{@code config}: 데이터셋과 질문 분석 관련 애플리케이션 설정</li>
 * </ul>
 */
package com.seoul.market.seoulmarketprice.ai;
