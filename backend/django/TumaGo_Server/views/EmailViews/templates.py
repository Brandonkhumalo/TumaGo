"""
Email HTML templates for TumaGo transactional emails.
Brand colors: primary #00a4e4, dark #0e74bc, page_bg #EFF8FE
"""


def _base_wrapper(content: str) -> str:
    """Wrap content in the standard TumaGo email layout."""
    return f"""
    <div style="font-family: Arial, sans-serif; max-width: 520px; margin: 0 auto; padding: 24px; background: #ffffff;">
        <div style="text-align: center; margin-bottom: 24px;">
            <h1 style="color: #0e74bc; margin: 0; font-size: 28px;">TumaGo</h1>
            <p style="color: #999; font-size: 12px; margin: 4px 0 0;">Last-mile delivery, simplified</p>
        </div>
        {content}
        <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;" />
        <p style="color: #999; font-size: 12px; text-align: center;">
            &copy; TumaGo — Harare, Zimbabwe<br/>
            You received this email because you have a TumaGo account.
        </p>
    </div>
    """


def welcome_client_email(name: str) -> tuple[str, str, str]:
    """Welcome email for new client accounts. Returns (subject, text, html)."""
    subject = "Welcome to TumaGo!"
    text = (
        f"Hi {name},\n\n"
        "Welcome to TumaGo! Your account has been created successfully.\n\n"
        "You can now request deliveries anytime from the TumaGo app. "
        "Our drivers are ready to pick up and deliver your packages across the city.\n\n"
        "Happy sending!\n"
        "— The TumaGo Team"
    )
    html = _base_wrapper(f"""
        <h2 style="color: #333;">Welcome, {name}!</h2>
        <p>Your account has been created successfully.</p>
        <p>You can now request deliveries anytime from the TumaGo app.
        Our drivers are ready to pick up and deliver your packages across the city.</p>
        <div style="background: #EFF8FE; padding: 16px; border-radius: 8px; margin: 16px 0; text-align: center;">
            <p style="margin: 0; color: #0e74bc; font-weight: bold;">Open the TumaGo app to get started</p>
        </div>
        <p>Happy sending!<br/><strong>— The TumaGo Team</strong></p>
    """)
    return subject, text, html


def welcome_driver_email(name: str) -> tuple[str, str, str]:
    """Welcome email for new driver accounts. Returns (subject, text, html)."""
    subject = "Welcome to TumaGo, Driver!"
    text = (
        f"Hi {name},\n\n"
        "Welcome to TumaGo! Your driver account has been created.\n\n"
        "Once your account is approved, you'll start receiving delivery requests "
        "in the TumaGo Driver app. Make sure your vehicle info and license are up to date.\n\n"
        "Drive safe!\n"
        "— The TumaGo Team"
    )
    html = _base_wrapper(f"""
        <h2 style="color: #333;">Welcome, {name}!</h2>
        <p>Your driver account has been created.</p>
        <p>Once your account is approved, you'll start receiving delivery requests
        in the TumaGo Driver app. Make sure your vehicle info and license are up to date.</p>
        <div style="background: #EFF8FE; padding: 16px; border-radius: 8px; margin: 16px 0; text-align: center;">
            <p style="margin: 0; color: #0e74bc; font-weight: bold;">Complete your profile in the Driver app</p>
        </div>
        <p>Drive safe!<br/><strong>— The TumaGo Team</strong></p>
    """)
    return subject, text, html


def welcome_partner_email(company_name: str) -> tuple[str, str, str]:
    """Welcome email for new partner registrations. Returns (subject, text, html)."""
    subject = "Welcome to TumaGo Partner Program!"
    text = (
        f"Hi {company_name},\n\n"
        "Your partner account has been registered on TumaGo.\n\n"
        "To activate your account, please pay the $15 setup fee from the Partner Dashboard. "
        "Once payment is confirmed, your API credentials will be generated and you can "
        "start integrating deliveries into your platform.\n\n"
        "— The TumaGo Team"
    )
    html = _base_wrapper(f"""
        <h2 style="color: #333;">Welcome, {company_name}!</h2>
        <p>Your partner account has been registered on TumaGo.</p>
        <p>To activate your account, please pay the <strong>$15 setup fee</strong> from the Partner Dashboard.
        Once payment is confirmed, your API credentials will be generated and you can start
        integrating deliveries into your platform.</p>
        <div style="background: #EFF8FE; padding: 16px; border-radius: 8px; margin: 16px 0; text-align: center;">
            <p style="margin: 0; color: #0e74bc; font-weight: bold;">Log in to the Partner Dashboard to pay &amp; activate</p>
        </div>
        <p><strong>— The TumaGo Team</strong></p>
    """)
    return subject, text, html


