<template>
  <div class="p-6">
    <div class="max-w-5xl mx-auto">
      <!-- 页面标题 -->
      <div class="mb-6">
        <h2 class="text-2xl font-bold text-gray-800">视频投稿</h2>
        <p class="text-sm text-gray-500 mt-1">上传您的视频作品，与更多人分享</p>
      </div>

      <section class="rounded-lg bg-white p-6 shadow-sm ring-1 ring-slate-200">
        <div class="mb-6 flex flex-wrap gap-4">
          <div v-if="videoFile" class="relative flex h-16 w-48 items-center rounded bg-[#00a1d6] px-3 text-white transition-opacity" :class="{ 'opacity-70': uploadProgress < 100 }">
            <div class="truncate text-xs">
              <p class="font-medium">{{ videoFile.name }}</p>
              <p class="opacity-80">{{ uploadProgress === 100 ? '已上传完成' : '待上传' }}</p>
            </div>
            <div v-if="uploadProgress === 100" class="absolute -top-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full bg-white text-[10px] text-[#00a1d6]">✓</div>
          </div>

          <input ref="videoInputRef" class="hidden" type="file" accept="video/*" @change="onVideoFileChange" />

          <button v-if="!videoFile" @click="triggerVideoUpload" class="flex h-16 w-24 cursor-pointer flex-col items-center justify-center rounded border border-dashed border-slate-300 bg-slate-50 hover:bg-slate-100">
            <span class="text-xl text-slate-400">+</span>
            <span class="text-xs text-slate-500">添加视频</span>
          </button>
        </div>

        <div class="mb-4" v-if="videoFile && uploadProgress < 100">
          <button
              @click="uploadVideo"
              :disabled="isUploadingVideo"
              class="rounded bg-[#00a1d6] px-4 py-1.5 text-xs text-white transition-opacity hover:bg-[#00b5e5] disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {{ isUploadingVideo ? '上传中...' : '开始上传' }}
          </button>
        </div>

        <div v-if="videoFile" class="flex items-center gap-4 rounded bg-[#f4f5f7] p-4">
          <div class="flex h-10 w-10 items-center justify-center rounded" :class="uploadProgress === 100 ? 'bg-[#00a1d6]' : 'bg-slate-300'">
            <svg class="h-6 w-6 text-white" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
          </div>
          <div class="flex-1">
            <div class="flex justify-between text-sm">
              <span class="font-medium truncate max-w-[300px]">{{ videoFile.name }}</span>
              <button class="text-[#00a1d6] hover:underline" :disabled="isUploadingVideo" @click="triggerVideoUpload">更换视频</button>
            </div>
            <div class="mt-2 h-1 w-full overflow-hidden rounded-full bg-slate-200">
              <div class="h-full bg-[#05c160] transition-all" :style="{ width: `${uploadProgress}%` }"></div>
            </div>
            <p class="mt-1 text-xs" :class="uploadProgress === 100 ? 'text-slate-500' : 'text-[#00a1d6]'">
              {{ uploadProgress === 100 ? '已上传完成' : `上传进度: ${uploadProgress}%` }}
            </p>
          </div>
        </div>
      </section>

      <section class="mt-6 rounded-lg bg-white p-8 shadow-sm ring-1 ring-slate-200">
        <div class="mb-8 flex items-center justify-between">
          <h2 class="text-lg font-bold">基本设置</h2>
        </div>

        <div class="space-y-8">
          <div class="flex">
            <label class="w-24 shrink-0 text-sm font-medium"><span class="mr-1 text-red-500">*</span>封面</label>
            <div class="flex-1">
              <div class="flex gap-6">
                <div class="relative aspect-[4/3] w-48 overflow-hidden rounded border border-slate-200 bg-slate-100">
                  <img v-if="coverPreviewUrl" :src="coverPreviewUrl" class="h-full w-full object-cover" />
                  <div v-if="!coverPreviewUrl" class="flex h-full flex-col items-center justify-center text-slate-400">
                    <span class="text-xs">暂无封面</span>
                  </div>
                  <div class="absolute bottom-0 w-full bg-black/50 py-1 text-center text-xs text-white">封面预览</div>
                </div>
                <div class="flex-1 rounded bg-[#f4f5f7] p-4 flex flex-col justify-center">
                  <p class="text-sm text-slate-500">系统默认截取视频第一帧作为视频封面。您也可以点击下方按钮上传自定义封面。</p>
                </div>
              </div>
              <div class="mt-3">
                <input type="file" id="coverInput" class="hidden" accept="image/*" @change="onCoverFileChange" />
                <button @click="triggerCoverUpload" class="text-xs text-[#00a1d6] hover:underline" :disabled="isUploadingCover">
                  {{ isUploadingCover ? '封面上传中...' : '上传封面' }}
                </button>
              </div>
            </div>
          </div>

          <div class="flex items-center">
            <label class="w-24 shrink-0 text-sm font-medium"><span class="mr-1 text-red-500">*</span>标题</label>
            <div class="relative flex-1">
              <input
                  v-model="form.title"
                  class="w-full rounded border border-slate-200 px-3 py-2 text-sm focus:border-[#00a1d6] focus:outline-none"
                  maxlength="80"
                  placeholder="请输入标题"
              />
              <span class="absolute right-3 top-2 text-xs text-slate-400">{{ form.title.length }}/80</span>
            </div>
          </div>

          <div class="flex items-center">
            <label class="w-24 shrink-0 text-sm font-medium"><span class="mr-1 text-red-500">*</span>分区</label>
            <div class="relative w-48">
              <div
                class="flex items-center justify-between rounded border border-slate-200 bg-white px-3 py-2 text-sm cursor-pointer focus-within:border-[#00a1d6] focus:outline-none"
                :class="{ 'border-[#00a1d6]': partitionDropdownOpen }"
                tabindex="0"
                @click="partitionDropdownOpen = !partitionDropdownOpen"
                @blur="partitionDropdownOpen = false"
              >
                <span class="truncate text-slate-900">{{ selectedPartition ? selectedPartition.partitionName : '请选择分区' }}</span>
                <svg class="ml-2 h-4 w-4 text-slate-400" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7"/></svg>
              </div>
              <div
                v-if="partitionDropdownOpen"
                class="absolute left-0 z-20 mt-1 max-h-56 w-full overflow-auto rounded-lg border border-slate-200 bg-white py-1 shadow-lg"
                style="min-width: 100%;"
              >
                <div
                  v-for="partition in partitions"
                  :key="partition.id"
                  @mousedown.prevent="selectPartition(partition)"
                  class="cursor-pointer px-4 py-2 text-sm hover:bg-[#f5faff] hover:text-[#00a1d6]"
                  :class="{ 'bg-[#f5faff] text-[#00a1d6] font-bold': form.partitionId === partition.id }"
                >
                  {{ partition.partitionName }}
                </div>
              </div>
            </div>
          </div>

          <div class="flex">
            <label class="w-24 shrink-0 mt-2 text-sm font-medium">
              <span class="mr-1 text-red-500">*</span>标签
              <span class="ml-1 cursor-help inline-flex h-3 w-3 items-center justify-center rounded-full border border-slate-400 text-[10px] leading-none text-slate-400" title="至少需要添加1个标签">?</span>
            </label>
            <div class="flex-1">
              <div class="flex w-full flex-wrap items-center gap-2 rounded border border-slate-200 bg-white p-1.5 transition-all focus-within:border-[#00a1d6]">
                <span
                    v-for="tag in tags"
                    :key="tag"
                    class="flex items-center gap-1 rounded bg-[#00a1d6] py-1 pl-2 pr-1 text-xs text-white"
                >
                  {{ tag }}
                  <button type="button" class="flex h-4 w-4 items-center justify-center rounded-full hover:bg-black/10" @click="removeTag(tag)">×</button>
                </span>
                <input
                    v-model.trim="tagDraft"
                    class="min-w-[150px] flex-1 px-1 text-sm outline-none bg-transparent"
                    placeholder="按回车键Enter创建标签"
                    @keydown.enter.prevent="addTagFromDraft"
                    @keydown.backspace="tagDraft === '' && tags.pop()"
                />
                <span class="px-2 text-xs text-slate-400">还可以添加 {{ 10 - tags.length }} 个标签</span>
              </div>
            </div>
          </div>

          <div class="flex">
            <label class="w-24 shrink-0 text-sm font-medium">简介</label>
            <textarea
                v-model="form.description"
                class="min-h-[120px] flex-1 rounded border border-slate-200 p-3 text-sm focus:border-[#00a1d6] focus:outline-none"
                placeholder="填写更详细的简介，让更多人找到你的视频"
            ></textarea>
          </div>
        </div>

        <div class="mt-12 flex justify-center">
          <button
              @click="submitVideo"
              :disabled="isSubmitting"
              class="w-48 rounded bg-[#00a1d6] py-2.5 font-medium text-white shadow-lg transition-opacity hover:opacity-90 disabled:bg-slate-300 disabled:cursor-not-allowed"
          >
            {{ isSubmitting ? '提交中...' : '立即投稿' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { getToken, clearUserAuth, isUserLoggedIn } from '../utils/auth';

const router = useRouter();
const API_BASE = import.meta.env.VITE_API_BASE_URL || '';
const CHUNK_SIZE = 5 * 1024 * 1024;

// 使用正确的token获取方式
const token = ref(getToken());
const videoInputRef = ref(null);
const videoFile = ref(null);
const coverFile = ref(null);
const coverPreviewUrl = ref('');
const uploadProgress = ref(0);
const isUploadingVideo = ref(false);
const isUploadingCover = ref(false);
const isSubmitting = ref(false);
const tagDraft = ref('');
const tags = ref([]);
const partitions = ref([]);
const partitionDropdownOpen = ref(false);

const form = reactive({
  title: '',
  description: '',
  coverUrl: '',
  duration: null,
  partitionId: null
});

// 标记封面是否需要上传（系统截取的默认封面为true）
const needUploadCover = ref(false);

const videoContext = reactive({
  videoId: null,
  uploadToken: ''
});

const selectedPartition = computed(() => partitions.value.find(p => p.id === form.partitionId) || null);

async function request(path, { method = 'GET', json, formData } = {}) {
  const headers = {};
  if (token.value) headers.token = token.value;
  let body;
  if (json) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(json);
  } else if (formData) {
    body = formData;
  }
  const response = await fetch(`${API_BASE}${path}`, { method, headers, body });
  const payload = await response.json().catch(() => ({}));
  
  // 检查是否未登录或token失效
  if (payload?.message && (
    payload.message.includes('未登录') || 
    payload.message.includes('失效') ||
    payload.message.includes('登录失败')
  )) {
    clearUserAuth();
    alert('登录已失效，请重新登录');
    router.push('/');
    return;
  }
  
  if (!response.ok) throw new Error(payload?.message || `HTTP ${response.status}`);
  if (payload?.code !== 0) throw new Error(payload?.message || '请求失败');
  return payload.data;
}

function triggerVideoUpload() {
  if (videoInputRef.value) videoInputRef.value.click();
}

function triggerCoverUpload() {
  const input = document.getElementById('coverInput');
  if (input) input.click();
}

function addTagFromDraft() {
  const val = tagDraft.value.trim();
  if (val && tags.value.length < 10 && !tags.value.includes(val)) {
    tags.value.push(val);
    tagDraft.value = '';
  }
}

function removeTag(tag) {
  tags.value = tags.value.filter(t => t !== tag);
}

function selectPartition(partition) {
  form.partitionId = partition.id;
  partitionDropdownOpen.value = false;
}

// 视频自动截帧（第一帧/第0秒）并获取时长
async function createVideoPoster(file) {
  const url = URL.createObjectURL(file);
  return new Promise((resolve) => {
    const v = document.createElement('video');
    v.src = url;
    v.crossOrigin = 'anonymous';
    v.onloadedmetadata = () => {
      // 获取视频时长（秒）
      form.duration = Math.floor(v.duration);
      v.currentTime = 0;
    };
    v.onseeked = () => {
      const canvas = document.createElement('canvas');
      canvas.width = v.videoWidth || 640;
      canvas.height = v.videoHeight || 360;
      canvas.getContext('2d').drawImage(v, 0, 0, canvas.width, canvas.height);
      const dataUrl = canvas.toDataURL('image/jpeg');
      URL.revokeObjectURL(url);
      resolve(dataUrl);
    };
    v.onerror = () => {
      URL.revokeObjectURL(url);
      resolve('');
    };
  });
}

function onVideoFileChange(e) {
  const file = e.target.files[0];
  if (!file) return;

  // 提取文件名（去后缀）作为默认标题
  const lastDotIndex = file.name.lastIndexOf('.');
  form.title = lastDotIndex !== -1 ? file.name.substring(0, lastDotIndex) : file.name;

  uploadProgress.value = 0;
  videoContext.videoId = null;
  videoContext.uploadToken = '';
  videoFile.value = file;
  needUploadCover.value = false; // 重置封面上传标记

  createVideoPoster(file).then(res => {
    if (res) {
      coverPreviewUrl.value = res;
      form.coverUrl = res;
      needUploadCover.value = true; // 标记需要上传默认封面
    }
  });
}

function onCoverFileChange(e) {
  const file = e.target.files[0];
  if (!file) return;
  coverFile.value = file;
  coverPreviewUrl.value = URL.createObjectURL(file);
  needUploadCover.value = false; // 用户自定义封面，不需要额外处理
  uploadCover();
}

async function uploadVideo() {
  if (!videoFile.value) return;
  try {
    isUploadingVideo.value = true;
    const totalChunks = Math.ceil(videoFile.value.size / CHUNK_SIZE);

    const initData = await request('/graduation-project/upload/multipart/new', {
      method: 'POST',
      json: {
        fileName: videoFile.value.name,
        totalChunks,
        contentType: videoFile.value.type || 'video/mp4'
      }
    });

    videoContext.videoId = initData.videoId;
    videoContext.uploadToken = initData.uploadToken;

    for (let i = 0; i < totalChunks; i++) {
      const start = i * CHUNK_SIZE;
      const end = Math.min(videoFile.value.size, start + CHUNK_SIZE);
      const chunk = videoFile.value.slice(start, end);
      const data = new FormData();
      data.append('videoId', String(initData.videoId));
      data.append('uploadToken', initData.uploadToken);
      data.append('chunkIndex', String(i));
      data.append('file', chunk, 'blob');

      await request('/graduation-project/upload/multipart/part', { method: 'POST', formData: data });
      uploadProgress.value = Math.round(((i + 1) / totalChunks) * 100);
    }

    await request('/graduation-project/upload/multipart/complete', {
      method: 'POST',
      json: { videoId: initData.videoId, uploadToken: initData.uploadToken }
    });
  } catch (error) {
    alert(`上传失败：${error.message}`);
  } finally {
    isUploadingVideo.value = false;
  }
}

async function uploadCover() {
  if (!coverFile.value) return;
  try {
    isUploadingCover.value = true;
    const data = new FormData();
    data.append('file', coverFile.value);
    const url = await request('/graduation-project/upload/image', { method: 'POST', formData: data });
    form.coverUrl = url;
  } catch (error) {
    console.error(`封面上传失败：${error.message}`);
  } finally {
    isUploadingCover.value = false;
  }
}

async function submitVideo() {
  try {
    if (!videoContext.videoId) throw new Error('请先上传视频');
    if (!form.title) throw new Error('标题不能为空');
    if (!form.partitionId) throw new Error('请选择分区');
    if (tags.value.length === 0) throw new Error('至少需要添加1个标签');

    isSubmitting.value = true;
    
    // 如果使用的是系统截取的默认封面，先上传封面
    let finalCoverUrl = form.coverUrl;
    if (needUploadCover.value && form.coverUrl && form.coverUrl.startsWith('data:')) {
      // 将 base64 转换为 File 对象并上传
      const coverFile = await base64ToFile(form.coverUrl, 'cover.jpg');
      const formData = new FormData();
      formData.append('file', coverFile);
      finalCoverUrl = await request('/graduation-project/upload/image', { 
        method: 'POST', 
        formData: formData 
      });
    }

    await request('/graduation-project/video/submit', {
      method: 'POST',
      json: {
        videoId: videoContext.videoId,
        title: form.title,
        description: form.description,
        coverUrl: finalCoverUrl,
        duration: form.duration,
        partitionId: form.partitionId,
        tagList: [...tags.value]
      }
    });
    alert('发布成功！');
  } catch (error) {
    alert(error.message);
  } finally {
    isSubmitting.value = false;
  }
}

// 将 base64 Data URL 转换为 File 对象
function base64ToFile(dataUrl, filename) {
  return new Promise((resolve) => {
    const arr = dataUrl.split(',');
    const mime = arr[0].match(/:(.*?);/)[1];
    const bstr = atob(arr[1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n);
    }
    resolve(new File([u8arr], filename, { type: mime }));
  });
}

onMounted(async () => {
  // 检查登录状态
  if (!isUserLoggedIn()) {
    alert('请先登录');
    router.push('/');
    return;
  }
  
  try {
    const data = await request('/graduation-project/video/partitions');
    partitions.value = Array.isArray(data) ? data : [];
  } catch (e) {
    partitions.value = [];
  }
});
</script>

<style scoped>
input::placeholder, textarea::placeholder {
  color: #9499a0;
}
</style>
