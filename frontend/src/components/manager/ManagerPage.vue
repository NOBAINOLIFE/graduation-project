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

            <!-- 用户管理 -->
            <button
              class="flex items-center gap-3 w-full px-4 py-3 rounded-lg text-sm font-medium transition-colors text-left"
              :class="activeTab === 'userManage' ? 'bg-[#00a1d6] text-white' : 'text-gray-700 hover:bg-gray-100'"
              @click="activeTab = 'userManage'"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a4 4 0 0 0-4-4h-1M9 20H4v-2a4 4 0 0 1 4-4h1m0-4a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm8 0a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"/>
              </svg>
              <span>用户管理</span>
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

            <button
              class="flex items-center gap-3 w-full px-4 py-3 rounded-lg text-sm font-medium transition-colors text-left"
              :class="activeTab === 'partitionManage' ? 'bg-[#00a1d6] text-white' : 'text-gray-700 hover:bg-gray-100'"
              @click="activeTab = 'partitionManage'"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7h18M6 12h12M10 17h4"/>
              </svg>
              <span>分区管理</span>
            </button>

            <button
              class="flex items-center gap-3 w-full px-4 py-3 rounded-lg text-sm font-medium transition-colors text-left"
              :class="activeTab === 'tagManage' ? 'bg-[#00a1d6] text-white' : 'text-gray-700 hover:bg-gray-100'"
              @click="activeTab = 'tagManage'"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h6l8 8-8 8-8-8V3h2z"/>
              </svg>
              <span>标签管理</span>
            </button>
          </div>
        </nav>
      </aside>

      <!-- 右侧内容区 -->
      <main class="flex-1 min-h-[calc(100vh-60px)] p-6">
        <ManagerVideoAuditTab v-if="activeTab === 'videoAudit'" />
        <ManagerUserTab v-else-if="activeTab === 'userManage'" />
        <ManagerReportTab v-else-if="activeTab === 'reportManage'" />
        <ManagerPartitionTab v-else-if="activeTab === 'partitionManage'" />
        <ManagerTagTab v-else />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import ManagerPartitionTab from './ManagerPartitionTab.vue';
import ManagerReportTab from './ManagerReportTab.vue';
import ManagerTagTab from './ManagerTagTab.vue';
import ManagerUserTab from './ManagerUserTab.vue';
import ManagerVideoAuditTab from './ManagerVideoAuditTab.vue';
import AdminMenu from './AdminMenu.vue';

const activeTab = ref('videoAudit');
</script>
