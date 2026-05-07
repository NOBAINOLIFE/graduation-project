import { request } from './http';

export function queryAuditVideoList(payload) {
  return request('/graduation-project/manager/video/audit/list', {
    method: 'POST',
    json: payload
  });
}

export function auditVideo(payload) {
  return request('/graduation-project/manager/video/audit', {
    method: 'POST',
    json: payload
  });
}

export function queryUserList(payload) {
  return request('/graduation-project/manager/user/list', {
    method: 'POST',
    json: payload
  });
}

export function queryVideoPartitionList(payload) {
  return request('/graduation-project/manager/video/partition/list', {
    method: 'POST',
    json: payload
  });
}

export function queryVideoTagList(payload) {
  return request('/graduation-project/manager/video/tag/list', {
    method: 'POST',
    json: payload
  });
}

export function queryReportList(payload) {
  return request('/graduation-project/manager/report/list', {
    method: 'POST',
    json: payload
  });
}

export function reviewReport(payload) {
  return request('/graduation-project/manager/report/review', {
    method: 'POST',
    json: payload
  });
}

export function banUser(userId) {
  return request(`/graduation-project/manager/user/ban/${userId}`, {
    method: 'POST'
  });
}

export function unbanUser(userId) {
  return request(`/graduation-project/manager/user/unban/${userId}`, {
    method: 'POST'
  });
}

export function banVideo(videoId) {
  return request(`/graduation-project/manager/video/ban/${videoId}`, {
    method: 'POST'
  });
}

export function unbanVideo(videoId) {
  return request(`/graduation-project/manager/video/unban/${videoId}`, {
    method: 'POST'
  });
}

export function deleteVideoPartition(partitionId) {
  return request(`/graduation-project/manager/video/partition/${partitionId}`, {
    method: 'DELETE'
  });
}

export function deleteVideoTag(tagId) {
  return request(`/graduation-project/manager/video/tag/${tagId}`, {
    method: 'DELETE'
  });
}

