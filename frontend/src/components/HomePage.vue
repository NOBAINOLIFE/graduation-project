<template>
  <div class="min-h-screen bg-[#f6f7fb]">
    <HeaderNav />

    <header class="border-t border-white/60 bg-white/70 backdrop-blur-sm">
      <div class="mx-auto flex max-w-7xl items-center gap-2 overflow-x-auto px-4 py-3">
        <button

          class="rounded-full px-4 py-1.5 text-sm whitespace-nowrap transition-colors"
          :class="selectedPartition === null ? 'bg-[#00a1d6] text-white' : 'text-[#61666d] hover:bg-[#f1f2f3]'"
          @click="selectPartition(null)"
        >
          首页
        </button>
        <button
          v-for="partition in partitions"
          :key="partition.id"
          class="rounded-full px-4 py-1.5 text-sm whitespace-nowrap transition-colors"
          :class="selectedPartition === partition.id ? 'bg-[#00a1d6] text-white' : 'text-[#61666d] hover:bg-[#f1f2f3]'"
          @click="selectPartition(partition.id)"
        >
          {{ partition.partitionName }}
        </button>
      </div>
    </header>

    <main class="mx-auto max-w-7xl px-4 py-6">
      <section
        v-if="selectedPartition === null"
        class="mb-6 overflow-hidden rounded-[28px] bg-gradient-to-r from-[#00a1d6] via-[#20b8ef] to-[#5fd0ff] p-8 text-white shadow-[0_24px_80px_-40px_rgba(0,161,214,0.75)]"
      >
        <p class="text-sm uppercase tracking-[0.4em] text-white/80">Video Hub</p>
        <h2 class="mt-3 text-3xl font-bold">发现新内容，也找到你的兴趣分区</h2>
      </section>

      <section class="rounded-[28px] bg-white px-4 py-6 shadow-sm ring-1 ring-black/5 sm:px-6">
        <div class="mb-5 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 class="text-xl font-semibold text-[#18191c]">{{ sectionTitle }}</h2>
            <p class="mt-1 text-sm text-[#9499a0]">
              <template v-if="selectedPartition === null">
                为你持续推荐最新内容
              </template>
              <template v-else>
                当前分区共 {{ total }} 个搜索结果
              </template>
            </p>
          </div>
        </div>

        <div v-if="loading && videos.length === 0" class="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
          <div v-for="index in 10" :key="index" class="overflow-hidden rounded-2xl bg-[#f6f7fb]">
            <div class="aspect-video animate-pulse bg-[#e8edf2]"></div>
            <div class="space-y-3 p-4">
              <div class="h-4 w-4/5 animate-pulse rounded bg-[#e8edf2]"></div>
              <div class="h-3 w-2/5 animate-pulse rounded bg-[#e8edf2]"></div>
            </div>
          </div>
        </div>

        <div v-else-if="videos.length === 0" class="py-24 text-center">
          <div class="text-6xl text-[#c9d2db]">📺</div>
          <p class="mt-4 text-lg text-[#61666d]">这个分区还没有视频</p>
          <p class="mt-2 text-sm text-[#9499a0]">换个分区看看，或者等大家多发点新内容。</p>
        </div>

        <div v-else class="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
          <article
            v-for="video in videos"
            :key="video.videoId"
            class="group cursor-pointer overflow-hidden rounded-lg bg-transparent"
            @click="goToVideo(video.videoId)"
          >
            <div class="relative aspect-video overflow-hidden rounded-lg bg-[#eef2f6]">
              <img
                v-if="video.coverUrl"
                :src="video.coverUrl"
                :alt="video.title"
                class="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                @error="handleImageError"
              />
              <div v-else class="flex h-full items-center justify-center text-4xl text-[#c2cad3]">🎬</div>
              <span class="absolute bottom-2 right-2 text-sm text-white drop-shadow-lg">
                {{ formatDuration(video.duration) }}
              </span>
              <span class="absolute bottom-2 left-2 flex items-center gap-1 text-sm text-white drop-shadow-lg">
                <svg viewBox="0 0 24 24" class="h-3.5 w-3.5 fill-current">
                  <path d="M8 5.14v13.72c0 .75.82 1.23 1.5.86l10.27-5.86a1 1 0 0 0 0-1.72L9.5 4.28A1 1 0 0 0 8 5.14Z"></path>
                </svg>
                {{ formatCount(video.playCount) }}
              </span>
            </div>

            <div class="mt-1">
              <h3 class="line-clamp-2 min-h-[3rem] text-left text-sm font-medium leading-6 text-[#18191c] transition-colors group-hover:text-[#00a1d6]">
                {{ video.title }}
              </h3>
              <p class="mt-1 truncate text-xs text-[#9499a0]">
                {{ video.username || '未知用户' }} · {{ formatDate(video.createTime) }}
              </p>
            </div>
          </article>
        </div>

        <div v-if="loading && videos.length > 0" class="mt-10 flex justify-center">
          <span class="text-sm text-[#9499a0]">加载中...</span>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getVideoPlayList, listPartitions, searchVideos } from '../api/video';
