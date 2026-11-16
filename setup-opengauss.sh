#!/usr/bin/env bash
# openGauss 一键配置脚本(Git Bash / Linux 通用)
# 功能:检查 Docker -> 启动容器 -> 等待就绪 -> 确保 conference_db 存在 -> 输出连接信息
set -e

cd "$(dirname "$0")"

echo "== 1/4 检查 Docker =="
if ! docker info > /dev/null 2>&1; then
  echo "❌ Docker 未运行。请先启动 Docker Desktop(系统托盘出现鲸鱼图标后重新执行本脚本)"
  exit 1
fi
docker -v

echo "== 2/4 启动 openGauss 容器(首次会拉取镜像,约 1-2GB,请耐心等待) =="
if docker inspect opengauss > /dev/null 2>&1; then
  # Reuse the existing database volume when this project was started elsewhere.
  if [ "$(docker inspect -f '{{.State.Running}}' opengauss)" != "true" ]; then
    docker start opengauss
  fi
else
  docker compose up -d
fi

echo "== 3/4 等待数据库就绪(首次初始化约 30-60 秒) =="
# 该镜像 gsql 不在默认 PATH,需设置 GAUSSHOME/LD_LIBRARY_PATH
GSQL() {
  docker exec opengauss bash -c "export GAUSSHOME=/usr/local/opengauss LD_LIBRARY_PATH=/usr/local/opengauss/lib PATH=/usr/local/opengauss/bin:\$PATH; gsql -U gaussdb -W Gauss@123 $*"
}
for i in $(seq 1 60); do
  if GSQL -d postgres -c "SELECT 1" > /dev/null 2>&1; then
    echo "数据库已就绪"
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "❌ 等待超时,查看日志:docker logs opengauss"
    exit 1
  fi
  sleep 3
done

echo "== 4/4 确保 conference_db 存在 =="
# GS_DB 环境变量通常已自动建库;此处兜底判断
if ! GSQL -d postgres -t -c "SELECT 1 FROM pg_database WHERE datname='conference_db'" 2>/dev/null | grep -q 1; then
  GSQL -d postgres -c "CREATE DATABASE conference_db ENCODING 'UTF8';"
fi
GSQL -d conference_db -c "SELECT version();"

cat << 'EOF'

✅ openGauss 配置完成!
   主机:localhost  端口:5432  库:conference_db  用户:gaussdb  密码:Gauss@123
   下一步:启动后端(IDEA 运行 ConferenceApplication,建表和演示数据会自动完成)

EOF
