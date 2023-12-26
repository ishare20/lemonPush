export namespace main {
	
	export class Config {
	    ip: string;
	    port: number;
	    auto_open_url: boolean;
	    folder: string;
	
	    static createFrom(source: any = {}) {
	        return new Config(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.ip = source["ip"];
	        this.port = source["port"];
	        this.auto_open_url = source["auto_open_url"];
	        this.folder = source["folder"];
	    }
	}
	export class Json {
	    code: number;
	    msg: string;
	
	    static createFrom(source: any = {}) {
	        return new Json(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.code = source["code"];
	        this.msg = source["msg"];
	    }
	}

}

