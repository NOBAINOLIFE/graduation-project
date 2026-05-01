<template>
  <div class="p-6">
    <div class="mx-auto max-w-7xl space-y-6">
      <section class="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div class="space-y-2">
            <div class="inline-flex items-center rounded-full bg-[#fff3e6] px-3 py-1 text-xs font-semibold tracking-[0.2em] text-[#ea580c]">
              COMMUNITY
            </div>
            <div>
              <h2 class="text-3xl font-bold text-slate-900">粉丝管理</h2>
              <p class="mt-2 text-sm text-slate-500">看看最近是谁在关注你，也可以一键回关或直接发起私聊。</p>
            </div>
          </div>

          <div class="grid min-w-full gap-3 sm:grid-cols-3 lg:min-w-[420px]">
            <article class="rounded-2xl bg-slate-50 p-4">
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">粉丝总数</p>
              <p class="mt-3 text-3xl font-bold text-slate-900">{{ userInfo?.fansNum || 0 }}</p>
              <p class="mt-2 text-xs text-slate-500">当前账号累计粉丝</p>
            </article>
            <article class="rounded-2xl bg-slate-50 p-4">
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">已回关</p>
              <p class="mt-3 text-3xl font-bold text-slate-900">{{ mutualFollowCount }}</p>
              <p class="mt-2 text-xs text-slate-500">你也关注了他们</p>
            </article>
            <article class="rounded-2xl bg-slate-50 p-4">
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">搜索结果</p>
              <p class="mt-3 text-3xl font-bold text-slate-900">{{ filteredFans.length }}</p>
              <p class="mt-2 text-xs text-slate-500">按昵称和简介实时筛选</p>
            </article>
          </div>
        </div>
      </section>

      <section class="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200">
        <el-input
          v-model="keyword"
          clearable
          size="large"
          placeholder="搜索粉丝昵称或简介"
        />
      </section>

      <section v-loading="loading">
        <div v-if="filteredFans.length" class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <article
            v-for="fan in filteredFans"
            :key="fan.userId"
            class="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200"
          >
            <div class="flex items-start gap-4">
              <div class="h-14 w-14 flex-shrink-0 overflow-hidden rounded-full bg-slate-100">
                <img
                  v-if="fan.avatarUrl"
                  :src="fan.avatarUrl"
                  :alt="fan.username"
                  class="h-full w-full object-cover"
                />
                <div v-else class="flex h-full w-full items-center justify-center text-lg font-bold text-slate-400">
                  {{ getInitial(fan.username) }}
                </div>
              </div>

              <div class="min-w-0 flex-1">
                <div class="flex items-start justify-between gap-3">
                  <div class="min-w-0">
                    <button class="truncate text-left text-lg font-semibold text-slate-900 hover:text-[#00a1d6]" @click="goToUserProfile(fan.userId)">
                      {{ fan.username }}
                    </button>
                    <p class="mt-1 text-xs text-slate-400">关注了你</p>
                  </div>
                  <span
                    class="rounded-full px-3 py-1 text-xs font-semibold"
                    :class="fan.isFollow ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'"
                  >
                    {{ fan.isFollow ? '已回关' : '待回关' }}
                  </span>
                </div>

                <p class="mt-3 line-clamp-2 text-sm leading-6 text-slate-500">
                  {{ fan.bio || '这个人很懒，暂时没有留下简介。' }}
                </p>
              </div>
            </div>

            <div class="mt-5 flex gap-3">
              <el-button
                class="flex-1"
                :type="fan.isFollow ? 'default' : 'primary'"
                :loading="followLoadingMap[fan.userId]"
                @click="toggleFollow(fan)"
              >
                {{ fan.isFollow ? '取消回关' : '回关' }}
              </el-button>
              <el-button class="flex-1" @click="goToMessage(fan.userId)">
                发消息
              </el-button>
            </div>
          </article>
        </div>

        <div v-else class="rounded-2xl bg-white px-6 py-20 shadow-sm ring-1 ring-slate-200">
          <el-empty :description="keyword ? '没有匹配到粉丝' : '暂时还没有粉丝'">
            <el-button v-if="keyword" @click="keyword = ''">清空搜索</el-button>
          </el-empty>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { followUser } from '../../api/video';
import { getUserInfo, listFansUsers } from '../../api/user';

const router = useRouter();

const keyword = ref('');
const loading = ref(false);
const userInfo = ref(null);
const fans = ref([]);
const followLoadingMap = ref({});

const filteredFans = computed(() => {
  const text = keyword.value.trim().toLowerCase();
  if (!text) {
    return fans.value;
  }
  return fans.value.filter(item => {
    const username = String(item.username || '').toLowerCase();
    const bio = String(item.bio || '').toLowerCase();
    return username.includes(text) || bio.includes(text);
  });
});

const mutualFollowCount = computed(() => fans.value.filter(item => item.isFollow).length);

async function initialize() {
  loading.value = true;
  try {
    const [info, fanList] = await Promise.all([
      getUserInfo(),
      listFansUsers()
    ]);
    userInfo.value = info;
    fans.value = Array.isArray(fanList) ? fanList : [];
  } catch (error) {
    ElMessage.error(error.message || '加载粉丝列表失败');
  } finally {
    loading.value = false;
  }
}

async function toggleFollow(fan) {
  if (!fan?.userId || followLoadingMap.value[fan.userId]) {
    return;
  }
  followLoadingMap.value = {
    ...followLoadingMap.value,
    [fan.userId]: true
  };

  const nextIsFollow = !fan.isFollow;
  try {
    await followUser({
      followeeId: fan.userId,
      operation: nextIsFollow ? 1 : 0
    });
    fan.isFollow = nextIsFollow;
    ElMessage.success(nextIsFollow ? '回关成功' : '已取消回关');
  } catch (error) {
    ElMessage.error(error.message || '关注操作失败');
  } finally {
    followLoadingMap.value = {
      ...followLoadingMap.value,
      [fan.userId]: false
    };
  }
}

function goToUserProfile(userId) {
  router.push({
    name: 'user-profile',
    params: { userId }
  });
}

function goToMessage(userId) {
  router.push({
    name: 'messages',
    params: { userId },
    query: { tab: 'chat' }
  });
}

function getInitial(username) {
  return String(username || 'U').charAt(0).toUpperCase();
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
