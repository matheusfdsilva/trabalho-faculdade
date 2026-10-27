import { api, csrf, notice, loginRequired } from './api.js';

const $ = selector => document.querySelector(selector);
const form = $('#movimento-form');
const dates = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Sao_Paulo' });
let product = null, pending = null, result = null, pendingKey, resultKey;
let sending = false, productRequest = 0, searchRequest = 0;
let historyPage = 0, historyPages = 0, historyRequest = 0, historyController;

function read(key) {
  try { return JSON.parse(sessionStorage.getItem(key) || 'null'); }
  catch { return null; }
}

function lockForm() {
  const locked = sending || pending !== null;
  for (const id of ['produto', 'tipo', 'quantidade', 'nome-busca', 'buscar-produto']) $(`#${id}`).disabled = locked;
  $('#confirmar').textContent = sending ? 'Confirmando…' : pending ? 'Reenviar confirmação' : 'Confirmar movimentação';
  preview();
}

function preview() {
  const quantity = Number($('#quantidade').value);
  const validQuantity = $('#quantidade').value !== '' && Number.isInteger(quantity) && quantity > 0 && quantity <= 2147483647;
  const balance = product && validQuantity ? product.quantidadeEstoque + (form.elements.tipo.value === 'ENTRADA' ? quantity : -quantity) : null;
  const validBalance = balance !== null && balance >= 0 && balance <= 2147483647;
  $('#previa-produto').textContent = product?.nome || 'Nenhum selecionado';
  $('#previa-atual').textContent = product ? `${product.quantidadeEstoque} un.` : '—';
  $('#previa-minimo').textContent = product ? `${product.estoqueMinimo} un.` : '—';
  $('#estoque-atual').textContent = product ? `Estoque atual: ${product.quantidadeEstoque} unidades · Mínimo: ${product.estoqueMinimo}` : 'Selecione um produto para consultar o saldo.';
  $('#previa-saldo').textContent = validBalance ? balance : '—';
  let message = 'Informe a quantidade para ver a previsão.';
  if (balance !== null && balance < 0) message = 'Quantidade superior ao estoque disponível.';
  else if (balance > 2147483647) message = 'A entrada ultrapassa o limite de unidades permitido.';
  else if (validBalance) message = balance <= product.estoqueMinimo ? '⚠ Reposição necessária após esta operação.' : '✓ O saldo previsto fica acima do estoque mínimo.';
  if (pending) message = 'Há uma tentativa sem confirmação. Reenviar usa a mesma operação e não duplica o estoque.';
  $('#previa-aviso').textContent = message;
  $('#saldo-no-form').textContent = validBalance ? `Saldo previsto: ${balance} unidades. ${message}` : message;
  $('#confirmar').disabled = sending || (!pending && (!product || !validQuantity || !validBalance));
  $('#historico-produto').disabled = !product;
}

async function selectProduct(id) {
  const request = ++productRequest;
  product = null; preview();
  if (!id) { if ($('#historico-produto').checked) loadHistory(true); return; }
  try {
    const selected = await api(`/api/produtos/${encodeURIComponent(id)}`);
    if (request !== productRequest) return;
    product = selected;
    if (![...$('#produto').options].some(option => option.value === String(selected.id))) {
      $('#produto').add(new Option(selected.nome, selected.id));
    }
    $('#produto').value = selected.id; preview();
    if ($('#historico-produto').checked) loadHistory(true);
  } catch (error) { if (!loginRequired(error)) notice($('#erro-movimento'), error.message, true, true); }
}

async function searchProducts() {
  if (pending || sending) return;
  const request = ++searchRequest;
  $('#buscar-produto').disabled = true;
  $('#estado-busca').textContent = 'Buscando produtos…';
  try {
    const params = new URLSearchParams({ nome: $('#nome-busca').value.trim(), tamanho: 100 });
    const data = await api(`/api/produtos?${params}`);
    if (request !== searchRequest || pending || sending) return;
    $('#produto').replaceChildren(new Option('Selecione um produto', ''));
    for (const item of data.itens) $('#produto').add(new Option(item.nome, item.id));
    if (product) {
      if (![...$('#produto').options].some(option => option.value === String(product.id))) $('#produto').add(new Option(product.nome, product.id));
      $('#produto').value = product.id;
    }
    $('#estado-busca').textContent = data.totalItens === 0 ? 'Nenhum produto encontrado. Cadastre-o no estoque ou tente outro nome.' : data.totalItens > 100 ? `Exibindo os primeiros 100 de ${data.totalItens} produtos. Refine a busca pelo nome para localizar os demais.` : `${data.totalItens} produto(s) encontrado(s). Escolha no campo abaixo.`;
  } catch (error) { if (!loginRequired(error)) $('#estado-busca').textContent = `${error.message} Use Buscar para tentar novamente.`; }
  finally { if (request === searchRequest) $('#buscar-produto').disabled = sending || pending !== null; }
}

