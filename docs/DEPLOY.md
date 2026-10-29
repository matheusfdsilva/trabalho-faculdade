# Publicação gratuita — Render e Neon

O projeto contém `render.yaml` com serviço Docker no plano Free, região Ohio e endpoint de saúde `/health`. A criação pelo formulário do Render deve repetir esses valores; o arquivo não é aplicado automaticamente nesse modo.

## Configuração

No Neon, criar PostgreSQL 16 dedicado e manter o plano Free. No Render, usar o repositório público, branch `main` e o Dockerfile da raiz. Definir estas variáveis protegidas, sem gravar credenciais no Git:

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL`: URL JDBC do Neon com TLS e validação do certificado.
- `SPRING_DATASOURCE_USERNAME`: usuário do banco.
- `SPRING_DATASOURCE_PASSWORD`: senha do banco.
- `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=65 -XX:+ExitOnOutOfMemoryError`

O perfil de produção exige cookie Secure, reconhece o proxy HTTPS e limita o pool do banco a três conexões, sem conexões mínimas ociosas. O schema é atualizado pelo Flyway na inicialização. Criar a conta da aplicação pelo comando administrativo documentado em `AUTENTICACAO.md`; não existe conta pública padrão.

O deploy deve utilizar uma revisão aprovada na pipeline. Quando o serviço está vinculado ao provedor Git, `After CI Checks Pass` automatiza essa condição. A conexão por URL pública pode exigir publicação manual; confirmar o comportamento no painel.

## Monitoramento básico

Configurar o **Health Check Path** do Render como `/health`. A rota consulta `SELECT 1` no PostgreSQL e responde HTTP 200 com `{"status":"UP"}` ou HTTP 503 com `{"status":"DOWN"}` em falhas de acesso ao banco. A resposta não é armazenada em cache e não expõe credenciais, nomes de servidores ou mensagens internas. Apenas a consulta GET é pública; as APIs de negócio continuam autenticadas.

O Render verifica as instâncias em execução, interrompe o encaminhamento para uma instância persistentemente não saudável e pode reiniciá-la. Conferir eventos, logs e preferências de notificação no painel. Referência: https://render.com/docs/health-checks.

Essas verificações são internas ao provedor, não constituem monitoramento externo independente. No plano Free, a aplicação pode suspender por inatividade; o primeiro acesso exige inicialização e não há garantia de disponibilidade contínua. Respeitar as cotas gratuitas do Render e do Neon, sem adicionar recursos pagos.

## Verificação e evidências

Antes de declarar a publicação concluída, conferir o link HTTPS, `/health`, autenticação, persistência de produtos e uma movimentação no banco remoto. Salvar as capturas reais do painel de deploy, da interface pública e da saúde em `docs/evidencias/deploy/`. O arquivo de configuração e os testes locais, isoladamente, não comprovam que o deploy já foi realizado.
