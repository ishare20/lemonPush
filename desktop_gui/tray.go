package main

import (
	"context"

	"github.com/getlantern/systray"
	wruntime "github.com/wailsapp/wails/v2/pkg/runtime"
	"net.blt/lemon_push/icon"
)

var instance Tray

type Tray struct {
	ctx context.Context // ctx from wails
}

func NewTray() *Tray {
	return &Tray{}
}

func init() {
	systray.Register(instance.onReady, nil)
}

func getTray(ctx context.Context) Tray {
	instance.ctx = ctx
	return instance
}

func (t *Tray) onReady() {
	systray.SetTemplateIcon(icon.Data, icon.Data)
	systray.SetTitle("柠檬Push")
	systray.SetTooltip("柠檬Push")
	mShow := systray.AddMenuItem("显示界面", "显示界面")
	mQuit := systray.AddMenuItem("退出应用", "退出应用")
	go func() {
		for {
			select {
			case <-mShow.ClickedCh:
				wruntime.WindowShow(instance.ctx)
			case <-mQuit.ClickedCh:
				systray.Quit()
				return
			}
		}
	}()
}
