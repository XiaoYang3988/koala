package com.example.koala;

import static java.lang.Runtime.getRuntime;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public final String TAG = MainActivity.class.getName();
    public final String tcpdump_url = "https://www.androidtcpdump.com/download/4.99.1.1.10.1/tcpdump";
    public final String tcpdump_dir = "/tcpdump_dir";
    public final String sdcard_path = Environment.getExternalStorageDirectory().getAbsolutePath();
    private Button start_button;
    private Button stop_button;
    private EditText text_view;
    private EditText res_view;
    private boolean command_state = false;
    private boolean thread_switch = true;
    private String res = "";
    private String res_1 = "";
    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage (Message msg) {
            switch (msg.what) {
                case 1:
                    mService.conmmand.run("cp " + sdcard_path + tcpdump_dir + "/tcpdump" + " /data/local/tmp", -1);
                    mService.conmmand.run("chmod -R 777 /data/local/tmp/tcpdump", -1);
                    mService.conmmand.run("ln -s /data/local/tmp/tcpdump /usr/bin", -1);
                    break;
                case 2:
                    start_button.setEnabled(true);
                    stop_button.setEnabled(false);
                    text_view.setEnabled(true);
                    break;
                case 3:
                    start_button.setEnabled(false);
                    stop_button.setEnabled(true);
                    text_view.setEnabled(false);
                    break;
                case 4:
                    res_view.setText(res);
                    res_view.setSelection(res.length());
                    break;
            }
        }
    };
    // 0 失败 1正在下载 2下载成功
    private int download_state;
    private final int SUCCEED = 1;
    private TcpDumpService mService;
    //绑定Service监听
    private ServiceConnection sconnection = new ServiceConnection() {
        /** onServiceDisconnected只会在Service丢失时才会调用 */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //Log.i(TAG, "已断开Service");
        }
        /**当绑定时执行 */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //Log.i(TAG, "已绑定到Service");
            mService = ((TcpDumpService.TcpDumpBinder)service).getService();
            Intent i = new Intent();
            mService.setCallback(new TcpDumpService.mCallback() {
                @Override
                public void getRunningState(boolean state) {
                    //Log.d("状态", String.valueOf(state));
                    command_state = state;
                }
                public void getRunningResult(String str) {
                    //Log.d("结果", str);
                    if(str.length() > res.length()) {
                        res = str;
                    }
                }
            });
            mService.onStartCommand(i, 0, 0);        //绑定成功后可以调用Service方法，此处等与是启动Service
        }
    };

    /**
     * 获取手机内部可用空间大小
     *
     * @return 大小，字节为单位
     */
    public long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        //获取可用区块数量
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }
    public boolean isRoot(){
        try {
            getRuntime().exec("su");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public void http_download(String url, String path) {
        DownloadUtil.get().download(url, path, new DownloadUtil.OnDownloadListener() {
            @Override
            public void onDownloadSuccess() {
                //Log.d(TAG, "下载完成");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Log.d(TAG, "accomplish");
                                download_state = 2;
                                Toast.makeText(MainActivity.this, "tcpdump download accomplish", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
            @Override
            public void onDownloading(int progress) {
                Log.d(TAG, "正在下载");
                download_state = 1;
            }
            @Override
            public void onDownloadFailed() {
                //Log.d(TAG, "下载失败");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Log.d(TAG, "fail");
                                download_state = 0;
                                Toast.makeText(MainActivity.this, "tcpdump download fail", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
    }
    public void permission_open() {
        //Log.d(TAG, "open");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, SUCCEED);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED)
                requestPermissions(new String[]{Manifest.permission.INTERNET}, SUCCEED);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(intent);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            finish();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED)
            finish();
        if (isRoot() == false) {
            Toast.makeText(MainActivity.this, "sorry not root", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (getAvailableInternalMemorySize() <= 1024 * 1024 * 512) {
            Toast.makeText(MainActivity.this, "Sorry you don't have enough storage space, storage minimum 512m", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    private boolean fileIsExists(String filePath) {
        try {
            File f = new File(filePath);
            if(!f.exists())
                return false;
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }
    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        permission_open();
        start_button = findViewById(R.id.start_button);
        stop_button = findViewById(R.id.stop_button);
        start_button.setEnabled(true);
        stop_button.setEnabled(false);
        text_view = findViewById(R.id.enter);
        res_view = findViewById(R.id.txtOne);
        Intent bind = new Intent(MainActivity.this, TcpDumpService.class);
        bindService(bind, sconnection, Context.BIND_AUTO_CREATE);
        start_button.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                if(fileIsExists("/data/local/tmp/tcpdump") == false) {
                    Toast.makeText(MainActivity.this, "tcpdump file does not exist", Toast.LENGTH_SHORT).show();
                    return;
                }
                mService.conmmand.run(text_view.getText().toString(), -1);
            }
        });
        stop_button.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                if(fileIsExists("/data/local/tmp/tcpdump") == false) {
                    Toast.makeText(MainActivity.this, "tcpdump file does not exist", Toast.LENGTH_SHORT).show();
                    return;
                }
                mService.conmmand.run("\003", -1);
            }
        });
        if(fileIsExists("/data/local/tmp/tcpdump") == false) {
            new Thread() {
                @Override
                public void run() {
                    int fail_num = 0;
                    while(fail_num < 3) {
                        http_download(tcpdump_url, tcpdump_dir);
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        while(true){
                            if(download_state == 0) {
                                fail_num += 1;
                                break;
                            }
                            else if(download_state == 1) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            else if(download_state == 2) {
                                Message msg = Message.obtain();
                                msg.what = 1;
                                handler.sendMessage(msg);
                                return;
                            }
                        }
                        finish();
                    }
                }
            }.start();
        }
        new Thread() {
            @Override
            public void run() {
                while(thread_switch == true) {
                    if(command_state == false) {
                        Message msg = Message.obtain();
                        msg.what = 2;
                        handler.sendMessage(msg);
                    }
                    else {
                        Message msg = Message.obtain();
                        msg.what = 3;
                        handler.sendMessage(msg);
                    }
                    if(res.length() > res_1.length()) {
                        res_1 = res;
                        Message msg = Message.obtain();
                        msg.what = 4;
                        handler.sendMessage(msg);
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    @Override
    public void onStart() {
        super.onStart();
        //Log.d(TAG,"++ ON START ++");
    }

    @Override
    public void onResume() {
        super.onResume();
        //Log.d(TAG,"+ ON RESUME +");
    }

    @Override
    public void onPause() {
        super.onPause();
        //Log.d(TAG,"- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        //Log.d(TAG,"-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        thread_switch = false;
        unbindService(sconnection);
        //Log.d(TAG,"- ON DESTROY -");
    }

}
