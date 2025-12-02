#!/bin/bash

# ---
#文献助手 macOS 和 Linux 启动脚本
# ---

# 定义一个函数，在退出时清理后台进程
cleanup() {
    echo ""
    echo "正在关闭服务器..."
    # 杀死此脚本进程组中的所有进程
    # 这确保了子进程（后端和前端）也会被终止
    kill -TERM 0
    wait
    echo "关闭完成。"
}

# 捕获 EXIT 信号（例如，当脚本关闭或按下 Ctrl+C 时）
# 并调用 cleanup 函数
trap cleanup SIGINT SIGTERM EXIT

# 设置输出的颜色代码
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # 无颜色

echo -e "${GREEN}==========================================================${NC}"
echo -e "${GREEN}                   启动文献助手                   	   ${NC}"
echo -e "${GREEN}==========================================================${NC}"
echo ""

# --- 项目设置 ---
BACKEND_DIR="./"
FRONTEND_DIR="literature-explorer-frontend"
JAR_FILE="literature-explorer-0.0.1-SNAPSHOT.jar"
JAR_PATH="$BACKEND_DIR/jar/$JAR_FILE"

# 检查后端 JAR 文件是否存在
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${RED}[错误] 后端 JAR 文件未找到: $JAR_PATH${NC}"
    echo -e "${RED}请确保后端 JAR 文件存在。${NC}"
    exit 1
fi

# 检查前端目录是否存在
if [ ! -d "$FRONTEND_DIR" ]; then
    echo -e "${RED}[错误] 前端目录 '$FRONTEND_DIR' 未找到。${NC}"
    exit 1
fi


# --- 启动后端服务器 ---
echo "[1/3] 正在从 JAR 文件后台启动 Spring Boot 后端服务器..."
(
    java -jar "$JAR_PATH"
) & # '&' 符号表示在后台运行此子 shell

echo -e "${GREEN}[成功] 后端服务器正在启动。${NC}"
echo ""
sleep 5 # 给后端一点启动时间

# --- 启动前端服务器 ---
echo "[2/3] 正在准备并后台启动 Vue 前端服务器..."
(
    cd "$FRONTEND_DIR" || exit
    # 检查 node_modules 是否存在，如果不存在则运行 'npm install'
    if [ ! -d "node_modules" ]; then
        echo "'node_modules' 目录未找到。正在运行 'npm install'..."
        npm install
    else
        echo "前端依赖已安装。"
    fi
    npm run dev
) & # '&' 符号表示在后台运行此子 shell

echo -e "${GREEN}[成功] 前端服务器正在启动。${NC}"
echo ""


# --- 完成 ---
echo "[3/3] 所有服务已启动。"
echo -e "${GREEN}==========================================================${NC}"
echo "后端和前端正在后台运行。"
echo "您可以在此终端中查看它们的日志。"
echo ""
echo -e "按 ${RED}Ctrl+C${NC} 来停止两个服务器。"
echo ""

# 等待所有后台任务完成
wait

