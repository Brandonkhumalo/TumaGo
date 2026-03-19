# WhatsApp Message Templates — TumaGo

## How to Create Templates

**Go to:** https://business.facebook.com/wa/manage/message-templates/

1. Select your WhatsApp Business Account
2. Click **"Create Template"**
3. Fill in the fields exactly as shown below for each template
4. Click **Submit**
5. Wait 24–48 hours for approval

### Rules
- Variables use `{{1}}`, `{{2}}`, etc. — numbered sequentially
- Max 3 Quick Reply buttons per message
- Max 2 Call to Action buttons (1 URL + 1 phone number)
- Don't use URL shorteners (bit.ly, etc.)
- Don't put promotional language in Utility templates
- Include sample values for each variable when submitting
- If rejected, Meta tells you why — fix and resubmit

---

## Submit Order (most critical first)

**Batch 1 — Go live:**
1–7 (authentication + core delivery + payment request)

**Batch 2 — Full payment & delivery flow:**
8–18 (payment instructions, confirmations, driver found, delivery updates, history)

**Batch 3 — Driver delivery flow:**
19–26 (driver accept/decline, navigation, confirm, online/offline, earnings)

**Batch 4 — Registration & onboarding:**
27–39 (client + driver registration, vehicle selection, document uploads, approval)

**Batch 5 — Account management & marketing:**
40–42

---

## AUTHENTICATION TEMPLATES

---

### 1. client_otp

- **Name:** `client_otp`
- **Category:** Authentication
- **Language:** English
- **Body:** `Your TumaGo verification code is {{1}}. Expires in 5 minutes.`
- **Buttons:** None
- **Sample:** `{{1}}` = `482901`

---

### 2. driver_otp

- **Name:** `driver_otp`
- **Category:** Authentication
- **Language:** English
- **Body:** `Your TumaGo driver verification code is {{1}}. Expires in 5 minutes.`
- **Buttons:** None
- **Sample:** `{{1}}` = `739205`

---

### 3. password_reset

- **Name:** `password_reset`
- **Category:** Authentication
- **Language:** English
- **Body:** `Your TumaGo password reset code is {{1}}. Expires in 10 minutes. If you didn't request this, ignore this message.`
- **Buttons:** None
- **Sample:** `{{1}}` = `561038`

---

## CLIENT DELIVERY FLOW TEMPLATES

---

### 4. delivery_vehicle_options

- **Name:** `delivery_vehicle_options`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Choose your delivery vehicle for pickup at {{1}} to {{2}}:

🛵 Scooter — ${{3}}
🚐 Van — ${{4}}
🚛 Truck — ${{5}}

Reply with the vehicle name to continue.
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Quick Reply → `Scooter` / `Van` / `Truck`
- **Samples:** `{{1}}` = `123 Samora Machel Ave` | `{{2}}` = `45 Robert Mugabe Rd` | `{{3}}` = `2.50` | `{{4}}` = `5.00` | `{{5}}` = `8.00`

---

### 5. delivery_payment_options

- **Name:** `delivery_payment_options`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Delivery #{{1}}
Vehicle: {{2}}
Total: ${{3}}

How would you like to pay?
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Quick Reply → `EcoCash` / `OneMoney` / `Cash`
- **Samples:** `{{1}}` = `TG-20260319-001` | `{{2}}` = `Scooter` | `{{3}}` = `2.50`

---

### 6. delivery_created

- **Name:** `delivery_created`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Delivery #{{1}} created!
Pickup: {{2}}
Drop-off: {{3}}
Vehicle: {{4}}
Cost: ${{5}}
Payment: {{6}}

Finding a driver for you...
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** `{{1}}` = `TG-20260319-001` | `{{2}}` = `123 Samora Machel Ave, Harare` | `{{3}}` = `45 Robert Mugabe Rd, Harare` | `{{4}}` = `Scooter` | `{{5}}` = `2.50` | `{{6}}` = `EcoCash`

---

### 7. payment_request

- **Name:** `payment_request`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Delivery #{{1}} — Payment Required

Amount: ${{2}}
Vehicle: {{3}}
Pickup: {{4}}
Drop-off: {{5}}

Tap "Pay Now" to pay securely via Paynow (EcoCash, OneMoney, or Visa/Mastercard).

