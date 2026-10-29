# Relatório de validação técnica e roteiro de aceite

Data: 27/10/2026. Este registro descreve a verificação técnica do desenvolvimento. Ele não representa aceite de um participante externo.

**Atualização de 29/10/2026:** a publicação gratuita com HTTPS no Render e PostgreSQL no Neon foi concluída e verificada. O endpoint `/health` respondeu `UP`, e o cadastro e uma saída de mercadoria foram exercitados no ambiente público. Consulte o [registro de deploy](DEPLOY.md). As referências à publicação pendente abaixo correspondem à situação histórica de 27/10; a medição no Android, a avaliação externa e as demais pendências não foram concluídas por este deploy.

## Requisitos e evidências

| Requisito | Implementação e verificação |
| --- | --- |
| RF01: cadastro de produtos | Cadastro validado, persistência PostgreSQL e formulário integrado; edição de dados sem alterar o saldo |
| RF02: consulta e saldos | Busca por nome, paginação de 20 itens, consulta individual e interface responsiva |
| RF03: entrada | Quantidade positiva, atualização transacional e bloqueio de overflow |
| RF04: saída | Rejeição de saldo insuficiente, concorrência protegida e histórico gravado na mesma transação |
| RF05: reposição | Estoque menor ou igual ao mínimo, filtro e alertas com texto e símbolo |
| RF06: histórico desejável | Consulta somente para leitura, filtros, paginação e preservação de resultados antigos |
| RNF01: acesso | Sessões, bcrypt custo 12, contas individuais, CSRF, logout, bloqueio após cinco falhas e cookie Secure no perfil prod; HTTPS externo ainda depende da publicação |
| RNF02: proteção dos dados | Validação no servidor, consultas parametrizadas, textContent, CSP, erros de banco sem detalhes internos e segredos fora do Git |
| RNF03: portabilidade/desempenho | JDK 21 no Windows e contêiner Linux verificados; medição exploratória abaixo de 3 s; teste no Android previsto no trabalho ainda pendente |
| RNF04: acessibilidade | Rótulos, foco, formulários, estados, diálogos e alertas implementados; larguras 320/720/1440 e ampliação de texto 200% verificadas no Chrome; validação externa e avaliação com tecnologia assistiva pendentes |

## Testes automatizados

Atualização de 28/10/2026: a suíte passou com **108 testes (11 unitários e 97 de integração)** e **98% de cobertura de instruções Java**. Consulte as [evidências e relatórios originais](evidencias/testes-2026-10-28/LEIA-ME.md). Os números abaixo registram a execução anterior da revisão final.

Com JDK 21 e Docker disponíveis:

```powershell
.\mvnw.cmd -B -ntp verify
```

No Linux/macOS: `./mvnw -B -ntp verify`. Foram aprovados **97 testes**, sem falhas, erros ou testes ignorados. Os relatórios reproduzíveis são gerados em `target/surefire-reports/`.

Os testes abrangem migrações, atualização da versão inicial com produtos e histórico existentes, validação de entradas, busca e paginação, exclusão protegida, transações, concorrência, reenvio de UUID, rollback em falha de gravação, mensagens seguras de erro e autenticação. A suíte utiliza PostgreSQL real via Testcontainers, separado do banco de desenvolvimento.

O workflow `.github/workflows/ci.yml` executa a suíte em push para `main` e pull requests. As ações oficiais são fixadas por hash, com permissão de leitura do repositório. A presença do workflow não equivale à confirmação de que uma execução remota específica foi aprovada; consulte a aba Actions do GitHub.

## Verificações no navegador e no Docker

Foram exercitados no Chrome os fluxos de login válido e inválido, cadastro, edição, exclusão permitida e bloqueada, busca, reposição, paginação, entrada, saída, resultado, histórico e logout. As capturas em `prototipo/` utilizam dados fictícios.

No teste de resposta perdida, a saída foi gravada antes da interrupção da resposta. A página foi recarregada, recuperou a tentativa e reenviou o mesmo UUID. O histórico manteve uma única operação e o saldo não foi debitado novamente.

