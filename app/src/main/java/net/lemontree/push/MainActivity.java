package net.lemontree.push;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    //MyReceiver yBroadCastReceiver;
    private static String host = "";
    private Button sendBt;
    private IpScanner ipScanner;
    private Handler handler = new Handler();
    private TextView infoTv;
    private static int PORT = 14756; //电脑端端口
    //private boolean isAutoSendCode = false;
    private SharedPreferences sp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoTv = findViewById(R.id.info);
        sendBt = findViewById(R.id.send);

        sp = getSharedPreferences("settings", MODE_PRIVATE);

        host = sp.getString("host", "");




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


    }

    @Override
    protected void onResume() {

        if (!NetworkUtil.isWifiConnected(this)) {
            Toast.makeText(this, "请打开WIFI连接局域网", Toast.LENGTH_LONG).show();
            return;
        } else {
            if (!host.equals("")) {
                infoTv.setText("电脑端：" + host + ":" + PORT);
            }
        }
        Intent intent = getIntent();
        String action = intent.getAction();
        if (!host.equals("") && action != null) {
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
            Toast.makeText(this, "手动设置电脑IP或扫描", Toast.LENGTH_LONG).show();
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


  /*  public void SetShowContent(String content) {

        SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();// 获取当前时间
        //将日志输出到app主页
        msgTv.append(sdf.format(date) + content + "\n");
    }*/

    /*public static class MyThread1 extends Thread {
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
    }*/


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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.set) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("电脑IP");
            EditText editText = new EditText(this);
            editText.setHint("请输入电脑端IP地址");
            builder.setView(editText);
            builder.setPositiveButton("确定", (dialogInterface, i) -> {
                SharedPreferences.Editor editor = sp.edit();
                // TODO: 2022/5/5 校验是否为ip地址
                host = editText.getText().toString();
                if (!host.equals("")){
                    editor.putString("host", host);
                    editor.apply();
                    infoTv.setText("电脑端：" + host + ":" + PORT);
                }else {
                    showToast("IP地址为空，请重新输入");
                }

            });
            builder.setNegativeButton("取消", (dialogInterface, i) -> {
                dialogInterface.dismiss();
            });
            builder.create().show();
        }else if (item.getItemId() == R.id.guide){
            openUrl("https://gitee.com/ishare20/pushToPC");
        }else if (item.getItemId() == R.id.scan){
            if (!NetworkUtil.isWifiConnected(this)) {
                Toast.makeText(this, "请打开WIFI连接局域网", Toast.LENGTH_LONG).show();
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
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("host", host);
                    editor.apply();
                    handler.post(() -> infoTv.setText("电脑端：" + host + ":" + PORT));
                }

                @Override
                public void onNotFound(String hostIp, int port) {
                    mProgressDialog.dismiss();
                    handler.post(() -> Toast.makeText(MainActivity.this, "未检测到电脑端", Toast.LENGTH_LONG).show());
                }
            });
            ipScanner.setExpendThreadNumber(10);
            ipScanner.setScannerLogger(System.out::println);
            ipScanner.startScan();
        }
        return super.onOptionsItemSelected(item);
    }
    private void showToast(String msg){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }

    private void openUrl(String url) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        startActivity(intent);
    }


    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
