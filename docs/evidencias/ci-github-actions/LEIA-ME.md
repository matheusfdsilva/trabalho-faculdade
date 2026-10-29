# Evidência de integração contínua

Capturas reais do GitHub Actions, consultado em 28/10/2026.

- Workflow: `Validar MVP`, definido em `.github/workflows/ci.yml`.
- Execução: https://github.com/matheusfdsilva/trabalho-faculdade/actions/runs/40604261983
- Commit: `c12ce65d29fd392825339d1dee1dd1253da6abe6`, branch `main`.
- Disparo: push de 27/10/2026 às 20:45 (America/Sao_Paulo).
- Resultado: **Success**, duração total de **1 min 12 s**.
- Job `testes`: aprovado em **1 min 7 s**.
- O workflow configura Java 21 e executa `sh ./mvnw -B -ntp verify` para compilar e testar com PostgreSQL temporário via Testcontainers.

Anexe `01-pipeline-success.jpg` para mostrar o status verde e a duração. A imagem `02-job-testes-aprovado.jpg` mostra os detalhes do job e suas etapas aprovadas.

Esta execução corresponde à versão enviada em 27/10, anterior aos 11 novos testes unitários e à configuração JaCoCo adicionados localmente em 28/10. Não representa a execução desses novos arquivos na nuvem.
