import {createRouter, createWebHistory} from 'vue-router';
import HomePage from '../components/HomePage.vue';
import SearchPage from '../components/SearchPage.vue';
import UserProfilePage from '../components/UserProfilePage.vue';
import CreatorCenter from '../components/CreatorCenter.vue';
import VideoSubmitPage from '../components/VideoSubmitPage.vue';
import VideoPlayerPage from '../components/VideoPlayerPage.vue';
import UserHistoryPage from '../components/UserHistoryPage.vue';
import MessageCenterPage from '../components/MessageCenterPage.vue';
import ManagerPage from '../components/manager/ManagerPage.vue';
import ManagerLoginPage from '../components/manager/ManagerLoginPage.vue';
import CreatorContentPage from '../components/creator/CreatorContentPage.vue';
import CreatorFansPage from '../components/creator/CreatorFansPage.vue';
import CreatorCommentsPage from '../components/creator/CreatorCommentsPage.vue';
import {
  clearAdminAuth,
  getAdminToken,
  isAdmin,
  isUserLoggedIn,
  openLoginModal,
  SHOW_LOGIN_MODAL_ONCE_KEY
} from '../utils/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage
    },
    {
      path: '/search',
      name: 'search',
      component: SearchPage
    },
    {
      path: '/user/:userId/following',
      name: 'user-following',
      redirect: to => ({
        name: 'user-profile',
        params: { userId: to.params.userId },
        query: { ...to.query, tab: 'following' }
      })
    },
    {
      path: '/user/:userId/fans',
      name: 'user-fans',
      redirect: to => ({
        name: 'user-profile',
        params: { userId: to.params.userId },
        query: { ...to.query, tab: 'fans' }
      })
    },
    {
      path: '/user/:userId',
      name: 'user-profile',
      component: UserProfilePage,
      meta: { requiresUserAuth: true }
    },
    {
      path: '/video/:id',
      name: 'video-player',
      component: VideoPlayerPage
    },
    {
      path: '/history',
      name: 'user-history',
      component: UserHistoryPage,
      meta: { requiresUserAuth: true }
    },
    {
      path: '/messages/:userId?',
      name: 'messages',
      component: MessageCenterPage,
      meta: { requiresUserAuth: true }
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
          path: 'content',
          name: 'creator-content',
          component: CreatorContentPage
        },
        {
          path: 'fans',
          name: 'creator-fans',
          component: CreatorFansPage
        },
        {
          path: 'comments',
          name: 'creator-comments',
          component: CreatorCommentsPage
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

router.beforeEach((to, from) => {
  if (to.meta.requiresUserAuth) {
    if (!isUserLoggedIn()) {
      if (from.matched.length === 0) {
        sessionStorage.setItem(SHOW_LOGIN_MODAL_ONCE_KEY, '1');
        return '/';
      }
      openLoginModal({ redirect: to.fullPath });
      return false;
    }
  }

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
    return typeof to.query.redirect === 'string' ? to.query.redirect : '/manager';
  }

  return true;
});

export default router;
