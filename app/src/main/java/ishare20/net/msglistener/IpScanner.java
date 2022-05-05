package ishare20.net.msglistener;

import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
public class IpScanner extends Thread {

	private int mPort = -1;
	private ScannerLogger mScannerLogger = null;
	private String mCommandStr = "ifconfig";
	private ScanCallback mScanCallback = null;
	private static final String LO_IP = "127.0.0.1";
	private Vector<String> mLocalNetIps = new Vector<>();
	private Set<String> mResult = new HashSet<>();
	private AtomicInteger mLocalNetCount = new AtomicInteger(0);
	private AtomicBoolean mCallbackCalled = new AtomicBoolean(false);
	private int mExpendThreadNumber = 0;
	private long mTimeOut = 1000l;

	public IpScanner(int port, ScanCallback callback) {
		mPort = port;
		if (callback == null) {
			throw new IllegalArgumentException("Params callback can't be null!");
		}
		mScanCallback = callback;
	}


	public IpScanner setTimeOut(long  time){
		this.mTimeOut = time;
		return this;
	}

	public IpScanner setExpendThreadNumber(int  number){
		this.mExpendThreadNumber = number;
		return this;
	}

	public final IpScanner setCommandLine(String command) {
		if (command == null) {
			return this;
		}
		this.mCommandStr = command;
		return this;
	}
	
	public static interface ScanCallback {
		public void onFound(Set<String> ip, String hostIp, int port);
		public void onNotFound(String hostIp, int port);
	}
	
	public IpScanner setScannerLogger(ScannerLogger logger) {
		this.mScannerLogger = logger;
		return this;
	}
	
	private void printLog(String log) {
		if (this.mScannerLogger != null) {
			this.mScannerLogger.onScanLogPrint(log);
		}
	}
	
	public static interface ScannerLogger {
		public void onScanLogPrint(String msg);
	}
	long start ;

	public void startScan() {
		start = SystemClock.uptimeMillis();
		if (this.mPort > 0 && mScanCallback != null) {
			this.start();
		}
	}
	
	private boolean isLocalServer(String firstWord) {
		if ("10".equals(firstWord) || "192".equals(firstWord) || "172".equals(firstWord)) {
			return true;
		}
		return false;
	}
	
	private static class Ip {
		int position = -1;
		int[] addr = new int[4];
		public void push(String addrWord) {
			position ++;
			if (position >= 4) {
				throw new IllegalArgumentException("Ip only 4 addr word");
			} 
			addr[position] = Integer.parseInt(addrWord);
		}
		
		public String toIpString(int v) {
			StringBuilder builder = new StringBuilder();
			int p = ((v & 0xff000000)>> 24);
			if ( p < 0) {
				p += 256;
			}
			builder.append(p).append('.')
			.append((v & 0x00ff0000) >> 16).append('.')
			.append((v & 0x0000ff00) >> 8).append('.')
			.append((v & 0x000000ff))
			;
			return builder.toString();
		}
		
		public int toInt() {
			if (!isFull()) {
				throw new IllegalArgumentException("Ip only 4 addr word");
			}
			int v = 0;
			v |= (addr[0] & 0xff) << 24;
			v |= (addr[1] & 0xff) << 16;
			v |= (addr[2] & 0xff) << 8;
			v |= (addr[3] & 0xff) ;
			return v;
		}
		
		public void reset() {
			this.position = -1;
		}
		
		public boolean isFull() {
			return this.position == 3;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			for (int p = 0; p <= 3; p ++) {
				builder.append(addr[p]);
				if (p < 3) {
					builder.append('.');
				}
			}
			return builder.toString();
		}
	}
	
	private static class Mask {
		int position = -1;
		int[] mask = new int[4];
		public void push(String maskWord) {
			position ++;
			if (position >= 4) {
				throw new IllegalArgumentException("Mask code only 4 mask word");
			} 
			mask[position] = Integer.parseInt(maskWord);
		}
		
		public int toInt() {
			if (!isFull()) {
				throw new IllegalArgumentException("Ip only 4 addr word");
			}
			int v = 0;
			v |= (mask[0] & 0xff) << 24;
			v |= (mask[1] & 0xff) << 16;
			v |= (mask[2] & 0xff) << 8;
			v |= (mask[3] & 0xff) ;
			return v;
		}
		
		private int getValueByChar(char c) {
			if (c >= '0' && c <= '9') {
				return c - '0';
			} 
			if (c >= 'a' && c <= 'f') {
				return 10 +( c - 'a');
			}
			return 0;
		}
		
		private int parserInt(char c1,char c2) {
			int sum = getValueByChar(c1);
			sum = sum << 4;
			sum += getValueByChar(c2);
			return sum;
		}
		
