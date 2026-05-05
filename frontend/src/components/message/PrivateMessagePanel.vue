<template>
  <section class="overflow-hidden rounded-[28px] bg-white shadow-sm ring-1 ring-black/5">
    <div class="flex min-h-[680px]">
      <aside class="flex w-[320px] shrink-0 flex-col border-r border-[#edf1f5] bg-[#fbfcfd]">
        <div class="border-b border-[#edf1f5] px-5 py-4">
          <div class="flex items-center justify-between">
            <div>
              <h2 class="text-xl font-semibold text-[#18191c]">我的消息</h2>
              <p class="mt-1 text-sm text-[#8b95a1]">
                {{ wsConnected ? '实时连接中' : '连接中断，正在重连' }}
              </p>
            </div>
            <button
              class="rounded-full border border-[#dfe5ec] px-3 py-1.5 text-sm text-[#61666d] transition-colors hover:border-[#00a1d6] hover:text-[#00a1d6]"
              @click="refreshSessions"
            >
              刷新
            </button>
          </div>
        </div>

        <div class="flex-1 overflow-y-auto px-3 py-3">
          <div v-if="sessionLoading" class="space-y-3">
            <div
              v-for="index in 6"
              :key="index"
              class="flex animate-pulse items-center gap-3 rounded-2xl px-3 py-3"
            >
              <div class="h-12 w-12 rounded-full bg-[#e9eef3]"></div>
              <div class="min-w-0 flex-1 space-y-2">
                <div class="h-4 w-24 rounded bg-[#e9eef3]"></div>
                <div class="h-3 w-36 rounded bg-[#eef2f6]"></div>
              </div>
            </div>
          </div>

          <template v-else-if="sessionList.length > 0">
            <button
              v-for="session in sessionList"
              :key="session.withUserId"
              class="mb-2 flex w-full items-center gap-3 rounded-2xl px-3 py-3 text-left transition-all"
              :class="selectedUserId === session.withUserId ? 'bg-[#eef8fd] shadow-sm ring-1 ring-[#c7ebfa]' : 'hover:bg-[#f3f6f9]'"
              @click="selectConversation(session.withUserId)"
            >
              <div class="relative">
                <div class="flex h-12 w-12 items-center justify-center overflow-hidden rounded-full bg-gradient-to-br from-[#00a1d6] to-[#43c7ef] text-lg font-semibold text-white">
                  <img
                    v-if="session.withAvatarUrl"
                    :src="session.withAvatarUrl"
                    :alt="getSessionName(session)"
                    class="h-full w-full object-cover"
                  />
                  <span v-else>{{ getUserInitial(getSessionName(session)) }}</span>
                </div>
                <span
                  v-if="session.unreadCount > 0"
                  class="absolute -right-1 -top-1 min-w-[20px] rounded-full bg-[#fb7299] px-1.5 text-center text-xs leading-5 text-white"
                >
                  {{ formatUnread(session.unreadCount) }}
                </span>
              </div>
              <div class="min-w-0 flex-1">
                <div class="flex items-center justify-between gap-3">
                  <div class="flex min-w-0 items-center gap-2">
                    <p class="truncate text-sm font-semibold text-[#18191c]">{{ getSessionName(session) }}</p>
                    <span v-if="session.withUserBanned" class="shrink-0 rounded-full bg-rose-50 px-2 py-0.5 text-[10px] font-medium text-rose-500">封禁</span>
                    <span v-if="session.withUserBlack" class="shrink-0 rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-medium text-slate-500">黑名单</span>
                  </div>
                  <span class="shrink-0 text-xs text-[#a0a7b1]">{{ formatSessionTime(session.lastTime) }}</span>
                </div>
                <p class="mt-1 truncate text-sm" :class="session.unreadCount > 0 ? 'text-[#18191c]' : 'text-[#7c8794]'">
                  {{ getSessionPreview(session) }}
                </p>
              </div>
            </button>
          </template>

          <div v-else class="flex h-full flex-col items-center justify-center px-6 text-center">
            <div class="flex h-16 w-16 items-center justify-center rounded-full bg-[#eef4f8] text-2xl text-[#9cb5c3]">聊</div>
            <h2 class="mt-5 text-lg font-medium text-[#18191c]">还没有私聊消息</h2>
            <p class="mt-2 text-sm leading-6 text-[#8b95a1]">去别人的主页点击“发消息”，就可以开始聊天了。</p>
          </div>
        </div>
      </aside>

      <section class="flex min-w-0 flex-1 flex-col">
        <template v-if="activeSession">
          <header class="border-b border-[#edf1f5] px-6 py-4">
            <div class="flex items-center gap-4">
              <div class="flex h-12 w-12 items-center justify-center overflow-hidden rounded-full bg-gradient-to-br from-[#00a1d6] to-[#43c7ef] text-lg font-semibold text-white">
                <img
                  v-if="activeSession.withAvatarUrl"
                  :src="activeSession.withAvatarUrl"
                  :alt="getSessionName(activeSession)"
                  class="h-full w-full object-cover"
                />
                <span v-else>{{ getUserInitial(getSessionName(activeSession)) }}</span>
              </div>
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2">
                  <p class="truncate text-lg font-semibold text-[#18191c]">{{ getSessionName(activeSession) }}</p>
                  <span v-if="activeSession.withUserBanned" class="rounded-full bg-rose-50 px-2.5 py-0.5 text-xs font-medium text-rose-500">已封禁</span>
                  <span v-if="activeSession.withUserBlack" class="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-500">黑名单</span>
                </div>
                <p class="mt-1 text-sm text-[#8b95a1]">{{ activeSessionStatusText }}</p>
              </div>
              <button
                class="rounded-full border border-[#dfe5ec] px-4 py-2 text-sm text-[#61666d] transition-colors hover:border-[#00a1d6] hover:text-[#00a1d6]"
                @click="goToUserProfile(activeSession.withUserId)"
              >
                查看主页
              </button>
            </div>
          </header>

          <div ref="messageScrollerRef" class="flex-1 overflow-y-auto bg-[linear-gradient(180deg,#ffffff_0%,#f8fbfd_100%)] px-6 py-5">
            <div class="mx-auto flex max-w-3xl flex-col gap-4">
              <div class="flex justify-center">
                <button
                  v-if="hasMoreHistory"
                  class="rounded-full border border-[#dfe5ec] bg-white px-4 py-2 text-sm text-[#61666d] transition-colors hover:border-[#00a1d6] hover:text-[#00a1d6]"
                  :disabled="messageLoading"
                  @click="loadOlderMessages"
                >
                  {{ messageLoading ? '加载中...' : '加载更早消息' }}
                </button>
                <span v-else class="text-xs text-[#a0a7b1]">没有更多消息了</span>
              </div>

              <div v-if="messageList.length === 0 && !messageLoading" class="py-20 text-center">
                <p class="text-4xl text-[#d0d7de]">聊</p>
                <p class="mt-3 text-base text-[#61666d]">这里还没有消息</p>
                <p class="mt-2 text-sm text-[#9499a0]">发送第一条消息，开启这段对话。</p>
              </div>

              <div v-if="activeSessionRestricted" class="rounded-2xl border border-amber-100 bg-amber-50 px-4 py-3 text-sm leading-6 text-amber-700">
                {{ activeSessionRestrictionText }}
              </div>

              <article
                v-for="message in messageList"
                :key="getMessageKey(message)"
                class="flex"
                :class="isOwnMessage(message) ? 'justify-end' : 'justify-start'"
              >
                <div
                  class="flex max-w-[75%] flex-col"
                  :class="isOwnMessage(message) ? 'items-end' : 'items-start'"
                >
                  <div
                    v-if="message.textContent"
                    class="flex max-w-full items-center gap-2"
                    :class="isOwnMessage(message) ? 'justify-end' : 'justify-start'"
                  >
                    <span
                      v-if="isOwnMessage(message) && isFailedMessage(message)"
                      class="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-rose-500 text-xs font-bold leading-none text-white"
                      :title="getMessageFailReasonText(message)"
                    >
                      !
                    </span>
                    <div
                      class="inline-block max-w-full whitespace-pre-wrap break-words rounded-3xl px-4 py-3 text-sm leading-7 shadow-sm"
                      :class="isOwnMessage(message) ? 'bg-[#00a1d6] text-white' : 'border border-[#ebf0f4] bg-white text-[#18191c]'"
                    >
                      {{ message.textContent }}
                    </div>
                  </div>
                  <button
                    v-if="message.shareVideoId"
                    class="mt-2 w-full max-w-[340px] overflow-hidden rounded-[24px] border border-[#d9e4ec] bg-white text-left shadow-sm transition-all hover:-translate-y-0.5 hover:border-[#8fd5ee] hover:shadow-[0_18px_50px_-30px_rgba(0,161,214,0.55)]"
                    @click="goToVideo(message.shareVideoId)"
                  >
                    <div class="flex gap-3 p-3">
                      <div class="h-20 w-[132px] shrink-0 overflow-hidden rounded-2xl bg-[linear-gradient(135deg,#d9eef6_0%,#eff6fb_100%)]">
                        <img
                          v-if="getVideoCard(message.shareVideoId)?.coverUrl"
                          :src="getVideoCard(message.shareVideoId).coverUrl"
                          :alt="getVideoCard(message.shareVideoId)?.title || `视频 ${message.shareVideoId}`"
                          class="h-full w-full object-cover"
                        />
                        <div v-else class="flex h-full w-full items-center justify-center text-xs font-medium text-[#6b7c8f]">
                          视频加载中
                        </div>
                      </div>
                      <div class="min-w-0 flex-1">
                        <div class="flex items-center gap-2 text-[11px] font-medium text-[#00a1d6]">
                          <span class="rounded-full bg-[#e8f7fd] px-2 py-0.5">视频分享</span>
                          <span class="text-[#9aa7b4]">ID {{ message.shareVideoId }}</span>
                        </div>
                        <p class="mt-2 line-clamp-2 text-sm font-semibold leading-6 text-[#18191c]">
                          {{ getVideoCard(message.shareVideoId)?.title || '正在加载视频信息...' }}
                        </p>
                        <div class="mt-3 flex items-center gap-3 text-xs text-[#7c8794]">
                          <span>{{ formatVideoDuration(getVideoCard(message.shareVideoId)?.duration) }}</span>
                          <span>{{ formatVideoPlayCount(getVideoCard(message.shareVideoId)?.playCount) }}</span>
                          <span class="truncate">{{ getVideoCard(message.shareVideoId)?.username || '视频详情页' }}</span>
                        </div>
                      </div>
                    </div>
                  </button>
                  <div
                    class="mt-2 flex items-center gap-2 px-1 text-xs"
                    :class="isOwnMessage(message) ? 'text-[#8ca0af]' : 'text-[#a0a7b1]'"
                  >
                    <span>{{ formatMessageTime(message.createTime || message.sendTime) }}</span>
                    <span v-if="isOwnMessage(message)">{{ getMessageStatusText(message) }}</span>
                  </div>
                </div>
              </article>
            </div>
          </div>

          <footer class="border-t border-[#edf1f5] px-6 py-4">
            <div class="mx-auto max-w-3xl">
              <div class="rounded-[24px] border border-[#dfe5ec] bg-white p-3 shadow-[0_10px_40px_-30px_rgba(0,0,0,0.35)]">
                <textarea
                  v-model="draftMessage"
                  rows="4"
                  class="w-full resize-none border-0 p-2 text-sm leading-7 text-[#18191c] outline-none"
                  placeholder="输入消息，按 Enter 发送，Shift + Enter 换行"
                  @keydown="handleDraftKeydown"
                ></textarea>
                <div class="mt-3 flex items-center justify-between px-2 pb-1">
                  <p class="text-xs text-[#9aa4af]">
                    {{ activeSessionRestricted ? activeSessionRestrictionText : (wsConnected ? '消息实时同步中' : '连接异常，发送前请等待重连') }}
                  </p>
                  <button
                    class="rounded-full bg-[#00a1d6] px-5 py-2 text-sm font-medium text-white transition-colors hover:bg-[#0093c7] disabled:cursor-not-allowed disabled:bg-[#b7d8e5]"
                    :disabled="sending || !draftMessage.trim() || !wsConnected"
                    @click="handleSendMessage"
                  >
                    {{ sending ? '发送中...' : '发送' }}
                  </button>
                </div>
              </div>
            </div>
          </footer>
        </template>

        <div v-else class="flex flex-1 flex-col items-center justify-center bg-[linear-gradient(180deg,#ffffff_0%,#f8fbfd_100%)] px-10 text-center">
          <div class="flex h-20 w-20 items-center justify-center rounded-full bg-[#eef4f8] text-3xl text-[#9cb5c3]">信</div>
          <h2 class="mt-6 text-2xl font-semibold text-[#18191c]">选择一个会话开始聊天</h2>
          <p class="mt-3 max-w-md text-sm leading-7 text-[#8b95a1]">这里会显示最近聊天记录、未读消息和实时状态。你也可以从用户主页直接跳到这里发起私聊。</p>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getUserInfo } from '../../api/user';
