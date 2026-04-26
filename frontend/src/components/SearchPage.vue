<template>
  <div class="min-h-screen bg-[#f6f7fb]">
    <HeaderNav />

    <main class="mx-auto max-w-[1680px] px-4 pb-10 pt-6 sm:px-6 lg:px-8">
      <!-- 搜索框区域 -->
      <div class="flex justify-center pb-6">
        <div class="w-full max-w-3xl">
          <div class="flex flex-col gap-4 sm:flex-row">
            <div class="relative flex-1">
              <span class="pointer-events-none absolute left-5 top-1/2 -translate-y-1/2 text-[#00a1d6]">
                <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-4.35-4.35m1.6-5.15a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </span>
              <input
                v-model="searchDraft"
                type="text"
                placeholder="搜索视频或用户"
                class="h-14 w-full rounded-2xl border border-[#dbe2ea] bg-[#f8fafc] pl-14 pr-4 text-xl text-[#18191c] outline-none transition-all focus:border-[#00a1d6] focus:bg-white focus:ring-4 focus:ring-[#00a1d6]/10"
                @keyup.enter="submitSearch"
              />
            </div>
            <button
              class="h-14 rounded-2xl bg-[#00a1d6] px-10 text-lg font-semibold text-white transition-colors hover:bg-[#0094c8]"
              @click="submitSearch"
            >
              搜索
            </button>
          </div>
        </div>
      </div>

      <!-- 标签区域 -->
      <div class="border-b border-[#e7e9ee] pb-4">
        <div class="mx-auto max-w-[1500px]">
          <div class="flex items-end gap-8">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              class="relative pb-4 text-lg font-medium transition-colors"
              :class="activeTab === tab.key ? 'text-[#00a1d6]' : 'text-[#61666d] hover:text-[#00a1d6]'"
              @click="switchTab(tab.key)"
            >
              {{ tab.label }}
              <span class="ml-2 rounded-full bg-[#f1f2f3] px-2 py-0.5 text-xs text-[#9499a0]">
                {{ formatBadgeCount(tab.count) }}
              </span>
              <span
                v-if="activeTab === tab.key"
                class="absolute bottom-0 left-0 h-1 w-full rounded-full bg-[#00a1d6]"
              ></span>
            </button>
          </div>
        </div>
      </div>

      <!-- 搜索结果区域 -->
      <div class="mx-auto max-w-[1500px] pt-4">
        <template v-if="activeTab === 'video'">
          <div class="flex flex-col gap-3">
            <!-- 排序选项 -->
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-6">
                <button
                  v-for="option in videoSortOptions"
                  :key="option.value"
                  class="rounded-lg py-2 text-sm font-medium transition-colors whitespace-nowrap w-[100px] text-center"
                  :class="videoFilters.sortType === option.value ? activeFilterClass : inactiveFilterClass"
                  @click="setVideoSort(option.value)"
                >
                  {{ option.label }}
                </button>
              </div>
              
              <button
                  class="rounded-lg py-2 text-sm font-medium transition-colors whitespace-nowrap w-[100px] text-center"
                :class="showMoreFilters ? activeFilterClass : inactiveFilterClass"
                @click="toggleMoreFilters"
              >
                {{ showMoreFilters ? '收起筛选' : '更多筛选' }}
              </button>
            </div>

            <!-- 更多筛选内容 -->
            <div v-if="showMoreFilters" class="flex flex-col gap-3">
              <!-- 日期筛选 -->
              <div class="flex items-center gap-6">
                <button
                  v-for="option in dateRangeOptions"
                  :key="option.value"
                  class="rounded-lg py-2 text-sm font-medium transition-colors whitespace-nowrap w-[100px] text-center"
                  :class="videoFilters.dateRange === option.value ? activeFilterClass : inactiveFilterClass"
                  @click="setDateRange(option.value)"
                >
                  {{ option.label }}
                </button>
                <div class="flex items-center gap-2">
                  <input
                    v-model="videoFilters.customStartDate"
                    type="date"
                    class="h-9 rounded-lg border border-[#dbe2ea] px-4 text-sm text-[#18191c] outline-none transition-colors focus:border-[#00a1d6]"
                    @change="applyCustomDateRange"
                  />
                  <span class="text-sm text-[#9499a0]">至</span>
                  <input
                    v-model="videoFilters.customEndDate"
                    type="date"
                    class="h-9 rounded-lg border border-[#dbe2ea] px-4 text-sm text-[#18191c] outline-none transition-colors focus:border-[#00a1d6]"
                    @change="applyCustomDateRange"
                  />
                </div>
              </div>

              <!-- 时长筛选 -->
              <div class="flex items-center gap-6">
                <button
                  v-for="option in durationOptions"
                  :key="option.value"
                  class="rounded-lg py-2 text-sm font-medium transition-colors whitespace-nowrap w-[100px] text-center"
                  :class="videoFilters.durationRange === option.value ? activeFilterClass : inactiveFilterClass"
                  @click="setDurationRange(option.value)"
                >
                  {{ option.label }}
                </button>
              </div>
            </div>
          </div>

          <div v-if="videoLoading && videoResults.length === 0" class="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
            <div v-for="index in 10" :key="index" class="overflow-hidden rounded-2xl bg-[#f6f7fb]">
              <div class="aspect-video animate-pulse bg-[#e8edf2]"></div>
              <div class="space-y-3 p-4">
                <div class="h-4 w-4/5 animate-pulse rounded bg-[#e8edf2]"></div>
                <div class="h-3 w-2/5 animate-pulse rounded bg-[#e8edf2]"></div>
              </div>
            </div>
          </div>

          <div v-else-if="videoResults.length === 0" class="py-24 text-center">
            <p class="text-5xl text-[#d0d7de]">搜</p>
            <p class="mt-4 text-lg text-[#61666d]">没有找到相关视频</p>
            <p class="mt-2 text-sm text-[#9499a0]">换个关键词或者调整筛选试试</p>
          </div>

          <div v-else class="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
            <article
              v-for="video in videoResults"
              :key="video.videoId"
              class="group cursor-pointer overflow-hidden rounded-2xl bg-white transition-all hover:-translate-y-1 hover:shadow-lg"
              @click="goToVideo(video.videoId)"
            >
              <div class="relative aspect-video overflow-hidden rounded-2xl bg-[#eef2f6]">
                <img
                  v-if="video.coverUrl"
                  :src="video.coverUrl"
                  :alt="video.title"
                  class="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                  @error="handleImageError"
                />
                <div v-else class="flex h-full items-center justify-center text-4xl text-[#c2cad3]">🎬</div>
                <span class="absolute bottom-2 right-2 rounded bg-black/70 px-2 py-1 text-xs text-white">
                  {{ formatDuration(video.duration) }}
                </span>
              </div>
              <div class="px-1 pb-1 pt-3">
                <h3 class="line-clamp-2 text-[15px] font-medium leading-6 text-[#18191c] transition-colors group-hover:text-[#00a1d6]">
                  {{ video.title }}
                </h3>
                <p class="mt-2 truncate text-sm text-[#61666d]">
                  {{ video.username || '未知用户' }}
                </p>
                <div class="mt-2 flex items-center justify-between text-xs text-[#9499a0]">
                  <span>{{ formatCount(video.playCount) }} 播放</span>
                  <span>{{ formatDate(video.createTime) }}</span>
                </div>
              </div>
            </article>
          </div>

          <div v-if="videoHasMore && videoResults.length > 0" class="mt-10 flex justify-center">
            <button
              class="rounded-2xl border border-[#dbe2ea] bg-white px-8 py-3 text-sm font-medium text-[#18191c] transition-colors hover:border-[#00a1d6] hover:text-[#00a1d6] disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="videoLoading"
              @click="loadMoreVideos"
            >
              {{ videoLoading ? '加载中...' : '加载更多视频' }}
            </button>
          </div>
        </template>

        <template v-else>
          <div class="flex flex-wrap items-center gap-3">
            <button
              v-for="option in userSortOptions"
              :key="option.value"
              class="rounded-lg py-2 text-sm font-medium transition-colors whitespace-nowrap w-[100px] text-center"
              :class="userFilters.sortType === option.value ? activeFilterClass : inactiveFilterClass"
              @click="setUserSort(option.value)"
            >
              {{ option.label }}
            </button>
          </div>

          <div v-if="userLoading && userResults.length === 0" class="mt-6 space-y-4">
            <div v-for="index in 6" :key="index" class="flex animate-pulse items-center gap-4 rounded-2xl bg-[#f6f7fb] p-5">
              <div class="h-16 w-16 rounded-full bg-[#e8edf2]"></div>
              <div class="flex-1 space-y-3">
                <div class="h-4 w-1/4 rounded bg-[#e8edf2]"></div>
                <div class="h-3 w-2/5 rounded bg-[#e8edf2]"></div>
              </div>
            </div>
          </div>

          <div v-else-if="userResults.length === 0" class="py-24 text-center">
            <p class="text-5xl text-[#d0d7de]">人</p>
            <p class="mt-4 text-lg text-[#61666d]">没有找到相关用户</p>
            <p class="mt-2 text-sm text-[#9499a0]">试试更准确一点的用户名关键词</p>
          </div>
          
          <div v-else class="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <article
              v-for="user in userResults"
              :key="user.userId"
              class="group cursor-pointer rounded-xl p-6 transition-all hover:bg-white hover:shadow-md"
              @click="goToUser(user.userId)"
            >
              <div class="flex items-center gap-4">
                <!-- 头像 -->
                <div class="relative h-20 w-20 flex-shrink-0 overflow-hidden rounded-full">
                  <img
                    v-if="user.avatar"
                    :src="user.avatar"
                    :alt="user.username"
                    class="h-full w-full object-cover"
                    @error="handleImageError"
                  />
                  <div v-else class="flex h-full w-full items-center justify-center text-3xl text-[#c2cad3]">👤</div>
                </div>
                          
                <!-- 右侧内容 -->
                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-3">
                    <!-- 用户信息 -->
                    <div class="min-w-0 flex-1">
                      <h3 class="text-lg font-bold text-[#18191c] line-clamp-1 group-hover:text-[#00a1d6] transition-colors">
                        {{ user.username || '未知用户' }}
                      </h3>
                      <p class="mt-1 text-xs text-[#61666d] line-clamp-1">
                        {{ formatCount(user.fansCount) }}粉丝 · {{ formatCount(user.videoCount) }}个视频 {{ user.bio || '官方账号' }}
                      </p>
                    </div>
                              
                    <!-- 关注按钮 -->
                    <button
                      class="flex-shrink-0 rounded-lg px-6 py-1.5 text-sm font-medium transition-colors"
                      :class="user.isFollow ? 'bg-gray-400 hover:bg-gray-500 text-white' : 'bg-[#00a1d6] hover:bg-[#0094c8] text-white'"
                      @click.stop="handleFollow(user)"
                    >
                      {{ user.isFollow ? '已关注' : '+ 关注' }}
                    </button>
                  </div>
                </div>
              </div>
            </article>
          </div>

          <div v-if="userHasMore && userResults.length > 0" class="mt-10 flex justify-center">
            <button
              class="rounded-2xl border border-[#dbe2ea] bg-white px-8 py-3 text-sm font-medium text-[#18191c] transition-colors hover:border-[#00a1d6] hover:text-[#00a1d6] disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="userLoading"
              @click="loadMoreUsers"
            >
              {{ userLoading ? '加载中...' : '加载更多用户' }}
            </button>
          </div>
        </template>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { searchUsers, searchVideos } from '../api/video';
