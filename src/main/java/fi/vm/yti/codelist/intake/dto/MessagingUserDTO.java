package fi.vm.yti.codelist.intake.dto;

import java.util.Set;
import java.util.UUID;

public class MessagingUserDTO {

    private UUID id;
    private String subscriptionType;
    private Set<MessagingResourceDTO> resources;

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(final String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public Set<MessagingResourceDTO> getResources() {
        return resources;
    }

    public void setResources(final Set<MessagingResourceDTO> resources) {
        this.resources = resources;
    }
}
