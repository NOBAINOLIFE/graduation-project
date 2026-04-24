<template>
  <div class="min-h-screen bg-[#f6f7fb] text-gray-800">
    <HeaderNav />

    <main class="max-w-7xl mx-auto px-4 py-6">
      <div class="flex flex-col gap-6 lg:flex-row">
        <div class="min-w-0 flex-1">
          <section class="mb-4 rounded-2xl bg-white p-5 shadow-sm">
            <div class="flex flex-wrap items-center gap-3 text-sm text-gray-500">
              <span class="rounded-full bg-[#eef7ff] px-3 py-1 font-medium text-[#00a1d6]">
                {{ videoInfo.partitionName || '视频' }}
              </span>
              <span class="flex items-center gap-1">
                <el-icon><View /></el-icon>
                {{ formatCount(videoInfo.playCount) }} 播放
              </span>
              <span class="flex items-center gap-1">
                <el-icon><ChatDotRound /></el-icon>
                {{ formatCount(videoInfo.commentCount) }} 评论
              </span>
              <span class="flex items-center gap-1">
                <el-icon><Clock /></el-icon>
                {{ formatDate(videoInfo.createTime) }}
              </span>
              <span v-if="videoInfo.duration" class="flex items-center gap-1">
                <el-icon><Timer /></el-icon>
                {{ formatDuration(videoInfo.duration) }}
              </span>
            </div>
            <h1 class="mt-3 text-2xl font-bold leading-9 text-gray-900">
              {{ videoInfo.title || '视频加载中...' }}
            </h1>
          </section>

          <section class="overflow-hidden bg-black shadow-[0_30px_70px_rgba(15,23,42,0.18)]">
            <div class="relative aspect-video">
              <AppVideoPlayer
                ref="appVideoPlayerRef"
                :media-key="currentVideoId"
                :sources="videoSources"
                :poster="videoInfo.coverUrl || ''"
                :loading="loading"
                :initial-time="videoInfo.lastPlayTime"
                autoplay
                @timeupdate="handlePlaybackTimeUpdate"
                @ended="handleVideoEnded"
              />
            </div>
          </section>

          <section class="mt-4 rounded-2xl bg-white p-4 shadow-sm">
            <div class="flex flex-wrap items-center gap-3">
              <button
                class="flex items-center gap-2 rounded-full px-4 py-2 transition-colors"
                :class="videoInfo.isLike ? 'bg-[#e6f7ff] text-[#00a1d6]' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
                :disabled="actionLoading.like"
                @click="handleLike"
              >
                <el-icon :size="20"><CaretTop /></el-icon>
                <span>{{ formatCount(videoInfo.likeCount) }}</span>
              </button>

              <button
                class="flex items-center gap-2 rounded-full px-4 py-2 transition-colors"
                :class="videoInfo.isCoin ? 'bg-[#fff4e5] text-[#ff9f1c]' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
                :disabled="actionLoading.coin"
                @click="handleCoin"
              >
                <el-icon :size="20"><Coin /></el-icon>
                <span>{{ formatCount(videoInfo.coinCount) }}</span>
              </button>

              <button
                class="flex items-center gap-2 rounded-full px-4 py-2 transition-colors"
                :class="videoInfo.isCollect ? 'bg-[#fff7d6] text-[#d48806]' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
                :disabled="actionLoading.collect"
                @click="handleCollect"
              >
                <el-icon :size="20"><Star /></el-icon>
                <span>{{ formatCount(videoInfo.collectCount) }}</span>
              </button>

              <button
                class="flex items-center gap-2 rounded-full bg-gray-100 px-4 py-2 text-gray-700 transition-colors hover:bg-gray-200"
                @click="handleShare"
              >
                <el-icon :size="20"><Share /></el-icon>
                <span>{{ formatCount(videoInfo.shareCount) }}</span>
              </button>
            </div>
          </section>

          <section v-if="videoInfo.description" class="mt-4 rounded-2xl bg-white p-5 shadow-sm">
            <h2 class="mb-3 text-sm font-semibold uppercase tracking-[0.2em] text-gray-400">简介</h2>
            <p class="whitespace-pre-line text-sm leading-7 text-gray-600">
              {{ videoInfo.description }}
            </p>
          </section>

          <section class="mt-4 rounded-2xl bg-white p-5 shadow-sm">
            <div class="mb-5 flex flex-wrap items-center justify-between gap-3">
              <h2 class="text-lg font-bold text-gray-900">
                评论 {{ commentTotal > 0 ? `(${commentTotal})` : '' }}
              </h2>
              <el-radio-group v-model="commentSort" size="small">
                <el-radio-button label="hot">最热</el-radio-button>
                <el-radio-button label="new">最新</el-radio-button>
              </el-radio-group>
            </div>

            <div class="mb-6 flex gap-3">
              <el-avatar :size="40" class="shrink-0 bg-[#00a1d6] text-white">
                {{ currentUsernameInitial }}
              </el-avatar>
              <div class="flex-1">
                <el-input
                  v-model="commentContent"
                  type="textarea"
                  :rows="3"
                  maxlength="500"
                  show-word-limit
                  placeholder="发条友善的评论吧"
                />
                <div class="mt-3 flex justify-end">
                  <el-button
                    type="primary"
                    :loading="commentSubmitting"
                    :disabled="!commentContent.trim()"
                    @click="submitComment"
                  >
                    发布评论
                  </el-button>
                </div>
              </div>
            </div>

            <div v-if="commentLoading && comments.length === 0" class="space-y-4">
              <div v-for="item in 3" :key="item" class="flex gap-3 rounded-xl bg-gray-50 p-4">
                <div class="h-10 w-10 rounded-full bg-gray-200"></div>
                <div class="flex-1 space-y-2">
                  <div class="h-4 w-24 rounded bg-gray-200"></div>
                  <div class="h-3 w-full rounded bg-gray-100"></div>
                  <div class="h-3 w-3/4 rounded bg-gray-100"></div>
                </div>
              </div>
            </div>

            <div v-else-if="comments.length === 0" class="flex flex-col items-center justify-center py-14 text-gray-400">
              <el-icon :size="42"><ChatLineRound /></el-icon>
              <p class="mt-3 text-sm">还没有评论，来抢个沙发吧</p>
            </div>

            <div v-else class="space-y-5">
              <article
                v-for="comment in comments"
                :key="comment.commentId"
                class="rounded-2xl border border-gray-100 p-4 transition-colors hover:border-[#d6eef8]"
              >
                <div class="flex gap-3">
                  <el-avatar :size="42" :src="comment.avatarUrl" class="shrink-0 bg-[#00a1d6] text-white">
                    {{ (comment.username || 'U').slice(0, 1) }}
                  </el-avatar>
                  <div class="min-w-0 flex-1">
                    <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
                      <span class="font-medium text-gray-800">{{ comment.username || '未知用户' }}</span>
                      <span class="text-xs text-gray-400">{{ formatDate(comment.createTime) }}</span>
                      <span v-if="comment.replyCount" class="rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-500">
                        {{ comment.replyCount }} 条回复
                      </span>
                    </div>
                    <p class="mt-2 whitespace-pre-line break-words text-sm leading-7 text-gray-700">
                      <template v-if="comment.replyUsername">回复 @{{ comment.replyUsername }}：</template>{{ comment.content }}
                    </p>
                    <div class="mt-3 flex items-center gap-4 text-sm text-gray-500">
                      <button
                        class="flex items-center gap-1 transition-colors hover:text-[#00a1d6]"
                        :class="comment.isLike ? 'text-[#00a1d6]' : ''"
                        @click="handleLikeComment(comment)"
                      >
                        <el-icon><CaretTop /></el-icon>
                        <span>{{ formatCount(comment.likeCount) }}</span>
                      </button>
                      <button class="transition-colors hover:text-[#00a1d6]" @click="showReplyTip">
                        回复
                      </button>
                    </div>
                  </div>
                </div>
              </article>
            </div>

            <div v-if="hasMoreComments" class="mt-6 text-center">
              <el-button :loading="commentLoading" @click="loadComments(true)">
                加载更多评论
              </el-button>
            </div>
          </section>
        </div>

        <aside class="w-full shrink-0 lg:w-80">
          <div class="sticky top-24 rounded-2xl bg-white p-5 shadow-sm">
            <div class="flex items-start gap-4">
              <el-avatar
                :size="56"
                :src="videoInfo.avatarUrl"
                class="cursor-pointer bg-[#00a1d6] text-white"
                @click="goToUserProfile"
              >
                {{ (videoInfo.username || 'U').slice(0, 1) }}
              </el-avatar>
              <div class="min-w-0 flex-1">
                <button class="max-w-full truncate text-left text-lg font-semibold text-gray-900 hover:text-[#00a1d6]" @click="goToUserProfile">
                  {{ videoInfo.username || '未知用户' }}
                </button>
                <p class="mt-1 text-sm text-gray-500">粉丝 {{ formatCount(videoInfo.fansCount) }}</p>
                <p class="mt-3 line-clamp-3 text-sm leading-6 text-gray-600">
                  {{ videoInfo.userBio || '这个 UP 主还没有留下简介。' }}
                </p>
                <el-button
                  type="primary"
                  class="mt-4 w-full"
                  :disabled="actionLoading.follow || isSelfVideo"
                  @click="handleFollow"
                >
                  {{ isSelfVideo ? '这是你自己的视频' : (videoInfo.isFollow ? '已关注' : '+ 关注') }}
                </el-button>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  CaretTop,
  ChatDotRound,
  ChatLineRound,
  Clock,
  Coin,
  Share,
  Star,
  Timer,
  View
} from '@element-plus/icons-vue';
import HeaderNav from './HeaderNav.vue';
import AppVideoPlayer from './common/AppVideoPlayer.vue';
import {
  coinVideo,
  collectVideo,
  followUser,
  getVideoComments,
  getVideoDetail,
  likeComment,
  likeVideo,
  reportVideoProgress,
  submitVideoComment
} from '../api/video';
import { createCollectionDirectory, listCollectionDirectories } from '../api/user';
import { getUserId, getUsername } from '../utils/auth';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const commentLoading = ref(false);
const commentSubmitting = ref(false);
const appVideoPlayerRef = ref(null);
const currentVideoId = ref(Number(route.params.id) || 0);
const commentSort = ref('hot');
const commentContent = ref('');
const comments = ref([]);
const commentTotal = ref(0);
const commentPageNum = ref(1);
const commentPageSize = 10;
const hasMoreComments = ref(false);
const lastReportedTime = ref(0);
const reportInFlight = ref(false);
const defaultCollectionDirectoryId = ref(null);

