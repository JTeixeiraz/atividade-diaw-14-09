package com.diaw.loginpuc.controller;

import com.diaw.service.EmailService;
import com.diaw.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.mail.MailException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
public class LoginController {

    private final UserService userService;
    private final EmailService emailService;

    public LoginController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
    }

    /* ===================== LOGIN / HOME / ADMIN ===================== */

    @GetMapping("/login")
    public String getLogin() {
        return "login";
    }

    @GetMapping("/home")
    public String getHome(Authentication authentication, Model model) {
        model.addAttribute("usuario", nomeDe(authentication));
        return "home";
    }

    @GetMapping("/admin")
    public String getAdmin(Authentication authentication, Model model) {
        model.addAttribute("usuario", nomeDe(authentication));
        return "admin";
    }

    private String nomeDe(Authentication authentication) {
        String nome = userService.getName(authentication.getName());
        return nome != null ? nome : authentication.getName();
    }

    /* ===================== CADASTRO ===================== */

    @GetMapping("/register")
    public String getRegister() {
        return "register";
    }

    @PostMapping("/register")
    public String postRegister(
            @RequestParam String nome,
            @RequestParam String email,
            @RequestParam String senha) {

        email = email.trim();

        if (userService.exists(email)) {
            return "redirect:/register?erro";
        }

        userService.createUser(email, senha, nome.trim());

        return "redirect:/login?cadastro";
    }

    /* ===================== RECUPERAR SENHA ===================== */

    @GetMapping("/recoverpassword")
    public String getRecoverPassword() {
        return "recoverPassword";
    }

    @PostMapping("/recoverpassword")
    public String postRecoverPassword(
            @RequestParam String email,
            HttpServletRequest request) {

        email = email.trim();

        if (!userService.exists(email)) {
            return "redirect:/recoverpassword?erro";
        }

        String token = userService.createResetToken(email);

        String link = ServletUriComponentsBuilder.fromContextPath(request)
                .path("/resetpassword")
                .queryParam("token", token)
                .toUriString();

        try {
            emailService.sendPasswordResetEmail(email, link);
        } catch (MailException e) {
            return "redirect:/recoverpassword?erroEnvio";
        }

        return "redirect:/recoverpassword?sucesso";
    }

    /* ===================== REDEFINIR SENHA ===================== */

    @GetMapping("/resetpassword")
    public String getResetPassword(
            @RequestParam(required = false) String token,
            Model model) {

        if (!userService.isValidToken(token)) {
            return "redirect:/recoverpassword?tokenInvalido";
        }

        model.addAttribute("token", token);

        return "resetPassword";
    }

    @PostMapping("/resetpassword")
    public String postResetPassword(
            @RequestParam String token,
            @RequestParam String senha,
            @RequestParam String confirmarSenha) {

        if (!senha.equals(confirmarSenha)) {
            return "redirect:/resetpassword?erro=senhas&token=" + token;
        }

        if (!userService.resetPassword(token, senha)) {
            return "redirect:/recoverpassword?tokenInvalido";
        }

        return "redirect:/login?senhaRedefinida";
    }
}
