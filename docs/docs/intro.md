---
sidebar_position: 1
---
# 使用教程
## 介绍
同一WiFi环境下手机高效推送文本到电脑剪切板的工具，移动端支持Android、iOS，电脑端支持Windows、Mac、Linux平台

## 功能特性
- 一键推送手机剪切板文本至电脑剪切板，文本中如包含网址可自动识别并使用默认浏览器打开
- 支持打开即推送，支持接收其他App分享的文本
- 体积小、电脑端支持多平台
- 支持多台手机推送到电脑，App支持多台电脑
- 电脑端提供支持下载、上传文件接口
- App支持扫码连接，无需手动输入

![](https://sibtools.app/lemon_push/img/gui_v1.0.5.1.png)

![lemonpush](https://sibtools.app/lemon_push/img/lemonpush.jpg)

## [下载地址(包含iOS快捷指令)](https://sibtools.app/lemon_push/docs/download)

## 配置教程
双击启动柠檬Push应用，Android端安装柠檬Push App，iOS端使用快捷指令，电脑端柠檬Push点击生成二维码，Android手机扫码匹配即可使用，iOS端修改快捷指令的IP为电脑IP即可使用

生成二维码时，电脑可能会出现多个IP，使用局域网所在网络的IP，一般192开头，扫码连接或填写电脑IP后点击推送剪切板即可获取剪切板并推送至电脑端

如出现端口冲突可修改端口号后重启程序

## 接口说明
### 写入电脑剪切板
`/set_clipboard?text=内容`

返回json
```
{
    "code":"0",
    "data":"ok"
}
```
### 获取电脑剪切板
`/get_clipboard`

返回json
```
{
    "code":"0",
    "data":"电脑剪切板内容"
}
```
### 上传文件
文件保存在目录`./_lemon_`

`/upload`

请求示例

`curl --location --request POST 'http://localhost:14756/upload' \
--form 'file=@"/E:/Downloads/__UNI__F0B72F8_0809143049.apk"'`

### 下载文件

`/download`

请求示例

`curl --location --request GET 'http://localhost:14756/download?filename=__UNI__F0B72F8_0809143049.apk'`

## 常见问题
- 电脑无法接收手机剪切板，需要配置电脑防火墙允许应用通过
- Mac系统暂不支持托盘图标
- Linux暂无图形界面版本

## 开发背景
日常手机与电脑互发消息频率较多，使用微信或QQ来发消息步骤略显繁琐

例如

一、手机的网页转发到电脑查看传统步骤：

1. 复制或分享链接
2. 选择 QQ 或微信发送
3. QQ 直接点击链接打开，微信还需复制链接到浏览器

二、手机验证码转发到电脑传统步骤：

1. 手机端复制验证码
2. 选择 QQ 或微信发送
3. 电脑端复制验证码

以上的痛点在手机厂商推出的多屏互联方案得以改善，但有所限制，如只支持部分手机或自家笔记本等

使用柠檬Push可减少以上步骤，在柠檬Push上面开启打开即推送，复制文本，切换至柠檬 Push 则会立刻推送文本到电脑剪切板，如文本含有链接自动使用默认浏览器打开

提高效率核心是减少步骤、减少选择。发文本到电脑几乎必然是复制到剪切板，发链接到电脑几乎必然用浏览器打开，所以柠檬Push基于以上设定开发

## 开发技术
电脑端使用固定端口作为服务端，基于局域网的http服务实现信息交互，使用Go语言实现电脑端程序，使用wails框架实现图形界面

## 已知问题
受限于作者的开发水平，软件还有许多未完善的地方。如使用传输内容未加密会存在安全性问题，除了局域网内有人主动攻击，对于多数的场景下是安全的

请不要从第三方平台下载软件，本项目代码开源，第三方平台的软件下载使用可能会被加入恶意代码导致信息泄露

## 建议反馈
欢迎在兔小巢建议反馈

[https://support.qq.com/products/405982](https://support.qq.com/products/405982)

电报群

[Telegram](https://t.me/+ZVIwHSBOg1o5NzFl)

微信订阅号【lemonTree杂货铺】

![wx](https://ishare20.net/files/images/wxdy.png)

## 支持开发者

柠檬Push如你对有所帮助，欢迎star、PR、feedback、share、donate支持开发者

![zw](https://ishare20.net/files/images/zw.jpg)