import {
  buildChatWebSocketUrl,
  getChatHistory,
  getChatSessions,
  getChatUnreadTotal,
  getChatWsPath,
  markChatRead
} from '../../api/chat';
import { getVideoDetail } from '../../api/video';
import { getToken, getUserId } from '../../utils/auth';
import { emitChatUnreadChange } from '../../utils/chat';

const CHAT_PAGE_SIZE = 20;
const STATUS_SENDING = -1;
const STATUS_FAIL = 3;
const VIDEO_LINK_PATTERN = /(https?:\/\/[^\s]+|\/video\/\d+[^\s]*)/ig;
const FAIL_REASON_TEXT_MAP = {
  0: '',
  1: '系统异常',
  2: '你已被封禁',
  3: '对方已被封禁',
  4: '你已拉黑对方',
  5: '对方已拉黑你'
};

const route = useRoute();
const router = useRouter();

const sessionList = ref([]);
const sessionLoading = ref(false);
const selectedUserId = ref(null);
const messageList = ref([]);
const messageLoading = ref(false);
const hasMoreHistory = ref(true);
const draftMessage = ref('');
const sending = ref(false);
const wsConnected = ref(false);
const messageScrollerRef = ref(null);

let ws = null;
let heartbeatTimer = null;
let reconnectTimer = null;
let reconnectAttempts = 0;
let manualClose = false;
let currentHistoryRequestId = 0;

