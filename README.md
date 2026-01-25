## OrzRepacker

### 打包流程
1. 更新 application.properties 里新版本的 key
2. 提交所有记录
3. 执行 UpdateVersionNumber 更新版本号
4. 将新版本的 key 提交到更新服务器
5. 执行 maven 面板的 package 进行打包
6. 修改 Luncher 的版本号（最新版本号在 application.properties 里）
7. 重新编译 Luncher
8. 将 Luncher 放入解压后的目录中，执行 Luncher 将 Jar 加密成 data.bin