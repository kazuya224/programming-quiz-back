package com.example.demo.service;

import com.example.demo.entity.ProcessedEvent;
import com.example.demo.entity.Subscription;
import com.example.demo.entity.User;
import com.example.demo.repository.ProcessedEventRepository;
import com.example.demo.repository.SubscriptionRepository;
import com.example.demo.repository.UserRepository;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StripeWebhookService {

        private final SubscriptionRepository subscriptionRepository;
        private final UserRepository userRepository;
        private final ProcessedEventRepository processedEventRepository;

        // ✅ status 昇順（巻き戻り防止）
        private static final List<String> STATUS_ORDER = List.of("incomplete", "trialing", "active", "past_due",
                        "canceled");

        private boolean shouldUpdateStatus(String current, String next) {
                if (current == null)
                        return true;
                int currentIdx = STATUS_ORDER.indexOf(current);
                int nextIdx = STATUS_ORDER.indexOf(next);
                if (currentIdx == -1 || nextIdx == -1)
                        return true;
                return nextIdx >= currentIdx;
        }

        @Transactional
        public void handleEvent(Event event) {

                // ✅ idempotency チェック（DB管理：再起動・スケールアウト対応）
                if (processedEventRepository.existsById(event.getId())) {
                        return;
                }

                try {
                        var deserializer = event.getDataObjectDeserializer();
                        String rawJson = deserializer.getRawJson();

                        if (rawJson == null || rawJson.isBlank()) {
                                return;
                        }

                        JsonObject obj = JsonParser.parseString(rawJson).getAsJsonObject();

                        switch (event.getType()) {
                                case "checkout.session.completed" -> handleCheckoutCompleted(obj);
                                case "invoice.payment_succeeded", "invoice.paid" -> handlePaymentSucceeded(obj);
                                case "customer.subscription.deleted" -> handleSubscriptionDeleted(obj);
                                case "customer.subscription.updated" -> handleSubscriptionUpdated(obj);
                                case "customer.subscription.created" -> handleSubscriptionCreated(obj);
                        }

                        // ✅ 正常完了後のみ処理済み登録（例外時は登録されずリトライ可能）
                        processedEventRepository.save(new ProcessedEvent(event.getId()));

                } catch (Exception e) {
                        System.err.println("Webhook handling error [" + event.getType() + "]: " + e.getMessage());
                        e.printStackTrace();
                        throw e; // ✅ @Transactional のロールバックを発火させる
                }
        }

        // ✅ userId を確定する唯一のポイント
        private void handleCheckoutCompleted(JsonObject obj) {

                JsonObject metadata = obj.getAsJsonObject("metadata");
                String userId = metadata != null && metadata.has("userId")
                                ? metadata.get("userId").getAsString()
                                : null;

                String subscriptionId = obj.has("subscription") && !obj.get("subscription").isJsonNull()
                                ? obj.get("subscription").getAsString()
                                : null;

                if (userId == null) {
                        return;
                }

                User user = userRepository.findById(UUID.fromString(userId))
                                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

                // ✅ トライアル使用済みフラグを立てる
                if (!user.isTrialUsed()) {
                        user.setTrialUsed(true);
                        userRepository.save(user);
                }

                Subscription sub = (subscriptionId != null)
                                ? subscriptionRepository
                                                .findByStripeSubscriptionId(subscriptionId)
                                                .orElseGet(Subscription::new)
                                : subscriptionRepository
                                                .findByUserId(user.getUserId())
                                                .orElseGet(Subscription::new);

                sub.setUserId(user.getUserId());
                sub.setStripeSubscriptionId(subscriptionId);
                sub.setCancelAtPeriodEnd(false);

                if (sub.getStatus() == null) {
                        sub.setStatus("incomplete");
                }

                subscriptionRepository.save(sub);
        }

        // ✅ Upsert（先着可）。userId はセットしない＝checkout が後から補完する
        private void handleSubscriptionCreated(JsonObject obj) {

                String stripeSubId = obj.get("id").getAsString();
                String status = obj.get("status").getAsString();

                Subscription sub = subscriptionRepository
                                .findByStripeSubscriptionId(stripeSubId)
                                .orElseGet(() -> {
                                        Subscription s = new Subscription();
                                        s.setStripeSubscriptionId(stripeSubId);
                                        return s;
                                });

                if (shouldUpdateStatus(sub.getStatus(), status)) {
                        sub.setStatus(status);
                }

                if (obj.has("current_period_end") && !obj.get("current_period_end").isJsonNull()) {
                        long periodEnd = obj.get("current_period_end").getAsLong();
                        sub.setCurrentPeriodEnd(
                                        LocalDateTime.ofInstant(
                                                        Instant.ofEpochSecond(periodEnd),
                                                        ZoneId.of("Asia/Tokyo")));
                }

                subscriptionRepository.save(sub);
        }

        // ✅ Upsert（先着可）。userId はセットしない＝checkout が後から補完する
        private void handlePaymentSucceeded(JsonObject obj) {

                String subscriptionId = null;

                if (obj.has("subscription") && !obj.get("subscription").isJsonNull()) {
                        subscriptionId = obj.get("subscription").getAsString();
                } else if (obj.has("parent") && !obj.get("parent").isJsonNull()) {
                        JsonObject parent = obj.getAsJsonObject("parent");
                        if (parent.has("subscription_details")) {
                                JsonObject subDetails = parent.getAsJsonObject("subscription_details");
                                if (subDetails.has("subscription")) {
                                        subscriptionId = subDetails.get("subscription").getAsString();
                                }
                        }
                }

                if (subscriptionId == null)
                        return;

                final String finalSubscriptionId = subscriptionId;
                Subscription sub = subscriptionRepository
                                .findByStripeSubscriptionId(finalSubscriptionId)
                                .orElseGet(() -> {
                                        Subscription s = new Subscription();
                                        s.setStripeSubscriptionId(finalSubscriptionId);
                                        return s;
                                });

                if (shouldUpdateStatus(sub.getStatus(), "active")) {
                        sub.setStatus("active");
                }

                if (obj.has("lines")) {
                        JsonObject lines = obj.getAsJsonObject("lines");
                        if (lines.has("data")) {
                                JsonObject first = lines.getAsJsonArray("data").get(0).getAsJsonObject();
                                JsonObject period = first.getAsJsonObject("period");
                                long periodEnd = period.get("end").getAsLong();
                                sub.setCurrentPeriodEnd(
                                                LocalDateTime.ofInstant(
                                                                Instant.ofEpochSecond(periodEnd),
                                                                ZoneId.of("Asia/Tokyo")));
                        }
                }

                subscriptionRepository.save(sub);
        }

        // ✅ Stripe準拠：status は Stripe の値をそのまま使う
        private void handleSubscriptionDeleted(JsonObject obj) {

                String stripeSubId = obj.get("id").getAsString();

                Subscription sub = subscriptionRepository
                                .findByStripeSubscriptionId(stripeSubId)
                                .orElse(null);

                if (sub == null) {
                        return;
                }

                // ✅ Stripe準拠（canceled / unpaid / incomplete_expired など将来拡張に対応）
                sub.setStatus(obj.get("status").getAsString());
                subscriptionRepository.save(sub);
        }

        // ✅ Upsert（先着可）。userId はセットしない＝checkout が後から補完する
        private void handleSubscriptionUpdated(JsonObject obj) {

                String stripeSubId = obj.get("id").getAsString();

                Subscription sub = subscriptionRepository
                                .findByStripeSubscriptionId(stripeSubId)
                                .orElseGet(() -> {
                                        Subscription s = new Subscription();
                                        s.setStripeSubscriptionId(stripeSubId);
                                        return s;
                                });

                String status = obj.get("status").getAsString();
                if (shouldUpdateStatus(sub.getStatus(), status)) {
                        sub.setStatus(status);
                }

                boolean cancelAtPeriodEnd = obj.get("cancel_at_period_end").getAsBoolean();
                sub.setCancelAtPeriodEnd(cancelAtPeriodEnd);

                if (obj.has("current_period_end") && !obj.get("current_period_end").isJsonNull()) {
                        long periodEnd = obj.get("current_period_end").getAsLong();
                        sub.setCurrentPeriodEnd(
                                        LocalDateTime.ofInstant(
                                                        Instant.ofEpochSecond(periodEnd),
                                                        ZoneId.of("Asia/Tokyo")));
                }

                subscriptionRepository.save(sub);
        }
}