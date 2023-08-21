package net.lemontree.push;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import net.lemontree.push.model.PCClient;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import jackmego.com.jieba_android.JiebaSegmenter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private Button sendBt;
    private Handler handler = new Handler();
    private Button infoTv;
    private SwitchCompat openAndPushSwitch;
    private SharedPreferences sp;
    private SharedPreferences pcListSp;
    private ClipboardManager clipboard;
    private List<PCClient> pcClientList = new ArrayList<>();
    private Context context;
    private int selectPC = 0;

    private final String LAST_PC_KEY = "lastPC";
    private final String TAG = "MainAc";

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        // 异步初始化
        JiebaSegmenter.init(getApplicationContext());

        infoTv = findViewById(R.id.info);
        sendBt = findViewById(R.id.send);
        openAndPushSwitch = findViewById(R.id.open_and_push_switch);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        sp = getSharedPreferences("settings", MODE_PRIVATE);
        pcListSp = getSharedPreferences("PCList", MODE_PRIVATE);
        sendBt.setOnClickListener(view -> sendClipboard(getClipboardContent()));
        openAndPushSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("openAndPush", b);
            editor.apply();
        });
        ((TextView) findViewById(R.id.version)).setText("v" + getVersionName());
        infoTv.setOnClickListener(view -> {
            if (pcClientList.size() > 0) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                String[] names = pcClientList.stream()
                        .map(pcClient -> pcClient.getIp() + ":" + pcClient.getPort())
                        .toArray(String[]::new);
                alertBuilder.setTitle("电脑选择");
                alertBuilder.setSingleChoiceItems(names, selectPC, (dialogInterface, i) -> {
                    selectPC = i;
                    SharedPreferences.Editor editor = pcListSp.edit();
                    editor.putInt(LAST_PC_KEY, selectPC);
                    editor.apply();
                    infoTv.setText("电脑端：" + pcClientList.get(selectPC).getIp() + ":" + pcClientList.get(selectPC).getPort());
                    dialogInterface.dismiss();
                });
                alertBuilder.create().show();
            }

        });
        findViewById(R.id.get).setOnClickListener(view -> getPCClipboard(pcClientList.get(selectPC)));
        findViewById(R.id.split).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<String> wordList = JiebaSegmenter.getJiebaSegmenterSingleton().getDividedString(getClipboardContent());
                CustomBottomSheetDialog customBottomSheetDialog = new CustomBottomSheetDialog(MainActivity.this, wordList, new DivideCard.OperationListener() {
                    @Override
                    public void pushCopy(String content) {
                        sendClipboard(content);
                    }
                });
                customBottomSheetDialog.show();
            }
        });
    }


    public void sendClipboard(String text) {
        new Thread(() -> {
            if (!text.equals("")) {
                if (sendToPC(toUrl(pcClientList.get(selectPC)) + "/set_clipboard", text)) {
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
                if (text != null) {
                    return text.toString();
                }
            }
        }
        return "";
    }

    @Override
    protected void onResume() {
        Intent intent = getIntent();
        String action = intent.getAction();
        Gson gson = new Gson();
        Type listType = new TypeToken<List<PCClient>>() {
        }.getType();
        String json = pcListSp.getString("PCList", "");
        if (!json.equals("")) {
            List<PCClient> list = gson.fromJson(json, listType);
            pcClientList.clear();
            pcClientList.addAll(list);
            selectPC = pcListSp.getInt(LAST_PC_KEY, 0);
            infoTv.setText("电脑端：" + pcClientList.get(selectPC).getIp() + ":" + pcClientList.get(selectPC).getPort());
        }
        if (pcClientList.size() > 0) {
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
                    if (sendToPC(toUrl(pcClientList.get(selectPC)) + "/get_clipboard", finalShare)) {
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

    private void getPCClipboard(PCClient pcClient) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(toUrl(pcClient) + "/get_clipboard")
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    String json = response.body().string();
                    JsonParser parser = new JsonParser();
                    JsonElement jsonElement = parser.parse(json);
                    ClipData clip = ClipData.newPlainText("label", jsonElement.getAsJsonObject().get("data").getAsString());
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(clip);
                    runOnUiThread(() -> Toast.makeText(context, "已经获取PC剪切板内容", Toast.LENGTH_SHORT).show());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    private String toUrl(PCClient pcClient) {
        return "http://" + pcClient.getIp() + ":" + pcClient.getPort();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            boolean openAndPush = sp.getBoolean("openAndPush", false);
            if (openAndPush) {
                sendClipboard(getClipboardContent());
                openAndPushSwitch.setChecked(true);
            } else {
                openAndPushSwitch.setChecked(false);
            }
        }
        super.onWindowFocusChanged(hasFocus);
    }

    public static boolean sendToPC(String url, String content) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url + "?text=" + content)
                .build();
        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                return true;
            }
        } catch (IOException exception) {
            exception.printStackTrace();
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
            /*AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
            builder.create().show();*/

            startActivity(new Intent(this, PCConfigActivity.class));

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
