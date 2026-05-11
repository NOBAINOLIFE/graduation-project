<template>
  <div class="p-6">
    <div class="mx-auto max-w-7xl space-y-6">
      <section class="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div class="space-y-2">
            <div class="inline-flex items-center rounded-full bg-[#e6f7fd] px-3 py-1 text-xs font-semibold tracking-[0.2em] text-[#0891b2]">
              CREATOR STUDIO
            </div>
            <div>
              <h2 class="text-3xl font-bold text-slate-900">稿件管理</h2>
              <p class="mt-2 text-sm text-slate-500">查看投稿状态、内容表现，并把待发布稿件继续推进到审核流程。</p>
            </div>
          </div>

          <div class="grid min-w-full gap-3 sm:grid-cols-3 lg:min-w-[420px]">
            <article class="rounded-2xl bg-slate-50 p-4">
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">已发布</p>
              <p class="mt-3 text-3xl font-bold text-slate-900">{{ userInfo?.videoNum || 0 }}</p>
              <p class="mt-2 text-xs text-slate-500">公开可见的视频数量</p>
            </article>
            <article class="rounded-2xl bg-slate-50 p-4">
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">稿件总览</p>
              <p class="mt-3 text-3xl font-bold text-slate-900">{{ pageData.total }}</p>
              <p class="mt-2 text-xs text-slate-500">当前筛选条件下的稿件数</p>
            </article>
            <article class="rounded-2xl bg-slate-50 p-4">
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">筛选状态</p>
              <p class="mt-3 text-2xl font-bold text-slate-900">{{ currentStatusLabel }}</p>
              <p class="mt-2 text-xs text-slate-500">支持按标题和状态快速检索</p>
            </article>
          </div>
        </div>
      </section>

      <section class="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200">
        <div class="flex flex-col gap-3 xl:flex-row xl:items-center">
          <div class="flex-1">
            <el-input
              v-model="query.keyword"
              clearable
              placeholder="搜索稿件标题"
              size="large"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="w-full xl:w-48">
            <el-select
              v-model="query.status"
              clearable
              placeholder="全部状态"
              size="large"
              class="w-full"
            >
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
          <div class="flex gap-3">
            <el-button type="primary" size="large" @click="handleSearch">筛选</el-button>
            <el-button size="large" @click="handleReset">重置</el-button>
            <el-button size="large" @click="goToUpload">继续投稿</el-button>
          </div>
        </div>
      </section>

      <section v-loading="loading" class="space-y-4">
        <template v-if="pageData.records.length">
          <article
            v-for="video in pageData.records"
            :key="video.videoId"
            class="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200"
          >
            <div class="flex flex-col gap-5 p-5 lg:flex-row">
              <div class="relative h-44 w-full overflow-hidden rounded-2xl bg-slate-100 lg:w-80 lg:flex-shrink-0">
                <img
                  v-if="video.coverUrl"
                  :src="video.coverUrl"
                  :alt="video.title"
                  class="h-full w-full object-cover"
                />
                <div v-else class="flex h-full items-center justify-center bg-gradient-to-br from-slate-100 via-slate-200 to-slate-100 text-slate-400">
                  暂无封面
                </div>
                <span class="absolute bottom-3 right-3 rounded-full bg-black/60 px-2.5 py-1 text-xs font-medium text-white">
                  {{ formatDuration(video.duration) }}
                </span>
              </div>

              <div class="flex min-w-0 flex-1 flex-col">
                <div class="flex flex-col gap-3 xl:flex-row xl:items-start xl:justify-between">
                  <div class="min-w-0">
                    <div class="flex flex-wrap items-center gap-2">
                      <h3 class="text-xl font-bold text-slate-900">{{ video.title || '未命名稿件' }}</h3>
                      <span
                        class="rounded-full px-3 py-1 text-xs font-semibold"
                        :class="statusClass(video.status)"
                      >
                        {{ video.statusText }}
                      </span>
                    </div>
                    <p class="mt-2 line-clamp-2 text-sm leading-6 text-slate-500">
                      {{ video.description || '这个稿件还没有填写简介。' }}
                    </p>
                  </div>

                  <div class="flex flex-wrap gap-2">
                    <el-button
                      v-if="canSubmitAudit(video.status)"
                      type="primary"
                      :loading="actionLoadingId === video.videoId"
                      @click="submitAudit(video)"
                    >
                      {{ video.status === 8 ? '重新提交审核' : '提交审核' }}
                    </el-button>
                    <el-button
                      @click="openEditDialog(video)"
                    >
                      编辑
                    </el-button>
                    <el-button
                      v-if="video.status === 9"
                      @click="viewVideo(video.videoId)"
                    >
                      查看视频
                    </el-button>
                    <el-button
                      v-if="video.status !== 9"
                      @click="goToUpload"
                    >
                      去投稿页
                    </el-button>
                  </div>
                </div>

                <div class="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                  <div class="rounded-2xl bg-slate-50 p-4">
                    <p class="text-xs text-slate-400">分区</p>
                    <p class="mt-2 text-base font-semibold text-slate-900">{{ video.partitionName || '未选择' }}</p>
                  </div>
                  <div class="rounded-2xl bg-slate-50 p-4">
                    <p class="text-xs text-slate-400">播放 / 点赞</p>
                    <p class="mt-2 text-base font-semibold text-slate-900">{{ formatCount(video.playCount) }} / {{ formatCount(video.likeCount) }}</p>
                  </div>
                  <div class="rounded-2xl bg-slate-50 p-4">
                    <p class="text-xs text-slate-400">收藏 / 评论</p>
                    <p class="mt-2 text-base font-semibold text-slate-900">{{ formatCount(video.collectCount) }} / {{ formatCount(video.commentCount) }}</p>
                  </div>
                  <div class="rounded-2xl bg-slate-50 p-4">
                    <p class="text-xs text-slate-400">最近更新</p>
                    <p class="mt-2 text-base font-semibold text-slate-900">{{ formatDateTime(video.updateTime || video.createTime) }}</p>
                  </div>
                </div>
              </div>
            </div>
          </article>

          <div class="flex justify-center pt-2">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="query.pageNum"
              :page-size="query.pageSize"
              :total="pageData.total"
              @current-change="handlePageChange"
            />
          </div>
        </template>

        <div v-else class="rounded-2xl bg-white px-6 py-20 shadow-sm ring-1 ring-slate-200">
          <el-empty description="当前没有符合条件的稿件">
            <el-button type="primary" @click="goToUpload">去投稿</el-button>
          </el-empty>
        </div>
      </section>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑稿件" width="640px" :close-on-click-modal="false" @closed="resetEditForm">
      <div class="space-y-6">
        <!-- 封面 -->
        <div class="flex">
          <label class="w-20 shrink-0 text-sm font-medium leading-8"><span class="mr-1 text-red-500">*</span>封面</label>
          <div class="flex-1">
            <div class="flex gap-4">
              <div class="relative aspect-[4/3] w-36 overflow-hidden rounded border border-slate-200 bg-slate-100">
                <img v-if="editForm.coverUrl" :src="editForm.coverUrl" class="h-full w-full object-cover" />
                <div v-else class="flex h-full items-center justify-center text-xs text-slate-400">暂无封面</div>
              </div>
              <div class="flex flex-col justify-center">
                <input type="file" id="editCoverInput" class="hidden" accept="image/*" @change="onEditCoverChange" />
                <el-button size="small" @click="document.getElementById('editCoverInput').click()" :loading="editCoverUploading">
                  {{ editCoverUploading ? '上传中...' : '更换封面' }}
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 标题 -->
        <div class="flex items-center">
          <label class="w-20 shrink-0 text-sm font-medium"><span class="mr-1 text-red-500">*</span>标题</label>
          <div class="relative flex-1">
            <el-input v-model="editForm.title" maxlength="80" placeholder="请输入标题" show-word-limit />
          </div>
        </div>

        <!-- 分区 -->
        <div class="flex items-center">
          <label class="w-20 shrink-0 text-sm font-medium"><span class="mr-1 text-red-500">*</span>分区</label>
          <el-select v-model="editForm.partitionId" placeholder="请选择分区" class="flex-1">
            <el-option v-for="p in editPartitions" :key="p.id" :label="p.partitionName" :value="p.id" />
          </el-select>
        </div>

        <!-- 标签 -->
        <div class="flex">
          <label class="w-20 shrink-0 text-sm font-medium leading-8"><span class="mr-1 text-red-500">*</span>标签</label>
          <div class="flex-1">
            <div class="flex w-full flex-wrap items-center gap-2 rounded border border-slate-200 bg-white p-1.5 transition-all focus-within:border-[#00a1d6]">
              <span
                v-for="tag in editForm.tags"
                :key="tag"
                class="flex items-center gap-1 rounded bg-[#00a1d6] py-1 pl-2 pr-1 text-xs text-white"
              >
                {{ tag }}
                <button type="button" class="flex h-4 w-4 items-center justify-center rounded-full hover:bg-black/10" @click="removeEditTag(tag)">×</button>
              </span>
              <input
                v-model.trim="editTagDraft"
                class="min-w-[100px] flex-1 px-1 text-sm outline-none bg-transparent"
                placeholder="回车创建标签"
                @keydown.enter.prevent="addEditTag"
                @keydown.backspace="editTagDraft === '' && editForm.tags.pop()"
              />
              <span class="px-2 text-xs text-slate-400">{{ 10 - editForm.tags.length }}</span>
            </div>
          </div>
        </div>

        <!-- 简介 -->
        <div class="flex">
          <label class="w-20 shrink-0 text-sm font-medium">简介</label>
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="填写更详细的简介" class="flex-1" />
        </div>
      </div>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getCreatorVideos, submitCreatorVideoAudit, updateCreatorVideo } from '../../api/creator';
