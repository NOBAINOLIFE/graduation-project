import { clearAuth, getToken } from '../utils/auth';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '';

function shouldRedirectToLogin(message) {
  if (!message) return false;
  return (
    message.includes('未登录') ||
    message.includes('失效') ||
    message.includes('登录失败')
  );
}

function redirectToLogin() {
  const currentPath = `${window.location.pathname}${window.location.search}`;
  if (window.location.pathname === '/manager/login') return;
  const redirect = encodeURIComponent(currentPath);
  window.location.href = `/manager/login?redirect=${redirect}`;
}

export async function request(path, { method = 'GET', json, formData } = {}) {
  const headers = {};
  const token = getToken();
  if (token) headers.token = token;

  let body;
  if (json) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(json);
  } else if (formData) {
    body = formData;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body
  });

  const payload = await response.json().catch(() => ({}));
  const failMessage = payload?.message || `HTTP ${response.status}`;

  if (!response.ok || payload?.code !== 0) {
    if (shouldRedirectToLogin(failMessage)) {
      clearAuth();
      redirectToLogin();
    }
    throw new Error(failMessage || '请求失败');
  }

  return payload.data;
}
