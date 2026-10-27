package br.com.mercadinhocerrado.seguranca;

import java.time.Instant;
import java.time.Duration;
import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;
    @Column(nullable = false, length = 80, unique = true)
    private String login;
    @Column(name = "senha_hash", nullable = false, length = 60)
    private String senhaHash;
    @Column(nullable = false)
    private int tentativas;
    @Column(name = "inicio_tentativas")
    private Instant inicioTentativas;
    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    protected Usuario() { }
    public Usuario(String login, String senhaHash) {
        this.login = login;
        this.senhaHash = senhaHash;
    }
    public boolean bloqueado(Instant agora) {
        return bloqueadoAte != null && agora.isBefore(bloqueadoAte);
    }
    public void registrarFalha(Instant agora) {
        if (inicioTentativas == null || !agora.isBefore(inicioTentativas.plus(Duration.ofMinutes(15)))) {
            tentativas = 0;
            inicioTentativas = agora;
        }
        tentativas++;
        if (tentativas >= 5) {
            bloqueadoAte = agora.plus(Duration.ofMinutes(15));
        }
    }
    public void limparFalhas() {
        tentativas = 0;
        inicioTentativas = null;
        bloqueadoAte = null;
    }
    public String getLogin() { return login; }
    public String getSenhaHash() { return senhaHash; }
}
