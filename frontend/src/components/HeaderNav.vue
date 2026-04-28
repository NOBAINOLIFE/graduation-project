<template>
  <header class="sticky top-0 z-50 bg-[#f1f2f3] shadow-sm">
    <div class="max-w-[1800px] mx-auto px-6 py-2 flex items-center justify-between gap-6">
      <!-- 左侧导航菜单 -->
      <div class="flex items-center gap-6 shrink-0">
        <h1 
          class="text-lg font-bold text-[#00a1d6] cursor-pointer hover:opacity-80 transition-opacity whitespace-nowrap" 
          @click="goHome"
        >
          首页
        </h1>
      </div>

      <!-- 中间搜索框 -->
      <div v-if="!isSearchPage" class="flex-1 max-w-[500px] mx-4">
        <div class="relative">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索视频..."
            class="w-full px-4 py-2 pr-12 bg-white border border-gray-300 rounded-lg focus:outline-none focus:border-[#00a1d6] focus:ring-1 focus:ring-[#00a1d6] text-sm transition-all"
            @keyup.enter="handleSearch"
          />
          <div class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-2">
            <button
              v-if="searchKeyword"
              class="text-gray-400 hover:text-gray-600 transition-colors"
              @click="clearSearch"
            >
              <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
              </svg>
            </button>
            <button
              class="text-gray-500 hover:text-[#00a1d6] transition-colors"
              @click="handleSearch"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- 右侧功能区域 -->
      <div class="flex items-center gap-4 shrink-0">
        <template v-if="isLoggedIn">
          <UserMenu />
        </template>
        <template v-else>
          <button
            class="px-5 py-1.5 bg-[#00a1d6] text-white rounded-lg text-sm hover:bg-[#0095c8] transition-colors whitespace-nowrap"
            @click="openLogin"
          >
            登录
          </button>
        </template>

        <div class="flex flex-col items-center gap-1 cursor-pointer group" @click="goToMessage">
          <div class="relative flex h-6 w-6 items-center justify-center">
            <svg class="w-5 h-5 text-gray-600 group-hover:text-[#00a1d6] transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/>
            </svg>
            <span
              v-if="isLoggedIn && unreadTotal > 0"
              class="absolute -right-2 -top-2 min-w-[18px] rounded-full bg-[#fb7299] px-1 text-center text-[10px] leading-[18px] text-white"
            >
              {{ unreadBadge }}
            </span>
          </div>
          <span class="text-xs text-gray-600 group-hover:text-[#00a1d6] transition-colors">消息</span>
        </div>

        <div class="flex flex-col items-center gap-1 cursor-pointer group" @click="goToCollection">
          <div class="w-6 h-6 flex items-center justify-center">
            <svg class="w-5 h-5 text-gray-600 group-hover:text-[#00a1d6] transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"/>
            </svg>
          </div>
          <span class="text-xs text-gray-600 group-hover:text-[#00a1d6] transition-colors">收藏</span>
        </div>

        <div class="flex flex-col items-center gap-1 cursor-pointer group" @click="goToHistory">
          <div class="w-6 h-6 flex items-center justify-center">
            <svg class="w-5 h-5 text-gray-600 group-hover:text-[#00a1d6] transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <span class="text-xs text-gray-600 group-hover:text-[#00a1d6] transition-colors">历史</span>
        </div>

        <div class="flex flex-col items-center gap-1 cursor-pointer group" @click="goToCreator">
          <div class="w-6 h-6 flex items-center justify-center">
            <svg class="w-5 h-5 text-gray-600 group-hover:text-[#00a1d6] transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
            </svg>
          </div>
          <span class="text-xs text-gray-600 group-hover:text-[#00a1d6] transition-colors">创作中心</span>
        </div>

        <button
          class="flex items-center gap-1.5 px-5 py-2 bg-[#fb7299] text-white rounded-lg text-sm font-medium hover:bg-[#fc8bab] transition-all shadow-sm hover:shadow-md whitespace-nowrap"
          @click="goToSubmit"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"/>
          </svg>
          <span>投稿</span>
        </button>
      </div>
    </div>
  </header>

  <!-- 登录弹窗 -->
  <LoginModal v-model:visible="showLoginModal" @success="handleLoginSuccess" />
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getChatUnreadTotal } from '../api/chat';
import {
  getUserId,
  isUserLoggedIn,
  openLoginModal,
  SHOW_LOGIN_MODAL_EVENT,
  SHOW_LOGIN_MODAL_ONCE_KEY,
  USER_AUTH_CHANGE_EVENT
} from '../utils/auth';
import { CHAT_UNREAD_CHANGE_EVENT } from '../utils/chat';
import UserMenu from './UserMenu.vue';
import LoginModal from './LoginModal.vue';

