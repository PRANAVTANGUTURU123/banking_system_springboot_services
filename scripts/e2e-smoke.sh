#!/usr/bin/env bash
# End-to-end checks against a running stack (docker compose up -d).
#
#   scripts/e2e-smoke.sh            # payment flows, idempotency, duplicates, concurrency
#   scripts/e2e-smoke.sh --with-dlq # also the retry -> dead-letter path (~1 min, restarts settlement)
#
# Needs only bash + curl (+ docker for the biller insert / DLQ restart).

set -uo pipefail
cd "$(dirname "$0")/.."

ACC=http://localhost:8084/api/v1
PAY=http://localhost:8086/api/v1
EFT=http://localhost:8089/api/v1
BILLPAY_MOCK=http://localhost:8090/api/mock/central1
EFT_MOCK=http://localhost:8091/api/mock/eftnetwork

RUN=$(date +%s)                                  # makes idempotency keys unique per run
EXEC_DATE=$(date -d "+30 days" +%Y-%m-%d)
PASS=0; FAIL=0

# --- helpers ---------------------------------------------------------------
field() { sed -n 's/.*"'"$1"'":"\{0,1\}\([^",}]*\).*/\1/p' | head -1; }   # flat JSON field
ok()    { PASS=$((PASS+1)); echo "  PASS  $1"; }
bad()   { FAIL=$((FAIL+1)); echo "  FAIL  $1"; }
check() { if [ "$2" = "$3" ]; then ok "$1 ($2)"; else bad "$1: expected '$3', got '$2'"; fi; }
num()   { printf '%.2f' "$1"; }                                            # 900 == 900.00

post()  { curl -s -X POST "$1" -H "Content-Type: application/json" ${3:+-H "Idempotency-Key: $3"} -d "$2"; }

new_account() {  # $1 opening balance -> account id
  post "$ACC/accounts" "{\"customerId\":\"cust-e2e\",\"accountType\":\"CHEQUING\",\"accountSubType\":\"PERSONAL\",\"status\":\"ACTIVE\",\"currency\":\"CAD\",\"openingBalance\":$1}" "acct-$RUN-$RANDOM" | field id
}
balance()   { curl -s "$ACC/accounts/$1/balance" | field "$2"; }
payment()   { curl -s "$PAY/payments/$1"; }
billpay()   { post "$PAY/payments/billpay" "{\"debtorAccountId\":\"$1\",\"billerReferenceNumber\":\"E2E-HYDRO\",\"invoiceReference\":\"INV-$RUN\",\"executionDate\":\"$EXEC_DATE\",\"amount\":{\"value\":$2,\"currency\":\"CAD\"}}" "$3"; }

wait_state() {   # $1 paymentId, $2 wanted state, $3 timeout seconds -> prints final state
  local s=""
  for _ in $(seq 1 "$3"); do
    s=$(payment "$1" | field state)
    [ "$s" = "$2" ] && break
    sleep 1
  done
  echo "$s"
}

# --- setup -------------------------------------------------------------------
echo "== setup"
docker compose exec -T postgres psql -q -U postgres -d billerdb -c \
  "insert into billers (id, customer_id, name, reference_number, category, status, created_at, updated_at)
   select gen_random_uuid(), 'cust-e2e', 'E2E Hydro', 'E2E-HYDRO', 'Electricity', 'ACTIVE', now(), now()
   where not exists (select 1 from billers where reference_number = 'E2E-HYDRO');" \
  && ok "biller E2E-HYDRO registered" || bad "biller insert"

# --- 1. bill pay happy path --------------------------------------------------
echo "== 1. bill pay: accepted -> batched -> submitted -> posted"
A=$(new_account 1000)
P=$(billpay "$A" 100 "pay-$RUN-1" | field paymentId)
[ -n "$P" ] && ok "payment accepted ($P)" || bad "payment not accepted"
check "100 held while in flight" "$(num "$(balance "$A" totalHolds)")" "100.00"
check "payment submitted after batch cutoff" "$(wait_state "$P" SUBMITTED 60)" "SUBMITTED"
B=$(payment "$P" | field batchId)
post "$BILLPAY_MOCK/pain002/$B" "" > /dev/null
check "payment posted after pain.002" "$(wait_state "$P" POSTED 30)" "POSTED"
check "balance debited" "$(num "$(balance "$A" balance)")" "900.00"
check "hold captured (no holds left)" "$(num "$(balance "$A" totalHolds)")" "0.00"

# --- 2. idempotency + duplicate delivery ------------------------------------
echo "== 2. idempotency and duplicate delivery"
P2=$(billpay "$A" 100 "pay-$RUN-1" | field paymentId)
check "same Idempotency-Key returns the same payment" "$P2" "$P"
post "$BILLPAY_MOCK/pain002/$B" "" > /dev/null          # clearing network sends the file again
sleep 8
check "redelivered pain.002 does not debit twice" "$(num "$(balance "$A" balance)")" "900.00"

