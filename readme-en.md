# LemonPush
LemonPush is an efficient tool for pushing text from your mobile device to your computer's clipboard under the same WiFi environment. It supports Android and iOS on the mobile side, and Windows, Mac, and Linux platforms on the computer side.

## Key Features
- One-click push of mobile clipboard text to computer clipboard, with automatic recognition of URLs for opening in the default browser.
- Supports push-on-open and receiving text shared from other apps.
- No GUI, no installation required, small size, and multi-platform support for computers.
- Allows multiple mobile devices to push to the computer, and the app supports multiple computers.

![lemonpush](https://sibtools.app/lemon_push/img/lemonpush.jpg)

## [Download (Including iOS Shortcuts) ↗](https://sibtools.app/lemon_push/docs/download)

## Configuration Guide
After double-clicking to start the program on your computer, it will display the computer's IP. Install the LemonPush App on your phone, click to set the IP displayed on the computer side. If multiple IPs are displayed, use the IP of your local network (usually starts with 192). After filling in the computer's IP, click Push Clipboard to get the clipboard and push it to the computer side.

The first time the program runs, it will create a default configuration file named `lemon_push.conf`. If there are any port conflicts, you can edit the port number in the configuration file and restart the program.

## API Documentation
### Writing to Computer's Clipboard
`/set_clipboard?text=content`

Returns JSON
```
{
    "code":"0",
    "data":"ok"
}
```
### Getting the Computer's Clipboard
`/get_clipboard`

Returns JSON
```
{
    "code":"0",
    "data":"content of the computer's clipboard"
}
```
## Frequently Asked Questions
- If the computer cannot receive the mobile clipboard, you need to configure the computer's firewall (tutorial to be supplemented).
- If the Mac cannot run the program with a double-click, you need to configure file permissions using the command `chmod u+x program filename`.
- If you do not need the console when running the program, you can run it in the background. On Windows, use `Start-Process -WindowStyle hidden -FilePath "program"`. On Mac, use `nohup program &`.

## Project Background
In daily life, the frequency of sending messages between mobile phones and computers is high, and using WeChat or QQ to send messages is slightly cumbersome.

For example, the traditional steps of forwarding a webpage from a mobile phone to a computer for viewing are:

1. Copy or share the link.
2. Choose to send via QQ or WeChat.
3. Click the link directly in QQ, or copy the link to the browser in WeChat.

The pain points of the above steps can be improved by the multi-screen interconnection scheme pushed by mobile phone manufacturers, but there are limitations, such as only supporting some mobile phones or their own notebooks.

Using LemonPush can reduce the above steps. If you turn on Push-on-Open on LemonPush, copy the text, switch to LemonPush, and it will immediately push the text to the computer's clipboard. If the text contains a link, it will automatically use the default browser to open it.

The core of improving efficiency is to reduce steps and choices. Sending text to the computer almost inevitably means copying it to the clipboard, and sending a link to the computer almost inevitably means opening it in a browser, so LemonPush is developed based on these settings.

## Technologies Used
The computer side turns the clipboard interface into an HTTP service. Information interaction is achieved based on the HTTP service of the local area network. The computer side program is implemented in Go language.

## Known Issues
Due to the author's development level, there are still many imperfections in the software. For example, the content transmitted is not encrypted and there are security issues. Except for someone deliberately attacking in the local area network, it is safe under most scenarios.

Please do not download software from third-party platforms. The project code is open source, and the software downloaded from third-party platforms may be added with malicious code, causing information leakage.

## Feedback and Suggestions
Feedback and suggestions are welcome at TXC.

[https://support.qq.com/products/405982 ↗](https://support.qq.com/products/405982)

Telegram Group

[Telegram ↗](https://t.me/+ZVIwHSBOg1o5NzFl)

## Support the Developer
If LemonPush has been helpful to you, feel free to star, PR, feedback, share, donate to support the developer.

![zw](https://raw.githubusercontent.com/ishare20/lemonPush/master/docs/static/img/zw.jpg)