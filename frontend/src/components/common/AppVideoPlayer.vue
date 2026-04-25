<template>
  <div
    ref="containerRef"
    class="video-player-shell group relative h-full w-full overflow-hidden bg-black text-white"
    @mousemove="wakeControls"
    @mouseleave="hideControlsImmediately"
  >
    <video
      ref="playerRef"
      class="relative z-[1] h-full w-full bg-black"
      :class="objectFitClass"
      playsinline
      preload="metadata"
      :poster="poster || ''"
      @click="togglePlay"
      @loadedmetadata="handleLoadedMetadata"
      @canplay="handleCanPlay"
      @timeupdate="handleTimeUpdate"
      @durationchange="syncState"
      @volumechange="handleVolumeChange"
      @play="handlePlay"
      @pause="handlePause"
      @ended="handleEnded"
    >
      您的浏览器暂不支持视频播放
    </video>

    <div
      v-if="showCenterPlayButton"
      class="absolute bottom-14 right-4 z-20 transition-opacity duration-200 sm:bottom-16 sm:right-5"
      :class="playerState.playing && !loading ? 'pointer-events-none opacity-0' : 'opacity-100'"
    >
      <button
        type="button"
        class="video-player-center-btn flex h-12 w-12 items-center justify-center text-white transition hover:scale-105"
        :class="compact ? 'h-10 w-10' : ''"
        @click.stop="togglePlay"
      >
        <svg v-if="playerState.playing" viewBox="0 0 24 24" class="h-8 w-8 fill-current">
          <rect x="6" y="5" width="4" height="14" rx="1"></rect>
          <rect x="14" y="5" width="4" height="14" rx="1"></rect>
        </svg>
        <svg v-else viewBox="0 0 24 24" class="ml-1 h-9 w-9 fill-current">
          <path d="M8 5.14v13.72c0 .75.82 1.23 1.5.86l10.27-5.86a1 1 0 0 0 0-1.72L9.5 4.28A1 1 0 0 0 8 5.14Z"></path>
        </svg>
      </button>
    </div>

    <div
      v-if="loading"
      class="absolute inset-0 z-30 flex items-center justify-center bg-black/25"
    >
      <div class="video-player-spinner"></div>
    </div>

    <div
      v-else-if="!currentSourceUrl"
      class="absolute inset-0 z-30 flex flex-col items-center justify-center gap-3 bg-black/55 text-sm text-white/80"
    >
      <svg viewBox="0 0 24 24" class="h-12 w-12 fill-current text-white/65">
        <path d="M4 6.75A2.75 2.75 0 0 1 6.75 4h10.5A2.75 2.75 0 0 1 20 6.75v10.5A2.75 2.75 0 0 1 17.25 20H6.75A2.75 2.75 0 0 1 4 17.25V6.75Zm4.6 1.74A1 1 0 0 0 7 9.36v5.28a1 1 0 0 0 1.6.8l4.26-3.14a1 1 0 0 0 0-1.6L8.6 8.49Z"></path>
      </svg>
      <p>{{ emptyText }}</p>
    </div>

    <div
      class="absolute inset-x-0 bottom-0 z-20 transition duration-200"
      :class="controlsVisible || !playerState.playing ? 'opacity-100' : 'opacity-0 md:pointer-events-none'"
    >
      <div class="relative px-3 pb-3 sm:px-5 sm:pb-4">
        <div class="mb-2 flex items-center">
          <div class="video-player-slider flex-1" :style="progressSliderStyle">
            <div class="video-player-slider__track">
              <div class="video-player-slider__fill"></div>
            </div>
            <input
              class="video-player-range video-player-slider__input"
              type="range"
              min="0"
              :max="Math.max(0.1, playerState.duration || 0.1)"
              step="0.1"
              :value="playerState.currentTime"
              @input="seekToValue($event)"
              @change="commitSeekValue($event)"
            />
          </div>
        </div>

        <div class="flex flex-wrap items-center justify-between gap-3">
          <div class="flex min-w-0 items-center gap-2 sm:gap-3">
            <button type="button" class="video-player-icon-btn" @click.stop="togglePlay">
              <svg v-if="playerState.playing" viewBox="0 0 24 24" class="h-5 w-5 fill-current">
                <rect x="6" y="5" width="4" height="14" rx="1"></rect>
                <rect x="14" y="5" width="4" height="14" rx="1"></rect>
              </svg>
              <svg v-else viewBox="0 0 24 24" class="ml-0.5 h-5 w-5 fill-current">
                <path d="M8 5.14v13.72c0 .75.82 1.23 1.5.86l10.27-5.86a1 1 0 0 0 0-1.72L9.5 4.28A1 1 0 0 0 8 5.14Z"></path>
              </svg>
            </button>

            <div class="min-w-[94px] text-sm font-semibold tabular-nums text-white/88">
              {{ playbackLabel }}
            </div>
          </div>

          <div class="flex flex-wrap items-center justify-end gap-2 sm:gap-3">
            <button type="button" class="video-player-icon-btn h-8 w-8" @click.stop="toggleMute">
              <svg v-if="playerState.muted || playerState.volume <= 0.01" viewBox="0 0 24 24" class="h-5 w-5 stroke-current" fill="none" stroke-width="1.8">
                <path d="M5 9.5h3.2L13 5v14l-4.8-4.5H5v-5Z" stroke-linejoin="round"></path>
                <path d="m18 9-4 6" stroke-linecap="round"></path>
                <path d="m14 9 4 6" stroke-linecap="round"></path>
              </svg>
              <svg v-else viewBox="0 0 24 24" class="h-5 w-5 stroke-current" fill="none" stroke-width="1.8">
                <path d="M5 9.5h3.2L13 5v14l-4.8-4.5H5v-5Z" stroke-linejoin="round"></path>
                <path d="M17 9.4a4.5 4.5 0 0 1 0 5.2" stroke-linecap="round"></path>
                <path d="M19.6 7.2a7.5 7.5 0 0 1 0 9.6" stroke-linecap="round"></path>
              </svg>
            </button>

            <div class="video-player-slider hidden w-20 sm:flex" :style="volumeSliderStyle">
              <div class="video-player-slider__track">
                <div class="video-player-slider__fill"></div>
              </div>
              <input
                class="video-player-volume-range video-player-slider__input"
                type="range"
                min="0"
                max="1"
                step="0.05"
                :value="playerState.muted ? 0 : playerState.volume"
                @input="changeVolume($event)"
              />
            </div>

            <label class="video-player-select-wrap">
              <span class="sr-only">播放速度</span>
              <select v-model.number="playbackRate" class="video-player-select" @change="applyPlaybackRate">
                <option v-for="rate in normalizedPlaybackRates" :key="`rate-${rate}`" :value="rate">
                  {{ rate }}x
                </option>
              </select>
            </label>

            <label v-if="showResolutionSelector" class="video-player-select-wrap">
              <span class="sr-only">清晰度</span>
              <select
                class="video-player-select"
                :disabled="resolutionSwitching"
                :value="selectedResolutionValue"
                @change="changeResolution($event)"
              >
                <option v-for="option in availableResolutionOptions" :key="`resolution-${option.value}`" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>

            <button type="button" class="video-player-icon-btn" @click.stop="toggleFullscreen">
              <svg viewBox="0 0 24 24" class="h-5 w-5 stroke-current" fill="none" stroke-width="1.8">
                <path d="M8 4H4v4" stroke-linecap="round" stroke-linejoin="round"></path>
                <path d="M16 4h4v4" stroke-linecap="round" stroke-linejoin="round"></path>
                <path d="M8 20H4v-4" stroke-linecap="round" stroke-linejoin="round"></path>
                <path d="M16 20h4v-4" stroke-linecap="round" stroke-linejoin="round"></path>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import Hls from 'hls.js';
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

