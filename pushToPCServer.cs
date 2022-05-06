using System;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Windows.Forms;

namespace ConsoleMsgApp
{
    class Program

    {

        static Socket socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
        private static byte[] result = new byte[1024];

        static DateTime dt = DateTime.Now;
        static void Main(string[] args)
        {
            SocketServie();
        }
        public static void SocketServie()
        {
            Console.WriteLine(dt.ToString() + "  服务端已启动");

            string host = GetLocalIP();

            int port = 14756;//监听端口
            Console.WriteLine(dt.ToString() + "  服务端IP:" + host );
            Console.WriteLine(dt.ToString() + "  服务端监听端口:" + port);

            socket.Bind(new IPEndPoint(IPAddress.Parse(host), port));
            socket.Listen(100);//设定最多100个排队连接请求
            Thread myThread = new Thread(ListenClientConnect);//通过多线程监听客户端连接
            myThread.Start();
            Console.ReadLine();
        }

        public static string GetLocalIP()
        {
            ///获取本地的IP地址
            string AddressIP = string.Empty;
            foreach (IPAddress _IPAddress in Dns.GetHostEntry(Dns.GetHostName()).AddressList)
            {

                if (_IPAddress.AddressFamily.ToString() == "InterNetwork")
                {
                    AddressIP = _IPAddress.ToString();
                }
            }
            return AddressIP;
        }

        private static void ListenClientConnect()
        {
            while (true)
            {
                Socket clientSocket = socket.Accept();
                //clientSocket.Send(Encoding.UTF8.GetBytes("服务器连接成功"));
                Thread receiveThread = new Thread(ReceiveMessage);
                receiveThread.SetApartmentState(ApartmentState.STA);
                receiveThread.Start(clientSocket);
            }
        }

        private static void ReceiveMessage(object clientSocket)
        {
            Socket myClientSocket = (Socket)clientSocket;
            while (true)
            {
                try
                {
                    //通过clientSocket接收数据
                    int receiveNumber = myClientSocket.Receive(result);
                    if (receiveNumber == 0)
                        return;

                    string code = Encoding.UTF8.GetString(result, 0, receiveNumber);
                    Console.WriteLine(dt.ToString() + "  接收客户端{0} 的消息：\n{1}", myClientSocket.RemoteEndPoint.ToString(), Encoding.UTF8.GetString(result, 0, receiveNumber));

                    Clipboard.SetDataObject(code);
                    //匹配URL
                    Regex p = new Regex(@"(http|ftp|https)://([\w_-]+(?:(?:\.[\w_-]+)+))([\w.,@?^=%&:/~+#-]*[\w@?^=%&/~+#-])?");
                    if (p.IsMatch(code))
                    {
                        foreach (Match match in p.Matches(code))
                        {
                            Console.WriteLine(dt.ToString() + "  启动浏览器打开链接：" + match.Value);
                            System.Diagnostics.Process.Start(match.Value);
                        }

                    }

                    //给Client端返回信息
                    string sendStr = "已成功接到您发送的消息";
                    byte[] bs = Encoding.UTF8.GetBytes(sendStr);//Encoding.UTF8.GetBytes()不然中文会乱码
                    myClientSocket.Send(bs, bs.Length, 0);  //返回信息给客户端
                    myClientSocket.Close(); //发送完数据关闭Socket并释放资源
                    Console.ReadLine();
                }
                catch (Exception ex)
                {
                    Console.WriteLine(ex.Message);
                    myClientSocket.Shutdown(SocketShutdown.Both);//禁止发送和上传
                    myClientSocket.Close();//关闭Socket并释放资源
                    break;
                }
            }
        }
    }
}
