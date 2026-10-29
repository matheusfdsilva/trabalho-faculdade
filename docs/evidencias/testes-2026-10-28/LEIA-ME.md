# Testes e cobertura — 28/10/2026

Execução concluída às 20:22:10 (America/Sao_Paulo): **108 testes aprovados**, com zero falhas, erros ou testes ignorados. São 11 testes unitários de `Produto` e `Usuario`, sem Spring ou banco, e 97 testes de integração com Spring e PostgreSQL temporário via Testcontainers.

O tempo total do Maven foi **56,438 s**. A soma dos tempos das suítes no relatório Surefire foi **44,130 s**; o total do Maven também inclui preparação e geração dos relatórios.

## Valor para o formulário

**98** — cobertura de instruções do backend Java, conforme o percentual inteiro da linha Total do relatório JaCoCo 0.8.15.

- Instruções: 1.355 de 1.369 (98,9774%).
- Linhas: 242 de 245 (98,7755%).
- Desvios condicionais: 57 de 66 (86,3636%).
- Classes: 31 de 31.

Não foram configuradas exclusões de classes na medição. HTML, CSS e JavaScript não fazem parte da cobertura Java medida pelo JaCoCo.

## Imagens para anexar

- [Execução original com BUILD SUCCESS, quantidade de testes e tempo total](01-execucao-aprovada.png).
- [Relatório Surefire: 108 testes, 100% de aprovação e tempo das suítes](02-relatorio-testes.png).
- [Relatório JaCoCo com a cobertura total](03-cobertura-jacoco.png).

As imagens são capturas dos relatórios e do log reais exibidos no navegador, sem alteração dos resultados. O log da execução bem-sucedida está em [execucao-maven.txt](execucao-maven.txt). Os contadores originais estão em [jacoco.xml](jacoco.xml), [jacoco.csv](jacoco.csv) e [metricas.json](metricas.json). O arquivo [relatorios-originais.zip](relatorios-originais.zip) preserva os relatórios HTML com seus arquivos de apoio; após extrair, abra `surefire.html` e `jacoco/index.html`.

## Reproduzir

Com Java 21 e Docker disponíveis, execute na raiz do projeto:

```powershell
.\mvnw.cmd -B -ntp verify
```

Nesta máquina, foi usado também `-Dmaven.repo.local=.maven-cache` para reaproveitar o cache local. Não é necessário configurar `.env` nem iniciar o Compose: os testes de integração usam um banco temporário independente.

Os relatórios são gerados em `target/site/surefire.html` e `target/site/jacoco/index.html`, e os resultados JUnit em `target/surefire-reports/`. Uma execução futura pode ter tempos e percentuais diferentes caso o código seja alterado.
