# WhatsApp Business API — Setup Guide (Meta Cloud API Direct)

## Why Direct Meta Cloud API?
- **Cheapest** — pay only Meta's per-conversation rates, zero monthly fees, zero markup
- **Full customization** — just REST endpoints, no BSP abstractions
- **No vendor lock-in** — your code talks directly to Meta's API

---

## Step 1: Create Facebook Business Account

**Go to:** https://business.facebook.com

1. Click "Create Account"
2. Enter business name (TumaGo), your name, and business email
3. Fill in business details (address, phone, website)
4. Free

---

## Step 2: Create a Meta App & Add WhatsApp

**Go to:** https://developers.facebook.com/apps/

1. Click "Create App"
2. Select "Business" as the app type
3. Name it (e.g., "TumaGo WhatsApp")
4. Link it to your Business Account from Step 1
5. Once created, on the app dashboard click "Add Product" → find **WhatsApp** → click "Set Up"
6. On the left sidebar, click **WhatsApp → Getting Started**
7. It will prompt you to **create a WhatsApp Business Account** — follow the steps
8. This gives you a free test phone number and temporary access token

---

## Step 3: Business Verification

**Go to:** https://business.facebook.com/settings/security/

1. Click "Start Verification"
2. Upload Zimbabwe company registration documents (CR14, CR6, or business registration certificate)
3. Enter company details exactly as they appear on the documents
4. Meta may also ask for a utility bill or bank statement matching the business address
5. Verification takes **2–7 business days**
6. You can develop using the test number while you wait

---

## Step 4: Add Your Phone Number

**Go to:** https://developers.facebook.com/apps/ → Your App → WhatsApp → **Getting Started**

Or: https://business.facebook.com/settings/whatsapp-business-accounts/

### Before you start — prepare the number:

- If this number is currently registered on WhatsApp (personal or WhatsApp Business app), you **must remove it first**:
  1. Open WhatsApp on the phone with that number
  2. Go to **Settings → Account → Delete my account**
  3. Delete the account
  4. Wait **5 minutes** before proceeding
- If the number was never on WhatsApp, you're good to go
- The number must be able to **receive an SMS or a voice call** for verification

### Add the number:

1. Click **"Add phone number"**
2. Enter your **display name** (e.g., "TumaGo Deliveries") — must match or relate to your verified business name
3. Select timezone (GMT+2 for Zimbabwe)
4. Enter the phone number with country code: **+263XXXXXXXXX**
5. Choose verification method: **SMS** or **Voice call**
6. Enter the verification code
7. Once verified, you get a **Phone Number ID** — save this

---

## Step 5: Get Permanent Access Token (System User)

**Go to:** https://business.facebook.com/settings/system-users/

1. Click **"Add"** to create a new system user
2. Name it (e.g., `tumago-api`)
3. Set the role to **Admin**
4. Click on the system user you just created
5. Click **"Add Assets"**:
   - Select **Apps** → find your TumaGo WhatsApp app → toggle **Full Control**
   - Select **WhatsApp Accounts** → find your WhatsApp Business Account → toggle **Full Control**
     - **Note:** WhatsApp Accounts only appears here AFTER you complete Step 2 (creating the WhatsApp Business Account through the app setup)
6. Click **"Generate New Token"**
   - Select your Meta App
   - Check these permissions:
     - `whatsapp_business_management`
     - `whatsapp_business_messaging`
   - Click **Generate Token**
7. **Copy and save this token immediately** — you won't see it again
8. This token **does not expire** (unlike the temporary one from Getting Started which expires in 24h)

---

## Step 6: Get Your IDs

**Go to:** https://developers.facebook.com/apps/ → Your App → WhatsApp → **API Setup**

You need three values for your `.env`:

| Value | Where to find it |
|-------|-----------------|
| `WHATSAPP_ACCESS_TOKEN` | From Step 5 (system user token) |
| `WHATSAPP_PHONE_NUMBER_ID` | API Setup page → "From" section → below the phone number dropdown |
| `WHATSAPP_BUSINESS_ACCOUNT_ID` | WhatsApp Business Account settings (e.g., `961143416579242`) |

Add to your `.env`:
```
WHATSAPP_ACCESS_TOKEN=EAAxxxxxxxxxxxxxxxx
WHATSAPP_PHONE_NUMBER_ID=1234567890123456
WHATSAPP_BUSINESS_ACCOUNT_ID=961143416579242
WHATSAPP_VERIFY_TOKEN=tumago_wh_verify_2026
WHATSAPP_API_VERSION=v21.0
```

---

## Step 7: Configure Webhook (Receive Incoming Messages)

**Go to:** https://developers.facebook.com/apps/ → Your App → WhatsApp → **Configuration**

### Prerequisites:
- Your server must have an HTTPS endpoint. For testing without a domain, use **ngrok**:
  ```bash
  # Install ngrok on EC2
  sudo snap install ngrok

  # Auth (sign up free at https://dashboard.ngrok.com)
  ngrok config add-authtoken YOUR_AUTH_TOKEN

  # Tunnel to your gateway
  ngrok http 80

  # For a persistent URL, claim a free static domain at:
  # https://dashboard.ngrok.com/domains
  # Then run:
  ngrok http 80 --url=your-chosen-name.ngrok-free.app
  ```
