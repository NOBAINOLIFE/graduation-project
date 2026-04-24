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

export function getVideoComments(videoId, { sortType = 1, pageNum = 1, pageSize = 10 } = {}) {
  return request('/graduation-project/interact/comment/list', {
    method: 'GET',
    params: { videoId, sortType, pageNum, pageSize }
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

export function followUser(payload) {
  return request('/graduation-project/interact/follow', {
    method: 'POST',
    json: payload
  });
}
