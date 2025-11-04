package com.nestly.server.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class PayPalService {

    private static final Logger logger = LoggerFactory.getLogger(PayPalService.class);

    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    @Value("${paypal.base.url}")
    private String baseUrl;

    @Value("${paypal.currency:USD}")
    private String currency;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAccessToken() {
        try {
            logger.info("🔑 Requesting PayPal access token...");
            String url = baseUrl + "/v1/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(clientId, clientSecret);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> entity = new HttpEntity<>("grant_type=client_credentials", headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {
                    });

            Map<String, Object> body = Objects.requireNonNull(response.getBody());
            logger.info("✅ PayPal access token retrieved");
            return (String) body.get("access_token");
        } catch (Exception e) {
            logger.error("❌ Failed to get PayPal access token", e);
            throw new RuntimeException("PayPal access token error");
        }
    }

    public Map<String, Object> createOrder(double amount) {
        try {
            logger.info("🛒 Creating PayPal order for amount={}", amount);
            String url = baseUrl + "/v2/checkout/orders";
            String accessToken = getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> orderRequest = Map.of(
                    "intent", "CAPTURE",
                    "purchase_units", new Object[] {
                            Map.of("amount", Map.of("currency_code", currency, "value", String.format("%.2f", amount)))
                    },
                    "application_context", Map.of(
                            "return_url",
                            System.getenv("FRONTEND_URL") != null ? System.getenv("FRONTEND_URL") + "/success"
                                    : "http://localhost:5173/success",
                            "cancel_url",
                            System.getenv("FRONTEND_URL") != null ? System.getenv("FRONTEND_URL") + "/cancel"
                                    : "http://localhost:5173/cancel"));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderRequest, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {
                    });

            logger.info("✅ PayPal createOrder response received");
            return Objects.requireNonNull(response.getBody());
        } catch (Exception e) {
            logger.error("❌ Failed to create PayPal order", e);
            throw new RuntimeException("PayPal create order error");
        }
    }

    public String captureOrder(String orderId) {
        try {
            logger.info("📦 Capturing PayPal order: {}", orderId);
            String url = baseUrl + "/v2/checkout/orders/" + orderId + "/capture";
            String accessToken = getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {
                    });

            Map<String, Object> body = Objects.requireNonNull(response.getBody());
            logger.info("✅ PayPal captureOrder response received");

            String captureId = null;
            try {
                var purchaseUnits = (java.util.List<Map<String, Object>>) body.get("purchase_units");
                if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
                    var payments = (Map<String, Object>) purchaseUnits.get(0).get("payments");
                    if (payments != null && payments.get("captures") != null) {
                        var captures = (java.util.List<Map<String, Object>>) payments.get("captures");
                        if (!captures.isEmpty()) {
                            captureId = (String) captures.get(0).get("id");
                            String captureStatus = (String) captures.get(0).get("status");
                            logger.info("💰 Capture successful: captureId={} status={}", captureId, captureStatus);
                        }
                    }
                }
            } catch (Exception inner) {
                logger.warn("⚠️ Could not extract captureId from PayPal response", inner);
            }

            return captureId;
        } catch (Exception e) {
            logger.error("❌ Failed to capture PayPal order", e);
            throw new RuntimeException("PayPal capture order error");
        }
    }

    public String refundPayment(String captureId, double amount) {
        try {
            logger.info("💰 Initiating refund for captureId={} amount={}", captureId, amount);

            String accessToken = getAccessToken();
            logger.info("🔑 PayPal access token retrieved");

            String url = "https://api-m.sandbox.paypal.com/v2/payments/captures/" + captureId + "/refund";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            Map<String, String> amountMap = new HashMap<>();
            amountMap.put("value", String.format("%.2f", amount));
            amountMap.put("currency_code", "USD");
            body.put("amount", amountMap);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map responseBody = response.getBody();
                String refundId = (String) responseBody.get("id");
                String status = (String) responseBody.get("status");
                logger.info("✅ Final refund status for captureId={} refundId={} => {}", captureId, refundId, status);
                return status; // <--- THIS IS IMPORTANT
            } else {
                logger.error("❌ PayPal refund failed. Response: {}", response);
                return "FAILED";
            }
        } catch (HttpClientErrorException e) {
            logger.error("❌ PayPal refund failed for captureId={}", captureId, e);
            return "FAILED";
        }
    }

    private String checkRefundStatus(String refundId) {
        try {
            String url = baseUrl + "/v2/payments/refunds/" + refundId;
            String accessToken = getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
                    });

            Map<String, Object> body = response.getBody();
            if (body != null && body.get("status") != null) {
                return (String) body.get("status");
            }
        } catch (Exception e) {
            logger.error("❌ Failed to check refund status for refundId=" + refundId, e);
        }
        return "PENDING";
    }
}
