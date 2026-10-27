package br.com.mercadinhocerrado.seguranca;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.criar-usuario", havingValue = "true")
public class CriarUsuarioCommand implements ApplicationRunner {
    private final UsuarioService usuarios;
    private final Environment ambiente;
    private final ConfigurableApplicationContext contexto;

    public CriarUsuarioCommand(UsuarioService usuarios, Environment ambiente, ConfigurableApplicationContext contexto) {
        this.usuarios = usuarios;
        this.ambiente = ambiente;
        this.contexto = contexto;
    }

    @Override
    public void run(ApplicationArguments args) {
        usuarios.criar(ambiente.getRequiredProperty("APP_USUARIO"), ambiente.getRequiredProperty("APP_SENHA"));
        System.out.println("Conta criada com sucesso. A aplicação pode ser iniciada normalmente.");
        contexto.close();
    }
}
