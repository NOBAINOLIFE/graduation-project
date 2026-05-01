<template>
  <div class="min-h-screen bg-[#f6f7fb] text-gray-800">
    <HeaderNav />

    <main class="max-w-7xl mx-auto px-4 py-6">
      <div class="flex flex-col gap-6 lg:flex-row">
        <div class="min-w-0 flex-1">
          <section class="mb-4">
            <h1 class="text-2xl leading-9 text-gray-900">
              {{ videoInfo.title || '视频加载中...' }}
            </h1>
            <div class="mt-2 flex flex-wrap items-center gap-3 text-sm text-gray-500">
              <span class="flex items-center gap-1">
                <el-icon><View /></el-icon>
                {{ formatCount(videoInfo.playCount) }} 播放
              </span>
              <span class="flex items-center gap-1">
                <el-icon><Clock /></el-icon>
                {{ formatDate(videoInfo.createTime) }}
              </span>
            </div>

          </section>

          <section class="overflow-hidden bg-black">
            <div class="relative aspect-video">
              <AppVideoPlayer
                ref="appVideoPlayerRef"
                :media-key="currentVideoId"
                :sources="videoSources"
                :poster="videoInfo.coverUrl || ''"
                :loading="loading"
                :initial-time="videoInfo.lastPlayTime"
                autoplay
                @timeupdate="handlePlaybackTimeUpdate"
                @ended="handleVideoEnded"
                @seek-commit="handleSeekCommit"
              />
            </div>
          </section>

          <section class="mt-4">
            <div class="flex flex-wrap items-center gap-3">
              <button
                class="flex items-center gap-2 rounded-full px-4 py-2 transition-colors"
                :class="videoInfo.isLike ? 'bg-[#e6f7ff] text-[#00a1d6]' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
                :disabled="actionLoading.like"
                @click="handleLike"
              >
                <el-icon :size="20"><CaretTop /></el-icon>
                <span>{{ formatCount(videoInfo.likeCount) }}</span>
              </button>

              <button
                class="flex items-center gap-2 rounded-full px-4 py-2 transition-colors"
                :class="videoInfo.isCoin ? 'bg-[#fff4e5] text-[#ff9f1c]' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
                :disabled="actionLoading.coin"
                @click="handleCoin"
              >
                <el-icon :size="20"><Coin /></el-icon>
                <span>{{ formatCount(videoInfo.coinCount) }}</span>
              </button>

              <button
                class="flex items-center gap-2 rounded-full px-4 py-2 transition-colors"
                :class="videoInfo.isCollect ? 'bg-[#fff7d6] text-[#d48806]' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
                :disabled="actionLoading.collect"
                @click="handleCollect"
              >
                <el-icon :size="20"><Star /></el-icon>
                <span>{{ formatCount(videoInfo.collectCount) }}</span>
              </button>

              <button
                class="flex items-center gap-2 rounded-full bg-gray-100 px-4 py-2 text-gray-700 transition-colors hover:bg-gray-200"
                :disabled="actionLoading.share"
                @click="handleShare"
              >
                <el-icon :size="20"><Share /></el-icon>
                <span>{{ formatCount(videoInfo.shareCount) }}</span>
              </button>

              <button
                class="flex items-center gap-2 rounded-full bg-gray-100 px-4 py-2 text-gray-700 transition-colors hover:bg-gray-200"
                @click="handleReport"
              >
                <el-icon :size="20"><WarnTriangleFilled /></el-icon>
                <span>举报</span>
              </button>
            </div>
          </section>

          <section v-if="videoInfo.description || videoInfo.tagList.length > 0" class="mt-4">
            <p v-if="videoInfo.description" class="whitespace-pre-line text-xm leading-7 text-gray-800">
              {{ videoInfo.description }}
            </p>

            <div v-if="videoInfo.tagList.length > 0" class="mt-4 flex flex-wrap gap-2">
              <span
                v-for="tag in videoInfo.tagList"
                :key="tag"
                class="rounded-full bg-gray-200 px-3 py-1 text-sm font-medium text-gray-600"
              >
                {{ tag }}
              </span>
            </div>
          </section>

          <section class="mt-4 rounded-2xl bg-white p-5 shadow-sm">
            <div class="mb-5 flex flex-wrap items-center justify-between gap-3">
              <h2 class="text-lg font-bold text-gray-900">
                评论 {{ commentTotal > 0 ? `${commentTotal}` : '' }}
              </h2>
              <el-radio-group v-model="commentSort" size="medium">
                <el-radio-button label="hot">最热</el-radio-button>
                <el-radio-button label="new">最新</el-radio-button>
              </el-radio-group>
            </div>

            <div class="mb-6 flex gap-3">
              <el-avatar :size="40" class="shrink-0 bg-[#00a1d6] text-white">
                {{ currentUsernameInitial }}
              </el-avatar>
              <div class="flex-1">
                <el-input
                  v-model="commentContent"
                  type="textarea"
                  :rows="3"
                  maxlength="500"
                  show-word-limit
                  :readonly="!viewerLoggedIn"
                  :placeholder="viewerLoggedIn ? '发条友善的评论吧' : '登录后参与评论互动'"
                  @focus="handleCommentFocus"
                />
                <div class="mt-3 flex justify-end">
                  <el-button
                    type="primary"
                    :loading="commentSubmitting"
                    :disabled="!commentContent.trim()"
                    @click="submitComment"
                  >
                    发布评论
                  </el-button>
                </div>
              </div>
            </div>

            <div v-if="commentLoading && comments.length === 0" class="space-y-4">
              <div v-for="item in 3" :key="item" class="flex gap-3 rounded-xl bg-gray-50 p-4">
                <div class="h-10 w-10 rounded-full bg-gray-200"></div>
                <div class="flex-1 space-y-2">
                  <div class="h-4 w-24 rounded bg-gray-200"></div>
                  <div class="h-3 w-full rounded bg-gray-100"></div>
                  <div class="h-3 w-3/4 rounded bg-gray-100"></div>
                </div>
              </div>
            </div>

            <div v-else-if="comments.length === 0" class="flex flex-col items-center justify-center py-14 text-gray-400">
              <el-icon :size="42"><ChatLineRound /></el-icon>
              <p class="mt-3 text-sm">还没有评论，来抢个沙发吧</p>
            </div>

            <div v-else class="space-y-5">
              <article
                v-for="comment in comments"
                :key="comment.commentId"
                class="transition-colors hover:border-[#d6eef8]"
              >
                <div class="flex gap-3">
                  <el-avatar :size="50" :src="comment.avatarUrl" class="shrink-0 bg-[#00a1d6] text-white">
                    {{ (comment.username || 'U').slice(0, 1) }}
                  </el-avatar>
                  <div class="min-w-0 flex-1">
                    <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
                      <span class="font-medium text-gray-500 text-xm">{{ comment.username || '未知用户' }}</span>
                      <span class="text-xs text-gray-400">{{ formatDate(comment.createTime) }}</span>
                    </div>
                    <p class="mt-2 whitespace-pre-line break-words text-base leading-7 text-gray-900">
                      <template v-if="shouldShowReplyTarget(comment)">回复 <span class="text-[#00a1d6]">@{{ comment.replyUsername }}</span>：</template>{{ comment.content }}
                    </p>
                    <div class="mt-3 flex items-center gap-4 text-sm text-gray-500">
                      <button
                        class="flex items-center gap-1 transition-colors hover:text-[#00a1d6]"
                        :class="comment.isLike ? 'text-[#00a1d6]' : ''"
                        @click="handleLikeComment(comment)"
                      >
                        <el-icon><CaretTop /></el-icon>
                        <span>{{ formatCount(comment.likeCount) }}</span>
                      </button>
                      <button class="transition-colors hover:text-[#00a1d6]" @click="handleReply(comment)">
                        回复
                      </button>
                    </div>

                    <div
                      v-if="!isReplyExpanded(comment.commentId) && !isReplyComposerVisible(comment.commentId) && comment.replyCount > 0"
                      class="mt-4 rounded-2xl bg-[#f7fbfd] p-4"
                    >
                      <div
                        v-for="reply in comment.replyPreviewList || []"
                        :key="`preview-${reply.commentId}`"
                      >
                        <div class="flex gap-3">
                          <el-avatar :size="34" :src="reply.avatarUrl" class="shrink-0 bg-[#00a1d6] text-white">
                            {{ (reply.username || 'U').slice(0, 1) }}
                          </el-avatar>
                          <div class="min-w-0 flex-1">
                            <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
                              <span class="text-sm font-medium text-gray-500">{{ reply.username || '未知用户' }}</span>
                              <span class="text-xs text-gray-400">{{ formatDate(reply.createTime) }}</span>
                            </div>
                            <p class="mt-2 whitespace-pre-line break-words text-base leading-7 text-gray-900">
                              <template v-if="shouldShowReplyTarget(reply)">回复 <span class="text-[#00a1d6]">@{{ reply.replyUsername }}</span>：</template>{{ reply.content }}
                            </p>
                          </div>
                        </div>
                      </div>

                      <button
                        class="mt-1 text-sm text-[#00a1d6] transition-colors hover:text-[#0090c0]"
                        @click="loadCommentReplies(comment)"
                      >
                        共{{ comment.replyCount }}条回复，点击查看
                      </button>
                    </div>

                    <div
                      v-if="isReplyComposerVisible(comment.commentId) || isReplyExpanded(comment.commentId)"
                      class="mt-4 rounded-2xl bg-[#f7fbfd] p-4"
                    >

                      <div
                        v-if="isReplyExpanded(comment.commentId) && replyStateMap[comment.commentId]?.loading && replyStateMap[comment.commentId]?.records.length === 0"
                        class="space-y-3"
                      >
                        <div v-for="index in 2" :key="`${comment.commentId}-reply-loading-${index}`" class="rounded-xl bg-white p-3">
                          <div class="h-3 w-24 rounded bg-gray-200"></div>
                          <div class="mt-2 h-3 w-full rounded bg-gray-100"></div>
                        </div>
                      </div>

                      <div v-else-if="isReplyExpanded(comment.commentId) && replyStateMap[comment.commentId]?.records.length" class="space-y-3">
                        <article
                          v-for="reply in replyStateMap[comment.commentId].records"
                          :key="reply.commentId"
                        >
                          <div class="flex gap-3">
                            <el-avatar :size="34" :src="reply.avatarUrl" class="shrink-0 bg-[#00a1d6] text-white">
                              {{ (reply.username || 'U').slice(0, 1) }}
                            </el-avatar>
                            <div class="min-w-0 flex-1">
                              <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
                                <span class="text-sm font-medium text-gray-500">{{ reply.username || '未知用户' }}</span>
                                <span class="text-xs text-gray-400">{{ formatDate(reply.createTime) }}</span>
                              </div>
                              <p class="mt-2 whitespace-pre-line break-words text-base leading-7 text-gray-900">
                                <template v-if="shouldShowReplyTarget(reply)">回复 <span class="text-[#00a1d6]">@{{ reply.replyUsername }}</span>：</template>{{ reply.content }}
                              </p>
                              <div class="mt-2 flex items-center gap-4 text-sm text-gray-500">
                                <button
                                  class="flex items-center gap-1 transition-colors hover:text-[#00a1d6]"
                                  :class="reply.isLike ? 'text-[#00a1d6]' : ''"
                                  @click="handleLikeComment(reply)"
                                >
                                  <el-icon><CaretTop /></el-icon>
                                  <span>{{ formatCount(reply.likeCount) }}</span>
                                </button>
                                <button class="transition-colors hover:text-[#00a1d6]" @click="handleReply(comment, reply)">
                                  回复
                                </button>
                              </div>
                            </div>
                          </div>
                        </article>
                      </div>

                      <div
                        v-else-if="isReplyExpanded(comment.commentId) && !replyStateMap[comment.commentId]?.loading"
                        class="rounded-2xl bg-white px-4 py-5 text-center text-sm text-gray-400"
                      >
                        暂无回复，来发第一条吧
                      </div>

                      <div
                        v-if="isReplyExpanded(comment.commentId)"
                        class="reply-pagination mt-4 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-gray-600"
                      >
                        <span class="reply-pagination__summary">共{{ getReplyTotalPages(comment) }}页</span>
                        <div
                          v-if="getReplyTotalPages(comment) > 1"
                          class="reply-pagination__pages flex flex-wrap items-center gap-1"
                        >
                          <button
                            v-for="item in getReplyPaginationItems(comment)"
                            :key="`${comment.commentId}-${item.type}-${item.value}`"
                            :disabled="item.type === 'ellipsis' || item.value === getReplyCurrentPage(comment)"
                            class="reply-pagination__item"
                            :class="{
                              'reply-pagination__item--active': item.value === getReplyCurrentPage(comment),
                              'reply-pagination__item--ellipsis': item.type === 'ellipsis'
                            }"
                            @click="item.type === 'page' && changeReplyPage(comment, item.value)"
                          >
                            {{ item.label }}
                          </button>
                          <button
                            v-if="getReplyCurrentPage(comment) < getReplyTotalPages(comment)"
                            class="reply-pagination__next"
                            @click="changeReplyPage(comment, getReplyCurrentPage(comment) + 1)"
                          >
                            下一页
                          </button>
                        </div>
                        <button
                          class="reply-pagination__collapse"
                          @click="toggleReplyExpand(comment)"
                        >
                          收起
                        </button>
                      </div>

                      <div v-if="isReplyComposerVisible(comment.commentId)" class="mt-4 border-t border-[#eaf0f4] pt-4">
                        <div class="mb-2 flex items-center justify-between gap-3">
                          <p class="text-sm text-gray-500">
                            {{ getReplyComposerLabel(comment) }}
                          </p>
                          <button class="text-sm text-gray-400 transition-colors hover:text-gray-600" @click="cancelReply(comment)">
                            取消
                          </button>
                        </div>
                        <el-input
                          v-model="replyStateMap[comment.commentId].draft"
                          type="textarea"
                          :rows="2"
                          maxlength="500"
                          show-word-limit
                          :placeholder="getReplyPlaceholder(comment)"
                        />
                        <div class="mt-3 flex justify-end">
                          <el-button
                            type="primary"
                            size="small"
                            :loading="replyStateMap[comment.commentId].submitting"
                            :disabled="!replyStateMap[comment.commentId].draft.trim()"
                            @click="submitReply(comment)"
                          >
                            发布回复
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </article>
            </div>

            <div
              v-if="hasMoreComments"
              ref="commentLoadTriggerRef"
              class="mt-6 flex items-center justify-center py-4 text-sm text-gray-400"
            >
              {{ commentLoading ? '正在加载更多评论...' : '滚动到底部自动加载更多评论' }}
            </div>
            <div v-else-if="comments.length > 0" class="mt-6 py-4 text-center text-sm text-gray-400">
              已经看到全部评论了
            </div>
          </section>
        </div>

        <aside class="w-full shrink-0 lg:w-80">
          <div class="sticky top-24 rounded-2xl bg-white p-5 shadow-sm">
            <div class="flex items-start gap-4">
              <el-avatar
                :size="56"
                :src="videoInfo.avatarUrl"
                class="cursor-pointer bg-[#00a1d6] text-white"
                @click="goToUserProfile"
              >
                {{ (videoInfo.username || 'U').slice(0, 1) }}
              </el-avatar>
              <div class="min-w-0 flex-1">
                <button class="max-w-full truncate text-left text-lg font-semibold text-gray-900 hover:text-[#00a1d6]" @click="goToUserProfile">
                  {{ videoInfo.username || '未知用户' }}
                </button>
                <p class="mt-1 text-sm text-gray-500">粉丝 {{ formatCount(videoInfo.fansCount) }}</p>
                <p class="mt-3 line-clamp-3 text-sm leading-6 text-gray-600">
                  {{ videoInfo.userBio || '这个 UP 主还没有留下简介。' }}
                </p>
                <el-button
                  type="primary"
                  class="mt-4 w-full"
                  :disabled="actionLoading.follow || isSelfVideo"
                  @click="handleFollow"
                >
                  {{ isSelfVideo ? '这是你自己的视频' : (videoInfo.isFollow ? '已关注' : '+ 关注') }}
                </el-button>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </main>

    <div
      v-if="collectDialog.visible"
      class="fixed inset-0 z-40 flex items-center justify-center bg-black/55 p-4"
      @click.self="closeCollectDialog"
    >
      <div class="w-full max-w-3xl rounded-xl bg-white px-8 py-6 shadow-[0_28px_80px_rgba(15,23,42,0.28)]">
        <div class="flex items-center justify-between border-b border-gray-100 pb-4">
          <h3 class="mx-auto text-3xl font-medium tracking-[0.08em] text-gray-900">添加到收藏夹</h3>
          <button class="ml-4 text-4xl leading-none text-gray-300 transition hover:text-gray-500" @click="closeCollectDialog">×</button>
        </div>

        <div class="mt-4 max-h-[420px] overflow-y-auto pr-2">
          <div v-if="collectDialog.loading" class="flex h-56 items-center justify-center text-sm text-gray-400">
            正在加载收藏夹...
          </div>

          <div v-else-if="collectDialog.directories.length === 0" class="flex h-56 items-center justify-center text-sm text-gray-400">
            暂无可用收藏夹
          </div>

          <button
            v-for="directory in collectDialog.directories"
            :key="directory.directoryId"
            class="flex w-full items-center justify-between gap-6 border-b border-gray-100 px-1 py-7 text-left transition hover:bg-gray-50/80"
            type="button"
            @click="toggleCollectDirectorySelection(directory.directoryId)"
          >
            <div class="flex min-w-0 items-center gap-8">
              <span
                class="flex h-8 w-8 items-center justify-center rounded border transition"
                :class="isCollectDialogSelected(directory.directoryId) ? 'border-[#00a1d6] bg-[#00a1d6]/10' : 'border-[#cfd6df] bg-white'"
              >
                <span
                  v-if="isCollectDialogSelected(directory.directoryId)"
                  class="h-4 w-4 rounded-sm bg-[#00a1d6]"
                ></span>
              </span>
              <div class="min-w-0">
                <p class="text-[1.125rem] font-medium text-gray-900">
                  {{ directory.directoryName }}
                  <span v-if="!directory.isPublic" class="ml-1 text-gray-400">[私密]</span>
                  <span v-else class="ml-1 text-gray-400">[公开]</span>
                </p>
                <p class="mt-2 text-sm text-gray-400">
                  {{ isCollectDialogSelected(directory.directoryId) ? '已选择' : '未选择' }}
                </p>
              </div>
            </div>
            <div class="shrink-0 text-right text-[1.125rem] text-gray-500">
              <span v-if="directory.isDefault" class="rounded-full bg-[#eef7ff] px-3 py-1 text-sm text-[#00a1d6]">默认收藏夹</span>
            </div>
          </button>
        </div>

        <div class="mt-8 border-t border-gray-100 pt-7 text-center">
          <button
            class="min-w-[240px] rounded-lg px-8 py-4 text-xl font-medium transition"
            :class="collectDialogHasChanges ? 'bg-[#00a1d6] text-white hover:bg-[#00b5e5]' : 'bg-gray-200 text-gray-400'"
            :disabled="collectDialog.submitting || !collectDialogHasChanges"
            @click="submitCollectSelection"
          >
            {{ collectDialog.submitting ? '提交中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="coinDialog.visible"
      class="fixed inset-0 z-40 flex items-center justify-center bg-black/55 p-4"
      @click.self="closeCoinDialog"
    >
      <div class="w-full max-w-4xl rounded-xl bg-white px-8 py-6 shadow-[0_28px_80px_rgba(15,23,42,0.28)]">
        <div class="flex items-center justify-between">
          <h3 class="flex-1 text-center text-[2.1rem] font-medium tracking-[0.04em] text-gray-900">
            给UP主投上 <span class="px-1 text-[#00a1d6]">{{ coinDialog.amount }}</span> 枚硬币
          </h3>
          <button class="ml-4 text-4xl leading-none text-gray-300 transition hover:text-gray-500" @click="closeCoinDialog">×</button>
        </div>

        <div class="mt-10 grid gap-6 md:grid-cols-2">
          <button
            v-for="option in coinOptions"
            :key="option.amount"
            class="group rounded-2xl border-2 border-dashed p-6 text-left transition"
            :class="coinDialog.amount === option.amount ? 'border-[#00a1d6] bg-[#f2fbff]' : 'border-[#d5dbe3] hover:border-[#8bd8f2]'"
            @click="coinDialog.amount = option.amount"
          >
            <div class="text-2xl font-medium" :class="coinDialog.amount === option.amount ? 'text-[#00a1d6]' : 'text-gray-500'">
              {{ option.amount }}硬币
            </div>
            <div class="mt-6 flex min-h-[220px] items-center justify-center rounded-xl bg-[radial-gradient(circle_at_top,_rgba(0,161,214,0.12),_transparent_55%),linear-gradient(180deg,_rgba(248,250,252,0.95),_rgba(241,245,249,0.85))]">
              <div class="relative flex items-end justify-center gap-3">
                <div
                  v-for="coin in option.amount"
                  :key="`coin-${option.amount}-${coin}`"
                  class="coin-dialog-coin"
                  :class="coin === 2 ? '-ml-5 mt-3' : ''"
                >
                  币
                </div>
              </div>
            </div>
          </button>
        </div>

        <label class="mt-7 flex cursor-pointer items-center gap-3 text-xl text-gray-700">
          <span
            class="flex h-7 w-7 items-center justify-center rounded bg-[#00a1d6] text-white"
            :class="coinDialog.alsoLike ? 'opacity-100' : 'bg-white text-transparent ring-1 ring-[#cfd6df]'"
          >
            ✓
          </span>
          <input v-model="coinDialog.alsoLike" type="checkbox" class="sr-only" />
          同时点赞内容
        </label>

        <div class="mt-10 text-center">
          <button
            class="min-w-[120px] rounded-lg bg-[#00a1d6] px-8 py-4 text-2xl font-medium text-white transition hover:bg-[#00b5e5] disabled:cursor-not-allowed disabled:bg-[#9ddff3]"
            :disabled="coinDialog.submitting"
            @click="submitCoinSelection"
          >
            {{ coinDialog.submitting ? '提交中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="reportDialog.visible"
      class="fixed inset-0 z-40 flex items-center justify-center bg-black/55 p-4"
      @click.self="closeReportDialog"
    >
      <div class="w-full max-w-3xl rounded-xl bg-white px-8 py-6 shadow-[0_28px_80px_rgba(15,23,42,0.28)]">
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-xl font-semibold text-slate-900">举报视频</h3>
          <button class="text-slate-400 transition hover:text-slate-600" @click="closeReportDialog">×</button>
        </div>

        <p class="mb-3 text-sm text-slate-500">请选择举报原因</p>
        <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <label
            v-for="option in reportReasonOptions"
            :key="option"
            class="flex cursor-pointer items-center gap-2 rounded-xl border border-slate-200 px-3 py-2 text-sm transition hover:border-[#00a1d6]"
          >
            <input v-model="reportDialog.reason" type="radio" :value="option" class="accent-[#00a1d6]" />
            <span>{{ option }}</span>
          </label>
        </div>

        <div class="mt-4">
          <p class="mb-1 text-sm text-slate-500">详细描述 <span class="text-rose-500">*</span></p>
          <textarea
            v-model="reportDialog.detail"
            maxlength="400"
            rows="4"
            placeholder="请填写举报详细信息，方便我们更好地处理"
            class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-[#00a1d6] focus:outline-none"
          ></textarea>
          <p class="mt-1 text-right text-xs text-slate-400">{{ reportDialog.detail.length }}/400</p>
        </div>

        <div class="mt-5 flex justify-end gap-3">
          <button class="rounded-lg border border-slate-200 px-6 py-2 text-sm text-slate-600 transition hover:bg-slate-50" @click="closeReportDialog">
            取消
          </button>
          <button
            class="rounded-lg bg-[#00a1d6] px-6 py-2 text-sm text-white transition hover:bg-[#00b5e5] disabled:cursor-not-allowed disabled:bg-slate-300"
            :disabled="reportDialog.submitting"
            @click="submitReport"
          >
            {{ reportDialog.submitting ? '提交中...' : '提交' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  CaretTop,
  ChatDotRound,
  ChatLineRound,
  Clock,
  Coin,
  Share,
  Star,
  Timer,
  View,
  WarnTriangleFilled
} from '@element-plus/icons-vue';
import HeaderNav from './HeaderNav.vue';
import AppVideoPlayer from './common/AppVideoPlayer.vue';
import {
  coinVideo,
  collectVideo,
  followUser,
  getCommentReplies,
  getVideoComments,
  getVideoDetail,
  likeComment,
  likeVideo,
  queryVideoDirectoryRelations,
  reportVideo,
  reportVideoProgress,
  shareVideo,
  submitVideoComment
} from '../api/video';
import {
  getUserId,
  getUsername,
  isUserLoggedIn,
  openLoginModal,
  USER_AUTH_CHANGE_EVENT
} from '../utils/auth';

const route = useRoute();
const router = useRouter();
const viewerLoggedIn = ref(isUserLoggedIn());
const currentUsername = ref(getUsername());

const loading = ref(false);
const commentLoading = ref(false);
const commentSubmitting = ref(false);
const appVideoPlayerRef = ref(null);
const currentVideoId = ref(Number(route.params.id) || 0);
const commentSort = ref('hot');
const commentContent = ref('');
const comments = ref([]);
const commentTotal = ref(0);
const commentPageSize = 20;
const nextCommentCursor = ref(null);
const hasMoreComments = ref(true);
const lastReportedTime = ref(0);
const reportInFlight = ref(false);
const replyStateMap = reactive({});
const commentLoadTriggerRef = ref(null);

let commentLoadObserver = null;

const collectDialog = reactive({
  visible: false,
  loading: false,
  submitting: false,
  directories: [],
  selectedDirectoryIds: [],
  initialSelectedDirectoryIds: []
});

const coinDialog = reactive({
  visible: false,
  submitting: false,
  amount: 2,
  alsoLike: true
});

const reportDialog = reactive({
  visible: false,
  submitting: false,
  reason: '',
  detail: ''
});

const reportReasonOptions = [
  '色情低俗',
  '违法犯罪',
  '赌博诈骗',
  '血腥暴力',
  '人身攻击',
  '侵犯隐私',
  '垃圾广告',
  '侵权',
  '其他'
];

const coinOptions = [
  { amount: 1 },
  { amount: 2 }
];

const actionLoading = reactive({
  like: false,
  coin: false,
  collect: false,
  follow: false,
  share: false
});

const videoInfo = ref(createDefaultVideoInfo());

const videoSources = computed(() => videoInfo.value.videoSourceList || []);
const currentUsernameInitial = computed(() => (currentUsername.value || 'U').slice(0, 1));
const isSelfVideo = computed(() => {
  const userId = getUserId();
  return Boolean(userId && videoInfo.value.userId && userId === videoInfo.value.userId);
});
const collectDialogHasChanges = computed(() => {
  const current = [...collectDialog.selectedDirectoryIds].sort((a, b) => a - b);
  const initial = [...collectDialog.initialSelectedDirectoryIds].sort((a, b) => a - b);
  if (current.length !== initial.length) return true;
  return current.some((id, index) => id !== initial[index]);
});

function createDefaultVideoInfo() {
  return {
    videoId: null,
    userId: null,
    title: '',
    description: '',
    coverUrl: '',
    videoSourceList: [],
    partitionName: '',
    tagList: [],
    duration: 0,
    lastPlayTime: 0,
    username: '',
    avatarUrl: '',
    userBio: '',
    fansCount: 0,
    isFollow: false,
    playCount: 0,
    likeCount: 0,
    coinCount: 0,
    collectCount: 0,
    shareCount: 0,
    commentCount: 0,
    isLike: false,
    isCoin: false,
    isCollect: false,
    createTime: ''
  };
}

function normalizeVideoDetail(data) {
  return {
    ...createDefaultVideoInfo(),
    ...data,
    playCount: Number(data?.playCount || 0),
    likeCount: Number(data?.likeCount || 0),
    coinCount: Number(data?.coinCount || 0),
    collectCount: Number(data?.collectCount || 0),
    shareCount: Number(data?.shareCount || 0),
    commentCount: Number(data?.commentCount || 0),
    fansCount: Number(data?.fansCount || 0),
    duration: Number(data?.duration || 0),
    lastPlayTime: Number(data?.lastPlayTime || 0),
    tagList: Array.isArray(data?.tagList) ? data.tagList : [],
    isLike: Boolean(data?.isLike),
    isCoin: Boolean(data?.isCoin),
    isCollect: Boolean(data?.isCollect),
    isFollow: Boolean(data?.isFollow),
    videoSourceList: Array.isArray(data?.videoSourceList) ? data.videoSourceList : []
  };
}

function disconnectCommentLoadObserver() {
  if (commentLoadObserver) {
    commentLoadObserver.disconnect();
    commentLoadObserver = null;
  }
}

function setupCommentLoadObserver() {
  disconnectCommentLoadObserver();
  if (typeof window === 'undefined' || !commentLoadTriggerRef.value) {
    return;
  }
  commentLoadObserver = new IntersectionObserver((entries) => {
    const entry = entries[0];
    if (!entry?.isIntersecting) {
      return;
    }
    if (commentLoading.value || !hasMoreComments.value) {
      return;
    }
    loadComments(true);
  }, {
    root: null,
    rootMargin: '180px 0px',
    threshold: 0
  });
  commentLoadObserver.observe(commentLoadTriggerRef.value);
}

function clearReplyStates() {
  Object.keys(replyStateMap).forEach((key) => {
    delete replyStateMap[key];
  });
}

function createReplyState(comment) {
  return {
    expanded: false,
    loaded: false,
    loading: false,
    records: [],
    total: Number(comment?.replyCount || 0),
    pageNum: 1,
    pageSize: 10,
    draft: '',
    submitting: false,
    showComposer: false,
    parentId: Number(comment?.commentId || 0),
    replyUserId: Number(comment?.userId || 0),
    replyUsername: comment?.username || ''
  };
}

function ensureReplyState(comment) {
  const commentId = Number(comment?.commentId || 0);
  if (!commentId) {
    return null;
  }
  if (!replyStateMap[commentId]) {
    replyStateMap[commentId] = createReplyState(comment);
  }
  replyStateMap[commentId].total = Number(comment?.replyCount || 0);
  return replyStateMap[commentId];
}

function resetReplyComposer(comment) {
  const state = ensureReplyState(comment);
  if (!state) {
    return;
  }
  state.draft = '';
  state.showComposer = false;
  state.parentId = Number(comment.commentId || 0);
  state.replyUserId = Number(comment.userId || 0);
  state.replyUsername = comment.username || '';
}

function isReplyExpanded(commentId) {
  return Boolean(replyStateMap[commentId]?.expanded);
}

function isReplyComposerVisible(commentId) {
  return Boolean(replyStateMap[commentId]?.showComposer);
}

function getReplyPlaceholder(comment) {
  const state = ensureReplyState(comment);
  if (!state) {
    return '写下你的回复';
  }
  if (Number(state.parentId || 0) === Number(comment?.commentId || 0)) {
    return '写下你的回复';
  }
  if (!state.replyUsername) {
    return '写下你的回复';
  }
  return `回复 @${state.replyUsername}`;
}

function getReplyComposerLabel(comment) {
  const state = ensureReplyState(comment);
  if (!state) {
    return '写下你的回复';
  }
  if (Number(state.parentId || 0) === Number(comment?.commentId || 0)) {
    return '正在回复该评论';
  }
  return `正在回复 @${state.replyUsername || ''}`.trim();
}

function shouldShowReplyTarget(comment) {
  if (!comment?.replyUsername) {
    return false;
  }
  const rootId = Number(comment.rootId || 0);
  const parentId = Number(comment.parentId || 0);
  return rootId > 0 && parentId > 0 && parentId !== rootId;
}

function getReplyCurrentPage(comment) {
  return Number(ensureReplyState(comment)?.pageNum || 1);
}

function getReplyTotalPages(comment) {
  const state = ensureReplyState(comment);
  if (!state) {
    return 1;
  }
  const total = Number(state.total || comment?.replyCount || 0);
  const pageSize = Number(state.pageSize || 10);
  return Math.max(1, Math.ceil(total / pageSize));
}

function getReplyPaginationItems(comment) {
  const totalPages = getReplyTotalPages(comment);
  const currentPage = getReplyCurrentPage(comment);
  if (totalPages <= 5) {
    return Array.from({ length: totalPages }, (_, index) => ({
      type: 'page',
      value: index + 1,
      label: `${index + 1}`
    }));
  }

  const pages = [];
  const addPage = (value) => {
    pages.push({ type: 'page', value, label: `${value}` });
  };
  const addEllipsis = (key) => {
    pages.push({ type: 'ellipsis', value: key, label: '...' });
  };

  if (currentPage <= 3) {
    addPage(1);
    addPage(2);
    addPage(3);
    addPage(4);
    addPage(5);
    addEllipsis(`tail-${totalPages}`);
    addPage(totalPages);
    return pages;
  }

  if (currentPage >= totalPages - 2) {
    addPage(1);
    addEllipsis(`head-${totalPages}`);
    for (let page = totalPages - 4; page <= totalPages; page += 1) {
      addPage(page);
    }
    return pages;
  }

  addPage(1);
  addEllipsis(`before-${currentPage}`);
  for (let page = currentPage - 1; page <= currentPage + 1; page += 1) {
    addPage(page);
  }
  addEllipsis(`after-${currentPage}`);
  addPage(totalPages);
  return pages;
}

async function loadCommentReplies(comment, pageNum = 1, forceRefresh = false) {
  const state = ensureReplyState(comment);
  if (!state || state.loading) {
    return;
  }
  const targetPage = Number(pageNum || 1);
  if (!forceRefresh && state.loaded && state.pageNum === targetPage) {
    state.expanded = true;
    return;
  }

  state.loading = true;
  state.expanded = true;
  try {
    const result = await getCommentReplies(comment.commentId, {
      pageNum: targetPage,
      pageSize: state.pageSize
    });
    const records = Array.isArray(result?.records) ? result.records : [];
    state.records = records;
    state.total = Number(result?.total || comment.replyCount || 0);
    state.pageNum = Number(result?.pageNum || targetPage);
    state.pageSize = Number(result?.pageSize || state.pageSize || 10);
    state.loaded = true;
  } catch (error) {
    console.error('加载回复失败:', error);
    ElMessage.error(error.message || '加载回复失败');
  } finally {
    state.loading = false;
  }
}

async function toggleReplyExpand(comment) {
  const state = ensureReplyState(comment);
  if (!state) {
    return;
  }
  if (state.expanded) {
    state.expanded = false;
    return;
  }
  await loadCommentReplies(comment);
}

async function changeReplyPage(comment, pageNum) {
  const targetPage = Number(pageNum || 1);
  if (targetPage < 1 || targetPage === getReplyCurrentPage(comment)) {
    return;
  }
  await loadCommentReplies(comment, targetPage);
}

async function loadVideoDetail() {
  if (!currentVideoId.value) return;
  loading.value = true;
  try {
    const data = await getVideoDetail(currentVideoId.value);
    videoInfo.value = normalizeVideoDetail(data);
    lastReportedTime.value = Number(videoInfo.value.lastPlayTime || 0);
  } catch (error) {
    console.error('加载视频详情失败:', error);
    ElMessage.error(error.message || '加载视频失败');
  } finally {
    loading.value = false;
  }
}

async function loadComments(isLoadMore = false) {
  if (!currentVideoId.value || commentLoading.value) return;
  if (isLoadMore && !hasMoreComments.value) return;
  commentLoading.value = true;
  try {
    const result = await getVideoComments(currentVideoId.value, {
      sortType: commentSort.value === 'hot' ? 1 : 2,
      cursor: isLoadMore ? nextCommentCursor.value : null,
      pageSize: commentPageSize
    });
    const records = Array.isArray(result?.records) ? result.records : [];
    if (!isLoadMore) {
      clearReplyStates();
    }
    comments.value = isLoadMore ? [...comments.value, ...records] : records;
    commentTotal.value = Number(result?.total || 0);
    nextCommentCursor.value = buildCommentCursor(result);
    hasMoreComments.value = !Boolean(result?.isEnd);
    await nextTick();
    setupCommentLoadObserver();
  } catch (error) {
    console.error('加载评论失败:', error);
    ElMessage.error(error.message || '加载评论失败');
  } finally {
    commentLoading.value = false;
  }
}

function buildCommentCursor(result) {
  if (!result || result.isEnd || !result.lastCreateTime || !result.lastCommentId) {
    return null;
  }
  return {
    lastHotScore: result.lastHotScore ?? null,
    lastCreateTime: result.lastCreateTime,
    lastCommentId: result.lastCommentId
  };
}

async function submitComment() {
  if (!ensureLoggedIn('登录后才可以发表评论')) return;
  const content = commentContent.value.trim();
  if (!content) return;
  commentSubmitting.value = true;
  try {
    await submitVideoComment({
      videoId: currentVideoId.value,
      rootId: 0,
      parentId: 0,
      replyUserId: 0,
      content
    });
    commentContent.value = '';
    videoInfo.value.commentCount += 1;
    commentTotal.value += 1;
    await loadComments(false);
    ElMessage.success('评论发布成功');
  } catch (error) {
    console.error('提交评论失败:', error);
    ElMessage.error(error.message || '评论发布失败');
  } finally {
    commentSubmitting.value = false;
  }
}

async function startReply(comment, targetReply = null) {
  if (!ensureLoggedIn('登录后可参与评论回复')) return;
  const state = ensureReplyState(comment);
  if (!state) {
    return;
  }
  state.expanded = true;
  state.showComposer = true;
  state.parentId = Number(targetReply?.commentId || comment.commentId || 0);
  state.replyUserId = Number(targetReply?.userId || comment.userId || 0);
  state.replyUsername = targetReply?.username || comment.username || '';
  if (!state.loaded && Number(comment.replyCount || 0) > 0) {
    await loadCommentReplies(comment);
  }
}

function cancelReply(comment) {
  resetReplyComposer(comment);
}

async function submitReply(comment) {
  if (!ensureLoggedIn('登录后可参与评论回复')) return;
  const state = ensureReplyState(comment);
  const content = state?.draft?.trim();
  if (!state || !content) {
    return;
  }

  state.submitting = true;
  try {
    await submitVideoComment({
      videoId: currentVideoId.value,
      rootId: Number(comment.commentId || 0),
      parentId: Number(state.parentId || comment.commentId || 0),
      replyUserId: Number(state.replyUserId || comment.userId || 0),
      content
    });
    comment.replyCount = Number(comment.replyCount || 0) + 1;
    resetReplyComposer(comment);
    await loadCommentReplies(comment, 1, true);
    ElMessage.success('回复发布成功');
  } catch (error) {
    console.error('提交回复失败:', error);
    ElMessage.error(error.message || '回复发布失败');
  } finally {
    state.submitting = false;
  }
}

async function handleLike() {
  if (!ensureLoggedIn('登录后可点赞视频')) return;
  if (actionLoading.like) return;
  actionLoading.like = true;
  const nextIsLike = !videoInfo.value.isLike;
  try {
    await likeVideo({
      targetId: currentVideoId.value,
      type: 0,
      operation: nextIsLike ? 1 : 0
    });
    videoInfo.value.isLike = nextIsLike;
    videoInfo.value.likeCount = Math.max(0, Number(videoInfo.value.likeCount || 0) + (nextIsLike ? 1 : -1));
    ElMessage.success(nextIsLike ? '点赞成功' : '已取消点赞');
  } catch (error) {
    console.error('点赞失败:', error);
    ElMessage.error(error.message || '点赞失败');
  } finally {
    actionLoading.like = false;
  }
}

async function handleCoin() {
  if (!ensureLoggedIn('登录后可给视频投币')) return;
  if (actionLoading.coin) return;
  coinDialog.visible = true;
  coinDialog.amount = videoInfo.value.isCoin ? 1 : 2;
  coinDialog.alsoLike = true;
}

function closeCoinDialog() {
  if (coinDialog.submitting) return;
  coinDialog.visible = false;
}

async function submitCoinSelection() {
  if (actionLoading.coin || !currentVideoId.value) return;
  actionLoading.coin = true;
  coinDialog.submitting = true;
  try {
    await coinVideo({
      videoId: currentVideoId.value,
      amount: coinDialog.amount
    });
    videoInfo.value.isCoin = true;
    videoInfo.value.coinCount = Number(videoInfo.value.coinCount || 0) + coinDialog.amount;

    let likedByCoin = false;
    if (coinDialog.alsoLike && !videoInfo.value.isLike) {
      try {
        await likeVideo({
          targetId: currentVideoId.value,
          type: 0,
          operation: 1
        });
        videoInfo.value.isLike = true;
        videoInfo.value.likeCount = Number(videoInfo.value.likeCount || 0) + 1;
        likedByCoin = true;
      } catch (error) {
        console.error('投币后点赞失败:', error);
        ElMessage.warning('投币成功，但自动点赞失败');
      }
    }

    coinDialog.visible = false;
    ElMessage.success(likedByCoin ? '投币并点赞成功' : '投币成功');
  } catch (error) {
    console.error('投币失败:', error);
    ElMessage.error(error.message || '投币失败');
  } finally {
    coinDialog.submitting = false;
    actionLoading.coin = false;
  }
}

async function ensureCollectDialogDirectories() {
  const relations = await queryVideoDirectoryRelations(currentVideoId.value);
  return Array.isArray(relations) ? relations : [];
}

async function handleCollect() {
  if (!ensureLoggedIn('登录后可收藏视频')) return;
  if (actionLoading.collect || !currentVideoId.value) return;

  collectDialog.visible = true;
  collectDialog.loading = true;
  collectDialog.directories = [];
  collectDialog.selectedDirectoryIds = [];
  collectDialog.initialSelectedDirectoryIds = [];
  try {
    const directories = await ensureCollectDialogDirectories();
    collectDialog.directories = directories;
    const collectedIds = directories
      .filter(item => item.isCollect)
      .map(item => Number(item.directoryId));
    collectDialog.selectedDirectoryIds = [...collectedIds];
    collectDialog.initialSelectedDirectoryIds = [...collectedIds];
  } catch (error) {
    console.error('加载收藏夹失败:', error);
    ElMessage.error(error.message || '加载收藏夹失败');
    collectDialog.visible = false;
  } finally {
    collectDialog.loading = false;
  }
}

function isCollectDialogSelected(directoryId) {
  return collectDialog.selectedDirectoryIds.includes(Number(directoryId));
}

function toggleCollectDirectorySelection(directoryId) {
  if (collectDialog.submitting) return;
  const normalizedId = Number(directoryId);
  if (isCollectDialogSelected(normalizedId)) {
    collectDialog.selectedDirectoryIds = collectDialog.selectedDirectoryIds.filter(id => id !== normalizedId);
    return;
  }
  collectDialog.selectedDirectoryIds = [...collectDialog.selectedDirectoryIds, normalizedId];
}

function closeCollectDialog() {
  if (collectDialog.submitting) return;
  collectDialog.visible = false;
  collectDialog.directories = [];
  collectDialog.selectedDirectoryIds = [];
  collectDialog.initialSelectedDirectoryIds = [];
}

async function submitCollectSelection() {
  if (actionLoading.collect || !currentVideoId.value) return;

  const initialSelectedDirectoryIds = [...collectDialog.initialSelectedDirectoryIds];
  const selectedDirectoryIds = [...collectDialog.selectedDirectoryIds];
  const initialDirectorySet = new Set(initialSelectedDirectoryIds.map(Number));
  const currentDirectorySet = new Set(selectedDirectoryIds.map(Number));
  const collectDirectoryIdList = [...currentDirectorySet].filter(id => !initialDirectorySet.has(id));
  const removeDirectoryIdList = [...initialDirectorySet].filter(id => !currentDirectorySet.has(id));

  if (collectDirectoryIdList.length === 0 && removeDirectoryIdList.length === 0) {
    closeCollectDialog();
    return;
  }

  closeCollectDialog();
  actionLoading.collect = true;
  collectDialog.submitting = true;
  try {
    const beforeCollect = initialSelectedDirectoryIds.length > 0;
    await collectVideo({
      videoId: currentVideoId.value,
      collectDirectoryIdList,
      removeDirectoryIdList
    });

    const afterCollect = selectedDirectoryIds.length > 0;
    videoInfo.value.isCollect = afterCollect;
    if (!beforeCollect && afterCollect) {
      videoInfo.value.collectCount = Number(videoInfo.value.collectCount || 0) + 1;
    } else if (beforeCollect && !afterCollect) {
      videoInfo.value.collectCount = Math.max(0, Number(videoInfo.value.collectCount || 0) - 1);
    }

    ElMessage.success('收藏夹已更新');
  } catch (error) {
    console.error('更新收藏夹失败:', error);
    ElMessage.error(error.message || '更新收藏夹失败');
  } finally {
    collectDialog.submitting = false;
    actionLoading.collect = false;
  }
}

async function handleFollow() {
  if (!ensureLoggedIn('登录后可关注 UP 主')) return;
  if (isSelfVideo.value || actionLoading.follow || !videoInfo.value.userId) return;
  actionLoading.follow = true;
  const nextIsFollow = !videoInfo.value.isFollow;
  try {
    await followUser({
      followeeId: videoInfo.value.userId,
      operation: nextIsFollow ? 1 : 0
    });
    videoInfo.value.isFollow = nextIsFollow;
    videoInfo.value.fansCount = Math.max(0, Number(videoInfo.value.fansCount || 0) + (nextIsFollow ? 1 : -1));
    ElMessage.success(nextIsFollow ? '关注成功' : '已取消关注');
  } catch (error) {
    console.error('关注失败:', error);
    ElMessage.error(error.message || '关注失败');
  } finally {
    actionLoading.follow = false;
  }
}

async function handleLikeComment(comment) {
  if (!ensureLoggedIn('登录后可点赞评论')) return;
  if (!comment?.commentId) return;
  const nextIsLike = !comment.isLike;
  try {
    await likeComment({
      targetId: comment.commentId,
      type: 1,
      operation: nextIsLike ? 1 : 0
    });
    comment.isLike = nextIsLike;
    comment.likeCount = Math.max(0, Number(comment.likeCount || 0) + (nextIsLike ? 1 : -1));
  } catch (error) {
    console.error('评论点赞失败:', error);
    ElMessage.error(error.message || '评论点赞失败');
  }
}

async function recordShareCount() {
  if (!viewerLoggedIn.value || !currentVideoId.value || actionLoading.share) {
    return;
  }
  actionLoading.share = true;
  try {
    const firstShare = await shareVideo(currentVideoId.value);
    if (firstShare) {
      videoInfo.value.shareCount = Number(videoInfo.value.shareCount || 0) + 1;
    }
  } catch (error) {
    console.error('分享计数上报失败:', error);
  } finally {
    actionLoading.share = false;
  }
}

async function handleShare() {
  const url = window.location.href;
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(url).then(async () => {
      ElMessage.success('链接已复制，快去分享吧');
      await recordShareCount();
    }).catch(() => {
      ElMessage.warning('复制失败，请手动复制地址栏链接');
    });
    return;
  }
  ElMessage.info(url);
  await recordShareCount();
}

async function handleReply(comment, targetReply = null) {
  await startReply(comment, targetReply);
}

function handleReport() {
  if (!ensureLoggedIn('登录后可举报视频')) return;
  reportDialog.visible = true;
  reportDialog.reason = '';
  reportDialog.detail = '';
}

function closeReportDialog() {
  if (reportDialog.submitting) return;
  reportDialog.visible = false;
  reportDialog.reason = '';
  reportDialog.detail = '';
}

async function submitReport() {
  if (!reportDialog.reason.trim()) {
    ElMessage.warning('请选择举报原因');
    return;
  }
  if (!reportDialog.detail.trim()) {
    ElMessage.warning('请填写详细描述');
    return;
  }

  reportDialog.submitting = true;
  try {
    await reportVideo({
      targetType: 2,
      targetId: currentVideoId.value,
      reason: reportDialog.reason,
      detail: reportDialog.detail
    });
    closeReportDialog();
    ElMessage.success('举报提交成功，我们会尽快处理');
  } catch (error) {
    console.error('举报失败:', error);
    ElMessage.error(error.message || '举报提交失败');
  } finally {
    reportDialog.submitting = false;
  }
}

function goToUserProfile() {
  if (!ensureLoggedIn('登录后可查看用户主页')) return;
  if (!videoInfo.value.userId) return;
  router.push(`/user/${videoInfo.value.userId}`);
}

async function handlePlaybackTimeUpdate(payload) {
  const currentSeconds = Math.floor(payload?.currentTime || 0);
  if (currentSeconds <= 0) return;
  await reportProgress(currentSeconds, false);
}

async function handleVideoEnded(payload) {
  const seconds = Math.floor(payload?.duration || payload?.currentTime || 0);
  if (!seconds) return;
  await reportProgress(seconds, true);
}

async function handleSeekCommit(payload) {
  const seconds = Math.floor(payload?.currentTime || 0);
  if (seconds <= 0) return;
  await reportProgress(seconds, true);
}

async function reportProgress(seconds, force) {
  if (!viewerLoggedIn.value) return;
  if (reportInFlight.value || !currentVideoId.value || !seconds) return;
  if (!force && seconds - lastReportedTime.value < 15) return;

  reportInFlight.value = true;
  try {
    await reportVideoProgress({
      videoId: currentVideoId.value,
      lastPlayTime: seconds
    });
    lastReportedTime.value = seconds;
  } catch (error) {
    console.error('上报播放进度失败:', error);
  } finally {
    reportInFlight.value = false;
  }
}

async function flushProgressBeforeLeave() {
  if (!viewerLoggedIn.value) return;
  if (!currentVideoId.value) return;
  const seconds = Math.floor(appVideoPlayerRef.value?.getCurrentTime?.() || 0);
  if (seconds > 0) {
    await reportProgress(seconds, true);
  }
}

function formatCount(count) {
  const value = Number(count || 0);
  if (value >= 100000000) return `${(value / 100000000).toFixed(1)}亿`;
  if (value >= 10000) return `${(value / 10000).toFixed(1)}万`;
  return `${value}`;
}

function formatDuration(seconds) {
  const totalSeconds = Number(seconds || 0);
  if (!totalSeconds) return '00:00';
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const secs = Math.floor(totalSeconds % 60);
  if (hours > 0) {
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }
  return `${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) return dateStr;

  const now = Date.now();
  const diff = now - date.getTime();
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;

  if (diff < hour) {
    return `${Math.max(1, Math.floor(diff / minute))} 分钟前`;
  }
  if (diff < day) {
    return `${Math.floor(diff / hour)} 小时前`;
  }
  if (diff < 30 * day) {
    return `${Math.floor(diff / day)} 天前`;
  }
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
}

function syncViewerAuth(event) {
  const wasLoggedIn = viewerLoggedIn.value;
  viewerLoggedIn.value = event?.detail?.isLoggedIn ?? isUserLoggedIn();
  currentUsername.value = event?.detail?.username ?? getUsername();

  // 如果从游客状态变为登录状态，重新加载页面以获取用户相关数据
  if (!wasLoggedIn && viewerLoggedIn.value) {
    loadPage();
  }
}

function promptLogin(message) {
  openLoginModal({ source: 'video-player' });
  if (message) {
    ElMessage.info(message);
  }
}

function ensureLoggedIn(message) {
  if (viewerLoggedIn.value) return true;
  promptLogin(message);
  return false;
}

function handleCommentFocus() {
  if (!viewerLoggedIn.value) {
    promptLogin('登录后才可以发表评论');
  }
}

async function loadPage() {
  comments.value = [];
  commentTotal.value = 0;
  hasMoreComments.value = true;
  nextCommentCursor.value = null;
  videoInfo.value = createDefaultVideoInfo();
  await loadVideoDetail();
  await loadComments(false);
}

watch(commentSort, () => {
  loadComments(false);
});

watch(commentLoadTriggerRef, () => {
  nextTick(() => {
    setupCommentLoadObserver();
  });
});

watch(
  () => route.params.id,
  async (newId) => {
    const nextId = Number(newId) || 0;
    if (nextId === currentVideoId.value) return;
    await flushProgressBeforeLeave();
    currentVideoId.value = nextId;
    await loadPage();
  }
);

onMounted(() => {
  syncViewerAuth();
  window.addEventListener(USER_AUTH_CHANGE_EVENT, syncViewerAuth);
  loadPage();
});

onBeforeUnmount(() => {
  flushProgressBeforeLeave();
  disconnectCommentLoadObserver();
  window.removeEventListener(USER_AUTH_CHANGE_EVENT, syncViewerAuth);
});
</script>

<style scoped>
.line-clamp-3 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-3 {
  -webkit-line-clamp: 3;
}

.coin-dialog-coin {
  display: flex;
  height: 5.75rem;
  width: 5.75rem;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  border: 2px solid #b4bac3;
  background: radial-gradient(circle at 30% 30%, #ffffff, #d8dbe1 58%, #b9bec7 100%);
  color: #808892;
  font-size: 2rem;
  font-weight: 700;
  box-shadow:
    inset 0 2px 4px rgba(255, 255, 255, 0.8),
    0 10px 18px rgba(148, 163, 184, 0.22);
}

.reply-pagination__item,
.reply-pagination__next,
.reply-pagination__collapse {
  border: none;
  background: transparent;
  padding: 0;
  color: #374151;
  line-height: 1.5;
  transition: color 0.2s ease;
}

.reply-pagination__item:hover,
.reply-pagination__next:hover,
.reply-pagination__collapse:hover {
  color: #00a1d6;
}

.reply-pagination__item--active {
  color: #00a1d6;
  cursor: default;
}

.reply-pagination__item--ellipsis {
  cursor: default;
  color: #9ca3af;
}

.reply-pagination__item:disabled,
.reply-pagination__next:disabled,
.reply-pagination__collapse:disabled {
  cursor: default;
}

.reply-pagination__summary {
  color: #1f2937;
}
</style>
