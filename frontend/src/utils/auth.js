const TOKEN_KEY = 'token';
const ROLE_KEY = 'roleCode';
const USERNAME_KEY = 'username';
const USER_ID_KEY = 'userId';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || '';
}

export function getRoleCode() {
  return localStorage.getItem(ROLE_KEY) || '';
}

export function isAdmin() {
  return getRoleCode().toUpperCase() === 'ADMIN';
}

export function isLoggedIn() {
  return Boolean(getToken());
}

export function saveLogin(loginData) {
  localStorage.setItem(TOKEN_KEY, loginData?.token || '');
  localStorage.setItem(ROLE_KEY, loginData?.roleCode || '');
  localStorage.setItem(USERNAME_KEY, loginData?.username || '');
  localStorage.setItem(USER_ID_KEY, String(loginData?.userId || ''));
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(ROLE_KEY);
  localStorage.removeItem(USERNAME_KEY);
  localStorage.removeItem(USER_ID_KEY);
}

