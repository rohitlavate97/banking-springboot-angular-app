package com.banking.notification.service;

import com.banking.notification.entity.Notification;
import com.banking.notification.entity.NotificationStatus;
import com.banking.notification.entity.NotificationType;
import com.banking.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Transactional
    public void sendTransactionSuccessNotification(String userId, String recipientEmail,
                                                    String transactionRef, String amount, String currency) {
        String subject = "Transaction Successful - " + transactionRef;
        String body = String.format(
                "Dear Customer,\n\nYour transaction %s of %s %s has been completed successfully.\n\nThank you for banking with us.",
                transactionRef, amount, currency);
        sendAndSave(userId, recipientEmail, subject, body, NotificationType.EMAIL,
                "TRANSACTION_SUCCESS", transactionRef);
    }

    @Transactional
    public void sendTransactionFailedNotification(String userId, String recipientEmail,
                                                   String transactionRef, String reason) {
        String subject = "Transaction Failed - " + transactionRef;
        String body = String.format(
                "Dear Customer,\n\nYour transaction %s could not be completed.\nReason: %s\n\nPlease contact support if you need assistance.",
                transactionRef, reason);
        sendAndSave(userId, recipientEmail, subject, body, NotificationType.EMAIL,
                "TRANSACTION_FAILED", transactionRef);
    }

    @Transactional
    public void sendFraudAlertNotification(String userId, String recipientEmail,
                                            String transactionRef, String description) {
        String subject = "Security Alert - Suspicious Activity Detected";
        String body = String.format(
                "Dear Customer,\n\nWe have detected suspicious activity on your account related to transaction %s.\n\nDetails: %s\n\nIf this was not you, please contact us immediately.",
                transactionRef, description);
        sendAndSave(userId, recipientEmail, subject, body, NotificationType.EMAIL,
                "FRAUD_ALERT", transactionRef);
    }

    private void sendAndSave(String userId, String recipientEmail, String subject,
                              String body, NotificationType type, String eventType, String referenceId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setRecipientEmail(recipientEmail);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setType(type);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setReferenceId(referenceId);
        notification.setEventType(eventType);
        notification = notificationRepository.save(notification);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            notification.setStatus(NotificationStatus.SENT);
            log.info("Email sent to [{}] for event [{}] ref [{}]", recipientEmail, eventType, referenceId);
        } catch (Exception ex) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(ex.getMessage());
            log.error("Failed to send email to [{}] for event [{}]: {}", recipientEmail, eventType, ex.getMessage(), ex);
        } finally {
            notificationRepository.save(notification);
        }
    }
}
