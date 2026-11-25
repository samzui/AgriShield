package com.examly.springapp.integration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
@Component @ConditionalOnProperty(name="app.mail.mode",havingValue="smtp")
public class SmtpEmailNotificationProvider implements EmailNotificationProvider {
    private static final Logger log=LoggerFactory.getLogger(SmtpEmailNotificationProvider.class);
    private final JavaMailSender sender;
    private final String host;
    private final String username;
    public SmtpEmailNotificationProvider(JavaMailSender sender,@Value("${spring.mail.host:}")String host,@Value("${spring.mail.username:}")String username) {
        this.sender=sender;
        this.host=host;
        this.username=username;
    }
    @PostConstruct void checkConfigured() {
        // app.mail.mode=smtp only decides WHICH provider is wired up; it doesn't guarantee the SMTP server
        // itself is reachable/authenticated. Surface that clearly at startup instead of failing silently
        // the first time someone registers.
        if(username==null||username.isBlank()||host==null||host.isBlank()||"localhost".equalsIgnoreCase(host)) {
            log.warn("app.mail.mode=smtp but SMTP does not look configured (host='{}', username='{}'). "+
                "Set these environment variables to actually deliver OTP emails: MAIL_HOST, MAIL_PORT, "+
                "MAIL_USERNAME, MAIL_PASSWORD, MAIL_AUTH=true, MAIL_STARTTLS=true. Until then, sending will fail.",host,username);
        }
    }
    public void send(String to,String subject,String body) {
        SimpleMailMessage m=new SimpleMailMessage();
        m.setTo(to);
        m.setSubject(subject);
        m.setText(body);
        sender.send(m);
    }
}
