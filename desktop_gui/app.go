package main

import (
	"bufio"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/atotto/clipboard"
	wruntime "github.com/wailsapp/wails/v2/pkg/runtime"
)

var dt = time.Now()
var config Config = Config{
	Ip:            "",
	Port:          14756,
	Auto_open_url: false,
	Folder:        "./_lemon_",
}
var configFileName = "lemon.conf"
var actx context.Context

// App struct
type App struct {
	ctx context.Context
}

// NewApp creates a new App application struct
func NewApp() *App {
	return &App{}
}

type Config struct {
	Ip            string `json:"ip"`
	Port          int    `json:"port"`
	Auto_open_url bool   `json:"auto_open_url"`
	Folder        string `json:"folder"`
}

// startup is called when the app starts. The context is saved
// so we can call the runtime methods
func (a *App) startup(ctx context.Context) {
	getTray(ctx)
	a.ctx = ctx
	actx = ctx
}

// Greet returns a greeting for the given name
func (a *App) Greet(name string) string {
	return fmt.Sprintf("Hello %s, It's show time!", name)
}

/*
	 type IpAddress struct {
		ipAddress string
	}
*/

func (a *App) GetIPList() []string {
	var ips []string
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		fmt.Println(err)
	}
	for _, address := range addrs {
		// 检查ip地址判断是否回环地址
		if ipnet, ok := address.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
			if ipnet.IP.To4() != nil {
				// fmt.Println(dt.Format("2006-01-02 15:04:05"), "  本机IP:", ipnet.IP.String())
				ips = append(ips, ipnet.IP.String())
			}
		}
	}
	/* jsonArray, err := json.Marshal(ips)
	if err != nil {
		fmt.Println("JSON marshaling failed:", err)
		return "[]"
	} */
	return ips
}

type Json struct {
	Code int    `json:"code"`
	Msg  string `json:"msg"`
}

func (a *App) InitListener(config Config) Json {
	fmt.Println("InitListener" + config.Folder)
	port := ":" + strconv.Itoa(config.Port) // 监听端口
	http.HandleFunc("/set_clipboard", setClipboard)
	http.HandleFunc("/get_clipboard", getClipboard)
	http.HandleFunc("/download", download)
	http.HandleFunc("/upload", upload)
	if config.Folder != "" {
		createFolderIfNotExists(config.Folder)
	}
	go func() {
		err := http.ListenAndServe(port, nil)
		if err != nil {
			fmt.Println("Error starting HTTP server:", err)
		}
	}()
	return Json{Code: 1, Msg: "启动成功"}
}

func (a *App) SaveConfig(lemonConfig Config) Json {
	config = lemonConfig
	execPath, err := os.Executable()
	if err != nil {
		return Json{Code: 0, Msg: "获取执行路径失败"}
	}
	execDir := filepath.Dir(execPath)
	filePath := filepath.Join(execDir, "lemon.conf")

	file, err := os.OpenFile(filePath, os.O_RDWR|os.O_CREATE, 0644)
	if err != nil {
		return Json{Code: 0, Msg: "打开配置文件失败"}
	}
	defer file.Close()
	var boolstr = "false"
	if lemonConfig.Auto_open_url {
		boolstr = "true"
	}

	writer := bufio.NewWriter(file)
	_, err = writer.WriteString(fmt.Sprintf("ip=%s\nport=%d\nauto_open_url=%s\nfolder=%s\n", lemonConfig.Ip, lemonConfig.Port, boolstr, lemonConfig.Folder))
	if err != nil {
		return Json{Code: 0, Msg: "写入配置文件失败"}
	}
	writer.Flush()
	return Json{Code: 1, Msg: "成功保存"}
}

func (a *App) LoadConfig() (Config, error) {
	var lemonConfig, err = initConfig()
	config = lemonConfig
	return lemonConfig, err
}

