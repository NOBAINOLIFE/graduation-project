<template>
  <section class="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
    <div class="mb-5 flex flex-wrap items-center gap-3">
      <h2 class="text-lg font-semibold text-slate-900">视频审核</h2>
      <select
        v-model.number="filters.status"
        class="rounded-lg border border-slate-200 px-3 py-1.5 text-sm text-slate-700 focus:border-[#00a1d6] focus:outline-none"
      >
        <option :value="-1">全部</option>
        <option :value="0">审核中</option>
        <option :value="1">已通过</option>
        <option :value="2">已驳回</option>
      </select>
      <span class="text-xs text-slate-400">切换状态后会自动刷新列表</span>
    </div>

    <div class="space-y-4">
      <article
        v-for="item in records"
        :key="item.videoId"
        class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
      >
        <div class="flex flex-wrap items-center gap-4 p-4 md:flex-nowrap">
          <div class="relative h-[108px] w-[192px] shrink-0 overflow-hidden rounded-xl bg-slate-100 ring-1 ring-slate-200">
            <img :src="item.coverUrl || fallbackCover" class="h-full w-full object-cover" alt="cover" />
            <span class="absolute bottom-2 right-2 rounded-md bg-black/75 px-2 py-0.5 text-xs text-white">
              {{ formatDurationLabel(item.videoId) }}
            </span>
          </div>

          <div class="min-w-[220px] flex-1">
            <div class="flex items-center gap-3">
              <img :src="resolveAvatar(item.avatar)" class="h-10 w-10 rounded-full object-cover ring-1 ring-slate-200" alt="avatar" />
              <div class="min-w-0">
                <p class="truncate font-medium text-slate-900">{{ item.username || '未知用户' }}</p>
                <p class="text-xs text-slate-500">用户 ID：{{ item.userId || '-' }}</p>
              </div>
            </div>
            <p class="mt-3 line-clamp-1 text-sm text-slate-700">{{ item.title || '-' }}</p>
            <p class="mt-1 text-xs text-slate-500">
              状态：{{ statusText(item.status) }} · 提交时间：{{ formatTime(item.createTime) }}
            </p>
          </div>

          <div class="flex shrink-0 items-center gap-2">
            <div v-if="item.status === 0" class="flex items-center gap-2">
              <button
                class="rounded-lg bg-emerald-500 px-3 py-1.5 text-xs text-white transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:bg-slate-300"
                :disabled="operatingVideoId === item.videoId"
                @click="submitAudit(item.videoId, 1, '')"
              >
                通过
              </button>
              <button
                class="rounded-lg bg-rose-500 px-3 py-1.5 text-xs text-white transition hover:bg-rose-600 disabled:cursor-not-allowed disabled:bg-slate-300"
                :disabled="operatingVideoId === item.videoId"
                @click="openRejectDialog(item.videoId)"
              >
                驳回
              </button>
            </div>

            <div v-else class="min-w-[220px]">
              <div class="flex items-center gap-2">
                <img
                  :src="resolveAvatar(item.reviewerAvatar)"
                  class="h-8 w-8 rounded-full object-cover ring-1 ring-slate-200"
                  alt="reviewer-avatar"
                />
                <div class="leading-tight">
                  <p class="text-sm font-medium text-slate-800">{{ item.reviewerName || '未知管理员' }}</p>
                  <p class="text-xs text-slate-500">管理员 ID：{{ item.reviewerId || '-' }}</p>
                </div>
              </div>
              <p class="mt-1 text-xs text-slate-500">审核时间：{{ formatTime(item.updateTime) }}</p>
              <p v-if="item.status === 2" class="mt-1 text-xs text-rose-500">驳回原因：{{ item.reviewNote || '-' }}</p>
            </div>

            <button
              class="ml-1 flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-500 transition hover:bg-slate-50"
              @click="toggleExpand(item.videoId)"
            >
              <svg
                class="h-4 w-4 transition-transform"
                :class="expandedVideoId === item.videoId ? 'rotate-180' : ''"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
              </svg>
            </button>
          </div>
        </div>

        <div v-if="expandedVideoId === item.videoId" class="border-t border-slate-200 bg-slate-50 p-4">
          <div class="grid gap-4 lg:grid-cols-[1fr_240px]">
            <div class="space-y-2 text-sm text-slate-700">
              <p><span class="text-slate-500">标题：</span>{{ item.title || '-' }}</p>
              <p><span class="text-slate-500">简介：</span>{{ item.description || '-' }}</p>
              <p><span class="text-slate-500">视频 ID：</span>{{ item.videoId }}</p>
              <p><span class="text-slate-500">分区：</span>{{ item.partitionName || '-' }}</p>
              <div class="flex flex-wrap items-center gap-2">
                <span class="text-slate-500">标签：</span>
                <span
                  v-for="tag in item.tagList || []"
                  :key="tag.tagId || tag.tagName"
                  class="rounded-full bg-[#e8f7fd] px-2 py-0.5 text-xs text-[#00a1d6]"
                >
                  {{ tag.tagName }}
                </span>
                <span v-if="!item.tagList || !item.tagList.length" class="text-slate-400">-</span>
              </div>
            </div>

            <div>
              <button class="group block w-full overflow-hidden rounded-xl ring-1 ring-slate-200" @click="openCoverPreview(item.coverUrl)">
                <img
                  :src="item.coverUrl || fallbackCover"
                  class="h-32 w-full object-cover transition group-hover:scale-[1.02]"
                  alt="cover"
                />
              </button>
              <p class="mt-1 text-center text-xs text-slate-500">点击放大封面</p>
            </div>
          </div>

          <div class="mt-4 bg-black p-2 shadow-[0_24px_50px_rgba(15,23,42,0.2)]">
            <div class="aspect-video">
              <AppVideoPlayer
                compact
                :media-key="item.videoId"
                :sources="item.videoSourceList || []"
                :poster="item.coverUrl || fallbackCover"
                :default-source-index="findDefaultSourceIndex(item)"
                empty-text="当前稿件暂无可播放源"
              />
            </div>
          </div>
        </div>
      </article>

      <div v-if="!records.length && !loading" class="py-8 text-center text-slate-400">暂无数据</div>
    </div>

    <div class="mt-4 flex items-center justify-between text-sm text-slate-500">
      <p>共 {{ pagination.total }} 条</p>
      <div class="flex items-center gap-2">
        <button
          class="rounded-lg border border-slate-200 px-3 py-1 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-300"
          :disabled="pagination.pageNum <= 1 || loading"
          @click="changePage(pagination.pageNum - 1)"
        >
          上一页
        </button>
        <span>第 {{ pagination.pageNum }} / {{ totalPages }} 页</span>
        <button
          class="rounded-lg border border-slate-200 px-3 py-1 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-300"
          :disabled="pagination.pageNum >= totalPages || loading"
          @click="changePage(pagination.pageNum + 1)"
        >
          下一页
        </button>
      </div>
    </div>
  </section>

  <div
    v-if="previewCoverUrl"
    class="fixed inset-0 z-40 flex items-center justify-center bg-black/70 p-4"
    @click="previewCoverUrl = ''"
  >
    <img :src="previewCoverUrl" class="max-h-[90vh] max-w-[90vw] rounded-2xl bg-white" alt="cover-preview" />
  </div>

  <div
    v-if="rejectDialog.visible"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4"
    @click.self="closeRejectDialog"
  >
    <div class="w-full max-w-3xl rounded-2xl bg-white p-6 shadow-xl">
      <div class="mb-4 flex items-center justify-between">
        <h3 class="text-xl font-semibold text-slate-900">驳回原因</h3>
        <button class="text-slate-400 transition hover:text-slate-600" @click="closeRejectDialog">×</button>
      </div>

      <p class="mb-3 text-sm text-slate-500">请选择违规原因</p>
      <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <label
          v-for="option in rejectReasonOptions"
          :key="option"
          class="flex cursor-pointer items-center gap-2 rounded-xl border border-slate-200 px-3 py-2 text-sm transition hover:border-[#00a1d6]"
        >
          <input v-model="rejectDialog.reason" type="radio" :value="option" class="accent-[#00a1d6]" />
          <span>{{ option }}</span>
        </label>
      </div>

      <div class="mt-4">
        <p class="mb-1 text-sm text-slate-500">详细描述 <span class="text-rose-500">*</span></p>
        <textarea
          v-model="rejectDialog.detail"
          maxlength="400"
          rows="4"
          placeholder="请填写驳回原因详情，方便投稿用户修改后重新提交"
          class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-[#00a1d6] focus:outline-none"
        ></textarea>
        <p class="mt-1 text-right text-xs text-slate-400">{{ rejectDialog.detail.length }}/400</p>
      </div>

      <div class="mt-5 flex justify-end gap-3">
        <button class="rounded-lg border border-slate-200 px-6 py-2 text-sm text-slate-600 transition hover:bg-slate-50" @click="closeRejectDialog">
          取消
        </button>
        <button
          class="rounded-lg bg-[#00a1d6] px-6 py-2 text-sm text-white transition hover:bg-[#00b5e5] disabled:cursor-not-allowed disabled:bg-slate-300"
          :disabled="operatingVideoId === rejectDialog.videoId"
          @click="confirmReject"
        >
          提交
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { auditVideo, queryAuditVideoList } from '../../api/manager';
import AppVideoPlayer from '../common/AppVideoPlayer.vue';

const fallbackCover =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="180"><rect width="100%" height="100%" fill="%23e2e8f0"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%2364748b" font-size="14">No Cover</text></svg>';
const fallbackAvatar =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80"><circle cx="40" cy="40" r="40" fill="%23e2e8f0"/></svg>';

const filters = reactive({
  status: -1
});

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

const loading = ref(false);
const operatingVideoId = ref(null);
const records = ref([]);
const expandedVideoId = ref(null);
const previewCoverUrl = ref('');
const durationMap = ref({});

const rejectDialog = reactive({
  visible: false,
  videoId: null,
  reason: '',
  detail: ''
});

const rejectReasonOptions = [
  '色情低俗',
  '违规广告引流',
  '涉政敏感',
  '引战、网暴、不友善',
  '传播谣言',
  '涉诈诈骗',
  '引人不适',
  '涉未成年人不良信息',
  '封面党、标题党',
  '其他'
];

const totalPages = computed(() => {
  const pages = Math.ceil((pagination.total || 0) / pagination.pageSize);
  return pages > 0 ? pages : 1;
});

function resolveAvatar(url) {
  return url || fallbackAvatar;
}

function statusText(status) {
  if (status === 0) return '审核中';
  if (status === 1) return '已通过';
  if (status === 2) return '已驳回';
  return `状态 ${status}`;
}

function formatDuration(seconds) {
  if (!Number.isFinite(seconds) || seconds < 0) return '--:--';
  const total = Math.floor(seconds);
  const hours = Math.floor(total / 3600);
  const mins = Math.floor((total % 3600) / 60);
  const secs = total % 60;
  if (hours > 0) {
    return `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

function formatDurationLabel(videoId) {
  return formatDuration(durationMap.value[videoId]);
}

function formatTime(time) {
  if (!time) return '-';
  return String(time).replace('T', ' ');
}

function findDefaultSourceIndex(item) {
  const sourceList = item?.videoSourceList || [];
  if (!sourceList.length) return 0;
  const masterIndex = sourceList.findIndex((source) => {
    const resolution = String(source?.resolution || '').toLowerCase();
    const url = String(source?.playUrl || '').toLowerCase();
    return resolution.includes('自适应') || resolution.includes('master') || url.includes('/master.m3u8');
  });
  return masterIndex >= 0 ? masterIndex : 0;
}

function toggleExpand(videoId) {
  expandedVideoId.value = expandedVideoId.value === videoId ? null : videoId;
}

function openCoverPreview(url) {
  previewCoverUrl.value = url || fallbackCover;
}

function openRejectDialog(videoId) {
  rejectDialog.visible = true;
  rejectDialog.videoId = videoId;
  rejectDialog.reason = '';
  rejectDialog.detail = '';
}

function closeRejectDialog() {
  rejectDialog.visible = false;
  rejectDialog.videoId = null;
  rejectDialog.reason = '';
  rejectDialog.detail = '';
}

function buildRejectNote() {
  const reason = rejectDialog.reason.trim();
  const detail = rejectDialog.detail.trim();
  return detail ? `${reason}：${detail}` : reason;
}

async function probeDuration(videoId, url) {
  if (!url || durationMap.value[videoId] !== undefined) return;
  await new Promise((resolve) => {
    const video = document.createElement('video');
    video.preload = 'metadata';
    video.src = url;
    video.onloadedmetadata = () => {
      durationMap.value[videoId] = Number.isFinite(video.duration) ? video.duration : null;
      resolve();
    };
    video.onerror = () => {
      durationMap.value[videoId] = null;
      resolve();
    };
  });
}

async function preloadDuration(recordsList) {
  const tasks = recordsList.map((item) => {
    const firstUrl = item?.videoSourceList?.[0]?.playUrl || '';
    return probeDuration(item.videoId, firstUrl);
  });
  await Promise.all(tasks);
}

async function fetchList() {
  try {
    loading.value = true;
    const data = await queryAuditVideoList({
      status: filters.status,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    });

    records.value = Array.isArray(data?.records) ? data.records : [];
    pagination.total = Number(data?.total || 0);
    pagination.pageNum = Number(data?.pageNum || pagination.pageNum);
    pagination.pageSize = Number(data?.pageSize || pagination.pageSize);
    expandedVideoId.value = null;

    await preloadDuration(records.value);
  } catch (error) {
    ElMessage.error(error.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

function reloadFirstPage() {
  pagination.pageNum = 1;
  fetchList();
}

function changePage(page) {
  pagination.pageNum = page;
  fetchList();
}

async function submitAudit(videoId, operation, reviewNote = '') {
  try {
    operatingVideoId.value = videoId;
    await auditVideo({
      videoId,
      operation,
      reviewNote
    });
    ElMessage.success(operation === 1 ? '审核通过成功' : '驳回成功');
    await fetchList();
  } catch (error) {
    ElMessage.error(error.message || '操作失败');
  } finally {
    operatingVideoId.value = null;
  }
}

async function confirmReject() {
  if (!rejectDialog.reason.trim()) {
    ElMessage.warning('请选择驳回原因');
    return;
  }
  if (!rejectDialog.detail.trim()) {
    ElMessage.warning('请填写详细描述');
    return;
  }
  const reviewNote = buildRejectNote();
  await submitAudit(rejectDialog.videoId, 0, reviewNote);
  closeRejectDialog();
}

watch(
  () => filters.status,
  () => {
    reloadFirstPage();
  }
);

onMounted(fetchList);
</script>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-1 {
  -webkit-line-clamp: 1;
}
</style>
