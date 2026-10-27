import { api, csrf, notice, loginRequired } from './api.js';
const $ = selector => document.querySelector(selector);
const dialog = $('#produto-dialog');
const form = $('#produto-form');
const deleteDialog = $('#excluir-dialog');
const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
let page = 0, totalPages = 0, selectedId = null, deletingId = null, saving = false, deleting = false;
let query = '', onlyLow = false, requestId = 0, controller;
function cell(row, label, text, className) {
  const td = document.createElement('td'); td.dataset.label = label;
  if (text !== undefined) td.textContent = text;
  if (className) td.className = className;
  row.append(td); return td;
}
function render(products) {
  $('#produtos').replaceChildren();
  for (const product of products) {
    const row = document.createElement('tr');
    const nameCell = cell(row, 'Produto');
    const name = document.createElement('span'); name.className = 'product-name'; name.textContent = product.nome;
    const category = document.createElement('span'); category.className = 'product-category'; category.textContent = product.categoria;
    nameCell.append(name, category);
    cell(row, 'Preço', money.format(product.preco));
    cell(row, 'Em estoque', `${product.quantidadeEstoque} un.`, 'stock-number');
    cell(row, 'Mínimo', `${product.estoqueMinimo} un.`);
    const low = product.quantidadeEstoque <= product.estoqueMinimo;
    const status = document.createElement('span'); status.className = `badge ${low ? 'low' : 'normal'}`;
    status.textContent = low ? '⚠ Reposição necessária' : '✓ Normal'; cell(row, 'Situação').append(status);
    const actions = document.createElement('div'); actions.className = 'row-actions';
    for (const [label, action] of [['Movimentar', () => location.assign(`/movimentacoes.html?produto=${product.id}`)], ['Editar', () => openProduct(product.id)], ['Excluir', () => openDelete(product)]]) {
      const button = document.createElement('button'); button.type = 'button'; button.textContent = label;
      button.setAttribute('aria-label', `${label} ${product.nome}`); button.addEventListener('click', action); actions.append(button);
    }
    cell(row, 'Ações').append(actions); $('#produtos').append(row);
  }
}
async function load() {
  controller?.abort(); controller = new AbortController(); const currentRequest = ++requestId;
  $('#lista').setAttribute('aria-busy', 'true'); $('#anterior').disabled = true; $('#proxima').disabled = true;
  $('#estado-lista').hidden = false; $('#estado-lista').textContent = 'Carregando produtos…'; $('#tabela').hidden = true;
  try {
    const params = new URLSearchParams({ nome: query, reposicao: onlyLow, pagina: page, tamanho: 20 });
    const [result, total, low] = await Promise.all([
      api(`/api/produtos?${params}`, { signal: controller.signal }),
      api('/api/produtos?tamanho=1', { signal: controller.signal }),
      api('/api/produtos?reposicao=true&tamanho=1', { signal: controller.signal })
    ]);
    if (currentRequest !== requestId) return;
    totalPages = result.totalPaginas;
    if (!result.itens.length && page > 0 && page >= totalPages) { page = Math.max(0, totalPages - 1); return load(); }
    $('#total-produtos').textContent = total.totalItens; $('#total-reposicao').textContent = low.totalItens;
    render(result.itens); $('#tabela').hidden = !result.itens.length;
    $('#estado-lista').hidden = !!result.itens.length;
    $('#estado-lista').textContent = total.totalItens === 0 ? 'Seu catálogo começa aqui. Cadastre o primeiro produto para acompanhar o estoque.' : 'Nenhum produto encontrado para esses filtros.';
    $('#resumo-lista').textContent = result.totalItens ? `${page * 20 + 1}–${page * 20 + result.itens.length} de ${result.totalItens} produtos · Página ${page + 1} de ${totalPages}` : '0 produtos';
    $('#anterior').disabled = page === 0; $('#proxima').disabled = page + 1 >= totalPages;
  } catch (error) {
    if (error.name === 'AbortError' || currentRequest !== requestId || loginRequired(error)) return;
    $('#estado-lista').textContent = 'Não foi possível carregar o estoque. Use Buscar para tentar novamente.';
    $('#total-produtos').textContent = '—'; $('#total-reposicao').textContent = '—'; $('#resumo-lista').textContent = '';
    notice($('#mensagem'), error.message, true);
  } finally { if (currentRequest === requestId) $('#lista').setAttribute('aria-busy', 'false'); }
}
function clearErrors() {
  notice($('#erro-produto'), '');
  for (const input of form.querySelectorAll('input')) input.removeAttribute('aria-invalid');
  for (const span of form.querySelectorAll('.field-error')) span.textContent = '';
}
async function openProduct(id = null) {
  selectedId = id; form.reset(); clearErrors();
  const editing = id !== null;
  $('#titulo-form').textContent = editing ? 'Editar produto' : 'Cadastrar produto';
  $('#salvar').textContent = editing ? 'Salvar alterações' : 'Cadastrar produto';
  $('#campo-quantidade').hidden = editing; $('#quantidadeEstoque').disabled = editing; $('#aviso-saldo').hidden = !editing;
  if (editing) {
    try {
      const product = await api(`/api/produtos/${id}`);
      for (const key of ['nome', 'categoria', 'preco', 'estoqueMinimo']) form.elements[key].value = product[key];
    } catch (error) { if (!loginRequired(error)) notice($('#mensagem'), error.message, true, true); return; }
  }
  dialog.showModal(); $('#nome').focus();
}
function closeProduct() { if (!saving) dialog.close(); }
$('#cadastrar').addEventListener('click', () => openProduct()); $('#nav-cadastrar').addEventListener('click', () => openProduct());
document.querySelectorAll('.fechar').forEach(button => button.addEventListener('click', closeProduct));
dialog.addEventListener('cancel', event => { if (saving) event.preventDefault(); });
form.addEventListener('submit', async event => {
  event.preventDefault(); if (saving) return; clearErrors(); saving = true; $('#salvar').disabled = true;
  const data = { nome: form.elements.nome.value, categoria: form.elements.categoria.value, preco: Number(form.elements.preco.value), estoqueMinimo: Number(form.elements.estoqueMinimo.value) };
  if (selectedId === null) data.quantidadeEstoque = Number(form.elements.quantidadeEstoque.value);
  try {
    await api(selectedId === null ? '/api/produtos' : `/api/produtos/${selectedId}`, { method: selectedId === null ? 'POST' : 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) });
    dialog.close(); notice($('#mensagem'), selectedId === null ? 'Produto cadastrado com sucesso.' : 'Produto atualizado com sucesso.', false, true);
    await load();
  } catch (error) {
    if (loginRequired(error)) return;
    for (const field of error.fields || []) {
      const input = form.elements[field.campo]; const hint = document.getElementById(`erro-${field.campo}`);
      if (input && hint) { input.setAttribute('aria-invalid', 'true'); hint.textContent = field.mensagem; }
    }
    notice($('#erro-produto'), error.status === 0 ? `${error.message} A gravação pode ter ocorrido. Feche o formulário e consulte o estoque antes de reenviar.` : error.message, true, true);
  } finally { saving = false; $('#salvar').disabled = false; }
});
function openDelete(product) { deletingId = product.id; $('#produto-excluir').textContent = product.nome; notice($('#erro-excluir'), ''); deleteDialog.showModal(); }
document.querySelectorAll('.fechar-excluir').forEach(button => button.addEventListener('click', () => { if (!deleting) deleteDialog.close(); }));
deleteDialog.addEventListener('cancel', event => { if (deleting) event.preventDefault(); });
$('#excluir-form').addEventListener('submit', async event => {
  event.preventDefault(); if (deleting) return; deleting = true; $('#confirmar-exclusao').disabled = true;
  try { await api(`/api/produtos/${deletingId}`, { method: 'DELETE' }); deleteDialog.close(); notice($('#mensagem'), 'Produto excluído do catálogo.', false, true); await load(); }
  catch (error) { if (!loginRequired(error)) notice($('#erro-excluir'), error.message, true, true); }
  finally { deleting = false; $('#confirmar-exclusao').disabled = false; }
});
function filter() { query = $('#busca').value.trim(); onlyLow = $('#reposicao').checked; page = 0; notice($('#mensagem'), ''); load(); }
$('#filtros').addEventListener('submit', event => { event.preventDefault(); filter(); });
$('#reposicao').addEventListener('change', filter);
$('#limpar').addEventListener('click', () => { $('#filtros').reset(); filter(); });
$('#anterior').addEventListener('click', () => { if (page > 0) { page--; load(); } });
$('#proxima').addEventListener('click', () => { if (page + 1 < totalPages) { page++; load(); } });
$('#sair').addEventListener('click', async () => {
  $('#sair').disabled = true;
  try { await api('/api/auth/logout', { method: 'POST' }); location.replace('/login.html'); }
  catch (error) { if (!loginRequired(error)) notice($('#mensagem'), error.message, true, true); }
  finally { $('#sair').disabled = false; }
});
try {
  const user = await api('/api/auth/me'); await csrf(); $('#usuario').textContent = user.login;
  $('.avatar').textContent = user.login[0].toUpperCase(); $('#aplicacao').hidden = false; $('#iniciando').hidden = true;
  await load();
} catch (error) { if (!loginRequired(error, false)) $('#iniciando').textContent = `${error.message} Atualize a página para tentar novamente.`; }
