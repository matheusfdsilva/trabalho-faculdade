import { api, csrf, notice } from './api.js';
const form = document.querySelector('#login-form');
const button = document.querySelector('#entrar');
const message = document.querySelector('#mensagem');
if (new URLSearchParams(location.search).get('motivo') === 'sessao') {
  notice(message, 'Sua sessão terminou. Entre novamente para continuar.', false);
}
try { await api('/api/auth/me'); location.replace('/'); }
catch (error) { if (error.status !== 401) notice(message, error.message, true); }
form.addEventListener('submit', async event => {
  event.preventDefault();
  button.disabled = true;
  button.textContent = 'Entrando…';
  notice(message, '');
  try {
    await csrf();
    await api('/api/auth/login', { method: 'POST', body: new URLSearchParams(new FormData(form)) });
    form.reset();
    location.replace('/');
  } catch (error) {
    notice(message, error.message, true, true);
    form.elements.password.value = '';
  } finally { button.disabled = false; button.textContent = 'Entrar no sistema →'; }
});
