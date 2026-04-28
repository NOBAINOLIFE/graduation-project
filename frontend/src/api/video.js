import { request } from './http';

export function getVideoPlayList(lastVideoId = 0) {
  return request('/graduation-project/video/videoPlayList', {
    method: 'GET',
    params: { lastVideoId }
  });
}

export function getVideoDetail(videoId) {
  return request(`/graduation-project/video/videoInfo/${videoId}`, {
    method: 'GET'
  });
}

export function reportVideoProgress(payload) {
  return request('/graduation-project/video/play/progress', {
    method: 'POST',
    json: payload
  });
}

export function getUserVideoHistory(pageNum = 1, pageSize = 20) {
  return request('/graduation-project/video/history', {
    method: 'GET',
    params: { pageNum, pageSize }
  });
}

export function listPartitions() {
  return request('/graduation-project/video/partitions', {
    method: 'GET'
  });
}

export function searchVideos(payload) {
  return request('/graduation-project/search/video', {
    method: 'POST',
    json: payload
  });
}

export function searchUsers(payload) {
  return request('/graduation-project/search/user', {
    method: 'POST',
    json: payload
  });
}

export function getVideoComments(videoId, { sortType = 1, cursor = null, pageSize = 20 } = {}) {
  return request('/graduation-project/interact/comment/list', {
    method: 'GET',
    params: {
      videoId,
      sortType,
      pageSize,
      ...(cursor && typeof cursor === 'object' ? cursor : {})
    }
  });
}

export function getCommentReplies(rootCommentId, { pageNum = 1, pageSize = 10 } = {}) {
  return request('/graduation-project/interact/comment/replies', {
    method: 'GET',
    params: { rootCommentId, pageNum, pageSize }
  });
}

export function submitVideoComment(payload) {
  return request('/graduation-project/interact/comment', {
    method: 'POST',
    json: payload
  });
}

export function likeVideo(payload) {
  return request('/graduation-project/interact/likeVideo', {
    method: 'POST',
    json: payload
  });
}

export function likeComment(payload) {
  return request('/graduation-project/interact/likeComment', {
    method: 'POST',
    json: payload
  });
}

export function coinVideo(payload) {
  return request('/graduation-project/interact/coin', {
    method: 'POST',
    json: payload
  });
}

export function collectVideo(payload) {
  return request('/graduation-project/interact/collectVideo', {
    method: 'POST',
    json: payload
  });
}

export function queryVideoDirectoryRelations(videoId) {
  return request('/graduation-project/interact/collection/rel', {
    method: 'GET',
    params: { videoId }
  });
}

export function followUser(payload) {
  return request('/graduation-project/interact/follow', {
    method: 'POST',
    json: payload
  });
}

export function reportVideo(payload) {
  return request('/graduation-project/interact/report', {
    method: 'POST',
    json: payload
  });
}