def delivery_completed_email(
    client_name: str,
    driver_name: str,
    delivery_id: str,
    fare: str,
    vehicle: str,
    payment_method: str,
) -> tuple[str, str, str]:
    """Email sent to client when delivery is completed. Returns (subject, text, html)."""
    subject = "Your TumaGo Delivery is Complete!"
    text = (
        f"Hi {client_name},\n\n"
        f"Your delivery has been completed successfully!\n\n"
        f"Delivery ID: {delivery_id}\n"
        f"Driver: {driver_name}\n"
        f"Vehicle: {vehicle or 'N/A'}\n"
        f"Fare: ${fare}\n"
        f"Payment: {payment_method}\n\n"
        "Thank you for using TumaGo!\n"
        "— The TumaGo Team"
    )
    html = _base_wrapper(f"""
        <h2 style="color: #333;">Delivery Complete!</h2>
        <p>Hi {client_name}, your delivery has been completed successfully.</p>
        <div style="background: #EFF8FE; padding: 16px; border-radius: 8px; margin: 16px 0;">
            <table style="width: 100%; border-collapse: collapse;">
                <tr><td style="padding: 6px 0; color: #666;">Delivery ID</td><td style="padding: 6px 0; text-align: right; font-weight: bold;">{delivery_id[:8]}...</td></tr>
                <tr><td style="padding: 6px 0; color: #666;">Driver</td><td style="padding: 6px 0; text-align: right; font-weight: bold;">{driver_name}</td></tr>
                <tr><td style="padding: 6px 0; color: #666;">Vehicle</td><td style="padding: 6px 0; text-align: right; font-weight: bold;">{vehicle or 'N/A'}</td></tr>
                <tr style="border-top: 1px solid #ddd;"><td style="padding: 8px 0; color: #666;">Fare</td><td style="padding: 8px 0; text-align: right; font-size: 18px; font-weight: bold; color: #0e74bc;">${fare}</td></tr>
                <tr><td style="padding: 6px 0; color: #666;">Payment</td><td style="padding: 6px 0; text-align: right; font-weight: bold;">{payment_method}</td></tr>
            </table>
        </div>
        <p>Thank you for using TumaGo!<br/><strong>— The TumaGo Team</strong></p>
    """)
    return subject, text, html


def delivery_cancelled_email(
    client_name: str,
    delivery_id: str,
    driver_name: str,
) -> tuple[str, str, str]:
    """Email sent to client when delivery is cancelled. Returns (subject, text, html)."""
    subject = "TumaGo Delivery Cancelled"
    text = (
        f"Hi {client_name},\n\n"
        f"Your delivery has been cancelled.\n\n"
        f"Delivery ID: {delivery_id}\n"
        f"Driver: {driver_name}\n\n"
        "You can request a new delivery anytime from the TumaGo app.\n\n"
        "— The TumaGo Team"
    )
    html = _base_wrapper(f"""
        <h2 style="color: #333;">Delivery Cancelled</h2>
        <p>Hi {client_name}, your delivery has been cancelled.</p>
        <div style="background: #FFF3F3; padding: 16px; border-radius: 8px; margin: 16px 0;">
            <table style="width: 100%; border-collapse: collapse;">
                <tr><td style="padding: 6px 0; color: #666;">Delivery ID</td><td style="padding: 6px 0; text-align: right; font-weight: bold;">{delivery_id[:8]}...</td></tr>
                <tr><td style="padding: 6px 0; color: #666;">Driver</td><td style="padding: 6px 0; text-align: right; font-weight: bold;">{driver_name}</td></tr>
            </table>
        </div>
        <p>You can request a new delivery anytime from the TumaGo app.</p>
        <p><strong>— The TumaGo Team</strong></p>
    """)
    return subject, text, html


def password_reset_email(name: str, reset_url: str) -> tuple[str, str, str]:
    """Password reset email with a link to the website. Returns (subject, text, html)."""
    subject = "TumaGo — Reset Your Password"
    text = (
        f"Hi {name},\n\n"
        "We received a request to reset your password.\n\n"
        f"Click this link to set a new password:\n{reset_url}\n\n"
        "This link expires in 30 minutes. If you didn't request this, you can safely ignore this email.\n\n"
        "— The TumaGo Team"
    )
    html = _base_wrapper(f"""
        <h2 style="color: #333;">Reset Your Password</h2>
        <p>Hi {name}, we received a request to reset your password.</p>
        <p>Click the button below to set a new password:</p>
        <div style="text-align: center; margin: 24px 0;">
            <a href="{reset_url}" style="display: inline-block; background: #0e74bc; color: #ffffff; padding: 14px 32px; border-radius: 8px; text-decoration: none; font-weight: bold; font-size: 16px;">Reset Password</a>
        </div>
        <p style="color: #666; font-size: 13px;">Or copy this link into your browser:<br/>
        <a href="{reset_url}" style="color: #0e74bc; word-break: break-all;">{reset_url}</a></p>
        <p style="color: #999; font-size: 12px;">This link expires in 30 minutes. If you didn't request this, you can safely ignore this email.</p>
        <p><strong>— The TumaGo Team</strong></p>
    """)
    return subject, text, html
