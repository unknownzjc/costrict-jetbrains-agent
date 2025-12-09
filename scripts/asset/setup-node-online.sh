#!/bin/bash

# Node.js 在线检测和安装脚本（精简版）
# 功能：检测 Node.js 环境，如果不存在或版本不符，从 npmmirror.com 下载安装

set -e

REQUIRED_VERSION="20.6.0"
MIRROR_BASE_URL="https://registry.npmmirror.com/-/binary/node"
# Install to user's local directory
NODE_INSTALL_DIR="$HOME/.local/share/nodejs"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检测操作系统和架构
detect_platform() {
    local os=$(uname -s)
    local arch=$(uname -m)
    
    case "$os" in
        Linux*)
            if [ "$arch" = "x86_64" ]; then
                echo "linux-x64"
            else
                echo "unsupported"
            fi
            ;;
        Darwin*)
            if [ "$arch" = "arm64" ]; then
                echo "macos-arm64"
            elif [ "$arch" = "x86_64" ]; then
                echo "macos-x64"
            else
                echo "unsupported"
            fi
            ;;
        *)
            echo "unsupported"
            ;;
    esac
}

# 版本比较函数
version_compare() {
    local version1=$1
    local version2=$2
    
    # 使用 sort -V 进行版本比较
    if [ "$(printf '%s\n' "$version1" "$version2" | sort -V | head -n1)" = "$version2" ]; then
        if [ "$version1" = "$version2" ]; then
            echo "="
        else
            echo ">"
        fi
    else
        echo "<"
    fi
}

# 检测 Shell 配置文件
detect_shell_config() {
    if [ -n "$BASH_VERSION" ]; then
        if [ -f "$HOME/.bashrc" ]; then
            echo "$HOME/.bashrc"
        elif [ -f "$HOME/.bash_profile" ]; then
            echo "$HOME/.bash_profile"
        else
            echo "$HOME/.profile"
        fi
    elif [ -n "$ZSH_VERSION" ]; then
        echo "$HOME/.zshrc"
    elif [ -n "$FISH_VERSION" ]; then
        echo "$HOME/.config/fish/config.fish"
    else
        # 默认检测
        if [ -f "$HOME/.zshrc" ]; then
            echo "$HOME/.zshrc"
        elif [ -f "$HOME/.bashrc" ]; then
            echo "$HOME/.bashrc"
        elif [ -f "$HOME/.bash_profile" ]; then
            echo "$HOME/.bash_profile"
        else
            echo "$HOME/.profile"
        fi
    fi
}

# 添加到 PATH 环境变量
add_to_path() {
    local node_bin_path="$1"
    local config_file=$(detect_shell_config)
    
    # 检查是否已经添加到配置文件
    if [ -f "$config_file" ] && grep -q "$node_bin_path" "$config_file" 2>/dev/null; then
        echo -e "${GREEN}✓ PATH 已经包含 Node.js 路径（在配置文件中）${NC}"
        # 确保当前会话也生效
        export PATH="$node_bin_path:$PATH"
        return 0
    fi
    
    # 自动添加到 PATH
    echo ""
    echo -e "${YELLOW}自动将 Node.js 添加到 PATH 环境变量...${NC}"
    echo "配置文件：$config_file"
    
    # 创建配置文件目录（如果不存在）
    mkdir -p "$(dirname "$config_file")"
    
    # 添加到配置文件
    echo "" >> "$config_file"
    echo "# Node.js PATH (added by setup-node-online.sh)" >> "$config_file"
    echo "export PATH=\"$node_bin_path:\$PATH\"" >> "$config_file"
    
    echo -e "${GREEN}✓ 已自动添加到 $config_file${NC}"
    echo -e "${YELLOW}请运行以下命令使配置在新终端中生效：${NC}"
    echo -e "${GREEN}source $config_file${NC}"
    echo ""
    echo "或者重新打开终端"
    
    # 在当前 session 中生效
    export PATH="$node_bin_path:$PATH"
    echo -e "${GREEN}✓ 当前会话已生效，可以直接使用 node 命令${NC}"
}

