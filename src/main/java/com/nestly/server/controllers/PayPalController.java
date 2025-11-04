package com.nestly.server.controllers;

import com.nestly.server.services.PayPalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/api/paypal")
@CrossOrigin(origins = "${frontend.url:http://localhost:5173}")
public class PayPalController {

    private static final Logger log = LoggerFactory.getLogger(PayPalController.class);

    @Autowired
    private PayPalService payPalService;

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestParam double amount) {
        log.info("💰 Received request to create PayPal order for amount: {}", amount);
        try {
            Map<String, Object> orderResponse = payPalService.createOrder(amount);
            log.info("✅ PayPal order created successfully");
            return ResponseEntity.ok(orderResponse);
        } catch (Exception e) {
            log.error("❌ Error while creating PayPal order: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/capture-order/{orderId}")
    public ResponseEntity<Map<String, Object>> captureOrder(@PathVariable String orderId) {
        log.info("🟢 Received request to capture PayPal payment for orderId: {}", orderId);
        try {
            String captureId = payPalService.captureOrder(orderId);
            if (captureId != null) {
                log.info("💰 Payment captured successfully! Capture ID: {}", captureId);
                return ResponseEntity.ok(Map.of(
                        "status", "COMPLETED",
                        "orderId", orderId,
                        "captureId", captureId));
            } else {
                log.warn("⚠️ Payment capture did not return a captureId. OrderId={}", orderId);
                return ResponseEntity.ok(Map.of(
                        "status", "FAILED",
                        "orderId", orderId));
            }
        } catch (Exception e) {
            log.error("🚨 Error capturing PayPal order (orderId={}): {}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refund/{captureId}")
    public ResponseEntity<Map<String, Object>> refundPayment(
            @PathVariable String captureId,
            @RequestParam(required = false) Double amount) {
        log.info("💸 Refund request received for captureId={} amount={}", captureId, amount);
        try {
            String status = payPalService.refundPayment(captureId, amount);
            return ResponseEntity.ok(Map.of(
                    "captureId", captureId,
                    "refundStatus", status));
        } catch (Exception e) {
            log.error("❌ Error processing refund for captureId={}", captureId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
