<template>
  <section class="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
    <div class="mb-5 border-b border-slate-200">
      <nav class="-mb-px flex gap-6">
        <button
          v-for="tab in tabs"
          :key="tab.type"
          class="border-b-2 px-4 py-3 text-sm font-medium transition"
          :class="activeTab === tab.type ? 'border-[#00AEEC] text-[#00AEEC]' : 'border-transparent text-slate-500 hover:border-slate-300 hover:text-slate-700'"
          @click="switchTab(tab.type)"
        >
          {{ tab.label }}
          <span class="ml-1 rounded-full bg-slate-100 px-2 py-0.5 text-xs">{{ tabCounts[tab.type] || 0 }}</span>
        </button>
      </nav>
    </div>

    <div class="mb-5 flex flex-wrap items-center gap-3">
      <select
        v-model.number="filters.status"
        class="rounded-lg border border-slate-200 px-3 py-1.5 text-sm text-slate-700 focus:border-[#00AEEC] focus:outline-none"
      >
        <option :value="null">全部状态</option>
        <option :value="0">待审核</option>
        <option :value="1">已通过</option>
        <option :value="2">已驳回</option>
      </select>
      <span class="text-xs text-slate-400">切换类型或状态后会自动刷新列表</span>
    </div>

    <div class="space-y-4">
      <article
        v-for="item in records"
        :key="item.reportId"
        class="overflow-hidden rounded-2xl border bg-white shadow-sm transition-shadow hover:shadow-md"
        :class="reportStatusBorderClass(item.status)"
      >
        <!-- Header -->
        <div class="flex flex-wrap items-center gap-2 border-b border-slate-100 bg-slate-50/60 px-4 py-2.5 text-xs">
          <span class="rounded-full bg-white px-2.5 py-0.5 font-medium text-slate-700 ring-1 ring-slate-200">
            {{ getReportTypeLabel(item.reportType) }}
          </span>
          <span class="rounded-full px-2 py-0.5 font-medium" :class="getStatusClass(item.status)">
            {{ statusText(item.status) }}
          </span>
          <span class="text-slate-400">#{{ item.reportId }}</span>
          <span class="hidden text-slate-400 sm:inline">创建：{{ formatTime(item.createTime) }}</span>
          <span class="hidden text-slate-400 sm:inline">更新：{{ formatTime(item.updateTime) }}</span>
        </div>

        <div class="grid gap-4 p-4 xl:grid-cols-[260px_minmax(0,1fr)_200px]">
          <!-- Reporter -->
          <div class="rounded-xl border border-slate-200 bg-slate-50/60 p-4">
            <p class="mb-3 text-xs font-medium uppercase tracking-wider text-slate-400">举报人</p>
            <div class="flex items-center gap-3">
              <img
                v-if="item.reporterAvatar"
                :src="item.reporterAvatar"
                :alt="item.reporterName || '举报人头像'"
                class="h-10 w-10 rounded-full object-cover ring-1 ring-slate-200"
              />
              <div
                v-else
                class="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-[#00AEEC] to-[#0095CC] text-sm font-medium text-white"
              >
                {{ getInitial(item.reporterName, 'U') }}
              </div>
              <div class="min-w-0">
                <p class="truncate text-sm font-medium text-slate-900">{{ item.reporterName || '未知用户' }}</p>
                <p class="text-xs text-slate-400">ID:{{ item.reporterId || '-' }}</p>
              </div>
            </div>
            <p class="mt-2 text-xs leading-5 text-slate-500">{{ item.reporterBio || '暂无简介' }}</p>

            <div class="mt-3 space-y-1.5 border-t border-slate-200 pt-3 text-xs">
              <p class="font-medium text-slate-700">{{ item.reason || '-' }}</p>
              <p class="leading-5 text-slate-500">{{ item.detail || '未填写详细说明' }}</p>
            </div>
          </div>

          <!-- Target info -->
          <div class="space-y-4">
            <!-- User report target -->
            <div v-if="item.reportType === 1" class="rounded-xl border border-slate-200 p-4">
              <p class="mb-3 text-xs font-medium uppercase tracking-wider text-slate-400">被举报用户</p>
              <div class="flex items-start gap-4">
                <img
                  v-if="item.reportInfo?.avatarUrl"
                  :src="item.reportInfo.avatarUrl"
                  :alt="item.reportInfo?.username || '用户头像'"
                  class="h-14 w-14 rounded-full object-cover ring-1 ring-slate-200"
                />
                <div
                  v-else
                  class="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-slate-200 text-base font-medium text-slate-500"
                >
                  {{ getInitial(item.reportInfo?.username, 'U') }}
                </div>
                <div class="min-w-0 flex-1">
                  <div class="flex flex-wrap items-center gap-2">
                    <p class="text-base font-semibold text-slate-900">{{ item.reportInfo?.username || '未知用户' }}</p>
                    <span class="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600 ring-1 ring-slate-200">
                      {{ userStatusText(item.reportInfo?.status) }}
                    </span>
                  </div>
                  <p class="mt-1 text-xs text-slate-400">UID:{{ item.reportInfo?.userId || '-' }}</p>
                  <p class="mt-1.5 text-sm leading-6 text-slate-600">{{ item.reportInfo?.bio || '暂无个人简介' }}</p>
                </div>
              </div>

              <div class="mt-4 flex flex-wrap gap-2">
                <div class="flex min-w-[60px] flex-1 flex-col rounded-lg bg-slate-100 p-2.5">
                  <p class="text-xs text-slate-400">投稿</p>
                  <p class="mt-0.5 text-base font-semibold text-slate-900">{{ formatCount(item.reportInfo?.videoNum) }}</p>
                </div>
                <div class="flex min-w-[60px] flex-1 flex-col rounded-lg bg-slate-100 p-2.5">
                  <p class="text-xs text-slate-400">粉丝</p>
                  <p class="mt-0.5 text-base font-semibold text-slate-900">{{ formatCount(item.reportInfo?.fansNum) }}</p>
                </div>
                <div class="flex min-w-[60px] flex-1 flex-col rounded-lg bg-slate-100 p-2.5">
                  <p class="text-xs text-slate-400">获赞</p>
                  <p class="mt-0.5 text-base font-semibold text-slate-900">{{ formatCount(item.reportInfo?.likeNum) }}</p>
                </div>
                <div class="flex min-w-[60px] flex-1 flex-col rounded-lg bg-slate-100 p-2.5">
                  <p class="text-xs text-slate-400">播放</p>
                  <p class="mt-0.5 text-base font-semibold text-slate-900">{{ formatCount(item.reportInfo?.playNum) }}</p>
                </div>
              </div>

              <p class="mt-3 text-xs text-slate-400">注册时间：{{ formatTime(item.reportInfo?.createTime) }}</p>
            </div>

            <!-- Video report target -->
            <div v-else-if="item.reportType === 2" class="space-y-4">
              <div class="rounded-xl border border-slate-200 p-4">
                <p class="mb-3 text-xs font-medium uppercase tracking-wider text-slate-400">被举报视频</p>
                <div class="flex flex-col gap-4 lg:flex-row">
                  <img
                    v-if="item.reportInfo?.coverUrl"
                    :src="item.reportInfo.coverUrl"
                    :alt="item.reportInfo?.title || '视频封面'"
                    class="h-36 w-full shrink-0 rounded-xl object-cover ring-1 ring-slate-200 lg:w-60"
                  />
                  <div class="min-w-0 flex-1">
                    <p class="text-base font-semibold text-slate-900">{{ item.reportInfo?.title || '未知视频' }}</p>
                    <p class="mt-1 text-xs text-slate-400">视频 ID：{{ item.reportInfo?.videoId || '-' }}</p>
                    <p class="mt-2 text-sm leading-6 text-slate-600">{{ item.reportInfo?.description || '暂无视频简介' }}</p>

                    <div class="mt-3 flex flex-wrap gap-1.5">
                      <span class="rounded-full bg-slate-100 px-2.5 py-1 text-xs text-slate-600">
                        分区：{{ item.reportInfo?.partitionName || '-' }}
                      </span>
                      <span
                        v-for="tag in item.reportInfo?.tagList || []"
                        :key="tag"
                        class="rounded-full bg-[#00AEEC]/10 px-2.5 py-1 text-xs text-[#00AEEC]"
                      >
                        {{ tag }}
                      </span>
                    </div>
                  </div>
                </div>

                <div class="mt-4 flex flex-wrap gap-2">
                  <div class="flex min-w-[55px] flex-1 flex-col rounded-lg bg-slate-100 p-2.5">
                    <p class="text-xs text-slate-400">播放</p>
                    <p class="mt-0.5 text-base font-semibold text-slate-900">{{ formatCount(item.reportInfo?.playCount) }}</p>
                  </div>
                  <div class="flex min-w-[55px] flex-1 flex-col rounded-lg bg-slate-100 p-2.5">
                    <p class="text-xs text-slate-400">点赞</p>
                    <p class="mt-0.5 text-base font-semibold text-slate-900">{{ formatCount(item.reportInfo?.likeCount) }}</p>
                  </div>
                  <div class="flex min-w-[55px] flex-1 flex-col rounded-lg bg-slate-100 p-2.5">
                    <p class="text-xs text-slate-400">收藏</p>
                    <p class="mt-0.5 text-base font-semibold text-slate-900">{{ formatCount(item.reportInfo?.collectCount) }}</p>
                  </div>
                  <div class="flex min-w-[55px] flex-1 flex-col rounded-lg bg-slate-100 p-2.5">
                    <p class="text-xs text-slate-400">评论</p>
                    <p class="mt-0.5 text-base font-semibold text-slate-900">{{ formatCount(item.reportInfo?.commentCount) }}</p>
                  </div>
                  <div class="flex min-w-[55px] flex-1 flex-col rounded-lg bg-slate-100 p-2.5">
                    <p class="text-xs text-slate-400">分享</p>
                    <p class="mt-0.5 text-base font-semibold text-slate-900">{{ formatCount(item.reportInfo?.shareCount) }}</p>
                  </div>
                </div>

                <p class="mt-3 text-xs text-slate-400">投稿时间：{{ formatTime(item.reportInfo?.createTime) }}</p>
              </div>

              <div class="rounded-xl border border-slate-200 p-4">
                <p class="mb-2 text-xs font-medium uppercase tracking-wider text-slate-400">视频作者</p>
                <div class="flex items-center gap-3">
                  <img
                    v-if="item.reportInfo?.avatarUrl"
                    :src="item.reportInfo.avatarUrl"
                    :alt="item.reportInfo?.username || '作者头像'"
                    class="h-9 w-9 rounded-full object-cover ring-1 ring-slate-200"
                  />
                  <div v-else class="flex h-9 w-9 items-center justify-center rounded-full bg-slate-200 text-xs font-medium text-slate-500">
                    {{ getInitial(item.reportInfo?.username, 'U') }}
                  </div>
                  <div>
                    <div class="flex flex-wrap items-center gap-2">
                      <p class="text-sm font-medium text-slate-900">{{ item.reportInfo?.username || '未知用户' }}</p>
                      <span class="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500 ring-1 ring-slate-200">
                        {{ userStatusText(item.reportInfo?.status) }}
                      </span>
                    </div>
                    <p class="text-xs text-slate-400">UID:{{ item.reportInfo?.userId || '-' }}</p>
                  </div>
                </div>
                <p class="mt-2 text-xs leading-5 text-slate-500">{{ item.reportInfo?.bio || '暂无简介' }}</p>
              </div>

              <div class="rounded-xl border border-slate-200 p-4">
                <p class="mb-2 text-xs font-medium uppercase tracking-wider text-slate-400">视频源</p>
                <div v-if="item.reportInfo?.sourceList?.length" class="space-y-2">
                  <div
                    v-for="source in item.reportInfo.sourceList"
                    :key="`${source.resolution}-${source.playUrl}`"
                    class="rounded-lg bg-slate-50 px-3 py-2"
                  >
                    <div class="flex items-center justify-between gap-2">
                      <p class="text-sm font-medium text-slate-700">{{ source.resolution || '未知清晰度' }}</p>
                      <a
                        v-if="source.playUrl"
                        :href="source.playUrl"
                        target="_blank"
                        rel="noreferrer"
                        class="shrink-0 rounded-md bg-[#00AEEC]/10 px-2 py-0.5 text-xs text-[#00AEEC] transition hover:bg-[#00AEEC]/20"
                      >
                        播放
                      </a>
                      <span v-else class="text-xs text-slate-400">无地址</span>
                    </div>
                  </div>
                </div>
                <p v-else class="text-sm text-slate-400">暂无可用视频源</p>
              </div>
            </div>

            <!-- Comment report target -->
            <div v-else-if="item.reportType === 3" class="space-y-4">
              <div class="rounded-xl border border-slate-200 p-4">
                <p class="mb-2 text-xs font-medium uppercase tracking-wider text-slate-400">评论作者</p>
                <div class="flex items-center gap-3">
                  <img
                    v-if="item.reportInfo?.avatarUrl"
                    :src="item.reportInfo.avatarUrl"
                    :alt="item.reportInfo?.username || '评论作者头像'"
                    class="h-9 w-9 rounded-full object-cover ring-1 ring-slate-200"
                  />
                  <div v-else class="flex h-9 w-9 items-center justify-center rounded-full bg-slate-200 text-xs font-medium text-slate-500">
                    {{ getInitial(item.reportInfo?.username, 'U') }}
                  </div>
                  <div>
                    <div class="flex flex-wrap items-center gap-2">
                      <p class="text-sm font-medium text-slate-900">{{ item.reportInfo?.username || '未知用户' }}</p>
                      <span class="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500 ring-1 ring-slate-200">
                        {{ userStatusText(item.reportInfo?.status) }}
                      </span>
                    </div>
                    <p class="text-xs text-slate-400">UID:{{ item.reportInfo?.userId || '-' }}</p>
                  </div>
                </div>
                <p class="mt-2 text-xs leading-5 text-slate-500">{{ item.reportInfo?.bio || '暂无简介' }}</p>
              </div>

              <div class="rounded-xl border border-slate-200 p-4">
                <p class="mb-2 text-xs font-medium uppercase tracking-wider text-slate-400">被举报评论</p>
                <div class="rounded-xl bg-slate-50 p-4">
                  <div class="flex flex-wrap items-center gap-2 text-xs text-slate-400">
                    <span>评论 ID：{{ item.reportInfo?.commentId || '-' }}</span>
                    <span>发布于 {{ formatTime(item.reportInfo?.createTime) }}</span>
                    <span class="rounded-full bg-white px-2 py-0.5 ring-1 ring-slate-200">
                      {{ item.reportInfo?.isRootComment ? '根评论' : '回复' }}
                    </span>
                  </div>
                  <p class="mt-2 whitespace-pre-wrap break-words text-sm leading-6 text-slate-700">
                    {{ item.reportInfo?.content || '评论内容为空' }}
                  </p>
                  <div v-if="item.reportInfo?.rootCommentContent" class="mt-3 rounded-lg border border-dashed border-slate-300 bg-white px-3 py-2">
                    <p class="text-xs text-slate-400">引用的根评论</p>
                    <p class="mt-1 text-sm leading-6 text-slate-600">{{ item.reportInfo.rootCommentContent }}</p>
                  </div>
                </div>
              </div>

              <div class="rounded-xl border border-slate-200 p-4">
                <p class="mb-2 text-xs font-medium uppercase tracking-wider text-slate-400">关联视频</p>
                <div class="flex flex-col gap-3 lg:flex-row">
                  <img
                    v-if="item.reportInfo?.coverUrl"
                    :src="item.reportInfo.coverUrl"
                    :alt="item.reportInfo?.title || '关联视频封面'"
                    class="h-28 w-full shrink-0 rounded-xl object-cover ring-1 ring-slate-200 lg:w-52"
                  />
                  <div class="min-w-0 flex-1">
                    <p class="text-sm font-semibold text-slate-900">{{ item.reportInfo?.title || '未知视频' }}</p>
                    <p class="mt-1 text-xs text-slate-400">视频 ID：{{ item.reportInfo?.videoId || '-' }}</p>
                    <p class="mt-1.5 text-xs leading-5 text-slate-500">{{ item.reportInfo?.description || '暂无视频简介' }}</p>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="rounded-xl border border-dashed border-slate-200 p-4 text-center text-sm text-slate-400">
              暂无可展示的举报目标信息
            </div>
          </div>

          <!-- Actions -->
          <div class="flex flex-col gap-3">
            <!-- Review actions -->
            <div v-if="item.status === 0" class="rounded-xl border border-slate-200 p-4">
              <p class="mb-3 text-xs font-medium uppercase tracking-wider text-slate-400">审核</p>
              <div class="flex flex-col gap-2">
                <button
                  class="rounded-lg bg-emerald-500 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-600 active:scale-95 disabled:cursor-not-allowed disabled:bg-slate-300"
                  :disabled="operatingId === item.reportId"
                  @click="openReviewDialog(item, 1)"
                >
                  通过举报
                </button>
                <button
                  class="rounded-lg bg-[#FB7299] px-4 py-2 text-sm font-medium text-white transition hover:bg-[#FB7299]/80 active:scale-95 disabled:cursor-not-allowed disabled:bg-slate-300"
                  :disabled="operatingId === item.reportId"
                  @click="openReviewDialog(item, 0)"
                >
                  驳回举报
                </button>
              </div>
            </div>

            <!-- Review result -->
            <div v-else class="rounded-xl border border-slate-200 p-4">
              <p class="mb-2 text-xs font-medium uppercase tracking-wider text-slate-400">审核结果</p>
              <div class="flex items-center gap-2">
                <svg v-if="item.status === 1" class="h-4 w-4 shrink-0 text-emerald-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                </svg>
                <svg v-else class="h-4 w-4 shrink-0 text-rose-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z"/>
                </svg>
                <span class="text-sm font-medium text-slate-700">{{ item.status === 1 ? '已通过' : '已驳回' }}</span>
              </div>
              <p class="mt-1 text-xs text-slate-400">{{ item.reviewerName || '未知管理员' }}</p>
              <p class="text-xs text-slate-400">{{ formatTime(item.updateTime) }}</p>
              <p class="mt-2 text-xs leading-5 text-slate-600">{{ item.reviewNote || '未填写审核备注' }}</p>
            </div>

            <!-- Manual actions (only when report is pending) -->
            <div v-if="item.status === 0" class="rounded-xl border border-slate-200 p-4">
              <p class="mb-2 text-xs font-medium uppercase tracking-wider text-slate-400">手动处置</p>
              <div class="flex flex-col gap-2">
                <template v-if="item.reportType === 1 || item.reportType === 2">
                  <button
                    class="rounded-lg border border-amber-400 px-3 py-2 text-xs font-medium text-amber-600 transition hover:bg-amber-50 active:scale-95 disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-300"
                    :disabled="operatingId === item.reportId"
                    @click="banTarget(item)"
                  >
                    {{ item.reportType === 1 ? '封禁用户' : '封禁视频' }}
                  </button>
                  <button
                    class="rounded-lg border border-sky-400 px-3 py-2 text-xs font-medium text-sky-600 transition hover:bg-sky-50 active:scale-95 disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-300"
                    :disabled="operatingId === item.reportId"
                    @click="unbanTarget(item)"
                  >
                    {{ item.reportType === 1 ? '解禁用户' : '解禁视频' }}
                  </button>
                </template>
                <template v-else-if="item.reportType === 3">
                  <button
                    class="rounded-lg border border-rose-400 px-3 py-2 text-xs font-medium text-rose-600 transition hover:bg-rose-50 active:scale-95 disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-300"
                    :disabled="operatingId === item.reportId"
                    @click="deleteTargetComment(item)"
                  >
                    删除评论
                  </button>
                </template>
              </div>
            </div>
          </div>
        </div>
      </article>

      <div v-if="!records.length && !loading" class="py-12 text-center text-slate-400">
        <svg class="mx-auto mb-3 h-12 w-12 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        <p>暂无{{ getCurrentTabLabel() }}数据</p>
      </div>
    </div>

    <div class="mt-5 flex items-center justify-between text-sm text-slate-500">
      <p>共 {{ pagination.total }} 条</p>
      <div class="flex items-center gap-2">
        <button
          class="rounded-lg border border-slate-200 px-3 py-1.5 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-300"
          :disabled="pagination.pageNum <= 1 || loading"
          @click="changePage(pagination.pageNum - 1)"
        >
          上一页
        </button>
        <span>第 {{ pagination.pageNum }} / {{ totalPages }} 页</span>
        <button
          class="rounded-lg border border-slate-200 px-3 py-1.5 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-300"
          :disabled="pagination.pageNum >= totalPages || loading"
          @click="changePage(pagination.pageNum + 1)"
        >
          下一页
        </button>
      </div>
    </div>
  </section>

  <!-- Review dialog -->
  <div
    v-if="reviewDialog.visible"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4"
    @click.self="closeReviewDialog"
  >
    <div class="w-full max-w-3xl rounded-2xl bg-white p-6 shadow-xl">
      <div class="mb-4 flex items-center justify-between">
        <h3 class="text-xl font-semibold text-slate-900">
          {{ reviewDialog.operation === 1 ? '通过举报' : '驳回举报' }}
        </h3>
        <button class="text-slate-400 transition hover:text-slate-600" @click="closeReviewDialog">&times;</button>
      </div>

      <div class="mb-4 rounded-xl bg-slate-50 p-4">
        <p class="text-sm text-slate-700">
          <span class="text-slate-400">举报人：</span>{{ reviewDialog.currentItem?.reporterName || '-' }}
        </p>
        <p class="mt-2 text-sm text-slate-700">
          <span class="text-slate-400">举报目标：</span>{{ buildReviewTargetText(reviewDialog.currentItem) }}
        </p>
        <p class="mt-2 text-sm text-slate-700">
          <span class="text-slate-400">举报原因：</span>{{ reviewDialog.currentItem?.reason || '-' }}
        </p>
        <p class="mt-2 whitespace-pre-wrap break-words text-sm leading-6 text-slate-700">
          <span class="text-slate-400">举报详情：</span>{{ reviewDialog.currentItem?.detail || '-' }}
        </p>
      </div>

      <p class="mb-2 text-sm text-slate-500">审核备注（可选）</p>
      <textarea
        v-model="reviewDialog.note"
        maxlength="200"
        rows="3"
        placeholder="请输入审核备注，方便后续追溯"
        class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-[#00AEEC] focus:outline-none"
      ></textarea>
      <p class="mt-1 text-right text-xs text-slate-400">{{ reviewDialog.note.length }}/200</p>

      <div class="mt-5 flex justify-end gap-3">
        <button
          class="rounded-lg border border-slate-200 px-6 py-2 text-sm text-slate-600 transition hover:bg-slate-50"
          @click="closeReviewDialog"
        >
          取消
        </button>
        <button
          class="rounded-lg px-6 py-2 text-sm font-medium text-white transition disabled:cursor-not-allowed disabled:bg-slate-300"
          :class="reviewDialog.operation === 1 ? 'bg-emerald-500 hover:bg-emerald-600' : 'bg-[#FB7299] hover:bg-[#FB7299]/80'"
          :disabled="operatingId === reviewDialog.currentItem?.reportId"
          @click="confirmReview"
        >
          {{ operatingId === reviewDialog.currentItem?.reportId ? '提交中...' : '确定' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  banUser,
  banVideo,
  deleteComment,
  queryReportList,
  reviewReport,
  unbanUser,
  unbanVideo
} from '../../api/manager';

const tabs = [
  { type: 2, label: '视频举报' },
  { type: 1, label: '用户举报' },
  { type: 3, label: '评论举报' }
];

const activeTab = ref(2);

const filters = reactive({
  status: 0
});

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

const loading = ref(false);
const operatingId = ref(null);
const records = ref([]);
const tabCounts = reactive({
  1: 0,
  2: 0,
  3: 0
});

const reviewDialog = reactive({
  visible: false,
  operation: 1,
  note: '',
  currentItem: null
});

const totalPages = computed(() => {
  const pages = Math.ceil((pagination.total || 0) / pagination.pageSize);
  return pages > 0 ? pages : 1;
});

function getCurrentTabLabel() {
  const tab = tabs.find(entry => entry.type === activeTab.value);
  return tab ? tab.label : '举报';
}

function getInitial(name, fallback) {
  return (name || fallback || '?').slice(0, 1);
}

function getReportTypeLabel(type) {
  if (type === 1) return '用户举报';
  if (type === 2) return '视频举报';
  if (type === 3) return '评论举报';
  return '未知类型';
}

function userStatusText(status) {
  if (status === 0) return '正常';
  if (status === 1) return '已封禁';
  if (status === 2) return '已删除';
  return status === null || status === undefined ? '未知状态' : `状态${status}`;
}

function statusText(status) {
  if (status === 0) return '待审核';
  if (status === 1) return '已通过';
  if (status === 2) return '已驳回';
  return `状态${status}`;
}

function getStatusClass(status) {
  if (status === 0) return 'bg-amber-50 text-amber-700';
  if (status === 1) return 'bg-emerald-50 text-emerald-700';
  if (status === 2) return 'bg-rose-50 text-rose-700';
  return 'bg-slate-100 text-slate-600';
}

function reportStatusBorderClass(status) {
  if (status === 0) return 'border-l-[3px] border-l-amber-400';
  if (status === 1) return 'border-l-[3px] border-l-emerald-400';
  if (status === 2) return 'border-l-[3px] border-l-rose-400';
  return 'border-slate-200';
}

function formatTime(time) {
  if (!time) return '-';
  return String(time).replace('T', ' ');
}

function formatCount(value) {
  const num = Number(value || 0);
  return Number.isFinite(num) ? num.toLocaleString('zh-CN') : '0';
}

function buildReviewTargetText(item) {
  const info = item?.reportInfo || {};
  if (!item) return '-';
  if (item.reportType === 1) {
    return `${info.username || '未知用户'} (ID: ${info.userId || '-'})`;
  }
  if (item.reportType === 2) {
    return `${info.title || '未知视频'} (ID: ${info.videoId || '-'})`;
  }
  if (item.reportType === 3) {
    return `评论 ID: ${info.commentId || '-'}${info.title ? `，关联视频：${info.title}` : ''}`;
  }
  return '-';
}

function getBanTargetId(item) {
  const info = item?.reportInfo || {};
  if (item?.reportType === 1) return info.userId;
  if (item?.reportType === 2) return info.videoId;
  return null;
}

async function refreshTabCounts() {
  try {
    const responses = await Promise.all(
      tabs.map(tab =>
        queryReportList({
          status: filters.status,
          reportType: tab.type,
          pageNum: 1,
          pageSize: 1
        }).catch(() => ({ total: 0 }))
      )
    );
    tabs.forEach((tab, index) => {
      tabCounts[tab.type] = Number(responses[index]?.total || 0);
    });
  } catch (error) {
    console.error('加载举报统计失败:', error);
  }
}

async function fetchList(options = {}) {
  const { silent = false } = options;
  try {
    if (!silent) {
      loading.value = true;
    }
    const data = await queryReportList({
      status: filters.status,
      reportType: activeTab.value,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    });
    records.value = data?.records || [];
    pagination.total = Number(data?.total || 0);
    pagination.pageNum = Number(data?.pageNum || pagination.pageNum);
    pagination.pageSize = Number(data?.pageSize || pagination.pageSize);
  } catch (error) {
    ElMessage.error(error.message || '加载失败');
  } finally {
    if (!silent) {
      loading.value = false;
    }
  }
}

function switchTab(type) {
  if (activeTab.value === type) return;
  activeTab.value = type;
  reloadFirstPage();
}

function reloadFirstPage() {
  pagination.pageNum = 1;
  fetchList();
  refreshTabCounts();
}

function changePage(page) {
  pagination.pageNum = page;
  fetchList();
}

function openReviewDialog(item, operation) {
  reviewDialog.visible = true;
  reviewDialog.operation = operation;
  reviewDialog.note = '';
  reviewDialog.currentItem = item;
}

function closeReviewDialog() {
  if (operatingId.value === reviewDialog.currentItem?.reportId) return;
  reviewDialog.visible = false;
  reviewDialog.note = '';
  reviewDialog.currentItem = null;
}

async function confirmReview() {
  if (!reviewDialog.currentItem) return;

  operatingId.value = reviewDialog.currentItem.reportId;
  try {
    await reviewReport({
      reportId: reviewDialog.currentItem.reportId,
      operation: reviewDialog.operation,
      reviewNote: reviewDialog.note || ''
    });
    operatingId.value = null;
    closeReviewDialog();
    ElMessage.success(reviewDialog.operation === 1 ? '举报审核通过' : '举报已驳回');
    await fetchList();
    await refreshTabCounts();
  } catch (error) {
    ElMessage.error(error.message || '操作失败');
  } finally {
    operatingId.value = null;
  }
}

async function banTarget(item) {
  try {
    const targetId = getBanTargetId(item);
    if (!targetId) {
      throw new Error('未找到可封禁的目标');
    }
    operatingId.value = item.reportId;
    if (item.reportType === 1) {
      await banUser(targetId);
      ElMessage.success('用户封禁成功');
    } else if (item.reportType === 2) {
      await banVideo(targetId);
      ElMessage.success('视频封禁成功');
    } else {
      throw new Error('当前目标类型不支持手动封禁');
    }
    await fetchList();
    await refreshTabCounts();
  } catch (error) {
    ElMessage.error(error.message || '封禁失败');
  } finally {
    operatingId.value = null;
  }
}

async function unbanTarget(item) {
  try {
    const targetId = getBanTargetId(item);
    if (!targetId) {
      throw new Error('未找到可解禁的目标');
    }
    operatingId.value = item.reportId;
    if (item.reportType === 1) {
      await unbanUser(targetId);
      ElMessage.success('用户解禁成功');
    } else if (item.reportType === 2) {
      await unbanVideo(targetId);
      ElMessage.success('视频解禁成功');
    } else {
      throw new Error('当前目标类型不支持手动解禁');
    }
    await fetchList();
    await refreshTabCounts();
  } catch (error) {
    ElMessage.error(error.message || '解禁失败');
  } finally {
    operatingId.value = null;
  }
}

async function deleteTargetComment(item) {
  const commentId = item?.reportInfo?.commentId;
  if (!commentId) {
    ElMessage.error('未找到可删除的评论');
    return;
  }
  try {
    operatingId.value = item.reportId;
    await deleteComment(commentId);
    ElMessage.success('评论删除成功');
    await fetchList();
    await refreshTabCounts();
  } catch (error) {
    ElMessage.error(error.message || '删除评论失败');
  } finally {
    operatingId.value = null;
  }
}

watch(
  () => filters.status,
  () => {
    reloadFirstPage();
  }
);

onMounted(async () => {
  await fetchList();
  await refreshTabCounts();
});
</script>