function clearErrors() {
  notice($('#erro-movimento'), '');
  for (const input of form.querySelectorAll('input, select')) input.removeAttribute('aria-invalid');
  $('#erro-quantidade').textContent = ''; $('#erro-produto').textContent = '';
}

function showResult(saved, focus = true) {
  result = saved;
  const data = saved.data;
  $('#registro').hidden = true; $('#resultado').hidden = false; $('#historico').hidden = true;
  $('#resultado-referencia').textContent = `Registro nº ${data.id} · ${dates.format(new Date(data.dataMovimentacao))} (Brasília)`;
  const details = $('#resultado-dados'); details.replaceChildren();
  for (const [label, value] of [['Produto', saved.nome], ['Operação', data.tipo === 'ENTRADA' ? 'Entrada' : 'Saída'], ['Quantidade movimentada', `${data.quantidade} unidades`], ['Estoque anterior', `${data.saldoAnterior} unidades`], ['Estoque mínimo', `${data.estoqueMinimo} unidades`]]) {
    const group = document.createElement('div'); const term = document.createElement('dt'); const description = document.createElement('dd');
    term.textContent = label; description.textContent = value; group.append(term, description); details.append(group);
  }
  $('#resultado-saldo').textContent = data.saldoAtual;
  notice($('#resultado-alerta'), data.reposicaoNecessaria ? '⚠ Reposição necessária: estoque no mínimo ou abaixo do mínimo cadastrado.' : '✓ Estoque acima do mínimo. Não há indicação de reposição para este produto nesta operação.');
  $('#resultado-alerta').classList.toggle('warning', data.reposicaoNecessaria === true);
  document.title = 'Resultado da movimentação · Mercadinho Cerrado';
  if (focus) { $('#titulo-resultado').focus({ preventScroll: true }); window.scrollTo(0, 0); }
}

async function submit(event) {
  event.preventDefault(); if (sending) return;
  clearErrors();
  if (!pending) {
    if (!product || !form.reportValidity() || $('#confirmar').disabled) return;
    const next = { nome: product.nome, produto: product, request: { idOperacao: crypto.randomUUID(), produtoId: product.id, tipo: $('#tipo').value, quantidade: Number($('#quantidade').value) } };
    try { sessionStorage.setItem(pendingKey, JSON.stringify(next)); }
    catch { notice($('#erro-movimento'), 'Não foi possível guardar a tentativa nesta aba. Habilite o armazenamento do navegador e tente novamente. Nenhuma movimentação foi enviada.', true, true); return; }
    pending = next;
  }
  sending = true; lockForm();
  try {
    const data = await api('/api/movimentacoes', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(pending.request) });
    const saved = { nome: pending.nome, data };
    // O resultado somente é exibido após a confirmação do servidor.
    sessionStorage.setItem(resultKey, JSON.stringify(saved));
    sessionStorage.removeItem(pendingKey); pending = null;
    result = saved; location.hash = 'resultado'; showResult(saved);
  } catch (error) {
    if (loginRequired(error)) return;
    if ([400, 404, 409].includes(error.status)) {
      sessionStorage.removeItem(pendingKey); pending = null;
      for (const field of error.fields || []) {
        const input = form.elements[field.campo]; const hint = field.campo === 'produtoId' ? $('#erro-produto') : field.campo === 'quantidade' ? $('#erro-quantidade') : null;
        if (input && hint) { input.setAttribute('aria-invalid', 'true'); hint.textContent = field.mensagem; }
      }
      if (product) await selectProduct(product.id);
    }
    notice($('#erro-movimento'), pending ? `${error.message} A operação pode ter sido registrada. Use Reenviar confirmação para consultar ou concluir a mesma tentativa, sem duplicar.` : error.message, true, true);
  } finally { sending = false; lockForm(); }
}

function historyCell(row, label, value) {
  const cell = document.createElement('td'); cell.dataset.label = label; cell.textContent = value; row.append(cell);
}

