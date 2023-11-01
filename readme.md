## 柠檬 Push

[简体中文](readme.md) | [English](readme-en.md)

同一 WiFi 环境下手机高效推送文本到电脑剪切板的工具，移动端支持 Android、iOS，电脑端支持 Windows、Mac、Linux 平台

LemonPush is an efficient tool for pushing text from your mobile device to your computer's clipboard under the same WiFi environment. It supports Android and iOS on the mobile side, and Windows, Mac, and Linux platforms on the computer side.

## 功能特性

-   一键推送手机剪切板文本至电脑剪切板，文本中如包含网址可自动识别并使用默认浏览器打开
-   支持打开即推送，支持接收其他 App 分享的文本
-   无图形界面、无需安装、体积小、电脑端支持多平台
-   支持多台手机推送到电脑，App 支持多台电脑
-   App 支持扫码连接，无需手动输入
-   webui

![lemonpush](https://sibtools.app/lemon_push/img/lemonpush.jpg)

## [下载地址(包含 iOS 快捷指令)](https://sibtools.app/lemon_push/docs/download)

## 配置教程

电脑双击启动程序后会显示电脑 IP，手机安装柠檬 Push App 后，点击设置电脑端显示 IP，可能会出现多个 IP，使用局域网所在网络的 IP，一般 192 开头，填写电脑 IP 后点击推送剪切板即可获取剪切板并推送至电脑端

程序首次运行会创建默认配置文件 lemon_push.conf，如出现端口冲突可在配置文件修改端口号后重启程序

## 接口说明

### 写入电脑剪切板

`/set_clipboard?text=内容`

返回 json

```
{
    "code":"0",
    "data":"ok"
}
```

### 获取电脑剪切板

`/get_clipboard`

返回 json

```
{
    "code":"0",
    "data":"电脑剪切板内容"
}
```

## 常见问题

-   电脑无法接收手机剪切板，需要配置电脑防火墙（教程待补充）
-   Mac 电脑双击无法运行，需配置文件权限，运行命令`chmod u+x 程序文件名`
-   双击程序运行会展示控制台并输出日志，如不需要控制台，可后台运行
    Windows 运行`Start-Process -WindowStyle hidden -FilePath "程序"`，Mac 运行`nohup 程序 &`

## 开发背景

日常手机与电脑互发消息频率较多，使用微信或 QQ 来发消息步骤略显繁琐

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

使用柠檬 Push 可减少以上步骤，在柠檬 Push 上面开启打开即推送，复制文本，切换至柠檬 Push 则会立刻推送文本到电脑剪切板，如文本含有链接自动使用默认浏览器打开

提高效率核心是减少步骤、减少选择。发文本到电脑几乎必然是复制到剪切板，发链接到电脑几乎必然用浏览器打开，所以柠檬 Push 基于以上设定开发

## 开发技术

电脑端将剪切板接口转为 http 服务，基于局域网的 http 服务实现信息交互，使用 Go 语言实现电脑端程序

## 已知问题

受限于作者的开发水平，软件还有许多未完善的地方。如使用传输内容未加密会存在安全性问题，除了局域网内有人主动攻击，对于多数的场景下是安全的

请不要从第三方平台下载软件，本项目代码开源，第三方平台的软件下载使用可能会被加入恶意代码导致信息泄露

## 建议反馈

欢迎在兔小巢建议反馈

[https://support.qq.com/products/405982](https://support.qq.com/products/405982)

电报群

[Telegram](https://t.me/+ZVIwHSBOg1o5NzFl)

## 支持开发者

柠檬 Push 如你对有所帮助，欢迎 star、PR、feedback、share、donate 支持开发者

![zw](https://raw.githubusercontent.com/ishare20/lemonPush/master/docs/static/img/zw.jpg)
