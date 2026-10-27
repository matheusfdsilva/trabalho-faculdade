package br.com.mercadinhocerrado.movimentacao;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Stream;

import br.com.mercadinhocerrado.produto.Produto;
import br.com.mercadinhocerrado.produto.ProdutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.config.import=")
@AutoConfigureMockMvc
@Testcontainers
class RegistroMovimentacaoTest {
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
    @Autowired MovimentacaoRepository movimentacoes;
    @Autowired MovimentacaoService servico;
    @Autowired JdbcTemplate jdbc;

    private Long produtoId;

    @BeforeEach
    void prepararProduto() {
        movimentacoes.deleteAll();
        produtos.deleteAll();
        produtoId = produtos.save(new Produto("Óleo de Soja 900 ml", "Alimentos", new BigDecimal("8.50"), 6, 5)).getId();
    }

    private RegistroMovimentacaoRequest dados(TipoMovimentacao tipo, int quantidade) {
        return new RegistroMovimentacaoRequest(UUID.randomUUID(), produtoId, tipo, quantidade);
    }

    @Test
    void saidaAtualizaSaldoEPreservaResultadoDoCenarioAcademico() throws Exception {
        mvc.perform(post("/api/movimentacoes").contentType(APPLICATION_JSON)
                        .content(json.writeValueAsBytes(dados(TipoMovimentacao.SAIDA, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoAnterior").value(6))
                .andExpect(jsonPath("$.saldoAtual").value(4))
                .andExpect(jsonPath("$.estoqueMinimo").value(5))
                .andExpect(jsonPath("$.reposicaoNecessaria").value(true))
                .andExpect(jsonPath("$.dataMovimentacao").isNotEmpty());
        assertEquals(4, produtos.findById(produtoId).orElseThrow().getQuantidadeEstoque());
        assertEquals(1, movimentacoes.count());
    }

    @Test
    void entradaAumentaSaldo() {
        var resultado = servico.registrar(dados(TipoMovimentacao.ENTRADA, 3));
        assertEquals(9, resultado.saldoAtual());
        assertFalse(resultado.reposicaoNecessaria());
        assertEquals(9, produtos.findById(produtoId).orElseThrow().getQuantidadeEstoque());
        assertEquals(1, movimentacoes.count());
    }

    @Test
    void saldoIgualAoMinimoTambemIndicaReposicao() {
        assertTrue(servico.registrar(dados(TipoMovimentacao.SAIDA, 1)).reposicaoNecessaria());
    }

    @Test
    void saidaPodeZerarEstoque() {
        assertEquals(0, servico.registrar(dados(TipoMovimentacao.SAIDA, 6)).saldoAtual());
    }

    @Test
    void rejeitaSaidaSemSaldoSemAlterarEstoqueOuHistorico() throws Exception {
        mvc.perform(post("/api/movimentacoes").contentType(APPLICATION_JSON)
                        .content(json.writeValueAsBytes(dados(TipoMovimentacao.SAIDA, 7))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Quantidade superior ao estoque disponível."));
        assertEquals(6, produtos.findById(produtoId).orElseThrow().getQuantidadeEstoque());
        assertEquals(0, movimentacoes.count());
    }

    @Test
    void impedeOverflowEmEntrada() {
        assertThrows(MovimentacaoInvalidaException.class,
                () -> servico.registrar(dados(TipoMovimentacao.ENTRADA, Integer.MAX_VALUE)));
        assertEquals(6, produtos.findById(produtoId).orElseThrow().getQuantidadeEstoque());
        assertEquals(0, movimentacoes.count());
    }

    @Test
    void reenvioRetornaResultadoOriginalMesmoAposOutraMovimentacao() {
        var operacao = dados(TipoMovimentacao.SAIDA, 2);
        var original = servico.registrar(operacao);
        servico.registrar(dados(TipoMovimentacao.ENTRADA, 10));
        assertEquals(original, servico.registrar(operacao));
        assertEquals(14, produtos.findById(produtoId).orElseThrow().getQuantidadeEstoque());
        assertEquals(2, movimentacoes.count());
    }

    @Test
    void chaveReutilizadaComOutrosDadosRetornaConflito() throws Exception {
        var original = dados(TipoMovimentacao.SAIDA, 2);
        servico.registrar(original);
        var alterada = new RegistroMovimentacaoRequest(original.idOperacao(), produtoId, TipoMovimentacao.ENTRADA, 2);
        mvc.perform(post("/api/movimentacoes").contentType(APPLICATION_JSON).content(json.writeValueAsBytes(alterada)))
                .andExpect(status().isConflict());
        assertEquals(4, produtos.findById(produtoId).orElseThrow().getQuantidadeEstoque());
        assertEquals(1, movimentacoes.count());
    }

    @Test
    void produtoInexistenteRetorna404() throws Exception {
        var operacao = new RegistroMovimentacaoRequest(UUID.randomUUID(), Long.MAX_VALUE, TipoMovimentacao.SAIDA, 1);
        mvc.perform(post("/api/movimentacoes").contentType(APPLICATION_JSON).content(json.writeValueAsBytes(operacao)))
                .andExpect(status().isNotFound());
        assertEquals(0, movimentacoes.count());
    }

    @ParameterizedTest
    @MethodSource("corposInvalidos")
    void rejeitaDadosInvalidosSemGravar(String corpo) throws Exception {
        corpo = corpo.replace("PRODUTO_ID", produtoId.toString());
        mvc.perform(post("/api/movimentacoes").contentType(APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
        assertEquals(6, produtos.findById(produtoId).orElseThrow().getQuantidadeEstoque());
        assertEquals(0, movimentacoes.count());
    }

    static Stream<String> corposInvalidos() {
        String base = """
                {"idOperacao":"f08a338e-cbc4-4e41-8a9c-b7f88a65a237","produtoId":PRODUTO_ID,"tipo":"SAIDA","quantidade":2}
                """;
        return Stream.of("{}", base.replace("\"quantidade\":2", "\"quantidade\":0"),
                base.replace("\"quantidade\":2", "\"quantidade\":-1"),
                base.replace("\"quantidade\":2", "\"quantidade\":1.5"),
                base.replace("\"tipo\":\"SAIDA\"", "\"tipo\":\"VENDA\""),
                base.replace("PRODUTO_ID", "0"),
                base.replace("\"idOperacao\":\"f08a338e-cbc4-4e41-8a9c-b7f88a65a237\",", ""),
                base.replace("f08a338e-cbc4-4e41-8a9c-b7f88a65a237", "invalido"));
    }

    @Test
    void falhaNaGravacaoDoHistoricoDesfazOperacaoInteira() {
        jdbc.execute("""
                CREATE FUNCTION rejeitar_movimentacao_teste() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'Falha de gravacao simulada'; END; $$
                """);
        jdbc.execute("CREATE TRIGGER falha_teste BEFORE INSERT ON movimentacao FOR EACH ROW EXECUTE FUNCTION rejeitar_movimentacao_teste()");
        try {
            assertThrows(RuntimeException.class, () -> servico.registrar(dados(TipoMovimentacao.SAIDA, 2)));
            assertEquals(6, produtos.findById(produtoId).orElseThrow().getQuantidadeEstoque());
            assertEquals(0, movimentacoes.count());
        } finally {
            jdbc.execute("DROP TRIGGER falha_teste ON movimentacao");
            jdbc.execute("DROP FUNCTION rejeitar_movimentacao_teste()");
        }
    }

    @Test
    void saidasSimultaneasNaoConsomemMesmoSaldo() throws Exception {
        var resultados = executarSimultaneamente(dados(TipoMovimentacao.SAIDA, 4), dados(TipoMovimentacao.SAIDA, 4));
        assertEquals(1, resultados.stream().filter(Boolean::booleanValue).count());
        assertEquals(2, produtos.findById(produtoId).orElseThrow().getQuantidadeEstoque());
        assertEquals(1, movimentacoes.count());
    }

    @Test
    void reenviosSimultaneosMovimentamApenasUmaVez() throws Exception {
        var operacao = dados(TipoMovimentacao.SAIDA, 2);
        assertEquals(List.of(true, true), executarSimultaneamente(operacao, operacao));
        assertEquals(4, produtos.findById(produtoId).orElseThrow().getQuantidadeEstoque());
        assertEquals(1, movimentacoes.count());
    }

    private List<Boolean> executarSimultaneamente(RegistroMovimentacaoRequest primeira,
                                                 RegistroMovimentacaoRequest segunda) throws Exception {
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futuros = Stream.of(primeira, segunda).map(operacao -> executor.submit(() -> {
                assertTrue(inicio.await(10, TimeUnit.SECONDS));
                try {
                    servico.registrar(operacao);
                    return true;
                } catch (MovimentacaoInvalidaException erro) {
                    return false;
                }
            })).toList();
            inicio.countDown();
            return List.of(futuros.get(0).get(30, TimeUnit.SECONDS), futuros.get(1).get(30, TimeUnit.SECONDS));
        }
    }
}
