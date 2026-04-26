<template>
  <div class="min-h-screen bg-[#f6f7fb]">
    <HeaderNav />

    <div class="mx-auto max-w-7xl px-4 py-8">
      <div class="mb-8 flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
        <div class="flex flex-wrap items-center gap-3">
          <button
            v-for="tab in relationTabs"
            :key="tab.type"
            class="rounded-2xl px-6 py-3 text-lg font-semibold transition-colors"
            :class="activeType === tab.type ? 'bg-[#18a8df] text-white shadow-sm' : 'bg-[#f0f2f5] text-[#5f6b7a] hover:bg-[#e5e9ef]'"
            @click="switchRelation(tab.type)"
          >
            {{ tab.label }}
          </button>
        </div>

        <label class="relative block w-full xl:w-[360px]">
          <input
            v-model.trim="keyword"
            type="text"
            class="h-14 w-full rounded-2xl border border-[#d8dee8] bg-white pl-5 pr-14 text-base text-[#111827] outline-none transition focus:border-[#18a8df] focus:ring-4 focus:ring-[#18a8df]/10"
            placeholder="输入关键词"
          />
          <svg class="pointer-events-none absolute right-5 top-1/2 h-7 w-7 -translate-y-1/2 text-[#111827]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="m21 21-4.35-4.35m1.85-5.15a7 7 0 1 1-14 0 7 7 0 0 1 14 0Z" />
          </svg>
        </label>
      </div>

      <div class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 class="text-3xl font-semibold text-[#111827]">{{ pageTitle }}</h1>
          <p class="mt-2 text-sm text-[#6b7280]">
            共 {{ relationList.length }} 位{{ activeType === 'fans' ? '粉丝' : '关注' }}
          </p>
        </div>

        <button
          class="inline-flex items-center justify-center rounded-xl border border-[#dbe2ea] bg-white px-4 py-2.5 text-sm font-medium text-[#475569] transition hover:border-[#18a8df] hover:text-[#18a8df]"
          @click="goBackProfile"
        >
          返回个人主页
        </button>
      </div>

      <div v-if="loading" class="grid gap-x-10 gap-y-8 md:grid-cols-2 xl:grid-cols-3">
        <div
          v-for="index in 6"
          :key="index"
          class="rounded-[28px] bg-white p-6 shadow-[0_12px_30px_rgba(15,23,42,0.06)]"
        >
          <div class="animate-pulse">
            <div class="flex items-start gap-5">
              <div class="h-24 w-24 rounded-full bg-[#e5e7eb]"></div>
              <div class="min-w-0 flex-1">
                <div class="h-7 w-32 rounded bg-[#e5e7eb]"></div>
                <div class="mt-3 h-5 w-full rounded bg-[#eef2f7]"></div>
                <div class="mt-2 h-5 w-4/5 rounded bg-[#eef2f7]"></div>
                <div class="mt-5 h-12 w-32 rounded-2xl bg-[#eef2f7]"></div>
              </div>
            </div>
          </div>
        </div>
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
        <h2 class="mt-5 text-xl font-semibold text-[#111827]">
          {{ keyword ? '没有找到匹配的用户' : emptyTitle }}
        </h2>
        <p class="mt-2 text-sm text-[#6b7280]">
          {{ keyword ? '换个昵称或简介关键词试试。' : emptyDescription }}
        </p>
      </div>

      <div v-else class="grid gap-x-10 gap-y-8 md:grid-cols-2 xl:grid-cols-3">
        <article
          v-for="item in filteredRelationList"
          :key="item.userId"
          class="rounded-[28px] bg-white p-6 shadow-[0_12px_30px_rgba(15,23,42,0.06)] transition-transform duration-200 hover:-translate-y-1"
        >
          <div class="flex items-start gap-5">
            <button
              class="relative h-24 w-24 flex-shrink-0 overflow-hidden rounded-full bg-[#edf2f7]"
              @click="goToUserProfile(item.userId)"
            >
              <img
                v-if="item.avatarUrl"
                :src="item.avatarUrl"
                :alt="item.username"
                class="h-full w-full object-cover"
              />
              <div v-else class="flex h-full w-full items-center justify-center bg-gradient-to-br from-[#18a8df] to-[#0f8fc5] text-3xl font-semibold text-white">
                {{ getNameInitial(item.username) }}
              </div>
            </button>

            <div class="min-w-0 flex-1">
              <button
                class="max-w-full truncate text-left text-[20px] font-semibold text-[#111827] transition hover:text-[#ff6b8f]"
                @click="goToUserProfile(item.userId)"
              >
                {{ item.username || '未命名用户' }}
              </button>
              <p class="mt-1 line-clamp-2 text-[15px] leading-7 text-[#6b7280]">
                {{ item.bio || '这个人很懒，什么都没写。' }}
              </p>

              <div class="mt-5 flex items-center gap-3">
                <button
                  v-if="!isCurrentUser(item.userId)"
                  class="inline-flex min-w-[144px] items-center justify-center rounded-2xl px-5 py-3 text-lg font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-60"
                  :class="item.isFollow ? 'bg-[#f3f4f6] text-[#8b95a1] hover:bg-[#e9edf2]' : 'bg-[#f3f4f6] text-[#ff6b8f] hover:bg-[#ffe5ec]'"
                  :disabled="followLoadingMap[item.userId]"
                  @click="toggleFollow(item)"
                >
                  {{ followLoadingMap[item.userId] ? '处理中...' : (item.isFollow ? '已关注' : '+ 关注') }}
                </button>
                <span v-else class="text-sm text-[#94a3b8]">这是你自己</span>
              </div>
            </div>
          </div>
        </article>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { listFansUsers, listFollowUsers, getUserInfo } from '../api/user';
