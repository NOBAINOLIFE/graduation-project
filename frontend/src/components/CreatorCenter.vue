<template>
  <div class="min-h-screen bg-[#f4f5f7]">
    <!-- 顶部导航栏 -->
    <header class="sticky top-0 z-50 bg-white border-b border-gray-200 shadow-sm">
      <div class="flex items-center justify-between px-6 py-3">
        <div class="flex items-center gap-4">
          <h1 class="text-xl font-bold text-[#00a1d6]">创作中心</h1>
          <button 
            class="flex items-center gap-1 text-sm text-gray-600 hover:text-[#00a1d6] transition-colors"
            @click="goToHome"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/>
            </svg>
            返回主站
          </button>
        </div>
        
        <UserMenu />
      </div>
    </header>

    <div class="flex">
      <!-- 左侧导航栏 -->
      <aside class="w-56 bg-white min-h-[calc(100vh-60px)] border-r border-gray-200 py-4">
        <nav class="px-3">
          <!-- 投稿按钮 - 突出的蓝色大按钮 -->
          <router-link
            to="/creator/upload"
            class="flex items-center justify-center gap-2 px-4 py-3 rounded-lg text-sm font-bold text-white mb-4 transition-all"
            :class="activeMenu === 'upload' ? 'bg-[#00a1d6] shadow-md' : 'bg-[#00a1d6] hover:bg-[#0090c0]'"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
            </svg>
            <span>投稿</span>
          </router-link>

          <!-- 分割线 -->
          <div class="border-t border-gray-200 my-3"></div>

          <!-- 其他导航项 -->
          <div class="space-y-1">
            <!-- 稿件管理 -->
            <router-link
              to="/creator/content"
              class="flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-colors"
              :class="activeMenu === 'content' ? 'bg-[#00a1d6] text-white' : 'text-gray-700 hover:bg-gray-100'"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
              <span>稿件管理</span>
            </router-link>

            <!-- 粉丝管理 -->
            <router-link
              to="/creator/fans"
              class="flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-colors"
              :class="activeMenu === 'fans' ? 'bg-[#00a1d6] text-white' : 'text-gray-700 hover:bg-gray-100'"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"/>
              </svg>
              <span>粉丝管理</span>
            </router-link>

            <!-- 评论管理 -->
            <router-link
              to="/creator/comments"
              class="flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-colors"
              :class="activeMenu === 'comments' ? 'bg-[#00a1d6] text-white' : 'text-gray-700 hover:bg-gray-100'"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
              </svg>
              <span>评论管理</span>
            </router-link>
          </div>
        </nav>
      </aside>

      <!-- 右侧内容区 -->
      <main class="flex-1 min-h-[calc(100vh-60px)]">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import UserMenu from './UserMenu.vue';

const route = useRoute();
const router = useRouter();
const activeMenu = ref('upload');

// 根据路由更新激活的菜单项
watch(() => route.path, (newPath) => {
  if (newPath.includes('/upload')) {
    activeMenu.value = 'upload';
  } else if (newPath.includes('/home')) {
    activeMenu.value = 'home';
  } else if (newPath.includes('/content')) {
    activeMenu.value = 'content';
  } else if (newPath.includes('/fans')) {
    activeMenu.value = 'fans';
  } else if (newPath.includes('/comments')) {
    activeMenu.value = 'comments';
  }
}, { immediate: true });

function goToHome() {
  router.push('/');
}
</script>
