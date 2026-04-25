import { clearUserAuth, getAdminToken, getToken } from '../utils/auth';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '';

function shouldRedirectToLogin(message) {
  if (!message) return false;
  return (
    message.includes('未登录') ||
    message.includes('失效')
  );
}

function isAuthFormRequest(path) {
  return (
    path === '/graduation-project/user/login' ||
    path === '/graduation-project/user/register'
  );
}

export async function request(path, { method = 'GET', json, formData, params } = {}) {
  const headers = {};
  
  // 根据路径判断使用哪种token
  // 管理后台接口使用admin_token
  // 普通用户接口使用user_token
  const isAdminApi = path.includes('/manager/') || path.includes('/admin/');
  const token = isAdminApi ? getAdminToken() : getToken();
  
  if (token) headers.token = token;

  let body;
  if (json) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(json);
  } else if (formData) {
    body = formData;
  }

  // Build query string from params
  let url = `${API_BASE}${path}`;
  if (params) {
    const queryString = Object.entries(params)
      .filter(([_, v]) => v != null)
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
      .join('&');
    if (queryString) {
      url += `?${queryString}`;
    }
  }

  const response = await fetch(url, {
    method,
    headers,
    body
  });

  const payload = await response.json().catch(() => ({}));
  const failMessage = payload?.message || `HTTP ${response.status}`;
  const shouldHandleAuthRedirect = !isAuthFormRequest(path);

  if (!response.ok || payload?.code !== 0) {
    if (shouldHandleAuthRedirect && shouldRedirectToLogin(failMessage)) {
      // 根据API类型清除对应的认证
      if (isAdminApi) {
        // 管理后台登录失效，跳转到管理登录页
        const { clearAdminAuth } = await import('../utils/auth');
        clearAdminAuth();
        if (window.location.pathname !== '/manager/login') {
          const currentPath = `${window.location.pathname}${window.location.search}`;
          const redirect = encodeURIComponent(currentPath);
          window.location.href = `/manager/login?redirect=${redirect}`;
        }
      } else {
        // 普通用户登录失效，清除用户认证
        clearUserAuth();
        // 如果在首页，可以显示登录弹窗而不是跳转
        if (window.location.pathname === '/') {
          // 触发自定义事件，让HomePage显示登录弹窗
          window.dispatchEvent(new CustomEvent('show-login-modal'));
        } else {
          // 其他页面跳转到首页
          window.location.href = '/';
        }
      }
    }
    throw new Error(failMessage || '请求失败');
  }

  return payload.data;
}