const props = defineProps({
  sources: {
    type: Array,
    default: () => []
  },
  poster: {
    type: String,
    default: ''
  },
  loading: {
    type: Boolean,
    default: false
  },
  initialTime: {
    type: Number,
    default: 0
  },
  defaultSourceIndex: {
    type: Number,
    default: -1
  },
  emptyText: {
    type: String,
    default: '当前视频暂无可播放源'
  },
  playbackRates: {
    type: Array,
    default: () => [0.75, 1, 1.25, 1.5, 2]
  },
  compact: {
    type: Boolean,
    default: false
  },
  mediaKey: {
    type: [String, Number],
    default: ''
  },
  objectFit: {
    type: String,
    default: 'contain'
  },
  showCenterPlayButton: {
    type: Boolean,
    default: true
  },
  autoplay: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  'timeupdate',
  'ended',
  'play',
  'pause',
  'seek-commit',
  'source-change',
  'error',
  'loadedmetadata'
]);

const containerRef = ref(null);
const playerRef = ref(null);
const hlsInstance = ref(null);
const activeSourceIndex = ref(0);
const playbackRate = ref(1);
const hlsLevelOptions = ref([]);
const selectedHlsLevel = ref(-1);
const resolutionSwitching = ref(false);
const controlsVisible = ref(true);
const hideControlsTimer = ref(null);
const sourceVersion = ref(0);
const pendingRestore = ref(null);
const initialResumeApplied = ref(false);
const boundSourceUrl = ref('');
const pendingAutoplay = ref(false);

