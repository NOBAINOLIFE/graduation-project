<template>
  <div class="min-h-screen bg-[#f4f5f7]">
    <!-- 顶部导航栏 -->
    <header class="sticky top-0 z-50 bg-white border-b border-gray-200 shadow-sm">
      <div class="flex items-center justify-between px-6 py-3">
        <h1 class="text-xl font-bold text-[#00a1d6]">管理后台</h1>
        
        <AdminMenu />
      </div>
    </header>

    <div class="flex">
      <!-- 左侧导航栏 -->
      <aside class="w-56 bg-white min-h-[calc(100vh-60px)] border-r border-gray-200 py-4">
        <nav class="px-3">
          <div class="space-y-1">
            <!-- 视频审核 -->
            <button
              class="flex items-center gap-3 w-full px-4 py-3 rounded-lg text-sm font-medium transition-colors text-left"
              :class="activeTab === 'videoAudit' ? 'bg-[#00a1d6] text-white' : 'text-gray-700 hover:bg-gray-100'"
              @click="activeTab = 'videoAudit'"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
              <span>视频审核</span>
            </button>

            <!-- 举报管理 -->
            <button
              class="flex items-center gap-3 w-full px-4 py-3 rounded-lg text-sm font-medium transition-colors text-left"
              :class="activeTab === 'reportManage' ? 'bg-[#00a1d6] text-white' : 'text-gray-700 hover:bg-gray-100'"
              @click="activeTab = 'reportManage'"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
              </svg>
              <span>举报管理</span>
            </button>
          </div>
        </nav>
      </aside>

      <!-- 右侧内容区 -->
      <main class="flex-1 min-h-[calc(100vh-60px)] p-6">
        <ManagerVideoAuditTab v-if="activeTab === 'videoAudit'" />
        <ManagerReportTab v-else />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import ManagerReportTab from './ManagerReportTab.vue';
import ManagerVideoAuditTab from './ManagerVideoAuditTab.vue';
import AdminMenu from './AdminMenu.vue';

const activeTab = ref('videoAudit');
</script>