func setClipboard(w http.ResponseWriter, r *http.Request) {
	values := r.URL.Query()
	code := values.Get("text")
	clipboard.WriteAll(code)
	fmt.Println("客户端 " + r.RemoteAddr + " 设置剪切板：" + code)
	wruntime.EventsEmit(actx, "showLogs", code)
	if config.Auto_open_url {
		p := regexp.MustCompile(`https?://[^\s]+/[^/]+`)
		if p.MatchString(code) {
			matches := p.FindAllString(code, -1)
			for _, match := range matches {
				fmt.Printf("%s  启动浏览器打开链接：%s\n", dt.Format("2006-01-02 15:04:05"), match)
				// openBrowser(match)
				wruntime.BrowserOpenURL(actx, match)
			}
		}
	}

	w.Header().Set("Content-Type", "application/json")
	resp := make(map[string]string)
	sendStr := "ok"
	resp["data"] = sendStr
	resp["code"] = "0"
	jsonResp, err := json.Marshal(resp)
	if err != nil {
		return
	}
	w.Write(jsonResp)
}

func getClipboard(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	resp := make(map[string]string)
	text, _ := clipboard.ReadAll()
	fmt.Println("客户端 " + r.RemoteAddr + " 获取剪切板：" + text)
	resp["data"] = text
	resp["code"] = "0"
	jsonResp, err := json.Marshal(resp)
	if err != nil {
		return
	}
	w.Write(jsonResp)
}

func (a *App) openBrowser(url string) {
	/* var cmd *exec.Cmd
	switch runtime.GOOS {
	case "darwin":
		cmd = exec.Command("open", url)
	case "linux":
		cmd = exec.Command("xdg-open", url)
	case "windows":
		cmd = exec.Command("cmd", "/c", "start", url)
	default:
		return fmt.Errorf("unsupported platform")
	} */
	wruntime.BrowserOpenURL(a.ctx, url)
}

func createFolderIfNotExists(folderPath string) error {
	_, err := os.Stat(folderPath)
	if os.IsNotExist(err) {
		errDir := os.MkdirAll(folderPath, 0755) // 0755代表默认的文件夹权限
		if errDir != nil {
			return errDir
		}
		fmt.Println("文件夹不存在，已创建:", folderPath)
	} else {
		fmt.Println("文件夹已存在:", folderPath)
	}
	return nil
}

func loadConfigFile(filename string) (map[string]string, error) {
	execPath, err := os.Executable()
	if err != nil {
		return nil, err
	}
	execDir := filepath.Dir(execPath)
	filePath := filepath.Join(execDir, filename)

	file, err := os.Open(filePath)
	if err != nil {
		// 文件不存在，使用默认配置并创建文件
		config := make(map[string]string)
		config["port"] = "14756"
		config["folder"] = "./_lemon_"
		config["ip"] = ""
		config["auto_open_url"] = "open"

		// 创建文件并写入默认配置
		file, err = os.OpenFile(filePath, os.O_RDWR|os.O_CREATE, 0644)
		if err != nil {
			return nil, err
		}
		defer file.Close()

		writer := bufio.NewWriter(file)
		for key, value := range config {
			_, err := writer.WriteString(key + "=" + value + "\n")
			if err != nil {
				return nil, err
			}
		}
		writer.Flush()

		return config, nil
	}
	defer file.Close()

	config := make(map[string]string)
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := scanner.Text()
		parts := strings.SplitN(line, "=", 2)
		if len(parts) == 2 {
			key := strings.TrimSpace(parts[0])
			value := strings.TrimSpace(parts[1])
			config[key] = value
		}
	}

	if err := scanner.Err(); err != nil {
		return nil, err
	}

	return config, nil
}

