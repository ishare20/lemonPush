package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"mime"
	"net"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"reflect"
	"regexp"
	"runtime"
	"strconv"
	"strings"
	"time"

	"github.com/atotto/clipboard"
	"github.com/dop251/goja"
	"github.com/mdp/qrterminal/v3"
	mylogger "net.blt/lemon_push/log"
	"net.blt/lemon_push/utils"
	"net.blt/lemon_push/utils/js"
)

type LemonConfig struct {
	Port        string `config:"port"`
	IP          string `config:"ip"`
	Folder      string `config:"folder"`
	SSL         string `config:"ssl"`
	ClippedHook string `config:"clippedHook"`
}

var logger *log.Logger

var config LemonConfig

var jsRuntime goja.Runtime

func init() {
	mylogger.InitLog()
	logger = mylogger.GetLogger()
	config = init_config()
	createFolderIfNotExists(config.Folder)
	init_hook()
}

func main() {
	defer mylogger.CloseLogFile() // 确保在程序退出时关闭日志文件
	// webui
	wd, _ := os.Getwd()
	webuiDir := filepath.Join(wd, "webui")
	fs := http.FileServer(http.Dir(webuiDir))
	http.Handle("/webui/", http.StripPrefix("/webui/", fs))

	http.HandleFunc("/set_clipboard", setClipboard)
	http.HandleFunc("/get_clipboard", getClipboard)
	http.HandleFunc("/download", download)
	http.HandleFunc("/upload", upload)
	http.HandleFunc("/list", list)
	http.HandleFunc("/files", getFiles)

	fmt.Println("  服务端监听端口:", config.Port)

	if config.IP == "" {
		localIPs := getLocalIP()
		fmt.Println("  本机IP列表:")
		for i, ip := range localIPs {
			fmt.Printf("%d. %s\n", i+1, ip)
		}
		for {
			fmt.Println("输入序号选择一个IP地址(仅用于生成二维码): ")
			reader := bufio.NewReader(os.Stdin)
			input, _ := reader.ReadString('\n')
			input = strings.TrimSpace(input)
			index, err := strconv.Atoi(input)
			if err == nil && index >= 1 && index <= len(localIPs) {
				config.IP = localIPs[index-1]
				break
			}
			fmt.Println("无效的选择，请重新输入.")
		}
	}

	url := fmt.Sprintf("%s:%s", config.IP, config.Port)
	// fixed bug: https://github.com/golang/go/issues/32350#issuecomment-1128475902
	_ = mime.AddExtensionType(".js", "text/javascript")
	fmt.Println("选择的IP地址:", config.IP, " 请使用App扫码连接")
	qRCode2ConsoleWithUrl(url)
	fmt.Println("服务端已启动")
	fmt.Printf("weui地址: http(s)://%s/webui", url)

	var serverType string
	var err error

	if config.SSL == "on" {
		// 判断证书是否存在不存在生成
		_, certErr := os.Stat("server.pem")
		if os.IsNotExist(certErr) {
			logger.Println("  证书不存在，正在生成证书...")
			gen()
		}
		err = http.ListenAndServeTLS(url, "server.pem", "server.key", nil)
		serverType = "https"
	} else {
		err = http.ListenAndServe(url, nil)
		serverType = "http"
	}

	if err != nil {
		logger.Panic("Error starting", serverType, "server:", err)
	} else {
		logger.Println("  服务端已启动，使用", serverType)
	}

}

func qRCode2ConsoleWithUrl(url string) {
	config := qrterminal.Config{
		Level:     qrterminal.L,
		BlackChar: qrterminal.BLACK,
		WhiteChar: qrterminal.WHITE,
		Writer:    os.Stdout,
	}
	qrterminal.GenerateWithConfig(url, config)
}

func getLocalIP() []string {
	var ips []string
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		logger.Println(err)
	}
	for _, address := range addrs {
		// 检查ip地址判断是否回环地址
		if ipnet, ok := address.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
			if ipnet.IP.To4() != nil {
				// logger.Println( "  本机IP:", ipnet.IP.String())
				ips = append(ips, ipnet.IP.String())
			}
		}
	}
	return ips
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

