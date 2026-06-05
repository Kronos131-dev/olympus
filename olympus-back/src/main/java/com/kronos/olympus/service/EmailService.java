package com.kronos.olympus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envoi des emails transactionnels d'Olympus (réinitialisation de mot de passe).
 * S'appuie sur la configuration {@code spring.mail.*} (SMTP Gmail partagé avec Chiron).
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        if (fromAddress == null || fromAddress.isBlank()) {
            // Sans expéditeur, JavaMail échoue avec un opaque « Could not parse mail ».
            throw new IllegalStateException(
                    "Envoi d'email non configuré : GMAIL_USERNAME / GMAIL_APP_PASSWORD manquants côté serveur.");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Olympus — Réinitialisation de votre mot de passe");
        message.setText(
                "Bonjour,\n\n" +
                "Vous avez demandé la réinitialisation de votre mot de passe Olympus.\n\n" +
                "Cliquez sur le lien ci-dessous (valable 1h) :\n" +
                resetLink + "\n\n" +
                "Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.\n\n" +
                "— Olympus"
        );
        mailSender.send(message);
    }
}
