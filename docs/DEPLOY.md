# Publicação gratuita — Render e Neon

## Publicação verificada em 29/10/2026

- Aplicação: https://mercadinho-cerrado-mvp.onrender.com
- Saúde: https://mercadinho-cerrado-mvp.onrender.com/health — HTTP 200, `{"status":"UP"}` na conferência.
- Render: serviço `mercadinho-cerrado-mvp`, Docker, plano **Free**, região Ohio.
- Neon: projeto `mercadinho-cerrado`, plano **Free**, PostgreSQL 16, banco dedicado.
- Revisão publicada: `9da63e5b96d77dc4b71be4216642b4ef38664f7f`.
- CI aprovada: https://github.com/matheusfdsilva/trabalho-faculdade/actions/runs/40964117035 — duração 1 min 13 s.
- Primeiro deploy: **Deploy succeeded | Live**, iniciado às 19:15:18 e concluído às 19:18:31 (America/Sao_Paulo), duração 3 min 13 s.

Foram conferidos HTTPS, saúde do banco, rejeição HTTP 401 da consulta de produtos sem login, autenticação com CSRF, cookie Secure e cadastro/consulta de quatro produtos fictícios no PostgreSQL remoto. Pela interface pública, foi confirmada a saída de duas unidades de Óleo de Soja 900 ml: saldo anterior 6, saldo resultante 4 e alerta de reposição. A consulta adicional do histórico por script não foi executada após a autorização ser recusada; não faz parte das verificações concluídas.

O endpoint de saúde possui dois testes unitários aprovados para banco disponível e falha de acesso, sem exposição de detalhes internos. A execução local selecionada passou com 13 testes unitários; a revisão completa foi aprovada na CI indicada acima. As evidências anteriores de 108 testes e 98% de cobertura são da execução de 28/10, anterior ao novo módulo de saúde.

O Health Check Path `/health` e a opção **Only failure notifications** foram salvos no Render. Não foi provocada indisponibilidade no serviço publicado para testar a entrega de um alerta; não se afirma que um e-mail de falha tenha sido recebido.

Capturas e resultados: [evidências do deploy](evidencias/deploy/LEIA-ME.md). As credenciais de avaliação foram entregues em `.env.acesso`, arquivo local ignorado pelo Git, sem senha publicada neste documento.

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
