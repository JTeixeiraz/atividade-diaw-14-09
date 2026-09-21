package com.diaw.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /*
     * Envia o e-mail com o link de redefinição de senha.
     */
    public void sendPasswordResetEmail(String destinatario, String link) {

        SimpleMailMessage mensagem = new SimpleMailMessage();

        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject("Redefinição de senha - PUC Minas");
        mensagem.setText(
                "Você solicitou a redefinição de senha.\n\n"
                + "Acesse o link abaixo para criar uma nova senha "
                + "(válido por 30 minutos):\n\n"
                + link
                + "\n\nSe você não fez essa solicitação, ignore este e-mail.");

        mailSender.send(mensagem);
    }
}
