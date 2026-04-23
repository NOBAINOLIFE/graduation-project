<template>
  <section class="rounded-lg bg-white p-6 shadow-sm ring-1 ring-slate-200">
    <div class="mb-5 flex flex-wrap items-center gap-3">
      <h2 class="text-lg font-semibold">举报管理</h2>
      <select v-model.number="filters.status" class="rounded border border-slate-200 px-3 py-1.5 text-sm focus:border-[#00a1d6] focus:outline-none">
        <option :value="0">待审核</option>
        <option :value="1">已通过</option>
        <option :value="2">已驳回</option>
      </select>
      <select v-model.number="filters.targetType" class="rounded border border-slate-200 px-3 py-1.5 text-sm focus:border-[#00a1d6] focus:outline-none">
        <option :value="0">全部类型</option>
        <option :value="1">用户</option>
        <option :value="2">视频</option>
      </select>
      <button
        class="rounded bg-[#00a1d6] px-4 py-1.5 text-sm text-white hover:bg-[#00b5e5]"
        :disabled="loading"
        @click="reloadFirstPage"
      >
        {{ loading ? '加载中...' : '查询' }}
      </button>
    </div>

    <div class="overflow-x-auto">
      <table class="min-w-full text-sm">
        <thead>
          <tr class="border-b border-slate-200 text-left text-slate-500">
            <th class="py-2 pr-3">举报ID</th>
            <th class="py-2 pr-3">举报人</th>
            <th class="py-2 pr-3">目标</th>
            <th class="py-2 pr-3">原因</th>
            <th class="py-2 pr-3">状态</th>
            <th class="py-2 pr-3">创建时间</th>
            <th class="py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in records" :key="item.reportId" class="border-b border-slate-100 align-top">
            <td class="py-3 pr-3">{{ item.reportId }}</td>
            <td class="py-3 pr-3">{{ item.reporterId }}</td>
            <td class="py-3 pr-3">{{ targetText(item.targetType) }}: {{ item.targetId }}</td>
            <td class="py-3 pr-3">
              <p class="font-medium text-slate-900">{{ item.reason || '-' }}</p>
              <p class="max-w-[280px] text-xs text-slate-500">{{ item.detail || '-' }}</p>
            </td>
            <td class="py-3 pr-3">{{ statusText(item.status) }}</td>
            <td class="py-3 pr-3">{{ formatTime(item.createTime) }}</td>
            <td class="py-3">
              <div class="flex min-w-[300px] flex-col gap-2">
                <input
                  v-model.trim="reviewNotes[item.reportId]"
                  class="rounded border border-slate-200 px-2 py-1 text-xs focus:border-[#00a1d6] focus:outline-none"
                  placeholder="审核备注（可选）"
                />
                <div class="flex flex-wrap gap-2">
                  <button
                    class="rounded bg-emerald-500 px-3 py-1 text-xs text-white hover:bg-emerald-600 disabled:cursor-not-allowed disabled:bg-slate-300"
                    :disabled="operatingId === item.reportId"
                    @click="submitReview(item.reportId, 1)"
                  >通过</button>
                  <button
                    class="rounded bg-rose-500 px-3 py-1 text-xs text-white hover:bg-rose-600 disabled:cursor-not-allowed disabled:bg-slate-300"
                    :disabled="operatingId === item.reportId"
                    @click="submitReview(item.reportId, 0)"
                  >驳回</button>
                  <button
                    class="rounded border border-amber-400 px-3 py-1 text-xs text-amber-600 hover:bg-amber-50 disabled:cursor-not-allowed disabled:border-slate-300 disabled:text-slate-300"
                    :disabled="operatingId === item.reportId"
                    @click="banTarget(item)"
                  >封禁目标</button>
                  <button
                    class="rounded border border-sky-400 px-3 py-1 text-xs text-sky-600 hover:bg-sky-50 disabled:cursor-not-allowed disabled:border-slate-300 disabled:text-slate-300"
                    :disabled="operatingId === item.reportId"
                    @click="unbanTarget(item)"
                  >解禁目标</button>
                </div>
              </div>
            </td>
          </tr>
          <tr v-if="!records.length && !loading">
            <td colspan="7" class="py-8 text-center text-slate-400">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="mt-4 flex items-center justify-between text-sm text-slate-500">
      <p>共 {{ pagination.total }} 条</p>
      <div class="flex items-center gap-2">
        <button
          class="rounded border border-slate-200 px-3 py-1 hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-300"
          :disabled="pagination.pageNum <= 1 || loading"
          @click="changePage(pagination.pageNum - 1)"
        >上一页</button>
        <span>第 {{ pagination.pageNum }} / {{ totalPages }} 页</span>
        <button
          class="rounded border border-slate-200 px-3 py-1 hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-300"
          :disabled="pagination.pageNum >= totalPages || loading"
          @click="changePage(pagination.pageNum + 1)"
        >下一页</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import {
  banUser,
  banVideo,
  queryReportList,
  reviewReport,
  unbanUser,
  unbanVideo
} from '../../api/manager';

const filters = reactive({
  status: 0,
  targetType: 0
});

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

const loading = ref(false);
const operatingId = ref(null);
const records = ref([]);
const reviewNotes = ref({});

const totalPages = computed(() => {
  const pages = Math.ceil((pagination.total || 0) / pagination.pageSize);
  return pages > 0 ? pages : 1;
});

function targetText(type) {
  if (type === 1) return '用户';
  if (type === 2) return '视频';
  return `类型${type}`;
}

function statusText(status) {
  if (status === 0) return '待审核';
  if (status === 1) return '已通过';
  if (status === 2) return '已驳回';
  return `状态${status}`;
}

function formatTime(time) {
  if (!time) return '-';
  return String(time).replace('T', ' ');
}

async function fetchList() {
  try {
    loading.value = true;
    const data = await queryReportList({
      status: filters.status,
      targetType: filters.targetType || null,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    });
    records.value = data?.records || [];
    pagination.total = data?.total || 0;
    pagination.pageNum = data?.pageNum || pagination.pageNum;
    pagination.pageSize = data?.pageSize || pagination.pageSize;
  } catch (error) {
    alert(error.message || '加载失败');
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

async function submitReview(reportId, operation) {
  try {
    operatingId.value = reportId;
    await reviewReport({
      reportId,
      operation,
      reviewNote: reviewNotes.value[reportId] || ''
    });
    alert(operation === 1 ? '举报审核通过' : '举报已驳回');
    await fetchList();
  } catch (error) {
    alert(error.message || '操作失败');
  } finally {
    operatingId.value = null;
  }
}

async function banTarget(item) {
  try {
    operatingId.value = item.reportId;
    if (item.targetType === 1) {
      await banUser(item.targetId);
      alert('用户封禁成功');
    } else if (item.targetType === 2) {
      await banVideo(item.targetId);
      alert('视频封禁成功');
    } else {
      throw new Error('未知目标类型');
    }
  } catch (error) {
    alert(error.message || '封禁失败');
  } finally {
    operatingId.value = null;
  }
}

async function unbanTarget(item) {
  try {
    operatingId.value = item.reportId;
    if (item.targetType === 1) {
      await unbanUser(item.targetId);
      alert('用户解禁成功');
    } else if (item.targetType === 2) {
      await unbanVideo(item.targetId);
      alert('视频解禁成功');
    } else {
      throw new Error('未知目标类型');
    }
  } catch (error) {
    alert(error.message || '解禁失败');
  } finally {
    operatingId.value = null;
  }
}

onMounted(fetchList);
</script>