import { followUser } from '../api/video';
import { getUserId } from '../utils/auth';
import HeaderNav from './HeaderNav.vue';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const keyword = ref('');
const relationList = ref([]);
const profileUserInfo = ref(null);
const followLoadingMap = reactive({});

const currentUserId = computed(() => String(getUserId() || ''));
const targetUserId = computed(() => String(route.params.userId || ''));
const isSelfPage = computed(() => currentUserId.value !== '' && currentUserId.value === targetUserId.value);
const activeType = computed(() => route.name === 'user-fans' ? 'fans' : 'following');

const relationTabs = computed(() => [
  {
    type: 'following',
    label: isSelfPage.value ? '我的关注' : 'TA的关注'
  },
  {
    type: 'fans',
    label: isSelfPage.value ? '我的粉丝' : 'TA的粉丝'
  }
]);

const filteredRelationList = computed(() => {
  const searchValue = keyword.value.trim().toLowerCase();
  if (!searchValue) {
    return relationList.value;
  }
  return relationList.value.filter((item) => {
    const username = String(item.username || '').toLowerCase();
    const bio = String(item.bio || '').toLowerCase();
    return username.includes(searchValue) || bio.includes(searchValue);
  });
});

const pageTitle = computed(() => {
  const username = profileUserInfo.value?.username || '用户';
  if (isSelfPage.value) {
    return activeType.value === 'fans' ? '我的粉丝' : '我的关注';
  }
  return activeType.value === 'fans' ? `${username}的粉丝` : `${username}的关注`;
});

const emptyTitle = computed(() => {
  if (activeType.value === 'fans') {
    return isSelfPage.value ? '你还没有粉丝' : 'TA还没有粉丝';
  }
  return isSelfPage.value ? '你还没有关注任何人' : 'TA还没有关注任何人';
});

const emptyDescription = computed(() => {
  if (activeType.value === 'fans') {
    return '等有人关注后，这里就会热闹起来。';
  }
  return '去发现感兴趣的创作者吧。';
});

async function loadPageData() {
  loading.value = true;
  keyword.value = '';
  relationList.value = [];

  try {
    const [userInfo, list] = await Promise.all([
      getUserInfo(route.params.userId),
      activeType.value === 'fans'
        ? listFansUsers(route.params.userId)
        : listFollowUsers(route.params.userId)
    ]);

    profileUserInfo.value = userInfo || null;
    relationList.value = Array.isArray(list) ? list : [];
  } catch (error) {
    console.error('加载关系列表失败:', error);
    profileUserInfo.value = null;
    relationList.value = [];
    ElMessage.error(error.message || '加载列表失败');
  } finally {
    loading.value = false;
  }
}

function switchRelation(type) {
  const routeName = type === 'fans' ? 'user-fans' : 'user-following';
  if (route.name === routeName) {
    return;
  }
  router.push({
    name: routeName,
    params: { userId: route.params.userId }
  });
}

function goBackProfile() {
  router.push({
    name: 'user-profile',
    params: { userId: route.params.userId }
  });
}

function goToUserProfile(userId) {
  router.push({
    name: 'user-profile',
    params: { userId }
  });
}

function getNameInitial(username) {
  return String(username || 'U').charAt(0).toUpperCase();
}

function isCurrentUser(userId) {
  return String(userId) === currentUserId.value;
}

async function toggleFollow(item) {
  if (!item?.userId || isCurrentUser(item.userId) || followLoadingMap[item.userId]) {
    return;
  }

  const nextIsFollow = !item.isFollow;
  followLoadingMap[item.userId] = true;

  try {
    await followUser({
      followeeId: item.userId,
      operation: nextIsFollow ? 1 : 0
    });

    if (activeType.value === 'following' && isSelfPage.value && !nextIsFollow) {
      relationList.value = relationList.value.filter((relation) => relation.userId !== item.userId);
    } else {
      item.isFollow = nextIsFollow;
    }

    ElMessage.success(nextIsFollow ? '关注成功' : '已取消关注');
  } catch (error) {
    console.error('关注操作失败:', error);
    ElMessage.error(error.message || '关注操作失败');
  } finally {
    followLoadingMap[item.userId] = false;
  }
}

watch(
  () => [route.params.userId, route.name],
  () => {
    loadPageData();
  },
  { immediate: true }
);
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
