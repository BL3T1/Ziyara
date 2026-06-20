package com.ziyara.backend.application.event;

import com.ziyara.backend.application.dto.event.DataExportRequestedEvent;
import com.ziyara.backend.application.service.DataExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Runs GDPR export materialization after the PENDING row is committed.
 */
@Component
@RequiredArgsConstructor
public class DataExportEventListener {

    private final DataExportService dataExportService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onExportRequested(DataExportRequestedEvent event) {
        dataExportService.completeExport(event.exportRequestId());
    }
}
