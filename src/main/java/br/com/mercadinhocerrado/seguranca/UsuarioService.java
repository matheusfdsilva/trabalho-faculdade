package br.com.mercadinhocerrado.seguranca;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder senhas) {
        this.usuarios = usuarios;
        this.senhas = senhas;
    }

    @Transactional
    public void criar(String login, String senha) {
        login = login.strip().toLowerCase(Locale.ROOT);
        if (!login.matches("[a-z0-9][a-z0-9._-]{2,79}")) {
            throw new IllegalArgumentException("Use login de 3 a 80 caracteres: letras sem acentos, números, ponto, hífen ou sublinhado.");
        }
        if (senha == null || senha.codePointCount(0, senha.length()) < 12 || senha.isBlank()
                || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Use senha com pelo menos 12 caracteres e no máximo 72 bytes em UTF-8.");
        }
        if (usuarios.existsByLogin(login)) {
            throw new IllegalArgumentException("Este login já está cadastrado.");
        }
        usuarios.save(new Usuario(login, senhas.encode(senha)));
    }
}
