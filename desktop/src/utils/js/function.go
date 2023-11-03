package js

import (
	"bufio"
	"bytes"
	"fmt"
	"io"
	"io/ioutil"
	"log"
	"mime/multipart"
	"net/http"
	"os"
	"path/filepath"

	mylogger "net.blt/lemon_push/log"
)

var logger *log.Logger

func init() {
	logger = mylogger.GetLogger()
}

func Get(url string) string {
	resp, err := http.Get(url)
	if err != nil {
		return fmt.Sprintf("Error: %s", err)
	}
	defer resp.Body.Close()

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return fmt.Sprintf("Error reading response: %s", err)
	}

	return string(body)
}

func Post(url string, body string) string {
	resp, err := http.Post(url, "application/json", bytes.NewBuffer([]byte(body)))
	if err != nil {
		return fmt.Sprintf("Error: %s", err)
	}
	defer resp.Body.Close()

	responseBody, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return fmt.Sprintf("Error reading response: %s", err)
	}

	return string(responseBody)
}

type UploadOptions struct {
	ContentType string
	FilePath    string
	FieldName   string
}

// uploadFile uploads the file to the given url.
func Upload(url string, options UploadOptions) string {
	// Open the file.
	file, err := os.Open(options.FilePath)
	if err != nil {
		return fmt.Sprintf("Error opening file: %s", err)
	}
	defer file.Close()

	// Prepare a buffer to store the form data.
	body := &bytes.Buffer{}
	writer := multipart.NewWriter(body)

	// Create a form file field.
	if options.FieldName == "" {
		options.FieldName = "file"
	}

	part, err := writer.CreateFormFile(options.FieldName, options.FilePath)
	if err != nil {
		return fmt.Sprintf("Error creating form file: %s", err)
	}

	// Copy the file content to the form field.
	_, err = io.Copy(part, file)
	if err != nil {
		return fmt.Sprintf("Error copying file content: %s", err)
	}

	// Close the form data writer.
	err = writer.Close()
	if err != nil {
		return fmt.Sprintf("Error closing form writer: %s", err)
	}

	// Create the request.
	req, err := http.NewRequest("POST", url, body)
	if err != nil {
		return fmt.Sprintf("Error creating request: %s", err)
	}

	contentType := writer.FormDataContentType()
	if options.ContentType != "" {
		contentType = options.ContentType
	}

	req.Header.Set("Content-Type", contentType)

	// Perform the request.
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return fmt.Sprintf("Error performing request: %s", err)
	}
	defer resp.Body.Close()

	// Read the response body.
	responseBody, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return fmt.Sprintf("Error reading response: %s", err)
	}

	return string(responseBody)
}

func GetScript(jspath string) string {
	// 设置当前目录
	dir, err := os.Getwd()
	if err != nil {
		log.Fatal(err)
	}

	jsPath := filepath.Join(dir, jspath)

	script, err := ioutil.ReadFile(jsPath)
	if err != nil {
		logger.Println("无法读取 JavaScript 文件:", err)
		exampleScript := `
		function hook(params) {
			// bark
			const url = 'https://api.day.app/your_key/' + params;
			return get(url);
			// else
			// return post(url, body);
		}
		`
		// 创建文件并写入默认配置
		jsFile, err := os.OpenFile(jsPath, os.O_RDWR|os.O_CREATE, 0644)
		if err != nil {
			logger.Println("无法创建 JavaScript 文件:", err)

		}
		defer jsFile.Close()

		writer := bufio.NewWriter(jsFile)
		_, err = writer.WriteString(exampleScript)
		if err != nil {
			logger.Fatal("无法写入 JavaScript 文件:", err)
		}
		writer.Flush()
		logger.Println("已创建 JavaScript 文件:", jsPath)
	}

	return string(script)
}