const userSummaryCache = new Map();
const pendingUserSummaryMap = new Map();
const videoCardCache = ref({});
const pendingVideoCardMap = new Map();

const currentUserId = computed(() => getUserId());
const routeTargetUserId = computed(() => parseUserId(route.params.userId));
const activeSession = computed(() => {
  if (!selectedUserId.value) {
    return null;
  }
  return sessionList.value.find(item => item.withUserId === selectedUserId.value) || null;
});
const activeSessionRestricted = computed(() => {
  return Boolean(activeSession.value?.withUserBanned) || Boolean(activeSession.value?.withUserBlack);
});
const activeSessionRestrictionText = computed(() => {
  if (activeSession.value?.withUserBanned) {
    return '对方账号已被封禁，消息会记录为发送失败。';
  }
  if (activeSession.value?.withUserBlack) {
    return '当前存在黑名单关系，消息会记录为发送失败且不会投递给对方。';
  }
  return '';
});
const activeSessionStatusText = computed(() => {
  return activeSessionRestrictionText.value || `与 ${getSessionName(activeSession.value)} 的私聊会话`;
});

function parseUserId(rawValue) {
  const userId = Number(rawValue);
  return Number.isFinite(userId) && userId > 0 ? userId : null;
}

function normalizeTimeString(value) {
  return value ? String(value).replace(' ', 'T') : '';
}

