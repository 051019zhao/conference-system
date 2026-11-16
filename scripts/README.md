# Windows 启动脚本

在任意目录双击或执行 `run-backend.cmd`、`run-frontend.cmd`，脚本会自动切换到项目目录。前端首次运行会执行 `npm ci`。

先启动 Docker Desktop，运行项目根目录的 `setup-opengauss.sh` 或 `docker compose up -d`。后端需要 JDK 17 和 Maven 在 PATH 中；前端需要 Node.js 18+ 和 npm。可选的 AI Key 等本地配置写入 `scripts/local.env.cmd`，该文件不会提交到 Git。

脚本在当前终端输出日志。按 Ctrl+C 停止服务。
