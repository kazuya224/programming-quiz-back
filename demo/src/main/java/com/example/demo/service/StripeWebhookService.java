package com.example.demo.service;

import com.example.demo.entity.Subscription;
import com.example.demo.entity.User;
import com.example.demo.repository.SubscriptionRepository;
import com.example.demo.repository.UserRepository;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StripeWebhookService {

        private final SubscriptionRepository subscriptionRepository;
        private final UserRepository userRepository;

        public void handleEvent(Event event) {
                try {
                        // ✅ rawJsonをJsonObjectとして直接パース（モデルクラスを使わない）
                        String rawJson = event.getDataObjectDeserializer().getRawJson();
                        JsonObject obj = JsonParser.parseString(rawJson).getAsJsonObject();

                        switch (event.getType()) {
                                case "checkout.session.completed" -> handleCheckoutCompleted(obj);
                                case "invoice.payment_succeeded" -> handlePaymentSucceeded(obj);
                                case "customer.subscription.deleted" -> handleSubscriptionDeleted(obj);
                                case "customer.subscription.updated" -> handleSubscriptionUpdated(obj);
                        }
                } catch (Exception e) {
                        System.err.println("Webhook handling error [" + event.getType() + "]: " + e.getMessage());
                        e.printStackTrace();
                }
        }

        private void handleCheckoutCompleted(JsonObject obj) {

                // metadataからuserIdを取得
                JsonObject metadata = obj.getAsJsonObject("metadata");
                String userId = metadata != null && metadata.has("userId")
                                ? metadata.get("userId").getAsString()
                                : null;

                // subscriptionIdを取得
                String subscriptionId = obj.has("subscription") && !obj.get("subscription").isJsonNull()
                                ? obj.get("subscription").getAsString()
                                : null;

                if (userId == null) {
                        System.err.println("userIdがnullです");
                        return;
                }

                User user = userRepository.findById(UUID.fromString(userId))
                                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

                Subscription sub = subscriptionRepository
                                .findByUserId(user.getUserId())
                                .orElse(new Subscription());

                sub.setUserId(user.getUserId());
                sub.setStripeSubscriptionId(subscriptionId);
                sub.setStatus("active");
                sub.setCancelAtPeriodEnd(false);
                // ✅ currentPeriodEndは invoice.payment_succeeded で上書きされるので仮値を入れる
                sub.setCurrentPeriodEnd(LocalDateTime.now().plusDays(1));

                Subscription saved = subscriptionRepository.save(sub);
        }

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

                if (subscriptionId == null) {
                        return;
                }

                // ✅ effectively final にするためコピー
                final String finalSubscriptionId = subscriptionId;

                Subscription sub = subscriptionRepository
                                .findByStripeSubscriptionId(finalSubscriptionId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Subscription not found: " + finalSubscriptionId));

                sub.setStatus("active");

                long periodEnd = obj.getAsJsonObject("lines")
                                .getAsJsonArray("data").get(0).getAsJsonObject()
                                .getAsJsonObject("period")
                                .get("end").getAsLong();

                sub.setCurrentPeriodEnd(
                                LocalDateTime.ofInstant(Instant.ofEpochSecond(periodEnd), ZoneId.systemDefault()));

                Subscription saved = subscriptionRepository.save(sub);
        }

        private void handleSubscriptionDeleted(JsonObject obj) {

                String stripeSubId = obj.get("id").getAsString();

                Subscription sub = subscriptionRepository
                                .findByStripeSubscriptionId(stripeSubId)
                                .orElseThrow(() -> new RuntimeException("Subscription not found: " + stripeSubId));

                sub.setStatus("canceled");
                subscriptionRepository.save(sub);
        }

        private void handleSubscriptionUpdated(JsonObject obj) {

                String stripeSubId = obj.get("id").getAsString();

                Subscription sub = subscriptionRepository
                                .findByStripeSubscriptionId(stripeSubId)
                                .orElse(null);

                if (sub == null) {
                        return;
                }

                boolean cancelAtPeriodEnd = obj.get("cancel_at_period_end").getAsBoolean();
                sub.setCancelAtPeriodEnd(cancelAtPeriodEnd);
                subscriptionRepository.save(sub);
        }
}