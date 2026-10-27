package br.com.mercadinhocerrado.produto;

import java.math.BigDecimal;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.config.import=")
@AutoConfigureMockMvc
@Testcontainers
class CadastroProdutoTest {
    @Container
    static final PostgreSQLContainer<?> BANCO = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", BANCO::getJdbcUrl);
        registro.add("spring.datasource.username", BANCO::getUsername);
        registro.add("spring.datasource.password", BANCO::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProdutoRepository produtos;

    private static final String CADASTRO = """
            {"nome":"  Óleo de Soja 900 ml  ","categoria":" Alimentos ",
             "preco":8.50,"quantidadeEstoque":6,"estoqueMinimo":5}
            """;

    @BeforeEach
    void limparProdutos() {
        produtos.deleteAll();
    }

    @Test
    void cadastraProdutoEPersisteDadosComIdentificadorGerado() throws Exception {
        var resposta = mvc.perform(post("/api/produtos").contentType(APPLICATION_JSON).content(CADASTRO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Óleo de Soja 900 ml"))
                .andExpect(jsonPath("$.categoria").value("Alimentos"))
                .andExpect(jsonPath("$.quantidadeEstoque").value(6))
                .andReturn().getResponse();

        long id = json.readTree(resposta.getContentAsString()).get("id").asLong();
        assertTrue(id > 0);
        var salvo = produtos.findById(id).orElseThrow();
        assertEquals("Óleo de Soja 900 ml", salvo.getNome());
        assertEquals("Alimentos", salvo.getCategoria());
        assertEquals(new BigDecimal("8.50"), salvo.getPreco());
        assertEquals(6, salvo.getQuantidadeEstoque());
        assertEquals(5, salvo.getEstoqueMinimo());
        assertEquals(1, produtos.count());
    }

    @Test
    void aceitaPrecoESaldosZerados() throws Exception {
        var dados = json.readTree(CADASTRO).deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) dados)
                .put("preco", 0).put("quantidadeEstoque", 0).put("estoqueMinimo", 0);
        mvc.perform(post("/api/produtos").contentType(APPLICATION_JSON).content(json.writeValueAsBytes(dados)))
                .andExpect(status().isCreated());
        assertEquals(1, produtos.count());
    }

    @ParameterizedTest(name = "Rejeita {0} = {1}")
    @MethodSource("camposInvalidos")
    void rejeitaDadosInvalidosSemGravar(String campo, Object valor) throws Exception {
        var dados = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(CADASTRO);
        dados.set(campo, json.valueToTree(valor));
        mvc.perform(post("/api/produtos").contentType(APPLICATION_JSON).content(json.writeValueAsBytes(dados)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Confira os campos informados."))
                .andExpect(jsonPath("$.campos[?(@.campo == '" + campo + "')]").isNotEmpty());
        assertEquals(0, produtos.count());
    }

    static Stream<Arguments> camposInvalidos() {
        return Stream.of(
                Arguments.of("nome", null),
                Arguments.of("nome", "   "),
                Arguments.of("nome", "a".repeat(151)),
                Arguments.of("nome", "<script>alert(1)</script>"),
                Arguments.of("categoria", null),
                Arguments.of("categoria", " "),
                Arguments.of("categoria", "a".repeat(81)),
                Arguments.of("preco", null),
                Arguments.of("preco", -1),
                Arguments.of("preco", new BigDecimal("1.999")),
                Arguments.of("preco", new BigDecimal("100000000.00")),
                Arguments.of("quantidadeEstoque", null),
                Arguments.of("quantidadeEstoque", -1),
                Arguments.of("estoqueMinimo", null),
                Arguments.of("estoqueMinimo", -1));
    }

    @ParameterizedTest
    @MethodSource("corposInvalidos")
    void rejeitaJsonOuTiposInvalidosSemExporDetalhesInternos(String corpo) throws Exception {
        mvc.perform(post("/api/produtos").contentType(APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
        assertEquals(0, produtos.count());
    }

    static Stream<String> corposInvalidos() {
        return Stream.of("{", "null", "{}",
                CADASTRO.replace("\"quantidadeEstoque\":6", "\"quantidadeEstoque\":1.5"),
                CADASTRO.replace("\"estoqueMinimo\":5", "\"estoqueMinimo\":0.5"),
                CADASTRO.replace("\"quantidadeEstoque\":6", "\"quantidadeEstoque\":2147483648"),
                CADASTRO.replace("\"preco\":8.50", "\"preco\":\"inválido\""));
    }

    @Test
    void trataTentativaDeSqlComoTextoSemModificarTabelas() throws Exception {
        var dados = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(CADASTRO);
        String nome = "Arroz'); DROP TABLE produto; --";
        dados.put("nome", nome);
        mvc.perform(post("/api/produtos").contentType(APPLICATION_JSON).content(json.writeValueAsBytes(dados)))
                .andExpect(status().isCreated());
        assertEquals(nome, produtos.findAll().getFirst().getNome());
        assertEquals(1, produtos.count());
    }
}
