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
        <div class="flex items-center justify-between">
          <!-- 左侧导航标签 -->
          <div class="flex items-center gap-1">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              class="px-4 py-3 text-sm font-medium transition-colors relative flex items-center gap-2"
              :class="activeTab === tab.key ? 'text-[#00a1d6]' : 'text-gray-600 hover:text-gray-800'"
              @click="activeTab = tab.key"
            >
              <!-- 图标 -->
              <svg v-if="tab.icon" class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                <path :d="tab.icon"/>
              </svg>
              <span>{{ tab.label }}</span>
              <span v-if="tab.count !== undefined && tab.count !== null" class="text-xs text-gray-400">{{ tab.count }}</span>
              <div v-if="activeTab === tab.key" class="absolute bottom-0 left-2 right-2 h-0.5 bg-[#00a1d6]"></div>
            </button>
          </div>

          <!-- 右侧统计信息 -->
          <div class="flex items-center gap-8 py-3">
            <div class="text-center">
              <div class="text-sm text-gray-500">关注数</div>
              <div class="text-lg font-semibold text-gray-800">{{ userInfo?.followNum || 0 }}</div>
            </div>
            <div class="text-center">
              <div class="text-sm text-gray-500">粉丝数</div>
              <div class="text-lg font-semibold text-gray-800">{{ formatCount(userInfo?.fansNum) }}</div>
            </div>
            <div class="text-center">
              <div class="text-sm text-gray-500">获赞数</div>
              <div class="text-lg font-semibold text-gray-800">{{ formatCount(userInfo?.likeNum) }}</div>
            </div>
            <div class="text-center">
              <div class="text-sm text-gray-500">播放数</div>
              <div class="text-lg font-semibold text-gray-800">{{ formatCount(userInfo?.playCount) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="max-w-7xl mx-auto px-4 mt-6 pb-12">
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

        <div v-else class="flex gap-6">
          <!-- 左侧收藏夹列表 -->
          <div class="w-64 flex-shrink-0">
            <div class="bg-white rounded-lg shadow-sm overflow-hidden">
              <div class="px-4 py-3 bg-gray-50 border-b border-gray-200">
                <h3 class="text-sm font-medium text-gray-700">我创建的收藏夹</h3>
              </div>
              <!-- 新建收藏夹按钮 -->
              <button
                class="w-full px-4 py-3 text-left transition-colors flex items-center justify-center gap-2 text-[#00a1d6] hover:bg-gray-50 border-b border-gray-200"
                @click="openCreateDialog"
              >
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
                </svg>
                <span class="text-sm font-medium">新建收藏夹</span>
              </button>
              <div class="max-h-[600px] overflow-y-auto">
                <div
                  v-for="dir in collectionDirectories"
                  :key="dir.directoryId"
                  class="relative group/dir"
                >
                  <button
                    class="w-full px-4 py-3 text-left transition-colors flex items-center justify-between"
                    :class="selectedDirectoryId === dir.directoryId ? 'bg-[#00a1d6] text-white' : 'text-gray-700 hover:bg-gray-50'"
                    @click="selectDirectory(dir.directoryId)"
                  >
                    <div class="flex items-center gap-2 flex-1 min-w-0">
                      <!-- 收藏夹封面 -->
                      <div class="w-10 h-10 rounded overflow-hidden flex-shrink-0 bg-gray-200">
                        <img v-if="dir.coverUrl" :src="dir.coverUrl" class="w-full h-full object-cover" />
                        <div v-else class="w-full h-full bg-gradient-to-br from-gray-300 to-gray-400 flex items-center justify-center">
                          <svg class="w-5 h-5 text-gray-500" fill="currentColor" viewBox="0 0 24 24">
                            <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                          </svg>
                        </div>
                      </div>
                      <span class="text-sm truncate">{{ dir.name }}</span>
                    </div>
                    
                    <!-- 视频数量（默认显示，悬停时隐藏） -->
                    <span 
                      class="text-xs ml-2 group-hover/dir:hidden" 
                      :class="selectedDirectoryId === dir.directoryId ? 'text-white/80' : 'text-gray-400'"
                    >
                      {{ dir.itemCount || 0 }}
                    </span>
                    
                    <!-- 三点下拉菜单（悬停收藏夹行时显示） -->
                    <el-dropdown 
                      v-if="isSelf"
                      trigger="hover" 
                      @command="(command) => handleDropdownCommand(command, dir)"
                      class="hidden group-hover/dir:block ml-2"
                      popper-class="collection-dropdown"
                      :hide-on-click="false"
                      placement="bottom-end"
                    >
                      <button
                        class="p-1 hover:bg-gray-200 rounded transition-colors"
                        @click.stop
                      >
                        <svg class="w-5 h-5 text-gray-500" fill="currentColor" viewBox="0 0 24 24">
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
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧视频列表 -->
          <div class="flex-1">
            <div v-if="selectedDirectory" class="mb-4">
              <!-- 收藏夹封面和信息 -->
              <div class="flex gap-4 mb-4">
                <!-- 封面 -->
                <div class="w-40 h-28 rounded-lg overflow-hidden flex-shrink-0 bg-gray-200">
                  <img v-if="selectedDirectory.coverUrl" :src="selectedDirectory.coverUrl" class="w-full h-full object-cover" />
                  <div v-else class="w-full h-full bg-gradient-to-br from-gray-300 to-gray-400 flex items-center justify-center">
                    <svg class="w-12 h-12 text-gray-500" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                    </svg>
                  </div>
                </div>
                <!-- 信息 -->
                <div class="flex-1">
                  <h2 class="text-xl font-bold text-gray-800 mb-2">{{ selectedDirectory.name }}</h2>
                  <p class="text-sm text-gray-500">{{ selectedDirectory.isPublic === 1 ? '公开' : '私密' }} · 视频数: {{ selectedDirectory.itemCount || 0 }}</p>
                </div>
              </div>
              
              <!-- 排序按钮 -->
              <div class="flex gap-2 mb-4">
                <button
                  v-for="sort in favoriteSortOptions"
                  :key="sort.key"
                  class="px-4 py-1.5 text-sm rounded transition-colors"
                  :class="activeFavoriteSort === sort.key ? 'bg-[#00a1d6] text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
                  @click="handleFavoriteSortChange(sort.key)"
                >
                  {{ sort.label }}
                </button>
              </div>
            </div>

            <div v-if="favoriteVideos.length === 0 && !favoriteLoading" class="bg-white rounded-lg p-12 text-center">
              <svg class="w-16 h-16 text-gray-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
              </svg>
              <p class="text-gray-500">这个收藏夹还没有视频</p>
            </div>

            <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
              <div
                v-for="video in favoriteVideos"
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
                    <span class="truncate">{{ video.username }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
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
import { getUserInfo, searchVideos, listCollectionDirectories, listCollectionItems, createCollectionDirectory, updateCollectionDirectory, deleteCollectionDirectory, uploadImage } from '../api/user';
import { getUserId, isUserLoggedIn } from '../utils/auth';
import HeaderNav from './HeaderNav.vue';
import { ElMessage, ElMessageBox } from 'element-plus';

const route = useRoute();
const router = useRouter();

const userId = ref(route.params.userId || getUserId());
const userInfo = ref(null);
const activeTab = ref('videos');
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
    favoriteCount.value = directories.reduce((sum, dir) => sum + (dir.itemCount || 0), 0);
    
    // 默认选中第一个收藏夹
    if (directories.length > 0 && !selectedDirectoryId.value) {
      selectedDirectoryId.value = directories[0].directoryId;
      loadFavoriteVideos();
    }
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
  } catch (error) {
    console.error('加载收藏视频失败:', error);
  } finally {
    favoriteLoading.value = false;
  }
}

// 切换收藏夹
function selectDirectory(directoryId) {
  // 如果已经选中该收藏夹，则不重复请求
  if (selectedDirectoryId.value === directoryId) {
    return;
  }
  selectedDirectoryId.value = directoryId;
  loadFavoriteVideos();
}

// 收藏夹排序切换
function handleFavoriteSortChange(sortType) {
  activeFavoriteSort.value = sortType;
  loadFavoriteVideos();
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
    loadUserInfo();
    loadUserVideos(true);
    loadCollectionDirectories();
  }
});

onMounted(() => {
  if (!isUserLoggedIn()) {
    router.push('/');
    return;
  }
  
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