import { followUser } from '../api/video';
import { getUserId } from '../utils/auth';
import HeaderNav from './HeaderNav.vue';
import { ElMessage } from 'element-plus';

const VIDEO_TAB = 'video';
const USER_TAB = 'user';
const PAGE_SIZE = 20;

const route = useRoute();
const router = useRouter();

const searchDraft = ref('');
const videoResults = ref([]);
const userResults = ref([]);
const videoTotal = ref(0);
const userTotal = ref(0);
const videoLoading = ref(false);
const userLoading = ref(false);
const videoPageNum = ref(1);
const userPageNum = ref(1);

const tabs = computed(() => ([
  { key: VIDEO_TAB, label: '视频', count: videoTotal.value },
  { key: USER_TAB, label: '用户', count: userTotal.value }
]));

const normalizedKeyword = computed(() => {
  const rawKeyword = route.query.keyword;
  return typeof rawKeyword === 'string' ? rawKeyword.trim() : '';
});

const activeTab = computed(() => (
  route.query.type === USER_TAB ? USER_TAB : VIDEO_TAB
));

const videoHasMore = computed(() => videoResults.value.length < videoTotal.value);
const userHasMore = computed(() => userResults.value.length < userTotal.value);

const activeFilterClass = 'bg-[#dff4ff] text-[#00a1d6]';
const inactiveFilterClass = 'bg-[#f6f7fb] text-[#61666d] hover:bg-[#edf1f5] hover:text-[#18191c]';

