package com.diaw.service;

import com.diaw.config.UserConfig;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {
    private final InMemoryUserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;

    /*
     * ============================================================
     * NOMES DOS USUÁRIOS
     * ============================================================
     *
     * Guarda o nome associado ao e-mail/usuário.
     *
     * Exemplo:
     *
     * joao -> João Paulo
     * admin -> Administrador
     * joao@gmail.com -> João Paulo
     *
     */

    private final Map<String, String> userNames = new HashMap<>();

    /*
     * Tokens de redefinição de senha (token -> e-mail e validade).
     */
    private static final long TOKEN_VALIDADE_SEGUNDOS = 30 * 60;

    private record ResetToken(String email, Instant expiraEm) {}

    private final Map<String, ResetToken> resetTokens = new ConcurrentHashMap<>();

    /*
     * ============================================================
     * CONSTRUTOR
     * ============================================================
     */

    public UserService(
            InMemoryUserDetailsManager userDetailsManager,
            PasswordEncoder passwordEncoder,
            UserConfig userConfig) {

        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;

        /*
         * Registra os nomes dos usuários pré-configurados.
         */
        userNames.put(
                userConfig.getUserUsername(),
                userConfig.getUserName());

        userNames.put(
                userConfig.getAdminUsername(),
                userConfig.getAdminName());
    }

    /*
     * ============================================================
     * CRIAR USUÁRIO
     * ============================================================
     */

    public void createUser(
            String email,
            String senha,
            String nome) {

        /*
         * Cria o usuário do Spring Security.
         */
        UserDetails user = User.builder()
                .username(email)
                .password(
                        passwordEncoder.encode(senha))
                .roles("USER")
                .build();

        /*
         * Salva o usuário em memória.
         */
        userDetailsManager.createUser(user);

        /*
         * Salva o nome associado ao e-mail.
         */
        userNames.put(
                email,
                nome);
    }

    /*
     * ============================================================
     * VERIFICAR SE USUÁRIO EXISTE
     * ============================================================
     */

    public boolean exists(String email) {

        return userDetailsManager.userExists(email);
    }

    /*
     * ============================================================
     * BUSCAR NOME DO USUÁRIO
     * ============================================================
     */

    public String getName(String email) {

        return userNames.get(email);
    }

    /*
     * ============================================================
     * ATUALIZAR SENHA
     * ============================================================
     */

    public void updatePassword(
            String email,
            String novaSenha) {

        /*
         * Busca o usuário atual.
         */
        UserDetails usuarioAtual = userDetailsManager.loadUserByUsername(email);

        /*
         * Cria uma nova versão do usuário
         * mantendo as permissões atuais.
         */
        UserDetails usuarioAtualizado = User.builder()
                .username(
                        usuarioAtual.getUsername())
                .password(
                        passwordEncoder.encode(novaSenha))
                .authorities(
                        usuarioAtual.getAuthorities())
                .build();

        /*
         * Atualiza o usuário no armazenamento em memória.
         */
        userDetailsManager.updateUser(
                usuarioAtualizado);
    }

    /*
     * ============================================================
     * TOKEN DE REDEFINIÇÃO DE SENHA
     * ============================================================
     */

    /** Gera um token válido por 30 minutos para o e-mail informado. */
    public String createResetToken(String email) {

        String token = UUID.randomUUID().toString();

        resetTokens.put(
                token,
                new ResetToken(
                        email,
                        Instant.now().plusSeconds(TOKEN_VALIDADE_SEGUNDOS)));

        return token;
    }

    /** Retorna true se o token existe e ainda não expirou. */
    public boolean isValidToken(String token) {

        if (token == null) {
            return false;
        }

        ResetToken resetToken = resetTokens.get(token);

        if (resetToken == null) {
            return false;
        }

        if (resetToken.expiraEm().isBefore(Instant.now())) {
            resetTokens.remove(token);
            return false;
        }

        return true;
    }

    /**
     * Redefine a senha usando o token (que é invalidado após o uso).
     * Retorna false se o token for inválido ou expirado.
     */
    public boolean resetPassword(String token, String novaSenha) {

        if (!isValidToken(token)) {
            return false;
        }

        ResetToken resetToken = resetTokens.remove(token);

        updatePassword(
                resetToken.email(),
                novaSenha);

        return true;
    }
}
