import { createRouter, createWebHistory } from 'vue-router';
import ManagerPage from '../components/ManagerPage.vue';
import ManagerLoginPage from '../components/manager/ManagerLoginPage.vue';
import { clearAuth, getToken, isAdmin } from '../utils/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/manager'
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
      redirect: '/manager'
    }
  ]
});

router.beforeEach((to) => {
  const token = getToken();

  if (to.meta.requiresAuth) {
    if (!token) {
      return {
        name: 'manager-login',
        query: { redirect: to.fullPath }
      };
    }
    if (!isAdmin()) {
      clearAuth();
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

