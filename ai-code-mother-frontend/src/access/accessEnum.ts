/**
 * 权限枚举
 * 值与后端用户表的 userRole 字段保持一致（user / admin），notLogin 表示未登录
 */
enum AccessEnum {
  NOT_LOGIN = 'notLogin',
  USER = 'user',
  ADMIN = 'admin',
}

export default AccessEnum
