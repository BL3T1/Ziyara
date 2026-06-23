package com.ziyara.backend.application.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends mail off the HTTP request thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailDispatchService {

    private final JavaMailSender mailSender;

    @Async("taskExecutor")
    public void sendHtml(String from, String replyTo, String to, String subject, String htmlBody) {
        sendHtml(from, replyTo, new String[]{to}, subject, htmlBody);
    }

    @Async("taskExecutor")
    public void sendHtml(String from, String replyTo, String[] to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            if (replyTo != null && !replyTo.isBlank()) {
                helper.setReplyTo(replyTo);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Async mail send failed subject='{}': {}", subject, ex.getMessage());
        }
    }
}
