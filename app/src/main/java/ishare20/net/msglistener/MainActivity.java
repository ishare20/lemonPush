package ishare20.net.msglistener;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    MyReceiver yBroadCastReceiver;
    private static String host;
    private Button sendBt;
    private Button scanBt;
    private IpScanner ipScanner;
    private Handler handler = new Handler();
    private TextView infoTv;
    private TextView msgTv;
    private static int PORT = 14756; //服务端端口
    private boolean isAutoSendCode = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoTv = findViewById(R.id.info);
        sendBt = findViewById(R.id.send);
        scanBt = findViewById(R.id.scan);
        msgTv = findViewById(R.id.msg);


        scanBt.setOnClickListener(view -> {
            if (!NetworkUtil.isWifiConnected(this)) {
                Toast.makeText(this, "请打开WIFI连接局域网", Toast.LENGTH_LONG).show();
                return;
            }
            final ProgressDialog mProgressDialog = new ProgressDialog(this);
            mProgressDialog.show();
            ipScanner = new IpScanner(PORT, new IpScanner.ScanCallback() {
                @Override
                public void onFound(Set<String> ip, String hostIp, int port) {
                    mProgressDialog.dismiss();
                    Log.i("hostIp", hostIp);
                    for (String s : ip) {
                        host = s;
                    }
                    Log.i("host", host);
                    handler.post(() -> infoTv.setText("\b\b\b本机地址：" + hostIp + "\n服务端地址：" + host + ":" + PORT));

                }

                @Override
                public void onNotFound(String hostIp, int port) {
                    mProgressDialog.dismiss();
                    handler.post(() -> Toast.makeText(MainActivity.this, "未检测到服务端", Toast.LENGTH_LONG).show());

                }
            });
            ipScanner.setExpendThreadNumber(10);
            ipScanner.setScannerLogger(System.out::println);
            ipScanner.startScan();
        });


        sendBt.setOnClickListener(view -> new Thread(() -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = clipboard.getPrimaryClip();
            String clipStr = "";
            if (clipData != null && clipData.getItemCount() > 0) {
                clipStr = clipData.getItemAt(0).getText().toString();
            }
            if (sendSocket(host, clipStr)) {
                handler.post(() -> Toast.makeText(MainActivity.this, "发送成功", Toast.LENGTH_SHORT).show());
            } else {
                handler.post(() -> Toast.makeText(MainActivity.this, "发送失败", Toast.LENGTH_SHORT).show());
            }

        }).start());

        if (isAutoSendCode) {
            if (checkAndRequestPermission()) {
                Log.i("注册广播", "run");

                yBroadCastReceiver = new MyReceiver();
                IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
                registerReceiver(yBroadCastReceiver, intentFilter,
                        "android.permission.RECEIVE_SMS", null);
            } else {
                isAutoSendCode = false;
            }
        }


    }

    @Override
    protected void onResume() {

        if (!NetworkUtil.isWifiConnected(this)) {
            Toast.makeText(this, "请打开WIFI连接局域网", Toast.LENGTH_LONG).show();
            return;
        } else {
            if (host != null) {
                infoTv.setText("\b\b\b本机地址：" + host + "\n服务端地址：" + host + ":" + PORT);
            }
        }
        Intent intent = getIntent();
        String action = intent.getAction();
        if (host != null && action != null) {
            String share = "";
            if (Intent.ACTION_SEND.equals(action)) {
                share = intent.getStringExtra(Intent.EXTRA_TEXT);
            }
            if (Intent.ACTION_VIEW.equals(action)) {
                share = intent.getDataString();
            }
            String finalShare = share;
            if (!finalShare.equals("")) {
                new Thread(() -> {
                    if (sendSocket(host, finalShare)) {
                        handler.post(() -> {
                                    Toast.makeText(MainActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                        );


                    } else {
                        handler.post(() -> Toast.makeText(MainActivity.this, "发送失败", Toast.LENGTH_SHORT).show());
                        finish();
                    }
                }).start();
            }
        } else {
            Toast.makeText(this, "请先扫描服务端", Toast.LENGTH_LONG).show();
        }
        super.onResume();
    }

    public static boolean sendSocket(String host, String content) {
        int port = PORT;
        Socket socket = null;
        OutputStream outputStream = null;
        try {
            socket = new Socket(host, port);
            // 建立连接后获得输出流
            outputStream = socket.getOutputStream();
            String message = content;
            socket.getOutputStream().write(message.getBytes("UTF-8"));
            outputStream.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
            return false;

        }
        return true;
    }

    private String getIPinWiFi() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return int2ip(wifiManager.getConnectionInfo().getIpAddress());
    }

    public static String int2ip(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }


    public void SetShowContent(String content) {

        SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();// 获取当前时间
        //将日志输出到app主页
        msgTv.append(sdf.format(date) + content + "\n");
    }

    public static class MyThread1 extends Thread {
        private Context context;
        private Map<String, String> params;
        private String url;
        private Handler handler;


        MyThread1(Context context, Map<String, String> params, String url, Handler handler) {
            this.context = context;
            this.params = params;
            this.url = url;
            this.handler = handler;
        }

        public void run() {
            try {
                //发送post通知
                boolean result = sendSocket(url, params.get("TA_content"));
                if (result) {
                    handler.post(() -> Toast.makeText(context, "发送成功", Toast.LENGTH_SHORT).show());
                } else {
                    handler.post(() -> Toast.makeText(context, "发送失败", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                //通知失败
                Log.i("SendMsg", "post失败了" + e);
            }
        }
    }

    public static boolean sendPostRequest(String path,
                                          Map<String, String> params, String encoding) throws Exception {
        StringBuilder data = new StringBuilder();
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                data.append(entry.getKey()).append("=");
                data.append(URLEncoder.encode(entry.getValue(), encoding));// 编码
                data.append('&');
            }
            data.deleteCharAt(data.length() - 1);
        }
        byte[] entity = data.toString().getBytes(); // 得到实体数据
        HttpURLConnection connection = (HttpURLConnection) new URL(path)
                .openConnection();
        connection.setConnectTimeout(5000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length",
                String.valueOf(entity.length));

        connection.setDoOutput(true);// 允许对外输出数据
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(entity);

        if (connection.getResponseCode() == 200) {
            return true;
        }
        return false;
    }

    public class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            //获得短信
            Object[] objs = (Object[]) arg1.getExtras().get("pdus");
            StringBuilder sb = new StringBuilder();
            String from = "";
            for (Object obj : objs) {
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) obj);
                from = smsMessage.getOriginatingAddress();
                sb.append(smsMessage.getMessageBody());
            }
            String body = sb.toString();
            SetShowContent("收到了来自" + from + "的短信" + body);
            Log.i("SendMsg", "收到了来自" + from + "的短信");
            Log.i("body", "body is:" + body + "");
            Map<String, String> params = new HashMap<>();
            if (body.contains("验证码") || body.contains("提取码")) {
                params.put("TA_action_on", "1");
                params.put("TA_title", from);
                params.put("TA_content", getCode(body));
                try {
                    Thread thread = new MainActivity.MyThread1(MainActivity.this, params, host, handler);
                    thread.start();
                } catch (Exception e) {
                    Log.i("SendMsg", "发送请求异常" + e);
                    SetShowContent("发送请求异常" + e);
                }
            }
        }

    }

    private String getCode(String text) {
        String pattern = "[0-9]{4,6}";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group();
        } else {
            return "";
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1024 && hasAllPermissionsGranted(grantResults)) {
            yBroadCastReceiver = new MyReceiver();
            IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
            registerReceiver(yBroadCastReceiver, intentFilter,
                    "android.permission.RECEIVE_SMS", null);
        } else {
            // 如果用户没有授权，那么应该说明意图，引导用户去设置里面授权。
            /*Toast.makeText(this, "应用缺少必要的权限！请点击\"权限\"，打开所需要的权限。", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            finish();*/
            Toast.makeText(this, "你已拒绝读取短信权限，不再启用自动发送验证码到电脑剪切板功能", Toast.LENGTH_LONG).show();
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private boolean checkAndRequestPermission() {
        List<String> lackedPermission = new ArrayList<String>();
        if (!(checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.RECEIVE_SMS);
        }

        if (!(checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.READ_SMS);
        }
        if (lackedPermission.size() == 0) {
            return true;
        } else {
            String[] requestPermissions = new String[lackedPermission.size()];
            lackedPermission.toArray(requestPermissions);
            requestPermissions(requestPermissions, 1024);
            return false;
        }
    }

    private boolean hasAllPermissionsGranted(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(yBroadCastReceiver);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
