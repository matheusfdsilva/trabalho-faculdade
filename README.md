# Mercadinho Cerrado — MVP

Projeto acadêmico de um sistema para auxiliar o Mercadinho Cerrado no controle de estoque e no acompanhamento das movimentações de produtos.

## Objetivo

Desenvolver um produto mínimo viável (MVP) que permita registrar entradas e saídas de mercadorias, consultar o estoque e visualizar os resultados das movimentações.

## Funcionalidades previstas

- Cadastro e consulta de produtos.
- Registro de entradas e saídas de mercadorias.
- Consulta da quantidade disponível em estoque.
- Visualização dos resultados das movimentações.

## Tecnologias escolhidas

- Java 21
- Spring Boot
- PostgreSQL
- HTML, CSS e JavaScript

## Estrutura do repositório

```text
mercadinho-cerrado-mvp/
├── README.md
├── .gitignore
├── .env.example
├── compose.yaml
├── pom.xml
├── docs/
│   └── PLANO.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/
│   └── test/java/
└── prototipo/
    └── .gitkeep
```

A pasta `prototipo/` está reservada para as imagens das telas. Quando disponíveis, adicione os arquivos:

- `tela-1-inicial.png`
- `tela-2-movimentacao.png`
- `tela-3-resultado.png`

## Desenvolvimento incremental

A base Java 21/Spring Boot e a infraestrutura PostgreSQL/Flyway estão criadas. O [plano de implementação](docs/PLANO.md) organiza o trabalho em dez commits adicionais ao commit da estrutura inicial, com requisitos e critérios de validação extraídos dos documentos acadêmicos.

O cadastro, a consulta, a edição e a exclusão protegida de produtos via API estão implementados, assim como o registro transacional de entradas e saídas. Consulta de histórico, filtros de reposição, interface e autenticação serão adicionados nas próximas etapas. Esta versão ainda não é o MVP funcional.

## Execução da base técnica

Pré-requisitos: JDK 21, Maven 3.6.3 ou superior e Docker com Docker Compose. Confira se `JAVA_HOME` aponta para o JDK 21 e inicie o Docker Desktop.

Na raiz do projeto, copie `.env.example` para `.env` e preencha `POSTGRES_PASSWORD` com uma senha local. O arquivo real é ignorado pelo Git. Utilize valores simples sem aspas; uma senha aleatória hexadecimal é compatível com os dois leitores do arquivo. A aplicação importa `.env` como propriedades, e o Compose utiliza as mesmas variáveis para criar o banco. Variáveis de ambiente do processo têm precedência na aplicação.

No PowerShell:

```powershell
Copy-Item .env.example .env
notepad .env
```

No Linux/macOS, utilize `cp .env.example .env` e edite o arquivo no seu editor.

Em seguida:

```sh
docker compose up -d --wait
mvn spring-boot:run
```

A aplicação utiliza a porta 8080 por padrão, configurável pela variável `PORT`. Nesta etapa não há telas; uma resposta 404 em `/` é esperada. O cadastro pode ser utilizado pela API descrita abaixo.

O PostgreSQL 16 fica acessível apenas no computador local, na porta definida por `DB_PORT` (padrão 5432). Se ela estiver ocupada, altere `DB_PORT` no `.env` antes de iniciar. O Compose mantém os dados em um volume persistente. Para parar os serviços sem apagar os dados, execute `docker compose down`.

## Cadastro de produtos (RF01)

Envie `POST /api/produtos` com `Content-Type: application/json`:

```json
{
  "nome": "Óleo de Soja 900 ml",
  "categoria": "Alimentos",
  "preco": 8.50,
  "quantidadeEstoque": 6,
  "estoqueMinimo": 5
}
```

Exemplo no PowerShell, após iniciar o banco e a aplicação:

```powershell
$produto = @{
    nome = 'Óleo de Soja 900 ml'
    categoria = 'Alimentos'
    preco = 8.50
    quantidadeEstoque = 6
    estoqueMinimo = 5
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/produtos -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes($produto))
```

