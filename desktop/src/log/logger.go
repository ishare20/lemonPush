package log

import (
	"io"
	"log"
	"os"
)

var logger *log.Logger
var logFile *os.File

func InitLog() {
	logFile, err := os.OpenFile("lemon_push.log", os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)
	if err != nil {
		log.Fatal(err)
	}

	mw := io.MultiWriter(os.Stdout, logFile)
	// log.SetOutput(mw)
	logger = log.New(mw, "", log.LstdFlags)
}

func GetLogger() *log.Logger {
	if logger == nil {
		InitLog()
	}
	return logger
}

func CloseLogFile() {
	if logFile != nil {
		err := logFile.Close()
		if err != nil {
			log.Fatal(err)
		}
	}
}

func init() {
	InitLog()
}
