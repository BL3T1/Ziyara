package com.ziyara.backend.modules.notification.api;

import com.ziyara.backend.application.dto.StaffNotificationEvent;

public interface StaffNotificationCommandPublisher {

    void publishAfterCommit(StaffNotificationEvent event);
}
