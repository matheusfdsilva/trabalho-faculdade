package br.com.mercadinhocerrado.produto;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.config.import=")
@AutoConfigureMockMvc
@WithMockUser
@Testcontainers
class ManutencaoProdutoTest {
    @Container
    static final PostgreSQLContainer<?> BANCO = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", BANCO::getJdbcUrl);
        registro.add("spring.datasource.username", BANCO::getUsername);
        registro.add("spring.datasource.password", BANCO::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ProdutoRepository produtos;
    @Autowired JdbcTemplate jdbc;

    private static final String EDICAO = """
            {"nome":" Arroz integral ","categoria":" Grãos ","preco":19.90,"estoqueMinimo":10}
            """;

    @BeforeEach
    void limparBanco() {
        jdbc.update("DELETE FROM movimentacao");
        produtos.deleteAll();
    }

    private Produto salvar(String nome) {
        return produtos.save(new Produto(nome, "Alimentos", new BigDecimal("8.50"), 6, 5));
    }

    @Test
    void listaVaziaTemMetadadosConsistentes() throws Exception {
        mvc.perform(get("/api/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.totalItens").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0));
    }

    @Test
    void paginaVinteItensComDesempatePorIdentificador() throws Exception {
        var ids = new ArrayList<Long>();
        for (int i = 0; i < 25; i++) {
            ids.add(salvar("Arroz").getId());
        }
        mvc.perform(get("/api/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(20))
                .andExpect(jsonPath("$.itens[0].id").value(ids.getFirst().intValue()))
                .andExpect(jsonPath("$.itens[19].id").value(ids.get(19).intValue()))
                .andExpect(jsonPath("$.totalItens").value(25))
                .andExpect(jsonPath("$.totalPaginas").value(2));
        mvc.perform(get("/api/produtos").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(5))
                .andExpect(jsonPath("$.itens[0].id").value(ids.get(20).intValue()))
                .andExpect(jsonPath("$.pagina").value(1));
        mvc.perform(get("/api/produtos").param("pagina", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty())
                .andExpect(jsonPath("$.totalItens").value(25));
    }

    @Test
    void buscaPorTrechoIgnoraMaiusculasEPermiteTamanhoPersonalizado() throws Exception {
        salvar("Arroz branco");
        salvar("Arroz integral");
        salvar("Feijão");
        mvc.perform(get("/api/produtos").param("nome", "  aRRoZ  ").param("tamanho", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].nome").value("Arroz branco"))
                .andExpect(jsonPath("$.totalItens").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void buscaTrataCuringasESqlComoTexto() throws Exception {
        salvar("Café 100%");
        salvar("Arroz");
        mvc.perform(get("/api/produtos").param("nome", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].nome").value("Café 100%"));
        mvc.perform(get("/api/produtos").param("nome", "' OR 1=1 --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty());
        assertEquals(2, produtos.count());
    }

    @Test
    void consultaProdutoPorIdentificador() throws Exception {
        var produto = salvar("Óleo de Soja 900 ml");
        mvc.perform(get("/api/produtos/{id}", produto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(produto.getId().intValue()))
                .andExpect(jsonPath("$.nome").value("Óleo de Soja 900 ml"))
                .andExpect(jsonPath("$.quantidadeEstoque").value(6));
    }

    @Test
    void editaDadosSemAlterarSaldoMesmoComHistorico() throws Exception {
        var produto = salvar("Arroz");
        registrarHistorico(produto.getId());
        mvc.perform(put("/api/produtos/{id}", produto.getId()).with(csrf()).contentType(APPLICATION_JSON).content(EDICAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Arroz integral"))
                .andExpect(jsonPath("$.categoria").value("Grãos"))
                .andExpect(jsonPath("$.quantidadeEstoque").value(6))
                .andExpect(jsonPath("$.estoqueMinimo").value(10));
        var salvo = produtos.findById(produto.getId()).orElseThrow();
        assertEquals("Arroz integral", salvo.getNome());
        assertEquals(new BigDecimal("19.90"), salvo.getPreco());
        assertEquals(6, salvo.getQuantidadeEstoque());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM movimentacao", Integer.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"nome\":\"\",\"categoria\":\"Alimentos\",\"preco\":10,\"estoqueMinimo\":1}",
            "{\"nome\":\"Arroz\",\"categoria\":\"Alimentos\",\"preco\":-1,\"estoqueMinimo\":1}",
            "{\"nome\":\"Arroz\",\"categoria\":\"Alimentos\",\"preco\":10,\"estoqueMinimo\":-1}",
            "{\"nome\":\"Arroz\",\"categoria\":\"Alimentos\",\"preco\":10,\"estoqueMinimo\":1,\"quantidadeEstoque\":999}",
            "{}"
    })
    void edicaoInvalidaNaoModificaProduto(String corpo) throws Exception {
        var produto = salvar("Original");
        mvc.perform(put("/api/produtos/{id}", produto.getId()).with(csrf()).contentType(APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
        var salvo = produtos.findById(produto.getId()).orElseThrow();
        assertEquals("Original", salvo.getNome());
        assertEquals(new BigDecimal("8.50"), salvo.getPreco());
        assertEquals(6, salvo.getQuantidadeEstoque());
        assertEquals(5, salvo.getEstoqueMinimo());
    }

    @Test
    void excluiProdutoSemHistorico() throws Exception {
        var produto = salvar("Arroz");
        mvc.perform(delete("/api/produtos/{id}", produto.getId()).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertFalse(produtos.existsById(produto.getId()));
    }

    @Test
    void impedeExclusaoComHistoricoSemExporSql() throws Exception {
        var produto = salvar("Arroz");
        registrarHistorico(produto.getId());
        mvc.perform(delete("/api/produtos/{id}", produto.getId()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Não é possível excluir um produto com histórico de movimentações."))
                .andExpect(jsonPath("$.trace").doesNotExist());
        assertTrue(produtos.existsById(produto.getId()));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM movimentacao", Integer.class));
    }

    @Test
    void produtoInexistenteRetorna404EmConsultaEdicaoEExclusao() throws Exception {
        long id = Long.MAX_VALUE;
        mvc.perform(get("/api/produtos/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(put("/api/produtos/{id}", id).with(csrf()).contentType(APPLICATION_JSON).content(EDICAO))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/produtos/{id}", id).with(csrf())).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"?pagina=-1", "?pagina=1000001", "?tamanho=0", "?tamanho=101",
            "?pagina=abc", "/0", "/-1", "/abc"})
    void parametrosInvalidosRetornam400(String sufixo) throws Exception {
        mvc.perform(get("/api/produtos" + sufixo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    @Test
    void reposicaoIncluiSaldoIgualAoMinimoEAbaixoDele() throws Exception {
        produtos.save(new Produto("Arroz baixo", "Alimentos", BigDecimal.ONE, 4, 5));
        produtos.save(new Produto("Feijão no mínimo", "Alimentos", BigDecimal.ONE, 5, 5));
        produtos.save(new Produto("Óleo zerado", "Alimentos", BigDecimal.ONE, 0, 0));
        salvar("Produto normal");
        mvc.perform(get("/api/produtos").param("reposicao", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(3));
        mvc.perform(get("/api/produtos").param("reposicao", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(4));
    }

    @Test
    void reposicaoCombinaBuscaEPaginacaoSemInterpretarCuringas() throws Exception {
        produtos.save(new Produto("Arroz 100% A", "Alimentos", BigDecimal.ONE, 1, 5));
        produtos.save(new Produto("Arroz 100% B", "Alimentos", BigDecimal.ONE, 2, 5));
        produtos.save(new Produto("Arroz integral", "Alimentos", BigDecimal.ONE, 1, 5));
        salvar("Arroz 100% normal");
        mvc.perform(get("/api/produtos").param("reposicao", "true")
                        .param("nome", "  ARROZ 100% ").param("tamanho", "1").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(2))
                .andExpect(jsonPath("$.itens[0].nome").value("Arroz 100% B"));
    }

    @Test
    void reposicaoSemResultadosRetornaPaginaVazia() throws Exception {
        salvar("Arroz");
        mvc.perform(get("/api/produtos").param("reposicao", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty())
                .andExpect(jsonPath("$.totalItens").value(0));
    }

    private void registrarHistorico(Long id) {
        jdbc.update("INSERT INTO movimentacao (id_produto, tipo, quantidade) VALUES (?, 'ENTRADA', 1)", id);
    }
}