After payment, you will be redirected back to WhatsApp automatically. Do NOT close the payment page until it says "Payment Successful".
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Call to Action → URL → `{{1}}` (label: `Pay Now`)
- **Samples:** Body `{{1}}` = `TG-20260319-001` | Body `{{2}}` = `2.50` | Body `{{3}}` = `Scooter` | Body `{{4}}` = `123 Samora Machel Ave, Harare` | Body `{{5}}` = `45 Robert Mugabe Rd, Harare` | Button `{{1}}` = `https://www.paynow.co.zw/payment/pay/abc123`

---

### 8. payment_ecocash_instructions

- **Name:** `payment_ecocash_instructions`
- **Category:** Utility
- **Language:** English
- **Body:**
```
EcoCash Payment for Delivery #{{1}}

Amount: ${{2}}

Tap "Pay Now" below. You will be taken to the Paynow payment page.

Steps:
1. Tap "Pay Now"
2. Select EcoCash on the Paynow page
3. Enter your EcoCash number (07XXXXXXXX)
4. You'll receive an EcoCash prompt on your phone
5. Enter your EcoCash PIN to confirm
6. Wait for the "Payment Successful" message
7. You'll be redirected back to WhatsApp

We'll send you a confirmation once payment is received.
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Call to Action → URL → `{{1}}` (label: `Pay Now`)
- **Samples:** Body `{{1}}` = `TG-20260319-001` | Body `{{2}}` = `2.50` | Button `{{1}}` = `https://www.paynow.co.zw/payment/pay/abc123`

---

### 9. payment_onemoney_instructions

- **Name:** `payment_onemoney_instructions`
- **Category:** Utility
- **Language:** English
- **Body:**
```
OneMoney Payment for Delivery #{{1}}

Amount: ${{2}}

Tap "Pay Now" below. You will be taken to the Paynow payment page.

Steps:
1. Tap "Pay Now"
2. Select OneMoney on the Paynow page
3. Enter your OneMoney number (071XXXXXXX)
4. You'll receive a OneMoney prompt on your phone
5. Enter your OneMoney PIN to confirm
6. Wait for the "Payment Successful" message
7. You'll be redirected back to WhatsApp

We'll send you a confirmation once payment is received.
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Call to Action → URL → `{{1}}` (label: `Pay Now`)
- **Samples:** Body `{{1}}` = `TG-20260319-001` | Body `{{2}}` = `2.50` | Button `{{1}}` = `https://www.paynow.co.zw/payment/pay/abc123`

---

### 10. payment_confirmed

- **Name:** `payment_confirmed`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Payment Successful!

Delivery #{{1}}
Amount paid: ${{2}}
Method: {{3}}
Reference: {{4}}

Finding a driver for you now. You'll be notified when a driver accepts your delivery.
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** `{{1}}` = `TG-20260319-001` | `{{2}}` = `2.50` | `{{3}}` = `EcoCash` | `{{4}}` = `PNW-20260319-78432`

---

### 11. payment_failed

- **Name:** `payment_failed`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Payment was not completed for delivery #{{1}}.

Amount: ${{2}}
Reason: {{3}}

This can happen if:
- You cancelled the payment
- Insufficient funds in your account
- The payment timed out

Tap "Retry Payment" to try again. You'll be taken back to the Paynow payment page.
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Call to Action → URL → `{{1}}` (label: `Retry Payment`)
- **Samples:** Body `{{1}}` = `TG-20260319-001` | Body `{{2}}` = `2.50` | Body `{{3}}` = `Payment timed out` | Button `{{1}}` = `https://www.paynow.co.zw/payment/pay/abc123`

---

### 12. driver_found

- **Name:** `driver_found`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Driver found for delivery #{{1}}!
Driver: {{2}}
Vehicle: {{3}} — {{4}} ({{5}})
Number plate: {{6}}
ETA to pickup: {{7}} min
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Call to Action → Phone → `{{1}}` (label: `Call Driver`)
- **Samples:** `{{1}}` = `TG-20260319-001` | `{{2}}` = `Tatenda Moyo` | `{{3}}` = `Scooter` | `{{4}}` = `Honda PCX` | `{{5}}` = `Red` | `{{6}}` = `ABG 1234` | `{{7}}` = `8` | Button `{{1}}` = `+263771234567`

