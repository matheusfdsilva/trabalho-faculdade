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
└── prototipo/
    └── .gitkeep
```

A pasta `prototipo/` está reservada para as imagens das telas. Quando disponíveis, adicione os arquivos:

- `tela-1-inicial.png`
- `tela-2-movimentacao.png`
- `tela-3-resultado.png`

## Desenvolvimento incremental

A base Java 21/Spring Boot está criada. O [plano de implementação](docs/PLANO.md) organiza o trabalho em dez commits adicionais ao commit da estrutura inicial, com requisitos e critérios de validação extraídos dos documentos acadêmicos.

As funcionalidades de estoque, o banco de dados e a autenticação serão implementados nas próximas etapas. Esta versão ainda não é o MVP funcional.

## Execução da base técnica

Pré-requisitos: JDK 21 e Maven 3.6.3 ou superior. Confira se `JAVA_HOME` aponta para o JDK 21.

```sh
mvn verify
mvn spring-boot:run
```

A aplicação utiliza a porta 8080 por padrão, configurável pela variável `PORT`. Nesta etapa não há telas nem endpoints de negócio; uma resposta 404 em `/` é esperada. O PostgreSQL e as instruções de configuração serão adicionados na etapa 2.

## Cuidados com o repositório público

Não envie senhas, tokens, credenciais de banco de dados ou dados pessoais reais. As configurações sensíveis deverão ser fornecidas por variáveis de ambiente.

Para atender à avaliação acadêmica, o repositório remoto deverá ter visibilidade **pública**. Nome sugerido: `mercadinho-cerrado-mvp`.
