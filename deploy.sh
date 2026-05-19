#!/bin/bash
set -e
cd /www/dk_project/daisimao

echo '[1/6] 拉取最新代码...'
git fetch --all
git reset --hard origin/main

echo '[2/6] 构建后端...'
cd /www/dk_project/daisimao/server
mvn clean package -DskipTests -q

echo '[3/6] 构建前端 web-app...'
cd /www/dk_project/daisimao/web-app
npm install --silent
npm run build --silent

echo '[4/6] 构建前端 web-admin...'
cd /www/dk_project/daisimao/web-admin
npm install --silent
npm run build --silent

echo '[5/6] 复制文件到 docker 目录并重建镜像...'
PROJECT_DIR=/www/dk_project/daisimao
DOCKER_DIR=$PROJECT_DIR/docker

# 复制JAR到docker目录
cp $PROJECT_DIR/server/target/daisimao-server-0.1.0.jar $DOCKER_DIR/daisimao-server-0.1.0.jar

# 从项目根目录构建spring镜像（build context是项目根目录，Dockerfile在docker/子目录）
docker build -t daisimao:latest -f $DOCKER_DIR/Dockerfile $PROJECT_DIR

# 构建nginx镜像
docker build -t daisimao-nginx:latest -f $DOCKER_DIR/Dockerfile.nginx $PROJECT_DIR

echo '[6/6] 重启容器...'
cd $DOCKER_DIR
docker-compose down 2>/dev/null || true
docker-compose up -d

echo '✅ 部署完成！'
docker ps --format 'table {{.Names}}\t{{.Status}}'
