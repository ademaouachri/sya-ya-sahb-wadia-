package com.example.backend.Service;

import com.example.backend.Service.ParameterService; // ✅ زيد الـ Import هذا
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final ParameterService parameterService; // ✅ زيد الـ Service هنا


    public EmailService(JavaMailSender mailSender, ParameterService parameterService) {
        this.mailSender = mailSender;
        this.parameterService = parameterService;
    }

    public void sendOtpEmail(String toEmail, String otp) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject("Code de vérification");

        String htmlContent = "<html>" +
                "<body style='font-family: Arial; background-color: #f4f4f4; padding: 20px;'>" +
                "<div style='background: white; padding: 20px; border-radius: 8px; text-align: center;'>" +
                "<img src='cid:logoImage' style='width:100px; margin-bottom:20px;' />" +
                "<h2 style='color: #333;'>🔐 Code de vérification</h2>" +
                "<p style='font-size: 16px;'>Votre code de vérification est :</p>" +
                "<h1 style='color: #007BFF; letter-spacing: 4px;'>" + otp + "</h1>" +
                "<p style='color: #777;'>Ne partagez ce code avec personne.</p>" +
                "</div></body></html>";

        helper.setText(htmlContent, true);
        helper.addInline("logoImage", new ClassPathResource("static/logo.png"));
        mailSender.send(message);
    }

    public void sendSetPasswordEmail(String toEmail, String token) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // ✅ نجبدو الـ URL مالـ Database عوض @Value
        String frontendUrl = parameterService.getValueByKey("frontendUrl");
        if (frontendUrl == null) frontendUrl = "http://localhost:4200"; // Default value

        String link = frontendUrl + "/set-password?token=" + token;

        helper.setTo(toEmail);
        helper.setSubject("🔐 Définissez votre mot de passe");

        String htmlContent = "<html>" +
                "<body style='font-family: Arial; background-color: #f4f4f4; padding: 20px;'>" +
                "<div style='background: white; padding: 20px; border-radius: 8px; text-align: center;'>" +
                "<img src='cid:logoImage' style='width:100px; margin-bottom:20px;' />" +
                "<h2 style='color: #333;'>🔑 Définissez votre mot de passe</h2>" +
                "<p style='font-size:16px;'>Votre identité a été vérifiée avec succès.</p>" +
                "<p>Cliquez ci-dessous pour choisir votre mot de passe :</p>" +
                "<a href='" + link + "' style='display:inline-block; margin-top:20px; " +
                "padding:12px 30px; background-color:#007BFF; color:white; " +
                "text-decoration:none; border-radius:5px; font-size:16px;'>" +
                "Définir mon mot de passe</a>" +
                "<p style='color:#aaa; margin-top:20px; font-size:12px;'>Ce lien expire dans 24h.</p>" +
                "</div></body></html>";

        helper.setText(htmlContent, true);
        helper.addInline("logoImage", new ClassPathResource("static/logo.png"));
        mailSender.send(message);
    }
    // أضف هذه الميثود في EmailService.java
    public void sendReportEmail(String toEmail, String nomAgent, byte[] pdfContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject("📊 Rapport Hebdomadaire de Recouvrement");

        String htmlContent = "<html><body>" +
                "<h2>Bonjour " + nomAgent + ",</h2>" +
                "<p>Veuillez trouver ci-joint votre rapport hebdomadaire généré automatiquement.</p>" +
                "<br><p>Cordialement,<br>Votre équipe GTI</p>" +
                "</body></html>";

        helper.setText(htmlContent, true);
        // إرفاق ملف الـ PDF
        helper.addAttachment("Rapport_Hebdomadaire.pdf", new org.springframework.core.io.ByteArrayResource(pdfContent));

        mailSender.send(message);
    }
}