O retorno é HTTP **201**, com o identificador gerado e os dados gravados. Cada envio válido cria um novo produto; nomes repetidos são permitidos. Espaços nas extremidades de nome e categoria são removidos.

Todos os campos são obrigatórios. Nome e categoria aceitam, respectivamente, até 150 e 80 caracteres, sem marcações HTML ou caracteres de controle. O preço deve estar entre zero e 99.999.999,99, com até duas casas decimais. Quantidade e estoque mínimo devem ser inteiros entre zero e 2.147.483.647. Zero é aceito para preço e saldos. O saldo informado é o estoque inicial; futuras alterações serão feitas por movimentações.

Dados inválidos retornam HTTP **400**, sem cadastrar o produto. Erros de validação incluem `mensagem` e a lista `campos`, com o nome do campo e uma orientação. JSON malformado, campos desconhecidos ou tipos incompatíveis retornam uma mensagem geral sem detalhes internos.

A autenticação será implementada na etapa 7; o endpoint ainda não exige login nesta versão intermediária.

## Consulta e manutenção de produtos (RF02)

| Método | Rota | Resultado |
| --- | --- | --- |
| GET | `/api/produtos` | Lista paginada, com busca opcional por nome |
| GET | `/api/produtos/{id}` | Dados de um produto |
| PUT | `/api/produtos/{id}` | Atualiza nome, categoria, preço e estoque mínimo |
| DELETE | `/api/produtos/{id}` | Exclui apenas produtos sem histórico |

A listagem aceita `nome` (trecho do nome, até 150 caracteres, sem distinguir maiúsculas de minúsculas), `pagina` (inicia em 0, até 1.000.000) e `tamanho` (padrão 20, de 1 a 100). A ordenação é por nome e, em caso de empate, identificador. `%` e `_` são tratados como texto na busca. A resposta contém `itens`, `pagina`, `tamanho`, `totalItens` e `totalPaginas`. Páginas além do final retornam uma lista vazia.

```powershell
Invoke-RestMethod 'http://localhost:8080/api/produtos?nome=arroz&pagina=0&tamanho=20'
Invoke-RestMethod 'http://localhost:8080/api/produtos/1'
```

Na edição, envie todos os quatro campos abaixo. As validações são as mesmas do cadastro, mas `quantidadeEstoque` não é aceito: o saldo será alterado exclusivamente por movimentações. O retorno HTTP **200** inclui o produto atualizado.

```json
{
  "nome": "Arroz integral 1 kg",
  "categoria": "Grãos",
  "preco": 19.90,
  "estoqueMinimo": 10
}
```

A exclusão retorna HTTP **204**, sem corpo. Se houver movimentações, retorna **409** com uma mensagem explicativa, preservando o produto e o histórico. Consulta, edição e exclusão de um identificador inexistente retornam **404**. Identificadores não positivos ou parâmetros inválidos retornam **400**. Edição e exclusão utilizam bloqueio da linha dentro da transação, e a chave estrangeira mantém a proteção do histórico no banco.

## Entradas e saídas de estoque (RF03/RF04)

Envie `POST /api/movimentacoes` com `Content-Type: application/json`:

```json
{
  "idOperacao": "f08a338e-cbc4-4e41-8a9c-b7f88a65a237",
  "produtoId": 1,
  "tipo": "SAIDA",
  "quantidade": 2
}
```

Todos os campos são obrigatórios. O identificador do produto deve ser positivo; o tipo aceita `ENTRADA` ou `SAIDA`; a quantidade deve ser um inteiro positivo. Saídas não podem exceder o saldo e entradas não podem ultrapassar 2.147.483.647 unidades no produto. O servidor gera a data e o horário da operação.

