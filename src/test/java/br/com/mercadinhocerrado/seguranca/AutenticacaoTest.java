package br.com.mercadinhocerrado.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.config.import=")
@AutoConfigureMockMvc
@Testcontainers
class AutenticacaoTest {
    @Container
    static final PostgreSQLContainer<?> BANCO = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", BANCO::getJdbcUrl);
        registro.add("spring.datasource.username", BANCO::getUsername);
        registro.add("spring.datasource.password", BANCO::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioService usuarios;
    @Autowired UsuarioRepository repositorio;
    @Autowired PasswordEncoder senhas;
    @Autowired JdbcTemplate jdbc;

    private static final String SENHA = "Senha-de-teste-2026!";

    @BeforeEach
    void prepararUsuario() {
        repositorio.deleteAll();
        usuarios.criar("operador", SENHA);
    }

    @Test
    void apisExigemAutenticacaoESemAcessoNaoGravam() throws Exception {
        mvc.perform(get("/api/produtos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/movimentacoes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/produtos").with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM produto", Integer.class));
    }

    @Test
    void loginComTokenRealRenovaIdDaSessaoEPersisteAutenticacao() throws Exception {
        var inicial = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var sessao = (MockHttpSession) inicial.getRequest().getSession(false);
        String idAnterior = sessao.getId();
        var token = json.readTree(inicial.getResponse().getContentAsString());
        mvc.perform(post("/api/auth/login").session(sessao)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .param("username", "OPERADOR").param("password", SENHA))
                .andExpect(status().isOk());
        assertNotEquals(idAnterior, sessao.getId());
        mvc.perform(get("/api/auth/me").session(sessao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.login").value("operador"));
        mvc.perform(get("/api/produtos").session(sessao)).andExpect(status().isOk());
    }

    @Test
    void csrfProtegeLoginAlteracoesELogout() throws Exception {
        mvc.perform(post("/api/auth/login").param("username", "operador").param("password", SENHA))
                .andExpect(status().isForbidden());
        var sessao = entrar();
        mvc.perform(post("/api/produtos").session(sessao).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").session(sessao)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(status().isOk());
    }

    @Test
    void logoutInvalidaSessao() throws Exception {
        var sessao = entrar();
        mvc.perform(post("/api/auth/logout").session(sessao).with(csrf())).andExpect(status().isOk());
        assertTrue(sessao.isInvalid());
        mvc.perform(get("/api/produtos")).andExpect(status().isUnauthorized());
    }

    @Test
    void quintaFalhaBloqueiaInclusiveSenhaCorretaEAposPrazoPermiteLogin() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").with(csrf()).param("username", "operador").param("password", "errada"))
                    .andExpect(status().isUnauthorized());
            assertEquals(i + 1, jdbc.queryForObject("SELECT tentativas FROM usuario WHERE login='operador'", Integer.class));
        }
        assertEquals(5, jdbc.queryForObject("SELECT tentativas FROM usuario WHERE login='operador'", Integer.class));
        mvc.perform(post("/api/auth/login").with(csrf()).param("username", "operador").param("password", SENHA))
                .andExpect(status().isUnauthorized());
        jdbc.update("UPDATE usuario SET bloqueado_ate = CURRENT_TIMESTAMP - INTERVAL '1 minute', inicio_tentativas = CURRENT_TIMESTAMP - INTERVAL '16 minutes'");
        entrar();
        assertEquals(0, jdbc.queryForObject("SELECT tentativas FROM usuario WHERE login='operador'", Integer.class));
    }

    @Test
    void sucessoLimpaFalhasEJanelaExpiradaReiniciaContagem() throws Exception {
        jdbc.update("UPDATE usuario SET tentativas=4, inicio_tentativas=CURRENT_TIMESTAMP - INTERVAL '16 minutes'");
        mvc.perform(post("/api/auth/login").with(csrf()).param("username", "operador").param("password", "errada"))
                .andExpect(status().isUnauthorized());
        assertEquals(1, jdbc.queryForObject("SELECT tentativas FROM usuario WHERE login='operador'", Integer.class));
        entrar();
        assertEquals(0, jdbc.queryForObject("SELECT tentativas FROM usuario WHERE login='operador'", Integer.class));
    }

    @Test
    void usuarioInexistenteESenhaErradaRetornamMesmaMensagem() throws Exception {
        var inexistente = mvc.perform(post("/api/auth/login").with(csrf())
                .param("username", "inexistente").param("password", "errada"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        var errada = mvc.perform(post("/api/auth/login").with(csrf())
                .param("username", "operador").param("password", "errada"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertEquals(inexistente, errada);
    }

    @Test
    void contasGuardamBcryptCustoDozeESaltsIndividuais() {
        usuarios.criar("segundo", SENHA);
        var hashes = jdbc.queryForList("SELECT senha_hash FROM usuario ORDER BY login", String.class);
        assertNotEquals(hashes.get(0), hashes.get(1));
        for (String hash : hashes) {
            assertTrue(hash.startsWith("$2a$12$"));
            assertTrue(senhas.matches(SENHA, hash));
            assertNotEquals(SENHA, hash);
        }
    }

    @Test
    void criacaoAdministrativaRejeitaSenhaFracaLongaOuContaDuplicada() {
        assertThrows(IllegalArgumentException.class, () -> usuarios.criar("curta", "123"));
        assertThrows(IllegalArgumentException.class, () -> usuarios.criar("longa", "á".repeat(40)));
        assertThrows(IllegalArgumentException.class, () -> usuarios.criar("operador", SENHA));
        assertEquals(1, repositorio.count());
    }

    @Test
    void respostaAplicaCspENaoExpoeHash() throws Exception {
        mvc.perform(get("/api/auth/me").session(entrar()))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void comandoAdministrativoCriaContaEEncerraSemServidorHttp() {
        var contexto = new org.springframework.boot.builder.SpringApplicationBuilder(
                br.com.mercadinhocerrado.MercadinhoApplication.class)
                .web(org.springframework.boot.WebApplicationType.NONE)
                .run("--spring.config.import=", "--app.criar-usuario=true", "--APP_USUARIO=novo.operador",
                        "--APP_SENHA=" + SENHA, "--spring.datasource.url=" + BANCO.getJdbcUrl(),
                        "--spring.datasource.username=" + BANCO.getUsername(),
                        "--spring.datasource.password=" + BANCO.getPassword());
        assertFalse(contexto.isActive());
        assertTrue(repositorio.existsByLogin("novo.operador"));
    }

    private MockHttpSession entrar() throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "operador").param("password", SENHA))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
}