import { getUserInfo } from '../../api/user';
import { listPartitions } from '../../api/video';
import { request } from '../../api/http';

const router = useRouter();

const loading = ref(false);
const actionLoadingId = ref(null);
const userInfo = ref(null);
const pageData = ref({
  total: 0,
  records: []
});

// 编辑弹窗状态
const editDialogVisible = ref(false);
const editSaving = ref(false);
const editCoverUploading = ref(false);
const editTagDraft = ref('');
const editPartitions = ref([]);
const editingVideoId = ref(null);
const editForm = reactive({
  title: '',
  description: '',
  coverUrl: '',
  partitionId: null,
  tags: []
});

const query = reactive({
  keyword: '',
  status: null,
  pageNum: 1,
  pageSize: 9
});

const statusOptions = [
  { value: 0, label: '上传中' },
  { value: 1, label: '上传成功' },
  { value: 2, label: '等待转码' },
  { value: 3, label: '转码中' },
  { value: 4, label: '转码成功' },
  { value: 5, label: '转码失败' },
  { value: 6, label: '审核中' },
  { value: 8, label: '审核未通过' },
  { value: 9, label: '已发布' },
  { value: 11, label: '已封禁' }
];

const currentStatusLabel = computed(() => {
  const current = statusOptions.find(item => item.value === query.status);
  return current ? current.label : '全部稿件';
});