---

### 13. no_driver_found

- **Name:** `no_driver_found`
- **Category:** Utility
- **Language:** English
- **Body:** `No drivers are available for delivery #{{1}} right now. We'll keep trying and notify you when one accepts.`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Quick Reply → `Keep Waiting` / `Cancel Delivery`
- **Samples:** `{{1}}` = `TG-20260319-001`

---

### 14. driver_at_pickup

- **Name:** `driver_at_pickup`
- **Category:** Utility
- **Language:** English
- **Body:** `Driver {{1}} has arrived at the pickup location for delivery #{{2}}. Please hand over your package.`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** `{{1}}` = `Tatenda M.` | `{{2}}` = `TG-20260319-001`

---

### 15. driver_en_route

- **Name:** `driver_en_route`
- **Category:** Utility
- **Language:** English
- **Body:** `Your package for delivery #{{1}} has been picked up. Driver {{2}} is on the way to the drop-off location.`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** `{{1}}` = `TG-20260319-001` | `{{2}}` = `Tatenda M.`

---

### 16. delivery_complete

- **Name:** `delivery_complete`
- **Category:** Utility
- **Language:** English
- **Body:** `Delivery #{{1}} is complete! Your package has been delivered by {{2}}. How was your experience?`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Quick Reply → `Excellent` / `Good` / `Report Issue`
- **Samples:** `{{1}}` = `TG-20260319-001` | `{{2}}` = `Tatenda M.`

---

### 17. delivery_cancelled

- **Name:** `delivery_cancelled`
- **Category:** Utility
- **Language:** English
- **Body:** `Delivery #{{1}} has been cancelled. Reason: {{2}}. Any payment will be refunded within 24 hours.`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** `{{1}}` = `TG-20260319-001` | `{{2}}` = `No drivers available`

---

### 18. delivery_history

- **Name:** `delivery_history`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Your recent deliveries:

1. #{{1}} — {{2}} — ${{3}} — {{4}}
2. #{{5}} — {{6}} — ${{7}} — {{8}}
3. #{{9}} — {{10}} — ${{11}} — {{12}}
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Quick Reply → `More History` / `New Delivery`
- **Samples:** `{{1}}` = `TG-001` | `{{2}}` = `Completed` | `{{3}}` = `2.50` | `{{4}}` = `18 Mar` | etc.

---

## DRIVER DELIVERY FLOW TEMPLATES

---

### 19. driver_new_delivery

- **Name:** `driver_new_delivery`
- **Category:** Utility
- **Language:** English
- **Body:**
```
New delivery request!
Pickup: {{1}}
Drop-off: {{2}}
Distance: {{3}} km
Your earnings: ${{4}}
Package: {{5}}
Payment method: {{6}}

Do you accept?
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Quick Reply → `Accept` / `Decline`
- **Samples:** `{{1}}` = `123 Samora Machel Ave, Harare` | `{{2}}` = `45 Robert Mugabe Rd, Harare` | `{{3}}` = `5.2` | `{{4}}` = `2.50` | `{{5}}` = `Small parcel` | `{{6}}` = `EcoCash`

---

### 20. driver_delivery_confirmed

- **Name:** `driver_delivery_confirmed`
- **Category:** Utility
- **Language:** English
- **Body:**
```
You accepted delivery #{{1}}.
Pickup: {{2}}
Sender: {{3}}
Navigate to pickup now.
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Call to Action → URL → `{{1}}` (label: `Navigate to Pickup`) + Phone → `{{2}}` (label: `Call Sender`)
- **Samples:** Body `{{1}}` = `TG-20260319-001` | Body `{{2}}` = `123 Samora Machel Ave, Harare` | Body `{{3}}` = `John Moyo` | Button URL `{{1}}` = `https://www.google.com/maps/dir/?api=1&destination=-17.8292,31.0522` | Button Phone `{{2}}` = `+263771234567`

---

### 21. driver_navigate_dropoff