		public void parserHex(String hex) {
			char[] code =new char[2];
			int p ;
			for (int index = 0; index < 8; index ++) {
				code[0] = hex.charAt(index);
				p = index >> 1;
				index ++;
				code[1] = hex.charAt(index);
				mask[p] = parserInt(code[0], code[1]);
			}
			position = 3;
		}
		
		public void reset() {
			this.position = -1;
		}
		public boolean isFull() {
			return this.position == 3;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			for (int p = 0; p <= 3; p ++) {
				builder.append(mask[p]);
				if (p < 3) {
					builder.append('.');
				}
			}
			return builder.toString();
		}
	}
	
	private void parser(InputStream is,Ip ip,Mask mask) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(is));
			StringBuilder ipBuilder = new StringBuilder();
            StringBuilder maskBuilder = new StringBuilder();
            String firstWord = null;
            String word = null;
            String line = reader.readLine();
            while (line != null) {
                line = line.toLowerCase();
                if (!ip.isFull()) {
                	firstWord = null;
                    int index = line.indexOf("inet ");
                    if (index >= 0) {
                        index += "inet ".length();
                        char c ;
                        while (index < line.length()) {
                            c = line.charAt(index);
                            if (ipBuilder.length() == 0 && (Character.isWhitespace(c) || !Character.isDigit(c))) {
                                index ++;
                                continue;
                            } else if (c == '.') {
                            	word = ipBuilder.toString();
                            	ipBuilder.delete(0, ipBuilder.length());
                            	if (firstWord == null) {
                            		firstWord = word;
                                	if (!isLocalServer(firstWord)) {
                                		break;
                                	}
                            	}
                            	if (ip.isFull()) {
                            		ip.reset();
                            		break;
                            	}
                            	ip.push(word);
                            	continue;
                            } else if (!Character.isDigit(c)) {
                            	if (ip.position == 2) {
                            		if (ipBuilder.length() <= 0) {
                            			ip.reset();
                            			break;
                            		} else {
                            			word = ipBuilder.toString();
                            			ipBuilder.delete(0, ipBuilder.length());
                            			ip.push(word);
                            		}
                            		break;
                            	} else {
                            		ip.reset();
                            		ipBuilder.delete(0, ipBuilder.length());
                            		break;
                            	}
                            }
                            ipBuilder.append(c);
                            index ++;
                        }
                    }
                }
                
                word = null;
                if (ip.isFull() && !mask.isFull()) {
                	try {
                		boolean isParserByHex = false;
                		int index = line.indexOf("mask");
                        if (index >= 0) {
                            index += "mask".length();
                            char c ;
                            while (index < line.length()) {
                                c = line.charAt(index);
                                if (maskBuilder.length() == 0 && (Character.isWhitespace(c) || !Character.isDigit(c))) {
                                    index ++;
                                    continue;
                                }else if (Character.isDigit(c)) {
                                	if (mask.position < 0 && maskBuilder.length() == 0) {
                                		if (c == '0') { //parse by hex
                                			index ++;
                                			c = line.charAt(index);
                                			if (c == 'x') {
                                				int start = index + 1;
                                				maskBuilder.append(line.substring(start  , start + "ffffffff".length()));
                                				isParserByHex = true;
                                				break;
                                			} else {
                                				maskBuilder.delete(0, maskBuilder.length());
                                        		mask.reset();
                                        		break;
                                			}
                                		}  else {
                                			isParserByHex = false;
                                		}
                                	}
                                }else if ('.' == c) {
                                	word = maskBuilder.toString();
                                	maskBuilder.delete(0, maskBuilder.length());
                                	mask.push(word);
                                	continue;
                                } else if(!Character.isDigit(c)){
                                	if (mask.position == 2) {
                                		if (maskBuilder.length() > 0) {
                                			mask.push(maskBuilder.toString());
                                			maskBuilder.delete(0, maskBuilder.length());
                                			break;
                                		} else {
                                			throw new IllegalArgumentException();
                                		}
                                	} else {
                                		throw new IllegalArgumentException();
                                	}
                                }
                                maskBuilder.append(c);
                                index ++;
                            }
                            if (isParserByHex && maskBuilder.length() > 0) {
                            	mask.parserHex(maskBuilder.toString());
                            	break;
                            }
                        }
                	} catch(Exception e){
                		maskBuilder.delete(0, maskBuilder.length());
                		mask.reset();
                	}
                }
                if (ip.isFull() && mask.isFull()) {
                    break;
                }
                line = reader.readLine();
            }
		} catch(Exception e) {
				//TODO
		}finally {
			if (reader != null) {
				try {
					reader.close();
				} catch(Exception e){}
			}
		}
	}
	
	@Override
	public void run() {
		try {
            Process process = Runtime.getRuntime().exec(mCommandStr);
            Ip ip = new Ip();
            Mask mask = new Mask();
            String ipHost = null;
            parser(process.getInputStream(), ip, mask);
			ipHost = ip.toString();
            if (ip.isFull() && mask.isFull()) {
            	 printLog("host ip:"+ipHost);
                 printLog("host mask:"+mask.toString()); 
                 int v = mask.toInt();
                 int vip = ip.toInt();
                 int begin = (vip & v);
                 v = ~v;
                 int ipValue;
                 for (int index = 1; index < v; index ++) {
                	 ipValue = begin + index;
					// System.out.println(ip.toIpString(ipValue));
                	 if (ipValue == vip) {
                		 continue;
                	 } else {
                		 String ipStr = ip.toIpString(ipValue);
						 mLocalNetIps.add(ipStr);
                	 }
                 }
				dispatchThreads(ipHost);
            }
		} catch(Exception e) {
			if (mScanCallback != null) {
				this.mScanCallback.onNotFound(LO_IP, this.mPort);
			}
		}
	}


	private void dispatchThreads(String iphost) {

		SelectChannelAction mainSelectAction = new SelectChannelAction(iphost,this.mPort,0);
		SelectChannelAction[] actions = new SelectChannelAction[mExpendThreadNumber + 1];
		actions[0] = mainSelectAction;
		for(int index = 1; index < mExpendThreadNumber + 1; index ++) {
			actions[index] = new SelectChannelAction(iphost,this.mPort,0);
		}
		int index = 0;
		SelectChannelAction action = null;
		for (String ip:mLocalNetIps) {
			index %= actions.length;
			action = actions[index];
			action.addChannel(ip);
		}

		for(index = 1; index < mExpendThreadNumber + 1; index ++) {
			actions[index].start();
		}
		mainSelectAction.run();

	}



	public class SelectChannelAction implements Runnable {

		String ipHost;
		int port;
		int index;
		private Map<SelectionKey,String> channels = new HashMap<>();
		private Selector selector = null;


		public SelectChannelAction(String ipHost, int port,int index) {
			this.ipHost = ipHost;
			this.port = port;
			this.index = index;
			try {
				selector = Selector.open();
			} catch (IOException e) {}
		}

		public void start() {
			new Thread(this,"Select#"+index).start();
		}

		public void addChannel(String ip) {
			try {
				SocketChannel socketChannel = SocketChannel.open();
				socketChannel.configureBlocking(false);
				SelectionKey key = socketChannel.register(selector,SelectionKey.OP_CONNECT);
				socketChannel.connect(new InetSocketAddress(ip,this.port));
				channels.put(key,ip);
				mLocalNetCount.incrementAndGet();
			} catch (Exception e) {}
		}

		@Override
		public void run() {
			long startTime = SystemClock.uptimeMillis();
			while(true) {
				try {
					int key = this.selector.select(100);
					//printLog(Thread.currentThread()+"select over "+key+":"+channels.size());
					if (key == 0) {
						if (channels.size() == 0) {
							break;
						} else {
							if (Math.abs(SystemClock.uptimeMillis() - startTime) > mTimeOut && channels.size() <= 2) {
								printLog("call timeout!!!");
								int size = channels.size() - 1;
								channels.clear();
								while (size >= 0) {
									mLocalNetCount.decrementAndGet();
									size --;
								}
								break;
							}
						}
						continue;
					}
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> iterator = selectedKeys.iterator();
					SelectionKey sKey = null;
					while(iterator.hasNext()) {
						sKey = iterator.next();
						if (sKey.isConnectable()) {
							SocketChannel channel = (SocketChannel)sKey.channel();
							String ip = this.channels.remove(sKey);
							try {
								startTime = SystemClock.uptimeMillis();
								if (channel.finishConnect()) {
									mResult.add(ip);
								}
								channel.close();
							} catch (Exception e) {}
						}
						sKey.cancel();
						iterator.remove();
						mLocalNetCount.decrementAndGet();
					}
					printLog(Thread.currentThread()+"size = "+this.channels.size());
					if (this.channels.size() == 0) {
						break;
					}
				} catch (Exception e) {
					//printLog("select exception "+e);
				}
			}
			try {
				this.selector.close();
			} catch (Exception e) {}
			int v = mLocalNetCount.get();
			printLog(Thread.currentThread()+" count = "+v);
			if (v == 0 && !mCallbackCalled.getAndSet(true)) {
				printLog("scan use time = "+(SystemClock.uptimeMillis() - start)/1000+"s");
				if (mResult.size() > 0) {
					mScanCallback.onFound(mResult,this.ipHost,mPort);
				} else {
					mScanCallback.onNotFound(this.ipHost,mPort);
				}
			}
			Thread.currentThread().interrupt();
		}
	}

}
