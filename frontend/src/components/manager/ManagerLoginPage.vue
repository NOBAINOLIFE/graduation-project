<template>
  <div class="min-h-screen bg-[#f6f7f8] text-slate-900">
    <header class="sticky top-0 z-10 border-b border-slate-200 bg-white">
      <div class="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
        <h1 class="text-lg font-medium">管理后台登录</h1>
        <span class="text-sm text-slate-500">仅管理员可访问</span>
      </div>
    </header>

    <main class="mx-auto mt-10 max-w-5xl px-4">
      <section class="mx-auto w-full max-w-md rounded-lg bg-white p-7 shadow-sm ring-1 ring-slate-200">
        <h2 class="mb-5 text-lg font-semibold">账号登录</h2>

        <p v-if="route.query.reason === 'no-admin'" class="mb-3 rounded bg-amber-50 px-3 py-2 text-sm text-amber-700">
          当前账号不是管理员，请使用管理员账号登录。
        </p>

        <div class="space-y-4">
          <label class="block">
            <span class="mb-1 block text-sm font-medium">账号</span>
            <input
              v-model.trim="form.account"
              type="text"
              class="w-full rounded border border-slate-200 px-3 py-2 text-sm focus:border-[#00a1d6] focus:outline-none"
              placeholder="请输入账号"
            />
          </label>

          <label class="block">
            <span class="mb-1 block text-sm font-medium">密码</span>
            <input
              v-model="form.password"
              type="password"
              class="w-full rounded border border-slate-200 px-3 py-2 text-sm focus:border-[#00a1d6] focus:outline-none"
              placeholder="请输入密码"
              @keyup.enter="submitLogin"
            />
          </label>

          <p v-if="errorMessage" class="rounded bg-rose-50 px-3 py-2 text-sm text-rose-600">
            {{ errorMessage }}
          </p>

          <button
            class="w-full rounded bg-[#00a1d6] py-2.5 font-medium text-white hover:bg-[#00b5e5] disabled:cursor-not-allowed disabled:bg-slate-300"
            :disabled="isSubmitting"
            @click="submitLogin"
          >
            {{ isSubmitting ? '登录中...' : '登录管理后台' }}
          </button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { login } from '../../api/user';
import { clearAdminAuth, saveAdminLogin } from '../../utils/auth';

const router = useRouter();
const route = useRoute();

const form = reactive({
  account: '',
  password: ''
});

const isSubmitting = ref(false);
const errorMessage = ref('');

async function submitLogin() {
  errorMessage.value = '';

  if (!form.account) {
    errorMessage.value = '请输入账号';
    return;
  }
  if (!form.password) {
    errorMessage.value = '请输入密码';
    return;
  }

  const account = Number(form.account);
  if (!Number.isInteger(account) || account <= 0) {
    errorMessage.value = '账号必须为正整数';
    return;
  }

  try {
    isSubmitting.value = true;
    const data = await login({
      account: form.account,
      password: form.password,
      isAdminLogin: true
    });

    if (!data?.token) {
      throw new Error('登录失败，未获取到令牌');
    }
    if ((data?.roleCode || '').toUpperCase() !== 'ADMIN') {
      clearAdminAuth();
      throw new Error('当前账号不是管理员');
    }

    // 保存管理员登录信息
    saveAdminLogin(data);

    const redirectTarget = typeof route.query.redirect === 'string' ? route.query.redirect : '/manager';
    await router.replace(redirectTarget);
  } catch (error) {
    clearAdminAuth();
    errorMessage.value = error.message || '登录失败';
  } finally {
    isSubmitting.value = false;
  }
}
</script>

