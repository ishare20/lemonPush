#!/bin/bash

APP_NAME="lemon_push"
APP_VERSION="v101"
mkdir dist
cd ./src
# 编译为 Windows 可执行文件
GOOS=windows GOARCH=amd64 go build -o ../dist/${APP_NAME}_${APP_VERSION}_windows_amd64.exe

# 编译为 macOS 可执行文件
GOOS=darwin GOARCH=amd64 go build -o ../dist/${APP_NAME}_${APP_VERSION}_darwin_amd64

# 编译为 macOS Apple Silicon 可执行文件
GOOS=darwin GOARCH=arm64 go build -o ../dist/${APP_NAME}_${APP_VERSION}_darwin_arm64

# 编译为 Linux 可执行文件
GOOS=linux GOARCH=amd64 go build -o ../dist/${APP_NAME}_${APP_VERSION}_linux_amd64