import AccessEnum from '@/access/accessEnum'

/**
 * 检查用户是否具有某个权限
 * @param loginUser 当前登录用户
 * @param needAccess 页面所需的访问权限，默认无需登录
 * @return 是否有权限
 */
export default function checkAccess(
  loginUser: API.LoginUserVO,
  needAccess: AccessEnum = AccessEnum.NOT_LOGIN,
): boolean {
  // 当前用户的权限：未登录时 userRole 为空，归为 NOT_LOGIN
  const loginUserAccess = loginUser?.userRole ?? AccessEnum.NOT_LOGIN

  // 页面无需登录，所有人可访问
  if (needAccess === AccessEnum.NOT_LOGIN) {
    return true
  }
  // 页面仅需登录：已登录即可（普通用户、管理员都行）
  if (needAccess === AccessEnum.USER) {
    return loginUserAccess !== AccessEnum.NOT_LOGIN
  }
  // 页面需要管理员权限
  return loginUserAccess === AccessEnum.ADMIN
}
