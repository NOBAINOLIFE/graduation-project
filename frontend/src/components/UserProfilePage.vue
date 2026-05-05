<template>
  <div class="min-h-screen bg-[#f4f5f7]">
    <!-- 顶部导航栏 -->
    <HeaderNav />

    <!-- 顶部Banner区域 -->
    <div class="relative h-40 bg-gradient-to-r from-[#00a1d6] to-[#0090c0]">
      <div class="absolute inset-0 bg-black/10"></div>
      
      <!-- 用户信息在Banner内 -->
      <div class="absolute bottom-4 left-0 right-0 max-w-7xl mx-auto px-4">
        <div class="flex items-end gap-4">
          <!-- 头像 -->
          <div class="flex-shrink-0">
            <div class="w-20 h-20 rounded-full overflow-hidden ring-4 ring-white shadow-lg bg-white">
              <img
                v-if="userInfo?.avatarUrl"
                :src="userInfo.avatarUrl"
                :alt="userInfo.username"
                class="w-full h-full object-cover"
              />
              <div v-else class="w-full h-full bg-gradient-to-br from-[#00a1d6] to-[#0095c8] flex items-center justify-center text-white text-2xl font-bold">
                {{ (userInfo?.username || 'U')[0].toUpperCase() }}
              </div>
            </div>
          </div>

          <!-- 用户名和信息 -->
          <div class="flex-1 pb-1">
            <div class="flex items-center gap-3 mb-1">
              <h1 class="text-xl font-bold text-white drop-shadow-lg">{{ userInfo?.username || '加载中...' }}</h1>
              <span v-if="userInfo?.isBanned" class="rounded-full bg-[#fb7299] px-2.5 py-0.5 text-xs font-medium text-white">已封禁</span>
              <span v-if="userInfo?.isBlack" class="rounded-full bg-slate-900/70 px-2.5 py-0.5 text-xs font-medium text-white">黑名单</span>
            </div>
            <p class="text-white/90 text-sm line-clamp-1 drop-shadow">{{ userInfo?.bio || '这个人很懒，什么都没写~' }}</p>
          </div>

          <!-- 操作按钮 -->
          <div class="flex-shrink-0 flex items-end gap-3 pb-1" v-if="!isSelf">
            <button
              class="px-6 py-2 bg-[#00a1d6] text-white rounded-lg hover:bg-[#0090c0] transition-colors font-medium text-sm h-9 w-18"
              :class="userInfo?.isFollow ? 'bg-[#fb7299] hover:bg-[#fc8bab]' : ''"
              :disabled="isProfileRestricted"
              @click="handleFollow"
            >
              {{ userInfo?.isFollow ? '已关注' : '+ 关注' }}
            </button>
            <button
              class="px-6 py-2 bg-white/20 backdrop-blur text-white rounded-lg hover:bg-white/30 transition-colors border border-white/30 text-sm h-9 w-18"
              :disabled="isProfileRestricted"
              @click="goToPrivateChat"
            >
              发消息
            </button>
            <div class="profile-more relative">
              <button
                class="flex h-9 w-9 items-center justify-center rounded-lg border border-white/30 bg-white/20 text-white backdrop-blur transition-colors hover:bg-white/30"
                type="button"
                aria-label="更多操作"
              >
                <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M12 7a1.75 1.75 0 1 0 0-3.5A1.75 1.75 0 0 0 12 7Zm0 6.75a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm0 6.75a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Z" />
                </svg>
              </button>
              <div class="profile-more-panel absolute right-0 top-full z-20 w-40 pt-2">
                <div class="rounded-2xl border border-white/20 bg-white/95 p-2 shadow-[0_16px_40px_rgba(15,23,42,0.16)]">
                  <button
                    class="flex w-full items-center rounded-xl px-3 py-2 text-sm text-[#111827] transition hover:bg-[#f5f7fb]"
                    type="button"
                    :disabled="isProfileRestricted"
                    @click="openBlockDialog({ targetUserId: userInfo?.userId, username: userInfo?.username })"
                  >
                    加入黑名单
                  </button>
                  <button
                    class="mt-1 flex w-full items-center rounded-xl px-3 py-2 text-sm text-[#111827] transition hover:bg-[#f5f7fb]"
                    type="button"
                    @click="openUserReportDialog"
                  >
                    举报
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 导航标签和统计信息 -->
    <div class="bg-white border-b border-gray-200">
      <div class="max-w-7xl mx-auto px-4">
        <div class="flex items-center justify-between gap-6 py-1">
          <!-- 左侧导航标签 -->
          <div class="flex min-w-0 flex-1 items-center gap-3">
            <div class="flex min-w-0 flex-1 items-center gap-1 overflow-x-auto">
              <button
                v-for="tab in tabs"
                :key="tab.key"
                class="relative flex shrink-0 items-center gap-2 whitespace-nowrap px-4 py-2.5 text-base font-medium transition-colors"
                :class="activeTab === tab.key ? 'text-[#00a1d6]' : 'text-gray-600 hover:text-gray-800'"
                @click="switchContentTab(tab.key)"
              >
                <!-- 图标 -->
                <el-icon v-if="tab.iconType" class="h-[18px] w-[18px] shrink-0" :color="tab.iconColor">
                  <VideoCamera v-if="tab.iconType === 'video'" />
                  <Star v-else-if="tab.iconType === 'star'" />
                </el-icon>
                <span>{{ tab.label }}</span>
                <span v-if="tab.count !== undefined && tab.count !== null" class="text-xs text-gray-400">{{ tab.count }}</span>
                <div v-if="activeTab === tab.key" class="absolute bottom-0 left-2 right-2 h-0.5 bg-[#00a1d6]"></div>
              </button>
            </div>

            <label class="relative w-36 flex-shrink-0 sm:w-44">
              <input
                v-model.trim="videoKeywordInput"
                type="text"
                class="h-8 w-full rounded-full border border-[#d8dee8] bg-[#f8fafc] pl-3 pr-9 text-xs text-[#111827] outline-none transition focus:border-[#00a1d6] focus:bg-white"
                placeholder="搜索投稿标题"
                @keyup.enter="handleVideoKeywordSearch"
              />
              <button
                class="absolute right-1.5 top-1/2 flex h-6 w-6 -translate-y-1/2 items-center justify-center rounded-full text-[#6b7280] transition hover:bg-[#e8f5fb] hover:text-[#00a1d6]"
                @click.prevent="handleVideoKeywordSearch"
              >
                <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m21 21-4.35-4.35m1.85-5.15a7 7 0 1 1-14 0 7 7 0 0 1 14 0Z" />
                </svg>
              </button>
            </label>
          </div>

          <!-- 右侧统计信息 -->
          <div class="flex shrink-0 items-center gap-6 py-2">
            <button
              class="text-center transition-colors"
              :class="activeTab === 'following' ? 'text-[#00a1d6]' : 'hover:text-[#00a1d6]'"
              :disabled="isProfileRestricted"
              @click="switchContentTab('following')"
            >
              <div class="text-xs text-gray-500">关注数</div>
              <div class="text-base font-semibold leading-5 text-gray-800">{{ userInfo?.followNum || 0 }}</div>
            </button>
            <button
              class="text-center transition-colors"
              :class="activeTab === 'fans' ? 'text-[#00a1d6]' : 'hover:text-[#00a1d6]'"
              :disabled="isProfileRestricted"
              @click="switchContentTab('fans')"
            >
              <div class="text-xs text-gray-500">粉丝数</div>
              <div class="text-base font-semibold leading-5 text-gray-800">{{ formatCount(userInfo?.fansNum) }}</div>
            </button>
            <div class="text-center">
              <div class="text-xs text-gray-500">获赞数</div>
              <div class="text-base font-semibold leading-5 text-gray-800">{{ formatCount(userInfo?.likeNum) }}</div>
            </div>
            <div class="text-center">
              <div class="text-xs text-gray-500">播放数</div>
              <div class="text-base font-semibold leading-5 text-gray-800">{{ formatCount(userInfo?.playNum) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="max-w-7xl mx-auto px-4 mt-5 pb-12">
      <div v-if="isProfileRestricted" class="rounded-2xl border border-dashed border-[#dbe3ec] bg-white px-6 py-16 text-center">
        <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-[#eef4f8] text-2xl text-[#94a3b8]">!</div>
        <h2 class="mt-5 text-xl font-semibold text-[#111827]">{{ profileRestrictionTitle }}</h2>
        <p class="mx-auto mt-2 max-w-xl text-sm leading-6 text-[#6b7280]">{{ profileRestrictionDescription }}</p>
      </div>

      <!-- 视频列表 -->
      <div v-else-if="activeTab === 'videos'">
        <div v-if="videoList.length === 0 && !loading" class="flex flex-col items-center justify-center py-20">
          <svg class="w-24 h-24 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
          </svg>
          <p class="text-gray-500 text-lg">{{ appliedVideoKeyword ? `没有找到与“${appliedVideoKeyword}”相关的投稿` : '该空间主人没有投过稿，这里什么都没有...' }}</p>
        </div>

        <div v-else>
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-xl font-bold text-gray-800">视频 <span class="text-sm font-normal text-gray-500">{{ videoList.length }}</span></h2>
            <div class="flex gap-2">
              <button
                v-for="sort in sortOptions"
                :key="sort.key"
                class="px-4 py-1.5 text-sm rounded transition-colors"
                :class="activeSort === sort.key ? 'bg-[#00a1d6] text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
                @click="handleSortChange(sort.key)"
              >
                {{ sort.label }}
              </button>
            </div>
          </div>

          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
            <article
                v-for="video in videoList"
                :key="video.videoId"
                class="group cursor-pointer overflow-hidden rounded-lg bg-transparent"
                @click="goToVideo(video.videoId)"
            >
              <div class="relative aspect-video overflow-hidden rounded-lg bg-[#eef2f6]">
                <img
                    v-if="video.coverUrl"
                    :src="video.coverUrl"
                    :alt="video.title"
                    class="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                    @error="handleImageError"
                />
                <div v-else class="flex h-full items-center justify-center text-4xl text-[#c2cad3]">🎬</div>
                <span class="absolute bottom-2 right-2 text-sm text-white drop-shadow-lg">
                {{ formatDuration(video.duration) }}
              </span>
                <span class="absolute bottom-2 left-2 flex items-center gap-1 text-sm text-white drop-shadow-lg">
                <svg viewBox="0 0 24 24" class="h-3.5 w-3.5 fill-current">
                  <path d="M8 5.14v13.72c0 .75.82 1.23 1.5.86l10.27-5.86a1 1 0 0 0 0-1.72L9.5 4.28A1 1 0 0 0 8 5.14Z"></path>
                </svg>
                {{ formatCount(video.playCount) }}
              </span>
              </div>

              <div class="mt-1">
                <h3 class="line-clamp-2 min-h-[3rem] text-left text-sm font-medium leading-6 text-[#18191c] transition-colors group-hover:text-[#00a1d6]">
                  {{ video.title }}
                </h3>
                <p class="mt-1 truncate text-xs text-[#9499a0]">
                  {{ formatDate(video.createTime) }}
                </p>
              </div>
            </article>
          </div>

          <!-- 加载更多 -->
          <div v-if="hasMore" class="mt-8 text-center">
            <button
              class="px-8 py-2 bg-white border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
              :disabled="loading"
              @click="loadMoreVideos"
            >
              {{ loading ? '加载中...' : '加载更多' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 收藏页面 -->
      <div v-else-if="activeTab === 'favorites'">
        <div v-if="collectionDirectories.length === 0" class="flex flex-col items-center justify-center py-20">
          <svg class="w-24 h-24 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
          </svg>
          <p class="text-gray-500 text-lg">您目前还没有收藏任何视频，快去寻找视频吧</p>
        </div>

        <div v-else class="flex flex-col gap-3 xl:flex-row">
          <!-- 左侧收藏夹列表 -->
          <div class="w-full xl:w-[280px] xl:flex-shrink-0 xl:pr-4">
            <div class="bg-transparent">
              <div class="flex items-center justify-between px-5 py-3">
                <h3 class="text-base font-semibold text-[#1f2937]">我创建的收藏夹</h3>
                <span class="text-xs text-[#94a3b8]">{{ collectionDirectories.length }} 个</span>
              </div>
              <!-- 新建收藏夹按钮 -->
              <button
                class="mx-2 mt-2 flex w-[calc(100%-16px)] items-center justify-center gap-2 rounded-md px-5 py-2.5 text-sm font-medium text-[#00a1d6] transition-colors hover:bg-[#f7fbff]"
                @click="openCreateDialog"
              >
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
                </svg>
                <span>新建收藏夹</span>
              </button>
              <div class="max-h-[680px] overflow-y-auto px-2 py-2">
                <div
                  v-for="dir in collectionDirectories"
                  :key="dir.directoryId"
                  class="relative mb-1 last:mb-0 group/dir"
                >
                  <button
                    class="flex h-10 w-full items-center justify-between rounded-md px-4 text-left transition-all"
                    :class="selectedDirectoryId === dir.directoryId ? 'bg-[#00a1d6] text-white' : 'text-[#334155] hover:bg-[#f8fafc]'"
                    @click="selectDirectory(dir.directoryId)"
                  >
                    <div class="flex items-center gap-2 flex-1 min-w-0">
                      <div class="flex h-5 w-5 flex-shrink-0 items-center justify-center">
                        <svg
                          v-if="dir.isPublic === 1"
                          class="h-5 w-5"
                          :class="selectedDirectoryId === dir.directoryId ? 'text-white' : 'text-[#7c8ea3]'"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M3.75 7.5a2.25 2.25 0 012.25-2.25h4.19c.597 0 1.17.237 1.591.659l1.06 1.06c.422.421.994.659 1.591.659H18A2.25 2.25 0 0120.25 9.75v6.75A2.25 2.25 0 0118 18.75H6A2.25 2.25 0 013.75 16.5V7.5z" />
                        </svg>
                        <svg
                          v-else
                          class="h-5 w-5"
                          :class="selectedDirectoryId === dir.directoryId ? 'text-white' : 'text-[#7c8ea3]'"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M3.75 7.5a2.25 2.25 0 012.25-2.25h4.19c.597 0 1.17.237 1.591.659l1.06 1.06c.422.421.994.659 1.591.659H18A2.25 2.25 0 0120.25 9.75v6.75A2.25 2.25 0 0118 18.75H6A2.25 2.25 0 013.75 16.5V7.5z" />
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M9 13.25h6m-5-2.5v-.5a2 2 0 114 0v.5" />
                          <rect x="9" y="13.25" width="6" height="4.5" rx="1.2" ry="1.2" stroke-width="1.8" />
                        </svg>
                      </div>
                      <span class="truncate text-sm">{{ dir.name }}</span>
                    </div>
                    
                    <!-- 视频数量（默认显示，悬停时隐藏） -->
                    <div class="ml-2 flex h-6 w-8 items-center justify-end">
                      <span 
                        class="text-xs group-hover/dir:hidden"
                        :class="selectedDirectoryId === dir.directoryId ? 'text-white/80' : 'text-[#94a3b8]'"
                      >
                        {{ dir.itemCount || 0 }}
                      </span>
                    
                      <!-- 三点下拉菜单（悬停收藏夹行时显示） -->
                      <el-dropdown 
                        v-if="isSelf"
                        trigger="hover" 
                        @command="(command) => handleDropdownCommand(command, dir)"
                        class="hidden group-hover/dir:block"
                        popper-class="collection-dropdown"
                        :hide-on-click="false"
                        placement="bottom-end"
                      >
                        <button
                          class="rounded-md p-1 transition-colors hover:bg-black/5"
                          @click.stop
                        >
                          <svg class="w-5 h-5" :class="selectedDirectoryId === dir.directoryId ? 'text-white/85' : 'text-gray-500'" fill="currentColor" viewBox="0 0 24 24">
                            <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/>
                          </svg>
                        </button>
                        <template #dropdown>
                          <el-dropdown-menu>
                            <el-dropdown-item command="edit">编辑信息</el-dropdown-item>
                            <el-dropdown-item v-if="!dir.isDefault" command="delete" divided>删除</el-dropdown-item>
                          </el-dropdown-menu>
                        </template>
                      </el-dropdown>
                    </div>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧视频列表 -->
          <div class="min-w-0 flex-1">
            <div v-if="selectedDirectory" class="space-y-2">
              <div class="border-b border-[#cfd8e3] bg-transparent pb-3">
                <div class="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
                  <div class="flex min-w-0 flex-col gap-3 sm:flex-row">
                    <div class="h-24 w-full flex-shrink-0 overflow-hidden rounded-md bg-gray-200 sm:w-[168px]">
                      <img v-if="selectedDirectory.coverUrl" :src="selectedDirectory.coverUrl" class="h-full w-full object-cover" />
                      <div v-else class="flex h-full w-full items-center justify-center bg-gradient-to-br from-[#d7e7f0] via-[#e9eef7] to-[#f7fafc]">
                        <svg class="h-10 w-10 text-[#94a3b8]" fill="currentColor" viewBox="0 0 24 24">
                          <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                        </svg>
                      </div>
                    </div>
                    <div class="min-w-0 flex-1">
                      <div class="mb-1.5 flex flex-wrap items-center gap-2.5">
                        <h2 class="text-2xl font-bold leading-tight text-[#111827]">{{ selectedDirectory.name }}</h2>
                        <span class="rounded-full bg-[#f1f5f9] px-2.5 py-0.5 text-[11px] font-medium text-[#64748b]">
                          {{ selectedDirectory.isPublic === 1 ? '公开' : '私密' }}
                        </span>
                        <span v-if="selectedDirectory.isDefault" class="rounded-full bg-[#e0f7ff] px-2.5 py-0.5 text-[11px] font-medium text-[#0891b2]">
                          默认收藏夹
                        </span>
                      </div>
                      <p class="text-xs text-[#64748b]">视频数 {{ selectedDirectory.itemCount || 0 }}</p>
                      <p class="mt-2 max-w-3xl text-xs leading-5 text-[#475569] line-clamp-3">
                        {{ selectedDirectory.description || '这个收藏夹还没有简介，先把喜欢的视频都放进来吧。' }}
                      </p>
                    </div>
                  </div>

                  <div class="flex flex-wrap items-center gap-3 xl:justify-end">
                    <button
                      v-if="isSelf"
                      class="rounded-md border border-[#dbe3ec] bg-transparent px-5 py-2 text-sm font-medium text-[#334155] transition-colors hover:border-[#00a1d6] hover:text-[#00a1d6] disabled:cursor-not-allowed disabled:opacity-40"
                      :disabled="batchActionLoading"
                      @click="toggleBatchMode"
                    >
                      {{ isBatchMode ? '返回' : '批量操作' }}
                    </button>
                  </div>
                </div>
              </div>

              <div class="bg-transparent">
                <div class="mb-3 flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
                  <div v-if="!isBatchMode" class="flex flex-wrap items-center gap-3">
                    <button
                      v-for="sort in favoriteSortOptions"
                      :key="sort.key"
                      class="rounded-md px-5 py-2.5 text-sm font-medium transition-all"
                      :class="activeFavoriteSort === sort.key ? 'bg-[#00a1d6] text-white' : 'bg-transparent text-[#475569] hover:bg-[#eef5fb]'"
                      @click="handleFavoriteSortChange(sort.key)"
                    >
                      {{ sort.label }}
                    </button>
                  </div>

                  <div v-else class="flex flex-wrap items-center gap-5">
                    <label class="flex cursor-pointer items-center gap-3 text-[15px] font-medium text-[#1f2937]">
                      <input
                        type="checkbox"
                        class="h-5 w-5 rounded border border-[#cfd8e3] text-[#00a1d6] focus:ring-[#00a1d6]"
                        :checked="isAllFavoriteSelected"
                        @change="toggleSelectAllFavorites"
                      />
                      <span>全选</span>
                    </label>
                    <span class="text-[15px] text-[#64748b]">已选择 {{ selectedFavoriteCount }} 个视频</span>
                  </div>

                  <div v-if="isBatchMode" class="flex flex-wrap items-center gap-3">
                    <button
                      class="rounded-md border border-[#dbe3ec] bg-transparent px-5 py-2.5 text-sm font-medium text-[#334155] transition-colors hover:border-[#00a1d6] hover:text-[#00a1d6] disabled:cursor-not-allowed disabled:opacity-40"
                      :disabled="batchActionLoading"
                      @click="handleClearInvalidItems"
                    >
                      清除失效内容
                    </button>
                    <button
                      class="rounded-md border border-[#dbe3ec] bg-transparent px-5 py-2.5 text-sm font-medium text-[#334155] transition-colors hover:border-[#f97316] hover:text-[#f97316] disabled:cursor-not-allowed disabled:opacity-40"
                      :disabled="batchActionLoading || selectedFavoriteCount === 0"
                      @click="handleBatchCancelCollection"
                    >
                      取消收藏
                    </button>
                    <button
                      class="rounded-md border border-[#dbe3ec] bg-transparent px-5 py-2.5 text-sm font-medium text-[#334155] transition-colors hover:border-[#00a1d6] hover:text-[#00a1d6] disabled:cursor-not-allowed disabled:opacity-40"
                      :disabled="batchActionLoading || selectedFavoriteCount === 0"
                      @click="openBatchTargetDialog('copy')"
                    >
                      复制至
                    </button>
                    <button
                      class="rounded-md border border-[#dbe3ec] bg-transparent px-5 py-2.5 text-sm font-medium text-[#334155] transition-colors hover:border-[#00a1d6] hover:text-[#00a1d6] disabled:cursor-not-allowed disabled:opacity-40"
                      :disabled="batchActionLoading || selectedFavoriteCount === 0"
                      @click="openBatchTargetDialog('move')"
                    >
                      移动至
                    </button>
                  </div>
                </div>

                <div v-if="favoriteVideos.length === 0 && !favoriteLoading" class="bg-[#f8fafc] px-8 py-16 text-center">
                  <svg class="mx-auto mb-4 h-16 w-16 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                  </svg>
                  <p class="text-[#64748b]">这个收藏夹还没有视频</p>
                </div>

                <div v-else class="grid grid-cols-2 gap-x-4 gap-y-6 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
                  <div
                    v-for="video in favoriteVideos"
                    :key="video.videoId"
                    class="group border border-transparent bg-transparent transition-all"
                    :class="isBatchMode && isFavoriteSelected(video.videoId) ? 'bg-[#f0fbff]' : ''"
                    @click="handleFavoriteCardClick(video.videoId)"
                  >
                    <div class="relative aspect-video overflow-hidden rounded-md bg-gray-200">
                      <img v-if="video.coverUrl" :src="video.coverUrl" class="h-full w-full object-cover transition-transform duration-300 group-hover:scale-[1.03]" />
                      <div v-else class="h-full w-full bg-gradient-to-br from-gray-300 to-gray-400"></div>
                      <button
                        v-if="isBatchMode"
                        class="absolute left-3 top-3 flex h-7 w-7 items-center justify-center border border-white/80 bg-black/25 backdrop-blur-sm transition"
                        :class="isFavoriteSelected(video.videoId) ? 'border-[#00a1d6] bg-[#00a1d6]' : 'hover:bg-black/40'"
                        @click.stop="toggleFavoriteSelection(video.videoId)"
                      >
                        <svg v-if="isFavoriteSelected(video.videoId)" class="h-4 w-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                        </svg>
                      </button>
                      <div class="absolute bottom-1.5 left-2 flex items-center gap-1 text-xs text-white [text-shadow:0_1px_2px_rgba(0,0,0,0.85)]">
                        <svg class="h-3.5 w-3.5" fill="currentColor" viewBox="0 0 24 24">
                          <path d="M8 5v14l11-7z" />
                        </svg>
                        <span>{{ formatCount(video.playCount) }}</span>
                      </div>
                      <div class="absolute bottom-1.5 right-2 text-xs text-white [text-shadow:0_1px_2px_rgba(0,0,0,0.85)]">
                        {{ formatDuration(video.duration) }}
                      </div>
                    </div>
                    <div class="px-0 py-3">
                      <h3 class="mb-2 text-[13px] font-medium leading-5 text-[#0f172a] line-clamp-2 group-hover:text-[#00a1d6]">{{ video.title }}</h3>
                      <div class="text-xs text-[#94a3b8] truncate">
                        {{ video.username || '未知作者' }} · 收藏于 {{ formatDate(video.collectTime || video.createTime) }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="border border-[#e7edf3] bg-transparent p-12 text-center text-[#64748b]">
              请选择一个收藏夹查看内容
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="isRelationTab" class="flex flex-col gap-6 xl:flex-row">
        <aside class="w-full xl:w-[220px] xl:flex-shrink-0">
            <div>
              <div class="mb-2 flex items-center justify-between">
                <h3 class="text-[15px] font-semibold text-[#111827]">{{ followingSectionTitle }}</h3>
              </div>
              <button
                class="flex w-full items-center justify-between rounded-2xl px-5 py-4 text-left text-[15px] font-medium transition-colors"
                :class="activeTab === 'following' ? 'bg-[#18a8df] text-white' : 'text-[#111827] hover:bg-[#f4f8fb]'"
                @click="switchContentTab('following')"
              >
                <span>关注列表</span>
                <span>{{ userInfo?.followNum || 0 }}</span>
              </button>
            </div>

            <div class="mt-2 border-t border-gray-300 pt-8">
              <div class="mb-2 flex items-center justify-between">
                <h3 class="text-[15px] font-semibold text-[#111827]">{{ fansSectionTitle }}</h3>
              </div>
              <button
                class="flex w-full items-center justify-between rounded-2xl px-5 py-4 text-left text-[15px] font-medium transition-colors"
                :class="activeTab === 'fans' ? 'bg-[#18a8df] text-white' : 'text-[#111827] hover:bg-[#f4f8fb]'"
                @click="switchContentTab('fans')"
              >
                <span>粉丝列表</span>
                <span>{{ userInfo?.fansNum || 0 }}</span>
              </button>
            </div>
        </aside>

        <div class="min-w-0 flex-1">
          <div class="mb-6 flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
            <div>
              <h2 class="text-[25px] leading-none text-[#18191c]">{{ relationTitle }}</h2>
            </div>

            <div class="w-full xl:w-[320px]">
              <label class="relative block">
                <input
                  v-model.trim="relationKeyword"
                  type="text"
                  class="h-12 w-full rounded-2xl border border-[#d8dee8] bg-white pl-5 pr-14 text-sm text-[#111827] outline-none transition focus:border-[#18a8df] focus:ring-4 focus:ring-[#18a8df]/10"
                  placeholder="输入关键词"
                />
                <svg class="pointer-events-none absolute right-5 top-1/2 h-6 w-6 -translate-y-1/2 text-[#111827]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m21 21-4.35-4.35m1.85-5.15a7 7 0 1 1-14 0 7 7 0 0 1 14 0Z" />
                </svg>
              </label>
            </div>
          </div>

          <div v-if="relationLoading" class="flex min-h-[420px] items-center justify-center text-center">
            <h3 class="text-xl font-semibold text-[#111827]">
              拼命加载中...
            </h3>
          </div>

          <div
            v-else-if="filteredRelationList.length === 0"
            class="flex min-h-[420px] flex-col items-center justify-center rounded-[32px] border border-dashed border-[#d7dee8] bg-white/70 px-6 text-center"
          >
            <div class="flex h-20 w-20 items-center justify-center rounded-full bg-[#eef6fb]">
              <svg class="h-10 w-10 text-[#8fa3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6" d="M17 20.5a8.38 8.38 0 0 0-10 0m10 0a8.5 8.5 0 1 0-10 0m10 0H7m8-11a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
              </svg>
            </div>
            <h3 class="mt-5 text-2xl font-semibold text-[#111827]">
              {{ relationKeyword ? '没有找到匹配的用户' : emptyRelationTitle }}
            </h3>
            <p class="mt-2 text-sm text-[#6b7280]">
              {{ relationKeyword ? '换个用户昵称试试。' : emptyRelationDescription }}
            </p>
          </div>

          <div v-else class="grid gap-x-6 gap-y-6 md:grid-cols-2 xl:grid-cols-3">
            <article
              v-for="item in filteredRelationList"
              :key="item.userId"
              class="rounded-2xl bg-white p-4 shadow-[0_8px_20px_rgba(15,23,42,0.06)] transition-transform duration-200 hover:-translate-y-1"
            >
              <div class="flex items-center gap-4">
                <button
                  class="relative h-[72px] w-[72px] flex-shrink-0 overflow-hidden rounded-full bg-[#edf2f7]"
                  @click="goToUserProfile(item.userId)"
                >
                  <img
                    v-if="item.avatarUrl"
                    :src="item.avatarUrl"
                    :alt="item.username"
                    class="h-full w-full object-cover"
                    @error="handleImageError"
                  />
                  <div v-else class="flex h-full w-full items-center justify-center bg-gradient-to-br from-[#18a8df] to-[#0f8fc5] text-2xl font-semibold text-white">
                    {{ getNameInitial(item.username) }}
                  </div>
                </button>

                <div class="min-w-0 flex-1">
                  <button
                    class="max-w-full truncate text-left text-base font-semibold text-[#18191c] transition hover:text-[#ff6b8f]"
                    @click="goToUserProfile(item.userId)"
                  >
                    {{ item.username || '未命名用户' }}
                  </button>
                  <p class="mt-1 line-clamp-2 text-sm leading-6 text-[#6b7280]">
                    {{ item.bio || '这个人很懒，什么都没写。' }}
                  </p>

                  <div class="mt-3 flex items-center gap-2">
                    <button
                      v-if="!isCurrentUser(item.userId)"
                      class="inline-flex min-w-[100px] items-center justify-center rounded-xl border px-4 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-60"
                      :class="getRelationButtonClass(item)"
                      :disabled="relationFollowLoadingMap[item.userId]"
                      @click="toggleRelationFollow(item)"
                    >
                      {{ relationFollowLoadingMap[item.userId] ? '处理中...' : getRelationButtonLabel(item) }}
                    </button>
                    <span v-else class="text-xs text-[#94a3b8]">这是你自己</span>
                  </div>
                </div>
              </div>
            </article>
          </div>
        </div>
      </div>
    </div>
    <div
      v-if="showBatchTargetDialog"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/45 px-4"
      @click="closeBatchTargetDialog"
    >
      <div class="w-full max-w-[520px] rounded-[28px] bg-white shadow-[0_24px_60px_rgba(15,23,42,0.2)]" @click.stop>
        <div class="flex items-center justify-between border-b border-[#edf2f7] px-6 py-5">
          <div>
            <h3 class="text-lg font-bold text-[#0f172a]">{{ batchTargetDialogTitle }}</h3>
            <p class="mt-1 text-sm text-[#64748b]">已选择 {{ selectedFavoriteCount }} 个视频</p>
          </div>
          <button class="text-2xl leading-none text-[#94a3b8] transition-colors hover:text-[#475569]" @click="closeBatchTargetDialog">×</button>
        </div>

        <div class="max-h-[360px] space-y-3 overflow-y-auto px-6 py-5">
          <label
            v-for="dir in batchTargetDirectories"
            :key="dir.directoryId"
            class="flex cursor-pointer items-center justify-between rounded-2xl border px-4 py-4 transition-all"
            :class="batchTargetDirectoryId === dir.directoryId ? 'border-[#00a1d6] bg-[#f0fbff]' : 'border-[#e5ebf2] hover:border-[#c8d7e6] hover:bg-[#f8fafc]'"
          >
            <div class="flex min-w-0 items-center gap-3">
              <input
                v-model="batchTargetDirectoryId"
                type="radio"
                class="h-4 w-4 text-[#00a1d6] focus:ring-[#00a1d6]"
                :value="dir.directoryId"
              />
              <div class="h-12 w-12 overflow-hidden rounded-xl bg-gray-200">
                <img v-if="dir.coverUrl" :src="dir.coverUrl" class="h-full w-full object-cover" />
                <div v-else class="flex h-full w-full items-center justify-center bg-gradient-to-br from-gray-300 to-gray-400">
                  <svg class="h-5 w-5 text-gray-500" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                  </svg>
                </div>
              </div>
              <div class="min-w-0">
                <div class="truncate text-sm font-semibold text-[#0f172a]">{{ dir.name }}</div>
                <div class="mt-1 text-xs text-[#94a3b8]">{{ dir.itemCount || 0 }} 个视频</div>
              </div>
            </div>
            <span v-if="dir.isDefault" class="rounded-full bg-[#e0f7ff] px-3 py-1 text-xs font-medium text-[#0891b2]">默认</span>
          </label>
        </div>

        <div class="flex items-center justify-end gap-3 border-t border-[#edf2f7] px-6 py-4">
          <button
            class="rounded-2xl border border-[#dbe3ec] bg-white px-5 py-2.5 text-sm font-medium text-[#475569] transition-colors hover:border-[#94a3b8] hover:text-[#1f2937]"
            @click="closeBatchTargetDialog"
          >
            取消
          </button>
          <button
            class="rounded-2xl bg-[linear-gradient(135deg,#00a1d6_0%,#35c3f2_100%)] px-5 py-2.5 text-sm font-semibold text-white shadow-[0_14px_28px_rgba(0,161,214,0.22)] transition-transform hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="!batchTargetDirectoryId || batchSubmitting || selectedFavoriteCount === 0"
            @click="submitBatchTargetOperation"
          >
            {{ batchSubmitting ? '处理中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="blockDialog.visible"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4"
      @click="closeBlockDialog"
    >
      <div class="w-full max-w-[520px] rounded-[28px] bg-white shadow-[0_24px_60px_rgba(15,23,42,0.2)]" @click.stop>
        <div class="border-b border-[#edf2f7] px-6 py-5">
          <h3 class="text-lg font-bold text-[#0f172a]">加入黑名单</h3>
        </div>
        <div class="px-6 py-6">
          <p class="text-sm leading-7 text-[#475569]">
            加入黑名单后，将自动解除对该用户的关注关系，并且禁止该用户与我互动或查看我的空间
          </p>
        </div>
        <div class="flex items-center justify-end gap-3 border-t border-[#edf2f7] px-6 py-4">
          <button
            class="rounded-2xl border border-[#dbe3ec] bg-white px-5 py-2.5 text-sm font-medium text-[#475569] transition-colors hover:border-[#94a3b8] hover:text-[#1f2937]"
            @click="closeBlockDialog"
          >
            取消
          </button>
          <button
            class="rounded-2xl bg-[#00a1d6] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[#0090c0] disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="blockDialog.submitting"
            @click="confirmBlockUser"
          >
            {{ blockDialog.submitting ? '处理中...' : '确认加入' }}
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="reportDialog.visible"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4"
      @click="closeReportDialog"
    >
      <div class="w-full max-w-3xl rounded-[28px] bg-white px-8 py-6 shadow-[0_28px_80px_rgba(15,23,42,0.28)]" @click.stop>
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-xl font-semibold text-slate-900">{{ reportDialog.title }}</h3>
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
            @click="submitUserReport"
          >
            {{ reportDialog.submitting ? '提交中...' : '提交' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 新建/编辑收藏夹对话框 -->
    <div v-if="showCreateDialog || showEditDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click="closeDialog">
      <div class="bg-white rounded-lg shadow-xl w-[500px] max-w-[90vw] max-h-[90vh] overflow-y-auto" @click.stop>
        <div class="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
          <h3 class="text-lg font-bold text-gray-800">{{ showEditDialog ? '编辑收藏夹' : '新建收藏夹' }}</h3>
          <button class="text-gray-400 hover:text-gray-600" @click="closeDialog">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="px-6 py-6 space-y-6">
          <!-- 封面上传 -->
          <div class="flex justify-center">
            <div 
              class="w-40 h-28 bg-gray-100 rounded-lg flex items-center justify-center cursor-pointer hover:bg-gray-200 transition-colors overflow-hidden relative"
              @click="$refs.coverInput.click()"
            >
              <img v-if="coverPreview" :src="coverPreview" class="w-full h-full object-cover" />
              <div v-else class="flex flex-col items-center text-gray-400">
                <svg class="w-8 h-8 mb-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                </svg>
              </div>
              <input
                ref="coverInput"
                type="file"
                accept="image/*"
                class="hidden"
                @change="handleCoverChange"
              />
            </div>
          </div>
          
          <!-- 名称 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">
              名称 <span class="text-red-500">*</span>
            </label>
            <div class="relative">
              <input
                v-model="directoryForm.name"
                type="text"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#00a1d6] pr-16"
                placeholder="快来给你的收藏夹命名吧"
                maxlength="20"
                @input="nameLength = directoryForm.name.length"
              />
              <span class="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-gray-400">{{ nameLength }}/20</span>
            </div>
          </div>
          
          <!-- 公开开关 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">公开</label>
            <label class="relative inline-flex items-center cursor-pointer">
              <input
                v-model="directoryForm.isPublic"
                type="checkbox"
                :true-value="1"
                :false-value="0"
                class="sr-only peer"
              />
              <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-[#00a1d6]/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[#00a1d6]"></div>
            </label>
          </div>
          
          <!-- 简介 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">简介</label>
            <div class="relative">
              <textarea
                v-model="directoryForm.description"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#00a1d6] resize-none"
                rows="4"
                placeholder="可以简单描述下你的收藏夹"
                maxlength="200"
                @input="descLength = directoryForm.description.length"
              ></textarea>
              <span class="absolute right-3 bottom-3 text-sm text-gray-400">{{ descLength }}/200</span>
            </div>
          </div>
        </div>
        <div class="px-6 py-4 border-t border-gray-100 flex justify-end gap-3">
          <button
            class="flex-1 px-4 py-2 bg-white border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
            @click="closeDialog"
          >
            取消
          </button>
          <button
            class="flex-1 px-4 py-2 bg-[#89d3f7] text-white rounded-lg hover:bg-[#6bc5f3] transition-colors disabled:opacity-50"
            :disabled="uploading || !directoryForm.name.trim()"
            @click="handleSubmitDirectory"
          >
            {{ uploading ? '上传中...' : (showEditDialog ? '保存' : '创建') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  getUserInfo,
  searchVideos,
  listCollectionDirectories,
  listCollectionItems,
  createCollectionDirectory,
  updateCollectionDirectory,
  deleteCollectionDirectory,
  batchOperateCollectionItems,
  clearInvalidCollectionItems,
  uploadImage,
  listFansUsers,
  listFollowUsers
} from '../api/user';
import { blockUser, followUser, submitReport } from '../api/video';
import { getUserId } from '../utils/auth';
import HeaderNav from './HeaderNav.vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { VideoCamera, Star } from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();

const PROFILE_TAB_KEYS = ['videos', 'favorites', 'following', 'fans'];

const userId = ref(route.params.userId || getUserId());
const userInfo = ref(null);
const activeTab = ref(resolveActiveTab(route.query.tab));
const activeSort = ref(2); // 2-最新发布
const videoList = ref([]);
const loading = ref(false);
const pageNum = ref(1);
const hasMore = ref(true);
const videoKeywordInput = ref('');
const appliedVideoKeyword = ref('');
const pendingVideoReload = ref(false);
const relationKeyword = ref('');
const relationLoading = ref(false);
const followingUsers = ref([]);
const fanUsers = ref([]);
const relationLoadedMap = ref({
  following: false,
  fans: false
});
const relationFollowLoadingMap = ref({});

const isSelf = computed(() => {
  const currentUserId = getUserId();
  return String(userId.value) === String(currentUserId);
});
const isProfileRestricted = computed(() => {
  return !isSelf.value && (Boolean(userInfo.value?.isBanned) || Boolean(userInfo.value?.isBlack));
});
const profileRestrictionTitle = computed(() => {
  if (userInfo.value?.isBanned) {
    return '该用户已被封禁';
  }
  if (userInfo.value?.isBlack) {
    return '无法查看该用户主页';
  }
  return '当前主页暂不可见';
});
const profileRestrictionDescription = computed(() => {
  if (userInfo.value?.isBanned) {
    return '该账号因违反社区规范已被封禁，主页内容和互动功能暂不可用。';
  }
  if (userInfo.value?.isBlack) {
    return '由于存在黑名单关系，你暂时无法查看该用户内容，也无法关注或发送私信。';
  }
  return '请稍后再试。';
});
const isRelationTab = computed(() => activeTab.value === 'following' || activeTab.value === 'fans');
const favoriteCount = ref(0);
const collectionDirectories = ref([]);
const selectedDirectoryId = ref(null);
const favoriteVideos = ref([]);
const favoriteLoading = ref(false);
const activeFavoriteSort = ref(1); // 1-最近收藏
const isBatchMode = ref(false);
const selectedFavoriteVideoIds = ref([]);
const batchActionLoading = ref(false);
const showBatchTargetDialog = ref(false);
const batchDialogOperation = ref(null);
const batchTargetDirectoryId = ref(null);
const batchSubmitting = ref(false);
const showCreateDialog = ref(false);
const showEditDialog = ref(false);
const editingDirectory = ref(null);

// 新建/编辑表单
const directoryForm = ref({
  name: '',
  description: '',
  coverUrl: '',
  isPublic: 0
});
const coverPreview = ref('');
const coverFile = ref(null);
const nameLength = ref(0);
const descLength = ref(0);
const uploading = ref(false);
const blockDialog = reactive({
  visible: false,
  submitting: false,
  targetUserId: null,
  username: ''
});
const reportDialog = reactive({
  visible: false,
  submitting: false,
  targetType: 1,
  targetId: null,
  title: '举报用户',
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

const selectedDirectory = computed(() => {
  return collectionDirectories.value.find(dir => dir.directoryId === selectedDirectoryId.value) || null;
});
const selectedFavoriteCount = computed(() => selectedFavoriteVideoIds.value.length);
const isAllFavoriteSelected = computed(() => {
  return favoriteVideos.value.length > 0 && favoriteVideos.value.every(video => selectedFavoriteVideoIds.value.includes(video.videoId));
});
const batchTargetDirectories = computed(() => {
  return collectionDirectories.value.filter(dir => dir.directoryId !== selectedDirectoryId.value);
});
const batchTargetDialogTitle = computed(() => {
  return batchDialogOperation.value === 'move' ? '移动到收藏夹' : '复制到收藏夹';
});
const currentRelationList = computed(() => {
  return activeTab.value === 'fans' ? fanUsers.value : followingUsers.value;
});
const filteredRelationList = computed(() => {
  const keyword = relationKeyword.value.trim().toLowerCase();
  if (!keyword) {
    return currentRelationList.value;
  }
  return currentRelationList.value.filter((item) => {
    const username = String(item.username || '').toLowerCase();
    return username.includes(keyword);
  });
});
const followingSectionTitle = computed(() => {
  return isSelf.value ? '我的关注' : `${userInfo.value?.username || 'TA'}的关注`;
});
const fansSectionTitle = computed(() => {
  return isSelf.value ? '我的粉丝' : `${userInfo.value?.username || 'TA'}的粉丝`;
});
const relationTitle = computed(() => {
  if (activeTab.value === 'fans') {
    return isSelf.value ? '我的粉丝' : `${userInfo.value?.username || 'TA'}的粉丝`;
  }
  return isSelf.value ? '全部关注' : `${userInfo.value?.username || 'TA'}的关注`;
});
const relationTotalCount = computed(() => currentRelationList.value.length);
const emptyRelationTitle = computed(() => {
  if (activeTab.value === 'fans') {
    return isSelf.value ? '你还没有粉丝' : 'TA还没有粉丝';
  }
  return isSelf.value ? '你还没有关注任何人' : 'TA还没有关注任何人';
});
const emptyRelationDescription = computed(() => {
  if (activeTab.value === 'fans') {
    return '等有人关注后，这里就会热闹起来。';
  }
  return '去发现感兴趣的创作者吧。';
});

const tabs = computed(() => [
  { key: 'videos', label: '投稿', count: userInfo.value?.videoNum, iconType: 'video', iconColor: '#00a1d6' },
  { key: 'favorites', label: '收藏', count: favoriteCount.value, iconType: 'star', iconColor: '#f59e0b' }
]);

const sortOptions = [
  { key: 2, label: '最新发布' },
  { key: 1, label: '最多播放' },
  { key: 3, label: '最多收藏' }
];

const favoriteSortOptions = [
  { key: 1, label: '最近收藏' },
  { key: 2, label: '最多播放' },
  { key: 3, label: '最近投稿' }
];

function resolveActiveTab(tab) {
  return PROFILE_TAB_KEYS.includes(tab) ? tab : 'videos';
}

function goHome() {
  router.push('/');
}

function resetRestrictedProfileContent() {
  videoList.value = [];
  collectionDirectories.value = [];
  favoriteVideos.value = [];
  followingUsers.value = [];
  fanUsers.value = [];
  favoriteCount.value = 0;
  selectedDirectoryId.value = null;
  hasMore.value = false;
  relationLoadedMap.value = {
    following: false,
    fans: false
  };
  exitBatchMode();
}

// 加载用户信息
async function loadUserInfo() {
  try {
    userInfo.value = await getUserInfo(userId.value);
    if (isProfileRestricted.value) {
      resetRestrictedProfileContent();
    }
  } catch (error) {
    console.error('加载用户信息失败:', error);
  }
}

// 加载用户视频
async function loadUserVideos(isRefresh = false) {
  if (loading.value || isProfileRestricted.value) return;
  
  try {
    loading.value = true;
    
    if (isRefresh) {
      pageNum.value = 1;
      videoList.value = [];
      hasMore.value = true;
    }

    const result = await searchVideos({
      userId: userId.value,
      keyword: appliedVideoKeyword.value || undefined,
      pageNum: pageNum.value,
      pageSize: 20,
      sortType: activeSort.value
    });

    if (result && result.records) {
      if (isRefresh) {
        videoList.value = result.records;
      } else {
        videoList.value = [...videoList.value, ...result.records];
      }
      
      hasMore.value = videoList.value.length < result.total;
    }
  } catch (error) {
    console.error('加载视频列表失败:', error);
  } finally {
    loading.value = false;
  }
}

// 加载收藏夹列表
async function loadCollectionDirectories() {
  if (isProfileRestricted.value) {
    resetRestrictedProfileContent();
    return;
  }
  try {
    const directories = await listCollectionDirectories(userId.value);
    collectionDirectories.value = directories || [];
    
    // 计算总收藏数
    favoriteCount.value = collectionDirectories.value.reduce((sum, dir) => sum + (dir.itemCount || 0), 0);
    
    if (collectionDirectories.value.length === 0) {
      selectedDirectoryId.value = null;
      favoriteVideos.value = [];
      exitBatchMode();
      return;
    }

    const hasSelectedDirectory = collectionDirectories.value.some(dir => dir.directoryId === selectedDirectoryId.value);
    if (!hasSelectedDirectory) {
      selectedDirectoryId.value = collectionDirectories.value[0].directoryId;
    }

    await loadFavoriteVideos();
  } catch (error) {
    console.error('加载收藏夹列表失败:', error);
  }
}

// 加载收藏夹内视频
async function loadFavoriteVideos() {
  if (!selectedDirectoryId.value || favoriteLoading.value || isProfileRestricted.value) return;
  
  try {
    favoriteLoading.value = true;
    const videos = await listCollectionItems(selectedDirectoryId.value, activeFavoriteSort.value);
    favoriteVideos.value = videos || [];
    const visibleVideoIds = new Set(favoriteVideos.value.map(video => video.videoId));
    selectedFavoriteVideoIds.value = selectedFavoriteVideoIds.value.filter(videoId => visibleVideoIds.has(videoId));
  } catch (error) {
    console.error('加载收藏视频失败:', error);
  } finally {
    favoriteLoading.value = false;
  }
}

async function loadRelationList(type = activeTab.value, forceRefresh = false) {
  if (!['following', 'fans'].includes(type)) {
    return;
  }
  if (relationLoading.value || isProfileRestricted.value) {
    return;
  }

  try {
    relationLoading.value = true;
    const list = type === 'fans'
      ? await listFansUsers(userId.value)
      : await listFollowUsers(userId.value);
    if (type === 'fans') {
      fanUsers.value = Array.isArray(list) ? list : [];
    } else {
      followingUsers.value = Array.isArray(list) ? list : [];
    }
    relationLoadedMap.value[type] = true;
  } catch (error) {
    console.error('加载关系列表失败:', error);
    ElMessage.error(error.message || '加载关系列表失败');
  } finally {
    relationLoading.value = false;
  }
}

// 切换收藏夹
function selectDirectory(directoryId) {
  if (selectedDirectoryId.value === directoryId) return;
  selectedDirectoryId.value = directoryId;
  exitBatchMode();
  loadFavoriteVideos();
}

// 收藏夹排序切换
function handleFavoriteSortChange(sortType) {
  activeFavoriteSort.value = sortType;
  loadFavoriteVideos();
}

function resetBatchSelection() {
  selectedFavoriteVideoIds.value = [];
}

function exitBatchMode() {
  isBatchMode.value = false;
  resetBatchSelection();
}

function toggleBatchMode() {
  if (!isSelf.value) return;
  if (isBatchMode.value) {
    exitBatchMode();
    return;
  }
  isBatchMode.value = true;
  resetBatchSelection();
}

function isFavoriteSelected(videoId) {
  return selectedFavoriteVideoIds.value.includes(videoId);
}

function toggleFavoriteSelection(videoId) {
  if (!isBatchMode.value) return;
  if (isFavoriteSelected(videoId)) {
    selectedFavoriteVideoIds.value = selectedFavoriteVideoIds.value.filter(id => id !== videoId);
    return;
  }
  selectedFavoriteVideoIds.value = [...selectedFavoriteVideoIds.value, videoId];
}

function handleFavoriteCardClick(videoId) {
  if (isBatchMode.value) {
    toggleFavoriteSelection(videoId);
    return;
  }
  goToVideo(videoId);
}

function toggleSelectAllFavorites() {
  if (favoriteVideos.value.length === 0) return;
  if (isAllFavoriteSelected.value) {
    selectedFavoriteVideoIds.value = [];
    return;
  }
  selectedFavoriteVideoIds.value = favoriteVideos.value.map(video => video.videoId);
}

function openBatchTargetDialog(operation) {
  if (selectedFavoriteVideoIds.value.length === 0) {
    ElMessage.warning('请先选择视频');
    return;
  }
  if (batchTargetDirectories.value.length === 0) {
    ElMessage.warning('没有可用的目标收藏夹');
    return;
  }
  batchDialogOperation.value = operation;
  batchTargetDirectoryId.value = batchTargetDirectories.value[0].directoryId;
  showBatchTargetDialog.value = true;
}

function closeBatchTargetDialog() {
  if (batchSubmitting.value) return;
  showBatchTargetDialog.value = false;
  batchDialogOperation.value = null;
  batchTargetDirectoryId.value = null;
}

async function handleClearInvalidItems() {
  if (!selectedDirectoryId.value || batchActionLoading.value) return;

  try {
    batchActionLoading.value = true;
    const affected = await clearInvalidCollectionItems(selectedDirectoryId.value);
    await loadCollectionDirectories();
    ElMessage.success(affected > 0 ? `已清除 ${affected} 个失效内容` : '没有可清除的失效内容');
  } catch (error) {
    console.error('清除失效内容失败:', error);
    ElMessage.error('清除失效内容失败：' + error.message);
  } finally {
    batchActionLoading.value = false;
  }
}

async function handleBatchCancelCollection() {
  if (selectedFavoriteVideoIds.value.length === 0 || !selectedDirectoryId.value || batchActionLoading.value) {
    if (selectedFavoriteVideoIds.value.length === 0) {
      ElMessage.warning('请先选择视频');
    }
    return;
  }

  try {
    await ElMessageBox.confirm(`确定要取消收藏选中的 ${selectedFavoriteVideoIds.value.length} 个视频吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });

    batchActionLoading.value = true;
    const affected = await batchOperateCollectionItems({
      sourceDirectoryId: selectedDirectoryId.value,
      videoIds: selectedFavoriteVideoIds.value,
      operation: 1
    });
    resetBatchSelection();
    await loadCollectionDirectories();
    ElMessage.success(`已取消收藏 ${affected} 个视频`);
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量取消收藏失败:', error);
      ElMessage.error('批量取消收藏失败：' + error.message);
    }
  } finally {
    batchActionLoading.value = false;
  }
}

async function submitBatchTargetOperation() {
  if (!selectedDirectoryId.value || !batchTargetDirectoryId.value || !batchDialogOperation.value || batchSubmitting.value || selectedFavoriteVideoIds.value.length === 0) return;

  try {
    batchSubmitting.value = true;
    const actionText = batchDialogOperation.value === 'move' ? '移动' : '复制';
    const operation = batchDialogOperation.value === 'move' ? 3 : 2;
    const affected = await batchOperateCollectionItems({
      sourceDirectoryId: selectedDirectoryId.value,
      targetDirectoryId: batchTargetDirectoryId.value,
      videoIds: selectedFavoriteVideoIds.value,
      operation
    });
    showBatchTargetDialog.value = false;
    batchDialogOperation.value = null;
    batchTargetDirectoryId.value = null;
    resetBatchSelection();
    await loadCollectionDirectories();
    ElMessage.success(`${actionText}成功，已处理 ${affected} 个视频`);
  } catch (error) {
    console.error('批量操作失败:', error);
    ElMessage.error('批量操作失败：' + error.message);
  } finally {
    batchSubmitting.value = false;
  }
}

// 打开新建收藏夹对话框
function openCreateDialog() {
  showCreateDialog.value = true;
  directoryForm.value = {
    name: '',
    description: '',
    coverUrl: '',
    isPublic: 0
  };
  coverPreview.value = '';
  coverFile.value = null;
  nameLength.value = 0;
  descLength.value = 0;
}

// 处理封面图片选择
function handleCoverChange(event) {
  const file = event.target.files[0];
  if (!file) return;
  
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件');
    return;
  }
  
  coverFile.value = file;
  
  // 本地预览
  const reader = new FileReader();
  reader.onload = (e) => {
    coverPreview.value = e.target.result;
  };
  reader.readAsDataURL(file);
}

// 处理下拉菜单命令
function handleDropdownCommand(command, dir) {
  if (command === 'edit') {
    handleEditDirectory(dir);
  } else if (command === 'delete') {
    handleDeleteDirectory(dir.directoryId);
  }
}

// 打开编辑收藏夹对话框
function handleEditDirectory(dir) {
  showEditDialog.value = true;
  editingDirectory.value = dir;
  directoryForm.value = {
    name: dir.name,
    description: dir.description || '',
    coverUrl: dir.coverUrl || '',
    isPublic: dir.isPublic
  };
  coverPreview.value = dir.coverUrl || '';
  coverFile.value = null;
  nameLength.value = dir.name.length;
  descLength.value = (dir.description || '').length;
}

// 删除收藏夹
async function handleDeleteDirectory(directoryId) {
  try {
    await ElMessageBox.confirm('确定要删除这个收藏夹吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    await deleteCollectionDirectory(directoryId);
    // 刷新收藏夹列表
    await loadCollectionDirectories();
    ElMessage.success('删除成功');
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除收藏夹失败:', error);
      ElMessage.error('删除失败：' + error.message);
    }
  }
}

// 提交新建/编辑收藏夹
async function handleSubmitDirectory() {
  if (!directoryForm.value.name.trim()) {
    ElMessage.warning('请输入收藏夹名称');
    return;
  }
  
  try {
    uploading.value = true;
    
    // 如果有新选择的封面文件，先上传图片
    if (coverFile.value) {
      const coverUrl = await uploadImage(coverFile.value);
      directoryForm.value.coverUrl = coverUrl;
    }
    
    if (showEditDialog.value) {
      // 编辑
      await updateCollectionDirectory({
        directoryId: editingDirectory.value.directoryId,
        ...directoryForm.value
      });
      ElMessage.success('编辑成功');
    } else {
      // 新建
      await createCollectionDirectory(directoryForm.value);
      ElMessage.success('创建成功');
    }
    
    // 关闭对话框
    closeDialog();
    // 刷新收藏夹列表
    await loadCollectionDirectories();
  } catch (error) {
    console.error('操作失败:', error);
    ElMessage.error('操作失败：' + error.message);
  } finally {
    uploading.value = false;
  }
}

// 关闭对话框
function closeDialog() {
  showCreateDialog.value = false;
  showEditDialog.value = false;
  editingDirectory.value = null;
  directoryForm.value = {
    name: '',
    description: '',
    coverUrl: '',
    isPublic: 0
  };
}

// 加载更多
function loadMoreVideos() {
  if (!hasMore.value || loading.value) return;
  pageNum.value++;
  loadUserVideos();
}

// 排序切换
function handleSortChange(sortType) {
  activeSort.value = sortType;
  loadUserVideos(true);
}

function handleVideoKeywordSearch() {
  const nextKeyword = videoKeywordInput.value.trim();
  appliedVideoKeyword.value = nextKeyword;
  if (activeTab.value !== 'videos') {
    pendingVideoReload.value = true;
    switchContentTab('videos');
    return;
  }
  loadUserVideos(true);
}

function switchContentTab(tabKey) {
  if (isProfileRestricted.value) {
    return;
  }
  const nextQuery = { ...route.query };
  if (tabKey === 'videos') {
    delete nextQuery.tab;
  } else {
    nextQuery.tab = tabKey;
  }
  const currentTab = typeof route.query.tab === 'string' ? route.query.tab : undefined;
  const nextTab = typeof nextQuery.tab === 'string' ? nextQuery.tab : undefined;
  if (route.name === 'user-profile' && currentTab === nextTab) {
    return;
  }
  router.push({
    name: 'user-profile',
    params: { userId: userId.value },
    query: nextQuery
  });
}

async function loadProfilePageData() {
  await loadUserInfo();
  if (isProfileRestricted.value) {
    return;
  }
  await Promise.all([
    loadUserVideos(true),
    loadCollectionDirectories()
  ]);
  if (['following', 'fans'].includes(activeTab.value)) {
    loadRelationList(activeTab.value, true);
  }
}

// 监听路由变化，切换标签时重新加载数据
watch(() => activeTab.value, (newTab) => {
  if (isProfileRestricted.value) {
    return;
  }
  if (newTab === 'videos' && pendingVideoReload.value) {
    pendingVideoReload.value = false;
    loadUserVideos(true);
    return;
  }
  if (['following', 'fans'].includes(newTab)) {
    loadRelationList(newTab, true);
  }
});

function isCurrentUser(targetUserId) {
  return String(targetUserId) === String(getUserId() || '');
}

function getNameInitial(username) {
  return String(username || 'U').charAt(0).toUpperCase();
}

function getRelationButtonLabel(item) {
  if (item.isFollow && item.isFans) {
    return '已互粉';
  }
  if (!item.isFollow && item.isFans) {
    return '回关';
  }
  return item.isFollow ? '已关注' : '+ 关注';
}

function getRelationButtonClass(item) {
  if (item.isFollow) {
    return 'border-transparent bg-[#f3f4f6] text-[#8b95a1] hover:bg-[#e9edf2]';
  }
  if (item.isFans) {
    return 'border-[#36a7ea] bg-white text-[#18a8df] hover:bg-[#f4fbff]';
  }
  return 'border-transparent bg-[#ffe8ef] text-[#ff6b8f] hover:bg-[#ffd8e5]';
}

function syncSelfFollowCount(delta) {
  if (!isSelf.value || !userInfo.value) {
    return;
  }
  userInfo.value.followNum = Math.max(0, Number(userInfo.value.followNum || 0) + delta);
}

async function toggleRelationFollow(item) {
  if (!item?.userId || isCurrentUser(item.userId) || relationFollowLoadingMap.value[item.userId] || isProfileRestricted.value) {
    return;
  }

  const nextIsFollow = !item.isFollow;
  relationFollowLoadingMap.value = {
    ...relationFollowLoadingMap.value,
    [item.userId]: true
  };

  try {
    await followUser({
      followeeId: item.userId,
      operation: nextIsFollow ? 1 : 0
    });

    item.isFollow = nextIsFollow;
    if (isSelf.value) {
      syncSelfFollowCount(nextIsFollow ? 1 : -1);
    }

    ElMessage.success(nextIsFollow ? '关注成功' : '已取消关注');
  } catch (error) {
    console.error('关注操作失败:', error);
    ElMessage.error(error.message || '关注操作失败');
  } finally {
    relationFollowLoadingMap.value = {
      ...relationFollowLoadingMap.value,
      [item.userId]: false
    };
  }
}

// 关注/取消关注
async function handleFollow() {
  if (isSelf.value || !userInfo.value?.userId || isProfileRestricted.value) {
    return;
  }

  const nextIsFollow = !userInfo.value.isFollow;
  try {
    await followUser({
      followeeId: userInfo.value.userId,
      operation: nextIsFollow ? 1 : 0
    });
    userInfo.value.isFollow = nextIsFollow;
    userInfo.value.fansNum = Math.max(0, Number(userInfo.value.fansNum || 0) + (nextIsFollow ? 1 : -1));
    if (activeTab.value === 'fans') {
      await loadRelationList('fans', true);
    }
    ElMessage.success(nextIsFollow ? '关注成功' : '已取消关注');
  } catch (error) {
    console.error('关注操作失败:', error);
    ElMessage.error(error.message || '关注操作失败');
  }
}

function openBlockDialog({ targetUserId, username }) {
  if (!targetUserId || isProfileRestricted.value) {
    return;
  }
  blockDialog.visible = true;
  blockDialog.targetUserId = Number(targetUserId);
  blockDialog.username = username || '';
}

function closeBlockDialog() {
  if (blockDialog.submitting) {
    return;
  }
  blockDialog.visible = false;
  blockDialog.targetUserId = null;
  blockDialog.username = '';
}

async function confirmBlockUser() {
  if (!blockDialog.targetUserId) {
    return;
  }

  blockDialog.submitting = true;
  try {
    await blockUser({
      targetUserId: blockDialog.targetUserId,
      operation: 1
    });

    if (userInfo.value?.userId && Number(userInfo.value.userId) === Number(blockDialog.targetUserId)) {
      const wasFollow = Boolean(userInfo.value.isFollow);
      userInfo.value.isFollow = false;
      if (wasFollow) {
        userInfo.value.fansNum = Math.max(0, Number(userInfo.value.fansNum || 0) - 1);
      }
    }

    blockDialog.submitting = false;
    closeBlockDialog();
    ElMessage.success('已加入黑名单');
  } catch (error) {
    console.error('加入黑名单失败:', error);
    ElMessage.error(error.message || '加入黑名单失败');
  } finally {
    blockDialog.submitting = false;
  }
}

function openUserReportDialog() {
  if (!userInfo.value?.userId) {
    return;
  }
  reportDialog.visible = true;
  reportDialog.targetType = 1;
  reportDialog.targetId = Number(userInfo.value.userId);
  reportDialog.title = '举报用户';
  reportDialog.reason = '';
  reportDialog.detail = '';
}

function closeReportDialog() {
  if (reportDialog.submitting) {
    return;
  }
  reportDialog.visible = false;
  reportDialog.targetId = null;
  reportDialog.reason = '';
  reportDialog.detail = '';
}

async function submitUserReport() {
  if (!reportDialog.targetId) {
    return;
  }
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
    await submitReport({
      targetType: reportDialog.targetType,
      targetId: reportDialog.targetId,
      reason: reportDialog.reason,
      detail: reportDialog.detail
    });
    reportDialog.submitting = false;
    closeReportDialog();
    ElMessage.success('举报提交成功，我们会尽快处理');
  } catch (error) {
    console.error('举报失败:', error);
    ElMessage.error(error.message || '举报提交失败');
  } finally {
    reportDialog.submitting = false;
  }
}

function goToPrivateChat() {
  if (isSelf.value || !userInfo.value?.userId || isProfileRestricted.value) {
    return;
  }
  router.push({
    name: 'messages',
    params: { userId: userInfo.value.userId },
    query: { tab: 'chat' }
  });
}

// 跳转到视频详情页
function goToVideo(videoId) {
  router.push(`/video/${videoId}`);
}

function goToUserProfile(targetUserId) {
  router.push({
    name: 'user-profile',
    params: { userId: targetUserId }
  });
}

function handleImageError(event) {
  if (event?.target) {
    event.target.style.display = 'none';
  }
}

// 格式化播放量
function formatCount(count) {
  if (!count) return '0';
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万';
  }
  return count.toString();
}

// 格式化时长
function formatDuration(seconds) {
  if (!seconds) return '00:00';
  const minutes = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

// 格式化日期
function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const month = date.getMonth() + 1;
  const day = date.getDate();
  return `${month}-${day}`;
}

// 监听路由变化
watch(() => route.params.userId, (newId) => {
  if (newId) {
    userId.value = newId;
    selectedDirectoryId.value = null;
    videoKeywordInput.value = '';
    appliedVideoKeyword.value = '';
    pendingVideoReload.value = false;
    relationKeyword.value = '';
    followingUsers.value = [];
    fanUsers.value = [];
    relationLoadedMap.value = {
      following: false,
      fans: false
    };
    exitBatchMode();
    loadProfilePageData();
  }
});

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = resolveActiveTab(tab);
    if (resolveActiveTab(tab) !== 'favorites') {
      exitBatchMode();
    }
    if (['following', 'fans'].includes(activeTab.value)) {
      loadRelationList(activeTab.value);
    }
  }
);

onMounted(() => {
  loadProfilePageData();
});
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.profile-more-panel {
  pointer-events: none;
  opacity: 0;
  transform: translateY(4px);
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.profile-more:hover .profile-more-panel,
.profile-more:focus-within .profile-more-panel {
  pointer-events: auto;
  opacity: 1;
  transform: translateY(0);
}
</style>

<style>
/* 修复 Element Plus 下拉菜单闪烁问题 */
.collection-dropdown {
  transition: opacity 0.2s ease;
}

.collection-dropdown.el-popper {
  transform-origin: top right !important;
}
</style>
