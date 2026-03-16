# AWS SES (Simple Email Service) — Getting Started

## Step 1: Request Production Access (Exit Sandbox)

In sandbox mode, you can only send to **verified** email addresses. To send to anyone:

1. Go to **AWS Console → SES → Account Dashboard**
2. Click **"Request Production Access"**
3. Fill out the form:
   - **Mail type**: Transactional (for delivery notifications, OTPs, etc.)
   - **Website URL**: Your app/website URL
   - **Use case description**: Be specific. Example:

> "TumaGo is a package delivery platform. We send transactional emails: delivery confirmations, OTP verification codes, password resets, and delivery status updates to our registered users. Users explicitly sign up and provide their email. We estimate ~500 emails/day initially. We will implement bounce/complaint handling via SNS notifications."

4. AWS typically responds within **24 hours**

**Tips to get approved faster:**
- Mention you'll handle **bounces and complaints** (via SNS)
- Mention you have an **unsubscribe mechanism** (even if transactional-only, mention it)
- Be specific about email types and volume estimates
- Having a real domain (not gmail) helps

---

## Step 2: Verify Your Sending Domain

1. **SES → Verified Identities → Create Identity → Domain**
2. Enter your domain (e.g., `tumago.co.zw`)
3. AWS gives you **DNS records** to add:
   - **DKIM** (3 CNAME records) — proves emails are from you
   - **SPF** — already handled if using SES
   - **DMARC** (optional but recommended) — add a TXT record:
     ```
     _dmarc.tumago.co.zw  TXT  "v=DMARC1; p=quarantine; rua=mailto:dmarc@tumago.co.zw"
     ```
4. Add these DNS records at your domain registrar
5. Verification takes **~72 hours** (usually faster)

---

## Step 3: Get Your Credentials

You need an **IAM user** with SES permissions:

1. **IAM → Users → Create User**
2. Attach policy: `AmazonSESFullAccess` (or a custom policy for least privilege)
3. Create **Access Keys** (for programmatic access)
4. Save these — you'll use them as environment variables:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `AWS_SES_REGION` (e.g., `eu-west-1` — pick the closest SES region to Zimbabwe; EU regions are closest)

**Available SES regions** (pick one with lowest latency from Zimbabwe):
- `eu-west-1` (Ireland) — recommended
- `eu-central-1` (Frankfurt)
- `af-south-1` (Cape Town) — closest, but check if SES is available there

---

## Step 4: Set Up Bounce/Complaint Handling

AWS requires this and it protects your sending reputation:

1. **SNS → Create Topic** (e.g., `ses-bounces`, `ses-complaints`)
2. **SES → Verified Identity → Notifications tab**
3. Link bounce/complaint topics to your SNS topics
4. Subscribe an endpoint (your backend) to process these

---

## What You'll Need Before Starting Development

| Item | Status |
|------|--------|
| AWS account | You likely have this |
| Domain you control | Need DNS access to add DKIM/DMARC records |
| IAM credentials with SES access | Create in IAM console |
| Production access approved | Submit request (24h wait) |
| Domain verified in SES | Add DNS records (up to 72h) |
| SES region chosen | `eu-west-1` or `af-south-1` |

---

## Development Plan for TumaGo

Once approved, add email sending to the **notification service** (`backend/notification/`). It already handles FCM — add SES alongside it using `boto3`:

```
POST /send-email  →  SES API  →  user's inbox
```

Email types needed:
- Email verification / OTP
- Password reset
- Delivery confirmation
- Delivery status updates
- Driver approval notifications
