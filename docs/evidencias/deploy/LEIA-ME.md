# Evidências reais da publicação — 29/10/2026

Aplicação pública: https://mercadinho-cerrado-mvp.onrender.com

As imagens foram capturadas diretamente das interfaces do navegador, sem alterar os resultados:

- `01-aplicacao-publicada.jpg`: estoque autenticado no endereço público, com quatro produtos de demonstração persistidos no Neon, antes da movimentação.
- `02-render-deploy-live.jpg`: confirmação **Deploy succeeded | Live** no Render; duração 3 min 13 s, revisão `9da63e5`.
- `03-health-up.jpg`: resposta real `{"status":"UP"}` no endpoint público de saúde.
- `04-movimentacao-publicada.jpg`: resultado da saída registrada pela interface hospedada, reduzindo o saldo do óleo de 6 para 4 unidades, com alerta de reposição.
- `monitoramento-render.jpg`: configuração efetiva do Health Check Path `/health`.
- `notificacoes-falha.jpg`: configuração de notificações do serviço.
- `ci-versao-publicada.jpg`: pipeline aprovada da revisão publicada.
- `configuracao-render.jpg`: formulário de preparação do plano Free; é evidência de configuração, não de conclusão do deploy.
- `verificacao-publica.json`: resultado das verificações por HTTPS, autenticação e persistência dos produtos. A saída foi verificada separadamente pela interface, conforme a quarta imagem.

O projeto e o banco foram criados nos planos Free. Limites gratuitos e suspensão por inatividade continuam aplicáveis. O monitoramento configurado é interno ao Render; não foi testada a entrega de um e-mail de incidente nem implementado um serviço externo independente de uptime. O teste do estado DOWN foi executado isoladamente no teste unitário, sem derrubar a aplicação publicada.

Credenciais não estão incluídas nas evidências. Consulte `../../DEPLOY.md` para o registro técnico e as limitações das verificações.
