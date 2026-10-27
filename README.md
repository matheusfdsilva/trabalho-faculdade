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

As APIs de estoque, a interface e a autenticação serão implementadas nas próximas etapas. Esta versão ainda não é o MVP funcional.

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

A aplicação utiliza a porta 8080 por padrão, configurável pela variável `PORT`. Nesta etapa não há telas nem endpoints de negócio; uma resposta 404 em `/` é esperada.

O PostgreSQL 16 fica acessível apenas no computador local, na porta definida por `DB_PORT` (padrão 5432). Se ela estiver ocupada, altere `DB_PORT` no `.env` antes de iniciar. O Compose mantém os dados em um volume persistente. Para parar os serviços sem apagar os dados, execute `docker compose down`.

## Migrações e testes

O Flyway executa as migrações em `src/main/resources/db/migration/` na inicialização e registra as versões aplicadas na tabela `flyway_schema_history`. A primeira migração cria `produto` e `movimentacao`, com chaves estrangeiras e restrições para quantidades, preço e tipo de operação. Produtos com movimentações não podem ser excluídos.

O Hibernate apenas valida o esquema (`ddl-auto=validate`); novas alterações deverão ser feitas por novas migrações, sem editar as já aplicadas. A senha e o usuário do `.env` são usados pelo PostgreSQL na criação inicial do volume; alterá-los depois exige atualizar a conta no banco existente.

```sh
mvn verify
```

Os testes utilizam Testcontainers para criar um PostgreSQL temporário e independente do banco de desenvolvimento. O Docker precisa estar disponível, mas não é necessário iniciar o Compose nem configurar `.env` para esses testes. Eles verificam reaplicação das migrações, bloqueio de saldo negativo, validação de movimentações e preservação do histórico. Nenhum teste substitui a futura regra transacional de atualização de saldo.

Referências: [imagem oficial PostgreSQL](https://hub.docker.com/_/postgres) e [suporte PostgreSQL no Flyway](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database).

## Cuidados com o repositório público

Não envie senhas, tokens, credenciais de banco de dados ou dados pessoais reais. As configurações sensíveis deverão ser fornecidas por variáveis de ambiente.

Para atender à avaliação acadêmica, o repositório remoto deverá ter visibilidade **pública**. Nome sugerido: `mercadinho-cerrado-mvp`.
