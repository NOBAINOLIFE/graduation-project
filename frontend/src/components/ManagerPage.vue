<template>
  <div class="min-h-screen bg-[#f6f7f8] pb-20 text-slate-900">
    <header class="sticky top-0 z-10 border-b border-slate-200 bg-white">
      <div class="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
        <h1 class="text-lg font-medium">管理后台</h1>
        <div class="flex items-center gap-3">
          <span class="text-sm text-slate-500">{{ username || '管理员' }}</span>
          <button
            class="rounded border border-slate-200 px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-50"
            @click="handleLogout"
          >
            退出登录
          </button>
          <div class="h-8 w-8 rounded-full bg-slate-200"></div>
        </div>
      </div>
    </header>

    <main class="mx-auto mt-6 grid max-w-6xl gap-4 px-4 md:grid-cols-[220px_1fr]">
      <aside class="h-fit rounded-lg bg-white p-3 shadow-sm ring-1 ring-slate-200">
        <button
          class="mb-2 w-full rounded px-3 py-2 text-left text-sm transition-colors"
          :class="
            activeTab === 'videoAudit'
              ? 'bg-[#e8f7fd] font-medium text-[#00a1d6]'
              : 'text-slate-700 hover:bg-slate-50'
          "
          @click="activeTab = 'videoAudit'"
        >
          视频审核
        </button>
        <button
          class="w-full rounded px-3 py-2 text-left text-sm transition-colors"
          :class="
            activeTab === 'reportManage'
              ? 'bg-[#e8f7fd] font-medium text-[#00a1d6]'
              : 'text-slate-700 hover:bg-slate-50'
          "
          @click="activeTab = 'reportManage'"
        >
          举报管理
        </button>
      </aside>

      <section>
        <ManagerVideoAuditTab v-if="activeTab === 'videoAudit'" />
        <ManagerReportTab v-else />
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { logout } from '../api/user';
import ManagerReportTab from './manager/ManagerReportTab.vue';
import ManagerVideoAuditTab from './manager/ManagerVideoAuditTab.vue';
import { clearAuth } from '../utils/auth';

const router = useRouter();
const activeTab = ref('videoAudit');
const username = ref(localStorage.getItem('username') || '');

async function handleLogout() {
  try {
    await logout();
  } catch (error) {
    // Ignore network failure and continue local logout to avoid dead session.
  } finally {
    clearAuth();
    await router.replace('/manager/login');
  }
}
</script>
