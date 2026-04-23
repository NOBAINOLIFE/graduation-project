<template>
  <section class="rounded-lg bg-white p-6 shadow-sm ring-1 ring-slate-200">
    <div class="mb-5 flex flex-wrap items-center gap-3">
      <h2 class="text-lg font-semibold">视频审核</h2>
      <select v-model.number="filters.status" class="rounded border border-slate-200 px-3 py-1.5 text-sm focus:border-[#00a1d6] focus:outline-none">
        <option :value="-1">全部</option>
        <option :value="0">审核中</option>
        <option :value="1">已通过</option>
        <option :value="2">已驳回</option>
      </select>
      <span class="text-xs text-slate-400">切换状态自动查询</span>
    </div>

    <div class="space-y-4">
      <article v-for="item in records" :key="item.videoId" class="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <div class="flex flex-wrap items-center gap-4 p-4 md:flex-nowrap">
          <div class="relative h-[108px] w-[192px] shrink-0 overflow-hidden rounded bg-slate-100 ring-1 ring-slate-200">
            <img :src="item.coverUrl || fallbackCover" class="h-full w-full object-cover" alt="cover" />
            <span class="absolute bottom-1 right-1 rounded bg-black/70 px-1.5 py-0.5 text-xs text-white">
              {{ formatDurationLabel(item.videoId) }}
            </span>
          </div>

          <div class="min-w-[220px] flex-1">
            <div class="flex items-center gap-3">
              <img :src="item.avatar || fallbackAvatar" class="h-10 w-10 rounded-full object-cover ring-1 ring-slate-200" alt="avatar" />
              <div>
                <p class="font-medium text-slate-900">{{ item.username || '未知用户' }}</p>
                <p class="text-xs text-slate-500">用户ID：{{ item.userId || '-' }}</p>
              </div>
            </div>
            <p class="mt-3 line-clamp-1 text-sm text-slate-700">{{ item.title || '-' }}</p>
            <p class="mt-1 text-xs text-slate-500">状态：{{ statusText(item.status) }} ｜ 提交时间：{{ formatTime(item.createTime) }}</p>
          </div>

          <div class="flex shrink-0 items-center gap-2">
            <div v-if="item.status === 0" class="flex items-center gap-2">
              <button
                class="rounded bg-emerald-500 px-3 py-1.5 text-xs text-white hover:bg-emerald-600 disabled:cursor-not-allowed disabled:bg-slate-300"
                :disabled="operatingVideoId === item.videoId"
                @click="submitAudit(item.videoId, 1, '')"
              >
                通过
              </button>
              <button
                class="rounded bg-rose-500 px-3 py-1.5 text-xs text-white hover:bg-rose-600 disabled:cursor-not-allowed disabled:bg-slate-300"
                :disabled="operatingVideoId === item.videoId"
                @click="openRejectDialog(item.videoId)"
              >
                驳回
              </button>
            </div>
            <div v-else class="min-w-[220px]">
              <div class="flex items-center gap-2">
                <img :src="item.reviewerAvatar || fallbackAvatar" class="h-8 w-8 rounded-full object-cover ring-1 ring-slate-200" alt="reviewer-avatar" />
                <div class="leading-tight">
                  <p class="text-sm font-medium text-slate-800">{{ item.reviewerName || '未知管理员' }}</p>
                  <p class="text-xs text-slate-500">管理员ID：{{ item.reviewerId || '-' }}</p>
                </div>
              </div>
              <p class="mt-1 text-xs text-slate-500">审核时间：{{ formatTime(item.updateTime) }}</p>
              <p v-if="item.status === 2" class="mt-1 text-xs text-rose-500">驳回原因：{{ item.reviewNote || '-' }}</p>
            </div>
            <button
              class="ml-1 flex h-8 w-8 items-center justify-center rounded border border-slate-200 text-slate-500 hover:bg-slate-50"
              @click="toggleExpand(item.videoId)"
            >
              <svg class="h-4 w-4 transition-transform" :class="expandedVideoId === item.videoId ? 'rotate-180' : ''" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
              </svg>
            </button>
          </div>
        </div>

        <div v-if="expandedVideoId === item.videoId" class="border-t border-slate-200 bg-slate-50 p-4">
          <div class="grid gap-4 lg:grid-cols-[1fr_240px]">
            <div class="space-y-2 text-sm">
              <p><span class="text-slate-500">标题：</span>{{ item.title || '-' }}</p>
              <p><span class="text-slate-500">描述：</span>{{ item.description || '-' }}</p>
              <p><span class="text-slate-500">视频ID：</span>{{ item.videoId }}</p>
              <p><span class="text-slate-500">分区：</span>{{ item.partitionName || '-' }}</p>
              <div class="flex flex-wrap items-center gap-2">
                <span class="text-slate-500">标签：</span>
                <span
                  v-for="tag in item.tagList || []"
                  :key="tag.tagId || tag.tagName"
                  class="rounded bg-[#e8f7fd] px-2 py-0.5 text-xs text-[#00a1d6]"
                >
                  {{ tag.tagName }}
                </span>
                <span v-if="!item.tagList || !item.tagList.length" class="text-slate-400">-</span>
              </div>
            </div>

            <div>
              <button class="group block w-full overflow-hidden rounded-lg ring-1 ring-slate-200" @click="openCoverPreview(item.coverUrl)">
                <img :src="item.coverUrl || fallbackCover" class="h-32 w-full object-cover transition group-hover:scale-[1.02]" alt="cover" />
              </button>
              <p class="mt-1 text-center text-xs text-slate-500">点击放大封面</p>
            </div>
          </div>

          <div class="mt-4 rounded-lg bg-black p-2">
            <p class="mb-2 text-xs text-white/80">视频预览</p>

            <div
              v-if="currentSourceUrl(item)"
              :id="`player-container-${item.videoId}`"
              class="group relative mt-2 text-xs text-white"
            >
              <video
                :ref="(el) => setVideoRef(item.videoId, el)"
                :src="''"
                class="h-full w-full rounded bg-black cursor-pointer object-contain"
                preload="metadata"
                @loadedmetadata="syncPlayerState(item.videoId)"
                @canplay="applyPendingResolutionRestore(item.videoId)"
                @play="setPlaying(item.videoId, true)"
                @pause="setPlaying(item.videoId, false)"
                @click="togglePlay(item.videoId)"
              ></video>

              <!-- 进度条放在控制栏上方 -->
              <div class="absolute bottom-[44px] left-0 right-0 z-20 px-4 opacity-0 group-hover:opacity-100 transition-opacity">
                <input
                  type="range"
                  min="0"
                  :max="Math.max(1, playerDuration(item.videoId))"
                  step="0.1"
                  class="h-1 w-full accent-[#00a1d6] cursor-pointer"
                  :value="playerCurrentTime(item.videoId)"
                  @input="seekVideo(item.videoId, $event)"
                />
              </div>
              <div class="absolute bottom-0 left-0 right-0 flex items-center justify-between px-4 py-2 rounded-b-lg bg-black/70 backdrop-blur-sm opacity-0 group-hover:opacity-100 transition-opacity z-10">
                <!-- 左侧：播放/暂停、时间 -->
                <div class="flex items-center gap-3 flex-1 min-w-0">
                  <button class="p-1 hover:bg-white/10 rounded" @click="togglePlay(item.videoId)">
                    <!-- 播放/暂停图标 -->
                    <span v-if="!isPlaying(item.videoId)">
                      <svg width="20" height="20" fill="currentColor" viewBox="0 0 20 20"><polygon points="5,3 19,10 5,17"/></svg>
                    </span>
                    <span v-else>
                      <svg width="20" height="20" fill="currentColor" viewBox="0 0 20 20"><rect x="4" y="3" width="4" height="14"/><rect x="12" y="3" width="4" height="14"/></svg>
                    </span>
                  </button>
                  <span class="w-[92px] text-center text-white/80 shrink-0">{{ formatPlaybackLabel(item.videoId) }}</span>
                </div>
                <!-- 右侧：音量、静音、倍速、分辨率、全屏 -->
                <div class="flex items-center gap-2 ml-4">
                  <button class="p-1 hover:bg-white/10 rounded" @click="toggleMute(item.videoId)">
                    <!-- 静音/音量图标 -->
                    <span v-if="playerMuted(item.videoId)">
                      <svg width="18" height="18" fill="currentColor" viewBox="0 0 20 20"><path d="M9 7H5v6h4l5 5V2l-5 5z"/><line x1="1" y1="1" x2="19" y2="19" stroke="currentColor" stroke-width="2"/></svg>
                    </span>
                    <span v-else>
                      <svg width="18" height="18" fill="currentColor" viewBox="0 0 20 20"><path d="M9 7H5v6h4l5 5V2l-5 5z"/></svg>
                    </span>
                  </button>
                  <input
                    type="range"
                    min="0"
                    max="1"
                    step="0.05"
                    class="h-1 w-20 accent-[#00a1d6]"
                    :value="playerVolume(item.videoId)"
                    @input="setVolume(item.videoId, $event)"
                  />
                  <select
                    v-model.number="playbackRateMap[item.videoId]"
                    class="rounded border border-white/20 bg-black px-2 py-1 text-xs text-white focus:outline-none"
                    @change="changePlaybackRate(item.videoId)"
                  >
                    <option v-for="rate in playbackRateOptions" :key="`rate-${rate}`" :value="rate">{{ rate }}x</option>
                  </select>
                  <select
                    v-if="resolutionOptions(item).length > 1"
                    :value="selectedResolutionValue(item)"
                    class="rounded border border-white/20 bg-black px-2 py-1 text-xs text-white focus:outline-none"
                    :disabled="isResolutionDisabled(item)"
                    @change="changeResolution(item, $event)"
                  >
                    <option v-for="option in resolutionOptions(item)" :key="`${item.videoId}-${option.value}`" :value="option.value">
                      {{ option.label }}
                    </option>
                  </select>
                  <!-- 全屏按钮 -->
                  <button class="p-1 hover:bg-white/10 rounded" @click="toggleFullscreen(item.videoId)" title="全屏">
                    <svg width="18" height="18" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M3 3h5v2H5v3H3V3zm14 0h-5v2h3v3h2V3zM3 17h5v-2H5v-3H3v5zm14 0h-5v-2h3v-3h2v5z"/>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </article>

      <div v-if="!records.length && !loading" class="py-8 text-center text-slate-400">暂无数据</div>
    </div>

    <div class="mt-4 flex items-center justify-between text-sm text-slate-500">
      <p>共 {{ pagination.total }} 条</p>
      <div class="flex items-center gap-2">
        <button
          class="rounded border border-slate-200 px-3 py-1 hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-300"
          :disabled="pagination.pageNum <= 1 || loading"
          @click="changePage(pagination.pageNum - 1)"
        >上一页</button>
        <span>第 {{ pagination.pageNum }} / {{ totalPages }} 页</span>
        <button
          class="rounded border border-slate-200 px-3 py-1 hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-300"
          :disabled="pagination.pageNum >= totalPages || loading"
          @click="changePage(pagination.pageNum + 1)"
        >下一页</button>
      </div>
    </div>
  </section>

  <div v-if="previewCoverUrl" class="fixed inset-0 z-40 flex items-center justify-center bg-black/70 p-4" @click="previewCoverUrl = ''">
    <img :src="previewCoverUrl" class="max-h-[90vh] max-w-[90vw] rounded bg-white" alt="cover-preview" />
  </div>

  <div v-if="rejectDialog.visible" class="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4" @click.self="closeRejectDialog">
    <div class="w-full max-w-3xl rounded-xl bg-white p-6 shadow-xl">
      <div class="mb-4 flex items-center justify-between">
        <h3 class="text-xl font-semibold">驳回原因</h3>
        <button class="text-slate-400 hover:text-slate-600" @click="closeRejectDialog">✕</button>
      </div>

      <p class="mb-3 text-sm text-slate-500">请选择违规原因</p>
      <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <label
          v-for="option in rejectReasonOptions"
          :key="option"
          class="flex cursor-pointer items-center gap-2 rounded border border-slate-200 px-3 py-2 text-sm hover:border-[#00a1d6]"
        >
          <input v-model="rejectDialog.reason" type="radio" :value="option" class="accent-[#00a1d6]" />
          <span>{{ option }}</span>
        </label>
      </div>

      <div class="mt-4">
        <p class="mb-1 text-sm text-slate-500">详细描述 <span class="text-rose-500">*</span></p>
        <textarea
          v-model="rejectDialog.detail"
          maxlength="400"
          rows="4"
          placeholder="请填写驳回原因详情，便于投稿用户修改后重新提交"
          class="w-full rounded border border-slate-200 px-3 py-2 text-sm focus:border-[#00a1d6] focus:outline-none"
        ></textarea>
        <p class="mt-1 text-right text-xs text-slate-400">{{ rejectDialog.detail.length }}/400</p>
      </div>

      <div class="mt-5 flex justify-end gap-3">
        <button class="rounded border border-slate-200 px-6 py-2 text-sm text-slate-600 hover:bg-slate-50" @click="closeRejectDialog">取消</button>
        <button
          class="rounded bg-[#00a1d6] px-6 py-2 text-sm text-white hover:bg-[#00b5e5] disabled:cursor-not-allowed disabled:bg-slate-300"
          :disabled="operatingVideoId === rejectDialog.videoId"
          @click="confirmReject"
        >
          提交
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import Hls from 'hls.js';
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { auditVideo, queryAuditVideoList } from '../../api/manager';

