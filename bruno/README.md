# Open Payments API Test Collection

This Bruno collection demonstrates a complete Open Payments flow for sending money between two wallets using the Interledger Protocol (ILP).

## Overview

The Open Payments standard enables secure, authorized payments between different Account Servicing Entities (ASEs). This collection tests a payment from **Grace Franklin** (Cloud Nine Wallet) to **Alice Smith** (Happy Life Bank).

## Prerequisites

1. **Docker Environment Running**
   ```bash
   cd /path/to/refiki-local-test
   docker compose up -d
   ```

2. **Bruno CLI or GUI**
   - Install Bruno: https://www.usebruno.com/
   - Or use CLI: `npm install -g @usebruno/cli`

3. **Install Dependencies**
   ```bash
   cd bruno
   npm install
   ```

## Environment Configuration

The `Local` environment is pre-configured with:

| Variable | Value | Description |
|----------|-------|-------------|
| `senderWalletAddress` | `http://localhost:3000/accounts/gfranklin` | Grace Franklin (Cloud Nine) |
| `receiverWalletAddress` | `http://localhost:4000/accounts/asmith` | Alice Smith (Happy Life) |
| `clientWalletAddress` | `http://localhost:4000/accounts/pfry` | Philip Fry - the "client" making requests |
| `clientKeyId` | `keyid-97a3a431-8ee1-48fc-ac85-70e2f5eba8e5` | Key ID for request signing |
| `clientPrivateKey` | (base64 Ed25519 key) | Private key for HTTP signatures |

## The Payment Flow

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────────┐     ┌────────────────┐
│   Client    │     │  Cloud Nine     │     │  Happy Life     │     │    Identity    │
│   (Bruno)   │     │  Wallet (3000)  │     │  Bank (4000)    │     │    Provider    │
└──────┬──────┘     └────────┬────────┘     └────────┬────────┘     └───────┬────────┘
       │                     │                       │                      │
       │ 1. GET /accounts/gfranklin                  │                      │
       │────────────────────>│                       │                      │
       │      wallet info    │                       │                      │
       │<────────────────────│                       │                      │
       │                     │                       │                      │
       │ 2. GET /accounts/asmith                     │                      │
       │─────────────────────────────────────────────>                      │
       │      wallet info                            │                      │
       │<─────────────────────────────────────────────                      │
       │                     │                       │                      │
       │ 3. POST /auth (grant for incoming-payment)  │                      │
       │─────────────────────────────────────────────>                      │
       │      access_token                           │                      │
       │<─────────────────────────────────────────────                      │
       │                     │                       │                      │
       │ 4. POST /incoming-payments                  │                      │
       │─────────────────────────────────────────────>                      │
       │      incoming payment created               │                      │
       │<─────────────────────────────────────────────                      │
       │                     │                       │                      │
       │ 5. POST /auth (grant for quote)             │                      │
       │────────────────────>│                       │                      │
       │      access_token   │                       │                      │
       │<────────────────────│                       │                      │
       │                     │                       │                      │
       │ 6. POST /quotes     │                       │                      │
       │────────────────────>│                       │                      │
       │      quote created  │                       │                      │
       │<────────────────────│                       │                      │
       │                     │                       │                      │
       │ 7. POST /auth (grant for outgoing-payment, interactive)            │
       │────────────────────>│                       │                      │
       │   redirect URL + continue token             │                      │
       │<────────────────────│                       │                      │
       │                     │                       │                      │
       │                  *** USER INTERACTION ***                          │
       │                  (approve in browser at redirect URL)              │
       │                     │                       │                      │
       │ 8. POST /continue   │                       │                      │
       │────────────────────>│                       │                      │
       │      access_token   │                       │                      │
       │<────────────────────│                       │                      │
       │                     │                       │                      │
       │ 9. POST /outgoing-payments                  │                      │
       │────────────────────>│                       │                      │
       │      payment created│──── ILP transfer ────>│                      │
       │<────────────────────│                       │                      │
       │                     │                       │                      │
       │ 10. GET /outgoing-payments/:id              │                      │
       │────────────────────>│                       │                      │
       │      payment status │                       │                      │
       │<────────────────────│                       │                      │