const route = useRoute();
const router = useRouter();
const searchKeyword = ref('');
const showLoginModal = ref(false);
const isLoggedIn = ref(isUserLoggedIn());
const unreadTotal = ref(0);
const isSearchPage = computed(() => route.path === '/search');
const unreadBadge = computed(() => (unreadTotal.value > 99 ? '99+' : String(unreadTotal.value)));

function goHome() {
  router.push('/');
}

function clearSearch() {
  searchKeyword.value = '';
}

function handleSearch() {
  const keyword = searchKeyword.value.trim();
  if (!keyword) return;
  const nextType = route.path === '/search' && route.query.type === 'user' ? 'user' : 'video';
  router.push({
    path: '/search',
    query: { keyword, type: nextType }
  });
}

function handleLoginSuccess() {
  showLoginModal.value = false;
  isLoggedIn.value = true;
}

function openLogin() {
  showLoginModal.value = true;
}

function ensureLoggedIn(actionText = '该功能需要登录后使用') {
  if (isLoggedIn.value) return true;
  openLoginModal({ actionText });
  showLoginModal.value = true;
  return false;
}

function goToCreator() {
  if (!ensureLoggedIn('登录后可进入创作中心')) return;
  router.push('/creator');
}

function goToSubmit() {
  if (!ensureLoggedIn('登录后可投稿')) return;
  router.push('/creator/upload');
}

function goToHistory() {
  if (!ensureLoggedIn('登录后可查看历史记录')) return;
  router.push('/history');
}

function goToCollection() {
  if (!ensureLoggedIn('登录后可查看收藏')) return;
  const userId = getUserId();
  if (!userId) return;
  router.push({
    path: `/user/${userId}`,
    query: { tab: 'favorites' }
  });
}

function goToMessage() {
  if (!ensureLoggedIn('登录后可查看消息')) return;
  router.push({
    name: 'messages',
    query: { tab: 'chat' }
  });
}

function syncLoginStatus(event) {
  isLoggedIn.value = event?.detail?.isLoggedIn ?? isUserLoggedIn();
  if (!isLoggedIn.value) {
    unreadTotal.value = 0;
    return;
  }
  refreshUnreadTotal();
}

function handleShowLoginModal() {
  showLoginModal.value = true;
}

async function refreshUnreadTotal() {
  if (!isUserLoggedIn()) {
    unreadTotal.value = 0;
    return;
  }
  try {
    unreadTotal.value = Number(await getChatUnreadTotal()) || 0;
  } catch (error) {
    console.error('加载未读消息数失败:', error);
  }
}

function handleUnreadChanged(event) {
  unreadTotal.value = Number(event?.detail?.total || 0);
}

onMounted(() => {
  syncLoginStatus();
  searchKeyword.value = typeof route.query.keyword === 'string' ? route.query.keyword : '';
  if (sessionStorage.getItem(SHOW_LOGIN_MODAL_ONCE_KEY) === '1') {
    sessionStorage.removeItem(SHOW_LOGIN_MODAL_ONCE_KEY);
    showLoginModal.value = true;
  }
  window.addEventListener(USER_AUTH_CHANGE_EVENT, syncLoginStatus);
  window.addEventListener(SHOW_LOGIN_MODAL_EVENT, handleShowLoginModal);
  window.addEventListener(CHAT_UNREAD_CHANGE_EVENT, handleUnreadChanged);
  refreshUnreadTotal();
});

onBeforeUnmount(() => {
  window.removeEventListener(USER_AUTH_CHANGE_EVENT, syncLoginStatus);
  window.removeEventListener(SHOW_LOGIN_MODAL_EVENT, handleShowLoginModal);
  window.removeEventListener(CHAT_UNREAD_CHANGE_EVENT, handleUnreadChanged);
});

watch(
  () => route.query.keyword,
  (keyword) => {
    searchKeyword.value = typeof keyword === 'string' ? keyword : '';
  }
);
</script>

<style scoped>
/* 可以添加自定义样式 */
</style>
