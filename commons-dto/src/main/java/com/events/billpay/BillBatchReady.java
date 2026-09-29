package com.events.billpay;

import java.util.UUID;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BillBatchReady {
  private String eventId;
  private UUID batchId;
  private int lineCount;           // payments in the batch
  private String correlationId;
  private String occurredAt;
  private String schemaVersion;
  private String channel;
}