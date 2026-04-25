// 普通用户认证
const USER_TOKEN_KEY = 'user_token';
const USER_ID_KEY = 'user_id';
const USERNAME_KEY = 'username';
export const USER_AUTH_CHANGE_EVENT = 'user-auth-changed';
export const SHOW_LOGIN_MODAL_EVENT = 'show-login-modal';
export const SHOW_LOGIN_MODAL_ONCE_KEY = 'show-login-modal-once';

// 管理员认证
const ADMIN_TOKEN_KEY = 'admin_token';
const ADMIN_ROLE_KEY = 'admin_roleCode';
const ADMIN_USER_ID_KEY = 'admin_userId';
const ADMIN_USERNAME_KEY = 'admin_username';

// ==================== 普通用户认证 ====================

/**
 * 获取普通用户Token
 */
export function getToken() {
  return localStorage.getItem(USER_TOKEN_KEY) || '';
}

/**
 * 检查普通用户是否登录
 */
export function isUserLoggedIn() {
  return Boolean(getToken());
}

/**
 * 保存普通用户登录信息
 */
export function saveUserLogin(loginData) {
  localStorage.setItem(USER_TOKEN_KEY, loginData?.token || '');
  localStorage.setItem(USER_ID_KEY, String(loginData?.userId || ''));
  localStorage.setItem(USERNAME_KEY, loginData?.username || '');
  emitUserAuthChanged(true);
}

/**
 * 设置普通用户认证信息
 */
export function setUserAuth(token, userId, username) {
  localStorage.setItem(USER_TOKEN_KEY, token || '');
  localStorage.setItem(USER_ID_KEY, String(userId || ''));
  localStorage.setItem(USERNAME_KEY, username || '');
  emitUserAuthChanged(Boolean(token));
}

/**
 * 获取普通用户ID
 */
export function getUserId() {
  const id = localStorage.getItem(USER_ID_KEY);
  return id ? Number(id) : null;
}

/**
 * 获取普通用户名
 */
export function getUsername() {
  return localStorage.getItem(USERNAME_KEY) || '';
}

/**
 * 清除普通用户认证信息
 */
export function clearUserAuth() {
  localStorage.removeItem(USER_TOKEN_KEY);
  localStorage.removeItem(USER_ID_KEY);
  localStorage.removeItem(USERNAME_KEY);
  emitUserAuthChanged(false);
}

function emitUserAuthChanged(isLoggedIn) {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(new CustomEvent(USER_AUTH_CHANGE_EVENT, {
    detail: {
      isLoggedIn,
      userId: getUserId(),
      username: getUsername()
    }
  }));
}

export function openLoginModal(detail = {}) {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(new CustomEvent(SHOW_LOGIN_MODAL_EVENT, { detail }));
}

// ==================== 管理员认证 ====================

/**
 * 获取管理员Token
 */
export function getAdminToken() {
  return localStorage.getItem(ADMIN_TOKEN_KEY) || '';
}

/**
 * 检查管理员是否登录
 */
export function isAdminLoggedIn() {
  return Boolean(getAdminToken());
}

/**
 * 检查是否为管理员
 */
export function isAdmin() {
  const roleCode = localStorage.getItem(ADMIN_ROLE_KEY) || '';
  return roleCode.toUpperCase() === 'ADMIN';
}

/**
 * 保存管理员登录信息
 */
export function saveAdminLogin(loginData) {
  localStorage.setItem(ADMIN_TOKEN_KEY, loginData?.token || '');
  localStorage.setItem(ADMIN_ROLE_KEY, loginData?.roleCode || '');
  localStorage.setItem(ADMIN_USER_ID_KEY, String(loginData?.userId || ''));
  localStorage.setItem(ADMIN_USERNAME_KEY, loginData?.username || '');
}

/**
 * 设置管理员认证信息
 */
export function setAdminAuth(token, userId, username, roleCode) {
  localStorage.setItem(ADMIN_TOKEN_KEY, token || '');
  localStorage.setItem(ADMIN_ROLE_KEY, roleCode || '');
  localStorage.setItem(ADMIN_USER_ID_KEY, String(userId || ''));
  localStorage.setItem(ADMIN_USERNAME_KEY, username || '');
}

/**
 * 获取管理员ID
 */
export function getAdminId() {
  const id = localStorage.getItem(ADMIN_USER_ID_KEY);
  return id ? Number(id) : null;
}

/**
 * 获取管理员用户名
 */
export function getAdminUsername() {
  return localStorage.getItem(ADMIN_USERNAME_KEY) || '';
}

/**
 * 清除管理员认证信息
 */
export function clearAdminAuth() {
  localStorage.removeItem(ADMIN_TOKEN_KEY);
  localStorage.removeItem(ADMIN_ROLE_KEY);
  localStorage.removeItem(ADMIN_USER_ID_KEY);
  localStorage.removeItem(ADMIN_USERNAME_KEY);
}

// ==================== 兼容旧API（逐步废弃）====================

/**
 * @deprecated 请使用 isUserLoggedIn()
 */
export function isLoggedIn() {
  return isUserLoggedIn();
}

/**
 * @deprecated 请使用 saveUserLogin() 或 saveAdminLogin()
 */
export function saveLogin(loginData) {
  // 根据角色判断保存到哪里
  if (loginData?.roleCode?.toUpperCase() === 'ADMIN') {
    saveAdminLogin(loginData);
  } else {
    saveUserLogin(loginData);
  }
}

/**
 * @deprecated 请使用 clearUserAuth() 或 clearAdminAuth()
 */
export function clearAuth() {
  // 同时清除两种认证（用于完全退出）
  clearUserAuth();
  clearAdminAuth();
}

