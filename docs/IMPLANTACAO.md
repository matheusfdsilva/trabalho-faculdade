# Execução e implantação

## Opção 1: Java no computador e PostgreSQL no Docker

Instale JDK 21 e Docker Desktop com contêineres Linux. Confira `java -version` e `JAVA_HOME`: ambos devem apontar para Java 21. Não é necessário instalar Maven manualmente; o Wrapper baixa a versão 3.9.11 na primeira execução.

No PowerShell, na raiz do projeto:

```powershell
Copy-Item .env.example .env
notepad .env
docker compose up -d --wait
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

Preencha `POSTGRES_PASSWORD` antes de iniciar. Crie uma conta com o procedimento em [AUTENTICACAO.md](AUTENTICACAO.md) e acesse `http://localhost:8080`. Nos comandos desse guia, `mvn` também pode ser substituído por `.\mvnw.cmd`.

No Linux/macOS, utilize `cp .env.example .env` e `./mvnw` em vez de `.\mvnw.cmd`. Os testes de integração exigem Docker disponível, mas não precisam do serviço PostgreSQL do Compose nem de um `.env` real.

Para gerar e executar o JAR:

```powershell
.\mvnw.cmd verify
java -jar target/mercadinho-cerrado-0.1.0-SNAPSHOT.jar
```

## Opção 2: aplicação e PostgreSQL no Docker

Esta opção exige apenas Docker com Compose para executar. O Dockerfile compila com Maven e JDK 21 e executa o JAR com JRE 21 e usuário sem privilégios de root. Não há necessidade de Node.js para executar ou compilar a aplicação.

Depois de criar e preencher o `.env`:

```powershell
docker compose --profile app up -d --build --wait
docker compose --profile app ps
```

O perfil `app` inclui a aplicação. Sem esse perfil, o Compose inicia apenas o banco, preservando o fluxo de desenvolvimento anterior. A imagem não inclui `.env`, repositório Git, capturas, arquivos de teste temporários nem credenciais do computador. O build da imagem empacota sem executar os testes; a aprovação dos testes é feita separadamente pelo Wrapper e pelo workflow do GitHub.

Para criar uma conta no banco usado pelos contêineres:

```powershell
$env:APP_USUARIO = Read-Host 'Login do funcionário'
$senhaProtegida = Read-Host 'Senha com pelo menos 12 caracteres' -AsSecureString
try {
    $env:APP_SENHA = [System.Net.NetworkCredential]::new('', $senhaProtegida).Password
    docker compose --profile app exec -T -e APP_USUARIO -e APP_SENHA app java -jar app.jar --spring.main.web-application-type=none --app.criar-usuario=true
} finally {
    Remove-Item Env:APP_SENHA -ErrorAction SilentlyContinue
    Remove-Item Env:APP_USUARIO -ErrorAction SilentlyContinue
    $senhaProtegida.Dispose()
}
```

A criação da conta utiliza outro processo Java, que encerra ao terminar; o servidor continua funcionando. Não há conta ou senha padrão.

| Variável | Uso |
| --- | --- |
| `POSTGRES_DB` | Nome do banco, padrão `mercadinho` |
| `POSTGRES_USER` | Usuário local do banco, padrão `mercadinho` |
| `POSTGRES_PASSWORD` | Senha obrigatória do banco |
| `DB_HOST` | Host do banco na execução Java local; no contêiner é `postgres` |
| `DB_PORT` | Porta do banco no computador, padrão 5432; internamente o contêiner usa 5432 |
| `APP_PORT` | Porta HTTP do Compose no computador, padrão 8080 |
| `PORT` | Porta HTTP da execução Java fora do Compose |
| `SPRING_PROFILES_ACTIVE` | `default` localmente; `prod` na publicação HTTPS |

As portas do Compose são vinculadas a `127.0.0.1`. Para parar sem apagar dados, execute `docker compose --profile app stop`. `docker compose --profile app down` remove os contêineres e a rede, mantendo o volume do PostgreSQL. Não remova o volume se precisar preservar o banco.

O healthcheck da aplicação confirma que o servidor responde à página de login. Ele não constitui uma consulta contínua de integridade do banco. A aplicação espera o PostgreSQL ficar saudável antes da inicialização; o Flyway aplica as migrações antes de servir as APIs.

## Publicação por HTTPS

Publique uma única instância da aplicação Java e um PostgreSQL persistente. Configure o domínio e o certificado HTTPS no proxy reverso ou na hospedagem, encaminhando as requisições à porta da aplicação. Não publique a porta do PostgreSQL para a internet.

Ative `SPRING_PROFILES_ACTIVE=prod`. Esse perfil configura o cookie de sessão como Secure, além de HttpOnly e SameSite=Lax. O endereço HTTP local não deve ser usado para testar login com esse perfil, pois o navegador não enviará o cookie Secure por HTTP.

Se o proxy encerra TLS, configure `SERVER_FORWARD_HEADERS_STRATEGY=framework` somente quando o acesso direto ao servidor estiver restrito ao proxy confiável. Assim, o Spring reconhece o protocolo HTTPS original. Confirme no navegador o certificado, os atributos do cookie e o encerramento efetivo da sessão no logout.

Para PostgreSQL gerenciado externo, a aplicação também aceita `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`. Use uma URL JDBC e as opções de TLS exigidas pelo provedor; configure verificação do certificado para conexão pública. Não inclua a senha na URL nem no código.

As sessões ficam na memória da instância e expiram após 30 minutos de inatividade. Reiniciar a aplicação exige novo login. A recuperação de uma tentativa de movimentação pode continuar na mesma aba depois do login, pois a prevenção de duplicidade fica no PostgreSQL. Distribuição de sessões entre várias réplicas e funcionamento offline não fazem parte deste MVP.

Não foi realizada publicação em hospedagem externa nesta entrega. Os testes de Docker foram locais. O perfil de produção e os controles locais não comprovam, sozinhos, a configuração HTTPS de um servidor publicado.

## Atualizações e cópia de segurança

Antes de atualizar um ambiente com dados reais, faça uma cópia de segurança e confira sua restauração em outro banco. Execute os testes, atualize o código e reconstrua a imagem com `docker compose --profile app up -d --build --wait`. O Flyway executará somente migrações novas; não edite migrações já aplicadas. O Hibernate está configurado para validar o esquema, sem recriá-lo.

Exemplo de backup local no PowerShell:

```powershell
New-Item -ItemType Directory -Path backups -Force | Out-Null
$arquivoBackup = 'backups/mercadinho-' + (Get-Date -Format yyyyMMdd-HHmmss) + '.dump'
docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/mercadinho.dump'
docker compose cp postgres:/tmp/mercadinho.dump $arquivoBackup
```

A pasta `backups/` e arquivos `.dump` são ignorados pelo Git. Mantenha cópias também fora do computador do servidor, com acesso restrito. Para restaurar, copie o arquivo para um ambiente de teste, crie um banco novo e vazio e use `pg_restore --no-owner --no-privileges` apontando para esse novo banco. Não utilize opções de limpeza sobre o banco original.

O procedimento de backup/restauração está documentado, mas o teste de restauração não foi executado nesta sessão por falta de autorização. Não há política automática de backup configurada.

Referências: [Maven Wrapper](https://maven.apache.org/tools/wrapper/), [imagem Maven](https://hub.docker.com/_/maven), [Eclipse Temurin](https://hub.docker.com/_/eclipse-temurin).