async function loadUserInfo() {
  userInfo.value = await getUserInfo();
}

async function loadVideos() {
  loading.value = true;
  try {
    pageData.value = await getCreatorVideos({ ...query });
  } catch (error) {
    ElMessage.error(error.message || '加载稿件列表失败');
  } finally {
    loading.value = false;
  }
}

async function initialize() {
  loading.value = true;
  try {
    const [info, videos] = await Promise.all([
      getUserInfo(),
      getCreatorVideos({ ...query })
    ]);
    userInfo.value = info;
    pageData.value = videos;
  } catch (error) {
    ElMessage.error(error.message || '加载稿件管理失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadVideos();
}

function handleReset() {
  query.keyword = '';
  query.status = null;
  query.pageNum = 1;
  loadVideos();
}

function handlePageChange(page) {
  query.pageNum = page;
  loadVideos();
}

function canSubmitAudit(status) {
  return status === 4 || status === 8;
}

async function submitAudit(video) {
  try {
    await ElMessageBox.confirm(
      `确定将《${video.title || '未命名稿件'}》提交到审核流程吗？`,
      '提交审核',
      {
        type: 'warning',
        confirmButtonText: '提交',
        cancelButtonText: '取消'
      }
    );
  } catch (error) {
    return;
  }

  actionLoadingId.value = video.videoId;
  try {
    await submitCreatorVideoAudit(video.videoId);
    ElMessage.success('已提交审核');
    await Promise.all([loadVideos(), loadUserInfo()]);
  } catch (error) {
    ElMessage.error(error.message || '提交审核失败');
  } finally {
    actionLoadingId.value = null;
  }
}

// ---- 编辑稿件 ----
function openEditDialog(video) {
  editingVideoId.value = video.videoId;
  editForm.title = video.title || '';
  editForm.description = video.description || '';
  editForm.coverUrl = video.coverUrl || '';
  editForm.partitionId = video.partitionId || null;
  editForm.tags = [];
  editTagDraft.value = '';
  editDialogVisible.value = true;
  if (editPartitions.value.length === 0) {
    listPartitions().then(data => {
      editPartitions.value = Array.isArray(data) ? data : [];
    }).catch(() => {});
  }
}

function resetEditForm() {
  editingVideoId.value = null;
  editForm.title = '';
  editForm.description = '';
  editForm.coverUrl = '';
  editForm.partitionId = null;
  editForm.tags = [];
  editTagDraft.value = '';
}

function addEditTag() {
  const val = editTagDraft.value.trim();
  if (val && editForm.tags.length < 10 && !editForm.tags.includes(val)) {
    editForm.tags.push(val);
    editTagDraft.value = '';
  }
}

function removeEditTag(tag) {
  editForm.tags = editForm.tags.filter(t => t !== tag);
}

function onEditCoverChange(e) {
  const file = e.target.files[0];
  if (!file) return;
  editCoverUploading.value = true;
  const formData = new FormData();
  formData.append('file', file);
  request('/graduation-project/upload/image', { method: 'POST', formData })
    .then(url => {
      editForm.coverUrl = url;
    })
    .catch(err => {
      ElMessage.error('封面上传失败：' + (err.message || '未知错误'));
    })
    .finally(() => {
      editCoverUploading.value = false;
    });
}

async function saveEdit() {
  if (!editForm.title.trim()) {
    ElMessage.warning('标题不能为空');
    return;
  }
  if (!editForm.partitionId) {
    ElMessage.warning('请选择分区');
    return;
  }
  if (editForm.tags.length === 0) {
    ElMessage.warning('至少需要添加1个标签');
    return;
  }

  editSaving.value = true;
  try {
    await updateCreatorVideo(editingVideoId.value, {
      title: editForm.title.trim(),
      description: editForm.description,
      coverUrl: editForm.coverUrl,
      partitionId: editForm.partitionId,
      tagList: [...editForm.tags]
    });
    ElMessage.success('修改已保存');
    editDialogVisible.value = false;
    await loadVideos();
  } catch (error) {
    ElMessage.error(error.message || '保存失败');
  } finally {
    editSaving.value = false;
  }
}

function goToUpload() {
  router.push('/creator/upload');
}

function viewVideo(videoId) {
  router.push(`/video/${videoId}`);
}

function statusClass(status) {
  if (status === 9) {
    return 'bg-emerald-50 text-emerald-600';
  }
  if (status === 6) {
    return 'bg-amber-50 text-amber-600';
  }
  if (status === 8 || status === 5 || status === 11) {
    return 'bg-rose-50 text-rose-600';
  }
  if (status === 4) {
    return 'bg-sky-50 text-sky-600';
  }
  return 'bg-slate-100 text-slate-600';
}

function formatCount(value) {
  const count = Number(value || 0);
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}万`;
  }
  return `${count}`;
}

function formatDuration(seconds) {
  const total = Number(seconds || 0);
  const minutes = Math.floor(total / 60);
  const remain = total % 60;
  return `${String(minutes).padStart(2, '0')}:${String(remain).padStart(2, '0')}`;
}

function formatDateTime(value) {
  if (!value) return '暂无';
  const date = new Date(value);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${month}-${day} ${hour}:${minute}`;
}

onMounted(() => {
  initialize();
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
