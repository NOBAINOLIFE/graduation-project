<template>
  <section class="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
    <div class="mb-5 flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-lg font-semibold text-slate-900">视频标签管理</h2>
        <p class="mt-1 text-sm text-slate-500">查看标签热度、关联视频数量，并支持删除失效标签。</p>
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
          class="w-full rounded-lg border border-slate-200 px-3 py-2 pr-10 text-sm text-slate-700 outline-none transition focus:border-[#00a1d6]"
          placeholder="按标签名搜索"
          @keyup.enter="reloadFirstPage"
        />
        <button
          class="absolute right-1.5 top-1/2 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-md text-slate-400 transition hover:bg-slate-100 hover:text-[#00a1d6]"
          type="button"
          @click="reloadFirstPage"
        >
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m21 21-4.35-4.35m1.85-5.15a7 7 0 1 1-14 0 7 7 0 0 1 14 0Z" />
          </svg>
        </button>
      </label>
      <span class="text-xs text-slate-400">删除标签后，会同步清理对应视频上的该标签展示。</span>
    </div>

    <div class="space-y-4">
      <article
        v-for="item in records"
        :key="item.tagId"
        class="grid gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm xl:grid-cols-[minmax(0,1fr)_220px]"
      >
        <div class="min-w-0">
          <div class="flex flex-wrap items-center gap-3">
            <span class="rounded-full bg-sky-50 px-3 py-1 text-sm font-medium text-sky-600">
              #{{ item.tagName || '未命名标签' }}
            </span>
            <span class="rounded-full bg-slate-100 px-2.5 py-1 text-xs text-slate-500">
              标签 ID：{{ item.tagId }}
            </span>
          </div>

          <div class="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <div class="rounded-xl bg-slate-50 p-3">
              <p class="text-xs text-slate-400">热度</p>
              <p class="mt-1 text-lg font-semibold text-slate-900">{{ formatCount(item.hot) }}</p>
            </div>
            <div class="rounded-xl bg-slate-50 p-3">
              <p class="text-xs text-slate-400">关联视频</p>
              <p class="mt-1 text-lg font-semibold text-slate-900">{{ formatCount(item.relatedVideoCount) }}</p>
            </div>
            <div class="rounded-xl bg-slate-50 p-3">
              <p class="text-xs text-slate-400">创建时间</p>
              <p class="mt-1 text-sm font-medium text-slate-900">{{ formatTime(item.createTime) }}</p>
            </div>
            <div class="rounded-xl bg-slate-50 p-3">
              <p class="text-xs text-slate-400">更新时间</p>
              <p class="mt-1 text-sm font-medium text-slate-900">{{ formatTime(item.updateTime) }}</p>
            </div>
          </div>
        </div>

        <div class="flex flex-col justify-between gap-3 rounded-2xl bg-slate-50 p-4">
          <div>
            <p class="text-xs font-medium uppercase tracking-[0.2em] text-slate-400">操作提示</p>
            <p class="mt-2 text-sm leading-6 text-slate-600">
              删除后会移除所有视频与该标签的关联关系，已发布视频的搜索索引也会同步刷新。
            </p>
          </div>
          <div class="flex flex-wrap gap-2">
            <button
              class="rounded-lg bg-rose-500 px-4 py-2 text-sm text-white transition hover:bg-rose-600 disabled:cursor-not-allowed disabled:bg-slate-300"
              :disabled="operatingTagId === item.tagId"
              @click="removeTag(item)"
            >
              {{ operatingTagId === item.tagId ? '删除中...' : '删除标签' }}
            </button>
          </div>
        </div>
      </article>

      <div v-if="loading" class="py-10 text-center text-sm text-slate-400">正在加载标签...</div>
      <div v-else-if="!records.length" class="py-12 text-center text-sm text-slate-400">暂无标签数据</div>
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
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { deleteVideoTag, queryVideoTagList } from '../../api/manager';

const filters = reactive({
  keyword: ''
});

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

const loading = ref(false);
const operatingTagId = ref(null);
const records = ref([]);

const totalPages = computed(() => {
  const pages = Math.ceil((pagination.total || 0) / pagination.pageSize);
  return pages > 0 ? pages : 1;
});

function formatCount(value) {
  const num = Number(value || 0);
  return Number.isFinite(num) ? num.toLocaleString('zh-CN') : '0';
}

function formatTime(time) {
  if (!time) return '-';
  return String(time).replace('T', ' ');
}

async function fetchList() {
  try {
    loading.value = true;
    const data = await queryVideoTagList({
      keyword: filters.keyword,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    });
    records.value = Array.isArray(data?.records) ? data.records : [];
    pagination.total = Number(data?.total || 0);
    pagination.pageNum = Number(data?.pageNum || pagination.pageNum);
    pagination.pageSize = Number(data?.pageSize || pagination.pageSize);
  } catch (error) {
    ElMessage.error(error.message || '加载标签失败');
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

async function removeTag(item) {
  try {
    await ElMessageBox.confirm(
      `确定删除标签「${item.tagName || '未命名标签'}」吗？该操作会清理所有关联视频上的此标签。`,
      '删除标签',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
  } catch (error) {
    return;
  }

  try {
    operatingTagId.value = item.tagId;
    await deleteVideoTag(item.tagId);
    ElMessage.success('标签删除成功');
    if (records.value.length === 1 && pagination.pageNum > 1) {
      pagination.pageNum -= 1;
    }
    await fetchList();
  } catch (error) {
    ElMessage.error(error.message || '删除标签失败');
  } finally {
    operatingTagId.value = null;
  }
}

onMounted(fetchList);
</script>
