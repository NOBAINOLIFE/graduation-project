<template>
  <section class="rounded-[28px] bg-white p-6 shadow-sm ring-1 ring-black/5">
    <div class="mb-5 flex items-center justify-between">
      <div>
        <h2 class="text-xl font-semibold text-[#18191c]">回复我的</h2>
        <p class="mt-1 text-sm text-[#8b95a1]">这里会显示谁回复了你的哪条评论。</p>
      </div>
      <button
        class="rounded-full border border-[#dfe5ec] px-4 py-2 text-sm text-[#61666d] transition-colors hover:border-[#00a1d6] hover:text-[#00a1d6]"
        @click="loadReplyMessages"
      >
        刷新
      </button>
    </div>

    <div v-if="loading" class="space-y-4">
      <div
        v-for="index in 5"
        :key="index"
        class="flex animate-pulse gap-4 rounded-3xl border border-[#eef2f6] bg-[#fbfcfd] p-4"
      >
        <div class="h-12 w-12 rounded-full bg-[#e9eef3]"></div>
        <div class="flex-1 space-y-3">
          <div class="h-4 w-40 rounded bg-[#e9eef3]"></div>
          <div class="h-4 w-full rounded bg-[#eef2f6]"></div>
          <div class="h-3 w-2/3 rounded bg-[#eef2f6]"></div>
        </div>
      </div>
    </div>

    <div v-else-if="replyMessages.length > 0" class="space-y-4">
      <article
        v-for="item in replyMessages"
        :key="item.replyCommentId"
        class="rounded-3xl border border-[#eef2f6] bg-[#fbfcfd] p-5 transition-colors hover:border-[#cfeaf7]"
      >
        <div class="flex gap-4">
          <div class="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-full bg-gradient-to-br from-[#00a1d6] to-[#43c7ef] text-lg font-semibold text-white">
            <img
              v-if="item.replierAvatarUrl"
              :src="item.replierAvatarUrl"
              :alt="item.replierUsername"
              class="h-full w-full object-cover"
            />
            <span v-else>{{ getUserInitial(item.replierUsername) }}</span>
          </div>

          <div class="min-w-0 flex-1">
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <p class="text-sm leading-7 text-[#18191c]">
                  <span class="font-semibold">{{ item.replierUsername || '未知用户' }}</span>
                  回复了你的评论
                </p>
                <p class="text-xs text-[#9aa4af]">{{ formatDateTime(item.createTime) }}</p>
              </div>
              <button
                v-if="item.videoId"
                class="shrink-0 rounded-full border border-[#dfe5ec] px-3 py-1.5 text-xs text-[#61666d] transition-colors hover:border-[#00a1d6] hover:text-[#00a1d6]"
                @click="goToVideo(item.videoId)"
              >
                查看视频
              </button>
            </div>

            <div class="mt-3 rounded-2xl bg-white px-4 py-3 text-sm leading-7 text-[#18191c]">
              {{ item.replyContent || '该回复内容为空' }}
            </div>

            <div class="mt-3 rounded-2xl border border-dashed border-[#dbe4eb] bg-[#f7fafc] px-4 py-3">
              <p class="text-xs text-[#9aa4af]">你被回复的评论</p>
              <p class="mt-2 line-clamp-2 text-sm leading-7 text-[#61666d]">
                {{ item.originalCommentContent || '原评论内容不可用' }}
              </p>
            </div>

            <p v-if="item.videoTitle" class="mt-3 text-xs text-[#7f8a97]">
              来自视频《{{ item.videoTitle }}》
            </p>
          </div>
        </div>
      </article>
    </div>

    <div v-else class="flex flex-col items-center justify-center py-24 text-center">
      <div class="flex h-20 w-20 items-center justify-center rounded-full bg-[#eef4f8] text-3xl text-[#9cb5c3]">回</div>
      <h2 class="mt-6 text-2xl font-semibold text-[#18191c]">还没有回复消息</h2>
      <p class="mt-3 max-w-md text-sm leading-7 text-[#8b95a1]">当别人回复你的评论时，这里会第一时间显示出来。</p>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getReplyMessages } from '../../api/chat';

const router = useRouter();
const loading = ref(false);
const replyMessages = ref([]);

function getUserInitial(name) {
  return String(name || 'U').trim().charAt(0).toUpperCase() || 'U';
}

function formatDateTime(value) {
  if (!value) {
    return '';
  }
  const date = new Date(String(value).replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${month}-${day} ${hour}:${minute}`;
}

function goToVideo(videoId) {
  router.push(`/video/${videoId}`);
}

async function loadReplyMessages() {
  try {
    loading.value = true;
    const list = await getReplyMessages();
    replyMessages.value = Array.isArray(list) ? list : [];
  } catch (error) {
    console.error('加载回复消息失败:', error);
    ElMessage.error(error.message || '加载回复消息失败');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadReplyMessages();
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