const videoSortOptions = [
  { value: null, label: '综合排序' },
  { value: 1, label: '最多播放' },
  { value: 2, label: '最新发布' },
  { value: 3, label: '最多收藏' }
];

const dateRangeOptions = [
  { value: 'all', label: '全部日期' },
  { value: 'day', label: '最近一天' },
  { value: 'week', label: '最近一周' },
  { value: 'halfYear', label: '最近半年' }
];

const durationOptions = [
  { value: 'all', label: '全部时长' },
  { value: 'short', label: '10分钟以下' },
  { value: 'medium', label: '10-30分钟' },
  { value: 'long', label: '30-60分钟' },
  { value: 'extraLong', label: '60分钟以上' }
];

const userSortOptions = [
  { value: null, label: '默认排序' },
  { value: 1, label: '粉丝最多' }
];

const videoFilters = reactive({
  sortType: null,
  dateRange: 'all',
  durationRange: 'all',
  customStartDate: '',
  customEndDate: ''
});

const userFilters = reactive({
  sortType: null
});

const showMoreFilters = ref(false);

function toggleMoreFilters() {
  showMoreFilters.value = !showMoreFilters.value;
}

function buildVideoSearchPayload(pageNum) {
  const payload = {
    keyword: normalizedKeyword.value || null,
    pageNum,
    pageSize: PAGE_SIZE
  };

  if (videoFilters.sortType != null) {
    payload.sortType = videoFilters.sortType;
  }

  const durationRange = resolveDurationRange(videoFilters.durationRange);
  if (durationRange.minDuration != null) {
    payload.minDuration = durationRange.minDuration;
  }
  if (durationRange.maxDuration != null) {
    payload.maxDuration = durationRange.maxDuration;
  }

  const dateRange = resolveDateRange(videoFilters);
  if (dateRange.publishStartTime) {
    payload.publishStartTime = dateRange.publishStartTime;
  }
  if (dateRange.publishEndTime) {
    payload.publishEndTime = dateRange.publishEndTime;
  }

  return payload;
}

