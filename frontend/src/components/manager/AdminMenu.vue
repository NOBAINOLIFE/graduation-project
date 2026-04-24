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
      <div class="w-full h-full bg-gradient-to-br from-[#00a1d6] to-[#0095c8] flex items-center justify-center text-white font-medium">
        {{ (username || 'A')[0].toUpperCase() }}
      </div>
    </div>

    <!-- 下拉菜单 -->
    <div
      v-show="showMenu"
      class="absolute right-0 top-full mt-2 w-56 bg-white rounded-lg shadow-xl border border-gray-200 py-2 z-50 animate-fade-in"
    >
      <!-- 用户名 -->
      <div class="px-4 py-3 border-b border-gray-100">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-gradient-to-br from-[#00a1d6] to-[#0095c8] flex items-center justify-center text-white font-medium">
            {{ (username || 'A')[0].toUpperCase() }}
          </div>
          <div class="flex-1 min-w-0">
            <h3 class="font-medium text-gray-800 truncate">{{ username || '管理员' }}</h3>
            <p class="text-xs text-gray-500">管理后台</p>
          </div>
        </div>
      </div>

      <!-- 退出登录 -->
      <div class="py-1">
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
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { logout } from '../../api/user';
import { clearAdminAuth, getAdminUsername } from '../../utils/auth';

const router = useRouter();
const showMenu = ref(false);
const username = ref(getAdminUsername() || '');
let hideTimer = null;

// 鼠标进入
function handleMouseEnter() {
  if (hideTimer) {
    clearTimeout(hideTimer);
    hideTimer = null;
  }
  showMenu.value = true;
}

// 鼠标离开
function handleMouseLeave() {
  hideTimer = setTimeout(() => {
    showMenu.value = false;
  }, 200);
}

// 点击头像（移动端）
function handleClick() {
  showMenu.value = !showMenu.value;
}

// 退出登录
async function handleLogout() {
  try {
    await logout();
  } catch (error) {
    // Ignore network failure and continue local logout
  } finally {
    clearAdminAuth();
    showMenu.value = false;
    await router.replace('/manager/login');
  }
}
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
</style>
