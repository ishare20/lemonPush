set APP_NAME=lemon_push
set APP_VERSION=v101
cd ./src
:: 编译为 Windows 可执行文件
set GOOS=windows
set GOARCH=amd64

go build -o ../dist/%APP_NAME%_%APP_VERSION%_%GOOS%_%GOARCH%.exe

:: 编译为 macOS 可执行文件
set GOOS=darwin
set GOARCH=amd64
go build -o ../dist/%APP_NAME%_%APP_VERSION%_%GOOS%_%GOARCH%

:: 编译为 macOS Apple Silicon可执行文件
set GOOS=darwin
set GOARCH=arm64
go build -o ../dist/%APP_NAME%_%APP_VERSION%_%GOOS%_%GOARCH%

:: 编译为 Linux 可执行文件
set GOOS=linux
set GOARCH=amd64
go build -o ../dist/%APP_NAME%_%APP_VERSION%_%GOOS%_%GOARCH%
