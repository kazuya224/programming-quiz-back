package com.example.demo.service;

import com.example.demo.entity.Subscription;
import com.example.demo.entity.User;
import com.example.demo.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import com.stripe.param.SubscriptionUpdateParams;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    @Value("${stripe.price-id}")
    private String priceId;

    private final SubscriptionRepository subscriptionRepository;

    // 🔥 有料判定
    public boolean isPremium(User user) {
        return subscriptionRepository.findByUserId(user.getUserId())
                .map(this::isActiveSubscription)
                .orElse(false);
    }

    // 🔥 サブスク取得
    public Subscription getSubscription(User user) {
        return subscriptionRepository.findByUserId(user.getUserId())
                .orElse(null);
    }

    // 🔥 モック課金（審査中用）
    public void checkout(User user) {

        Subscription sub = subscriptionRepository.findByUserId(user.getUserId())
                .orElse(new Subscription());

        sub.setUserId(user.getUserId());
        sub.setStripeSubscriptionId("sub_mock_" + user.getUserId());
        sub.setStatus("active");
        // sub.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));
        sub.setCancelAtPeriodEnd(false);

        subscriptionRepository.save(sub);
    }

    public String createCheckoutSession(User user) throws Exception {

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl("https://programing-quiz-zeta.vercel.app/success")
                .setCancelUrl("https://programing-quiz-zeta.vercel.app/cancel")
                .putMetadata("userId", user.getUserId().toString())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build());

        // ✅ trialUsedがfalseの場合のみトライアルを付与
        if (!user.isTrialUsed()) {
            builder.setSubscriptionData(
                    SessionCreateParams.SubscriptionData.builder()
                            .setTrialPeriodDays(7L)
                            .build());
        } else {
        }

        Session session = Session.create(builder.build());
        return session.getUrl();
    }

    // 🔥 解約（仮実装）
    public void cancel(User user) {

        Subscription subEntity = subscriptionRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (subEntity.getStripeSubscriptionId() == null) {
            throw new RuntimeException("stripeSubscriptionId is null");
        }

        try {
            com.stripe.model.Subscription stripeSub = com.stripe.model.Subscription
                    .retrieve(subEntity.getStripeSubscriptionId());

            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(true)
                    .build();

            stripeSub.update(params);

            subEntity.setCancelAtPeriodEnd(true);
            subscriptionRepository.save(subEntity);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Stripe解約失敗", e);
        }
    }

    public boolean isPremium(UUID userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(this::isActiveSubscription)
                .orElse(false);
    }

    private boolean isActiveSubscription(Subscription sub) {
        String status = sub.getStatus();

        boolean isActiveStatus = "active".equals(status) || "trialing".equals(status);

        boolean notExpired = sub.getCurrentPeriodEnd() != null &&
                sub.getCurrentPeriodEnd().isAfter(LocalDateTime.now());

        return isActiveStatus && notExpired;
    }
}