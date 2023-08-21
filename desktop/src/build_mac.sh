#!/bin/bash

APP_NAME="lemon_push"
APP_VERSION="v102"
CONF_FILE="lemon_push.conf"
# mkdir dist
# cd ./src
# 编译为 Windows 可执行文件
GOOS=windows GOARCH=amd64 go build  -o ${APP_NAME}_${APP_VERSION}_windows_amd64.exe 
# zip ${APP_NAME}_${APP_VERSION}_windows_amd64 ${APP_NAME}_${APP_VERSION}_windows_amd64.exe 

# 编译为 macOS 可执行文件
GOOS=darwin GOARCH=amd64 go build -o ${APP_NAME}_${APP_VERSION}_darwin_amd64 
fileName=${APP_NAME}_${APP_VERSION}_darwin_amd64
# echo $fileName
# zip ${fileName} ${fileName} ${CONF_FILE} 

# 编译为 macOS Apple Silicon 可执行文件
GOOS=darwin GOARCH=arm64 go build -o ${APP_NAME}_${APP_VERSION}_darwin_arm64 
# fileName=${APP_NAME}_${APP_VERSION}_darwin_arm64
# zip ${fileName} ${fileName} ${CONF_FILE}

# 编译为 Linux 可执行文件
GOOS=linux GOARCH=amd64 go build -o ${APP_NAME}_${APP_VERSION}_linux_amd64 
# fileName=${APP_NAME}_${APP_VERSION}_linux_amd64
# zip ${fileName} ${fileName} ${CONF_FILE}