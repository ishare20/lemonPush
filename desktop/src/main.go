package main

import (
	"fmt"
	"net"
	"os/exec"
	"regexp"
	"runtime"
	"time"

	"github.com/atotto/clipboard"
)

var dt = time.Now()

func main() {
	socketService()
}

func socketService() {
	fmt.Println(dt.Format("2006-01-02 15:04:05"), "  服务端已启动")

	getLocalIP()

	port := 14756 //监听端口

	fmt.Println(dt.Format("2006-01-02 15:04:05"), "  服务端监听端口:", port)

	listener, err := net.Listen("tcp", fmt.Sprintf("%s:%d", "0.0.0.0", port))
	if err != nil {
		fmt.Println("Error listening:", err.Error())
		return
	}
	defer listener.Close()

	for {
		conn, err := listener.Accept()
		if err != nil {
			fmt.Println("Error accepting: ", err.Error())
			return
		}
		go handleRequest(conn)
	}
}

func getLocalIP() {
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		fmt.Println(err)
	}
	for _, address := range addrs {
		// 检查ip地址判断是否回环地址
		if ipnet, ok := address.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
			if ipnet.IP.To4() != nil {
				fmt.Println(dt.Format("2006-01-02 15:04:05"), "  本机IP:", ipnet.IP.String())
			}
		}
	}
}
func openBrowser(url string) error {
	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "darwin":
		cmd = exec.Command("open", url)
	case "linux":
		cmd = exec.Command("xdg-open", url)
	case "windows":
		cmd = exec.Command("cmd", "/c", "start", url)
	default:
		return fmt.Errorf("unsupported platform")
	}
	return cmd.Start()
}

func handleRequest(conn net.Conn) {
	//处理客户端请求
	defer conn.Close()

	buf := make([]byte, 1024)
	n, err := conn.Read(buf)
	if err != nil {
		fmt.Println("Error reading:", err.Error())
		return
	}

	code := string(buf[:n])
	fmt.Printf("%s   接收客户端%s的消息：\n%s\n", dt.Format("2006-01-02 15:04:05"), conn.RemoteAddr().String(), code)
	clipboard.WriteAll(code)
	//匹配URL
	p := regexp.MustCompile(`(http|ftp|https)://([\w_-]+(?:(?:\.[\w_-]+)+))([\w.,@?^=%&:/~+#-]*[\w@?^=%&/~+#-])?`)
	if p.MatchString(code) {
		matches := p.FindAllString(code, -1)
		for _, match := range matches {
			fmt.Printf("%s  启动浏览器打开链接：%s\n", dt.Format("2006-01-02 15:04:05"), match)
			openBrowser(match)
		}
	}

	//给Client端返回信息
	sendStr := "已成功接到您发送的消息"
	_, err = conn.Write([]byte(sendStr))
	if err != nil {
		fmt.Println("Error sending:", err.Error())
		return
	}
}
