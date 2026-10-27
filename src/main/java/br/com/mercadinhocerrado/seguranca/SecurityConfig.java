package br.com.mercadinhocerrado.seguranca;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(acesso -> acesso
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/login.html", "/movimentacoes.html", "/css/**", "/js/**", "/favicon.ico", "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .formLogin(login -> login.loginPage("/login.html").loginProcessingUrl("/api/auth/login")
                        .successHandler((req, res, auth) -> escrever(res, 200, "Autenticação concluída."))
                        .failureHandler((req, res, erro) -> escrever(res, 401, "Credenciais inválidas ou acesso temporariamente bloqueado.")))
                .logout(logout -> logout.logoutUrl("/api/auth/logout").invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((req, res, auth) -> escrever(res, 200, "Sessão encerrada.")))
                .requestCache(cache -> cache.disable())
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint((req, res, erro) -> escrever(res, 401, "Entre na sua conta para continuar."))
                        .accessDeniedHandler((req, res, erro) -> escrever(res, 403, "Acesso negado. Atualize a página e tente novamente.")))
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'")))
                .build();
    }

    private static void escrever(HttpServletResponse resposta, int status, String mensagem) throws IOException {
        resposta.setStatus(status);
        resposta.setContentType("application/json");
        resposta.setCharacterEncoding("UTF-8");
        resposta.getWriter().write("{\"mensagem\":\"" + mensagem + "\"}");
    }
}
