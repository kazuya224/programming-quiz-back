package com.example.demo.controller;

import com.example.demo.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final StripeWebhookService webhookService;

    @Value("${stripe.webhook-secret:dummy}")
    private String endpointSecret;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            HttpServletRequest request,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        // ✅ @RequestBody ではなく InputStream から直接読む
        String payload;
        try (InputStream is = request.getInputStream()) {
            payload = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Failed to read body");
        }

        Event event;
        try {
            if ("dummy".equals(endpointSecret) || sigHeader == null) {
                event = Event.GSON.fromJson(payload, Event.class);
            } else {
                event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            }
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(401).body("Invalid signature");
        }

        webhookService.handleEvent(event);

        return ResponseEntity.ok("success");
    }
}