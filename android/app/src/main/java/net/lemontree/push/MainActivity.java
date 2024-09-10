package net.lemontree.push;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.xuexiang.xqrcode.XQRCode;

import net.lemontree.push.model.PCClient;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.Socket;
import java.net.URLEncoder;
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
    private Button autoOperationBtn;
    private SharedPreferences sp;
    private SharedPreferences pcListSp;
    private ClipboardManager clipboard;
    private List<PCClient> pcClientList = new ArrayList<>();
    private Context context;
    private int selectPC = 0;
    private int autoOperation = 0; // 0: 无操作；1:打开即推送；2：打开即获取；3：悬浮窗点击推送；4：悬浮窗点击获取；
    private String[] autoOperationItems;

    private final int PERMISSION_REQUEST_CAMERA = 104;
    private final int REQUEST_CODE = 105;
    private static final int REQUEST_CODE_DRAW_OVER_OTHER_APPS_WITH_AUTOOPERATION_3 = 106;
    private static final int REQUEST_CODE_DRAW_OVER_OTHER_APPS_WITH_AUTOOPERATION_4 = 107;


    private final String LAST_PC_KEY = "lastPC";

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
        autoOperationBtn = findViewById(R.id.auto_operation_button);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        sp = getSharedPreferences("settings", MODE_PRIVATE);
        pcListSp = getSharedPreferences("PCList", MODE_PRIVATE);
        sendBt.setOnClickListener(view -> sendClipboard(getClipboardContent()));

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

        autoOperationItems = getResources().getStringArray(R.array.auto_operation_array);
        autoOperationBtn.setText(autoOperationItems[0]);
        autoOperationBtn.setOnClickListener(view->{
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
            alertBuilder.setTitle("请选择自动操作");
            alertBuilder.setSingleChoiceItems(autoOperationItems, autoOperation, (dialogInterface, which) -> {
                chooseAutoOperationMode(which);
                dialogInterface.dismiss();
            });
            alertBuilder.create().show();
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
    private void chooseAutoOperationMode(int which){
        removeFloatWindow();
        int prevMode = autoOperation;
        autoOperation = which;
        autoOperationBtn.setText(autoOperationItems[which]);
        if(which == 3 || which == 4){
            if(!chooseFloatWindow(which)){
                chooseAutoOperationMode(prevMode);
            }
        }
    }
    private boolean chooseFloatWindow(int which){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // 如果需要SYSTEM_ALERT_WINDOW权限但尚未授予，则引导用户到设置

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("需要特殊权限")
                .setMessage("为了使用此功能，请允许应用在其他应用上显示")
                .setPositiveButton("去设置", (dialog, whichone) -> {
                    // 跳转到设置页面
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, which==3?REQUEST_CODE_DRAW_OVER_OTHER_APPS_WITH_AUTOOPERATION_3:REQUEST_CODE_DRAW_OVER_OTHER_APPS_WITH_AUTOOPERATION_4);
                })
                .setNegativeButton("取消", null)
                .show();
            return false;
        } else {
            // 权限已存在，直接创建悬浮窗
            createFloatWindow();
        }
        return true;
    }
    private float floatwindow_initialX;
    private float floatwindow_initialY;
    private float floatwindow_initialTouchX;
    private float floatwindow_initialTouchY;
    private View floatWindow;
    private WindowManager windowManager;
    private boolean floatwindow_isDragging = false;
    private static final float CLICK_DRAG_TOLERANCE = 10; // 点击时允许的拖动距离
    private void createFloatWindow() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        params.x = 50;
        params.y = 100;

        LayoutInflater inflater = LayoutInflater.from(this);
        floatWindow = inflater.inflate(R.layout.float_window_layout, null);
        TextView tv = floatWindow.findViewById(R.id.float_tv);
        tv.setText(autoOperation==3?"推送剪切板":"获取剪切板");

        if(windowManager == null){
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        windowManager.addView(floatWindow, params);

        // 设置触摸监听以拖动悬浮窗
        floatWindow.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    floatwindow_initialX = params.x;
                    floatwindow_initialY = params.y;
                    floatwindow_initialTouchX = event.getRawX();
                    floatwindow_initialTouchY = event.getRawY();
                    floatwindow_isDragging = false;
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
                    windowManager.updateViewLayout(floatWindow, params);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - floatwindow_initialTouchX;
                    float dy = event.getRawY() - floatwindow_initialTouchY;
                    if (Math.abs(dx) > CLICK_DRAG_TOLERANCE || Math.abs(dy) > CLICK_DRAG_TOLERANCE) {
                        floatwindow_isDragging = true;
                    }
                    params.x = (int)(floatwindow_initialX - dx);
                    params.y = (int)(floatwindow_initialY - dy);
                    windowManager.updateViewLayout(floatWindow, params);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!floatwindow_isDragging) {
                        // 没有拖动，视为点击
                        if(autoOperation == 3){
                            // 推送
                            sendClipboard(getClipboardContent());
                        } else if(autoOperation == 4) {
                            // 获取
                            tryGetPCClipboard();
                        }
                    }
                    floatwindow_isDragging = false; // 重置拖动状态
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    windowManager.updateViewLayout(floatWindow, params);
                    return true;
            }
            return false;
        });
    }
    private void removeFloatWindow(){
        if(floatWindow != null && windowManager != null){
            windowManager.removeView(floatWindow);
            floatWindow = null;
        }
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
        if (json != null && !json.equals("")) {
            List<PCClient> list = gson.fromJson(json, listType);
            if (list.size() > 0) {
                pcClientList.clear();
                pcClientList.addAll(list);
                selectPC = pcListSp.getInt(LAST_PC_KEY, 0);
                infoTv.setText("电脑端：" + pcClientList.get(selectPC).getIp() + ":" + pcClientList.get(selectPC).getPort());
            }
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
            Toast.makeText(this, "请设置电脑IP和端口", Toast.LENGTH_LONG).show();
        }
        super.onResume();
    }

    private void tryGetPCClipboard(){
        if(pcClientList.size()<=0){
            Toast.makeText(MainActivity.this, "请先置电脑IP和端口", Toast.LENGTH_LONG).show();
            return;
        }
        getPCClipboard(pcClientList.get(selectPC));
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
                runOnUiThread(() -> Toast.makeText(context, "获取失败：电脑未启动程序或IP地址错误", Toast.LENGTH_SHORT).show());
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
            if(autoOperation==1){
                // openAndPush
                sendClipboard(getClipboardContent());
            }else if(autoOperation == 2){
                // openAndGet
                tryGetPCClipboard();
            }
        }
        super.onWindowFocusChanged(hasFocus);
    }

    public static boolean sendToPC(String url, String content) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url + "?text=" + URLEncoder.encode(content, "UTF-8"))
                    .build();
            Call call = client.newCall(request);
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        /**
         * 处理二维码扫描结果
         */
        //处理二维码扫描结果
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            //处理扫描结果（在界面上显示）
            handleScanResult(data);
        }

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_DRAW_OVER_OTHER_APPS_WITH_AUTOOPERATION_3) {
            chooseAutoOperationMode(3);
        }else if(requestCode == REQUEST_CODE_DRAW_OVER_OTHER_APPS_WITH_AUTOOPERATION_4) {
            chooseAutoOperationMode(4);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                XQRCode.startScan(this, REQUEST_CODE);
            }
        } else {
            Toast.makeText(context, "未授权相机权限", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 处理二维码扫描结果
     *
     * @param data
     */
    private void handleScanResult(Intent data) {
        if (data != null) {
            Bundle bundle = data.getExtras();
            if (bundle != null) {
                if (bundle.getInt(XQRCode.RESULT_TYPE) == XQRCode.RESULT_SUCCESS) {
                    String result = bundle.getString(XQRCode.RESULT_DATA);
                    Toast.makeText(this, "扫码二维码成功", Toast.LENGTH_LONG).show();
                    if (result != null) {
                        String[] str = result.split(":");
                        String ip = str[0];
                        String port = str[1];
                        PCClient pcClient = new PCClient(ip, ip, port);
                        if (checkSameIPAndPort(pcClient) != -1) {
                            selectPC = checkSameIPAndPort(pcClient);
                            Toast.makeText(this, "切换电脑成功", Toast.LENGTH_LONG).show();
                        } else {
                            pcClientList.add(pcClient);
                            Gson gson = new Gson();
                            pcListSp.edit().putString("PCList", gson.toJson(pcClientList)).apply();
                            selectPC = pcClientList.size() - 1;
                        }
                        pcListSp.edit().putInt(LAST_PC_KEY, selectPC).apply();
                        infoTv.setText("电脑端：" + pcClientList.get(selectPC).getIp() + ":" + pcClientList.get(selectPC).getPort());

                    }
                } else if (bundle.getInt(XQRCode.RESULT_TYPE) == XQRCode.RESULT_FAILED) {
                    Toast.makeText(this, "解析二维码失败", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private int checkSameIPAndPort(PCClient pcClient) {
        if (pcClientList.size() > 0) {
            for (int i = 0; i < pcClientList.size(); i++) {
                if (pcClientList.get(i).getIp().equals(pcClient.getIp()) && pcClientList.get(i).getPort().equals(pcClient.getPort())) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.set) {
            startActivity(new Intent(this, PCConfigActivity.class));
        } else if (item.getItemId() == R.id.guide) {
            openUrl("https://sibtools.app/lemon_push/docs/intro");
        } else if (item.getItemId() == R.id.sib_tools) {
            openUrl("https://sibtools.app");
        } else if (item.getItemId() == R.id.version) {
            openUrl("https://sibtools.app/lemon_push/docs/version");
        } else if (item.getItemId() == R.id.scan) {
            requestCameraPermission();
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA);
        } else {
            XQRCode.startScan(this, REQUEST_CODE);
        }
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
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "v1.0.1";
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        removeFloatWindow();
    }
}
