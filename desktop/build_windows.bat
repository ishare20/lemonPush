set APP_NAME=lemon_push
set APP_VERSION=v104
cd ./src
:: 编译为 Windows 可执行文件
set GOOS=windows
set GOARCH=amd64

go build -ldflags="-s -w" -o ../dist/%APP_NAME%_%APP_VERSION%_%GOOS%_%GOARCH%.exe && upx -9 ../dist/%APP_NAME%_%APP_VERSION%_%GOOS%_%GOARCH%.exe

@REM :: 编译为 macOS 可执行文件
@REM set GOOS=darwin
@REM set GOARCH=amd64
@REM go build -o ../dist/%APP_NAME%_%APP_VERSION%_%GOOS%_%GOARCH%

@REM :: 编译为 macOS Apple Silicon可执行文件
@REM set GOOS=darwin
@REM set GOARCH=arm64
@REM go build -o ../dist/%APP_NAME%_%APP_VERSION%_%GOOS%_%GOARCH%

@REM :: 编译为 Linux 可执行文件
@REM set GOOS=linux
@REM set GOARCH=amd64
@REM go build -o ../dist/%APP_NAME%_%APP_VERSION%_%GOOS%_%GOARCH%