const actionLoading = reactive({
  like: false,
  coin: false,
  collect: false,
  follow: false
});

const videoInfo = ref(createDefaultVideoInfo());

const videoSources = computed(() => videoInfo.value.videoSourceList || []);
const currentUsernameInitial = computed(() => (getUsername() || 'U').slice(0, 1));
const isSelfVideo = computed(() => {
  const userId = getUserId();
  return Boolean(userId && videoInfo.value.userId && userId === videoInfo.value.userId);
});

function createDefaultVideoInfo() {
  return {
    videoId: null,
    userId: null,
    title: '',
    description: '',
    coverUrl: '',
    videoSourceList: [],
    partitionName: '',
    duration: 0,
    lastPlayTime: 0,
    username: '',
    avatarUrl: '',
    userBio: '',
    fansCount: 0,
    isFollow: false,
    playCount: 0,
    likeCount: 0,
    coinCount: 0,
    collectCount: 0,
    shareCount: 0,
    commentCount: 0,
    isLike: false,
    isCoin: false,
    isCollect: false,
    createTime: ''
  };
}

function normalizeVideoDetail(data) {
  return {
    ...createDefaultVideoInfo(),
    ...data,
    playCount: Number(data?.playCount || 0),
    likeCount: Number(data?.likeCount || 0),
    coinCount: Number(data?.coinCount || 0),
    collectCount: Number(data?.collectCount || 0),
    shareCount: Number(data?.shareCount || 0),
    commentCount: Number(data?.commentCount || 0),
    fansCount: Number(data?.fansCount || 0),
    duration: Number(data?.duration || 0),
    lastPlayTime: Number(data?.lastPlayTime || 0),
    isLike: Boolean(data?.isLike),
    isCoin: Boolean(data?.isCoin),
    isCollect: Boolean(data?.isCollect),
    isFollow: Boolean(data?.isFollow),
    videoSourceList: Array.isArray(data?.videoSourceList) ? data.videoSourceList : []
  };
}

