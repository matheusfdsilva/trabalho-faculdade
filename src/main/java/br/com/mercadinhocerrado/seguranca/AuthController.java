package br.com.mercadinhocerrado.seguranca;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record TokenResponse(String headerName, String token) { }
    public record UsuarioResponse(String login) { }

    @GetMapping("/csrf")
    public TokenResponse csrf(CsrfToken token) {
        return new TokenResponse(token.getHeaderName(), token.getToken());
    }

    @GetMapping("/me")
    public UsuarioResponse usuario(Authentication autenticacao) {
        return new UsuarioResponse(autenticacao.getName());
    }
}