const fallbackCover =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="180"><rect width="100%" height="100%" fill="%23e2e8f0"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%2364748b" font-size="14">No Cover</text></svg>';
const fallbackAvatar =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80"><circle cx="40" cy="40" r="40" fill="%23e2e8f0"/></svg>';

const filters = reactive({
  status: -1
});

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

const loading = ref(false);
const operatingVideoId = ref(null);
const records = ref([]);
const expandedVideoId = ref(null);
const sourceIndexMap = ref({});
const previewCoverUrl = ref('');
const durationMap = ref({});
const playerRefMap = ref({});
const playerStateMap = ref({});
const pendingResolutionRestoreMap = ref({});
const resolutionSwitchingMap = ref({});
const hlsMap = ref({});
const hlsLevelOptionsMap = ref({});
const selectedHlsLevelMap = ref({});
const boundSourceUrlMap = ref({});
const playbackRateMap = ref({});
const playbackRateOptions = [0.75, 1, 1.25, 1.5, 2];

const rejectReasonOptions = [
  '色情低俗',
  '违规广告引流',
  '涉政敏感',
  '引战、网暴、不友善',
  '传播谣言',
  '涉欺诈骗',
  '引人不适',
  '涉未成年人不良信息',
  '封面党、标题党',
  '其他'
];

