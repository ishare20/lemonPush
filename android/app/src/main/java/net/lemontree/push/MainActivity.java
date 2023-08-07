package net.lemontree.push;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CompoundButton;
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

    private static String host = "";
    private Button sendBt;
    private Handler handler = new Handler();
    private TextView infoTv;
    private SwitchCompat openAndPushSwitch;
    private static final int PORT = 14756; //电脑端端口
    private SharedPreferences sp;
    private ClipboardManager clipboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoTv = findViewById(R.id.info);
        sendBt = findViewById(R.id.send);
        openAndPushSwitch = findViewById(R.id.open_and_push_switch);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        sp = getSharedPreferences("settings", MODE_PRIVATE);
        host = sp.getString("host", "");
        sendBt.setOnClickListener(view -> sendClipboard());
        openAndPushSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("openAndPush", b);
            editor.apply();
        });
        ((TextView) findViewById(R.id.version)).setText("v" + getVersionName());
    }

    private void sendClipboard() {
        new Thread(() -> {
            String clipStr = getClipboardContent();
            if (!clipStr.equals("")) {
                if (sendSocket(host, clipStr)) {
                    handler.post(() -> Toast.makeText(MainActivity.this, "推送成功", Toast.LENGTH_SHORT).show());
                } else {
                    handler.post(() -> Toast.makeText(MainActivity.this, "推送失败", Toast.LENGTH_SHORT).show());
                }
            } else {
                handler.post(() -> Toast.makeText(MainActivity.this, "获取剪切板失败或剪切板为空", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public String getClipboardContent() {
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clipData = clipboard.getPrimaryClip();
            if (clipData != null && clipData.getItemCount() > 0) {
                ClipData.Item item = clipData.getItemAt(0);
                CharSequence text = item.getText();
                Log.i("TAG", "getClipboardContent: " + text);
                if (text != null) {
                    return text.toString();
                }
            }
        }
        return "";
    }

    @Override
    protected void onResume() {
        if (!host.equals("")) {
            infoTv.setText("电脑端：" + host + ":" + PORT);
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
            Toast.makeText(this, "请手动设置电脑IP", Toast.LENGTH_LONG).show();
        }


        super.onResume();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            boolean openAndPush = sp.getBoolean("openAndPush", false);
            if (openAndPush) {
                sendClipboard();
                openAndPushSwitch.setChecked(true);
            } else {
                openAndPushSwitch.setChecked(false);
            }
        }
        super.onWindowFocusChanged(hasFocus);
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


    public static String int2ip(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
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
            editText.setHint("请输入电脑端IP地址,如192.168.1.66");
            builder.setView(editText);
            builder.setPositiveButton("确定", (dialogInterface, i) -> {
                SharedPreferences.Editor editor = sp.edit();
                // TODO: 2022/5/5 校验是否为ip地址
                host = editText.getText().toString();
                if (!host.equals("")) {
                    editor.putString("host", host);
                    editor.apply();
                    infoTv.setText("电脑端：" + host + ":" + PORT);
                } else {
                    showToast("IP地址为空，请重新输入");
                }
            });
            builder.setNegativeButton("取消", (dialogInterface, i) -> {
                dialogInterface.dismiss();
            });
            builder.create().show();
        } else if (item.getItemId() == R.id.guide) {
            openUrl("https://sibtools.app/lemon_push/docs/intro");
        } else if (item.getItemId() == R.id.sib_tools) {
            openUrl("https://sibtools.app");
        } else if (item.getItemId() == R.id.version) {
            openUrl("https://sibtools.app/lemon_push/docs/version");
        }
        return super.onOptionsItemSelected(item);
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void openUrl(String url) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        startActivity(intent);
    }

    private String getVersionName() {
        try {
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
            // 在这里可以使用 versionName 对应的值
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "v1.0.1";
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