function toTimestamp(value) {
  const normalized = normalizeTimeString(value);
  const time = normalized ? new Date(normalized).getTime() : 0;
  return Number.isFinite(time) ? time : 0;
}

function sortSessions() {
  sessionList.value = [...sessionList.value].sort((left, right) => {
    const rightTime = Math.max(toTimestamp(right.lastTime), Number(right.lastMsgId || 0));
    const leftTime = Math.max(toTimestamp(left.lastTime), Number(left.lastMsgId || 0));
    return rightTime - leftTime;
  });
}

function getSessionName(session) {
  return session?.withUsername || `用户 ${session?.withUserId ?? ''}`;
}

function getUserInitial(name) {
  return String(name || 'U').trim().charAt(0).toUpperCase() || 'U';
}

function formatUnread(count) {
  return count > 99 ? '99+' : String(count);
}

function formatSessionTime(value) {
  const timestamp = toTimestamp(value);
  if (!timestamp) {
    return '';
  }
  const date = new Date(timestamp);
  const now = new Date();
  const sameDay = date.toDateString() === now.toDateString();
  if (sameDay) {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  }
  const month = date.getMonth() + 1;
  const day = date.getDate();
  return `${month}-${day}`;
}

function formatMessageTime(value) {
  const timestamp = toTimestamp(value);
  if (!timestamp) {
    return '';
  }
  const date = new Date(timestamp);
  const now = new Date();
  const sameDay = date.toDateString() === now.toDateString();
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  if (sameDay) {
    return `${hour}:${minute}`;
  }
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${month}-${day} ${hour}:${minute}`;
}

function getMessageKey(message) {
  return message.serverMsgId || message.id || message.clientMsgId;
}

function trimLinkCandidate(value) {
  return String(value || '').trim().replace(/[)\]}>，。！？；;,]+$/g, '');
}

function parseVideoIdFromLink(rawLink) {
  const link = trimLinkCandidate(rawLink);
  if (!link) {
    return null;
  }
  try {
    const url = link.startsWith('http://') || link.startsWith('https://')
      ? new URL(link)
      : new URL(link, window.location.origin);
    const matched = url.pathname.match(/^\/video\/(\d+)(?:\/)?$/);
    if (!matched) {
      return null;
    }
    const videoId = Number(matched[1]);
    return Number.isFinite(videoId) && videoId > 0 ? videoId : null;
  } catch (error) {
    return null;
  }
}

function parseShareMessageContent(content) {
  const rawContent = String(content || '');
  const matches = rawContent.match(VIDEO_LINK_PATTERN) || [];
  for (const rawLink of matches) {
    const videoId = parseVideoIdFromLink(rawLink);
    if (!videoId) {
      continue;
    }
    return {
      textContent: rawContent.replace(rawLink, '').trim(),
      shareVideoId: videoId
    };
  }
  return {
    textContent: rawContent,
    shareVideoId: null
  };
}

function applyMessagePresentation(message) {
  const parsed = parseShareMessageContent(message.content);
  if (parsed.shareVideoId) {
    ensureVideoCard(parsed.shareVideoId);
  }
  return {
    ...message,
    textContent: parsed.textContent,
    shareVideoId: parsed.shareVideoId
  };
}

function isOwnMessage(message) {
  return Number(message.fromUserId) === Number(currentUserId.value);
}

function getMessageStatusText(message) {
  if (message.status === STATUS_SENDING) {
    return '发送中';
  }
  if (message.status === 4) {
    return '已读';
  }
  if (message.status === 2) {
    return '已接收';
  }
  if (message.status === 1) {
    return '已送达';
  }
  if (isFailedMessage(message)) {
    return getMessageFailReasonText(message) || '发送失败';
  }
  return '已发送';
}

function isFailedMessage(message) {
  return Number(message?.status) === STATUS_FAIL;
}

function getMessageFailReasonText(message) {
  return message?.failReasonText || FAIL_REASON_TEXT_MAP[Number(message?.failReason || 0)] || '发送失败';
}

function computeUnreadTotal() {
  return sessionList.value.reduce((sum, item) => sum + Number(item.unreadCount || 0), 0);
}

function notifyUnreadChange() {
  emitChatUnreadChange(computeUnreadTotal());
}

function getVideoCard(videoId) {
  return videoCardCache.value[videoId] || null;
}

function formatVideoDuration(duration) {
  const totalSeconds = Number(duration || 0);
  if (!totalSeconds) {
    return '时长未知';
  }
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  if (hours > 0) {
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
  }
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

function formatVideoPlayCount(playCount) {
  const count = Number(playCount || 0);
  if (!count) {
    return '0 播放';
  }
  if (count >= 10000) {
    return `${(count / 10000).toFixed(count >= 100000 ? 0 : 1)}万播放`;
  }
  return `${count} 播放`;
}

function getSessionPreview(session) {
  const content = String(session?.lastContent || '').trim();
  if (!content) {
    return '开始聊天吧';
  }
  const parsed = parseShareMessageContent(content);
  if (!parsed.shareVideoId) {
    return content;
  }
  return parsed.textContent ? `${parsed.textContent} [视频]` : '分享了一个视频';
}

async function syncUnreadTotalFromServer() {
  try {
    const total = await getChatUnreadTotal();
    emitChatUnreadChange(total);
  } catch (error) {
    console.error('同步未读数失败:', error);
    notifyUnreadChange();
  }
}

function updateSession(withUserId, patch = {}) {
  if (!withUserId) {
    return null;
  }
  const nextList = [...sessionList.value];
  const index = nextList.findIndex(item => item.withUserId === withUserId);
  if (index === -1) {
      nextList.push({
        withUserId,
        withUsername: patch.withUsername || '',
        withAvatarUrl: patch.withAvatarUrl || '',
        withUserBanned: Boolean(patch.withUserBanned),
        withUserBlack: Boolean(patch.withUserBlack),
        lastMsgId: patch.lastMsgId || null,
        lastContent: patch.lastContent || '',
        lastTime: patch.lastTime || null,
      unreadCount: Number(patch.unreadCount || 0)
    });
  } else {
    nextList[index] = {
      ...nextList[index],
      ...patch,
      unreadCount: patch.unreadCount == null ? Number(nextList[index].unreadCount || 0) : Number(patch.unreadCount || 0)
    };
  }
  sessionList.value = nextList;
  sortSessions();
  notifyUnreadChange();
  return sessionList.value.find(item => item.withUserId === withUserId) || null;
}

async function ensureUserSummary(userId) {
  if (!userId) {
    return null;
  }
  if (userSummaryCache.has(userId)) {
    return userSummaryCache.get(userId);
  }
  if (pendingUserSummaryMap.has(userId)) {
    return pendingUserSummaryMap.get(userId);
  }
  const task = getUserInfo(userId)
    .then((info) => {
      const summary = info ? {
        userId: info.userId,
        username: info.username,
        avatarUrl: info.avatarUrl,
        isBanned: Boolean(info.isBanned),
        isBlack: Boolean(info.isBlack)
      } : null;
      if (summary) {
        userSummaryCache.set(userId, summary);
      }
      return summary;
    })
    .finally(() => {
      pendingUserSummaryMap.delete(userId);
    });
  pendingUserSummaryMap.set(userId, task);
  return task;
}

async function hydrateSessionUsers(sessions) {
  const tasks = sessions
    .filter(item => item?.withUserId && (!item.withUsername || !item.withAvatarUrl))
    .map(async (item) => {
      const summary = await ensureUserSummary(item.withUserId);
      if (!summary) {
        return;
      }
      item.withUsername = item.withUsername || summary.username;
      item.withAvatarUrl = item.withAvatarUrl || summary.avatarUrl;
      item.withUserBanned = item.withUserBanned || summary.isBanned;
      item.withUserBlack = item.withUserBlack || summary.isBlack;
    });
  await Promise.all(tasks);
}

async function ensureVideoCard(videoId) {
  if (!videoId) {
    return null;
  }
  if (videoCardCache.value[videoId]) {
    return videoCardCache.value[videoId];
  }
  if (pendingVideoCardMap.has(videoId)) {
    return pendingVideoCardMap.get(videoId);
  }
  const task = getVideoDetail(videoId)
    .then((detail) => {
      if (!detail) {
        return null;
      }
      videoCardCache.value = {
        ...videoCardCache.value,
        [videoId]: detail
      };
      return detail;
    })
    .catch((error) => {
      console.error('加载视频卡片失败:', error);
      return null;
    })
    .finally(() => {
      pendingVideoCardMap.delete(videoId);
    });
  pendingVideoCardMap.set(videoId, task);
  return task;
}

async function ensureConversationEntry(withUserId) {
  if (!withUserId || Number(withUserId) === Number(currentUserId.value)) {
    return null;
  }
  let session = sessionList.value.find(item => item.withUserId === withUserId);
  if (session) {
    if (!session.withUsername || !session.withAvatarUrl) {
      const summary = await ensureUserSummary(withUserId);
      if (summary) {
        session = updateSession(withUserId, {
          withUsername: session.withUsername || summary.username,
          withAvatarUrl: session.withAvatarUrl || summary.avatarUrl,
          withUserBanned: session.withUserBanned || summary.isBanned,
          withUserBlack: session.withUserBlack || summary.isBlack
        });
      }
    }
    return session;
  }

  const summary = await ensureUserSummary(withUserId);
  if (!summary) {
    throw new Error('用户不存在');
  }
  return updateSession(withUserId, {
    withUsername: summary.username,
    withAvatarUrl: summary.avatarUrl,
    withUserBanned: summary.isBanned,
    withUserBlack: summary.isBlack,
    unreadCount: 0,
    lastContent: '',
    lastTime: null,
    lastMsgId: null
  });
}

function normalizeHistoryMessage(item) {
  return applyMessagePresentation({
    ...item,
    serverMsgId: item.id,
    clientMsgId: item.clientMsgId || null,
    createTime: item.createTime,
    sendTime: item.createTime,
    failReason: item.failReason || 0,
    failReasonText: item.failReasonText || ''
  });
}

function normalizeSocketMessage(payload) {
  return applyMessagePresentation({
    id: payload.serverMsgId,
    serverMsgId: payload.serverMsgId,
    clientMsgId: payload.clientMsgId || null,
    fromUserId: payload.fromUserId,
    toUserId: payload.toUserId,
    content: payload.content,
    status: payload.status == null ? 1 : payload.status,
    failReason: payload.failReason || 0,
    failReasonText: payload.failReasonText || '',
    createTime: payload.sendTime,
    sendTime: payload.sendTime
  });
}

function mergeMessages(messages, { append = true } = {}) {
  const merged = append ? [...messageList.value] : [];
  messages.forEach((message) => {
    const key = getMessageKey(message);
    const index = merged.findIndex(item => getMessageKey(item) === key);
    if (index === -1) {
      merged.push(message);
    } else {
      merged[index] = {
        ...merged[index],
        ...message
      };
    }
  });
  merged.sort((left, right) => {
    const leftRank = Number(left.serverMsgId || left.id || 0);
    const rightRank = Number(right.serverMsgId || right.id || 0);
    if (leftRank && rightRank && leftRank !== rightRank) {
      return leftRank - rightRank;
    }
    return toTimestamp(left.createTime || left.sendTime) - toTimestamp(right.createTime || right.sendTime);
  });
  messageList.value = merged;
}

async function scrollToBottom() {
  await nextTick();
  const scroller = messageScrollerRef.value;
  if (!scroller) {
    return;
  }
  scroller.scrollTop = scroller.scrollHeight;
}

function getOldestMessageId() {
  const ids = messageList.value
    .map(item => Number(item.serverMsgId || item.id || 0))
    .filter(id => id > 0);
  if (ids.length === 0) {
    return null;
  }
  return Math.min(...ids);
}

function getLastIncomingMessageId(withUserId) {
  const incomingMessages = messageList.value
    .filter(item => Number(item.fromUserId) === Number(withUserId))
    .map(item => Number(item.serverMsgId || item.id || 0))
    .filter(id => id > 0);
  if (incomingMessages.length === 0) {
    return null;
  }
  return Math.max(...incomingMessages);
}

async function markConversationRead(withUserId) {
  const upToMsgId = getLastIncomingMessageId(withUserId);
  if (!upToMsgId) {
    updateSession(withUserId, { unreadCount: 0 });
    return;
  }
  updateSession(withUserId, { unreadCount: 0 });
  try {
    await markChatRead(withUserId, upToMsgId);
    messageList.value = messageList.value.map((item) => {
      if (Number(item.fromUserId) === Number(withUserId) && Number(item.serverMsgId || item.id || 0) <= upToMsgId) {
        return {
          ...item,
          status: 4
        };
      }
      return item;
    });
    await syncUnreadTotalFromServer();
  } catch (error) {
    console.error('标记消息已读失败:', error);
  }
}

async function handleConversationChange(withUserId) {
  if (!withUserId) {
    selectedUserId.value = null;
    messageList.value = [];
    return;
  }
  if (Number(withUserId) === Number(currentUserId.value)) {
    ElMessage.warning('不能给自己发私信');
    router.replace({
      name: 'messages',
      query: { ...route.query, tab: 'chat' }
    });
    return;
  }

  const requestId = ++currentHistoryRequestId;
  selectedUserId.value = withUserId;
  draftMessage.value = '';
  hasMoreHistory.value = true;
  messageLoading.value = true;
  messageList.value = [];

  try {
    await ensureConversationEntry(withUserId);
    const history = await getChatHistory(withUserId, null, CHAT_PAGE_SIZE);
    if (requestId !== currentHistoryRequestId) {
      return;
    }
    const normalized = Array.isArray(history)
      ? history.map(normalizeHistoryMessage).reverse()
      : [];
    messageList.value = normalized;
    hasMoreHistory.value = normalized.length >= CHAT_PAGE_SIZE;
    await scrollToBottom();
    await markConversationRead(withUserId);
  } catch (error) {
    if (requestId !== currentHistoryRequestId) {
      return;
    }
    console.error('加载聊天记录失败:', error);
    ElMessage.error(error.message || '加载聊天记录失败');
  } finally {
    if (requestId === currentHistoryRequestId) {
      messageLoading.value = false;
    }
  }
}

async function loadOlderMessages() {
  if (!selectedUserId.value || messageLoading.value || !hasMoreHistory.value) {
    return;
  }
  const beforeId = getOldestMessageId();
  if (!beforeId) {
    hasMoreHistory.value = false;
    return;
  }

  try {
    messageLoading.value = true;
    const history = await getChatHistory(selectedUserId.value, beforeId, CHAT_PAGE_SIZE);
    const normalized = Array.isArray(history)
      ? history.map(normalizeHistoryMessage).reverse()
      : [];
    if (normalized.length === 0) {
      hasMoreHistory.value = false;
      return;
    }
    messageList.value = [...normalized, ...messageList.value];
    hasMoreHistory.value = normalized.length >= CHAT_PAGE_SIZE;
  } catch (error) {
    console.error('加载更早消息失败:', error);
    ElMessage.error(error.message || '加载更早消息失败');
  } finally {
    messageLoading.value = false;
  }
}

function generateClientMessageId() {
  return `c_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
}

