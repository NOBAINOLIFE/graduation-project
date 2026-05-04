<template>
  <div class="min-h-screen bg-[radial-gradient(circle_at_top,_#e4f6ff,_#f6f8fb_42%,_#eef2f7_100%)] text-[#1f2937]">
    <HeaderNav />

    <main class="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <section class="overflow-hidden rounded-[28px] border border-white/70 bg-white/90 shadow-[0_24px_70px_rgba(15,23,42,0.08)] backdrop-blur">
        <div class="bg-[linear-gradient(135deg,_#00a1d6,_#4ecdc4)] px-6 py-8 text-white sm:px-8">
          <div class="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
            <div class="flex items-center gap-5">
              <div class="relative h-24 w-24 overflow-hidden rounded-full border-4 border-white/80 bg-white/20 shadow-lg">
                <img
                  v-if="profileForm.avatarUrl"
                  :src="profileForm.avatarUrl"
                  :alt="profileForm.username || '用户头像'"
                  class="h-full w-full object-cover"
                  @error="handleImageError"
                />
                <div v-else class="flex h-full w-full items-center justify-center text-3xl font-bold">
                  {{ getNameInitial(profileForm.username) }}
                </div>
              </div>

              <div class="min-w-0">
                <p class="text-sm uppercase tracking-[0.32em] text-white/70">Personal Center</p>
                <h1 class="mt-2 truncate text-3xl font-bold">{{ profileForm.username || '加载中...' }}</h1>
                <p class="mt-2 max-w-2xl text-sm leading-6 text-white/85">
                  {{ profileForm.bio || '在这里统一管理个人资料、账号安全和硬币记录。' }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <div class="grid gap-6 p-6 lg:grid-cols-[260px_minmax(0,1fr)] lg:p-8">
          <aside class="space-y-4">

            <nav class="rounded-3xl border border-[#e7eef6] bg-[#f8fbff] p-3">
              <button
                v-for="item in sectionItems"
                :key="item.key"
                class="mb-2 flex w-full items-center justify-between rounded-2xl px-4 py-3 text-left transition last:mb-0"
                :class="activeSection === item.key ? 'bg-[#00a1d6] text-white shadow-[0_12px_30px_rgba(0,161,214,0.25)]' : 'text-[#334155] hover:bg-white hover:text-[#00a1d6]'"
                @click="activeSection = item.key"
              >
                <div>
                  <p class="text-sm font-semibold">{{ item.label }}</p>
                  <p class="mt-1 text-xs" :class="activeSection === item.key ? 'text-white/80' : 'text-[#94a3b8]'">
                    {{ item.description }}
                  </p>
                </div>
                <span class="text-sm font-medium">{{ item.short }}</span>
              </button>
            </nav>
          </aside>

          <div class="min-w-0">
            <section v-if="activeSection === 'profile'" class="rounded-3xl border border-[#e8eef5] bg-[#fbfdff] p-6">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <h2 class="text-xl font-semibold text-[#111827]">我的信息</h2>
                  <p class="mt-1 text-sm text-[#6b7280]">修改头像、用户名和简介，保存后会立即同步到当前账号。</p>
                </div>
                <button
                  class="rounded-full border border-[#b9e9f8] bg-[#eefbff] px-4 py-2 text-sm font-medium text-[#0095c8] transition hover:border-[#00a1d6] hover:text-[#00a1d6]"
                  :disabled="profileSubmitting"
                  @click="triggerAvatarUpload"
                >
                  更换头像
                </button>
              </div>

              <input
                ref="avatarInputRef"
                type="file"
                accept="image/*"
                class="hidden"
                @change="handleAvatarChange"
              />

              <div class="mt-6 grid gap-6 lg:grid-cols-[220px_minmax(0,1fr)]">
                <div class="rounded-3xl bg-white p-5 shadow-sm ring-1 ring-[#edf2f7]">
                  <div class="mx-auto h-40 w-40 overflow-hidden rounded-[24px] bg-[linear-gradient(135deg,_#d6f3ff,_#f2fbff)]">
                    <img
                      v-if="profileForm.avatarUrl"
                      :src="profileForm.avatarUrl"
                      :alt="profileForm.username || '头像预览'"
                      class="h-full w-full object-cover"
                      @error="handleImageError"
                    />
                    <div v-else class="flex h-full w-full items-center justify-center text-5xl font-bold text-[#00a1d6]">
                      {{ getNameInitial(profileForm.username) }}
                    </div>
                  </div>
                  <p class="mt-4 text-sm text-[#475569]">{{ avatarUploading ? '头像上传中...' : '支持常见图片格式，上传后自动替换。' }}</p>
                </div>

                <form class="space-y-5" @submit.prevent="submitProfile">
                  <label class="block">
                    <span class="mb-2 block text-sm font-medium text-[#334155]">用户名</span>
                    <input
                      v-model.trim="profileForm.username"
                      type="text"
                      maxlength="20"
                      class="w-full rounded-2xl border border-[#dbe3ec] bg-white px-4 py-3 text-sm outline-none transition focus:border-[#00a1d6] focus:ring-4 focus:ring-[#d7f3fd]"
                      placeholder="请输入用户名"
                    />
                  </label>

                  <label class="block">
                    <span class="mb-2 block text-sm font-medium text-[#334155]">个人简介</span>
                    <textarea
                      v-model.trim="profileForm.bio"
                      rows="5"
                      maxlength="120"
                      class="w-full resize-none rounded-2xl border border-[#dbe3ec] bg-white px-4 py-3 text-sm outline-none transition focus:border-[#00a1d6] focus:ring-4 focus:ring-[#d7f3fd]"
                      placeholder="介绍一下自己，让大家更快认识你"
                    ></textarea>
                    <div class="mt-2 text-right text-xs text-[#94a3b8]">{{ profileForm.bio.length }}/120</div>
                  </label>

                  <div class="flex flex-wrap items-center gap-3">
                    <button
                      type="submit"
                      class="rounded-full bg-[#00a1d6] px-6 py-3 text-sm font-medium text-white transition hover:bg-[#0095c8] disabled:cursor-not-allowed disabled:opacity-60"
                      :disabled="profileSubmitting || avatarUploading"
                    >
                      {{ profileSubmitting ? '保存中...' : '保存资料' }}
                    </button>
                    <button
                      type="button"
                      class="rounded-full border border-[#dbe3ec] bg-white px-6 py-3 text-sm font-medium text-[#475569] transition hover:border-[#00a1d6] hover:text-[#00a1d6]"
                      :disabled="profileSubmitting"
                      @click="resetProfileForm"
                    >
                      重置
                    </button>
                  </div>
                </form>
              </div>
            </section>

            <section v-else-if="activeSection === 'security'" class="rounded-3xl border border-[#e8eef5] bg-white p-6 shadow-sm">
              <h2 class="text-xl font-semibold text-[#111827]">账号安全</h2>
              <p class="mt-1 text-sm text-[#6b7280]">输入当前密码并设置新密码，建议使用更安全的新组合。</p>

              <form class="mt-6 max-w-2xl space-y-4" @submit.prevent="submitPassword">
                <label class="block">
                  <span class="mb-2 block text-sm font-medium text-[#334155]">当前密码</span>
                  <input
                    v-model="passwordForm.oldPassword"
                    type="password"
                    class="w-full rounded-2xl border border-[#dbe3ec] bg-white px-4 py-3 text-sm outline-none transition focus:border-[#00a1d6] focus:ring-4 focus:ring-[#d7f3fd]"
                    placeholder="请输入当前密码"
                  />
                </label>

                <label class="block">
                  <span class="mb-2 block text-sm font-medium text-[#334155]">新密码</span>
                  <input
                    v-model="passwordForm.newPassword"
                    type="password"
                    class="w-full rounded-2xl border border-[#dbe3ec] bg-white px-4 py-3 text-sm outline-none transition focus:border-[#00a1d6] focus:ring-4 focus:ring-[#d7f3fd]"
                    placeholder="请输入新密码"
                  />
                </label>

                <label class="block">
                  <span class="mb-2 block text-sm font-medium text-[#334155]">确认新密码</span>
                  <input
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    class="w-full rounded-2xl border border-[#dbe3ec] bg-white px-4 py-3 text-sm outline-none transition focus:border-[#00a1d6] focus:ring-4 focus:ring-[#d7f3fd]"
                    placeholder="请再次输入新密码"
                  />
                </label>

                <button
                  type="submit"
                  class="rounded-full bg-[#111827] px-6 py-3 text-sm font-medium text-white transition hover:bg-[#1f2937] disabled:cursor-not-allowed disabled:opacity-60"
                  :disabled="passwordSubmitting"
                >
                  {{ passwordSubmitting ? '提交中...' : '更新密码' }}
                </button>
              </form>
            </section>

            <section v-else class="space-y-6">
              <section class="rounded-3xl border border-[#e8eef5] bg-white p-6 shadow-sm">
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <h2 class="text-xl font-semibold text-[#111827]">我的硬币</h2>
                    <p class="mt-1 text-sm text-[#6b7280]">查看当前余额和最近的硬币获得情况。</p>
                  </div>
                  <button
                    class="rounded-full border border-[#dbe3ec] px-4 py-2 text-sm font-medium text-[#475569] transition hover:border-[#00a1d6] hover:text-[#00a1d6]"
                    :disabled="rewardLoading"
                    @click="loadRewardData"
                  >
                    刷新
                  </button>
                </div>

                <div class="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                  <div class="rounded-2xl bg-[#f8fbff] p-5">
                    <p class="text-sm text-[#64748b]">硬币余额</p>
                    <div class="mt-3 flex items-end gap-2">
                      <span class="text-3xl font-bold text-[#0f172a]">{{ wallet.balance }}</span>
                      <span class="pb-1 text-sm text-[#64748b]">枚</span>
                    </div>
                  </div>
                  <div class="rounded-2xl bg-[#f8fbff] p-5">
                    <p class="text-sm text-[#64748b]">变动记录</p>
                    <div class="mt-3 flex items-end gap-2">
                      <span class="text-3xl font-bold text-[#0f172a]">{{ rewardLogs.length }}</span>
                      <span class="pb-1 text-sm text-[#64748b]">条</span>
                    </div>
                  </div>
                  <div class="rounded-2xl bg-[#fff8ec] p-5">
                    <p class="text-sm text-[#8b5e00]">近7天总变动</p>
                    <p class="mt-3 text-lg font-semibold text-[#7c5f1a]">{{ totalChangeLabel }}</p>
                  </div>
                </div>
              </section>

              <section class="rounded-3xl border border-[#e8eef5] bg-white p-6 shadow-sm">
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <h3 class="text-lg font-semibold text-[#111827]">近7天硬币变化记录</h3>
                    <p class="mt-1 text-sm text-[#6b7280]">按时间倒序展示最近 7 天的硬币增减明细。</p>
                  </div>
                  <span class="rounded-full bg-[#f1f5f9] px-3 py-1 text-xs font-medium text-[#64748b]">{{ rewardLogs.length }} 条</span>
                </div>

                <div class="mt-6 space-y-3">
                  <div
                    v-for="item in rewardLogs"
                    :key="`reward-${item.id}`"
                    class="flex items-start justify-between gap-4 rounded-2xl border border-[#ecf1f6] bg-[#fcfdff] px-4 py-4"
                  >
                    <div class="min-w-0">
                      <h3 class="font-medium text-[#0f172a]">{{ item.changeDesc }}</h3>
                      <p class="mt-2 text-sm text-[#64748b]">
                        变动时间：{{ formatDateTime(item.createTime) }}
                      </p>
                    </div>
                    <span
                      class="shrink-0 rounded-full px-3 py-1.5 text-sm font-semibold"
                      :class="getAmountClass(item.changeAmount)"
                    >
                      {{ formatSignedAmount(item.changeAmount) }} 硬币
                    </span>
                  </div>

                  <div v-if="rewardLoading" class="rounded-2xl border border-dashed border-[#dbe3ec] px-4 py-10 text-center text-sm text-[#94a3b8]">
                    正在加载硬币变化记录...
                  </div>

                  <div v-else-if="rewardLogs.length === 0" class="rounded-2xl border border-dashed border-[#dbe3ec] px-4 py-10 text-center text-sm text-[#94a3b8]">
                    最近 7 天暂无可展示的硬币变化记录
                  </div>
                </div>
              </section>
            </section>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import HeaderNav from './HeaderNav.vue';
import {
  getMyCoinChangeLogs,
  getMyCoinWallet,
  getUserInfo,
  updateUserInfo,
  updateUserPassword,
  uploadAvatar
} from '../api/user';
import { getUserId, updateStoredUsername } from '../utils/auth';

const avatarInputRef = ref(null);
const activeSection = ref('profile');
const rewardLoading = ref(false);
const avatarUploading = ref(false);
const profileSubmitting = ref(false);
const passwordSubmitting = ref(false);
const rewardLogs = ref([]);
const wallet = reactive({
  balance: 0
});
const profileForm = reactive({
  username: '',
  avatarUrl: '',
  bio: ''
});
const initialProfile = reactive({
  username: '',
  avatarUrl: '',
  bio: ''
});
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const totalChangeLabel = computed(() => {
  if (rewardLogs.value.length === 0) {
    return '0';
  }
  const total = rewardLogs.value.reduce((sum, item) => sum + (item.changeAmount || 0), 0);
  return formatSignedAmount(total);
});
const sectionItems = [
  {
    key: 'profile',
    label: '我的信息',
    description: '修改头像、用户名和简介',
    short: '01'
  },
  {
    key: 'security',
    label: '账号安全',
    description: '重置当前登录密码',
    short: '02'
  },
  {
    key: 'coin',
    label: '我的硬币',
    description: '查看硬币数量和近7天变化记录',
    short: '03'
  }
];

function getNameInitial(username) {
  return String(username || 'U').charAt(0).toUpperCase();
}

function handleImageError(event) {
  if (event?.target) {
    event.target.style.display = 'none';
  }
}

function triggerAvatarUpload() {
  avatarInputRef.value?.click();
}

function applyProfile(userInfo = {}) {
  profileForm.username = userInfo.username || '';
  profileForm.avatarUrl = userInfo.avatarUrl || '';
  profileForm.bio = userInfo.bio || '';
  initialProfile.username = profileForm.username;
  initialProfile.avatarUrl = profileForm.avatarUrl;
  initialProfile.bio = profileForm.bio;
}

async function loadProfile() {
  const userInfo = await getUserInfo(getUserId());
  applyProfile(userInfo);
}

async function loadRewardData() {
  rewardLoading.value = true;
  try {
    const [walletData, rewardData] = await Promise.all([
      getMyCoinWallet(),
      getMyCoinChangeLogs(7)
    ]);
    wallet.balance = Number(walletData?.balance || 0);
    rewardLogs.value = Array.isArray(rewardData) ? rewardData : [];
  } catch (error) {
    console.error('加载硬币数据失败:', error);
    ElMessage.error(error.message || '加载硬币数据失败');
  } finally {
    rewardLoading.value = false;
  }
}

async function initializePage() {
  try {
    await Promise.all([loadProfile(), loadRewardData()]);
  } catch (error) {
    console.error('初始化个人中心失败:', error);
    ElMessage.error(error.message || '加载个人中心失败');
  }
}

async function handleAvatarChange(event) {
  const file = event.target?.files?.[0];
  if (!file) {
    return;
  }
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件');
    event.target.value = '';
    return;
  }

  try {
    avatarUploading.value = true;
    profileForm.avatarUrl = await uploadAvatar(file);
    ElMessage.success('头像上传成功');
  } catch (error) {
    console.error('上传头像失败:', error);
    ElMessage.error(error.message || '上传头像失败');
  } finally {
    avatarUploading.value = false;
    event.target.value = '';
  }
}

function resetProfileForm() {
  applyProfile(initialProfile);
}

async function submitProfile() {
  if (!profileForm.username.trim()) {
    ElMessage.warning('用户名不能为空');
    return;
  }

  try {
    profileSubmitting.value = true;
    await updateUserInfo({
      username: profileForm.username.trim(),
      avatarUrl: profileForm.avatarUrl,
      bio: profileForm.bio.trim()
    });
    updateStoredUsername(profileForm.username.trim());
    await loadProfile();
    ElMessage.success('个人资料已更新');
  } catch (error) {
    console.error('更新个人资料失败:', error);
    ElMessage.error(error.message || '更新个人资料失败');
  } finally {
    profileSubmitting.value = false;
  }
}

function resetPasswordForm() {
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
}

async function submitPassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请完整填写密码信息');
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致');
    return;
  }
  if (passwordForm.oldPassword === passwordForm.newPassword) {
    ElMessage.warning('新密码不能与原密码相同');
    return;
  }

  try {
    passwordSubmitting.value = true;
    await updateUserPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    });
    resetPasswordForm();
    ElMessage.success('密码更新成功');
  } catch (error) {
    console.error('更新密码失败:', error);
    ElMessage.error(error.message || '更新密码失败');
  } finally {
    passwordSubmitting.value = false;
  }
}

function formatRewardDate(value) {
  if (!value) {
    return '未知日期';
  }
  const date = parseDateValue(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${month}-${day}`;
}

function formatSignedAmount(value) {
  const amount = Number(value || 0);
  return amount > 0 ? `+${amount}` : `${amount}`;
}

function getAmountClass(value) {
  return Number(value || 0) >= 0
    ? 'bg-[#ecfeff] text-[#0891b2]'
    : 'bg-[#fff1f2] text-[#e11d48]';
}

function formatDateTime(value) {
  if (!value) {
    return '暂无时间';
  }
  const date = parseDateValue(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  const hour = `${date.getHours()}`.padStart(2, '0');
  const minute = `${date.getMinutes()}`.padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

function parseDateValue(value) {
  if (typeof value === 'string') {
    if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      const [year, month, day] = value.split('-').map(Number);
      return new Date(year, month - 1, day);
    }
    if (/^\d{4}-\d{2}-\d{2}T/.test(value)) {
      return new Date(value.replace('T', ' '));
    }
  }
  return new Date(value);
}

onMounted(() => {
  initializePage();
});
</script>
