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
                @seek-commit="handleSeekCommit"
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

          <section v-if="videoInfo.description || videoInfo.tagList.length > 0" class="mt-4 rounded-2xl bg-white p-5 shadow-sm">
            <h2 class="mb-3 text-sm font-semibold uppercase tracking-[0.2em] text-gray-400">简介</h2>
            <p v-if="videoInfo.description" class="whitespace-pre-line text-sm leading-7 text-gray-600">
              {{ videoInfo.description }}
            </p>

            <div v-if="videoInfo.tagList.length > 0" class="mt-4 flex flex-wrap gap-2">
              <span
                v-for="tag in videoInfo.tagList"
                :key="tag"
                class="rounded-full bg-gray-100 px-3 py-1 text-sm font-medium text-gray-500"
              >
                {{ tag }}
              </span>
            </div>
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
                  :readonly="!viewerLoggedIn"
                  :placeholder="viewerLoggedIn ? '发条友善的评论吧' : '登录后参与评论互动'"
                  @focus="handleCommentFocus"
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
                      <button class="transition-colors hover:text-[#00a1d6]" @click="handleReply">
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

    <div
      v-if="collectDialog.visible"
      class="fixed inset-0 z-40 flex items-center justify-center bg-black/55 p-4"
      @click.self="closeCollectDialog"
    >
      <div class="w-full max-w-3xl rounded-xl bg-white px-8 py-6 shadow-[0_28px_80px_rgba(15,23,42,0.28)]">
        <div class="flex items-center justify-between border-b border-gray-100 pb-4">
          <h3 class="mx-auto text-3xl font-medium tracking-[0.08em] text-gray-900">添加到收藏夹</h3>
          <button class="ml-4 text-4xl leading-none text-gray-300 transition hover:text-gray-500" @click="closeCollectDialog">×</button>
        </div>

        <div class="mt-4 max-h-[420px] overflow-y-auto pr-2">
          <div v-if="collectDialog.loading" class="flex h-56 items-center justify-center text-sm text-gray-400">
            正在加载收藏夹...
          </div>

          <div v-else-if="collectDialog.directories.length === 0" class="flex h-56 items-center justify-center text-sm text-gray-400">
            暂无可用收藏夹
          </div>

          <button
            v-for="directory in collectDialog.directories"
            :key="directory.directoryId"
            class="flex w-full items-center justify-between gap-6 border-b border-gray-100 px-1 py-7 text-left transition hover:bg-gray-50/80"
            type="button"
            @click="toggleCollectDirectorySelection(directory.directoryId)"
          >
            <div class="flex min-w-0 items-center gap-8">
              <span
                class="flex h-8 w-8 items-center justify-center rounded border transition"
                :class="isCollectDialogSelected(directory.directoryId) ? 'border-[#00a1d6] bg-[#00a1d6]/10' : 'border-[#cfd6df] bg-white'"
              >
                <span
                  v-if="isCollectDialogSelected(directory.directoryId)"
                  class="h-4 w-4 rounded-sm bg-[#00a1d6]"
                ></span>
              </span>
              <div class="min-w-0">
                <p class="text-[1.125rem] font-medium text-gray-900">
                  {{ directory.directoryName }}
                  <span v-if="!directory.isPublic" class="ml-1 text-gray-400">[私密]</span>
                  <span v-else class="ml-1 text-gray-400">[公开]</span>
                </p>
                <p class="mt-2 text-sm text-gray-400">
                  {{ isCollectDialogSelected(directory.directoryId) ? '已选择' : '未选择' }}
                </p>
              </div>
            </div>
            <div class="shrink-0 text-right text-[1.125rem] text-gray-500">
              <span v-if="directory.isDefault" class="rounded-full bg-[#eef7ff] px-3 py-1 text-sm text-[#00a1d6]">默认收藏夹</span>
            </div>
          </button>
        </div>

        <div class="mt-8 border-t border-gray-100 pt-7 text-center">
          <button
            class="min-w-[240px] rounded-lg px-8 py-4 text-xl font-medium transition"
            :class="collectDialogHasChanges ? 'bg-[#00a1d6] text-white hover:bg-[#00b5e5]' : 'bg-gray-200 text-gray-400'"
            :disabled="collectDialog.submitting || !collectDialogHasChanges"
            @click="submitCollectSelection"
          >
            {{ collectDialog.submitting ? '提交中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="coinDialog.visible"
      class="fixed inset-0 z-40 flex items-center justify-center bg-black/55 p-4"
      @click.self="closeCoinDialog"
    >
      <div class="w-full max-w-4xl rounded-xl bg-white px-8 py-6 shadow-[0_28px_80px_rgba(15,23,42,0.28)]">
        <div class="flex items-center justify-between">
          <h3 class="flex-1 text-center text-[2.1rem] font-medium tracking-[0.04em] text-gray-900">
            给UP主投上 <span class="px-1 text-[#00a1d6]">{{ coinDialog.amount }}</span> 枚硬币
          </h3>
          <button class="ml-4 text-4xl leading-none text-gray-300 transition hover:text-gray-500" @click="closeCoinDialog">×</button>
        </div>

        <div class="mt-10 grid gap-6 md:grid-cols-2">
          <button
            v-for="option in coinOptions"
            :key="option.amount"
            class="group rounded-2xl border-2 border-dashed p-6 text-left transition"
            :class="coinDialog.amount === option.amount ? 'border-[#00a1d6] bg-[#f2fbff]' : 'border-[#d5dbe3] hover:border-[#8bd8f2]'"
            @click="coinDialog.amount = option.amount"
          >
            <div class="text-2xl font-medium" :class="coinDialog.amount === option.amount ? 'text-[#00a1d6]' : 'text-gray-500'">
              {{ option.amount }}硬币
            </div>
            <div class="mt-6 flex min-h-[220px] items-center justify-center rounded-xl bg-[radial-gradient(circle_at_top,_rgba(0,161,214,0.12),_transparent_55%),linear-gradient(180deg,_rgba(248,250,252,0.95),_rgba(241,245,249,0.85))]">
              <div class="relative flex items-end justify-center gap-3">
                <div
                  v-for="coin in option.amount"
                  :key="`coin-${option.amount}-${coin}`"
                  class="coin-dialog-coin"
                  :class="coin === 2 ? '-ml-5 mt-3' : ''"
                >
                  币
                </div>
              </div>
            </div>
          </button>
        </div>

        <label class="mt-7 flex cursor-pointer items-center gap-3 text-xl text-gray-700">
          <span
            class="flex h-7 w-7 items-center justify-center rounded bg-[#00a1d6] text-white"
            :class="coinDialog.alsoLike ? 'opacity-100' : 'bg-white text-transparent ring-1 ring-[#cfd6df]'"
          >
            ✓
          </span>
          <input v-model="coinDialog.alsoLike" type="checkbox" class="sr-only" />
          同时点赞内容
        </label>

        <div class="mt-10 text-center">
          <button
            class="min-w-[120px] rounded-lg bg-[#00a1d6] px-8 py-4 text-2xl font-medium text-white transition hover:bg-[#00b5e5] disabled:cursor-not-allowed disabled:bg-[#9ddff3]"
            :disabled="coinDialog.submitting"
            @click="submitCoinSelection"
          >
            {{ coinDialog.submitting ? '提交中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>
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
  queryVideoDirectoryRelations,
  reportVideoProgress,
  submitVideoComment
} from '../api/video';
import {
  getUserId,
  getUsername,
  isUserLoggedIn,
  openLoginModal,
  USER_AUTH_CHANGE_EVENT
} from '../utils/auth';

const route = useRoute();
const router = useRouter();
const viewerLoggedIn = ref(isUserLoggedIn());
const currentUsername = ref(getUsername());

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

const collectDialog = reactive({
  visible: false,
  loading: false,
  submitting: false,
  directories: [],
  selectedDirectoryIds: [],
  initialSelectedDirectoryIds: []
});

const coinDialog = reactive({
  visible: false,
  submitting: false,
  amount: 2,
  alsoLike: true
});

const coinOptions = [
  { amount: 1 },
  { amount: 2 }
];

const actionLoading = reactive({
  like: false,
  coin: false,
  collect: false,
  follow: false
});

const videoInfo = ref(createDefaultVideoInfo());

const videoSources = computed(() => videoInfo.value.videoSourceList || []);
const currentUsernameInitial = computed(() => (currentUsername.value || 'U').slice(0, 1));
const isSelfVideo = computed(() => {
  const userId = getUserId();
  return Boolean(userId && videoInfo.value.userId && userId === videoInfo.value.userId);
});
const collectDialogHasChanges = computed(() => {
  const current = [...collectDialog.selectedDirectoryIds].sort((a, b) => a - b);
  const initial = [...collectDialog.initialSelectedDirectoryIds].sort((a, b) => a - b);
  if (current.length !== initial.length) return true;
  return current.some((id, index) => id !== initial[index]);
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
    tagList: [],
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
    tagList: Array.isArray(data?.tagList) ? data.tagList : [],
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
  if (!ensureLoggedIn('登录后才可以发表评论')) return;
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
  if (!ensureLoggedIn('登录后可点赞视频')) return;
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
  if (!ensureLoggedIn('登录后可给视频投币')) return;
  if (actionLoading.coin) return;
  coinDialog.visible = true;
  coinDialog.amount = videoInfo.value.isCoin ? 1 : 2;
  coinDialog.alsoLike = true;
}

function closeCoinDialog() {
  if (coinDialog.submitting) return;
  coinDialog.visible = false;
}

async function submitCoinSelection() {
  if (actionLoading.coin || !currentVideoId.value) return;
  actionLoading.coin = true;
  coinDialog.submitting = true;
  try {
    await coinVideo({
      videoId: currentVideoId.value,
      amount: coinDialog.amount
    });
    videoInfo.value.isCoin = true;
    videoInfo.value.coinCount = Number(videoInfo.value.coinCount || 0) + coinDialog.amount;

    let likedByCoin = false;
    if (coinDialog.alsoLike && !videoInfo.value.isLike) {
      try {
        await likeVideo({
          targetId: currentVideoId.value,
          type: 0,
          operation: 1
        });
        videoInfo.value.isLike = true;
        videoInfo.value.likeCount = Number(videoInfo.value.likeCount || 0) + 1;
        likedByCoin = true;
      } catch (error) {
        console.error('投币后点赞失败:', error);
        ElMessage.warning('投币成功，但自动点赞失败');
      }
    }

    coinDialog.visible = false;
    ElMessage.success(likedByCoin ? '投币并点赞成功' : '投币成功');
  } catch (error) {
    console.error('投币失败:', error);
    ElMessage.error(error.message || '投币失败');
  } finally {
    coinDialog.submitting = false;
    actionLoading.coin = false;
  }
}

async function ensureCollectDialogDirectories() {
  const relations = await queryVideoDirectoryRelations(currentVideoId.value);
  return Array.isArray(relations) ? relations : [];
}

async function handleCollect() {
  if (!ensureLoggedIn('登录后可收藏视频')) return;
  if (actionLoading.collect || !currentVideoId.value) return;

  collectDialog.visible = true;
  collectDialog.loading = true;
  collectDialog.directories = [];
  collectDialog.selectedDirectoryIds = [];
  collectDialog.initialSelectedDirectoryIds = [];
  try {
    const directories = await ensureCollectDialogDirectories();
    collectDialog.directories = directories;
    const collectedIds = directories
      .filter(item => item.isCollect)
      .map(item => Number(item.directoryId));
    collectDialog.selectedDirectoryIds = [...collectedIds];
    collectDialog.initialSelectedDirectoryIds = [...collectedIds];
  } catch (error) {
    console.error('加载收藏夹失败:', error);
    ElMessage.error(error.message || '加载收藏夹失败');
    collectDialog.visible = false;
  } finally {
    collectDialog.loading = false;
  }
}

function isCollectDialogSelected(directoryId) {
  return collectDialog.selectedDirectoryIds.includes(Number(directoryId));
}

function toggleCollectDirectorySelection(directoryId) {
  if (collectDialog.submitting) return;
  const normalizedId = Number(directoryId);
  if (isCollectDialogSelected(normalizedId)) {
    collectDialog.selectedDirectoryIds = collectDialog.selectedDirectoryIds.filter(id => id !== normalizedId);
    return;
  }
  collectDialog.selectedDirectoryIds = [...collectDialog.selectedDirectoryIds, normalizedId];
}

function closeCollectDialog() {
  if (collectDialog.submitting) return;
  collectDialog.visible = false;
  collectDialog.directories = [];
  collectDialog.selectedDirectoryIds = [];
  collectDialog.initialSelectedDirectoryIds = [];
}

async function submitCollectSelection() {
  if (actionLoading.collect || !currentVideoId.value) return;

  const initialSelectedDirectoryIds = [...collectDialog.initialSelectedDirectoryIds];
  const selectedDirectoryIds = [...collectDialog.selectedDirectoryIds];
  const initialDirectorySet = new Set(initialSelectedDirectoryIds.map(Number));
  const currentDirectorySet = new Set(selectedDirectoryIds.map(Number));
  const collectDirectoryIdList = [...currentDirectorySet].filter(id => !initialDirectorySet.has(id));
  const removeDirectoryIdList = [...initialDirectorySet].filter(id => !currentDirectorySet.has(id));

  if (collectDirectoryIdList.length === 0 && removeDirectoryIdList.length === 0) {
    closeCollectDialog();
    return;
  }

  closeCollectDialog();
  actionLoading.collect = true;
  collectDialog.submitting = true;
  try {
    const beforeCollect = initialSelectedDirectoryIds.length > 0;
    await collectVideo({
      videoId: currentVideoId.value,
      collectDirectoryIdList,
      removeDirectoryIdList
    });

    const afterCollect = selectedDirectoryIds.length > 0;
    videoInfo.value.isCollect = afterCollect;
    if (!beforeCollect && afterCollect) {
      videoInfo.value.collectCount = Number(videoInfo.value.collectCount || 0) + 1;
    } else if (beforeCollect && !afterCollect) {
      videoInfo.value.collectCount = Math.max(0, Number(videoInfo.value.collectCount || 0) - 1);
    }

    ElMessage.success('收藏夹已更新');
  } catch (error) {
    console.error('更新收藏夹失败:', error);
    ElMessage.error(error.message || '更新收藏夹失败');
  } finally {
    collectDialog.submitting = false;
    actionLoading.collect = false;
  }
}

async function handleFollow() {
  if (!ensureLoggedIn('登录后可关注 UP 主')) return;
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
  if (!ensureLoggedIn('登录后可点赞评论')) return;
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

function handleReply() {
  if (!ensureLoggedIn('登录后可参与评论回复')) return;
  ElMessage.info('当前版本先支持一级评论，楼中楼回复后续可以继续补');
}

function goToUserProfile() {
  if (!ensureLoggedIn('登录后可查看用户主页')) return;
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

async function handleSeekCommit(payload) {
  const seconds = Math.floor(payload?.currentTime || 0);
  if (seconds <= 0) return;
  await reportProgress(seconds, true);
}

async function reportProgress(seconds, force) {
  if (!viewerLoggedIn.value) return;
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
  if (!viewerLoggedIn.value) return;
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

function syncViewerAuth(event) {
  viewerLoggedIn.value = event?.detail?.isLoggedIn ?? isUserLoggedIn();
  currentUsername.value = event?.detail?.username ?? getUsername();
}

function promptLogin(message) {
  openLoginModal({ source: 'video-player' });
  if (message) {
    ElMessage.info(message);
  }
}

function ensureLoggedIn(message) {
  if (viewerLoggedIn.value) return true;
  promptLogin(message);
  return false;
}

function handleCommentFocus() {
  if (!viewerLoggedIn.value) {
    promptLogin('登录后才可以发表评论');
  }
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
  syncViewerAuth();
  window.addEventListener(USER_AUTH_CHANGE_EVENT, syncViewerAuth);
  loadPage();
});

onBeforeUnmount(() => {
  flushProgressBeforeLeave();
  window.removeEventListener(USER_AUTH_CHANGE_EVENT, syncViewerAuth);
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

.coin-dialog-coin {
  display: flex;
  height: 5.75rem;
  width: 5.75rem;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  border: 2px solid #b4bac3;
  background: radial-gradient(circle at 30% 30%, #ffffff, #d8dbe1 58%, #b9bec7 100%);
  color: #808892;
  font-size: 2rem;
  font-weight: 700;
  box-shadow:
    inset 0 2px 4px rgba(255, 255, 255, 0.8),
    0 10px 18px rgba(148, 163, 184, 0.22);
}
</style>