```

## Request Details

### 1. Get Sender Wallet Address

**Purpose:** Discover the sender's wallet capabilities and auth server URL.

```
GET http://localhost:3000/accounts/gfranklin
Accept: application/json
```

**Response includes:**
- `id`: Wallet address URL
- `assetCode`: Currency (USD)
- `assetScale`: Decimal places (2)
- `authServer`: URL for GNAP authorization
- `resourceServer`: URL for Open Payments resources

**What happens:** The script extracts the auth server URL and stores it as `senderOpenPaymentsAuthHost` for later grant requests.

---

### 2. Get Receiver Wallet Address

**Purpose:** Discover the receiver's wallet capabilities and auth server URL.

```
GET http://localhost:4000/accounts/asmith
Accept: application/json
```

**What happens:** Same as above, but for the receiver. Stores `receiverOpenPaymentsAuthHost` and `receiverOpenPaymentsHost`.

---

### 3. Grant Request: Incoming Payment

**Purpose:** Request permission to create an incoming payment on the receiver's account.

```
POST http://localhost:4006/  (Happy Life Bank Auth Server)
Content-Type: application/json

{
  "access_token": {
    "access": [{
      "type": "incoming-payment",
      "actions": ["create", "read", "list", "complete"]
    }]
  },
  "client": "http://localhost:4000/accounts/pfry"
}
```

**Key concepts:**
- **GNAP (Grant Negotiation and Authorization Protocol):** The authorization protocol used by Open Payments
- **Client:** The wallet address making the request (pfry) - this identifies who is asking for access
- **HTTP Signatures:** The request is signed with the client's private key to prove identity

**Response includes:**
- `access_token.value`: Bearer token for subsequent requests
- `access_token.manage`: URL to manage/revoke the token

---

### 4. Create Incoming Payment

**Purpose:** Create a payment destination on the receiver's account.

```
POST http://localhost:4000/accounts/asmith/incoming-payments
Authorization: GNAP <access_token>
Content-Type: application/json

{
  "walletAddress": "http://localhost:4000/accounts/asmith",
  "incomingAmount": {
    "value": "100",
    "assetCode": "USD",
    "assetScale": 2
  },
  "expiresAt": "2024-01-15T00:00:00.000Z",
  "metadata": {
    "description": "Free Money!"
  }
}
```

**Key concepts:**
- **Incoming Payment:** A one-time payment destination that can receive funds
- **incomingAmount:** The expected amount (100 = $1.00 with scale 2)
- **expiresAt:** When this payment destination expires

**Response includes:**
- `id`: URL of the created incoming payment (used in the quote)
- `receivedAmount`: How much has been received (initially 0)

---

### 5. Grant Request: Quote

**Purpose:** Request permission to create quotes on the sender's account.

```
POST http://localhost:3006/  (Cloud Nine Wallet Auth Server)
Content-Type: application/json

{
  "access_token": {
    "access": [{
      "type": "quote",
      "actions": ["create", "read"]
    }]
  },
  "client": "http://localhost:4000/accounts/pfry"
}
```

**Why a separate grant?** Each resource type (quote, incoming-payment, outgoing-payment) requires its own authorization grant for security.

---

### 6. Create Quote

**Purpose:** Calculate the cost of sending money to the incoming payment.

```
POST http://localhost:3000/accounts/gfranklin/quotes
Authorization: GNAP <access_token>
Content-Type: application/json

{
  "walletAddress": "http://localhost:3000/accounts/gfranklin",
  "receiver": "http://localhost:4000/accounts/asmith/incoming-payments/<id>",
  "method": "ilp"
}
```

**Key concepts:**
- **Quote:** A binding offer showing how much the sender will pay
- **receiver:** The incoming payment URL from step 4
- **method: "ilp":** Use Interledger Protocol for the transfer

**Response includes:**
- `debitAmount`: What will be deducted from sender (includes fees)
- `receiveAmount`: What receiver will get
- `expiresAt`: Quote validity period

---

### 7. Grant Request: Outgoing Payment (Interactive)

**Purpose:** Request permission to send money. This requires user consent.

```
POST http://localhost:3006/
Content-Type: application/json

