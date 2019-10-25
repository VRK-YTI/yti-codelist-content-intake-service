package fi.vm.yti.codelist.intake.dto;

import java.util.UUID;

public class SubscriptionTypeRequestDTO {

    private String subscriptionType;
    private UUID userId;

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(final String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(final UUID userId) {
        this.userId = userId;
    }
}
