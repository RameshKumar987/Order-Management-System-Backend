package com.oms.auth_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.otp.validity-minutes:5}")
    private Integer otpValidityMinutes;

    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setFrom(senderEmail);
            helper.setSubject("Email Verification - OMS Order Management System");

            String htmlContent = buildOtpEmailHtml(otpCode);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    private String buildOtpEmailHtml(String otpCode) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; }" +
                "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "        .header { background-color: #4CAF50; color: white; padding: 20px; border-radius: 5px 5px 0 0; }" +
                "        .content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; }" +
                "        .otp-code { font-size: 32px; font-weight: bold; color: #4CAF50; text-align: center; margin: 20px 0; }" +
                "        .footer { font-size: 12px; color: #666; text-align: center; margin-top: 20px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>OMS - Order Management System</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <p>Hello,</p>" +
                "            <p>Your One-Time Password (OTP) for email verification is:</p>" +
                "            <div class='otp-code'>" + otpCode + "</div>" +
                "            <p>This OTP will expire in " + otpValidityMinutes + " minutes.</p>" +
                "            <p><strong>Security Note:</strong> Do not share this OTP with anyone. We will never ask you for this OTP.</p>" +
                "            <p>If you didn't request this OTP, please ignore this email.</p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>&copy; 2026 Order Management System. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
}

