package com.scut.conference.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务。app.mail-enabled=false 时不真正发信,只打印日志,
 * 便于本地未配置 SMTP 时联调注册流程。
 */
@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.mail-enabled}")
    private boolean enabled;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void send(String to, String subject, String text) {
        if (!enabled) {
            log.info("[邮件未启用] 收件人:{} 主题:{} 内容:{}", to, subject, text);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