const rejectDialog = reactive({
  visible: false,
  videoId: null,
  reason: '',
  detail: ''
});

const totalPages = computed(() => {
  const pages = Math.ceil((pagination.total || 0) / pagination.pageSize);
  return pages > 0 ? pages : 1;
});

function statusText(status) {
  if (status === 0) return '审核中';
  if (status === 1) return '已通过';
  if (status === 2) return '已驳回';
  return `状态${status}`;
}

function formatDuration(seconds) {
  if (!Number.isFinite(seconds) || seconds < 0) return '--:--';
  const total = Math.floor(seconds);
  const hours = Math.floor(total / 3600);
  const mins = Math.floor((total % 3600) / 60);
  const secs = total % 60;
  if (hours > 0) {
    return `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

function formatDurationLabel(videoId) {
  return formatDuration(durationMap.value[videoId]);
}

function formatPlaybackSeconds(seconds) {
  if (!Number.isFinite(seconds) || seconds < 0) return '00:00';
  const total = Math.floor(seconds);
  const mins = Math.floor(total / 60);
  const secs = total % 60;
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

function formatTime(time) {
  if (!time) return '-';
  return String(time).replace('T', ' ');
}

function currentSourceUrl(item) {
  const sourceList = item.videoSourceList || [];
  if (!sourceList.length) return '';
  const selectedIndex = sourceIndexMap.value[item.videoId] ?? findDefaultSourceIndex(item);
  return sourceList[selectedIndex]?.playUrl || sourceList[0]?.playUrl || '';
}

function findDefaultSourceIndex(item) {
  const sourceList = item?.videoSourceList || [];
  if (!sourceList.length) return 0;
  const masterIndex = sourceList.findIndex((source) => {
    const resolution = String(source?.resolution || '').toLowerCase();
    const url = String(source?.playUrl || '').toLowerCase();
    return resolution.includes('自适应') || resolution.includes('master') || url.includes('/master.m3u8');
  });
  return masterIndex >= 0 ? masterIndex : 0;
}

function isM3u8Url(url) {
  return /\.m3u8($|[?#])/i.test(url || '');
}

function getRecordById(videoId) {
  return records.value.find((item) => item.videoId === videoId) || null;
}

function isUsingHlsLevels(videoId) {
  return Array.isArray(hlsLevelOptionsMap.value[videoId]) && hlsLevelOptionsMap.value[videoId].length > 0;
}

function resolutionOptions(item) {
  const videoId = item.videoId;
  if (isUsingHlsLevels(videoId)) {
    return hlsLevelOptionsMap.value[videoId];
  }
  return (item.videoSourceList || []).map((source, idx) => ({
    value: idx,
    label: source.resolution || `源${idx + 1}`
  }));
}

function selectedResolutionValue(item) {
  const videoId = item.videoId;
  if (isUsingHlsLevels(videoId)) {
    return selectedHlsLevelMap.value[videoId] ?? -1;
  }
  return sourceIndexMap.value[videoId] ?? findDefaultSourceIndex(item);
}

function isResolutionDisabled(item) {
  return Boolean(resolutionSwitchingMap.value[item.videoId]);
}

function destroyHls(videoId) {
  const hls = hlsMap.value[videoId];
  if (hls) {
    hls.destroy();
    delete hlsMap.value[videoId];
  }
  delete hlsLevelOptionsMap.value[videoId];
  delete selectedHlsLevelMap.value[videoId];
}

function buildHlsLevelOptions(levels) {
  const options = [{ value: -1, label: '自动' }];
  (levels || []).forEach((level, idx) => {
    const height = Number(level?.height || 0);
    const label = height > 0 ? `${height}p` : `清晰度${idx + 1}`;
    options.push({ value: idx, label });
  });
  return options;
}

function setupHls(videoId, url) {
  const player = playerRefMap.value[videoId];
  if (!player) return;

  destroyHls(videoId);

  if (Hls.isSupported()) {
    const hls = new Hls({
      enableWorker: true,
      lowLatencyMode: true,
      backBufferLength: 90
    });
    hlsMap.value[videoId] = hls;
    hls.attachMedia(player);
    hls.on(Hls.Events.MEDIA_ATTACHED, () => {
      hls.loadSource(url);
    });
    hls.on(Hls.Events.MANIFEST_PARSED, (_, data) => {
      hlsLevelOptionsMap.value[videoId] = buildHlsLevelOptions(data?.levels || []);
      const initialLevel = selectedHlsLevelMap.value[videoId] ?? -1;
      selectedHlsLevelMap.value[videoId] = initialLevel;
      hls.currentLevel = initialLevel;
      resolutionSwitchingMap.value[videoId] = false;
    });
    hls.on(Hls.Events.LEVEL_SWITCHED, (_, data) => {
      selectedHlsLevelMap.value[videoId] = Number.isFinite(data?.level) ? data.level : -1;
      resolutionSwitchingMap.value[videoId] = false;
    });
    hls.on(Hls.Events.ERROR, (_, data) => {
      if (!data?.fatal) return;
      resolutionSwitchingMap.value[videoId] = false;
    });
    return;
  }

  if (player.canPlayType('application/vnd.apple.mpegurl')) {
    player.src = url;
    player.load();
    return;
  }

  resolutionSwitchingMap.value[videoId] = false;
}

function ensureVideoSourceBinding(videoId) {
  const item = getRecordById(videoId);
  const player = playerRefMap.value[videoId];
  if (!item || !player) return;

  const url = currentSourceUrl(item);
  if (!url) return;
  const prevUrl = boundSourceUrlMap.value[videoId];
  const hls = hlsMap.value[videoId];
  const hasBoundCurrentHls = Boolean(hls && hls.media === player);

  if (prevUrl === url && (isM3u8Url(url) ? hasBoundCurrentHls : player.src === url)) {
    return;
  }

  boundSourceUrlMap.value[videoId] = url;
  if (isM3u8Url(url)) {
    setupHls(videoId, url);
    return;
  }

  destroyHls(videoId);
  player.src = url;
  player.load();
}

function ensurePlayerState(videoId) {
  if (!playerStateMap.value[videoId]) {
    playerStateMap.value[videoId] = {
      currentTime: 0,
      duration: 0,
      volume: 1,
      muted: false,
      playing: false
    };
  }
  if (playbackRateMap.value[videoId] === undefined) {
    playbackRateMap.value[videoId] = 1;
  }
  return playerStateMap.value[videoId];
}

function syncPlayerState(videoId) {
  const player = playerRefMap.value[videoId];
  if (!player) return;

  const state = ensurePlayerState(videoId);
  state.currentTime = player.currentTime || 0;
  state.duration = Number.isFinite(player.duration) ? player.duration : 0;
  state.volume = player.volume;
  state.muted = player.muted;
  state.playing = !player.paused;
  player.playbackRate = playbackRateMap.value[videoId] || 1;

  applyPendingResolutionRestore(videoId);
}

function clampVolume(volume) {
  if (!Number.isFinite(volume)) return 1;
  if (volume < 0) return 0;
  if (volume > 1) return 1;
  return volume;
}

function clearPendingResolutionRestore(videoId) {
  delete pendingResolutionRestoreMap.value[videoId];
  delete resolutionSwitchingMap.value[videoId];
}

function applyPendingResolutionRestore(videoId) {
  const player = playerRefMap.value[videoId];
  const pendingRestore = pendingResolutionRestoreMap.value[videoId];
  if (!player || !pendingRestore) return;
  if (!Number.isFinite(player.duration) || player.duration <= 0) return;

  if (!pendingRestore.metaApplied) {
    const maxSeek = Math.max(0, player.duration - 0.1);
    const resumeTime = Math.min(Math.max(pendingRestore.time || 0, 0), maxSeek);
    if (typeof player.fastSeek === 'function') {
      try {
        player.fastSeek(resumeTime);
      } catch (e) {
        player.currentTime = resumeTime;
      }
    } else {
      player.currentTime = resumeTime;
    }
    player.volume = clampVolume(pendingRestore.volume);
    player.muted = Boolean(pendingRestore.muted);
    player.playbackRate = pendingRestore.rate || 1;
    pendingRestore.metaApplied = true;
  }

  if (!pendingRestore.shouldPlay) {
    clearPendingResolutionRestore(videoId);
    syncPlayerState(videoId);
    return;
  }

  if (pendingRestore.playTried) {
    return;
  }
  pendingRestore.playTried = true;
  player
    .play()
    .catch(() => {})
    .finally(() => {
      syncPlayerState(videoId);
      clearPendingResolutionRestore(videoId);
    });
}

function setVideoRef(videoId, el) {
  if (!el) {
    delete playerRefMap.value[videoId];
    delete boundSourceUrlMap.value[videoId];
    clearPendingResolutionRestore(videoId);
    destroyHls(videoId);
    return;
  }
  playerRefMap.value[videoId] = el;
  ensurePlayerState(videoId);
  if (el.dataset.bound === '1') {
    return;
  }
  el.dataset.bound = '1';
  el.addEventListener('timeupdate', () => syncPlayerState(videoId));
  el.addEventListener('loadedmetadata', () => syncPlayerState(videoId));
  el.addEventListener('volumechange', () => syncPlayerState(videoId));
  ensureVideoSourceBinding(videoId);
}

function playerCurrentTime(videoId) {
  return ensurePlayerState(videoId).currentTime || 0;
}

function playerDuration(videoId) {
  return ensurePlayerState(videoId).duration || 0;
}

function playerVolume(videoId) {
  return ensurePlayerState(videoId).volume ?? 1;
}

function playerMuted(videoId) {
  return ensurePlayerState(videoId).muted;
}

function isPlaying(videoId) {
  return ensurePlayerState(videoId).playing;
}

function formatPlaybackLabel(videoId) {
  return `${formatPlaybackSeconds(playerCurrentTime(videoId))} / ${formatPlaybackSeconds(playerDuration(videoId))}`;
}

function togglePlay(videoId) {
  const player = playerRefMap.value[videoId];
  if (!player) return;
  if (player.paused) {
    player.play().catch(() => {});
    return;
  }
  player.pause();
}

function toggleFullscreen(videoId) {
  const container = document.getElementById(`player-container-${videoId}`);
  if (!container) return;
  if (!document.fullscreenElement) {
    if (container.requestFullscreen) {
      container.requestFullscreen();
    } else if (container.webkitRequestFullscreen) {
      container.webkitRequestFullscreen();
    } else if (container.msRequestFullscreen) {
      container.msRequestFullscreen();
    }
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen();
    }
  }
}

function setPlaying(videoId, value) {
  ensurePlayerState(videoId).playing = value;
}

function seekVideo(videoId, event) {
  const player = playerRefMap.value[videoId];
  if (!player) return;
  player.currentTime = Number(event.target.value || 0);
  syncPlayerState(videoId);
}

function setVolume(videoId, event) {
  const player = playerRefMap.value[videoId];
  if (!player) return;
  const volume = Number(event.target.value);
  player.volume = Number.isFinite(volume) ? volume : 1;
  player.muted = player.volume <= 0;
  syncPlayerState(videoId);
}

function toggleMute(videoId) {
  const player = playerRefMap.value[videoId];
  if (!player) return;
  player.muted = !player.muted;
  syncPlayerState(videoId);
}

function changePlaybackRate(videoId) {
  const player = playerRefMap.value[videoId];
  if (!player) return;
  player.playbackRate = playbackRateMap.value[videoId] || 1;
}

function changeResolution(item, event) {
  const videoId = item.videoId;
  const nextIndex = Number(event?.target?.value ?? 0);
  const currentIndex = selectedResolutionValue(item);
  if (nextIndex === currentIndex) {
    return;
  }

  if (isUsingHlsLevels(videoId) && hlsMap.value[videoId]) {
    const hls = hlsMap.value[videoId];
    resolutionSwitchingMap.value[videoId] = true;
    selectedHlsLevelMap.value[videoId] = nextIndex;
    hls.currentLevel = nextIndex;
    setTimeout(() => {
      resolutionSwitchingMap.value[videoId] = false;
    }, 800);
    return;
  }

  const player = playerRefMap.value[videoId];
  const state = ensurePlayerState(videoId);
  const keepTime = player ? player.currentTime : state.currentTime;
  const shouldPlay = player ? !player.paused : state.playing;
  const keepVolume = player ? player.volume : state.volume;
  const keepMuted = player ? player.muted : state.muted;
  const keepRate = playbackRateMap.value[videoId] || player?.playbackRate || 1;

  pendingResolutionRestoreMap.value[videoId] = {
    time: keepTime,
    shouldPlay,
    volume: keepVolume,
    muted: keepMuted,
    rate: keepRate,
    metaApplied: false,
    playTried: false
  };

  resolutionSwitchingMap.value[videoId] = true;
  sourceIndexMap.value[videoId] = nextIndex;

  nextTick(() => {
    if (playerRefMap.value[videoId]) {
      ensureVideoSourceBinding(videoId);
    } else {
      clearPendingResolutionRestore(videoId);
    }
  });
}

function toggleExpand(videoId) {
  const isClosing = expandedVideoId.value === videoId;
  expandedVideoId.value = isClosing ? null : videoId;
  if (expandedVideoId.value === videoId) {
    nextTick(() => {
      ensureVideoSourceBinding(videoId);
    });
  }
}

function openCoverPreview(url) {
  previewCoverUrl.value = url || fallbackCover;
}

function openRejectDialog(videoId) {
  rejectDialog.visible = true;
  rejectDialog.videoId = videoId;
  rejectDialog.reason = '';
  rejectDialog.detail = '';
}

function closeRejectDialog() {
  rejectDialog.visible = false;
  rejectDialog.videoId = null;
  rejectDialog.reason = '';
  rejectDialog.detail = '';
}

function buildRejectNote() {
  const reason = rejectDialog.reason.trim();
  const detail = rejectDialog.detail.trim();
  return detail ? `${reason}：${detail}` : reason;
}

async function probeDuration(videoId, url) {
  if (!url || durationMap.value[videoId] !== undefined) return;
  await new Promise((resolve) => {
    const video = document.createElement('video');
    video.preload = 'metadata';
    video.src = url;
    video.onloadedmetadata = () => {
      durationMap.value[videoId] = Number.isFinite(video.duration) ? video.duration : null;
      resolve();
    };
    video.onerror = () => {
      durationMap.value[videoId] = null;
      resolve();
    };
  });
}

async function preloadDuration(recordsList) {
  const tasks = recordsList.map((item) => {
    const firstUrl = item?.videoSourceList?.[0]?.playUrl || '';
    return probeDuration(item.videoId, firstUrl);
  });
  await Promise.all(tasks);
}

async function fetchList() {
  try {
    loading.value = true;
    Object.keys(hlsMap.value).forEach((key) => destroyHls(Number(key)));
    const data = await queryAuditVideoList({
      status: filters.status,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    });
    records.value = data?.records || [];
    pagination.total = data?.total || 0;
    pagination.pageNum = data?.pageNum || pagination.pageNum;
    pagination.pageSize = data?.pageSize || pagination.pageSize;

    expandedVideoId.value = null;
    sourceIndexMap.value = {};
    pendingResolutionRestoreMap.value = {};
    resolutionSwitchingMap.value = {};
    boundSourceUrlMap.value = {};
    records.value.forEach((item) => {
      sourceIndexMap.value[item.videoId] = findDefaultSourceIndex(item);
      ensurePlayerState(item.videoId);
    });
    await preloadDuration(records.value);
  } catch (error) {
    alert(error.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

function reloadFirstPage() {
  pagination.pageNum = 1;
  fetchList();
}

function changePage(page) {
  pagination.pageNum = page;
  fetchList();
}

async function submitAudit(videoId, operation, reviewNote = '') {
  try {
    operatingVideoId.value = videoId;
    await auditVideo({
      videoId,
      operation,
      reviewNote
    });
    alert(operation === 1 ? '审核通过成功' : '驳回成功');
    await fetchList();
  } catch (error) {
    alert(error.message || '操作失败');
  } finally {
    operatingVideoId.value = null;
  }
}

async function confirmReject() {
  if (!rejectDialog.reason.trim()) {
    alert('请选择驳回原因');
    return;
  }
  if (!rejectDialog.detail.trim()) {
    alert('请填写详细描述');
    return;
  }
  const reviewNote = buildRejectNote();
  await submitAudit(rejectDialog.videoId, 0, reviewNote);
  closeRejectDialog();
}

watch(
  () => filters.status,
  () => {
    reloadFirstPage();
  }
);

onBeforeUnmount(() => {
  Object.keys(hlsMap.value).forEach((key) => destroyHls(Number(key)));
});

onMounted(fetchList);
</script>

