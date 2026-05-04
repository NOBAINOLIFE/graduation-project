<template>
  <div 
    class="relative"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <!-- 头像 -->
    <div
      class="w-9 h-9 rounded-full overflow-hidden cursor-pointer ring-2 ring-transparent hover:ring-[#00a1d6] transition-all"
      @click="handleClick"
    >
      <img
        v-if="userInfo?.avatarUrl"
        :src="userInfo.avatarUrl"
        :alt="userInfo.username"
        class="w-full h-full object-cover"
        @error="handleImageError"
      />
      <div v-else class="w-full h-full bg-gradient-to-br from-[#00a1d6] to-[#0095c8] flex items-center justify-center text-white font-medium">
        {{ (userInfo?.username || 'U')[0].toUpperCase() }}
      </div>
    </div>

    <!-- 下拉菜单 -->
    <div
      v-show="showMenu"
      class="absolute right-0 top-full mt-2 w-72 bg-white rounded-lg shadow-xl border border-gray-200 py-2 z-50 animate-fade-in"
    >
      <!-- 用户信息卡片 -->
      <div class="px-4 py-3 border-b border-gray-100">
        <div class="flex items-start gap-3">
          <!-- 头像 -->
          <div class="w-12 h-12 rounded-full overflow-hidden flex-shrink-0 ring-2 ring-gray-100">
            <img
              v-if="userInfo?.avatarUrl"
              :src="userInfo.avatarUrl"
              :alt="userInfo.username"
              class="w-full h-full object-cover"
              @error="handleImageError"
            />
            <div v-else class="w-full h-full bg-gradient-to-br from-[#00a1d6] to-[#0095c8] flex items-center justify-center text-white text-lg font-medium">
              {{ (userInfo?.username || 'U')[0].toUpperCase() }}
            </div>
          </div>

          <!-- 用户名和签名 -->
          <div class="flex-1 min-w-0">
            <h3 class="font-medium text-gray-800 truncate">{{ userInfo?.username }}</h3>
            <p class="text-xs text-gray-500 mt-1 line-clamp-2">{{ userInfo?.bio || '这个人很懒，什么都没写~' }}</p>
          </div>
        </div>

        <!-- 统计数据 -->
        <div class="flex items-center justify-around mt-3 pt-3 border-t border-gray-100">
          <div class="text-center cursor-pointer hover:text-[#00a1d6] transition-colors" @click="goToMyVideos">
            <div class="text-lg font-semibold text-gray-800">{{ userInfo?.videoNum || 0 }}</div>
            <div class="text-xs text-gray-500">视频</div>
          </div>
          <div class="text-center cursor-pointer hover:text-[#00a1d6] transition-colors" @click="goToFollowers">
            <div class="text-lg font-semibold text-gray-800">{{ userInfo?.fansNum || 0 }}</div>
            <div class="text-xs text-gray-500">粉丝</div>
          </div>
          <div class="text-center cursor-pointer hover:text-[#00a1d6] transition-colors" @click="goToFollowing">
            <div class="text-lg font-semibold text-gray-800">{{ userInfo?.followNum || 0 }}</div>
            <div class="text-xs text-gray-500">关注</div>
          </div>
        </div>
      </div>

      <!-- 菜单项 -->
      <div class="py-1">
        <button
          class="w-full px-4 py-2.5 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-3 transition-colors"
          @click="goToPersonalCenter"
        >
          <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7h16M4 12h10M4 17h7"/>
          </svg>
          <span>个人中心</span>
        </button>

        <button
          class="w-full px-4 py-2.5 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-3 transition-colors"
          @click="goToVideoManage"
        >
          <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
          </svg>
          <span>投稿管理</span>
        </button>

      </div>

      <!-- 退出登录 -->
      <div class="border-t border-gray-100 pt-1">
        <button
          class="w-full px-4 py-2.5 text-left text-sm text-red-600 hover:bg-red-50 flex items-center gap-3 transition-colors"
          @click="handleLogout"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
          </svg>
          <span>退出登录</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { getUserInfo, logout } from '../api/user';
import { clearUserAuth, getUserId, USER_AUTH_CHANGE_EVENT } from '../utils/auth';

const router = useRouter();
const showMenu = ref(false);
const userInfo = ref(null);
let hideTimer = null;

// 鼠标进入
function handleMouseEnter() {
  if (hideTimer) {
    clearTimeout(hideTimer);
    hideTimer = null;
  }
  showMenu.value = true;

  // 如果还没有用户信息，加载它
  if (!userInfo.value) {
    loadUserInfo();
  }
}

// 鼠标离开
function handleMouseLeave() {
  hideTimer = setTimeout(() => {
    showMenu.value = false;
  }, 200);
}

// 点击头像（移动端）
function handleClick() {
  const userId = getUserId();
  if (userId) {
    router.push(`/user/${userId}`);
  }
  showMenu.value = false;
}

// 加载用户信息
async function loadUserInfo() {
  try {
    const userId = getUserId();
    if (userId) {
      userInfo.value = await getUserInfo(userId);
    }
  } catch (error) {
    console.error('加载用户信息失败:', error);
  }
}

// 图片加载错误
function handleImageError(e) {
  e.target.style.display = 'none';
}

// 跳转到个人中心
function goToPersonalCenter() {
  router.push('/user-center');
  showMenu.value = false;
}

// 跳转到投稿管理
function goToVideoManage() {
  router.push('/creator/upload');
  showMenu.value = false;
}

// 跳转到我的视频
function goToMyVideos() {
  const userId = getUserId();
  if (userId) {
    router.push(`/user/${userId}`);
  }
  showMenu.value = false;
}

// 跳转到粉丝列表
function goToFollowers() {
  const userId = getUserId();
  if (userId) {
    router.push({
      name: 'user-profile',
      params: { userId },
      query: { tab: 'fans' }
    });
  }
  showMenu.value = false;
}

// 跳转到关注列表
function goToFollowing() {
  const userId = getUserId();
  if (userId) {
    router.push({
      name: 'user-profile',
      params: { userId },
      query: { tab: 'following' }
    });
  }
  showMenu.value = false;
}

// 退出登录
async function handleLogout() {
  try {
    await logout();
  } catch (error) {
    console.error('退出登录失败:', error);
  } finally {
    // 只清除普通用户认证，不影响管理员
    clearUserAuth();
    showMenu.value = false;

    // 通知HomePage更新登录状态
    window.dispatchEvent(new CustomEvent('user-logout'));

    // 刷新页面或跳转到首页
    window.location.href = '/';
  }
}

// 监听登录状态变化，重新加载用户信息
function handleAuthChange(event) {
  if (event?.detail?.isLoggedIn) {
    // 登录后重新加载用户信息
    userInfo.value = null;
    loadUserInfo();
  } else {
    // 登出后清空用户信息
    userInfo.value = null;
  }
}

onMounted(() => {
  // 预加载用户信息
  loadUserInfo();
  window.addEventListener(USER_AUTH_CHANGE_EVENT, handleAuthChange);
});

onBeforeUnmount(() => {
  window.removeEventListener(USER_AUTH_CHANGE_EVENT, handleAuthChange);
});
</script>

<style scoped>
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in {
  animation: fade-in 0.2s ease-out;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
