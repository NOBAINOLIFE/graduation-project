<template>
  <section class="overflow-hidden rounded-[28px] bg-white shadow-sm ring-1 ring-black/5">
    <div class="flex min-h-[680px]">
      <aside class="flex w-[360px] shrink-0 flex-col border-r border-[#edf1f5] bg-[#fbfcfd]">
        <div class="border-b border-[#edf1f5] px-5 py-4">
          <div class="flex items-center justify-between">
            <div>
              <h2 class="text-xl font-semibold text-[#18191c]">收到的赞</h2>
              <p class="mt-1 text-sm text-[#8b95a1]">评论被赞和视频被赞都会显示在这里。</p>
            </div>
            <button
              class="rounded-full border border-[#dfe5ec] px-3 py-1.5 text-sm text-[#61666d] transition-colors hover:border-[#00a1d6] hover:text-[#00a1d6]"
              @click="loadSummaries"
            >
              刷新
            </button>
          </div>
        </div>

        <div class="flex-1 overflow-y-auto px-3 py-3">
          <div v-if="summaryLoading" class="space-y-3">
            <div
              v-for="index in 5"
              :key="index"
              class="rounded-3xl border border-[#eef2f6] bg-white p-4 animate-pulse"
            >
              <div class="h-4 w-2/3 rounded bg-[#e9eef3]"></div>
              <div class="mt-3 h-16 rounded-2xl bg-[#eef2f6]"></div>
            </div>
          </div>

          <template v-else-if="summaries.length > 0">
            <button
              v-for="item in summaries"
              :key="`${item.targetType}-${item.targetId}`"
              class="mb-3 w-full rounded-3xl border p-4 text-left transition-all"
              :class="isActiveSummary(item) ? 'border-[#c7ebfa] bg-[#eef8fd] shadow-sm' : 'border-[#eef2f6] bg-white hover:border-[#dce8f2]'"
              @click="selectSummary(item)"
            >
              <p class="text-sm leading-7 text-[#18191c]">
                {{ buildSummaryText(item) }}
              </p>
              <div class="mt-3 flex items-center gap-3">
                <div v-if="item.targetType === 'video'" class="h-14 w-24 overflow-hidden rounded-2xl bg-[#eef2f6]">
                  <img
                    v-if="item.videoCoverUrl"
                    :src="item.videoCoverUrl"
                    :alt="item.videoTitle"
                    class="h-full w-full object-cover"
                  />
                  <div v-else class="flex h-full w-full items-center justify-center text-[#a9b6c2]">封面</div>
                </div>
                <div
                  v-else
                  class="line-clamp-3 flex-1 rounded-2xl bg-[#f6f8fb] px-3 py-3 text-sm leading-6 text-[#61666d]"
                >
                  {{ item.commentContent || '评论内容不可用' }}
                </div>
              </div>
              <p class="mt-3 text-xs text-[#9aa4af]">{{ formatDateTime(item.latestLikeTime) }}</p>
            </button>
          </template>

          <div v-else class="flex h-full flex-col items-center justify-center px-6 text-center">
            <div class="flex h-16 w-16 items-center justify-center rounded-full bg-[#eef4f8] text-2xl text-[#9cb5c3]">赞</div>
            <h2 class="mt-5 text-lg font-medium text-[#18191c]">还没有收到赞</h2>
            <p class="mt-2 text-sm leading-6 text-[#8b95a1]">当有人给你的评论或视频点赞时，这里会汇总展示。</p>
          </div>
        </div>
      </aside>

      <section class="flex min-w-0 flex-1 flex-col bg-[linear-gradient(180deg,#ffffff_0%,#f8fbfd_100%)]">
        <template v-if="activeSummary">
          <div class="border-b border-[#edf1f5] px-6 py-5">
            <div class="flex items-start justify-between gap-4">
              <div class="min-w-0">
                <p class="text-xs uppercase tracking-[0.3em] text-[#9aa4af]">
                  {{ activeSummary.targetType === 'comment' ? 'Comment Like' : 'Video Like' }}
                </p>
                <h3 class="mt-2 text-2xl font-semibold text-[#18191c]">{{ buildSummaryText(activeSummary) }}</h3>
                <p class="mt-2 text-sm text-[#8b95a1]">最近 20 位点赞用户会显示在下方。</p>
              </div>
              <button
                v-if="detail?.videoId"
                class="shrink-0 rounded-full border border-[#dfe5ec] px-4 py-2 text-sm text-[#61666d] transition-colors hover:border-[#00a1d6] hover:text-[#00a1d6]"
                @click="goToVideo(detail.videoId)"
              >
                {{ detail.targetType === 'comment' ? '查看评论所在视频' : '查看视频' }}
              </button>
            </div>

            <div v-if="detail" class="mt-5 rounded-3xl border border-[#ebf0f4] bg-white p-4">
              <template v-if="detail.targetType === 'comment'">
                <p class="text-xs text-[#9aa4af]">被点赞的评论</p>
                <p class="mt-2 text-sm leading-7 text-[#18191c]">{{ detail.commentContent || '评论内容不可用' }}</p>
                <p v-if="detail.videoTitle" class="mt-3 text-xs text-[#7f8a97]">来自视频《{{ detail.videoTitle }}》</p>
              </template>
              <template v-else>
                <div class="flex items-center gap-4">
                  <div class="h-20 w-36 overflow-hidden rounded-2xl bg-[#eef2f6]">
                    <img
                      v-if="detail.videoCoverUrl"
                      :src="detail.videoCoverUrl"
                      :alt="detail.videoTitle"
                      class="h-full w-full object-cover"
                    />
                    <div v-else class="flex h-full w-full items-center justify-center text-[#a9b6c2]">封面</div>
                  </div>
                  <div class="min-w-0 flex-1">
                    <p class="text-xs text-[#9aa4af]">被点赞的视频</p>
                    <p class="mt-2 truncate text-lg font-medium text-[#18191c]">{{ detail.videoTitle || '未命名视频' }}</p>
                  </div>
                </div>
              </template>
            </div>
          </div>

          <div class="flex-1 overflow-y-auto px-6 py-5">
            <div v-if="detailLoading" class="space-y-4">
              <div
                v-for="index in 6"
                :key="index"
                class="flex animate-pulse gap-4 rounded-3xl border border-[#eef2f6] bg-white p-4"
              >
                <div class="h-12 w-12 rounded-full bg-[#e9eef3]"></div>
                <div class="flex-1 space-y-2">
                  <div class="h-4 w-28 rounded bg-[#e9eef3]"></div>
                  <div class="h-3 w-20 rounded bg-[#eef2f6]"></div>
                </div>
              </div>
            </div>

            <div v-else-if="detail?.recentUsers?.length" class="space-y-4">
              <article
                v-for="user in detail.recentUsers"
                :key="`${detail.targetType}-${detail.targetId}-${user.userId}-${user.likeTime}`"
                class="flex items-center gap-4 rounded-3xl border border-[#eef2f6] bg-white p-4"
              >
                <div class="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-full bg-gradient-to-br from-[#00a1d6] to-[#43c7ef] text-lg font-semibold text-white">
                  <img
                    v-if="user.avatarUrl"
                    :src="user.avatarUrl"
                    :alt="user.username"
                    class="h-full w-full object-cover"
                  />
                  <span v-else>{{ getUserInitial(user.username) }}</span>
                </div>
                <div class="min-w-0 flex-1">
                  <p class="truncate text-sm font-semibold text-[#18191c]">{{ user.username || '未知用户' }}</p>
                  <p class="mt-1 text-xs text-[#9aa4af]">{{ formatDateTime(user.likeTime) }}</p>
                </div>
                <button
                  class="rounded-full border border-[#dfe5ec] px-3 py-1.5 text-xs text-[#61666d] transition-colors hover:border-[#00a1d6] hover:text-[#00a1d6]"
                  @click="goToUser(user.userId)"
                >
                  进入主页
                </button>
              </article>
            </div>

            <div v-else class="flex flex-col items-center justify-center py-24 text-center">
              <div class="flex h-20 w-20 items-center justify-center rounded-full bg-[#eef4f8] text-3xl text-[#9cb5c3]">赞</div>
              <h2 class="mt-6 text-2xl font-semibold text-[#18191c]">还没有点赞详情</h2>
              <p class="mt-3 max-w-md text-sm leading-7 text-[#8b95a1]">选择左侧一条消息后，这里会展示最近 20 位点赞用户。</p>
            </div>
          </div>
        </template>

        <div v-else class="flex flex-1 flex-col items-center justify-center px-10 text-center">
          <div class="flex h-20 w-20 items-center justify-center rounded-full bg-[#eef4f8] text-3xl text-[#9cb5c3]">赞</div>
          <h2 class="mt-6 text-2xl font-semibold text-[#18191c]">选择一条点赞消息</h2>
          <p class="mt-3 max-w-md text-sm leading-7 text-[#8b95a1]">左侧会汇总展示谁赞了你的评论或视频，点击后即可查看最近的点赞用户。</p>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getLikeMessageDetail, getLikeMessageSummaries } from '../../api/chat';

