export const CHAT_UNREAD_CHANGE_EVENT = 'chat-unread-change';

export function emitChatUnreadChange(total = 0) {
  if (typeof window === 'undefined') {
    return;
  }
  window.dispatchEvent(new CustomEvent(CHAT_UNREAD_CHANGE_EVENT, {
    detail: {
      total: Math.max(0, Number(total || 0))
    }
  }));
}
