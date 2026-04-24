<template>
  <div v-if="visible" class="fixed inset-0 z-[100] flex items-center justify-center" @click.self="handleClose">
    <!-- 遮罩层 -->
    <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"></div>
    
    <!-- 登录框 -->
    <div class="relative w-full max-w-md bg-white rounded-lg shadow-2xl overflow-hidden animate-scale-in">
      <!-- 关闭按钮 -->
      <button
        class="absolute top-3 right-3 w-8 h-8 flex items-center justify-center text-gray-400 hover:text-gray-600 transition-colors z-10"
        @click="handleClose"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>

      <!-- 头部 -->
      <div class="px-8 pt-8 pb-4">
        <h2 class="text-2xl font-bold text-gray-800 text-center">登录</h2>
        <p class="text-sm text-gray-500 text-center mt-2">登录后享受更多功能</p>
      </div>

      <!-- 表单 -->
      <div class="px-8 pb-8">
        <form @submit.prevent="handleLogin">
          <!-- 用户名 -->
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">账号</label>
            <input
              v-model="loginForm.account"
              type="text"
              placeholder="请输入账号"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-md focus:outline-none focus:border-[#00a1d6] focus:ring-1 focus:ring-[#00a1d6] transition-colors"
              :class="{ 'border-red-500': errors.account }"
              @input="clearError('username')"
            />
            <p v-if="errors.account" class="mt-1 text-xs text-red-500">{{ errors.account }}</p>
          </div>

          <!-- 密码 -->
          <div class="mb-6">
            <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
            <div class="relative">
              <input
                v-model="loginForm.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-md focus:outline-none focus:border-[#00a1d6] focus:ring-1 focus:ring-[#00a1d6] transition-colors pr-10"
                :class="{ 'border-red-500': errors.password }"
                @input="clearError('password')"
              />
              <button
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                @click="showPassword = !showPassword"
              >
                <svg v-if="!showPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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

          <!-- 错误提示 -->
          <div v-if="errorMessage" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md">
            <p class="text-sm text-red-600">{{ errorMessage }}</p>
          </div>

          <!-- 登录按钮 -->
          <button
            type="submit"
            class="w-full py-2.5 bg-[#00a1d6] text-white rounded-md font-medium hover:bg-[#0095c8] active:bg-[#0088b8] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="loading"
          >
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </form>

        <!-- 注册链接 -->
        <div class="mt-4 text-center">
          <span class="text-sm text-gray-600">还没有账号？</span>
          <button
            class="text-sm text-[#00a1d6] hover:text-[#0095c8] ml-1 font-medium"
            @click="handleGoToRegister"
          >
            立即注册
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { login } from '../api/user';
import { saveUserLogin } from '../utils/auth';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:visible', 'success']);

const loginForm = ref({
  account: '',
  password: ''
});

const showPassword = ref(false);
const loading = ref(false);
const errorMessage = ref('');
const errors = ref({});

// 清除错误
function clearError(field) {
  delete errors.value[field];
  errorMessage.value = '';
}

// 验证表单
function validateForm() {
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
  
  return true;
}

// 登录
async function handleLogin() {
  if (!validateForm()) return;
  
  loading.value = true;
  errorMessage.value = '';
  
  try {
    // 将account转换为数字
    const account = Number(loginForm.value.account);
    if (!Number.isInteger(account) || account <= 0) {
      errorMessage.value = '账号必须为正整数';
      loading.value = false;
      return;
    }
    
    const result = await login({
      account: account,
      password: loginForm.value.password
    });
    
    // 保存普通用户登录信息
    saveUserLogin(result);
    
    // 触发成功事件
    emit('success', result);
    
    // 关闭弹窗
    handleClose();
    
    // 重置表单
    resetForm();
  } catch (error) {
    errorMessage.value = error.message || '登录失败，请重试';
  } finally {
    loading.value = false;
  }
}

// 关闭弹窗
function handleClose() {
  emit('update:visible', false);
}

// 重置表单
function resetForm() {
  loginForm.value = {
    account: '',
    password: ''
  };
  showPassword.value = false;
  errorMessage.value = '';
  errors.value = {};
}

// 去注册
function handleGoToRegister() {
  // TODO: 实现注册页面路由
  console.log('跳转到注册页面');
  handleClose();
}

// 监听visible变化，重置表单
watch(() => props.visible, (newVal) => {
  if (newVal) {
    resetForm();
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
