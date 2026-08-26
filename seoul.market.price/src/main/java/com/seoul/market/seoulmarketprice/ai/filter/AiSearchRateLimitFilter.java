package com.seoul.market.seoulmarketprice.ai.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoul.market.seoulmarketprice.common.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Public natural-language search endpoint's small, per-instance IP rate limiter. */
@Component
public class AiSearchRateLimitFilter extends OncePerRequestFilter {

    private static final String SEARCH_PATH = "/api/ai/search-natural";

    private final ObjectMapper objectMapper;
    private final int maxRequestsPerWindow;
    private final int maxConcurrentRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, ClientState> clients = new ConcurrentHashMap<>();

    public AiSearchRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.ai.search-rate-limit.requests-per-minute:5}") int maxRequestsPerWindow,
            @Value("${app.ai.search-rate-limit.max-concurrent-requests:1}") int maxConcurrentRequests
    ) {
        this.objectMapper = objectMapper;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.windowMillis = Duration.ofMinutes(1).toMillis();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !HttpMethod.POST.matches(request.getMethod()) || !SEARCH_PATH.equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String clientIp = request.getRemoteAddr();
        ClientState state = clients.computeIfAbsent(clientIp, ignored -> new ClientState());
        LimitResult result = state.tryAcquire(System.currentTimeMillis());

        if (result != LimitResult.ALLOWED) {
            writeLimitExceeded(response, result);
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            state.release();
        }
    }

    private void writeLimitExceeded(HttpServletResponse response, LimitResult result) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", result == LimitResult.CONCURRENT_LIMIT ? "5" : "60");
        String message = result == LimitResult.CONCURRENT_LIMIT
                ? "이전 AI 검색을 처리 중입니다. 잠시 후 다시 시도해주세요."
                : "AI 검색 요청은 1분에 5회까지 가능합니다. 잠시 후 다시 시도해주세요.";
        objectMapper.writeValue(response.getWriter(), new ErrorResponse("AI-RATE-001", message));
    }

    private enum LimitResult {
        ALLOWED,
        REQUEST_LIMIT,
        CONCURRENT_LIMIT
    }

    private final class ClientState {
        private long windowStartedAt = System.currentTimeMillis();
        private int requestCount;
        private int inFlight;

        synchronized LimitResult tryAcquire(long now) {
            if (now - windowStartedAt >= windowMillis) {
                windowStartedAt = now;
                requestCount = 0;
            }
            if (inFlight >= maxConcurrentRequests) {
                return LimitResult.CONCURRENT_LIMIT;
            }
            if (requestCount >= maxRequestsPerWindow) {
                return LimitResult.REQUEST_LIMIT;
            }
            requestCount++;
            inFlight++;
            return LimitResult.ALLOWED;
        }

        synchronized void release() {
            inFlight = Math.max(0, inFlight - 1);
        }
    }
}
