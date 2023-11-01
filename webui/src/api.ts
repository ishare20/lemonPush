import axios from 'axios';

interface lemonPush {
    /**
     * @description 服务器地址
     */
    baseUrl: string;
    /**
     * @description 文件列表
     */
    getFiles: () => Promise<string[]>;
    /**
     * @description 下载文件
     * @param fileName 文件名称
     */
    download: (fileName: string) => Promise<void>;
    /**
     * @description 上传文件
     * @param file 文件
     */
    upload: (file: File) => Promise<void>;
    /**
     * 设置剪贴板
     */
    setClipboard(text: string): Promise<void>;
    /**
     * @description 获取剪贴板
     * @returns 剪贴板内容
     */
    getClipboard(): Promise<string>;
}

export class LemonPushClient implements lemonPush {
    baseUrl: string;

    constructor(baseUrl: string) {
        this.baseUrl = baseUrl;
    }

    async getFiles(): Promise<string[]> {
        const { data, status } = await axios.get(`${this.baseUrl}/list`);
        if (this.isOk(status)) {
            return data.data;
        }
        alert(data.msg);
        return [];
    }

    async download(fileName: string): Promise<void> {
        window.open(`${this.baseUrl}/download?filename=${fileName}`);
    }

    async upload(file: File): Promise<void> {
        const formData = new FormData();
        formData.append('file', file);
        const { status } = await axios.post(`${this.baseUrl}/upload`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
        if (this.isOk(status)) {
            alert('ok');
        }
    }

    async setClipboard(text: string): Promise<void> {
        const { status } = await axios.get(`${this.baseUrl}/set_clipboard?text=${text}`);
        if (this.isOk(status)) {
            alert('ok');
        } else {
            alert('fail');
        }
    }

    async getClipboard(): Promise<string> {
        const { data, status } = await axios.get(`${this.baseUrl}/get_clipboard`);
        if (this.isOk(status)) {
            return data.data;
        } else {
            return '';
        }
    }

    isOk(status: number): boolean {
        return status === 200;
    }
}
