# Windows 启动脚本

在任意目录双击或执行 `run-backend.cmd`、`run-frontend.cmd`，脚本会自动切换到项目目录。前端首次运行会执行 `npm ci`。

先启动 Docker Desktop。`run-backend.cmd` 会调用 `run-db.cmd`，自动启动或复用 `opengauss` 容器并等待数据库就绪；如果设置了 `DB_URL`，则跳过本地 Docker。后端脚本优先使用 `JAVA_HOME` 中的 JDK 17，也会寻找用户目录中的 JDK 17；Maven 可来自 PATH、`MAVEN_HOME` 或 IntelliJ IDEA 自带版本。运行 `run-backend.cmd --check` 可检查检测结果。前端需要 Node.js 18+ 和 npm。可选的 AI Key 等本地配置写入 `scripts/local.env.cmd`，该文件不会提交到 Git。

脚本在当前终端输出日志。按 Ctrl+C 停止服务。