func download(w http.ResponseWriter, r *http.Request) {
	values := r.URL.Query()
	fileName := values.Get("filename")
	folderPath := config.Folder

	fmt.Println("客户端 " + r.RemoteAddr + " 下载文件：" + fileName)
	separator := string(filepath.Separator)
	file, err := os.Open(folderPath + separator + fileName)
	if err != nil {
		fmt.Println(err)
		http.Error(w, "文件未找到", http.StatusNotFound)
		return
	}
	defer file.Close()

	fileInfo, err := file.Stat()
	if err != nil {
		fmt.Println(err)
		http.Error(w, "文件信息无法获取", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Disposition", "attachment; filename="+fileName)
	w.Header().Set("Content-Type", "application/octet-stream")
	w.Header().Set("Content-Length", fmt.Sprint(fileInfo.Size()))

	_, copyErr := io.Copy(w, file)
	if copyErr != nil {
		fmt.Println(copyErr)
		http.Error(w, "文件无法下载", http.StatusInternalServerError)
		return
	}
}

func upload(w http.ResponseWriter, r *http.Request) {
	fmt.Println("客户端 " + r.RemoteAddr + " 上传文件")
	r.ParseMultipartForm(32 << 20)
	file, handler, err := r.FormFile("file")
	if err != nil {
		fmt.Println(err)
		return
	}
	defer file.Close()
	fmt.Println("文件名: " + handler.Filename)
	fmt.Println("文件大小: ", handler.Size)
	fmt.Println("MIME类型: " + handler.Header.Get("Content-Type"))
	// 创建一个目标文件
	separator := string(filepath.Separator)
	targetFile, err := os.Create(config.Folder + separator + handler.Filename)
	fmt.Println("文件路径: " + targetFile.Name())
	if err != nil {
		fmt.Println(err)
		return
	}
	defer targetFile.Close()

	// 将上传的文件内容拷贝到目标文件
	_, copyErr := io.Copy(targetFile, file)
	if copyErr != nil {
		fmt.Println(copyErr)
		return
	}

	resp := make(map[string]string)
	sendStr := "ok"
	resp["data"] = sendStr
	resp["code"] = "0"
	jsonResp, err := json.Marshal(resp)
	if err != nil {
		return
	}
	w.Write(jsonResp)
}

func initConfig() (Config, error) {
	execPath, err := os.Executable()
	if err != nil {
		return Config{}, err
	}
	execDir := filepath.Dir(execPath)
	filePath := filepath.Join(execDir, configFileName)
	file, err := os.Open(filePath)
	if err != nil {
		// 文件不存在，使用默认配置并创建文件
		config := Config{
			Port:          14756,
			Folder:        "./_lemon_",
			Ip:            "",
			Auto_open_url: true,
		}

		// 创建文件并写入默认配置
		file, err = os.OpenFile(filePath, os.O_RDWR|os.O_CREATE, 0644)
		if err != nil {
			return Config{}, err
		}
		defer file.Close()

		var boolstr = "false"
		if config.Auto_open_url {
			boolstr = "true"
		}

		writer := bufio.NewWriter(file)
		_, err := writer.WriteString(fmt.Sprintf("ip=%s\nport=%d\nauto_open_url=%s\nfolder=%s\n", config.Ip, config.Port, boolstr, config.Folder))
		if err != nil {
			return Config{}, err
		}
		writer.Flush()

		return config, nil
	}
	defer file.Close()

	var config Config
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := scanner.Text()
		parts := strings.SplitN(line, "=", 2)
		if len(parts) == 2 {
			key := strings.TrimSpace(parts[0])
			value := strings.TrimSpace(parts[1])

			switch key {
			case "ip":
				config.Ip = value
			case "port":
				port, err := strconv.Atoi(value)
				if err != nil {
					return Config{}, fmt.Errorf("invalid port value: %s", value)
				}
				config.Port = port
			case "auto_open_url":
				b, err := strconv.ParseBool(value)
				config.Auto_open_url = b
				if err == nil {
					fmt.Println(b) // 输出：true
				} else {
					fmt.Println("解析错误:", err)
					config.Auto_open_url = true
				}
			case "folder":
				config.Folder = value
			}
		}
	}

	if err := scanner.Err(); err != nil {
		return Config{}, err
	}

	return config, nil
}
