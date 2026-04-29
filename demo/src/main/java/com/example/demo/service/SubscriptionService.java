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

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    @Value("${stripe.price-id}")
    private String priceId;

    private final SubscriptionRepository subscriptionRepository;

    // 🔥 有料判定
    public boolean isPremium(User user) {
        return subscriptionRepository.findByUserId(user.getUserId())
                .map(sub -> {

                    boolean result = "active".equals(sub.getStatus()) &&
                            sub.getCurrentPeriodEnd() != null &&
                            sub.getCurrentPeriodEnd().isAfter(LocalDateTime.now());

                    return result;
                })
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
        sub.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));
        sub.setCancelAtPeriodEnd(false);

        subscriptionRepository.save(sub);
    }

    public String createCheckoutSession(User user) throws Exception {

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl("https://programing-quiz-zeta.vercel.app/success")
                .setCancelUrl("https://programing-quiz-zeta.vercel.app/cancel")
                // .setSuccessUrl("http://localhost:3000/success")
                // .setCancelUrl("http://localhost:3000/cancel")

                // 🔥 ここが最重要（ユーザー紐付け）
                .putMetadata("userId", user.getUserId().toString())

                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(priceId) // ← Stripeで作ったPrice
                                .setQuantity(1L)
                                .build())
                .build();

        Session session = Session.create(params);

        return session.getUrl();
    }

    // 🔥 解約（仮実装）
    public void cancel(User user) {

        Subscription subEntity = subscriptionRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        try {
            com.stripe.model.Subscription stripeSub = com.stripe.model.Subscription
                    .retrieve(subEntity.getStripeSubscriptionId());

            stripeSub.update(
                    Map.of("cancel_at_period_end", true));

        } catch (Exception e) {
            throw new RuntimeException("Stripe解約失敗", e);
        }
    }
}