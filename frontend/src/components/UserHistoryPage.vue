<template>
  <div class="min-h-screen bg-[#f4f5f7]">
    <HeaderNav />

    <main class="max-w-7xl mx-auto px-4 py-6 pb-12">
      <section class="rounded-3xl bg-white shadow-sm border border-[#e7e9ee] overflow-hidden">
        <div class="px-6 py-5 border-b border-[#f1f2f3]">
          <h1 class="text-2xl font-bold text-[#18191c]">历史记录</h1>
        </div>

        <div v-if="loading && historyItems.length === 0" class="px-6 py-5">
          <div class="grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-4">
            <div
              v-for="i in 12"
              :key="i"
              class="rounded-2xl border border-[#f1f2f3] p-3"
            >
              <div class="aspect-video rounded-2xl bg-[#eef1f3]"></div>
              <div class="mt-3 h-5 w-full rounded bg-[#eef1f3]"></div>
              <div class="mt-2 h-5 w-4/5 rounded bg-[#eef1f3]"></div>
              <div class="mt-3 h-2 w-full rounded-full bg-[#eef1f3]"></div>
              <div class="mt-3 flex items-center justify-between gap-2">
                <div class="h-4 w-1/3 rounded bg-[#eef1f3]"></div>
                <div class="h-4 w-1/4 rounded bg-[#eef1f3]"></div>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="historyItems.length === 0" class="flex flex-col items-center justify-center px-6 py-20 text-center">
          <div class="mb-5 flex h-24 w-24 items-center justify-center rounded-full bg-[#f6f7f8] text-[#c9ccd0]">
            <svg class="h-12 w-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <h3 class="text-xl font-semibold text-[#18191c]">还没有历史记录</h3>
          <p class="mt-2 text-sm text-[#9499a0]">去首页找点感兴趣的视频看看吧。</p>
          <button
            class="mt-6 rounded-full bg-[#00a1d6] px-5 py-2.5 text-sm font-medium text-white transition hover:bg-[#0093c4]"
            @click="goHome"
          >
            去首页逛逛
          </button>
        </div>

        <div v-else class="px-6 py-5">
          <div class="grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-4">
            <article
              v-for="item in historyItems"
              :key="item.videoId"
              class="group cursor-pointer overflow-hidden rounded-lg bg-transparent"
              @click="goToVideo(item.videoId)"
            >
              <div class="relative aspect-video overflow-hidden rounded-xl bg-transparent">
                <img
                  v-if="item.coverUrl"
                  :src="item.coverUrl"
                  :alt="item.title"
                  class="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                  @error="handleImageError"
                />
                <div v-else class="flex h-full w-full items-center justify-center bg-gradient-to-br from-[#dbeafe] to-[#fce7f3] text-4xl text-white/80">
                  <span>▶</span>
                </div>

                <div v-if="item.isFinished" class="absolute left-3 top-3 rounded-full bg-black/65 px-2.5 py-1 text-xs text-white">
                  已看完
                </div>
                <span class="absolute bottom-2 right-2 text-sm text-white drop-shadow-lg">
                  {{ formatDuration(item.lastPlayTime) }}/{{ formatDuration(item.duration) }}
                </span>
                <div class="absolute bottom-0 left-0 right-0 h-1 bg-black/15">
                  <div
                    class="h-full bg-[#fb7299]"
                    :style="{ width: `${formatProgress(item)}%` }"
                  ></div>
                </div>
              </div>

              <div class="mt-1">
                <h3 class="line-clamp-2 min-h-[3rem] text-left text-sm font-medium leading-6 text-[#18191c] transition-colors group-hover:text-[#00a1d6]">
                  {{ item.title || '未命名视频' }}
                </h3>

                <div class="mt-1 flex items-center justify-between gap-3 text-xs text-[#9499a0]">
                  <span class="truncate">{{ item.username || '未知用户' }}</span>
                  <span class="flex-shrink-0">{{ formatAbsoluteDateTime(item.historyTime) }}</span>
                </div>
              </div>
            </article>
          </div>

          <div v-if="hasMore" class="mt-8 text-center">
            <button
              class="rounded-full border border-[#dcdfe4] bg-white px-6 py-2.5 text-sm font-medium text-[#61666d] transition hover:border-[#00a1d6] hover:text-[#00a1d6] disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="loading"
              @click="loadMore"
            >
              {{ loading ? '加载中...' : '加载更多' }}
            </button>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getUserVideoHistory } from '../api/video';
import HeaderNav from './HeaderNav.vue';

const router = useRouter();
const historyItems = ref([]);
const loading = ref(false);
const hasMore = ref(true);
const pageNum = ref(1);
const pageSize = 12;

async function loadHistory(isLoadMore = false) {
  if (loading.value) return;

  loading.value = true;
  try {
    const records = (await getUserVideoHistory(pageNum.value, pageSize)) || [];
    if (isLoadMore) {
      historyItems.value = [...historyItems.value, ...records];
    } else {
      historyItems.value = records;
    }
    hasMore.value = records.length >= pageSize;
  } catch (error) {
    console.error('加载历史记录失败:', error);
  } finally {
    loading.value = false;
  }
}

function loadMore() {
  pageNum.value += 1;
  loadHistory(true);
}

function goToVideo(videoId) {
  router.push(`/video/${videoId}`);
}

function goHome() {
  router.push('/');
}

function formatDuration(seconds) {
  const safeSeconds = Number.isFinite(seconds) ? Math.max(0, seconds) : 0;
  const hours = Math.floor(safeSeconds / 3600);
  const mins = Math.floor((safeSeconds % 3600) / 60);
  const secs = safeSeconds % 60;

  if (hours > 0) {
    return `${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

function formatProgress(item) {
  const duration = Number(item.duration) || 0;
  const lastPlayTime = Number(item.lastPlayTime) || 0;

  if (!duration) {
    return item.isFinished === 1 ? 100 : 0;
  }

  const percent = Math.round((Math.min(lastPlayTime, duration) / duration) * 100);
  return Math.max(item.isFinished === 1 ? 100 : percent, 0);
}

function formatAbsoluteDateTime(dateStr) {
  if (!dateStr) return '--';
  return new Date(dateStr).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  });
}

function handleImageError(event) {
  event.target.style.display = 'none';
}

onMounted(() => {
  loadHistory();
});
</script>

<style scoped>
.history-card {
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.history-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 40px rgba(24, 25, 28, 0.08);
  border-color: rgba(0, 161, 214, 0.18);
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
