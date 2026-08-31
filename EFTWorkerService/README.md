# EFT Worker Service (Mock) — Spring Boot Skeleton

Consumes EFT transfer requests and publishes EFT status events.

## Topics
- Consumes: `eft.transfer.requested`
- Produces: `eft.transfer.status`

## What viewers must implement (TODOs)
- Idempotency / de-dup using `processed_events`
- EFTServiceClient: validate external account + ownership
- AccountM2MClient: debit internal account
- Retry/backoff for rail submission
- Publish statuses: PROCESSING, POSTED, FAILED

## Run
```bash
mvn spring-boot:run
```
