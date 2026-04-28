import { request } from './http';
import { getToken } from '../utils/auth';

const RAW_API_BASE = import.meta.env.VITE_API_BASE_URL || '';

export function getChatWsPath() {
  return request('/graduation-project/interact/chat/wsPath', {
    method: 'GET'
  });
}

export function getChatSessions() {
  return request('/graduation-project/interact/chat/sessions', {
    method: 'GET'
  });
}

export function getChatHistory(withUserId, beforeId = null, pageSize = 20) {
  return request('/graduation-project/interact/chat/history', {
    method: 'GET',
    params: {
      withUserId,
      beforeId,
      pageSize
    }
  });
}

export function getChatUnreadTotal() {
  return request('/graduation-project/interact/chat/unread/total', {
    method: 'GET'
  });
}

export function markChatRead(withUserId, upToMsgId = null) {
  return request('/graduation-project/interact/chat/read', {
    method: 'POST',
    params: {
      withUserId,
      upToMsgId
    }
  });
}

export function getReplyMessages() {
  return request('/graduation-project/interact/message/replies', {
    method: 'GET'
  });
}

export function getLikeMessageSummaries() {
  return request('/graduation-project/interact/message/likes', {
    method: 'GET'
  });
}

export function getLikeMessageDetail(targetType, targetId) {
  return request('/graduation-project/interact/message/likes/detail', {
    method: 'GET',
    params: {
      targetType,
      targetId
    }
  });
}

export function buildChatWebSocketUrl(wsPath, token = getToken()) {
  const baseUrl = RAW_API_BASE
    ? new URL(RAW_API_BASE, window.location.origin)
    : new URL(window.location.origin);
  const wsOrigin = baseUrl.origin.replace(/^http/i, 'ws');
  const url = new URL(wsPath, `${wsOrigin}/`);
  if (token) {
    url.searchParams.set('token', token);
  }
  return url.toString();
}
