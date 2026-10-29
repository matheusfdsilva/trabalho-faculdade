package br.com.mercadinhocerrado.monitoramento;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SaudeControllerTest {
    @Test
    void respondeUpQuandoBancoRespondeSemPermitirCache() throws Exception {
        var banco = mock(JdbcTemplate.class);
        when(banco.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        var mvc = MockMvcBuilders.standaloneSetup(new SaudeController(banco)).build();
        mvc.perform(get("/health")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("{\"status\":\"UP\"}", true));
        verify(banco).queryForObject("SELECT 1", Integer.class);
    }

    @Test
    void responde503SemExporDetalhesDaFalhaDoBanco() throws Exception {
        var banco = mock(JdbcTemplate.class);
        when(banco.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new DataAccessResourceFailureException("detalhe interno do banco"));
        var mvc = MockMvcBuilders.standaloneSetup(new SaudeController(banco)).build();
        mvc.perform(get("/health")).andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("{\"status\":\"DOWN\"}", true));
    }
}