- The Go gateway already has the webhook handler at `/whatsapp/webhook` — it echoes Meta's verification challenge and acknowledges incoming messages.

### Configure:

1. Under **Webhook**, click **"Edit"**
2. Enter:
   - **Callback URL:** `https://your-ngrok-url.ngrok-free.app/whatsapp/webhook`
   - **Verify token:** `tumago_wh_verify_2026` (must match `WHATSAPP_VERIFY_TOKEN` in your `.env`)
3. Click **"Verify and Save"** — Meta sends a GET request, your gateway echoes back the challenge
4. After verification, click **"Manage"** and subscribe to:
   - `messages` — incoming messages, button replies, delivery receipts
   - `message_template_status_update` — notifies when templates are approved/rejected

---

## Step 8: Submit Message Templates

**Go to:** https://business.facebook.com/wa/manage/message-templates/

Templates needed for TumaGo:

| Template Name | Category | Body | Buttons |
|---------------|----------|------|---------|
| `delivery_otp` | Authentication | "Your TumaGo code is {{1}}" | None |
| `delivery_created` | Utility | "Delivery #{{1}} created. Finding a driver..." | None |
| `driver_found` | Utility | "Driver {{1}} accepted your delivery. ETA: {{2}} min" | None |
| `driver_en_route` | Utility | "Your driver is on the way to pickup" | None |
| `delivery_complete` | Utility | "Package delivered! Rate your experience." | None |
| `no_driver_found` | Utility | "No drivers available right now. We'll keep trying." | None |
| `payment_request` | Utility | "Your TumaGo delivery total is ${{1}}. Tap below to pay." | CTA → URL → `{{1}}` (Paynow link) |
| `driver_new_delivery` | Utility | "New delivery!\nPickup: {{1}}\nDrop-off: {{2}}\nPayment: ${{3}}" | Quick Reply → "Accept" / "Decline" |

### How to create:
1. Click **"Create Template"**
2. Fill in name, category, language (English), body with `{{1}}` variables
3. Add buttons if needed
4. Submit — Meta reviews within **24–48 hours**
5. If rejected, Meta tells you why — fix and resubmit

### Tips:
- Don't include promotional language in Utility templates
- Match the category correctly
- Don't use URL shorteners in templates

---

## Step 9: Go Live

**Go to:** https://developers.facebook.com/apps/ → Your App → top toggle

### Checklist:
- [ ] Business verification approved (Step 3)
- [ ] Phone number verified (Step 4)
- [ ] System user token generated (Step 5)
- [ ] Webhook configured and receiving (Step 7)
- [ ] At least 1 message template approved (Step 8)

### Switch to live:
1. App dashboard → top toggle says "Development" → switch to **"Live"**
2. In Development mode, you can only message numbers added as testers
3. In Live mode, you can message anyone

---

## Step 10: Deploy to EC2

```bash
# SSH into EC2
ssh ubuntu@13.247.19.74

# Pull latest code
cd ~/TumaGo/backend
git pull

# Make sure .env has all WHATSAPP_* vars
# Then rebuild gateway
docker compose up -d --build gateway
```

---

## Quick Test

Send a test message via curl:

```bash
curl -X POST "https://graph.facebook.com/v21.0/YOUR_PHONE_NUMBER_ID/messages" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "messaging_product": "whatsapp",
    "to": "263XXXXXXXXX",
    "type": "text",
    "text": { "body": "Hello from TumaGo!" }
  }'
```

If you get a `200` response with a message ID, your setup is working.

---

## Architecture

```
Meta WhatsApp → ngrok (HTTPS) → EC2 (port 80) → nginx → Go Gateway → /whatsapp/webhook
                                                                        │
                                                                        ▼ (future)
                                                                  WhatsApp FastAPI Service
```

The Go gateway currently handles webhook verification and acknowledges incoming messages. Once the WhatsApp FastAPI service is built, the gateway will proxy `/whatsapp/` requests to it.

---

## Cost (Sub-Saharan Africa Rates)

| Category | Per 24h conversation |
|----------|---------------------|
| Service (user-initiated) | 1,000 free/month, then ~$0.004 |
| Utility (delivery updates) | ~$0.008 |
| Authentication (OTP) | ~$0.009 |
| Marketing (promos) | ~$0.023 |

Estimated cost at 100 deliveries/day: **~$15–30/month**

---

## Quick Reference Links

| What | Link |
|------|------|
| Business Manager | https://business.facebook.com |
| Developer Portal | https://developers.facebook.com/apps/ |
| System Users (tokens) | https://business.facebook.com/settings/system-users/ |
| Message Templates | https://business.facebook.com/wa/manage/message-templates/ |
| WhatsApp API Docs | https://developers.facebook.com/docs/whatsapp/cloud-api |
| Pricing | https://developers.facebook.com/docs/whatsapp/pricing |
| Webhook Reference | https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks |
| ngrok Dashboard | https://dashboard.ngrok.com |
| ngrok Static Domains | https://dashboard.ngrok.com/domains |