function buildUserSearchPayload(pageNum) {
  return {
    keyword: normalizedKeyword.value || null,
    sortType: userFilters.sortType,
    pageNum,
    pageSize: PAGE_SIZE
  };
}

async function fetchVideoResults({ append = false } = {}) {
  if (videoLoading.value) return;
  videoLoading.value = true;
  try {
    const result = await searchVideos(buildVideoSearchPayload(videoPageNum.value));
    videoTotal.value = result?.total || 0;
    const records = result?.records || [];
    videoResults.value = append ? [...videoResults.value, ...records] : records;
  } catch (error) {
    console.error('搜索视频失败:', error);
    if (!append) {
      videoResults.value = [];
      videoTotal.value = 0;
    }
  } finally {
    videoLoading.value = false;
  }
}

async function fetchUserResults({ append = false } = {}) {
  if (userLoading.value) return;
  userLoading.value = true;
  try {
    const result = await searchUsers(buildUserSearchPayload(userPageNum.value));
    userTotal.value = result?.total || 0;
    const records = result?.records || [];
    userResults.value = append ? [...userResults.value, ...records] : records;
  } catch (error) {
    console.error('搜索用户失败:', error);
    if (!append) {
      userResults.value = [];
      userTotal.value = 0;
    }
  } finally {
    userLoading.value = false;
  }
}

async function refreshCounts() {
  try {
    const [videoCountResult, userCountResult] = await Promise.all([
      searchVideos({
        keyword: normalizedKeyword.value || null,
        pageNum: 1,
        pageSize: 1
      }),
      searchUsers({
        keyword: normalizedKeyword.value || null,
        pageNum: 1,
        pageSize: 1
      })
    ]);
    videoTotal.value = videoCountResult?.total || 0;
    userTotal.value = userCountResult?.total || 0;
  } catch (error) {
    console.error('刷新搜索统计失败:', error);
  }
}

async function refreshActiveTab() {
  if (activeTab.value === VIDEO_TAB) {
    videoPageNum.value = 1;
    await fetchVideoResults();
    return;
  }
  userPageNum.value = 1;
  await fetchUserResults();
}

function submitSearch() {
  const keyword = searchDraft.value.trim();
  if (!keyword) return;
  
  // 如果当前已经在搜索页且关键词相同，重新发起搜索
  if (route.path === '/search' && normalizedKeyword.value === keyword) {
    videoPageNum.value = 1;
    userPageNum.value = 1;
    refreshActiveTab();
    return;
  }
  
  router.push({
    path: '/search',
    query: {
      keyword,
      type: activeTab.value
    }
  });
}

function switchTab(tabKey) {
  if (tabKey === activeTab.value) return;
  router.push({
    path: '/search',
    query: {
      keyword: normalizedKeyword.value,
      type: tabKey
    }
  });
}

function setVideoSort(sortType) {
  if (videoFilters.sortType === sortType) return;
  videoFilters.sortType = sortType;
}

function setDateRange(dateRange) {
  if (videoFilters.dateRange === dateRange) return;
  videoFilters.dateRange = dateRange;
  if (dateRange !== 'custom') {
    videoFilters.customStartDate = '';
    videoFilters.customEndDate = '';
  }
}

