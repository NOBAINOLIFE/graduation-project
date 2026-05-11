import { request } from './http';

export function getCreatorVideos(payload) {
  return request('/graduation-project/video/creator/list', {
    method: 'POST',
    json: payload
  });
}

export function submitCreatorVideoAudit(videoId) {
  return request(`/graduation-project/video/publish/${videoId}`, {
    method: 'POST'
  });
}

export function getCreatorComments(payload) {
  return request('/graduation-project/interact/comment/creator/list', {
    method: 'POST',
    json: payload
  });
}

export function updateCreatorVideo(videoId, payload) {
  return request(`/graduation-project/video/update/${videoId}`, {
    method: 'POST',
    json: payload
  });
}

export function deleteCreatorComment(commentId) {
  return request(`/graduation-project/interact/comment/creator/delete/${commentId}`, {
    method: 'POST'
  });
}
