package br.com.mercadinhocerrado.monitoramento;

import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SaudeController {
    private final JdbcTemplate banco;

    public SaudeController(JdbcTemplate banco) {
        this.banco = banco;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> verificar() {
        try {
            banco.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok().header("Cache-Control", "no-store")
                    .body(Map.of("status", "UP"));
        } catch (DataAccessException erro) {
            return ResponseEntity.status(503).header("Cache-Control", "no-store")
                    .body(Map.of("status", "DOWN"));
        }
    }
}