async function loadVideoDetail() {
  if (!currentVideoId.value) return;
  loading.value = true;
  try {
    const data = await getVideoDetail(currentVideoId.value);
    videoInfo.value = normalizeVideoDetail(data);
    lastReportedTime.value = Number(videoInfo.value.lastPlayTime || 0);
  } catch (error) {
    console.error('加载视频详情失败:', error);
    ElMessage.error(error.message || '加载视频失败');
  } finally {
    loading.value = false;
  }
}

async function loadComments(isLoadMore = false) {
  if (!currentVideoId.value) return;
  commentLoading.value = true;
  try {
    const nextPage = isLoadMore ? commentPageNum.value + 1 : 1;
    const result = await getVideoComments(currentVideoId.value, {
      sortType: commentSort.value === 'hot' ? 1 : 2,
      pageNum: nextPage,
      pageSize: commentPageSize
    });
    const records = Array.isArray(result?.records) ? result.records : [];
    comments.value = isLoadMore ? [...comments.value, ...records] : records;
    commentTotal.value = Number(result?.total || 0);
    commentPageNum.value = Number(result?.pageNum || nextPage);
    hasMoreComments.value = comments.value.length < commentTotal.value;
  } catch (error) {
    console.error('加载评论失败:', error);
    ElMessage.error(error.message || '加载评论失败');
  } finally {
    commentLoading.value = false;
  }
}

