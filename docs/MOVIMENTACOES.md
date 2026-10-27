# Movimentação de estoque pela interface

Após entrar, selecione **Registrar movimentação** no estoque ou acesse **Movimentações** no menu. A ação **Movimentar** de cada produto abre a página com esse produto selecionado.

1. Localize o produto pela busca e selecione-o. A busca apresenta até 100 opções; refine o nome se o catálogo for maior.
2. Escolha **Entrada de mercadoria** ou **Saída de mercadoria** e informe uma quantidade inteira positiva.
3. Confira o estoque atual, o mínimo e o saldo previsto. A interface impede a confirmação de uma saída maior que o saldo consultado e de entradas que excedam o limite de unidades.
4. Confirme a movimentação. O servidor confere novamente o saldo e grava a operação de forma transacional.
5. Verifique o resultado: produto, tipo, quantidade, saldo anterior, novo saldo e eventual necessidade de reposição. Use **Voltar ao estoque** ou **Nova movimentação** para continuar.

O saldo previsto é apenas uma orientação. Se outro funcionário movimentar o produto antes da confirmação, o servidor poderá rejeitar a saída e a interface consultará novamente o saldo.

## Falhas de conexão e reenvios

Antes de enviar, a interface guarda a tentativa e seu UUID no `sessionStorage` da aba, separado por usuário autenticado. Se houver uma falha sem confirmação definitiva, produto, tipo e quantidade ficam preservados e bloqueados para edição. O botão **Reenviar confirmação** envia o mesmo UUID e os mesmos dados. O servidor retorna o resultado original se já tiver gravado a operação.

Essa tentativa também é recuperada ao recarregar a página ou entrar novamente com a mesma conta na mesma aba. Não é feito reenvio automático. Rejeições definitivas por dados inválidos, produto inexistente ou conflito de saldo liberam o formulário para correção.

Não feche a aba nem limpe seus dados enquanto houver uma operação sem confirmação. O armazenamento é da aba: o sistema não recupera a tentativa em outro navegador ou dispositivo. Se perder essa informação, consulte o histórico e o estoque antes de registrar novamente a mesma saída ou entrada.

Depois da confirmação, o resultado fica disponível ao recarregar a tela de resultado na mesma aba. Ele representa o saldo na ocasião da operação; para conhecer o estoque atual, volte à consulta de produtos.

## Histórico

O histórico fica abaixo do formulário, é somente para leitura e apresenta 20 registros por página, da data mais recente para a mais antiga. É possível filtrar entradas ou saídas e marcar **Somente o produto selecionado** depois de escolher um produto no formulário. O botão **Atualizar histórico** refaz a consulta.

Datas e horários são apresentados no fuso de Brasília. Registros antigos sem saldo armazenado apresentam **Não registrado**, sem inventar um saldo com base no estoque atual. Não é possível editar ou excluir movimentações pela interface.

## Cenário para validação externa

Use dados fictícios e um produto com saldo inicial de 6 unidades e mínimo de 5. Apresente as tarefas sem explicar onde clicar:

1. A partir do estoque, encontre como registrar uma saída.
2. Registre uma saída de 2 unidades desse produto.
3. Identifique o saldo de 4 unidades, explique o alerta de reposição e retorne ao estoque.

Registre os tempos, erros e pedidos de ajuda. Os critérios definidos no trabalho são três tarefas concluídas sem ajuda, movimentação em até 120 segundos e satisfação mínima de 4/5. A execução técnica pelo desenvolvedor não constitui validação externa nem comprova essas metas.
