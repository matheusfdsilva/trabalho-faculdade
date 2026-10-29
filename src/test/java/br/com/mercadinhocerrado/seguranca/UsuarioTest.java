package br.com.mercadinhocerrado.seguranca;

import java.time.Instant;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Testes unitários da janela de falhas, sem autenticação HTTP ou persistência.
class UsuarioTest {
    private static final Instant INICIO = Instant.parse("2026-10-28T12:00:00Z");

    @Test
    void bloqueiaExatamenteNaQuintaFalhaELiberaAoFinalDeQuinzeMinutos() {
        var usuario = new Usuario("operador", "hash-nao-utilizado-neste-teste-unitario");
        for (int i = 0; i < 4; i++) usuario.registrarFalha(INICIO);
        assertFalse(usuario.bloqueado(INICIO));
        usuario.registrarFalha(INICIO);
        assertTrue(usuario.bloqueado(INICIO.plus(Duration.ofMinutes(15)).minusNanos(1)));
        assertFalse(usuario.bloqueado(INICIO.plus(Duration.ofMinutes(15))));
    }

    @Test
    void janelaExpiradaReiniciaContagemSemBloquearNaPrimeiraFalha() {
        var usuario = new Usuario("operador", "hash-nao-utilizado-neste-teste-unitario");
        for (int i = 0; i < 4; i++) usuario.registrarFalha(INICIO);
        var novaJanela = INICIO.plus(Duration.ofMinutes(15));
        usuario.registrarFalha(novaJanela);
        assertFalse(usuario.bloqueado(novaJanela));
        for (int i = 0; i < 3; i++) usuario.registrarFalha(novaJanela);
        assertFalse(usuario.bloqueado(novaJanela));
        usuario.registrarFalha(novaJanela);
        assertTrue(usuario.bloqueado(novaJanela));
    }

    @Test
    void autenticacaoBemSucedidaLimpaBloqueioEContagem() {
        var usuario = new Usuario("operador", "hash-nao-utilizado-neste-teste-unitario");
        for (int i = 0; i < 5; i++) usuario.registrarFalha(INICIO);
        usuario.limparFalhas();
        assertFalse(usuario.bloqueado(INICIO));
        for (int i = 0; i < 4; i++) usuario.registrarFalha(INICIO);
        assertFalse(usuario.bloqueado(INICIO));
    }
}