const playerState = ref({
  currentTime: 0,
  duration: 0,
  volume: 1,
  muted: false,
  playing: false
});

const normalizedSources = computed(() => {
  if (!Array.isArray(props.sources)) return [];
  return props.sources.filter((item) => item && item.playUrl);
});

const normalizedPlaybackRates = computed(() => {
  if (!Array.isArray(props.playbackRates) || props.playbackRates.length === 0) {
    return [0.75, 1, 1.25, 1.5, 2];
  }
  return props.playbackRates;
});

const currentSource = computed(() => normalizedSources.value[activeSourceIndex.value] || null);
const currentSourceUrl = computed(() => currentSource.value?.playUrl || '');
const isCurrentSourceM3u8 = computed(() => /\.m3u8($|[?#])/i.test(currentSourceUrl.value));

const availableResolutionOptions = computed(() => {
  if (hlsLevelOptions.value.length > 0) {
    return hlsLevelOptions.value;
  }
  return normalizedSources.value.map((source, index) => ({
    value: index,
    label: source.resolution || `线路${index + 1}`
  }));
});

const showResolutionSelector = computed(() => availableResolutionOptions.value.length > 1);
const selectedResolutionValue = computed(() => {
  if (hlsLevelOptions.value.length > 0) {
    return selectedHlsLevel.value;
  }
  return activeSourceIndex.value;
});

const progressPercent = computed(() => {
  const duration = Number(playerState.value.duration || 0);
  if (!duration) return 0;
  return Math.min(100, Math.max(0, (playerState.value.currentTime / duration) * 100));
});

const volumePercent = computed(() => {
  const volume = playerState.value.muted ? 0 : Number(playerState.value.volume ?? 1);
  return Math.min(100, Math.max(0, volume * 100));
});

const progressSliderStyle = computed(() => createSliderStyle(progressPercent.value));
const volumeSliderStyle = computed(() => createSliderStyle(volumePercent.value));

const playbackLabel = computed(() => `${formatDuration(playerState.value.currentTime)} / ${formatDuration(playerState.value.duration)}`);
const objectFitClass = computed(() => (props.objectFit === 'contain' ? 'object-contain' : 'object-cover'));

function findDefaultSourceIndex() {
  if (!normalizedSources.value.length) return 0;
  if (props.defaultSourceIndex >= 0 && props.defaultSourceIndex < normalizedSources.value.length) {
    return props.defaultSourceIndex;
  }
  const smartIndex = normalizedSources.value.findIndex((source) => {
    const resolution = String(source?.resolution || '').toLowerCase();
    const url = String(source?.playUrl || '').toLowerCase();
    return resolution.includes('自适应') || resolution.includes('master') || url.includes('/master.m3u8');
  });
  return smartIndex >= 0 ? smartIndex : 0;
}

function wakeControls() {
  controlsVisible.value = true;
  clearControlsTimer();
}

function hideControlsImmediately() {
  clearControlsTimer();
  controlsVisible.value = false;
}

function clearControlsTimer() {
  if (hideControlsTimer.value) {
    window.clearTimeout(hideControlsTimer.value);
    hideControlsTimer.value = null;
  }
}

function clampVolume(value) {
  if (!Number.isFinite(value)) return 1;
  if (value < 0) return 0;
  if (value > 1) return 1;
  return value;
}

function createSliderStyle(percent) {
  const normalizedPercent = Math.min(100, Math.max(0, Number(percent) || 0));
  return {
    '--player-slider-percent': normalizedPercent
  };
}

function formatDuration(seconds) {
  if (!Number.isFinite(seconds) || seconds < 0) return '00:00';
  const total = Math.floor(seconds);
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const secs = total % 60;
  if (hours > 0) {
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }
  return `${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

function destroyHls() {
  if (hlsInstance.value) {
    hlsInstance.value.destroy();
    hlsInstance.value = null;
  }
  hlsLevelOptions.value = [];
  selectedHlsLevel.value = -1;
}

function teardownMedia() {
  destroyHls();
  const player = playerRef.value;
  if (!player) return;
  player.pause();
  player.removeAttribute('src');
  player.load();
  boundSourceUrl.value = '';
}

function syncState() {
  const player = playerRef.value;
  if (!player) return;
  playerState.value = {
    currentTime: player.currentTime || 0,
    duration: Number.isFinite(player.duration) ? player.duration : 0,
    volume: clampVolume(player.volume),
    muted: Boolean(player.muted),
    playing: !player.paused
  };
}

function captureRestoreState() {
  const player = playerRef.value;
  return {
    time: player ? player.currentTime || 0 : playerState.value.currentTime || 0,
    shouldPlay: player ? !player.paused : playerState.value.playing,
    volume: player ? player.volume : playerState.value.volume,
    muted: player ? player.muted : playerState.value.muted,
    rate: player ? player.playbackRate || playbackRate.value : playbackRate.value,
    metaApplied: false,
    playTried: false
  };
}

function applyPendingRestore() {
  const player = playerRef.value;
  const restore = pendingRestore.value;
  if (!player || !restore) return;
  if (!Number.isFinite(player.duration) || player.duration <= 0) return;

  if (!restore.metaApplied) {
    const maxSeek = Math.max(0, player.duration - 0.1);
    const resumeTime = Math.min(Math.max(restore.time || 0, 0), maxSeek);
    if (typeof player.fastSeek === 'function') {
      try {
        player.fastSeek(resumeTime);
      } catch (error) {
        player.currentTime = resumeTime;
      }
    } else {
      player.currentTime = resumeTime;
    }
    player.volume = clampVolume(restore.volume);
    player.muted = Boolean(restore.muted);
    player.playbackRate = restore.rate || 1;
    playbackRate.value = restore.rate || 1;
    restore.metaApplied = true;
  }

  if (!restore.shouldPlay) {
    pendingRestore.value = null;
    syncState();
    return;
  }

  if (restore.playTried) return;
  restore.playTried = true;
  player.play().catch(() => {}).finally(() => {
    pendingRestore.value = null;
    syncState();
  });
}

function tryAutoplay() {
  const player = playerRef.value;
  if (!player || !pendingAutoplay.value || pendingRestore.value) return;
  pendingAutoplay.value = false;
  player.play().catch(() => {});
}

function applyInitialResume() {
  const player = playerRef.value;
  const resumeTime = Number(props.initialTime || 0);
  if (!player || initialResumeApplied.value || !resumeTime) return;
  if (!Number.isFinite(player.duration) || player.duration <= 0) return;
  if (resumeTime >= player.duration) {
    initialResumeApplied.value = true;
    return;
  }
  player.currentTime = resumeTime;
  initialResumeApplied.value = true;
  syncState();
}

function buildHlsLevelOptions(levels) {
  const options = [{ value: -1, label: '自动' }];
  (levels || []).forEach((level, index) => {
    const height = Number(level?.height || 0);
    options.push({
      value: index,
      label: height > 0 ? `${height}P` : `清晰度 ${index + 1}`
    });
  });
  return options;
}

function bindCurrentSource() {
  const player = playerRef.value;
  const url = currentSourceUrl.value;
  if (!player || !url) {
    teardownMedia();
    return;
  }

  if (boundSourceUrl.value === url && (isCurrentSourceM3u8.value ? Boolean(hlsInstance.value) : player.currentSrc === url || player.src === url)) {
    syncState();
    return;
  }

  destroyHls();
  boundSourceUrl.value = url;
  player.pause();
  player.currentTime = 0;

  if (isCurrentSourceM3u8.value && Hls.isSupported()) {
    const hls = new Hls({
      enableWorker: true,
      lowLatencyMode: true,
      backBufferLength: 90
    });
    hlsInstance.value = hls;
    hls.attachMedia(player);
    hls.on(Hls.Events.MEDIA_ATTACHED, () => {
      hls.loadSource(url);
    });
    hls.on(Hls.Events.MANIFEST_PARSED, (_, data) => {
      hlsLevelOptions.value = buildHlsLevelOptions(data?.levels || []);
      hls.currentLevel = selectedHlsLevel.value ?? -1;
      resolutionSwitching.value = false;
    });
    hls.on(Hls.Events.LEVEL_SWITCHED, (_, data) => {
      selectedHlsLevel.value = Number.isFinite(data?.level) ? data.level : -1;
      resolutionSwitching.value = false;
    });
    hls.on(Hls.Events.ERROR, (_, data) => {
      if (data?.fatal) {
        resolutionSwitching.value = false;
        emit('error', data);
      }
    });
    return;
  }

  player.src = url;
  player.load();
  resolutionSwitching.value = false;
}

function resetPlayerForMedia() {
  clearControlsTimer();
  controlsVisible.value = true;
  resolutionSwitching.value = false;
  sourceVersion.value += 1;
  activeSourceIndex.value = findDefaultSourceIndex();
  playbackRate.value = normalizedPlaybackRates.value.includes(1) ? 1 : normalizedPlaybackRates.value[0];
  pendingRestore.value = null;
  initialResumeApplied.value = false;
  pendingAutoplay.value = Boolean(props.autoplay);
  teardownMedia();
  nextTick(() => {
    bindCurrentSource();
  });
}

function switchSourceIndex(nextIndex) {
  if (!Number.isFinite(nextIndex) || nextIndex < 0 || nextIndex >= normalizedSources.value.length || nextIndex === activeSourceIndex.value) {
    return;
  }
  pendingRestore.value = captureRestoreState();
  activeSourceIndex.value = nextIndex;
  resolutionSwitching.value = true;
  emit('source-change', {
    sourceIndex: nextIndex,
    source: normalizedSources.value[nextIndex] || null
  });
}

function changeResolution(event) {
  const nextValue = Number(event?.target?.value ?? 0);
  if (hlsInstance.value && hlsLevelOptions.value.length > 0) {
    if (nextValue === selectedHlsLevel.value) return;
    resolutionSwitching.value = true;
    selectedHlsLevel.value = nextValue;
    hlsInstance.value.currentLevel = nextValue;
    window.setTimeout(() => {
      resolutionSwitching.value = false;
    }, 800);
    return;
  }
  switchSourceIndex(nextValue);
}

function togglePlay() {
  const player = playerRef.value;
  if (!player || !currentSourceUrl.value) return;
  wakeControls();
  if (player.paused) {
    player.play().catch(() => {});
    return;
  }
  player.pause();
}

function toggleMute() {
  const player = playerRef.value;
  if (!player) return;
  player.muted = !player.muted;
  syncState();
}

function changeVolume(event) {
  const player = playerRef.value;
  if (!player) return;
  const nextVolume = clampVolume(Number(event?.target?.value));
  player.volume = nextVolume;
  player.muted = nextVolume <= 0;
  syncState();
}

function applyPlaybackRate() {
  const player = playerRef.value;
  if (!player) return;
  player.playbackRate = playbackRate.value || 1;
  syncState();
}

function seekTo(seconds) {
  const player = playerRef.value;
  if (!player || !Number.isFinite(seconds)) return;
  player.currentTime = Math.min(Math.max(seconds, 0), player.duration || seconds || 0);
  syncState();
}

function seekToValue(event) {
  seekTo(Number(event?.target?.value || 0));
}

function commitSeekValue(event) {
  const seconds = Number(event?.target?.value || 0);
  seekTo(seconds);
  emit('seek-commit', {
    currentTime: Number(playerRef.value?.currentTime || seconds || 0),
    duration: playerState.value.duration,
    sourceIndex: activeSourceIndex.value
  });
}

function handleLoadedMetadata() {
  syncState();
  applyInitialResume();
  applyPendingRestore();
  tryAutoplay();
  emit('loadedmetadata', {
    duration: playerState.value.duration,
    sourceIndex: activeSourceIndex.value
  });
}

function handleCanPlay() {
  applyInitialResume();
  applyPendingRestore();
  tryAutoplay();
}

function handleTimeUpdate() {
  syncState();
  emit('timeupdate', {
    currentTime: playerState.value.currentTime,
    duration: playerState.value.duration,
    sourceIndex: activeSourceIndex.value
  });
}

function handleVolumeChange() {
  syncState();
}

function handlePlay() {
  syncState();
  wakeControls();
  emit('play', {
    currentTime: playerState.value.currentTime
  });
}

function handlePause() {
  syncState();
  controlsVisible.value = true;
  clearControlsTimer();
  emit('pause', {
    currentTime: playerState.value.currentTime
  });
}

function handleEnded() {
  syncState();
  controlsVisible.value = true;
  clearControlsTimer();
  emit('ended', {
    currentTime: playerState.value.currentTime,
    duration: playerState.value.duration
  });
}

async function toggleFullscreen() {
  const container = containerRef.value;
  if (!container) return;
  try {
    if (document.fullscreenElement) {
      await document.exitFullscreen();
      return;
    }
    if (container.requestFullscreen) {
      await container.requestFullscreen();
    }
  } catch (error) {
    emit('error', error);
  }
}

function play() {
  return playerRef.value?.play?.();
}

function pause() {
  playerRef.value?.pause?.();
}

function getCurrentTime() {
  return Number(playerRef.value?.currentTime || playerState.value.currentTime || 0);
}

function getDuration() {
  return Number(playerRef.value?.duration || playerState.value.duration || 0);
}

watch(
  () => [props.mediaKey, props.sources],
  () => {
    resetPlayerForMedia();
  },
  {
    immediate: true,
    deep: true
  }
);

watch(currentSourceUrl, () => {
  bindCurrentSource();
});

onMounted(() => {
  wakeControls();
});

onBeforeUnmount(() => {
  clearControlsTimer();
  teardownMedia();
});

defineExpose({
  play,
  pause,
  seekTo,
  getCurrentTime,
  getDuration
});
</script>

<style scoped>
.video-player-shell {
  --player-slider-hit-area: 0.875rem;
  --player-slider-track-height: 0.125rem;
  --player-slider-thumb-size: 0.55rem;
  --player-slider-percent: 0%;
  --player-slider-offset: calc(0.5 * var(--player-slider-thumb-size));
  box-shadow: 0 18px 40px rgba(3, 9, 18, 0.28);
}

.video-player-center-btn {
  filter: drop-shadow(0 10px 18px rgba(0, 0, 0, 0.45));
}

.video-player-spinner {
  width: 40px;
  height: 40px;
  border-radius: 9999px;
  border: 3px solid rgba(255, 255, 255, 0.22);
  border-top-color: #2ac4ff;
  animation: player-spin 0.9s linear infinite;
}

.video-player-icon-btn {
  display: inline-flex;
  height: 2.25rem;
  width: 2.25rem;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  color: rgba(255, 255, 255, 0.92);
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease;
}

.video-player-icon-btn:hover {
  background: transparent;
  transform: translateY(-1px);
}

.video-player-select-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.video-player-select-wrap::after {
  content: "";
  position: absolute;
  right: 0.75rem;
  top: 50%;
  width: 0.45rem;
  height: 0.45rem;
  border-right: 1.5px solid rgba(255, 255, 255, 0.72);
  border-bottom: 1.5px solid rgba(255, 255, 255, 0.72);
  transform: translateY(-65%) rotate(45deg);
  pointer-events: none;
}

.video-player-select {
  min-height: 2.25rem;
  border: none;
  background: transparent;
  padding: 0.45rem 2rem 0.45rem 0.25rem;
  color: rgba(255, 255, 255, 0.92);
  font-size: 0.8125rem;
  line-height: 1;
  appearance: none;
  outline: none;
}

.video-player-select:hover,
.video-player-select:focus {
  color: #2ac4ff;
}

.video-player-select:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.video-player-slider {
  position: relative;
  display: flex;
  align-items: center;
  height: var(--player-slider-hit-area);
}

.video-player-slider__track {
  position: relative;
  width: 100%;
  height: var(--player-slider-track-height);
  background: rgba(255, 255, 255, 0.35);
}

.video-player-slider__fill {
  height: 100%;
  background: #2ac4ff;
  width: calc(
      var(--player-slider-percent) * (100% - var(--player-slider-thumb-size)) / 100
      + var(--player-slider-thumb-size) / 2
  );
}

.video-player-slider__input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  cursor: pointer;
  appearance: none;
  background: transparent;
}

.video-player-range::-webkit-slider-thumb,
.video-player-volume-range::-webkit-slider-thumb {
  width: var(--player-slider-thumb-size);
  height: var(--player-slider-thumb-size);
  border-radius: 9999px;
  background: #ffffff;
  border: none;
  box-shadow: none;
  appearance: none;
  /* 新增：将滑块向上偏移，距离为 (轨道高度 - 滑块高度) 的一半，使其完美垂直居中 */
  margin-top: calc((var(--player-slider-track-height) - var(--player-slider-thumb-size)) / 2);
}

.video-player-range::-moz-range-thumb,
.video-player-volume-range::-moz-range-thumb {
  width: var(--player-slider-thumb-size);
  height: var(--player-slider-thumb-size);
  border: none;
  border-radius: 9999px;
  background: #ffffff;
  box-shadow: none;
}

.video-player-range::-webkit-slider-runnable-track,
.video-player-volume-range::-webkit-slider-runnable-track {
  height: var(--player-slider-track-height);
  background: transparent;
}

.video-player-range::-moz-range-track,
.video-player-volume-range::-moz-range-track {
  height: var(--player-slider-track-height);
  background: transparent;
}

@keyframes player-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
