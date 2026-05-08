<template>
  <section class="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
    <div class="mb-5 flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-lg font-semibold text-slate-900">用户管理</h2>
        <p class="mt-1 text-sm text-slate-500">查看用户基础信息，并对异常账号进行封禁或解禁。</p>
      </div>
      <button
        class="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-300"
        :disabled="loading"
        @click="fetchList"
      >
        {{ loading ? '刷新中...' : '刷新' }}
      </button>
    </div>

    <div class="mb-5 flex flex-wrap items-center gap-3">
      <label class="relative w-full max-w-xs">
        <input
          v-model.trim="filters.keyword"
          type="text"
          class="w-full rounded-lg border border-slate-200 px-3 py-2 pr-10 text-sm text-slate-700 outline-none transition focus:border-[#00AEEC]"
          placeholder="按用户名搜索"
          @keyup.enter="reloadFirstPage"
        />
        <button
          class="absolute right-1.5 top-1/2 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-md text-slate-400 transition hover:bg-slate-100 hover:text-[#00AEEC]"
          type="button"
          @click="reloadFirstPage"
        >
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m21 21-4.35-4.35m1.85-5.15a7 7 0 1 1-14 0 7 7 0 0 1 14 0Z" />
          </svg>
        </button>
      </label>

      <select
        v-model.number="filters.status"
        class="rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-700 focus:border-[#00AEEC] focus:outline-none"
      >
        <option :value="-1">全部状态</option>
        <option :value="0">正常</option>
        <option :value="1">已封禁</option>
      </select>
    </div>

    <div class="space-y-4">
      <article
        v-for="item in records"
        :key="item.userId"
        class="overflow-hidden rounded-2xl border bg-white shadow-sm transition-shadow hover:shadow-md"
        :class="userStatusBorderClass(item.status)"
      >
        <div class="flex flex-col gap-4 p-4 lg:flex-row">
          <!-- User info -->
          <div class="min-w-0 flex-1">
            <div class="flex items-start gap-4">
              <div class="flex h-14 w-14 shrink-0 items-center justify-center overflow-hidden rounded-full bg-slate-100 text-lg font-semibold text-slate-500 ring-1 ring-slate-200">
                <img
                  v-if="item.avatarUrl"
                  :src="item.avatarUrl"
                  :alt="item.username || '用户头像'"
                  class="h-full w-full object-cover"
                  @error="handleImageError"
                />
                <span v-else>{{ getInitial(item.username) }}</span>
              </div>
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2">
                  <h3 class="truncate text-base font-semibold text-slate-900">{{ item.username || '未知用户' }}</h3>
                  <span class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1" :class="getUserStatusClass(item.status)">
                    {{ item.statusText || userStatusText(item.status) }}
                  </span>
                </div>
                <p class="mt-1 text-xs text-slate-500">UID:{{ item.userId }} · 账号：{{ item.account || '-' }}</p>
                <p class="mt-1.5 line-clamp-2 text-sm leading-6 text-slate-600">{{ item.bio || '暂无个人简介' }}</p>
              </div>
            </div>

            <!-- Stats -->
            <div class="mt-4 flex flex-wrap gap-3">
              <div class="flex min-w-[80px] flex-1 flex-col rounded-xl bg-slate-50 p-3">
                <p class="text-xs text-slate-400">投稿</p>
                <p class="mt-1 text-lg font-semibold text-slate-900">{{ formatCount(item.videoNum) }}</p>
              </div>
              <div class="flex min-w-[80px] flex-1 flex-col rounded-xl bg-slate-50 p-3">
                <p class="text-xs text-slate-400">粉丝</p>
                <p class="mt-1 text-lg font-semibold text-slate-900">{{ formatCount(item.fansNum) }}</p>
              </div>
              <div class="flex min-w-[80px] flex-1 flex-col rounded-xl bg-slate-50 p-3">
                <p class="text-xs text-slate-400">关注</p>
                <p class="mt-1 text-lg font-semibold text-slate-900">{{ formatCount(item.followNum) }}</p>
              </div>
              <div class="flex min-w-[80px] flex-1 flex-col rounded-xl bg-slate-50 p-3">
                <p class="text-xs text-slate-400">获赞</p>
                <p class="mt-1 text-lg font-semibold text-slate-900">{{ formatCount(item.likeNum) }}</p>
              </div>
              <div class="flex min-w-[80px] flex-1 flex-col rounded-xl bg-slate-50 p-3">
                <p class="text-xs text-slate-400">播放</p>
                <p class="mt-1 text-lg font-semibold text-slate-900">{{ formatCount(item.playNum) }}</p>
              </div>
            </div>

            <p class="mt-3 text-xs text-slate-400">
              注册于 {{ formatTime(item.createTime) }} · 更新于 {{ formatTime(item.updateTime) }}
            </p>
          </div>

          <!-- Actions -->
          <div class="flex shrink-0 flex-row items-center gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4 lg:flex-col lg:justify-center">
            <div class="text-center">
              <p class="text-xs font-medium text-slate-400 uppercase tracking-wide">
                {{ item.status === 1 ? '已封禁' : '正常' }}
              </p>
              <p class="mt-1 text-xs text-slate-500">
                {{ item.status === 1 ? '可解禁恢复' : '可封禁该账号' }}
              </p>
            </div>
            <button
              v-if="item.status === 1"
              class="w-full rounded-lg bg-emerald-500 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-600 active:scale-95 disabled:cursor-not-allowed disabled:bg-slate-300"
              :disabled="operatingUserId === item.userId"
              @click="unbanTarget(item)"
            >
              {{ operatingUserId === item.userId ? '解禁中...' : '解禁' }}
            </button>
            <button
              v-else
              class="w-full rounded-lg bg-[#FB7299] px-4 py-2 text-sm font-medium text-white transition hover:bg-[#FB7299]/80 active:scale-95 disabled:cursor-not-allowed disabled:bg-slate-300"
              :disabled="operatingUserId === item.userId"
              @click="banTarget(item)"
            >
              {{ operatingUserId === item.userId ? '封禁中...' : '封禁' }}
            </button>
          </div>
        </div>
      </article>

      <div v-if="loading" class="py-10 text-center text-sm text-slate-400">正在加载用户...</div>
      <div v-else-if="!records.length" class="py-12 text-center text-sm text-slate-400">暂无用户数据</div>
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
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { banUser, queryUserList, unbanUser } from '../../api/manager';