const router = useRouter();
const summaryLoading = ref(false);
const detailLoading = ref(false);
const summaries = ref([]);
const activeKey = ref('');
const detail = ref(null);

const activeSummary = computed(() => {
  return summaries.value.find(item => `${item.targetType}-${item.targetId}` === activeKey.value) || null;
});

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

function buildSummaryText(item) {
  const names = Array.isArray(item?.previewUsernames) ? item.previewUsernames.filter(Boolean) : [];
  const subject = item?.targetType === 'comment' ? '我的评论' : '我的视频';
  if (names.length === 0) {
    return `共有 ${item?.totalCount || 0} 人赞了${subject}`;
  }
  return `${names.join('、')}等总计${item?.totalCount || names.length}人赞了${subject}`;
}

function isActiveSummary(item) {
  return activeKey.value === `${item.targetType}-${item.targetId}`;
}

function goToVideo(videoId) {
  router.push(`/video/${videoId}`);
}

function goToUser(userId) {
  router.push({
    name: 'user-profile',
    params: { userId }
  });
}

async function loadDetail(item) {
  if (!item?.targetType || !item?.targetId) {
    detail.value = null;
    return;
  }
  try {
    detailLoading.value = true;
    detail.value = await getLikeMessageDetail(item.targetType, item.targetId);
  } catch (error) {
    console.error('加载点赞详情失败:', error);
    ElMessage.error(error.message || '加载点赞详情失败');
  } finally {
    detailLoading.value = false;
  }
}

async function selectSummary(item) {
  const nextKey = `${item.targetType}-${item.targetId}`;
  activeKey.value = nextKey;
  await loadDetail(item);
}

async function loadSummaries() {
  try {
    summaryLoading.value = true;
    const list = await getLikeMessageSummaries();
    summaries.value = Array.isArray(list) ? list : [];
    if (summaries.value.length === 0) {
      activeKey.value = '';
      detail.value = null;
      return;
    }
    const current = summaries.value.find(item => `${item.targetType}-${item.targetId}` === activeKey.value);
    await selectSummary(current || summaries.value[0]);
  } catch (error) {
    console.error('加载点赞消息失败:', error);
    ElMessage.error(error.message || '加载点赞消息失败');
  } finally {
    summaryLoading.value = false;
  }
}

onMounted(() => {
  loadSummaries();
});
</script>

<style scoped>
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