O Dockerfile foi construído e o perfil `app` do Compose iniciou aplicação e PostgreSQL, com healthchecks saudáveis. A criação administrativa de conta foi executada dentro do contêiner. O conjunto de desempenho foi preparado com exatamente 300 produtos e 1.000 movimentações.

## Desempenho exploratório

Foi utilizado Chrome 156 no Windows, aplicação e PostgreSQL em contêineres Linux, viewport 390 × 844, cache desabilitado e desaceleração de CPU de 4×. A rede foi limitada a 1,6 Mbps de download, 750 Kbps de upload e 150 ms de latência. Após um aquecimento, foram medidos cinco carregamentos até a apresentação dos 20 produtos e atualização da listagem.

| Medição | Tempo |
| --- | --- |
| 1 | 1.234 ms |
| 2 | 1.233 ms |
| 3 | 1.217 ms |
| 4 | 1.183 ms |
| 5 | 1.217 ms |
| Mediana | **1.217 ms (1,217 s)** |

O [registro JSON](evidencias/desempenho-exploratorio.json) contém o resultado e as condições. O tempo ficou abaixo da referência neste ambiente. **Não foi utilizado um smartphone Android real com pelo menos 3 GB de RAM**; portanto, essa medição não comprova o atendimento integral do RNF03.

Para preparar outra medição, use um banco exclusivamente de teste, com migrações aplicadas e sem produtos ou movimentações. `scripts/dados-desempenho.sql` gera o conjunto e recusa bancos com catálogo/histórico preenchidos. Copie o script para o contêiner PostgreSQL e execute `psql -v ON_ERROR_STOP=1 -f arquivo.sql` com a conta desse banco. Não use o banco do parceiro.

O script opcional `scripts/medir-carregamento.cjs` exige Node.js, Google Chrome e o pacote `playwright`. É possível instalar o pacote localmente para esse teste com `npm install --no-save --package-lock=false playwright`; Node.js não é uma dependência da aplicação. Defina `BASE_URL`, `TEST_USER` e `TEST_PASSWORD` no ambiente de teste, sem registrar a senha no histórico do terminal, e execute `node scripts/medir-carregamento.cjs`. O relatório sai em `target/desempenho-exploratorio.json`. Esse script também é exploratório em desktop.

Para comprovar o requisito do trabalho, repita as cinco medições no Android previsto, registre modelo, RAM, Chrome, rede, aquecimento e tempos e preserve as capturas do DevTools. Não misture resultados de ambientes diferentes para calcular a mediana.

## Roteiro de aceite externo

Solicite consentimento, utilize dados fictícios e mantenha identificação e registros pessoais do participante fora do repositório público. Apresente exatamente as três tarefas do [guia de movimentações](MOVIMENTACOES.md), sem indicar onde clicar.

| Tarefa | Sucesso sem ajuda | Tempo | Erros/ajuda solicitada |
| --- | --- | --- | --- |
| Localizar como registrar uma saída | A preencher | A preencher | A preencher |
| Registrar saída de 2 unidades, de 6 para 4 | A preencher | A preencher | A preencher |
| Interpretar saldo, reposição e voltar ao estoque | A preencher | A preencher | A preencher |

Registre também satisfação de 1 a 5, comentários, versão do sistema e decisão final. A referência é 3/3 tarefas sem ajuda, saída em até 120 segundos e satisfação mínima 4/5, sem falhas críticas. Não existe aceite externo registrado nesta entrega.

## Pendências fora da verificação local

- Publicação em hospedagem externa e verificação de HTTPS e cookies nesse ambiente.
- Medição no Android definido no RNF03 e avaliação externa com participante real.
- Teste de restauração de backup: execução não autorizada nesta sessão.
- Visibilidade pública do repositório: consulta externa não autorizada; confirmar manualmente no GitHub em Settings → General → Danger Zone.
- Conferência da execução do workflow na aba Actions e revisão periódica das dependências antes de uso publicado.

A implementação e sua documentação estão concluídas. Essas pendências não devem ser apresentadas como testes aprovados ou resultados de validação obtidos.
