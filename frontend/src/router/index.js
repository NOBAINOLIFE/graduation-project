import { createRouter, createWebHistory } from 'vue-router';
import HomePage from '../components/HomePage.vue';
import UserProfilePage from '../components/UserProfilePage.vue';
import CreatorCenter from '../components/CreatorCenter.vue';
import VideoSubmitPage from '../components/VideoSubmitPage.vue';
import VideoPlayerPage from '../components/VideoPlayerPage.vue';
import ManagerPage from '../components/ManagerPage.vue';
import ManagerLoginPage from '../components/manager/ManagerLoginPage.vue';
import { clearAdminAuth, getAdminToken, isAdmin, isUserLoggedIn } from '../utils/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage
    },
    {
      path: '/user/:userId',
      name: 'user-profile',
      component: UserProfilePage
    },
    {
      path: '/video/:id',
      name: 'video-player',
      component: VideoPlayerPage
    },
    {
      path: '/creator',
      component: CreatorCenter,
      meta: { requiresUserAuth: true },
      children: [
        {
          path: '',
          redirect: '/creator/upload'
        },
        {
          path: 'upload',
          name: 'creator-upload',
          component: VideoSubmitPage
        },
        {
          path: 'home',
          name: 'creator-home',
          component: { template: '<div class="p-6"><h2 class="text-2xl font-bold mb-4">创作中心首页</h2><p class="text-gray-600">这里显示创作中心概览数据</p></div>' }
        },
        {
          path: 'content',
          name: 'creator-content',
          component: { template: '<div class="p-6"><h2 class="text-2xl font-bold mb-4">稿件管理</h2><p class="text-gray-600">这里管理您的所有稿件</p></div>' }
        },
        {
          path: 'fans',
          name: 'creator-fans',
          component: { template: '<div class="p-6"><h2 class="text-2xl font-bold mb-4">粉丝管理</h2><p class="text-gray-600">这里管理您的粉丝</p></div>' }
        },
        {
          path: 'comments',
          name: 'creator-comments',
          component: { template: '<div class="p-6"><h2 class="text-2xl font-bold mb-4">评论管理</h2><p class="text-gray-600">这里管理您的评论</p></div>' }
        }
      ]
    },
    {
      path: '/manager/login',
      name: 'manager-login',
      component: ManagerLoginPage,
      meta: { guestOnly: true }
    },
    {
      path: '/manager',
      name: 'manager-home',
      component: ManagerPage,
      meta: { requiresAuth: true }
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/'
    }
  ]
});

router.beforeEach((to) => {
  // 检查是否需要普通用户登录
  if (to.meta.requiresUserAuth) {
    if (!isUserLoggedIn()) {
      // 未登录，显示登录弹窗或跳转到首页
      if (window.location.pathname === '/') {
        window.dispatchEvent(new CustomEvent('show-login-modal'));
        return false;
      } else {
        return '/';
      }
    }
  }

  // 检查是否需要管理员登录
  const token = getAdminToken();
  if (to.meta.requiresAuth) {
    if (!token) {
      return {
        name: 'manager-login',
        query: { redirect: to.fullPath }
      };
    }
    if (!isAdmin()) {
      clearAdminAuth();
      return {
        name: 'manager-login',
        query: { reason: 'no-admin' }
      };
    }
  }

  if (to.meta.guestOnly && token && isAdmin()) {
    const target = typeof to.query.redirect === 'string' ? to.query.redirect : '/manager';
    return target;
  }

  return true;
});

export default router;

