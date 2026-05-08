<template>
  <div v-if="visible" class="fixed inset-0 z-[100] flex items-center justify-center">
    <div
      class="absolute inset-0 bg-black/50 backdrop-blur-sm"
      @mousedown="handleBackdropMouseDown"
      @mouseup="handleBackdropMouseUp"
    ></div>

    <div class="relative w-full max-w-md bg-white rounded-lg shadow-2xl overflow-hidden animate-scale-in">
      <button
        class="absolute top-3 right-3 w-8 h-8 flex items-center justify-center text-gray-400 hover:text-gray-600 transition-colors z-10"
        @click="handleClose"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>

      <div class="px-8 pt-8 pb-4">
        <h2 class="text-2xl font-bold text-gray-800 text-center">{{ isRegisterMode ? '注册' : '登录' }}</h2>
        <p class="text-sm text-gray-500 text-center mt-2">
          {{ isRegisterMode ? '创建账号后即可开始使用平台功能' : '登录后享受更多功能' }}
        </p>
      </div>

      <div class="px-8 pb-8">
        <form @submit.prevent="isRegisterMode ? handleRegister() : handleLogin()">
          <div v-if="isRegisterMode" class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">用户名</label>
            <input
              v-model="registerForm.username"
              type="text"
              placeholder="请输入用户名"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-md focus:outline-none focus:border-[#00a1d6] focus:ring-1 focus:ring-[#00a1d6] transition-colors"
              :class="{ 'border-red-500': errors.username }"
              @input="clearError('username')"
            />
            <p v-if="errors.username" class="mt-1 text-xs text-red-500">{{ errors.username }}</p>
          </div>

          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">账号</label>
            <input
              v-model="currentAccount"
              type="text"
              placeholder="请输入账号"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-md focus:outline-none focus:border-[#00a1d6] focus:ring-1 focus:ring-[#00a1d6] transition-colors"
              :class="{ 'border-red-500': errors.account }"
              @input="clearError('account')"
            />
            <p v-if="errors.account" class="mt-1 text-xs text-red-500">{{ errors.account }}</p>
          </div>

          <div :class="isRegisterMode ? 'mb-4' : 'mb-6'">
            <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
            <div class="relative">
              <input
                v-model="currentPassword"
                :type="currentPasswordVisible ? 'text' : 'password'"
                placeholder="请输入密码"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-md focus:outline-none focus:border-[#00a1d6] focus:ring-1 focus:ring-[#00a1d6] transition-colors pr-10"
                :class="{ 'border-red-500': errors.password }"
                @input="clearError('password')"
              />
              <button
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                @click="toggleCurrentPassword"
              >
                <svg v-if="!currentPasswordVisible" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
                <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                </svg>
              </button>
            </div>
            <p v-if="errors.password" class="mt-1 text-xs text-red-500">{{ errors.password }}</p>
          </div>

          <div v-if="isRegisterMode" class="mb-6">
            <label class="block text-sm font-medium text-gray-700 mb-2">确认密码</label>
            <div class="relative">
              <input
                v-model="registerForm.confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                placeholder="请再次输入密码"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-md focus:outline-none focus:border-[#00a1d6] focus:ring-1 focus:ring-[#00a1d6] transition-colors pr-10"
                :class="{ 'border-red-500': errors.confirmPassword }"
                @input="clearError('confirmPassword')"
              />
              <button
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                @click="showConfirmPassword = !showConfirmPassword"
              >
                <svg v-if="!showConfirmPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
                <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                </svg>
              </button>
            </div>
            <p v-if="errors.confirmPassword" class="mt-1 text-xs text-red-500">{{ errors.confirmPassword }}</p>
          </div>

          <div v-if="successMessage" class="mb-4 p-3 bg-green-50 border border-green-200 rounded-md">
            <p class="text-sm text-green-600">{{ successMessage }}</p>
          </div>

          <div v-if="errorMessage" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md">
            <p class="text-sm text-red-600">{{ errorMessage }}</p>
          </div>

          <button
            type="submit"
            class="w-full py-2.5 bg-[#00a1d6] text-white rounded-md font-medium hover:bg-[#0095c8] active:bg-[#0088b8] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="loading"
          >
            {{ submitButtonText }}
          </button>
        </form>

        <div class="mt-4 text-center">
          <span class="text-sm text-gray-600">{{ isRegisterMode ? '已有账号？' : '还没有账号？' }}</span>
          <button
            class="text-sm text-[#00a1d6] hover:text-[#0095c8] ml-1 font-medium"
            @click="isRegisterMode ? switchToLogin() : switchToRegister()"
          >
            {{ isRegisterMode ? '返回登录' : '立即注册' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { login, register } from '../api/user';
import { saveUserLogin } from '../utils/auth';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:visible', 'success']);

const viewMode = ref('login');
const loginForm = ref({
  account: '',
  password: ''
});
const registerForm = ref({
  username: '',
  account: '',
  password: '',
  confirmPassword: ''
});

const showLoginPassword = ref(false);
const showRegisterPassword = ref(false);
const showConfirmPassword = ref(false);
const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const errors = ref({});
const shouldCloseOnBackdropMouseUp = ref(false);

const isRegisterMode = computed(() => viewMode.value === 'register');
const currentAccount = computed({
  get: () => (isRegisterMode.value ? registerForm.value.account : loginForm.value.account),
  set: (value) => {
    if (isRegisterMode.value) {
      registerForm.value.account = value;
    } else {
      loginForm.value.account = value;
    }
  }
});
const currentPassword = computed({
  get: () => (isRegisterMode.value ? registerForm.value.password : loginForm.value.password),
  set: (value) => {
    if (isRegisterMode.value) {
      registerForm.value.password = value;
    } else {
      loginForm.value.password = value;
    }
  }
});
const currentPasswordVisible = computed(() => (
  isRegisterMode.value ? showRegisterPassword.value : showLoginPassword.value
));
const submitButtonText = computed(() => {
  if (!loading.value) {
    return isRegisterMode.value ? '注册' : '登录';
  }
  return isRegisterMode.value ? '注册中...' : '登录中...';
});

function clearError(field) {
  delete errors.value[field];
  errorMessage.value = '';
}

function clearMessages() {
  errorMessage.value = '';
  successMessage.value = '';
}

function normalizeAccount(value) {
  const accountText = String(value ?? '').trim();
  if (!/^\d+$/.test(accountText)) return null;
  const account = Number(accountText);
  if (!Number.isSafeInteger(account) || account <= 0) return null;
  return account;
}

function validateLoginForm() {
  errors.value = {};

  if (!loginForm.value.account) {
    errors.value.account = '请输入账号';
    return false;
  }

  if (!loginForm.value.password) {
    errors.value.password = '请输入密码';
    return false;
  }

  if (loginForm.value.password.length < 6) {
    errors.value.password = '密码长度不能少于6位';
    return false;
  }

  if (normalizeAccount(loginForm.value.account) === null) {
    errors.value.account = '账号必须为正整数';
    return false;
  }

  return true;
}

function validateRegisterForm() {
  errors.value = {};

  if (!registerForm.value.username?.trim()) {
    errors.value.username = '请输入用户名';
    return false;
  }

  if (!registerForm.value.account) {
    errors.value.account = '请输入账号';
    return false;
  }

  if (normalizeAccount(registerForm.value.account) === null) {
    errors.value.account = '账号必须为正整数';
    return false;
  }

  if (!registerForm.value.password) {
    errors.value.password = '请输入密码';
    return false;
  }

  if (registerForm.value.password.length < 6) {
    errors.value.password = '密码长度不能少于6位';
    return false;
  }

  if (!registerForm.value.confirmPassword) {
    errors.value.confirmPassword = '请再次输入密码';
    return false;
  }

  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    errors.value.confirmPassword = '两次输入的密码不一致';
    return false;
  }

  return true;
}

async function handleLogin() {
  if (!validateLoginForm()) return;

  loading.value = true;
  clearMessages();

  try {
    const result = await login({
      account: normalizeAccount(loginForm.value.account),
      password: loginForm.value.password,
      isAdminLogin: false
    });

    saveUserLogin(result);
    emit('success', result);
    handleClose();
  } catch (error) {
    errorMessage.value = error.message || '登录失败，请重试';
  } finally {
    loading.value = false;
  }
}

async function handleRegister() {
  if (!validateRegisterForm()) return;

  loading.value = true;
  clearMessages();

  try {
    await register({
      username: registerForm.value.username.trim(),
      account: normalizeAccount(registerForm.value.account),
      password: registerForm.value.password
    });

    const registeredAccount = String(registerForm.value.account).trim();
    switchToLogin();
    loginForm.value.account = registeredAccount;
    loginForm.value.password = '';
    successMessage.value = '注册成功，请使用新账号登录';
    resetRegisterForm();
  } catch (error) {
    errorMessage.value = error.message || '注册失败，请重试';
  } finally {
    loading.value = false;
  }
}

function toggleCurrentPassword() {
  if (isRegisterMode.value) {
    showRegisterPassword.value = !showRegisterPassword.value;
    return;
  }
  showLoginPassword.value = !showLoginPassword.value;
}

function switchToRegister() {
  viewMode.value = 'register';
  errors.value = {};
  clearMessages();
}

function switchToLogin() {
  viewMode.value = 'login';
  errors.value = {};
  errorMessage.value = '';
}

function resetLoginForm() {
  loginForm.value = {
    account: '',
    password: ''
  };
}

function resetRegisterForm() {
  registerForm.value = {
    username: '',
    account: '',
    password: '',
    confirmPassword: ''
  };
}

function resetAllState() {
  viewMode.value = 'login';
  resetLoginForm();
  resetRegisterForm();
  showLoginPassword.value = false;
  showRegisterPassword.value = false;
  showConfirmPassword.value = false;
  loading.value = false;
  errors.value = {};
  clearMessages();
}

function handleBackdropMouseDown(event) {
  shouldCloseOnBackdropMouseUp.value = event.target === event.currentTarget;
}

function handleBackdropMouseUp(event) {
  const shouldClose = shouldCloseOnBackdropMouseUp.value && event.target === event.currentTarget;
  shouldCloseOnBackdropMouseUp.value = false;

  if (shouldClose) {
    handleClose();
  }
}

function handleClose() {
  shouldCloseOnBackdropMouseUp.value = false;
  emit('update:visible', false);
  resetAllState();
}

watch(() => props.visible, (newVal) => {
  if (newVal) {
    resetAllState();
  }
});
</script>

<style scoped>
@keyframes scale-in {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.animate-scale-in {
  animation: scale-in 0.2s ease-out;
}
</style>