# 下载 Node.js
download_nodejs() {
    local platform=$1
    local filename=""
    local url=""
    
    case "$platform" in
        linux-x64)
            filename="node-v${REQUIRED_VERSION}-linux-x64.tar.xz"
            ;;
        macos-x64)
            filename="node-v${REQUIRED_VERSION}-darwin-x64.tar.gz"
            ;;
        macos-arm64)
            filename="node-v${REQUIRED_VERSION}-darwin-arm64.tar.gz"
            ;;
        *)
            echo -e "${RED}错误：不支持的平台: $platform${NC}"
            exit 1
            ;;
    esac
    
    url="$MIRROR_BASE_URL/v${REQUIRED_VERSION}/$filename"
    
    echo -e "${BLUE}正在从国内镜像源下载 Node.js...${NC}"
    echo "下载地址：$url"
    echo ""
    
    # 创建临时目录
    local temp_dir=$(mktemp -d)
    local temp_file="$temp_dir/$filename"
    
    # 下载文件
    if command -v curl >/dev/null 2>&1; then
        curl -L --progress-bar -o "$temp_file" "$url"
    elif command -v wget >/dev/null 2>&1; then
        wget --show-progress -O "$temp_file" "$url"
    else
        echo -e "${RED}错误：需要 curl 或 wget 来下载文件${NC}"
        rm -rf "$temp_dir"
        exit 1
    fi
    
    # 检查下载的文件
    if [ ! -f "$temp_file" ] || [ ! -s "$temp_file" ]; then
        echo -e "${RED}错误：下载失败${NC}"
        rm -rf "$temp_dir"
        exit 1
    fi
    
    # 创建安装目录
    mkdir -p "$NODE_INSTALL_DIR"
    
    # 解压文件
    echo "正在解压 Node.js..."
    if [[ "$filename" == *.tar.xz ]]; then
        tar -xJf "$temp_file" -C "$NODE_INSTALL_DIR"
    elif [[ "$filename" == *.tar.gz ]]; then
        tar -xzf "$temp_file" -C "$NODE_INSTALL_DIR"
    fi
    
    # 移动文件到统一目录
    local extract_path="$NODE_INSTALL_DIR/node-v${REQUIRED_VERSION}-${platform#*-}"
    
    if [ -d "$extract_path" ]; then
        mv "$extract_path"/* "$NODE_INSTALL_DIR/" 2>/dev/null || true
        rmdir "$extract_path" 2>/dev/null || true
    fi
    
    # 清理临时文件
    rm -rf "$temp_dir"
    
    echo -e "${GREEN}✓ Node.js ${REQUIRED_VERSION} 下载并安装成功！${NC}"
    echo -e "${GREEN}安装路径：$NODE_INSTALL_DIR${NC}"
}

# 在线安装 Node.js
install_online_node() {
    local platform=$1
    
    echo -e "${YELLOW}正在在线安装 Node.js ${REQUIRED_VERSION}...${NC}"
    
    # 下载并安装
    download_nodejs "$platform"
    
    # 自动添加到 PATH
    add_to_path "$NODE_INSTALL_DIR/bin"
}

# 主程序
main() {
    echo "========================================="
    echo "   Node.js 在线安装工具（精简版）"
    echo "========================================="
    echo ""
    
    # 检测平台
    platform=$(detect_platform)
    if [ "$platform" = "unsupported" ]; then
        echo -e "${RED}错误：不支持的操作系统或架构${NC}"
        exit 1
    fi
    
    echo "检测到平台：$platform"
    echo ""
    
    # 检查是否已安装 Node.js
    if command -v node &> /dev/null; then
        current_version=$(node -v | sed 's/v//')
        echo -e "${GREEN}✓ 检测到已安装的 Node.js 版本：$current_version${NC}"
        
        # 比较版本
        comparison=$(version_compare "$current_version" "$REQUIRED_VERSION")
        
        if [ "$comparison" = "<" ]; then
            echo -e "${YELLOW}⚠ 警告：当前 Node.js 版本 ($current_version) 低于推荐版本 ($REQUIRED_VERSION)${NC}"
            echo -e "${YELLOW}将自动升级到 Node.js ${REQUIRED_VERSION}${NC}"
            echo ""
            install_online_node "$platform"
        elif [ "$comparison" = "=" ]; then
            echo -e "${GREEN}✓ Node.js 版本符合要求 (${REQUIRED_VERSION})${NC}"
            echo -e "${GREEN}✓ 可以继续下一步操作！${NC}"
        else
            echo -e "${GREEN}✓ Node.js 版本 ($current_version) 高于推荐版本 (${REQUIRED_VERSION})${NC}"
            echo -e "${GREEN}✓ 可以继续下一步操作！${NC}"
        fi
    else
        echo -e "${YELLOW}未检测到 Node.js 环境${NC}"
        
        # 检查是否已经安装了在线版本
        if [ -f "$NODE_INSTALL_DIR/bin/node" ]; then
            echo -e "${GREEN}✓ 检测到已安装的在线下载 Node.js${NC}"
            installed_version=$("$NODE_INSTALL_DIR/bin/node" -v | sed 's/v//')
            echo -e "${GREEN}版本：$installed_version${NC}"
            
            # 版本检查
            comparison=$(version_compare "$installed_version" "$REQUIRED_VERSION")
            if [ "$comparison" = "<" ]; then
                echo -e "${YELLOW}当前版本低于要求，将重新下载安装${NC}"
                install_online_node "$platform"
            else
                # 自动添加到 PATH
                add_to_path "$NODE_INSTALL_DIR/bin"
            fi
        else
            # 在线安装 Node.js
            install_online_node "$platform"
        fi
    fi
    
    echo ""
    echo "========================================="
}

# 运行主程序
main