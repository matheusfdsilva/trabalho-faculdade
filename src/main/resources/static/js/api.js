export class ApiError extends Error {
  constructor(message, status = 0, fields = []) { super(message); this.status = status; this.fields = fields; }
}
let token;
export async function csrf() {
  token = await api('/api/auth/csrf');
  return token;
}
export async function api(path, options = {}) {
  const method = options.method || 'GET';
  const headers = new Headers(options.headers);
  if (!['GET', 'HEAD'].includes(method)) {
    const current = token || await csrf();
    headers.set(current.headerName, current.token);
  }
  let response;
  try { response = await fetch(path, { ...options, method, headers, credentials: 'same-origin', cache: 'no-store' }); }
  catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new ApiError('Não foi possível conectar ao sistema. Confira sua conexão e tente novamente.');
  }
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 403) {
      token = undefined;
      if (path !== '/api/auth/login') {
        const session = await fetch('/api/auth/me', { credentials: 'same-origin', cache: 'no-store' }).catch(() => null);
        if (session?.status === 401) throw new ApiError('Sua sessão terminou. Entre novamente para continuar.', 401);
      }
    }
    throw new ApiError(data?.mensagem || 'Não foi possível concluir a operação. Tente novamente.', response.status, data?.campos || []);
  }
  return data;
}
export function notice(element, message, error = false, focus = false) {
  element.textContent = message;
  element.classList.toggle('error', error);
  element.hidden = !message;
  if (focus && message) element.focus();
}
export function loginRequired(error, expired = true) {
  if (error.status !== 401) return false;
  location.replace(expired ? '/login.html?motivo=sessao' : '/login.html');
  return true;
}