- **Name:** `driver_navigate_dropoff`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Package picked up for delivery #{{1}}.
Drop-off: {{2}}
Recipient: {{3}}
Navigate to drop-off now.
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Call to Action → URL → `{{1}}` (label: `Navigate to Drop-off`)
- **Samples:** Body `{{1}}` = `TG-20260319-001` | Body `{{2}}` = `45 Robert Mugabe Rd, Harare` | Body `{{3}}` = `Jane Dube` | Button URL `{{1}}` = `https://www.google.com/maps/dir/?api=1&destination=-17.8310,31.0455`

---

### 22. driver_confirm_delivery

- **Name:** `driver_confirm_delivery`
- **Category:** Utility
- **Language:** English
- **Body:** `Have you delivered the package for delivery #{{1}} to {{2}}?`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Quick Reply → `Delivered` / `Problem`
- **Samples:** `{{1}}` = `TG-20260319-001` | `{{2}}` = `45 Robert Mugabe Rd, Harare`

---

### 23. driver_delivery_complete_confirm

- **Name:** `driver_delivery_complete_confirm`
- **Category:** Utility
- **Language:** English
- **Body:** `Delivery #{{1}} marked as complete. ${{2}} has been added to your earnings. Today's total: ${{3}}.`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** `{{1}}` = `TG-20260319-001` | `{{2}}` = `2.50` | `{{3}}` = `17.50`

---

### 24. driver_go_online

- **Name:** `driver_go_online`
- **Category:** Utility
- **Language:** English
- **Body:** `You are now online and available for deliveries. We'll send you requests as they come in. Reply "offline" to stop receiving requests.`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** None

---

### 25. driver_go_offline

- **Name:** `driver_go_offline`
- **Category:** Utility
- **Language:** English
- **Body:** `You are now offline. You won't receive delivery requests. Reply "online" to start receiving requests again.`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** None

---

### 26. driver_earnings_summary

- **Name:** `driver_earnings_summary`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Daily summary for {{1}}:
Deliveries completed: {{2}}
Total earned: ${{3}}
Charges: ${{4}}
Profit: ${{5}}
Rating: {{6}}/5
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** `{{1}}` = `19 Mar 2026` | `{{2}}` = `7` | `{{3}}` = `24.50` | `{{4}}` = `2.45` | `{{5}}` = `22.05` | `{{6}}` = `4.8`

---

## CLIENT REGISTRATION & ONBOARDING TEMPLATES

---

### 27. client_welcome

- **Name:** `client_welcome`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Welcome to TumaGo, {{1}}!

To get started, we need a few details. Please reply with your:

1. Full name
2. Surname
3. Email address
4. Phone number
5. Street address
6. City
7. Province
8. Postal code

Send them in that order, separated by commas.

Example: John, Moyo, john@email.com, 0771234567, 123 Main St, Harare, Harare, 00263
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** `{{1}}` = `John`

---

### 28. client_registration_complete

- **Name:** `client_registration_complete`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Your TumaGo account is set up, {{1}}!

You can now request deliveries via WhatsApp. Just send "deliver" to start.

Or download our app for the full experience:
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Call to Action → URL → `https://play.google.com/store/apps/details?id=com.techmania.tumago` (label: `Download App`)
- **Samples:** `{{1}}` = `John`

---

### 29. client_request_delivery

- **Name:** `client_request_delivery`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Let's set up your delivery!

Please send the following details:

1. Pickup address
2. Drop-off address
3. Package description (what are you sending?)

Send them separated by commas.

Example: 123 Samora Machel Ave Harare, 45 Robert Mugabe Rd Harare, Small box with documents
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** None

---

## DRIVER REGISTRATION & ONBOARDING TEMPLATES

---

### 30. driver_registration_start

- **Name:** `driver_registration_start`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Welcome to TumaGo Driver!

To register as a driver, we need your personal details first. Please reply with:

1. Full name
2. Surname
3. Email address
4. Phone number
5. ID number
6. Street address
7. City
8. Province
9. Postal code

Send them in that order, separated by commas.

Example: Tatenda, Moyo, tatenda@email.com, 0771234567, 63-123456-A-42, 10 Kwame Nkrumah Ave, Harare, Harare, 00263
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** None

---

### 31. driver_vehicle_details

- **Name:** `driver_vehicle_details`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Thanks {{1}}! Now we need your vehicle details.

