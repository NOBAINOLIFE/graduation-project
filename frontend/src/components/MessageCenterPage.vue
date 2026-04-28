<template>
  <div class="min-h-screen bg-[#f6f7fb]">
    <HeaderNav />

    <main class="mx-auto max-w-7xl px-4 py-6">
      <div class="mb-6 flex flex-wrap gap-3">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="rounded-full px-5 py-2.5 text-sm font-medium transition-all"
          :class="activeTab === tab.key ? 'bg-[#00a1d6] text-white shadow-sm' : 'bg-white text-[#61666d] ring-1 ring-black/5 hover:text-[#00a1d6]'"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>

      <PrivateMessagePanel v-if="activeTab === 'chat'" />
      <ReplyMessagePanel v-else-if="activeTab === 'replies'" />
      <LikeMessagePanel v-else />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import HeaderNav from './HeaderNav.vue';
import PrivateMessagePanel from './message/PrivateMessagePanel.vue';
import ReplyMessagePanel from './message/ReplyMessagePanel.vue';
import LikeMessagePanel from './message/LikeMessagePanel.vue';

const route = useRoute();
const router = useRouter();

const tabs = [
  { key: 'chat', label: '我的消息' },
  { key: 'replies', label: '回复我的' },
  { key: 'likes', label: '收到的赞' }
];

const activeTab = computed(() => {
  const tab = String(route.query.tab || 'chat');
  return tabs.some(item => item.key === tab) ? tab : 'chat';
});

function switchTab(tab) {
  router.push({
    name: 'messages',
    params: route.params,
    query: {
      ...route.query,
      tab
    }
  });
}
</script>
