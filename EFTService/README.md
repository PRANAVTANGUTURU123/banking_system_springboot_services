# EFT Service (External Accounts) — Spring Boot Skeleton

This repo is intentionally a **skeleton** for viewers to implement.

## What viewers must implement (TODOs)
- Create external account (store only masked account number)
- List external accounts (pagination + search)
- Get external account (ownership check)
- Update nickname/institution (optimistic locking with If-Match)
- Delete (prefer soft delete)
- Verification flow (mock: flip PENDING -> VERIFIED)
- Idempotency on create

## Quick start
```bash
mvn spring-boot:run
```

> Note: Configure DB + JWT issuer-uri for your environment.
