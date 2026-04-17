package com.project.billing_service.events;

import com.project.billing_service.model.entities.UsageChargeEntity;
import lombok.Getter;

@Getter
public class UsageChargeCreatedEvent {

    private final UsageChargeEntity entity;

    public UsageChargeCreatedEvent(UsageChargeEntity entity) {
        this.entity = entity;
    }

}