function sendSocketEnvelope(type, data) {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    return false;
  }
  ws.send(JSON.stringify({ type, data }));
  return true;
}

async function handleSendMessage() {
  const toUserId = selectedUserId.value;
  const content = draftMessage.value.trim();
  if (!toUserId || !content) {
    return;
  }
  if (!wsConnected.value) {
    connectWebSocket();
    ElMessage.warning('消息连接恢复中，请稍后重试');
    return;
  }

  const clientMsgId = generateClientMessageId();
  const now = new Date().toISOString();
  const localMessage = {
    id: null,
    serverMsgId: null,
    clientMsgId,
    fromUserId: currentUserId.value,
    toUserId,
    content,
    status: STATUS_SENDING,
    createTime: now,
    sendTime: now
  };

  mergeMessages([applyMessagePresentation(localMessage)], { append: true });
  updateSession(toUserId, {
    lastContent: content,
    lastTime: now
  });
  draftMessage.value = '';
  sending.value = true;
  await scrollToBottom();

  const sent = sendSocketEnvelope('chat', {
    clientMsgId,
    toUserId,
    content
  });

  if (!sent) {
    messageList.value = messageList.value.map((item) => {
      if (item.clientMsgId === clientMsgId) {
        return {
          ...item,
          status: STATUS_FAIL,
          failReason: 1,
          failReasonText: FAIL_REASON_TEXT_MAP[1]
        };
      }
      return item;
    });
    sending.value = false;
    connectWebSocket();
    ElMessage.error('消息发送失败，请稍后重试');
    return;
  }

  setTimeout(() => {
    sending.value = false;
  }, 200);
}