First, what type of delivery vehicle do you have?
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Quick Reply → `Scooter` / `Van` / `Truck`
- **Samples:** `{{1}}` = `Tatenda`

---

### 32. driver_vehicle_info

- **Name:** `driver_vehicle_info`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Great, you selected {{1}}.

Now send your vehicle details:

1. Vehicle name (e.g., Honda PCX, Toyota HiAce)
2. Number plate
3. Color
4. Vehicle model/year

Send them separated by commas.

Example: Honda PCX, ABG 1234, Red, 2022
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** `{{1}}` = `Scooter`

---

### 33. driver_upload_id

- **Name:** `driver_upload_id`
- **Category:** Utility
- **Language:** English
- **Body:** `Almost done! Please send a clear photo of your national ID (front side). Make sure all text is readable.`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** None

---

### 34. driver_upload_license

- **Name:** `driver_upload_license`
- **Category:** Utility
- **Language:** English
- **Body:** `Now send a clear photo of your driver's license (front side). Make sure all text is readable.`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** None

---

### 35. driver_upload_profile

- **Name:** `driver_upload_profile`
- **Category:** Utility
- **Language:** English
- **Body:** `Last step! Send a clear photo of yourself for your driver profile picture. This will be shown to clients.`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** None

---

### 36. driver_registration_complete

- **Name:** `driver_registration_complete`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Your driver registration is complete, {{1}}!

Your application is now under review. We'll notify you once approved (usually within 24-48 hours).

While you wait, download the driver app:
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Call to Action → URL → `https://play.google.com/store/apps/details?id=com.techmania.tumago_driver` (label: `Download Driver App`)
- **Samples:** `{{1}}` = `Tatenda`

---

### 37. driver_approved

- **Name:** `driver_approved`
- **Category:** Utility
- **Language:** English
- **Body:**
```
Congratulations {{1}}! Your driver account has been approved.

You can now receive delivery requests. Reply "online" to start receiving deliveries, or use the driver app.

Vehicle: {{2}} — {{3}} ({{4}})
Plate: {{5}}
```
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Quick Reply → `Go Online`
- **Samples:** `{{1}}` = `Tatenda Moyo` | `{{2}}` = `Scooter` | `{{3}}` = `Honda PCX` | `{{4}}` = `Red` | `{{5}}` = `ABG 1234`

---

### 38. driver_rejected

- **Name:** `driver_rejected`
- **Category:** Utility
- **Language:** English
- **Body:** `Hi {{1}}, your driver application was not approved. Reason: {{2}}. You can reapply after addressing the issue by sending "register driver".`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** None
- **Samples:** `{{1}}` = `Tatenda` | `{{2}}` = `Driver's license photo is not readable`

---

### 39. driver_docs_reminder

- **Name:** `driver_docs_reminder`
- **Category:** Utility
- **Language:** English
- **Body:** `Hi {{1}}, your driver registration is incomplete. You still need to submit: {{2}}. Reply "continue" to pick up where you left off.`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Quick Reply → `Continue Registration`
- **Samples:** `{{1}}` = `Tatenda` | `{{2}}` = `driver's license photo, profile picture`

---

## ACCOUNT MANAGEMENT TEMPLATES

---

### 40. account_deactivated

- **Name:** `account_deactivated`
- **Category:** Utility
- **Language:** English
- **Body:** `Your TumaGo account has been deactivated. If this was a mistake, reply "help" to contact support.`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Quick Reply → `Contact Support`
- **Samples:** None

---

### 41. driver_balance_reminder

- **Name:** `driver_balance_reminder`
- **Category:** Utility
- **Language:** English
- **Body:** `Hi {{1}}, you have an outstanding balance of ${{2}} with TumaGo. Please make a payment to continue receiving delivery requests.`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Call to Action → URL → `{{1}}` (label: `Pay Now`)
- **Samples:** Body `{{1}}` = `Tatenda` | Body `{{2}}` = `15.00` | Button `{{1}}` = `https://www.paynow.co.zw/payment/pay/xyz789`

---

### 42. promo_offer