{
  "access_token": {
    "access": [{
      "type": "outgoing-payment",
      "actions": ["create", "read", "list"],
      "identifier": "http://localhost:3000/accounts/gfranklin",
      "limits": {
        "debitAmount": {"value": "103", "assetCode": "USD", "assetScale": 2}
      }
    }]
  },
  "client": "http://localhost:4000/accounts/pfry",
  "interact": {
    "start": ["redirect"]
  }
}
```

**Key concepts:**
- **Interactive Grant:** Because this involves sending money, user consent is required
- **limits.debitAmount:** Maximum amount that can be sent (from the quote)
- **interact.start: ["redirect"]:** Request a redirect URL for user approval

**Response includes:**
- `interact.redirect`: URL where user must approve the payment
- `continue.uri`: URL to poll/continue after approval
- `continue.access_token.value`: Token for the continuation request

---

### 8. Continuation Request

**Purpose:** After user approves in browser, exchange the approval for an access token.

```
POST http://localhost:3006/continue/<continue_id>
Authorization: GNAP <continue_token>
```

**What happens between steps 7 and 8:**
1. User opens the `interact.redirect` URL in a browser
2. User sees the payment details and clicks "Approve"
3. The identity provider records the approval
4. Client calls this continuation endpoint

**Response includes:**
- `access_token.value`: The actual token to create the outgoing payment

---

### 9. Create Outgoing Payment

**Purpose:** Execute the payment using the approved quote.

```
POST http://localhost:3000/accounts/gfranklin/outgoing-payments
Authorization: GNAP <access_token>
Content-Type: application/json