function applySendAck(payload) {
  if (!payload?.clientMsgId) {
    return;
  }
  messageList.value = messageList.value.map((item) => {
    if (item.clientMsgId === payload.clientMsgId) {
      return {
        ...item,
        id: payload.serverMsgId || item.id,
        serverMsgId: payload.serverMsgId || item.serverMsgId,
        status: payload.status == null ? (payload.delivered ? 1 : 0) : payload.status,
        failReason: payload.failReason || 0,
        failReasonText: payload.failReasonText || ''
      };
    }
    return item;
  });
  updateSession(payload.toUserId, {
    lastMsgId: payload.serverMsgId || null,
    lastTime: new Date().toISOString()
  });
}

async function handleIncomingChat(payload) {
  if (!payload?.fromUserId || !payload?.toUserId) {
    return;
  }
  const normalized = normalizeSocketMessage(payload);
  mergeMessages([normalized], { append: true });
  await ensureConversationEntry(payload.fromUserId);

  const isActiveConversation = Number(selectedUserId.value) === Number(payload.fromUserId);
  const currentUnread = sessionList.value.find(item => item.withUserId === payload.fromUserId)?.unreadCount || 0;
  updateSession(payload.fromUserId, {
    lastMsgId: payload.serverMsgId,
    lastContent: payload.content,
    lastTime: payload.sendTime,
    unreadCount: isActiveConversation ? 0 : currentUnread + 1
  });

  sendSocketEnvelope('chat_recv_ack', {
    serverMsgId: payload.serverMsgId
  });

  if (isActiveConversation) {
    await scrollToBottom();
    await markConversationRead(payload.fromUserId);
    return;
  }

  notifyUnreadChange();
}

