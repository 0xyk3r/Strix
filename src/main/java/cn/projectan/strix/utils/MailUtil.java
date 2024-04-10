package cn.projectan.strix.utils;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 邮件工具类
 *
 * @author ProjectAn
 * @date 2023/5/15 21:23
 */
@Slf4j
@Component
@ConditionalOnClass(JavaMailSender.class)
@ConditionalOnProperty(prefix = "spring.mail", name = "username")
public class MailUtil {

    private final JavaMailSender mailSender;

    @Autowired(required = false)
    public MailUtil(final JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 简单文本邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content 内容
     */
    public void sendSimpleMail(String to, String subject, String content) {
        if (mailSender == null) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        // 邮件发送人
        message.setFrom(from);
        // 邮件接收人
        message.setTo(to);
        // 邮件主题
        message.setSubject(subject);
        // 邮件内容
        message.setText(content);
        // 发送邮件
        mailSender.send(message);
    }

    /**
     * html邮件
     *
     * @param to      收件人,多个时参数形式 ："xxx@xxx.com,xxx@xxx.com,xxx@xxx.com"
     * @param subject 主题
     * @param content 内容
     */
    public void sendHtmlMail(String to, String subject, String content) {
        if (mailSender == null) {
            return;
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper messageHelper;
        try {
            messageHelper = new MimeMessageHelper(message, true);
            // 邮件发送人
            messageHelper.setFrom(from);
            // 邮件接收人,设置多个收件人地址
            InternetAddress[] internetAddressTo = InternetAddress.parse(to);
            messageHelper.setTo(internetAddressTo);
            // 邮件主题
            message.setSubject(subject);
            // 邮件内容，html格式
            messageHelper.setText(content, true);
            // 发送
            mailSender.send(message);
        } catch (Exception e) {
            log.error("发送邮件时发生异常！", e);
        }
    }

    /**
     * 带附件的邮件
     *
     * @param to       收件人
     * @param subject  主题
     * @param content  内容
     * @param filePath 附件
     */
    public void sendAttachmentsMail(String to, String subject, String content, String filePath) {
        if (mailSender == null) {
            return;
        }
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            FileSystemResource file = new FileSystemResource(new File(filePath));
            String fileName = filePath.substring(filePath.lastIndexOf(File.separator));
            helper.addAttachment(fileName, file);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("发送邮件时发生异常！", e);
        }
    }

}