async function loadHistory(reset = false) {
  if (reset) historyPage = 0;
  historyController?.abort(); historyController = new AbortController(); const request = ++historyRequest;
  $('#historico-lista').setAttribute('aria-busy', 'true'); $('#historico-tabela').hidden = true;
  $('#historico-estado').hidden = false; $('#historico-estado').textContent = 'Carregando histórico…';
  $('#historico-anterior').disabled = true; $('#historico-proxima').disabled = true;
  try {
    const params = new URLSearchParams({ pagina: historyPage, tamanho: 20 });
    if ($('#historico-tipo').value) params.set('tipo', $('#historico-tipo').value);
    if ($('#historico-produto').checked && product) params.set('produtoId', product.id);
    const data = await api(`/api/movimentacoes?${params}`, { signal: historyController.signal });
    if (request !== historyRequest) return;
    historyPages = data.totalPaginas; $('#historico-itens').replaceChildren();
    for (const item of data.itens) {
      const row = document.createElement('tr');
      historyCell(row, 'Produto', item.produtoNome); historyCell(row, 'Operação', item.tipo === 'ENTRADA' ? '↓ Entrada' : '↑ Saída');
      historyCell(row, 'Quantidade', `${item.quantidade} un.`); historyCell(row, 'Data e hora (Brasília)', dates.format(new Date(item.dataMovimentacao)));
      historyCell(row, 'Saldo após', item.saldoAtual === null ? 'Não registrado' : `${item.saldoAtual} un.`);
      $('#historico-itens').append(row);
    }
    $('#historico-tabela').hidden = data.itens.length === 0; $('#historico-estado').hidden = data.itens.length > 0;
    $('#historico-estado').textContent = 'Nenhuma movimentação encontrada para esta consulta.';
    $('#historico-resumo').textContent = data.totalItens ? `${historyPage * 20 + 1}–${historyPage * 20 + data.itens.length} de ${data.totalItens} registros · Página ${historyPage + 1} de ${historyPages}` : '0 registros';
    $('#historico-anterior').disabled = historyPage === 0; $('#historico-proxima').disabled = historyPage + 1 >= historyPages;
  } catch (error) {
    if (error.name !== 'AbortError' && request === historyRequest && !loginRequired(error)) {
      $('#historico-estado').textContent = `${error.message} Use Atualizar histórico para tentar novamente.`;
      $('#historico-resumo').textContent = '';
    }
  } finally { if (request === historyRequest) $('#historico-lista').setAttribute('aria-busy', 'false'); }
}

function showForm() {
  $('#registro').hidden = false; $('#resultado').hidden = true; $('#historico').hidden = false;
  document.title = 'Movimentações · Mercadinho Cerrado';
}

$('#busca-produto').addEventListener('submit', event => { event.preventDefault(); searchProducts(); });
$('#produto').addEventListener('change', () => { clearErrors(); selectProduct($('#produto').value); });
$('#quantidade').addEventListener('input', preview); $('#tipo').addEventListener('change', preview);
form.addEventListener('submit', submit);
$('#historico-filtros').addEventListener('submit', event => { event.preventDefault(); loadHistory(true); });
$('#historico-tipo').addEventListener('change', () => loadHistory(true));
$('#historico-produto').addEventListener('change', () => loadHistory(true));
$('#historico-anterior').addEventListener('click', () => { if (historyPage > 0) { historyPage--; loadHistory(); } });
$('#historico-proxima').addEventListener('click', () => { if (historyPage + 1 < historyPages) { historyPage++; loadHistory(); } });
async function newMovement() {
  history.replaceState(null, '', location.pathname); showForm(); clearErrors(); form.reset();
  await selectProduct(null); await searchProducts(); await loadHistory(true); $('#produto').focus();
}
$('#nova-movimentacao').addEventListener('click', newMovement);
window.addEventListener('hashchange', () => {
  if (location.hash === '#resultado' && result) showResult(result);
  else if (!location.hash && result && !pending) newMovement();
  else if (!location.hash) showForm();
});
$('#sair').addEventListener('click', async () => {
  if (sending) return; $('#sair').disabled = true;
  try { await api('/api/auth/logout', { method: 'POST' }); location.replace('/login.html'); }
  catch (error) { if (!loginRequired(error)) notice($('#mensagem'), error.message, true, true); }
  finally { $('#sair').disabled = false; }
});

try {
  const user = await api('/api/auth/me'); await csrf();
  pendingKey = `mercadinho.movimento.pendente.${user.login}`; resultKey = `mercadinho.movimento.resultado.${user.login}`;
  $('#usuario').textContent = user.login; $('.avatar').textContent = user.login[0].toUpperCase();
  $('#aplicacao').hidden = false; $('#iniciando').hidden = true;
  result = read(resultKey); pending = read(pendingKey);
  if (pending?.request && pending.produto) {
    product = pending.produto; $('#produto').add(new Option(pending.nome, pending.request.produtoId));
    $('#produto').value = pending.request.produtoId; $('#tipo').value = pending.request.tipo; $('#quantidade').value = pending.request.quantidade;
    notice($('#erro-movimento'), 'Há uma tentativa anterior sem confirmação. Reenvie a confirmação para obter o resultado da mesma operação.', true);
    $('#estado-busca').textContent = 'Produto preservado da tentativa anterior.'; lockForm();
  } else {
    pending = null; await searchProducts();
    const requested = new URLSearchParams(location.search).get('produto');
    if (requested && /^[1-9]\d*$/.test(requested)) await selectProduct(requested);
  }
  if (location.hash === '#resultado' && result?.data && !pending) showResult(result, false);
  else { showForm(); await loadHistory(); }
} catch (error) { if (!loginRequired(error, false)) $('#iniciando').textContent = `${error.message} Atualize a página para tentar novamente.`; }
