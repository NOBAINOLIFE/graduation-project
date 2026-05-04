import { request } from './http';

export function login(payload) {
  return request('/graduation-project/user/login', {
    method: 'POST',
    json: payload
  });
}

export function register(payload) {
  return request('/graduation-project/user/register', {
    method: 'POST',
    json: payload
  });
}

export function logout() {
  return request('/graduation-project/user/logout', {
    method: 'POST'
  });
}

export function getUserInfo(userId = null) {
  const params = userId ? { userId } : {};
  return request('/graduation-project/user/info', {
    method: 'GET',
    params
  });
}

export function updateUserInfo(payload) {
  return request('/graduation-project/user/update-info', {
    method: 'POST',
    json: payload
  });
}

export function updateUserPassword(payload) {
  return request('/graduation-project/user/update-password', {
    method: 'POST',
    json: payload
  });
}

export function uploadAvatar(file) {
  const formData = new FormData();
  formData.append('file', file);
  return request('/graduation-project/user/upload-avatar', {
    method: 'POST',
    formData
  });
}

export function getMyCoinWallet() {
  return request('/graduation-project/interact/coin/wallet', {
    method: 'GET'
  });
}

export function getMyCoinChangeLogs(days = 7) {
  return request('/graduation-project/user/coin/change-logs', {
    method: 'GET',
    params: { days }
  });
}

export function searchVideos(payload) {
  return request('/graduation-project/search/video', {
    method: 'POST',
    json: payload
  });
}

export function listCollectionDirectories(targetUserId = null) {
  const params = targetUserId ? { targetUserId } : {};
  return request('/graduation-project/interact/collection/directory/list', {
    method: 'GET',
    params
  });
}

export function listCollectionItems(directoryId, sortType = 1) {
  return request('/graduation-project/interact/collection/item/list', {
    method: 'GET',
    params: { directoryId, sortType }
  });
}

export function createCollectionDirectory(payload) {
  return request('/graduation-project/interact/collection/directory/create', {
    method: 'POST',
    json: payload
  });
}

export function updateCollectionDirectory(payload) {
  return request('/graduation-project/interact/collection/directory/update', {
    method: 'POST',
    json: payload
  });
}

export function deleteCollectionDirectory(directoryId) {
  return request('/graduation-project/interact/collection/directory/delete', {
    method: 'GET',
    params: { directoryId }
  });
}

export function batchOperateCollectionItems(payload) {
  return request('/graduation-project/interact/collection/item/batch', {
    method: 'POST',
    json: payload
  });
}

export function clearInvalidCollectionItems(directoryId) {
  return request('/graduation-project/interact/collection/item/clearInvalid', {
    method: 'POST',
    params: { directoryId }
  });
}

export function uploadImage(file) {
  const formData = new FormData();
  formData.append('file', file);
  return request('/graduation-project/upload/image', {
    method: 'POST',
    formData
  });
}

export function listFansUsers(targetUserId = null) {
  const params = targetUserId ? { targetUserId } : {};
  return request('/graduation-project/interact/fansList', {
    method: 'POST',
    params
  });
}

export function listFollowUsers(targetUserId = null) {
  const params = targetUserId ? { targetUserId } : {};
  return request('/graduation-project/interact/followList', {
    method: 'POST',
    params
  });
}