const filters = reactive({
  keyword: '',
  status: -1
});

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

const loading = ref(false);
const operatingUserId = ref(null);
const records = ref([]);

const totalPages = computed(() => {
  const pages = Math.ceil((pagination.total || 0) / pagination.pageSize);
  return pages > 0 ? pages : 1;
});

function getInitial(name) {
  return String(name || 'U').slice(0, 1).toUpperCase();
}

function userStatusText(status) {
  if (status === 0) return '正常';
  if (status === 1) return '已封禁';
  if (status === 2) return '已删除';
  return status === null || status === undefined ? '未知' : `状态${status}`;
}

function getUserStatusClass(status) {
  if (status === 0) return 'bg-emerald-50 text-emerald-700 ring-emerald-300';
  if (status === 1) return 'bg-rose-50 text-rose-700 ring-rose-300';
  if (status === 2) return 'bg-slate-100 text-slate-500 ring-slate-300';
  return 'bg-amber-50 text-amber-600 ring-amber-300';
}

function userStatusBorderClass(status) {
  if (status === 0) return 'border-l-[3px] border-l-emerald-400';
  if (status === 1) return 'border-l-[3px] border-l-rose-400';
  if (status === 2) return 'border-l-[3px] border-l-slate-300';
  return 'border-slate-200';
}

function formatCount(value) {
  const num = Number(value || 0);
  return Number.isFinite(num) ? num.toLocaleString('zh-CN') : '0';
}

function formatTime(time) {
  if (!time) return '-';
  return String(time).replace('T', ' ');
}

function handleImageError(event) {
  if (event?.target) {
    event.target.style.display = 'none';
  }
}

async function fetchList() {
  try {
    loading.value = true;
    const data = await queryUserList({
      keyword: filters.keyword,
      status: filters.status,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    });
    records.value = Array.isArray(data?.records) ? data.records : [];
    pagination.total = Number(data?.total || 0);
    pagination.pageNum = Number(data?.pageNum || pagination.pageNum);
    pagination.pageSize = Number(data?.pageSize || pagination.pageSize);
  } catch (error) {
    ElMessage.error(error.message || '加载用户失败');
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

async function banTarget(item) {
  try {
    await ElMessageBox.confirm(`确定封禁「${item.username || '该用户'}」吗？`, '封禁用户', {
      confirmButtonText: '封禁',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch (error) {
    return;
  }

  try {
    operatingUserId.value = item.userId;
    await banUser(item.userId);
    ElMessage.success('用户封禁成功');
    await fetchList();
  } catch (error) {
    ElMessage.error(error.message || '封禁失败');
  } finally {
    operatingUserId.value = null;
  }
}

async function unbanTarget(item) {
  try {
    operatingUserId.value = item.userId;
    await unbanUser(item.userId);
    ElMessage.success('用户解禁成功');
    await fetchList();
  } catch (error) {
    ElMessage.error(error.message || '解禁失败');
  } finally {
    operatingUserId.value = null;
  }
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
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
