package com.techmania.tumago.helper;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSenderHelper {

    public static void sendEmail(final String to, final String subject, final int body) {
        new SendEmailTask(to, subject, body).execute();
    }

    private static class SendEmailTask extends AsyncTask<Void, Void, Boolean> {
        private final String to;
        private final String subject;
        private final int body;

        private static final String FROM_EMAIL = "apex2.0predator@gmail.com";  // Sender email
        private static final String PASSWORD = "hrobguwmsqgjkswf";  // App Password

        public SendEmailTask(String to, String subject, int body) {
            this.to = to;
            this.subject = subject;
            this.body = body;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Properties properties = new Properties();
                properties.put("mail.smtp.host", "smtp.gmail.com");
                properties.put("mail.smtp.port", "587");
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.starttls.enable", "true");

                Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(FROM_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                message.setSubject(subject);
                message.setText(String.valueOf(body)); // Convert int to string

                Transport.send(message);
                return true;
            } catch (MessagingException e) {
                Log.e("SendEmail", "Failed to send email", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Log.d("SendEmail", "Email sent successfully!");
            } else {
                Log.e("SendEmail", "Failed to send email!");
            }
        }
    }
}