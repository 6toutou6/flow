import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

/* Layout */
import Layout from '@/layout'

/**
 * Note: sub-menu only appear when route children.length >= 1
 * Detail see: https://panjiachen.github.io/vue-element-admin-site/guide/essentials/router-and-nav.html
 *
 * hidden: true                   if set true, item will not show in the sidebar(default is false)
 * alwaysShow: true               if set true, will always show the root menu
 *                                if not set alwaysShow, when item has more than one children route,
 *                                it will becomes nested mode, otherwise not show the root menu
 * redirect: noRedirect           if set noRedirect will no redirect in the breadcrumb
 * name:'router-name'             the name is used by <keep-alive> (must set!!!)
 * meta : {
    roles: ['admin','editor']    control the page roles (you can set multiple roles)
    title: 'title'               the name show in sidebar and breadcrumb (recommend set)
    icon: 'svg-name'/'el-icon-x' the icon show in the sidebar
    breadcrumb: false            if set false, the item will hidden in breadcrumb(default is true)
    activeMenu: '/example/list'  if set path, the sidebar will highlight the path you set
  }
 */

/**
 * constantRoutes
 * a base page that does not have permission requirements
 * all roles can be accessed
 * 菜单顺序：普通用户（任务处理）→ 管理员 → 超管；不做路由权限控制，数据返回由后端按登录用户区分
 */
export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/login/index'),
    hidden: true
  },

  {
    path: '/404',
    component: () => import('@/views/404'),
    hidden: true
  },

  {
    path: '/',
    redirect: '/dashboard/index'
  },

  // ==================== 操作指引（Dashboard） ====================
  {
    path: '/dashboard',
    component: Layout,
    children: [
      {
        path: 'index',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index'),
        meta: { title: '操作指引', icon: 'el-icon-reading' }
      }
    ]
  },

  // ==================== 普通用户：任务处理（我的任务） ====================
  {
    path: '/task-process',
    component: Layout,
    children: [
      {
        path: 'index',
        name: 'TaskProcess',
        component: () => import('@/views/taskprocess/index'),
        meta: { title: '任务处理', icon: 'el-icon-s-claim' }
      },
      {
        path: 'detail',
        name: 'TaskProcessDetail',
        component: () => import('@/views/taskprocess/ProcessDetail'),
        hidden: true
      }
    ]
  },

  // ==================== 管理员 ====================
  {
    path: '/flow-dispatch',
    component: Layout,
    name: '管理员',
    meta: { title: '管理员', icon: 'el-icon-s-help' },
    children: [
      {
        path: 'index',
        name: 'FlowDispatch',
        component: () => import('@/views/flowdispatch/index'),
        meta: { title: '任务管理', icon: 'el-icon-s-order' }
      },
      {
        path: 'flow-template',
        name: 'FlowTemplate',
        component: () => import('@/views/flowtemplate/index'),
        meta: { title: '模板管理', icon: 'el-icon-document' }
      },
      {
        path: 'config-template',
        name: 'FlowDispatchConfigTemplate',
        component: () => import('@/views/flowdispatch/ConfigTemplate'),
        meta: { title: '下发配置模板', icon: 'el-icon-setting' }
      },
      {
        path: 'data-view',
        name: 'DataView',
        component: () => import('@/views/dataview/index'),
        meta: { title: '数据展示', icon: 'el-icon-data-analysis' }
      },
      {
        path: 'config-template/edit',
        name: 'FlowDispatchConfigTemplateEdit',
        component: () => import('@/views/flowdispatch/ConfigTemplateEdit'),
        meta: { title: '下发配置模板编辑', activeMenu: '/flow-dispatch/config-template' },
        hidden: true
      },
      {
        path: 'edit',
        name: 'FlowDispatchEdit',
        component: () => import('@/views/flowdispatch/EditTask'),
        meta: { title: '任务编辑', activeMenu: '/flow-dispatch/index' },
        hidden: true
      },
      {
        path: 'period-users',
        name: 'FlowDispatchPeriodUsers',
        component: () => import('@/views/flowdispatch/PeriodUsers'),
        meta: { title: '期次人员', activeMenu: '/flow-dispatch/index' },
        hidden: true
      },
      {
        path: 'period-flow',
        name: 'FlowDispatchPeriodFlow',
        component: () => import('@/views/flowdispatch/PeriodFlow'),
        meta: { title: '人员流程', activeMenu: '/flow-dispatch/index' },
        hidden: true
      },
      {
        path: 'person-view',
        name: 'FlowDispatchPersonView',
        component: () => import('@/views/flowdispatch/PersonView'),
        meta: { title: '按人员查看', activeMenu: '/flow-dispatch/index' },
        hidden: true
      },
      {
        path: 'task-link',
        name: 'FlowDispatchTaskLink',
        component: () => import('@/views/flowdispatch/TaskLink'),
        meta: { title: '期次任务关联', activeMenu: '/flow-dispatch/index' },
        hidden: true
      }
    ]
  },

  // ==================== 超管 ====================
  {
    path: '/dept-admin',
    component: Layout,
    name: '超管',
    meta: { title: '超管', icon: 'el-icon-s-custom' },
    children: [
      {
        path: 'index',
        name: 'DeptAdmin',
        component: () => import('@/views/deptadmin/index'),
        meta: { title: '系统管理员', icon: 'el-icon-s-custom' }
      }
    ]
  },

  // ==================== 隐藏路由：表单设计器 ====================
  {
    path: '/form-designer',
    component: Layout,
    hidden: true,
    children: [
      {
        path: 'index',
        name: 'FormDesigner',
        component: () => import('@/views/formdesigner/index'),
        meta: { title: '表单设计器', icon: 'el-icon-set-up', activeMenu: '/flow-dispatch/flow-template' }
      }
    ]
  },

  // 404 page must be placed at the end !!!
  { path: '*', redirect: '/404', hidden: true }
]

const createRouter = () => new Router({
  // mode: 'history', // require service support
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})

const router = createRouter()

// Detail see: https://github.com/vuejs/vue-router/issues/1234#issuecomment-357941465
export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher // reset router
}

export default router
