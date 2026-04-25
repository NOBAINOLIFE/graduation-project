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
            </div>
            <p class="text-white/90 text-sm line-clamp-1 drop-shadow">{{ userInfo?.bio || '这个人很懒，什么都没写~' }}</p>
          </div>

          <!-- 操作按钮 -->
          <div class="flex-shrink-0 flex gap-3 pb-1" v-if="!isSelf">
            <button
              class="px-6 py-2 bg-[#00a1d6] text-white rounded-lg hover:bg-[#0090c0] transition-colors font-medium text-sm"
              :class="userInfo?.isFollow ? 'bg-gray-400 hover:bg-gray-500' : ''"
              @click="handleFollow"
            >
              {{ userInfo?.isFollow ? '已关注' : '+ 关注' }}
            </button>
            <button class="px-6 py-2 bg-white/20 backdrop-blur text-white rounded-lg hover:bg-white/30 transition-colors border border-white/30 text-sm">
              发消息
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 导航标签和统计信息 -->
    <div class="bg-white border-b border-gray-200">
      <div class="max-w-7xl mx-auto px-4">
        <div class="flex items-center justify-between gap-6">
          <!-- 左侧导航标签 -->
          <div class="flex min-w-0 flex-1 items-center gap-1 overflow-x-auto">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              class="relative flex shrink-0 items-center gap-2 whitespace-nowrap px-4 py-2.5 text-sm font-medium transition-colors"
              :class="activeTab === tab.key ? 'text-[#00a1d6]' : 'text-gray-600 hover:text-gray-800'"
              @click="activeTab = tab.key"
            >
              <!-- 图标 -->
              <svg v-if="tab.icon" class="h-[18px] w-[18px] shrink-0" fill="currentColor" viewBox="0 0 24 24">
                <path :d="tab.icon"/>
              </svg>
              <span>{{ tab.label }}</span>
              <span v-if="tab.count !== undefined && tab.count !== null" class="text-xs text-gray-400">{{ tab.count }}</span>
              <div v-if="activeTab === tab.key" class="absolute bottom-0 left-2 right-2 h-0.5 bg-[#00a1d6]"></div>
            </button>
          </div>

          <!-- 右侧统计信息 -->
          <div class="flex shrink-0 items-center gap-6 py-2">
            <div class="text-center">
              <div class="text-xs text-gray-500">关注数</div>
              <div class="text-base font-semibold leading-5 text-gray-800">{{ userInfo?.followNum || 0 }}</div>
            </div>
            <div class="text-center">
              <div class="text-xs text-gray-500">粉丝数</div>
              <div class="text-base font-semibold leading-5 text-gray-800">{{ formatCount(userInfo?.fansNum) }}</div>
            </div>
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
      <!-- 视频列表 -->
      <div v-if="activeTab === 'videos'">
        <div v-if="videoList.length === 0 && !loading" class="flex flex-col items-center justify-center py-20">
          <svg class="w-24 h-24 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
          </svg>
          <p class="text-gray-500 text-lg">该空间主人没有投过稿，这里什么都没有...</p>
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

          <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
            <div
              v-for="video in videoList"
              :key="video.videoId"
              class="bg-white rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-shadow cursor-pointer group"
              @click="goToVideo(video.videoId)"
            >
              <div class="relative aspect-video bg-gray-200">
                <img v-if="video.coverUrl" :src="video.coverUrl" class="w-full h-full object-cover" />
                <div v-else class="w-full h-full bg-gradient-to-br from-gray-300 to-gray-400"></div>
                <div class="absolute bottom-2 right-2 px-2 py-1 bg-black/70 text-white text-xs rounded">
                  {{ formatDuration(video.duration) }}
                </div>
              </div>
              <div class="p-3">
                <h3 class="text-sm font-medium text-gray-800 line-clamp-2 mb-2 group-hover:text-[#00a1d6]">{{ video.title }}</h3>
                <div class="flex items-center gap-3 text-xs text-gray-500">
                  <span class="flex items-center gap-1">
                    <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M8 5v14l11-7z"/>
                    </svg>
                    {{ formatCount(video.playCount) }}
                  </span>
                  <span>{{ formatDate(video.createTime) }}</span>
                </div>
              </div>
            </div>
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
      <div v-if="activeTab === 'favorites'">
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
import { ref, computed, onMounted, watch } from 'vue';
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
  uploadImage
} from '../api/user';
import { getUserId } from '../utils/auth';
import HeaderNav from './HeaderNav.vue';
import { ElMessage, ElMessageBox } from 'element-plus';

const route = useRoute();
const router = useRouter();

const PROFILE_TAB_KEYS = ['videos', 'favorites'];

const userId = ref(route.params.userId || getUserId());
const userInfo = ref(null);
const activeTab = ref(resolveActiveTab(route.query.tab));
const activeSort = ref(2); // 2-最新发布
const videoList = ref([]);
const loading = ref(false);
const pageNum = ref(1);
const hasMore = ref(true);

const isSelf = computed(() => {
  const currentUserId = getUserId();
  return String(userId.value) === String(currentUserId);
});
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

const tabs = computed(() => [
  { key: 'videos', label: '投稿', count: userInfo.value?.videoNum, icon: 'M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z' },
  { key: 'favorites', label: '收藏', count: favoriteCount.value, icon: 'M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z' }
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

// 加载用户信息
async function loadUserInfo() {
  try {
    userInfo.value = await getUserInfo(userId.value);
  } catch (error) {
    console.error('加载用户信息失败:', error);
  }
}

// 加载用户视频
async function loadUserVideos(isRefresh = false) {
  if (loading.value) return;
  
  try {
    loading.value = true;
    
    if (isRefresh) {
      pageNum.value = 1;
      videoList.value = [];
      hasMore.value = true;
    }

    const result = await searchVideos({
      userId: userId.value,
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
  if (!selectedDirectoryId.value || favoriteLoading.value) return;
  
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

// 关注/取消关注
async function handleFollow() {
  // TODO: 实现关注功能
  console.log('关注操作');
}

// 跳转到视频详情页
function goToVideo(videoId) {
  router.push(`/video/${videoId}`);
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
    exitBatchMode();
    loadUserInfo();
    loadUserVideos(true);
    loadCollectionDirectories();
  }
});

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = resolveActiveTab(tab);
    if (resolveActiveTab(tab) !== 'favorites') {
      exitBatchMode();
    }
  }
);

onMounted(() => {
  loadUserInfo();
  loadUserVideos(true);
  loadCollectionDirectories();
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