function applyCustomDateRange() {
  videoFilters.dateRange = 'custom';
}

function setDurationRange(durationRange) {
  if (videoFilters.durationRange === durationRange) return;
  videoFilters.durationRange = durationRange;
}

function setUserSort(sortType) {
  if (userFilters.sortType === sortType) return;
  userFilters.sortType = sortType;
}

function loadMoreVideos() {
  if (!videoHasMore.value || videoLoading.value) return;
  videoPageNum.value += 1;
  fetchVideoResults({ append: true });
}

function loadMoreUsers() {
  if (!userHasMore.value || userLoading.value) return;
  userPageNum.value += 1;
  fetchUserResults({ append: true });
}

function goToVideo(videoId) {
  router.push(`/video/${videoId}`);
}

function goToUser(userId) {
  router.push(`/user/${userId}`);
}

function handleFollow(user) {
  const currentUserId = getUserId();
  if (!currentUserId) {
    ElMessage.warning('请先登录');
    return;
  }
  
  if (String(user.userId) === String(currentUserId)) {
    ElMessage.warning('不能关注自己');
    return;
  }

  const nextIsFollow = !user.isFollow;
  
  followUser({
    followeeId: user.userId,
    operation: nextIsFollow ? 1 : 0
  }).then(() => {
    user.isFollow = nextIsFollow;
    user.fansCount = Math.max(0, Number(user.fansCount || 0) + (nextIsFollow ? 1 : -1));
    ElMessage.success(nextIsFollow ? '关注成功' : '已取消关注');
  }).catch(error => {
    console.error('关注操作失败:', error);
    ElMessage.error(error.message || '关注操作失败');
  });
}

function handleImageError(event) {
  event.target.style.display = 'none';
}

function resolveDurationRange(type) {
  switch (type) {
    case 'short':
      return { minDuration: 0, maxDuration: 600 };
    case 'medium':
      return { minDuration: 600, maxDuration: 1800 };
    case 'long':
      return { minDuration: 1800, maxDuration: 3600 };
    case 'extraLong':
      return { minDuration: 3600, maxDuration: null };
    default:
      return { minDuration: null, maxDuration: null };
  }
}

function resolveDateRange(filters) {
  const now = new Date();
  const end = formatDateTimeValue(now, false);

  switch (filters.dateRange) {
    case 'day':
      return { publishStartTime: formatDateTimeValue(addDays(now, -1), false), publishEndTime: end };
    case 'week':
      return { publishStartTime: formatDateTimeValue(addDays(now, -7), false), publishEndTime: end };
    case 'halfYear':
      return { publishStartTime: formatDateTimeValue(addMonths(now, -6), false), publishEndTime: end };
    case 'custom':
      return {
        publishStartTime: filters.customStartDate ? `${filters.customStartDate} 00:00:00` : null,
        publishEndTime: filters.customEndDate ? `${filters.customEndDate} 23:59:59` : null
      };
    default:
      return { publishStartTime: null, publishEndTime: null };
  }
}

function addDays(date, days) {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

function addMonths(date, months) {
  const result = new Date(date);
  result.setMonth(result.getMonth() + months);
  return result;
}

function formatDateTimeValue(date, startOfDay) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  const hours = startOfDay ? '00' : `${date.getHours()}`.padStart(2, '0');
  const minutes = startOfDay ? '00' : `${date.getMinutes()}`.padStart(2, '0');
  const seconds = startOfDay ? '00' : `${date.getSeconds()}`.padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
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

function formatBadgeCount(count) {
  const numericCount = Number(count || 0);
  if (numericCount > 99) {
    return '99+';
  }
  return numericCount;
}

watch(
  () => [route.query.keyword, route.query.type],
  async () => {
    searchDraft.value = normalizedKeyword.value;
    videoPageNum.value = 1;
    userPageNum.value = 1;
    videoResults.value = [];
    userResults.value = [];
    await refreshCounts();
    await refreshActiveTab();
  },
  { immediate: true }
);

watch(
  () => ({
    sortType: videoFilters.sortType,
    dateRange: videoFilters.dateRange,
    durationRange: videoFilters.durationRange,
    customStartDate: videoFilters.customStartDate,
    customEndDate: videoFilters.customEndDate
  }),
  () => {
    if (activeTab.value !== VIDEO_TAB) return;
    videoPageNum.value = 1;
    fetchVideoResults();
  }
);

watch(
  () => userFilters.sortType,
  () => {
    if (activeTab.value !== USER_TAB) return;
    userPageNum.value = 1;
    fetchUserResults();
  }
);
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
