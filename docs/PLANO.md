# Plano de implementação

Baseado nas Experiências Práticas I e II fornecidas pelo autor. O Mercadinho Cerrado é o estabelecimento fictício do estudo de caso, vinculado ao ODS 8. Os documentos acadêmicos e dados pessoais dos participantes não serão copiados para o repositório público.

## Entregas e commits

Cada etapa deverá resultar em um commit com mudanças verificáveis. A estrutura inicial já possui um commit; as dez etapas abaixo serão adicionais.

1. `build: iniciar aplicacao Java 21 com Spring Boot` — projeto Maven, ponto de entrada, configuração e plano.
2. `infra: configurar PostgreSQL e migracoes` — Docker Compose, variáveis de ambiente e esquema versionado.
3. `feat: implementar cadastro de produtos` — entidade, validação e criação de produtos (RF01).
4. `feat: consultar e manter produtos` — busca, paginação de 20 itens, edição e exclusão protegida (RF02).
5. `feat: registrar movimentacoes transacionais` — entradas e saídas atômicas, concorrência e bloqueio de saldo negativo (RF03/RF04).
6. `feat: consultar reposicao e historico` — estoque menor ou igual ao mínimo (RF05) e histórico somente leitura (RF06, desejável).
7. `feat: proteger acesso com sessoes e bcrypt` — contas individuais, bcrypt custo 12, CSRF, logout e bloqueio após cinco falhas em 15 minutos.
8. `feat: criar interface de estoque e produtos` — login, listagem, busca e formulário responsivos.
9. `feat: criar fluxo de movimentacao e resultado` — seleção de produto, saldo previsto, confirmação e alerta textual.
10. `test: validar MVP e documentar execucao` — testes das regras e APIs, instruções de implantação e roteiro manual de acessibilidade e desempenho.

## Decisões técnicas

- Java 21, Spring Boot 3.x, PostgreSQL e HTML/CSS/JavaScript sem framework frontend.
- Monólito em camadas: controllers, services e repositories. O frontend acessa apenas as APIs.
- Produto: nome, categoria, preço decimal, quantidade inteira não negativa e estoque mínimo.
- Movimentação: produto, tipo ENTRADA/SAIDA, quantidade positiva e data gerada pelo servidor. Histórico imutável pela interface.
- Após o cadastro inicial, alterar o saldo somente por movimentações; edição de produto altera seus dados descritivos e mínimo.
- Restringir a exclusão de produtos com histórico. Evitar registros duplicados em reenvios da mesma operação.
- Contas provisionadas pelo administrador, sem cadastro público. Segredos por ambiente; HTTPS na publicação.
- Não incluir emissão fiscal, pagamentos, finanças avançadas ou integrações com fornecedores.

## Critérios de validação

- Cenário principal: Óleo de Soja 900 ml com saldo 6 e mínimo 5; saída de 2 resulta em saldo 4 e alerta de reposição.
- Rejeitar saída maior que o saldo, quantidades inválidas e operações sem autenticação, preservando os registros.
- Interface a partir de 320 px, controles de pelo menos 44 px, teclado, foco visível, zoom de 200%, rótulos persistentes e alertas com texto e símbolo. Contraste: 4,5:1 em textos e 3:1 em componentes essenciais.
- Desempenho a medir: mediana de cinco carregamentos após aquecimento em até 3 s, com 300 produtos, 1.000 movimentações e primeira página de 20 produtos. Rede: 1,6 Mbps download, 750 Kbps upload, latência 150 ms.
- Validação externa: três tarefas independentes, 100% de sucesso, saída em até 120 s e satisfação mínima 4/5. Não registrar aprovação ou medições sem executar os testes.
- O documento II apresenta um roteiro preliminar com quatro tarefas; para validação, adotar o roteiro posterior de exatamente três tarefas e seus critérios integrados.

## Estado atual

Etapas 1 a 3 concluídas: base técnica, configuração PostgreSQL por ambiente, Docker Compose, primeira migração Flyway e cadastro de produtos (`POST /api/produtos`). O cadastro utiliza entidade JPA, repository, service transacional, DTOs e validação no servidor, com mensagens de erro por campo. A suíte passa com 29 testes, incluindo persistência real e rejeição de entradas inválidas sem gravação. A próxima etapa será consulta paginada, edição e exclusão protegida. Interface e autenticação continuam previstas para as etapas posteriores; esta versão ainda não constitui o MVP funcional.
