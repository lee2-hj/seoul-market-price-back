# AI 질문 분석 계약

FastAPI의 `POST /ai/analyze-question` 엔드포인트는 사용자의 질문을 **검색 실행 계획**으로 변환한다. 응답은 반드시 JSON 객체 하나여야 하며 Markdown, 설명 문장, 코드 블록을 포함하면 안 된다.

실제 가격 조회, 평균·순위·증감률 계산은 데이터 조회 서비스가 담당한다. LLM은 숫자를 추측하거나 직접 계산하지 않는다.

## 응답 규칙

- 추출할 수 없거나 확신할 수 없는 값은 추측하지 않고 `null`로 반환한다.
- `intent`, `target`, `metric`, `direction`, 지역 `type`, 후보 `metric`은 대문자 enum으로 반환한다.
- `limit`은 1~100 정수이며 질문에 개수가 없으면 10을 사용한다.
- `regions`에는 질문에서 확인된 지역명과 `DISTRICT`, `DONG` 등의 유형을 넣는다.
- 필수값이 없거나 불명확하면 해당 필드명을 `missingFields`에 넣고 `requiresClarification`을 `true`로 설정한다.
- 인기, 좋은 곳, 추천 등 지표가 여러 의미로 해석되면 `ambiguousConcept`와 `metricCandidates`를 반환한다.
- `metricCandidates[].confidence`는 0~1 범위다.
- `requestedMetrics`에는 사용자가 요청한 데이터 지표만, `toolPlan`에는 백엔드가 수행할 조회 작업만 기록한다.
- 존재하지 않는 지역, 아파트, 가격, 거래량 또는 순위를 만들어내면 안 된다.

## 응답 구조

정확한 제약은 [`ai-question-analysis.schema.json`](./ai-question-analysis.schema.json)을 따른다. FastAPI에서는 가능하면 모델의 JSON Schema 응답 형식과 Pydantic 응답 모델을 함께 적용한다.

## 예시 1: 명확한 순위 질문

입력: `거래가 제일 많은 강남구 아파트 알려줘`

```json
{
  "intent": "APARTMENT_RANKING",
  "regions": [{"name": "강남구", "type": "DISTRICT"}],
  "referencePlace": null,
  "target": "APARTMENT",
  "metric": "TRADE_COUNT",
  "direction": "DESC",
  "limit": 10,
  "period": null,
  "requestedMetrics": ["TRADE_COUNT"],
  "toolPlan": ["SEARCH_APARTMENTS", "RANK_BY_TRADE_COUNT"],
  "missingFields": [],
  "ambiguousConcept": null,
  "metricCandidates": [],
  "requiresClarification": false
}
```

## 예시 2: 지역 비교

입력: `강동구와 성동구 평균 가격 비교해줘`

```json
{
  "intent": "REGION_COMPARISON",
  "regions": [
    {"name": "강동구", "type": "DISTRICT"},
    {"name": "성동구", "type": "DISTRICT"}
  ],
  "referencePlace": null,
  "target": "REGION",
  "metric": "AVERAGE_PRICE",
  "direction": null,
  "limit": 10,
  "period": null,
  "requestedMetrics": ["AVERAGE_PRICE"],
  "toolPlan": ["COMPARE_REGIONS"],
  "missingFields": [],
  "ambiguousConcept": null,
  "metricCandidates": [],
  "requiresClarification": false
}
```

## 예시 3: 애매한 지표

입력: `인기 있는 아파트 알려줘`

`인기`를 임의의 점수로 만들지 않는다. 거래 건수 등의 후보를 제시하되 안전하게 하나를 선택할 수 없으면 `metric`을 `null`로 두고 명확화를 요청한다.

```json
{
  "intent": "APARTMENT_RANKING",
  "regions": [],
  "referencePlace": null,
  "target": "APARTMENT",
  "metric": null,
  "direction": "DESC",
  "limit": 10,
  "period": null,
  "requestedMetrics": [],
  "toolPlan": [],
  "missingFields": ["region", "metric"],
  "ambiguousConcept": "POPULARITY",
  "metricCandidates": [
    {"metric": "TRADE_COUNT", "confidence": 0.7, "reason": "거래 건수로 인기를 해석할 수 있음"}
  ],
  "requiresClarification": true
}
```

## FastAPI 반영 방법

1. [`ai-question-analysis-system-prompt.txt`](./ai-question-analysis-system-prompt.txt)를 질문 분석 모델의 system prompt로 사용한다.
2. [`ai-question-analysis.schema.json`](./ai-question-analysis.schema.json)을 structured output 스키마로 지정한다.
3. 모델 출력을 Pydantic 모델로 다시 검증하고 검증 실패 시 임의 보정 대신 오류 또는 명확화 응답을 반환한다.
4. 데이터 조회·집계 도구는 검증된 검색 계획을 받은 뒤에만 실행한다.