async function submitComment() {
  const content = commentContent.value.trim();
  if (!content) return;
  commentSubmitting.value = true;
  try {
    await submitVideoComment({
      videoId: currentVideoId.value,
      rootId: 0,
      parentId: 0,
      replyUserId: 0,
      content
    });
    commentContent.value = '';
    videoInfo.value.commentCount += 1;
    commentTotal.value += 1;
    await loadComments(false);
    ElMessage.success('评论发布成功');
  } catch (error) {
    console.error('提交评论失败:', error);
    ElMessage.error(error.message || '评论发布失败');
  } finally {
    commentSubmitting.value = false;
  }
}

async function handleLike() {
  if (actionLoading.like) return;
  actionLoading.like = true;
  const nextIsLike = !videoInfo.value.isLike;
  try {
    await likeVideo({
      targetId: currentVideoId.value,
      type: 0,
      operation: nextIsLike ? 1 : 0
    });
    videoInfo.value.isLike = nextIsLike;
    videoInfo.value.likeCount = Math.max(0, Number(videoInfo.value.likeCount || 0) + (nextIsLike ? 1 : -1));
    ElMessage.success(nextIsLike ? '点赞成功' : '已取消点赞');
  } catch (error) {
    console.error('点赞失败:', error);
    ElMessage.error(error.message || '点赞失败');
  } finally {
    actionLoading.like = false;
  }
}

async function handleCoin() {
  if (actionLoading.coin) return;
  if (videoInfo.value.isCoin) {
    ElMessage.info('你已经给这个视频投过币了');
    return;
  }
  actionLoading.coin = true;
  try {
    await coinVideo({ videoId: currentVideoId.value, amount: 1 });
    videoInfo.value.isCoin = true;
    videoInfo.value.coinCount = Number(videoInfo.value.coinCount || 0) + 1;
    ElMessage.success('投币成功');
  } catch (error) {
    console.error('投币失败:', error);
    ElMessage.error(error.message || '投币失败');
  } finally {
    actionLoading.coin = false;
  }
}

async function ensureCollectionDirectory() {
  if (defaultCollectionDirectoryId.value) {
    return defaultCollectionDirectoryId.value;
  }

  const directories = await listCollectionDirectories();
  if (Array.isArray(directories) && directories.length > 0) {
    const target = directories.find(item => item.isDefault) || directories[0];
    defaultCollectionDirectoryId.value = target.directoryId;
    return target.directoryId;
  }

  const newDirectoryId = await createCollectionDirectory({
    name: '默认收藏夹',
    description: '播放页自动创建的收藏夹',
    coverUrl: '',
    isPublic: 0
  });
  defaultCollectionDirectoryId.value = newDirectoryId;
  return newDirectoryId;
}

async function handleCollect() {
  if (actionLoading.collect) return;
  actionLoading.collect = true;
  const nextIsCollect = !videoInfo.value.isCollect;
  try {
    const directoryId = await ensureCollectionDirectory();
    await collectVideo({
      videoId: currentVideoId.value,
      collectionDirectoryId: directoryId,
      operation: nextIsCollect ? 1 : 0
    });
    videoInfo.value.isCollect = nextIsCollect;
    videoInfo.value.collectCount = Math.max(0, Number(videoInfo.value.collectCount || 0) + (nextIsCollect ? 1 : -1));
    ElMessage.success(nextIsCollect ? '已加入收藏' : '已取消收藏');
  } catch (error) {
    console.error('收藏失败:', error);
    ElMessage.error(error.message || '收藏失败');
  } finally {
    actionLoading.collect = false;
  }
}

