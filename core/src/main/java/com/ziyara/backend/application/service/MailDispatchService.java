package com.ziyara.backend.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    public void send(SimpleMailMessage message) {
        try {
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Async mail send failed subject='{}': {}", message.getSubject(), ex.getMessage());
        }
    }
}