- **Name:** `promo_offer`
- **Category:** Marketing
- **Language:** English
- **Body:** `Hi {{1}}! Get {{2}}% off your next TumaGo delivery. Use code: {{3}}. Valid until {{4}}.`
- **Footer:** `TumaGo Deliveries`
- **Buttons:** Quick Reply → `Send a Delivery`
- **Samples:** `{{1}}` = `John` | `{{2}}` = `20` | `{{3}}` = `TUMA20` | `{{4}}` = `31 March 2026`

---

## TEMPLATE COUNT

| Category | Count |
|----------|-------|
| Authentication | 3 |
| Utility — Client delivery flow (inc. payment) | 15 |
| Utility — Driver delivery flow | 8 |
| Utility — Client registration | 3 |
| Utility — Driver registration | 10 |
| Utility — Account management | 2 |
| Marketing | 1 |
| **Total** | **42** |

---

## API USAGE EXAMPLES

### Sending a template with body variables only

```bash
curl -X POST "https://graph.facebook.com/v21.0/YOUR_PHONE_NUMBER_ID/messages" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "messaging_product": "whatsapp",
    "to": "263XXXXXXXXX",
    "type": "template",
    "template": {
      "name": "delivery_created",
      "language": { "code": "en" },
      "components": [
        {
          "type": "body",
          "parameters": [
            { "type": "text", "text": "TG-20260319-001" },
            { "type": "text", "text": "123 Samora Machel Ave, Harare" },
            { "type": "text", "text": "45 Robert Mugabe Rd, Harare" },
            { "type": "text", "text": "Scooter" },
            { "type": "text", "text": "2.50" },
            { "type": "text", "text": "EcoCash" }
          ]
        }
      ]
    }
  }'
```

### Sending a template with URL button

```bash
curl -X POST "https://graph.facebook.com/v21.0/YOUR_PHONE_NUMBER_ID/messages" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "messaging_product": "whatsapp",
    "to": "263XXXXXXXXX",
    "type": "template",
    "template": {
      "name": "payment_request",
      "language": { "code": "en" },
      "components": [
        {
          "type": "body",
          "parameters": [
            { "type": "text", "text": "TG-20260319-001" },
            { "type": "text", "text": "2.50" }
          ]
        },
        {
          "type": "button",
          "sub_type": "url",
          "index": "0",
          "parameters": [
            { "type": "text", "text": "https://www.paynow.co.zw/payment/pay/abc123" }
          ]
        }
      ]
    }
  }'
```

### Sending a template with phone button

```bash
curl -X POST "https://graph.facebook.com/v21.0/YOUR_PHONE_NUMBER_ID/messages" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "messaging_product": "whatsapp",
    "to": "263XXXXXXXXX",
    "type": "template",
    "template": {
      "name": "driver_found",
      "language": { "code": "en" },
      "components": [
        {
          "type": "body",
          "parameters": [
            { "type": "text", "text": "TG-20260319-001" },
            { "type": "text", "text": "Tatenda Moyo" },
            { "type": "text", "text": "Scooter" },
            { "type": "text", "text": "Honda PCX" },
            { "type": "text", "text": "Red" },
            { "type": "text", "text": "ABG 1234" },
            { "type": "text", "text": "8" }
          ]
        },
        {
          "type": "button",
          "sub_type": "phone_number",
          "index": "0",
          "parameters": [
            { "type": "text", "text": "+263771234567" }
          ]
        }
      ]
    }
  }'
```

### Quick Reply buttons — no parameters needed

Quick Reply buttons are static (defined in the template). When a user taps one, the button text comes through your webhook as a `button.text` payload. You don't send button parameters for Quick Reply templates — only body parameters.

```bash
curl -X POST "https://graph.facebook.com/v21.0/YOUR_PHONE_NUMBER_ID/messages" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "messaging_product": "whatsapp",
    "to": "263XXXXXXXXX",
    "type": "template",
    "template": {
      "name": "driver_new_delivery",
      "language": { "code": "en" },
      "components": [
        {
          "type": "body",
          "parameters": [
            { "type": "text", "text": "123 Samora Machel Ave, Harare" },
            { "type": "text", "text": "45 Robert Mugabe Rd, Harare" },
            { "type": "text", "text": "5.2" },
            { "type": "text", "text": "2.50" },
            { "type": "text", "text": "Small parcel" },
            { "type": "text", "text": "EcoCash" }
          ]
        }
      ]
    }
  }'
```
