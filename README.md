## OrzRepacker

### 打包流程
1. 提交所有记录
2. 执行 UpdateVersionNumber 更新版本号
3. 执行 maven 面板的 package 进行打包
4. 修改 Luncher 的版本号（最新版本号在 application.properties 里）
5. 重新编译 Luncher
6. 将 Luncher 放入解压后的目录中，执行 Luncher 将 Jar 加密成 data.bin