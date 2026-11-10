# Vue 模板构建部署

1. 在应用后端切换到新代码前，备份数据库，执行 `sql/migrate_vue_template_v1.sql` 一次。旧应用的 `templateVersion` 保持 `NULL`，现有工程不会自动覆盖；需单独备份、迁移业务文件和验收后再启用新模板。
2. 从仓库根目录构建隔离构建镜像：

   ```powershell
   docker build -f deployment/vue-builder/Dockerfile -t ai-code-mother/vue-builder:v1 .
   ```

3. 后端基础默认使用 `app.vue-build.runner=docker`，需要 Docker Engine 正常运行。构建容器不联网、非 root、只读根文件系统，限制 CPU、内存和进程数，输入工程只读挂载，输出单独挂载。
4. 本仓库默认激活 `druid` 本机开发配置，该配置使用 `app.vue-build.runner=local`，直接调用本机 Node 构建，不依赖 Docker Engine。该模式仅限可信开发环境，**不能对不可信用户开放**。部署环境应显式设置 `APP_VUE_BUILD_RUNNER=docker` 并启动 Docker Engine。
5. 预览页面应通过独立域名反向代理到 `/api/static`，前端设置 `VITE_PREVIEW_BASE_URL=https://preview.example.com/api/static`，后端设置 `APP_PREVIEW_ALLOWED_HOSTS=preview.example.com`。反向代理需保留正确的 `Host`，并在网络层限制后台域名访问此静态路由；单靠请求头检查不能形成隔离。部署作品也要使用独立域名。为截图浏览器限制目标地址和内网访问。容器构建不替代浏览器侧隔离。

发布新模板时使用新的版本号，不修改已发布版本的内容；同步更新构建镜像和后端资源。新应用构建成功后 `app.buildStatus=SUCCESS`，前端才显示本次预览。
