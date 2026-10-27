package br.com.mercadinhocerrado.seguranca;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AutenticacaoProvider implements AuthenticationProvider {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final String hashInexistente;

    public AutenticacaoProvider(UsuarioRepository usuarios, PasswordEncoder senhas) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.hashInexistente = senhas.encode(java.util.UUID.randomUUID().toString());
    }

    @Override
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public Authentication authenticate(Authentication autenticacao) {
        String login = autenticacao.getName().strip().toLowerCase(Locale.ROOT);
        String senha = autenticacao.getCredentials() == null ? "" : autenticacao.getCredentials().toString();
        boolean tamanhoValido = senha.getBytes(StandardCharsets.UTF_8).length <= 72;
        var usuario = usuarios.buscarParaAutenticar(login).orElse(null);
        boolean confere = senhas.matches(tamanhoValido ? senha : "", usuario == null ? hashInexistente : usuario.getSenhaHash());
        Instant agora = Instant.now();
        if (usuario == null || usuario.bloqueado(agora)) {
            throw new BadCredentialsException("Credenciais inválidas ou acesso temporariamente bloqueado.");
        }
        if (!tamanhoValido || !confere) {
            usuario.registrarFalha(agora);
            throw new BadCredentialsException("Credenciais inválidas ou acesso temporariamente bloqueado.");
        }
        usuario.limparFalhas();
        var principal = User.withUsername(usuario.getLogin()).password("").roles("USUARIO").build();
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> tipo) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(tipo);
    }
}