# --- 3. rejected payment ------------------------------------------------------
echo "== 3. bill pay rejected by the network -> FAILED, hold released"
P3=$(billpay "$A" 50 "pay-$RUN-3" | field paymentId)
check "payment submitted" "$(wait_state "$P3" SUBMITTED 60)" "SUBMITTED"
post "$BILLPAY_MOCK/pain002/$(payment "$P3" | field batchId)?rejectAll=true" "" > /dev/null
check "payment failed" "$(wait_state "$P3" FAILED 30)" "FAILED"
check "balance untouched" "$(num "$(balance "$A" balance)")" "900.00"
check "hold released" "$(num "$(balance "$A" available)")" "900.00"

# --- 4. validation errors map to proper HTTP codes ----------------------------
echo "== 4. error responses"
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$PAY/payments/billpay" -H "Content-Type: application/json" \
  -H "Idempotency-Key: pay-$RUN-4" \
  -d "{\"debtorAccountId\":\"$A\",\"billerReferenceNumber\":\"E2E-HYDRO\",\"invoiceReference\":\"X\",\"executionDate\":\"$EXEC_DATE\",\"amount\":{\"value\":999999,\"currency\":\"CAD\"}}")
check "insufficient funds -> 422 (was 500)" "$code" "422"
code=$(curl -s -o /dev/null -w '%{http_code}' "$PAY/payments/00000000-0000-0000-0000-000000000000")
check "unknown payment -> 404 (was 500)" "$code" "404"

# --- 5. concurrent debits cannot overdraw ------------------------------------
echo "== 5. 20 concurrent debits of 10.00 against a 100.00 balance"
C=$(new_account 100)
codes=$(for i in $(seq 1 20); do
  curl -s -o /dev/null -w '%{http_code}\n' -X POST "$ACC/accounts/$C/debit" \
    -H "Content-Type: application/json" -d '{"amount":10.00,"reason":"race"}' &
done; wait)
codes=$(echo "$codes" | grep -oE '[0-9]{3}')   # parallel output can interleave stray bytes
check "exactly 10 debits succeeded" "$(echo "$codes" | grep -c '^201$')" "10"
check "the other 10 were rejected (422)" "$(echo "$codes" | grep -c '^422$')" "10"
check "balance is exactly 0, never negative" "$(num "$(balance "$C" balance)")" "0.00"

# --- 6. EFT rail --------------------------------------------------------------
echo "== 6. EFT: register + verify external account, pay, ack -> posted"
X=$(post "$EFT/external-accounts" "{\"customerId\":\"cust-e2e\",\"accountHolderName\":\"E2E Payee\",\"institutionNumber\":\"001\",\"transitNumber\":\"12345\",\"accountNumber\":\"$(printf '%09d' $((RUN % 1000000000)))\",\"currency\":\"CAD\"}" | field id)
post "$EFT/external-accounts/$X/verify" "" > /dev/null
E=$(post "$PAY/payments/eft" "{\"debtorAccountId\":\"$A\",\"externalAccountId\":\"$X\",\"executionDate\":\"$EXEC_DATE\",\"amount\":{\"value\":200,\"currency\":\"CAD\"}}" "eft-$RUN-1" | field paymentId)
check "EFT payment submitted" "$(wait_state "$E" SUBMITTED 60)" "SUBMITTED"
post "$EFT_MOCK/ack/$(payment "$E" | field batchId)" "" > /dev/null
check "EFT payment posted" "$(wait_state "$E" POSTED 30)" "POSTED"
check "balance debited" "$(num "$(balance "$A" balance)")" "700.00"

# --- 7. retry -> dead letter (optional) ---------------------------------------
if [ "${1:-}" = "--with-dlq" ]; then
  echo "== 7. Central1 down: 4 failed uploads (5s/10s/20s backoff) -> dead letter -> FAILED, hold released"
  CENTRAL1_FAILURE_RATE=1 docker compose up -d settlement-service > /dev/null 2>&1
  sleep 25   # let settlement restart
  P7=$(billpay "$A" 70 "pay-$RUN-7" | field paymentId)
  check "payment failed after retries exhausted" "$(wait_state "$P7" FAILED 120)" "FAILED"
  payment "$P7" | field reason | grep -q BATCH_DEAD_LETTERED && ok "reason is BATCH_DEAD_LETTERED" || bad "reason not BATCH_DEAD_LETTERED"
  check "hold released" "$(num "$(balance "$A" available)")" "700.00"
  docker compose up -d settlement-service > /dev/null 2>&1   # back to healthy uploads
fi

echo
echo "== $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