async function handleFollow() {
  if (isSelfVideo.value || actionLoading.follow || !videoInfo.value.userId) return;
  actionLoading.follow = true;
  const nextIsFollow = !videoInfo.value.isFollow;
  try {
    await followUser({
      followeeId: videoInfo.value.userId,
      operation: nextIsFollow ? 1 : 0
    });
    videoInfo.value.isFollow = nextIsFollow;
    videoInfo.value.fansCount = Math.max(0, Number(videoInfo.value.fansCount || 0) + (nextIsFollow ? 1 : -1));
    ElMessage.success(nextIsFollow ? '关注成功' : '已取消关注');
  } catch (error) {
    console.error('关注失败:', error);
    ElMessage.error(error.message || '关注失败');
  } finally {
    actionLoading.follow = false;
  }
}

async function handleLikeComment(comment) {
  if (!comment?.commentId) return;
  const nextIsLike = !comment.isLike;
  try {
    await likeComment({
      targetId: comment.commentId,
      type: 1,
      operation: nextIsLike ? 1 : 0
    });
    comment.isLike = nextIsLike;
    comment.likeCount = Math.max(0, Number(comment.likeCount || 0) + (nextIsLike ? 1 : -1));
  } catch (error) {
    console.error('评论点赞失败:', error);
    ElMessage.error(error.message || '评论点赞失败');
  }
}

function handleShare() {
  const url = window.location.href;
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(url).then(() => {
      ElMessage.success('链接已复制，快去分享吧');
    }).catch(() => {
      ElMessage.warning('复制失败，请手动复制地址栏链接');
    });
    return;
  }
  ElMessage.info(url);
}

function showReplyTip() {
  ElMessage.info('当前版本先支持一级评论，楼中楼回复后续可以继续补');
}

function goToUserProfile() {
  if (!videoInfo.value.userId) return;
  router.push(`/user/${videoInfo.value.userId}`);
}

async function handlePlaybackTimeUpdate(payload) {
  const currentSeconds = Math.floor(payload?.currentTime || 0);
  if (currentSeconds <= 0) return;
  await reportProgress(currentSeconds, false);
}

async function handleVideoEnded(payload) {
  const seconds = Math.floor(payload?.duration || payload?.currentTime || 0);
  if (!seconds) return;
  await reportProgress(seconds, true);
}

async function reportProgress(seconds, force) {
  if (reportInFlight.value || !currentVideoId.value || !seconds) return;
  if (!force && seconds - lastReportedTime.value < 15) return;

  reportInFlight.value = true;
  try {
    await reportVideoProgress({
      videoId: currentVideoId.value,
      lastPlayTime: seconds
    });
    lastReportedTime.value = seconds;
  } catch (error) {
    console.error('上报播放进度失败:', error);
  } finally {
    reportInFlight.value = false;
  }
}

async function flushProgressBeforeLeave() {
  if (!currentVideoId.value) return;
  const seconds = Math.floor(appVideoPlayerRef.value?.getCurrentTime?.() || 0);
  if (seconds > 0) {
    await reportProgress(seconds, true);
  }
}

function formatCount(count) {
  const value = Number(count || 0);
  if (value >= 100000000) return `${(value / 100000000).toFixed(1)}亿`;
  if (value >= 10000) return `${(value / 10000).toFixed(1)}万`;
  return `${value}`;
}

function formatDuration(seconds) {
  const totalSeconds = Number(seconds || 0);
  if (!totalSeconds) return '00:00';
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const secs = Math.floor(totalSeconds % 60);
  if (hours > 0) {
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }
  return `${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) return dateStr;

  const now = Date.now();
  const diff = now - date.getTime();
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;

  if (diff < hour) {
    return `${Math.max(1, Math.floor(diff / minute))} 分钟前`;
  }
  if (diff < day) {
    return `${Math.floor(diff / hour)} 小时前`;
  }
  if (diff < 30 * day) {
    return `${Math.floor(diff / day)} 天前`;
  }
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
}

async function loadPage() {
  comments.value = [];
  commentTotal.value = 0;
  hasMoreComments.value = false;
  commentPageNum.value = 1;
  videoInfo.value = createDefaultVideoInfo();
  await loadVideoDetail();
  await loadComments(false);
}

watch(commentSort, () => {
  loadComments(false);
});

watch(
  () => route.params.id,
  async (newId) => {
    const nextId = Number(newId) || 0;
    if (nextId === currentVideoId.value) return;
    await flushProgressBeforeLeave();
    currentVideoId.value = nextId;
    await loadPage();
  }
);

onMounted(() => {
  loadPage();
});

onBeforeUnmount(() => {
  flushProgressBeforeLeave();
});
</script>

<style scoped>
.line-clamp-3 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-3 {
  -webkit-line-clamp: 3;
}
</style>
