<template>
  <div class="min-h-screen bg-[#f4f4f4]">
    <!-- 顶部导航栏 -->
    <HeaderNav />

    <!-- 分区导航 -->
    <header class="border-t border-gray-100">
      <div class="max-w-7xl mx-auto px-4 py-2 flex items-center gap-2 overflow-x-auto">
        <button
          class="px-4 py-1.5 rounded-full text-sm whitespace-nowrap transition-colors"
          :class="selectedPartition === null ? 'bg-[#00a1d6] text-white' : 'text-gray-700 hover:bg-gray-100'"
          @click="selectPartition(null)"
        >
          首页
        </button>
        <button
          v-for="partition in partitions"
          :key="partition.id"
          class="px-4 py-1.5 rounded-full text-sm whitespace-nowrap transition-colors"
          :class="selectedPartition === partition.id ? 'bg-[#00a1d6] text-white' : 'text-gray-700 hover:bg-gray-100'"
          @click="selectPartition(partition.id)"
        >
          {{ partition.partitionName }}
        </button>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="max-w-7xl mx-auto px-4 py-6">
      <!-- Banner区域（可选） -->
      <section v-if="showBanner && !searchKeyword" class="mb-6 rounded-lg overflow-hidden bg-gradient-to-r from-[#00a1d6] to-[#0095c8] p-8 text-white">
        <h2 class="text-2xl font-bold mb-2">欢迎来到视频平台</h2>
        <p class="text-sm opacity-90">发现精彩视频，分享美好生活</p>
      </section>

      <!-- 搜索结果提示 -->
      <section v-if="searchKeyword" class="mb-4">
        <div class="flex items-center justify-between">
          <p class="text-sm text-gray-600">
            搜索 "<span class="text-[#00a1d6] font-medium">{{ searchKeyword }}</span>" 的结果
          </p>
          <button
            class="text-sm text-gray-500 hover:text-[#00a1d6]"
            @click="clearSearch"
          >
            清除搜索
          </button>
        </div>
      </section>

      <!-- 视频网格 -->
      <section>
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-semibold text-gray-800">
            {{ searchKeyword ? '搜索结果' : (selectedPartition ? getPartitionName(selectedPartition) : '推荐视频') }}
          </h2>
          <span v-if="videos.length > 0" class="text-sm text-gray-500">
            共 {{ total }} 个视频
          </span>
        </div>

        <div v-if="loading && videos.length === 0" class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
          <div v-for="i in 10" :key="i" class="bg-white rounded-lg overflow-hidden animate-pulse">
            <div class="aspect-video bg-gray-200"></div>
            <div class="p-3 space-y-2">
              <div class="h-4 bg-gray-200 rounded w-3/4"></div>
              <div class="h-3 bg-gray-200 rounded w-1/2"></div>
            </div>
          </div>
        </div>

        <div v-else-if="videos.length === 0" class="text-center py-20">
          <div class="text-gray-400 text-6xl mb-4">📺</div>
          <p class="text-gray-500 text-lg">暂无视频</p>
          <p class="text-gray-400 text-sm mt-2">快来上传第一个视频吧！</p>
        </div>

        <div v-else class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
          <div
            v-for="video in videos"
            :key="video.videoId"
            class="bg-white rounded-lg overflow-hidden shadow-sm hover:shadow-lg transition-all duration-300 cursor-pointer group"
            @click="goToVideo(video.videoId)"
          >
            <!-- 封面图 -->
            <div class="relative aspect-video overflow-hidden bg-gray-100">
              <img
                v-if="video.coverUrl"
                :src="video.coverUrl"
                :alt="video.title"
                class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                @error="handleImageError"
              />
              <div v-else class="w-full h-full flex items-center justify-center bg-gradient-to-br from-gray-200 to-gray-300">
                <span class="text-gray-400 text-4xl">🎬</span>
              </div>

              <!-- 时长标签 -->
              <div v-if="video.duration" class="absolute bottom-2 right-2 px-1.5 py-0.5 bg-black/70 text-white text-xs rounded">
                {{ formatDuration(video.duration) }}
              </div>
            </div>

            <!-- 视频信息 -->
            <div class="p-3">
              <h3 class="text-sm font-medium text-gray-800 line-clamp-2 mb-2 group-hover:text-[#00a1d6] transition-colors">
                {{ video.title }}
              </h3>

              <div class="flex items-center justify-between text-xs text-gray-500">
                <span class="truncate max-w-[60%]">{{ video.account || '未知用户' }}</span>
                <div class="flex items-center gap-2">
                  <span v-if="video.playCount" class="flex items-center gap-1">
                    <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M10 12a2 2 0 100-4 2 2 0 000 4z"/>
                      <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd"/>
                    </svg>
                    {{ formatCount(video.playCount) }}
                  </span>
                </div>
              </div>

              <div class="mt-2 text-xs text-gray-400">
                {{ formatDate(video.createTime) }}
              </div>
            </div>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMore && !loading" class="mt-8 text-center">
          <button
            class="px-6 py-2 bg-white border border-gray-300 rounded-md text-sm text-gray-700 hover:bg-gray-50 hover:border-[#00a1d6] hover:text-[#00a1d6] transition-colors"
            @click="loadMore"
            :disabled="loading"
          >
            {{ loading ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { getVideoPlayList, listPartitions, searchVideos } from '../api/video';
import HeaderNav from './HeaderNav.vue';

const router = useRouter();
const videos = ref([]);
const partitions = ref([]);
const selectedPartition = ref(null);
const loading = ref(false);
const hasMore = ref(true);
const lastVideoId = ref(0);
const showBanner = ref(true);
const searchKeyword = ref('');
const currentPage = ref(1);
const total = ref(0);
const pageSize = 20;

// 加载分区列表
async function loadPartitions() {
  try {
    const data = await listPartitions();
    partitions.value = data || [];
  } catch (error) {
    console.error('加载分区失败:', error);
  }
}

// 加载视频列表（首页推荐）
async function loadVideos(isLoadMore = false) {
  if (loading.value) return;

  loading.value = true;
  try {
    const data = await getVideoPlayList(lastVideoId.value);
    const newVideos = data || [];

    if (isLoadMore) {
      videos.value = [...videos.value, ...newVideos];
    } else {
      videos.value = newVideos;
    }

    // 更新lastVideoId用于分页
    if (newVideos.length > 0) {
      lastVideoId.value = newVideos[newVideos.length - 1].videoId;
      hasMore.value = newVideos.length >= 10; // 假设每页10条
      total.value = videos.value.length;
    } else {
      hasMore.value = false;
    }
  } catch (error) {
    console.error('加载视频失败:', error);
  } finally {
    loading.value = false;
  }
}

// 搜索视频
async function handleSearch() {
  if (!searchKeyword.value.trim()) {
    clearSearch();
    return;
  }

  currentPage.value = 1;
  lastVideoId.value = 0;
  await searchVideosByKeyword(true);
}

// 执行搜索
async function searchVideosByKeyword(isNewSearch = false) {
  if (loading.value) return;

  loading.value = true;
  try {
    const result = await searchVideos({
      keyword: searchKeyword.value,
      pageNum: currentPage.value,
      pageSize: pageSize,
      sortType: 2 // 最新发布
    });

    const newVideos = result?.records || [];
    total.value = result?.total || 0;

    if (isNewSearch) {
      videos.value = newVideos;
    } else {
      videos.value = [...videos.value, ...newVideos];
    }

    hasMore.value = videos.value.length < total.value;
  } catch (error) {
    console.error('搜索视频失败:', error);
  } finally {
    loading.value = false;
  }
}

// 清除搜索
function clearSearch() {
  searchKeyword.value = '';
  currentPage.value = 1;
  lastVideoId.value = 0;
  loadVideos(false);
}

// 加载更多
function loadMore() {
  if (searchKeyword.value) {
    currentPage.value++;
    searchVideosByKeyword(false);
  } else {
    loadVideos(true);
  }
}

// 选择分区
function selectPartition(partitionId) {
  selectedPartition.value = partitionId;
  // TODO: 根据分区筛选视频，目前先清空搜索
  if (searchKeyword.value) {
    clearSearch();
  } else {
    lastVideoId.value = 0;
    loadVideos(false);
  }
}

// 获取分区名称
function getPartitionName(partitionId) {
  if (!partitionId) return '首页';
  const partition = partitions.value.find(p => p.id === partitionId);
  return partition ? partition.partitionName : '推荐视频';
}

// 跳转到视频详情页
function goToVideo(videoId) {
  router.push(`/video/${videoId}`);
}

// 返回首页
function goHome() {
  router.push('/');
}

// 格式化时长
function formatDuration(seconds) {
  if (!seconds) return '00:00';
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

// 格式化播放量
function formatCount(count) {
  if (!count) return '0';
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}万`;
  }
  return count.toString();
}

// 格式化日期
function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now - date;

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 30) return `${days}天前`;

  return date.toLocaleDateString('zh-CN');
}

// 图片加载错误处理
function handleImageError(e) {
  e.target.style.display = 'none';
}

onMounted(() => {
  loadPartitions();
  loadVideos();
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