func setClipboard(w http.ResponseWriter, r *http.Request) {
	// 设置跨域
	setCORS(w)
	values := r.URL.Query()
	code := values.Get("text")
	clipboard.WriteAll(code)
	logger.Println("客户端 " + r.RemoteAddr + " 设置剪切板：" + code)
	p := regexp.MustCompile(`https?://[^\s]+/[^/]+`)
	if p.MatchString(code) {
		matches := p.FindAllString(code, -1)
		for _, match := range matches {
			logger.Printf("%s  启动浏览器打开链接：%s\n", match)
			openBrowser(match)
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
	setCORS(w)
	w.Header().Set("Content-Type", "application/json")
	resp := make(map[string]string)
	text, _ := clipboard.ReadAll()
	logger.Println("客户端 " + r.RemoteAddr + " 获取剪切板：" + text)
	resp["data"] = text
	resp["code"] = "0"
	jsonResp, err := json.Marshal(resp)
	if err != nil {
		return
	}
	w.Write(jsonResp)
}

func getFiles(w http.ResponseWriter, r *http.Request) {
	setCORS(w)
	resp := make(map[string]interface{})
	filenames, err := utils.Clipboard().Files()

	if err != nil {
		logger.Println(err)
		resp["code"] = "1"
		resp["msg"] = err.Error()
	} else {
		resp["data"] = filenames
		resp["code"] = "0"
	}

	jsonResp, err := json.Marshal(resp)
	if err != nil {
		return
	}
	w.Write(jsonResp)
}

var lastText string    // 用于存储前一次的剪贴板内容
var lastFiles []string // 用于存储前一次的剪贴板内容

// 监听剪贴板
func monitorClipboard() {

	jsRuntime.RunString(js.GetScript(config.ClippedHook))

	var hookFn func([]string, string) string

	for {
		files, err := utils.Clipboard().Files()
		if err != nil {
			fmt.Println("无法获取剪贴板文件，尝试读取剪贴板文字", err)
			time.Sleep(time.Second * 1)
		}

		var text string
		if len(files) == 0 {
			text, _ = clipboard.ReadAll()
		}

		if lastText == text && reflect.DeepEqual(lastFiles, files) {
			time.Sleep(time.Second * 1)
			continue
		}

		logger.Println("剪切板内容:", text, files)

		err = jsRuntime.ExportTo(jsRuntime.Get("hook"), &hookFn)
		if err != nil {
			log.Fatal("无法导出 JavaScript 函数:", err)
			time.Sleep(time.Second * 1)
			continue
		}

		jsResult := hookFn(files, text)
		logger.Println("hook 函数返回:", jsResult)

		lastText = text
		lastFiles = files

		time.Sleep(time.Second * 1)
	}
}

func download(w http.ResponseWriter, r *http.Request) {
	setCORS(w)
	values := r.URL.Query()
	fileName := values.Get("filename")
	folderPath := config.Folder

	logger.Println("客户端 " + r.RemoteAddr + " 下载文件：" + fileName)
	separator := string(filepath.Separator)
	file, err := os.Open(folderPath + separator + fileName)
	if err != nil {
		logger.Println(err)
		http.Error(w, "文件未找到", http.StatusNotFound)
		return
	}
	defer file.Close()

	fileInfo, err := file.Stat()
	if err != nil {
		logger.Println(err)
		http.Error(w, "文件信息无法获取", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Disposition", "attachment; filename="+fileName)
	w.Header().Set("Content-Type", "application/octet-stream")
	w.Header().Set("Content-Length", fmt.Sprint(fileInfo.Size()))

	_, copyErr := io.Copy(w, file)
	if copyErr != nil {
		logger.Println(copyErr)
		http.Error(w, "文件无法下载", http.StatusInternalServerError)
		return
	}
}

func upload(w http.ResponseWriter, r *http.Request) {
	setCORS(w)
	logger.Println("客户端 " + r.RemoteAddr + " 上传文件")
	r.ParseMultipartForm(32 << 20)
	file, handler, err := r.FormFile("file")
	if err != nil {
		logger.Println(err)
		return
	}
	defer file.Close()
	logger.Println("文件名: " + handler.Filename)
	logger.Println("文件大小: ", handler.Size)
	logger.Println("MIME类型: " + handler.Header.Get("Content-Type"))
	// 创建一个目标文件
	separator := string(filepath.Separator)
	targetFile, err := os.Create(config.Folder + separator + handler.Filename)
	logger.Println("文件路径: " + targetFile.Name())
	if err != nil {
		logger.Println(err)
		return
	}
	defer targetFile.Close()

	// 将上传的文件内容拷贝到目标文件
	_, copyErr := io.Copy(targetFile, file)
	if copyErr != nil {
		logger.Println(copyErr)
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

func list(w http.ResponseWriter, r *http.Request) {
	setCORS(w)
	resp := make(map[string]interface{})
	list, _ := getFilesInFolder(config.Folder)
	resp["data"] = list
	resp["code"] = "0"
	jsonResp, err := json.Marshal(resp)
	if err != nil {
		return
	}
	w.Write(jsonResp)
}

func createFolderIfNotExists(folderPath string) error {
	_, err := os.Stat(folderPath)
	if os.IsNotExist(err) {
		errDir := os.MkdirAll(folderPath, 0755) // 0755代表默认的文件夹权限
		if errDir != nil {
			return errDir
		}
		logger.Println("文件夹不存在，已创建:", folderPath)
	} else {
		logger.Println("文件夹已存在:", folderPath)
	}
	return nil
}

func getFilesInFolder(folderPath string) ([]string, error) {
	var files []string

	// 读取文件夹
	err := filepath.Walk(folderPath, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if !info.IsDir() {
			files = append(files, info.Name())
		}
		return nil
	})

	if err != nil {
		return nil, err
	}

	return files, nil
}

// 设置跨域
func setCORS(w http.ResponseWriter) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
}

// 时间格式化
func timeFormat() string {
	return time.Now().Format("2006-01-02 15:04:05")
}

func init_config() LemonConfig {
	loadedConfig, lerr := loadConfigFile("lemon_push.conf")
	if lerr != nil {
		log.Fatal("加载配置lemon_push.conf失败:", lerr)
	}

	return loadedConfig
}

func init_hook() {
	if config.ClippedHook != "" {
		jsRuntime = *goja.New()
		jsRuntime.Set("get", js.Get)
		jsRuntime.Set("post", js.Post)
		jsRuntime.Set("upload", js.Upload)

		go monitorClipboard()
	}
}

func loadConfigFile(filename string) (LemonConfig, error) {
	execPath, err := os.Executable()
	if err != nil {
		return LemonConfig{}, err
	}
	execDir := filepath.Dir(execPath)
	filePath := filepath.Join(execDir, filename)

	file, err := os.Open(filePath)
	if err != nil {
		// 文件不存在，使用默认配置并创建文件
		config := LemonConfig{
			Port:   "14756",
			IP:     "0.0.0.0",
			Folder: "./_lemon_",
			SSL:    "off",
		}

		// 创建文件并写入默认配置
		file, err = os.OpenFile(filePath, os.O_RDWR|os.O_CREATE, 0644)
		if err != nil {
			return LemonConfig{}, err
		}
		defer file.Close()

		writer := bufio.NewWriter(file)

		// 使用反射遍历结构体字段
		val := reflect.ValueOf(config)
		typ := val.Type()

		for i := 0; i < val.NumField(); i++ {
			field := val.Field(i)
			fieldName := typ.Field(i).Name
			fieldValue := field.Interface()
			_, err := writer.WriteString(fieldName + "=" + fieldValue.(string) + "\n")
			if err != nil {
				return LemonConfig{}, err
			}
		}

		writer.Flush()

		return config, nil
	}
	defer file.Close()

	config := LemonConfig{}
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := scanner.Text()
		parts := strings.SplitN(line, "=", 2)
		if len(parts) == 2 {
			key := strings.TrimSpace(parts[0])
			value := strings.TrimSpace(parts[1])
			// 根据键设置配置结构体的相应字段
			elem := reflect.ValueOf(&config).Elem()

			for i := 0; i < elem.NumField(); i++ {
				field := elem.Type().Field(i)
				if strings.EqualFold(key, field.Tag.Get("config")) {
					// 获取字段值
					fieldValue := elem.Field(i)
					if fieldValue.CanSet() {
						fieldValue.SetString(value)
					}
					break
				}
			}

		}
	}

	if err := scanner.Err(); err != nil {
		return LemonConfig{}, err
	}

	return config, nil
}