import HeaderNav from './HeaderNav.vue';

const PARTITION_PAGE_SIZE = 20;

const router = useRouter();
const videos = ref([]);
const partitions = ref([]);
const selectedPartition = ref(null);
const loading = ref(false);
const hasMore = ref(true);
const lastVideoId = ref(0);
const partitionPageNum = ref(1);
const total = ref(0);

const sectionTitle = computed(() => {
  if (selectedPartition.value === null) {
    return '推荐视频';
  }
  const targetPartition = partitions.value.find(item => item.id === selectedPartition.value);
  return targetPartition ? `${targetPartition.partitionName} 分区` : '分区搜索结果';
});

async function loadPartitions() {
  try {
    const data = await listPartitions();
    partitions.value = data || [];
  } catch (error) {
    console.error('加载分区失败:', error);
  }
}

async function loadHomeVideos(append = false) {
  if (loading.value) return;
  loading.value = true;
  try {
    const data = await getVideoPlayList(append ? lastVideoId.value : 0);
    const records = data || [];
    videos.value = append ? [...videos.value, ...records] : records;
    total.value = videos.value.length;
    if (records.length > 0) {
      lastVideoId.value = records[records.length - 1].videoId;
    }
    hasMore.value = records.length > 0;
  } catch (error) {
    console.error('加载首页视频失败:', error);
    if (!append) {
      videos.value = [];
      total.value = 0;
    }
  } finally {
    loading.value = false;
  }
}

async function loadPartitionVideos(append = false) {
  if (loading.value || selectedPartition.value == null) return;
  loading.value = true;
  try {
    const result = await searchVideos({
      partitionId: selectedPartition.value,
      sortType: 2,
      pageNum: partitionPageNum.value,
      pageSize: PARTITION_PAGE_SIZE
    });
    const records = result?.records || [];
    videos.value = append ? [...videos.value, ...records] : records;
    total.value = result?.total || 0;
    hasMore.value = videos.value.length < total.value;
  } catch (error) {
    console.error('加载分区视频失败:', error);
    if (!append) {
      videos.value = [];
      total.value = 0;
    }
  } finally {
    loading.value = false;
  }
}

function selectPartition(partitionId) {
  if (selectedPartition.value === partitionId) return;
  selectedPartition.value = partitionId;
  videos.value = [];
  hasMore.value = true;
  total.value = 0;
  lastVideoId.value = 0;
  partitionPageNum.value = 1;

  if (partitionId === null) {
    loadHomeVideos(false);
    return;
  }
  loadPartitionVideos(false);
}

function loadMore() {
  if (selectedPartition.value === null) {
    loadHomeVideos(true);
    return;
  }
  partitionPageNum.value += 1;
  loadPartitionVideos(true);
}

function goToVideo(videoId) {
  router.push(`/video/${videoId}`);
}

function handleImageError(event) {
  event.target.style.display = 'none';
}

function formatDuration(seconds) {
  if (seconds == null || Number.isNaN(Number(seconds))) {
    return '--:--';
  }
  const safeSeconds = Math.max(Number(seconds), 0);
  const hours = Math.floor(safeSeconds / 3600);
  const minutes = Math.floor((safeSeconds % 3600) / 60);
  const secs = safeSeconds % 60;
  if (hours > 0) {
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }
  return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

function formatCount(count) {
  const numericCount = Number(count || 0);
  if (numericCount >= 100000000) {
    return `${(numericCount / 100000000).toFixed(1)}亿`;
  }
  if (numericCount >= 10000) {
    return `${(numericCount / 10000).toFixed(1)}万`;
  }
  return `${numericCount}`;
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const now = new Date();
  const diff = now - date;
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 60) return `${Math.max(minutes, 1)}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 30) return `${days}天前`;

  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${month}-${day}`;
}

function handleScroll() {
  if (!hasMore.value || loading.value) return;

  const scrollHeight = document.documentElement.scrollHeight;
  const scrollTop = window.scrollY || document.documentElement.scrollTop;
  const clientHeight = document.documentElement.clientHeight;

  if (scrollHeight - scrollTop - clientHeight < 300) {
    loadMore();
  }
}

onMounted(async () => {
  await loadPartitions();
  await loadHomeVideos(false);
  window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