function handleReadEvent(payload) {
  if (!payload?.readByUserId) {
    return;
  }
  if (Number(payload.readByUserId) !== Number(selectedUserId.value)) {
    return;
  }

  const upToMsgId = payload.upToMsgId == null ? Number.MAX_SAFE_INTEGER : Number(payload.upToMsgId);
  messageList.value = messageList.value.map((item) => {
    if (isOwnMessage(item) && Number(item.serverMsgId || item.id || 0) <= upToMsgId) {
      return {
        ...item,
        status: 4
      };
    }
    return item;
  });
}

function handleSocketMessage(event) {
  let envelope;
  try {
    envelope = JSON.parse(event.data);
  } catch (error) {
    console.error('解析私聊消息失败:', error);
    return;
  }

  const { type, data } = envelope || {};
  if (type === 'chat') {
    handleIncomingChat(data);
    return;
  }
  if (type === 'chat_send_ack') {
    applySendAck(data);
    return;
  }
  if (type === 'read') {
    handleReadEvent(data);
    return;
  }
  if (type === 'error' && data) {
    ElMessage.error(String(data));
  }
}

function clearHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
    heartbeatTimer = null;
  }
}

function startHeartbeat() {
  clearHeartbeat();
  heartbeatTimer = setInterval(() => {
    sendSocketEnvelope('ping', {});
  }, 25000);
}