{
  "walletAddress": "http://localhost:3000/accounts/gfranklin",
  "quoteId": "http://localhost:3000/accounts/gfranklin/quotes/<quote_id>",
  "metadata": {
    "description": "Free Money!"
  }
}
```

**Key concepts:**
- **Outgoing Payment:** The actual payment execution
- **quoteId:** References the quote from step 6 (ensures same terms)

**What happens:**
1. Cloud Nine Wallet debits Grace Franklin's account
2. ILP packets are sent to Happy Life Bank
3. Happy Life Bank credits Alice Smith's account
4. The incoming payment's `receivedAmount` is updated

---

### 10. Get Outgoing Payment

**Purpose:** Check the payment status and final amounts.

```
GET http://localhost:3000/accounts/gfranklin/outgoing-payments/<id>
Authorization: GNAP <access_token>
```

**Response includes:**
- `state`: Payment state (COMPLETED, FAILED, PENDING)
- `sentAmount`: Actual amount sent
- `debitAmount`: Actual amount debited

---

## HTTP Signatures

All authenticated requests use HTTP Message Signatures (RFC 9421). The signature:

1. **Signs** specific headers and body content
2. **Proves** the request comes from the claimed client
3. **Prevents** tampering and replay attacks

The `scripts.js` file handles this by calling an external signing service:
```
signatureUrl: https://kxu5d4mr4blcthphxomjlc4xk40rvdsx.lambda-url.eu-central-1.on.aws/
```

---

## Running the Collection

### Using Bruno GUI

1. Open Bruno
2. Import collection from `refiki-local-test/bruno`
3. Select "Local" environment
4. Run requests 1-7 in sequence
5. **CRITICAL - Interactive Grant Approval (after request 7):**
   - Look at the response from request 7
   - Find the `interact.redirect` URL (also printed in console)
   - Open this URL in your browser
   - Click **"Approve"** to authorize the payment
   - Return to Bruno
6. Run request 8 (Continuation Request)
   - Console should print: `SUCCESS: Got access token for outgoing-payment grant!`
   - If it prints `WARNING: Grant is still pending approval!`, go back to step 5
7. Run requests 9-10

### Using Bruno CLI

```bash
cd bruno
bru run --env Local
```

**Note:** The CLI will pause at step 7-8 requiring manual browser interaction for approval.

---

## Interactive Grant Flow (Steps 7-8)

The outgoing payment grant requires **user consent** because it authorizes sending money. This is an interactive GNAP flow:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        INTERACTIVE GRANT FLOW                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Request 7                        Browser                    Request 8      │
│  ──────────                       ───────                    ──────────     │
│                                                                              │
│  POST /auth ──┐                                                              │
│               │                                                              │
│  Response:    │    1. Copy redirect URL                                      │
│  {            │    ─────────────────────>  Open in browser                   │
│    interact:  │                                                              │
│      redirect:├───────────────────────────────────────────┐                  │
│    continue:  │                            2. Click       │                  │
│      uri:     │                               "Approve"   ▼                  │
│      token:   │                            ┌─────────────────┐               │
│  }            │                            │  Approval Page  │               │
│               │                            │                 │               │
│               │                            │  [  Approve  ]  │               │
│               │                            └────────┬────────┘               │
│               │                                     │                        │
│               │    3. Grant state                   │                        │
│               │       changes to                    │                        │
│               │       "Approved"  <─────────────────┘                        │
│               │                                                              │
│               │                            4. Run continuation               │
│               │                            ─────────────────────>            │
│               │                                                              │
│               │                            POST /continue                    │
│               │                            Authorization: GNAP <token>       │
│               │                                                              │
│               │                            Response:                         │
│               │                            { access_token: { value: "..." }} │
│               │                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### What happens if you skip the approval?

If you run request 8 **before** approving in the browser:
1. The auth server returns a `continue` response (grant is still "Pending")
2. The `accessToken` variable is NOT updated
3. Request 9 uses the old **Quote token** instead of the **Outgoing Payment token**
4. You get: `{"error": {"code": "403", "description": "Insufficient Grant"}}`

### How to fix "Insufficient Grant"

1. Go back to request 7 and check the `interact.redirect` URL
2. Open that URL in your browser
3. Click "Approve"
4. Run request 8 again - verify console says `SUCCESS`
5. Run request 9

---

## Troubleshooting

### "Insufficient Grant" error on Create Outgoing Payment (request 9)

**Error:** `{"error": {"code": "403", "description": "Insufficient Grant"}}`

**Cause:** The access token being used is for a **Quote** grant, not an **Outgoing Payment** grant. This happens when request 8 was run before approving the grant in the browser.

**Solution:**
1. Check the console output from request 8
   - If it says `WARNING: Grant is still pending approval!`, you need to approve first
2. Get the redirect URL from request 7's response (`interact.redirect`)
3. Open the URL in your browser and click "Approve"
4. Run request 8 again - verify console says `SUCCESS: Got access token`
5. Run request 9

**Why this happens:**
- Request 5 (Quote grant) returns an `access_token` → stored in `accessToken` variable
- Request 7 (Outgoing Payment grant) is interactive → returns `continue`, NOT `access_token`
- Request 8 polls the continuation endpoint:
  - If grant is still Pending → returns `continue` (accessToken NOT updated)
  - If grant is Approved → returns `access_token` (accessToken IS updated)
- Request 9 uses whatever is in `accessToken` → wrong token if approval was skipped

### "invalid signature" error
- Ensure Happy Life Bank is using the correct `private-key.pem`
- The key must match the `clientPrivateKey` in the environment

### "wallet address not found" error
- Wait for services to fully initialize after `docker compose up`
- Check that seeding completed: `docker compose logs cloud-nine-mock-ase`

### Quote fails with ILP error
- Ensure peering is configured between Cloud Nine and Happy Life
- Check peer liquidity in the admin UIs

### Continuation request returns "continue" instead of "access_token"
- The grant hasn't been approved yet
- Open the `interact.redirect` URL from request 7 in your browser
- Click "Approve" on the approval page
- Run the continuation request again

---

## Key Specifications

- **Open Payments:** https://openpayments.dev
- **GNAP (RFC 9635):** Grant Negotiation and Authorization Protocol
- **HTTP Signatures (RFC 9421):** HTTP Message Signatures
- **Interledger Protocol:** https://interledger.org
