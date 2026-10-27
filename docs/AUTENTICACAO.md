# Acesso ao sistema

As APIs de produtos e movimentações exigem uma sessão autenticada. Todos os usuários cadastrados possuem o mesmo perfil operacional. Não existe cadastro público nem conta padrão.

## Criar uma conta

Com o PostgreSQL iniciado e o `.env` configurado, execute `.\mvnw.cmd package -DskipTests` no PowerShell (ou `./mvnw package -DskipTests` no Linux/macOS). Usando Java 21:

```powershell
$env:APP_USUARIO = Read-Host 'Login do funcionário'
$senhaProtegida = Read-Host 'Senha com pelo menos 12 caracteres' -AsSecureString
try {
    $env:APP_SENHA = [System.Net.NetworkCredential]::new('', $senhaProtegida).Password
    java -jar target/mercadinho-cerrado-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --app.criar-usuario=true
} finally {
    Remove-Item Env:APP_SENHA -ErrorAction SilentlyContinue
    Remove-Item Env:APP_USUARIO -ErrorAction SilentlyContinue
    $senhaProtegida.Dispose()
}
```

O comando cria a conta e encerra a aplicação, sem abrir um servidor HTTP. Execute-o novamente com outro login para cadastrar outro funcionário. O login aceita de 3 a 80 caracteres (letras sem acento, números, ponto, hífen e sublinhado), começa com letra ou número e é normalizado para minúsculas. Logins existentes não são sobrescritos. A senha deve ter pelo menos 12 caracteres e até 72 bytes em UTF-8, limite do bcrypt. Apenas hashes bcrypt com salt individual e custo 12 são persistidos. Não salve `APP_SENHA` no repositório nem a forneça por argumento de linha de comando.

## Login pela API

Inicie a aplicação com `.\mvnw.cmd spring-boot:run` no PowerShell (ou `./mvnw spring-boot:run` no Linux/macOS) e acesse `http://localhost:8080`. A tela de login solicita usuário e senha e, após a autenticação, apresenta o estoque. O botão Sair encerra a sessão. Se a sessão expirar, a interface solicita um novo login. Para executar tudo em Docker, siga [IMPLANTACAO.md](IMPLANTACAO.md).

Para utilizar diretamente a API pelo PowerShell, inicie uma sessão HTTP:

```powershell
$csrf = Invoke-RestMethod http://localhost:8080/api/auth/csrf -SessionVariable sessao
$cabecalhos = @{}
$cabecalhos[$csrf.headerName] = $csrf.token
$login = Read-Host 'Login'
$senhaProtegida = Read-Host 'Senha' -AsSecureString
try {
    $credenciais = @{
        username = $login
        password = [System.Net.NetworkCredential]::new('', $senhaProtegida).Password
    }
    Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -WebSession $sessao -Headers $cabecalhos -ContentType 'application/x-www-form-urlencoded' -Body $credenciais
} finally {
    if ($credenciais) { $credenciais.Clear() }
    $senhaProtegida.Dispose()
}
# O login renova o token CSRF. Obtenha o novo token para alterações e logout.
$csrf = Invoke-RestMethod http://localhost:8080/api/auth/csrf -WebSession $sessao
$cabecalhos = @{}
$cabecalhos[$csrf.headerName] = $csrf.token
Invoke-RestMethod http://localhost:8080/api/auth/me -WebSession $sessao
Invoke-RestMethod http://localhost:8080/api/produtos -WebSession $sessao
```

Nos exemplos de cadastro, edição e movimentação do README, adicione `-WebSession $sessao -Headers $cabecalhos`. Consultas GET precisam apenas de `-WebSession $sessao`.

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/logout -WebSession $sessao -Headers $cabecalhos
```

O login aceita formulário com `username` e `password`, não JSON. Login e logout retornam HTTP 200 quando concluídos. Credenciais inválidas, usuário inexistente ou conta temporariamente bloqueada retornam a mesma mensagem e HTTP 401. Cinco falhas por conta dentro de 15 minutos bloqueiam novas autenticações por 15 minutos a partir da quinta falha. O bloqueio é persistido no PostgreSQL e compartilhado entre sessões. Uma autenticação bem-sucedida zera a contagem; uma janela expirada reinicia a contagem de falhas. O bloqueio não encerra sessões anteriormente autenticadas.

As sessões expiram após 30 minutos de inatividade e têm o identificador renovado no login. O logout invalida a sessão e remove o cookie. Não há HTTP Basic nem autenticação por token permanente. Operações sem autenticação retornam 401; requisições que alteram dados sem um token CSRF válido retornam 403, inclusive login e logout.

## Publicação

Publique exclusivamente por HTTPS e inicie com o perfil `prod` (`SPRING_PROFILES_ACTIVE=prod`). Esse perfil exige cookies de sessão Secure, além de HttpOnly e SameSite=Lax. HTTP local utiliza o perfil padrão. Se houver proxy reverso terminando HTTPS, configure o encaminhamento seguro de acordo com a hospedagem e restrinja o acesso direto à aplicação. As credenciais nunca devem trafegar por HTTP no ambiente publicado.

O sistema aplica uma Content Security Policy que permite scripts e estilos locais, sem código inline, além das proteções padrão de cabeçalhos do Spring Security. As próximas telas devem carregar JavaScript e CSS em arquivos separados.

Referências: [CSRF no Spring Security](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) e [sessões e autenticação](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).
