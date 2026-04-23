import { request } from './http';

export function login(payload) {
  return request('/graduation-project/user/login', {
    method: 'POST',
    json: payload
  });
}

export function logout() {
  return request('/graduation-project/user/logout', {
    method: 'POST'
  });
}