O retorno HTTP **200** apresenta `id`, `idOperacao`, `produtoId`, `tipo`, `quantidade`, `dataMovimentacao`, `saldoAnterior`, `saldoAtual`, `estoqueMinimo` e `reposicaoNecessaria`. Esta última informação é verdadeira quando o saldo resultante é menor ou igual ao mínimo no momento da operação. No cenário do protótipo, saída de 2 unidades de um saldo 6, com mínimo 5, retorna saldo 4 e reposição necessária.

Gere um UUID novo para cada operação pretendida. Em caso de falha de conexão, reenvie exatamente os mesmos dados e UUID: a proteção é por produto e `idOperacao`, retornando o resultado original sem alterar novamente o estoque. Usar o mesmo identificador no mesmo produto com outro tipo ou quantidade retorna **409**. O saldo retornado em um reenvio é o resultado daquela operação; consulte o produto para obter o saldo atual após outras movimentações.

```powershell
$movimentacao = @{
    idOperacao = [guid]::NewGuid().ToString()
    produtoId = 1
    tipo = 'SAIDA'
    quantidade = 2
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/movimentacoes -ContentType 'application/json' -Body $movimentacao
# Para repetir uma tentativa, reutilize $movimentacao sem gerar outro UUID.
```

Dados inválidos retornam **400**, produto inexistente retorna **404** e saldo insuficiente, excesso de saldo ou reutilização incompatível do UUID retornam **409**. Essas rejeições não alteram saldo nem histórico. O registro e a atualização do saldo participam da mesma transação; uma falha de gravação desfaz a operação. O bloqueio de linha serializa operações concorrentes no mesmo produto. O endpoint ainda não exige login nesta versão intermediária.

## Migrações e testes

O Flyway executa as migrações em `src/main/resources/db/migration/` na inicialização e registra as versões aplicadas na tabela `flyway_schema_history`. A primeira migração cria `produto` e `movimentacao`, com chaves estrangeiras e restrições para quantidades, preço e tipo de operação. Produtos com movimentações não podem ser excluídos.

O Hibernate apenas valida o esquema (`ddl-auto=validate`); novas alterações deverão ser feitas por novas migrações, sem editar as já aplicadas. A senha e o usuário do `.env` são usados pelo PostgreSQL na criação inicial do volume; alterá-los depois exige atualizar a conta no banco existente.

A migração V2 acrescenta o identificador de operação e o resultado histórico (saldo anterior, saldo resultante e mínimo na ocasião), com unicidade por produto e operação. Registros anteriores são preservados com esses novos campos nulos, pois seus resultados originais não foram coletados.

```sh
mvn verify
```

Os testes utilizam Testcontainers para criar um PostgreSQL temporário e independente do banco de desenvolvimento. O Docker precisa estar disponível, mas não é necessário iniciar o Compose nem configurar `.env` para esses testes. Eles verificam reaplicação das migrações, bloqueio de saldo negativo, validação de movimentações e preservação do histórico. O cadastro é testado com MockMvc e persistência real, incluindo dados válidos, limites, campos ausentes, tipos inválidos, quantidades fracionadas e tentativa de injeção SQL tratada como texto. Nenhum teste substitui a futura regra transacional de atualização de saldo.

Referências: [imagem oficial PostgreSQL](https://hub.docker.com/_/postgres) e [suporte PostgreSQL no Flyway](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database).

A manutenção também é testada com PostgreSQL real: paginação sem sobreposição em nomes repetidos, busca, consulta individual, edição sem alteração de saldo, exclusão com e sem histórico, identificadores inexistentes e rejeição de parâmetros ou edições inválidas. Os testes de movimentação verificam entradas, saídas, estoque zerado, saldo insuficiente, overflow, reenvios, concorrência e rollback mediante falha simulada no banco. A suíte atual contém 71 testes.

## Cuidados com o repositório público

Não envie senhas, tokens, credenciais de banco de dados ou dados pessoais reais. As configurações sensíveis deverão ser fornecidas por variáveis de ambiente.

Para atender à avaliação acadêmica, o repositório remoto deverá ter visibilidade **pública**. Nome sugerido: `mercadinho-cerrado-mvp`.
