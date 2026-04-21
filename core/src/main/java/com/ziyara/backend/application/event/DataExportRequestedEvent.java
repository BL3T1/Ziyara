package com.ziyara.backend.application.event;

import java.util.UUID;

public record DataExportRequestedEvent(UUID exportRequestId) {
}
