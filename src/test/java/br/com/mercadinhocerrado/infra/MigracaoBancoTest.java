package br.com.mercadinhocerrado.infra;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import br.com.mercadinhocerrado.MercadinhoApplication;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class MigracaoBancoTest {
    @Container
    static final PostgreSQLContainer<?> BANCO = new PostgreSQLContainer<>("postgres:16-alpine");

    static Flyway flyway;

    @BeforeAll
    static void prepararBanco() {
        flyway = Flyway.configure()
                .dataSource(BANCO.getJdbcUrl(), BANCO.getUsername(), BANCO.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }

    @Test
    void aplicacaoIniciaComBancoMigrado() {
        try (var contexto = new SpringApplicationBuilder(MercadinhoApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.config.import=",
                        "--spring.datasource.url=" + BANCO.getJdbcUrl(),
                        "--spring.datasource.username=" + BANCO.getUsername(),
                        "--spring.datasource.password=" + BANCO.getPassword())) {
            assertTrue(contexto.isActive());
            assertTrue(contexto.getBean(Flyway.class).validateWithResult().validationSuccessful);
        }
    }

    @Test
    void migracoesPodemSerReexecutadasSemRecriarTabelas() {
        assertEquals(0, flyway.migrate().migrationsExecuted);
        assertTrue(flyway.validateWithResult().validationSuccessful);
    }

    @Test
    void bancoImpedeSaldoNegativo() throws SQLException {
        try (var conexao = conectar(); var comando = conexao.createStatement()) {
            var erro = assertThrows(SQLException.class, () -> comando.executeUpdate("""
                    INSERT INTO produto (nome, categoria, preco, quantidade_estoque, estoque_minimo)
                    VALUES ('Arroz', 'Alimentos', 25.90, -1, 5)
                    """));
            assertEquals("23514", erro.getSQLState());
        }
    }

    @Test
    void bancoPreservaHistoricoEValidaMovimentacoes() throws SQLException {
        try (var conexao = conectar(); var comando = conexao.createStatement()) {
            long id;
            try (var resultado = comando.executeQuery("""
                    INSERT INTO produto (nome, categoria, preco, quantidade_estoque, estoque_minimo)
                    VALUES ('Óleo de Soja 900 ml', 'Alimentos', 8.50, 6, 5)
                    RETURNING id_produto
                    """)) {
                assertTrue(resultado.next());
                id = resultado.getLong(1);
            }
            try (var inserir = conexao.prepareStatement(
                    "INSERT INTO movimentacao (id_produto, tipo, quantidade) VALUES (?, ?, ?)")) {
                inserir.setLong(1, id);
                inserir.setString(2, "SAIDA");
                inserir.setInt(3, 2);
                assertEquals(1, inserir.executeUpdate());
                inserir.setInt(3, 0);
                assertEquals("23514", assertThrows(SQLException.class, inserir::executeUpdate).getSQLState());
                inserir.setInt(3, 1);
                inserir.setString(2, "INVALIDO");
                assertEquals("23514", assertThrows(SQLException.class, inserir::executeUpdate).getSQLState());
            }
            try (var excluir = conexao.prepareStatement("DELETE FROM produto WHERE id_produto = ?")) {
                excluir.setLong(1, id);
                assertEquals("23503", assertThrows(SQLException.class, excluir::executeUpdate).getSQLState());
            }
            try (var consultar = conexao.prepareStatement(
                    "SELECT data_movimentacao FROM movimentacao WHERE id_produto = ?")) {
                consultar.setLong(1, id);
                try (var resultado = consultar.executeQuery()) {
                    assertTrue(resultado.next());
                    assertNotNull(resultado.getObject(1));
                }
            }
        }
    }

    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(BANCO.getJdbcUrl(), BANCO.getUsername(), BANCO.getPassword());
    }
}
