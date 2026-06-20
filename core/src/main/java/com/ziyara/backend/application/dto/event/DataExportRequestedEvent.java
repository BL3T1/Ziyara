package com.ziyara.backend.application.dto.event;

import java.util.UUID;

public record DataExportRequestedEvent(UUID exportRequestId) {
}
