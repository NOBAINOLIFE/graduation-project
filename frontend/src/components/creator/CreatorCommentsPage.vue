<template>
  <div class="p-6">
    <div class="mx-auto max-w-7xl space-y-6">
      <section class="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div class="space-y-2">
            <div class="inline-flex items-center rounded-full bg-[#eef2ff] px-3 py-1 text-xs font-semibold tracking-[0.2em] text-[#4f46e5]">
              INTERACTION DESK
            </div>
            <div>
              <h2 class="text-3xl font-bold text-slate-900">评论管理</h2>
              <p class="mt-2 text-sm text-slate-500">集中处理你的视频评论，快速定位热门讨论，也能及时移除不合适的内容。</p>
            </div>
          </div>

          <div class="grid min-w-full gap-3 sm:grid-cols-3 lg:min-w-[420px]">
            <article class="rounded-2xl bg-slate-50 p-4">
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">评论总数</p>
              <p class="mt-3 text-3xl font-bold text-slate-900">{{ pageData.total }}</p>
              <p class="mt-2 text-xs text-slate-500">当前筛选条件下的评论量</p>
            </article>
            <article class="rounded-2xl bg-slate-50 p-4">
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">当前页评论</p>
              <p class="mt-3 text-3xl font-bold text-slate-900">{{ pageData.records.length }}</p>
              <p class="mt-2 text-xs text-slate-500">本页可直接处理的评论</p>
            </article>
            <article class="rounded-2xl bg-slate-50 p-4">
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">排序方式</p>
              <p class="mt-3 text-2xl font-bold text-slate-900">{{ currentSortLabel }}</p>
              <p class="mt-2 text-xs text-slate-500">支持最新评论和最多点赞</p>
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
              size="large"
              placeholder="搜索评论内容"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="w-full xl:w-48">
            <el-select
              v-model="query.sortType"
              size="large"
              class="w-full"
            >
              <el-option :value="1" label="最新评论" />
              <el-option :value="2" label="最多点赞" />
            </el-select>
          </div>
          <div class="flex gap-3">
            <el-button type="primary" size="large" @click="handleSearch">筛选</el-button>
            <el-button size="large" @click="handleReset">重置</el-button>
          </div>
        </div>
      </section>

      <section v-loading="loading" class="space-y-4">
        <template v-if="pageData.records.length">
          <article
            v-for="comment in pageData.records"
            :key="comment.commentId"
            class="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200"
          >
            <div class="flex flex-col gap-5 xl:flex-row">
              <div class="min-w-0 flex-1">
                <div class="flex items-start gap-4">
                  <div class="h-12 w-12 flex-shrink-0 overflow-hidden rounded-full bg-slate-100">
                    <img
                      v-if="comment.avatarUrl"
                      :src="comment.avatarUrl"
                      :alt="comment.username"
                      class="h-full w-full object-cover"
                    />
                    <div v-else class="flex h-full w-full items-center justify-center text-sm font-bold text-slate-400">
                      {{ getInitial(comment.username) }}
                    </div>
                  </div>

                  <div class="min-w-0 flex-1">
                    <div class="flex flex-wrap items-center gap-2">
                      <button class="text-left text-base font-semibold text-slate-900 hover:text-[#00a1d6]" @click="goToUserProfile(comment.userId)">
                        {{ comment.username }}
                      </button>
                      <span
                        class="rounded-full px-2.5 py-1 text-xs font-semibold"
                        :class="comment.isRootComment ? 'bg-sky-50 text-sky-600' : 'bg-violet-50 text-violet-600'"
                      >
                        {{ comment.isRootComment ? '主评论' : '回复' }}
                      </span>
                      <span class="text-xs text-slate-400">{{ formatDateTime(comment.createTime) }}</span>
                    </div>

                    <p v-if="comment.replyUsername && !comment.isRootComment" class="mt-2 text-xs text-slate-400">
                      回复 @{{ comment.replyUsername }}
                    </p>
                    <p class="mt-2 whitespace-pre-wrap text-sm leading-7 text-slate-700">
                      {{ comment.content }}
                    </p>

                    <div class="mt-4 flex flex-wrap items-center gap-4 text-xs text-slate-400">
                      <span>点赞 {{ formatCount(comment.likeCount) }}</span>
                      <span>回复 {{ formatCount(comment.replyCount) }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="flex w-full flex-col gap-3 rounded-2xl bg-slate-50 p-4 xl:w-80 xl:flex-shrink-0">
                <div class="flex gap-3">
                  <div class="h-20 w-32 flex-shrink-0 overflow-hidden rounded-xl bg-slate-100">
                    <img
                      v-if="comment.videoCoverUrl"
                      :src="comment.videoCoverUrl"
                      :alt="comment.videoTitle"
                      class="h-full w-full object-cover"
                    />
                    <div v-else class="flex h-full w-full items-center justify-center text-xs text-slate-400">
                      暂无封面
                    </div>
                  </div>
                  <div class="min-w-0 flex-1">
                    <p class="text-xs uppercase tracking-[0.18em] text-slate-400">所属视频</p>
                    <button class="mt-2 line-clamp-2 text-left text-sm font-semibold leading-6 text-slate-900 hover:text-[#00a1d6]" @click="viewVideo(comment.videoId)">
                      {{ comment.videoTitle }}
                    </button>
                  </div>
                </div>

                <div class="flex gap-3">
                  <el-button class="flex-1" @click="viewVideo(comment.videoId)">查看视频</el-button>
                  <el-button
                    class="flex-1"
                    type="danger"
                    plain
                    :loading="deletingCommentId === comment.commentId"
                    @click="removeComment(comment)"
                  >
                    删除评论
                  </el-button>
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
          <el-empty :description="query.keyword ? '没有匹配到评论' : '暂时还没有评论'">
            <el-button v-if="query.keyword" @click="handleReset">清空筛选</el-button>
          </el-empty>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { deleteCreatorComment, getCreatorComments } from '../../api/creator';

const router = useRouter();

const loading = ref(false);
const deletingCommentId = ref(null);
const pageData = ref({
  total: 0,
  records: []
});

const query = reactive({
  keyword: '',
  sortType: 1,
  pageNum: 1,
  pageSize: 8
});

const currentSortLabel = computed(() => (query.sortType === 2 ? '最多点赞' : '最新评论'));

async function loadComments() {
  loading.value = true;
  try {
    pageData.value = await getCreatorComments({ ...query });
  } catch (error) {
    ElMessage.error(error.message || '加载评论列表失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadComments();
}

function handleReset() {
  query.keyword = '';
  query.sortType = 1;
  query.pageNum = 1;
  loadComments();
}

function handlePageChange(page) {
  query.pageNum = page;
  loadComments();
}

async function removeComment(comment) {
  try {
    await ElMessageBox.confirm(
      `确定删除这条${comment.isRootComment ? '主评论' : '回复'}吗？`,
      '删除评论',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    );
  } catch (error) {
    return;
  }

  deletingCommentId.value = comment.commentId;
  try {
    await deleteCreatorComment(comment.commentId);
    ElMessage.success('评论已删除');
    await loadComments();
  } catch (error) {
    ElMessage.error(error.message || '删除评论失败');
  } finally {
    deletingCommentId.value = null;
  }
}

function viewVideo(videoId) {
  router.push(`/video/${videoId}`);
}

function goToUserProfile(userId) {
  router.push({
    name: 'user-profile',
    params: { userId }
  });
}

function getInitial(username) {
  return String(username || 'U').charAt(0).toUpperCase();
}

function formatCount(value) {
  const count = Number(value || 0);
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}万`;
  }
  return `${count}`;
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
  loadComments();
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