function clearReconnect() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
}

function scheduleReconnect() {
  clearReconnect();
  if (manualClose || !getToken()) {
    return;
  }
  const timeout = Math.min(10000, 1500 * (reconnectAttempts + 1));
  reconnectTimer = setTimeout(() => {
    reconnectAttempts += 1;
    connectWebSocket();
  }, timeout);
}

async function connectWebSocket() {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return;
  }
  if (!getToken()) {
    return;
  }

  try {
    const wsPath = await getChatWsPath();
    const wsUrl = buildChatWebSocketUrl(wsPath);
    ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      wsConnected.value = true;
      reconnectAttempts = 0;
      startHeartbeat();
    };

    ws.onmessage = handleSocketMessage;

    ws.onerror = (error) => {
      console.error('私聊连接异常:', error);
    };

    ws.onclose = () => {
      wsConnected.value = false;
      clearHeartbeat();
      if (!manualClose) {
        scheduleReconnect();
      }
    };
  } catch (error) {
    wsConnected.value = false;
    console.error('连接私聊服务失败:', error);
    scheduleReconnect();
  }
}

function disconnectWebSocket() {
  manualClose = true;
  clearHeartbeat();
  clearReconnect();
  if (ws) {
    try {
      ws.close();
    } catch (error) {
      console.error('关闭私聊连接失败:', error);
    }
    ws = null;
  }
  wsConnected.value = false;
}

async function refreshSessions() {
  try {
    sessionLoading.value = true;
    const sessions = await getChatSessions();
    const nextSessions = Array.isArray(sessions) ? sessions : [];
    await hydrateSessionUsers(nextSessions);
    sessionList.value = nextSessions.map(item => ({
      withUserId: item.withUserId,
      withUsername: item.withUsername || '',
      withAvatarUrl: item.withAvatarUrl || '',
      lastMsgId: item.lastMsgId || null,
      lastContent: item.lastContent || '',
      lastTime: item.lastTime || null,
      unreadCount: Number(item.unreadCount || 0)
    }));

    const targetUserId = routeTargetUserId.value;
    if (targetUserId) {
      await ensureConversationEntry(targetUserId);
    }
    sortSessions();
    notifyUnreadChange();
  } catch (error) {
    console.error('加载会话列表失败:', error);
    ElMessage.error(error.message || '加载会话列表失败');
  } finally {
    sessionLoading.value = false;
  }
}

function selectConversation(withUserId) {
  router.push({
    name: 'messages',
    params: { userId: withUserId },
    query: { ...route.query, tab: 'chat' }
  });
}

function goToUserProfile(withUserId) {
  router.push({
    name: 'user-profile',
    params: { userId: withUserId }
  });
}

function goToVideo(videoId) {
  if (!videoId) {
    return;
  }
  router.push(`/video/${videoId}`);
}

function handleDraftKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    handleSendMessage();
  }
}

watch(routeTargetUserId, async (nextUserId) => {
  await handleConversationChange(nextUserId);
});

watch(
  () => activeSession.value?.withUserId,
  async (withUserId) => {
    if (withUserId) {
      await ensureConversationEntry(withUserId);
    }
  }
);

onMounted(async () => {
  await refreshSessions();
  await syncUnreadTotalFromServer();
  manualClose = false;
  connectWebSocket();

  if (routeTargetUserId.value) {
    await handleConversationChange(routeTargetUserId.value);
  } else if (sessionList.value.length > 0) {
    router.replace({
      name: 'messages',
      params: { userId: sessionList.value[0].withUserId },
      query: { ...route.query, tab: 'chat' }
    });
  }
});

onBeforeUnmount(() => {
  disconnectWebSocket();
});
</script>
