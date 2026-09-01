/**
 * 全局导航菜单配置
 * 新增页面时：先在 router/index.ts 注册路由，再在此处添加对应菜单项即可
 */
export interface MenuConfigItem {
  /** 菜单项唯一标识，同时作为路由路径使用 */
  key: string
  /** 菜单展示标题 */
  label: string
}

export const menuItems: MenuConfigItem[] = [
  { key: '/', label: '首页' },
  { key: '/about', label: '关于' },
]
