package com.ziyarah.domain.entity;

import com.ziyarah.domain.enums.NotificationStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void markAsRead_ShouldUpdateStatusAndReadAt() {
        Notification notification = new Notification();
        notification.setStatus(NotificationStatus.SENT);
        
        notification.markAsRead();
        
        assertEquals(NotificationStatus.READ, notification.getStatus());
        assertNotNull(notification.getReadAt());
    }
}
