package com.example.koala;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

public class TcpDumpService extends Service {
    /** 类名 */
    private final String TAG = TcpDumpService.class.getName();
    /** 绑定的客户端接口 */
    private final IBinder binder = new TcpDumpBinder();
    /** 标识服务如果被杀死之后的行为 */
    private int mStartMode = 0;
    /** 标识是否可以使用onRebind */
    private boolean mAllowRebind;
    public ExeCommand conmmand = new ExeCommand(false);
    private mCallback callback;
    private Timer mTimer = new Timer();

    public class TcpDumpBinder extends Binder {
        public TcpDumpService getService() {
            return TcpDumpService.this;    //返回本服务
        }
    }

    TimerTask task = new TimerTask(){
        @Override
        public void run() {
            if(callback!=null){
                callback.getRunningState(conmmand.isRunning());
                callback.getRunningResult(conmmand.getResult());
                //callback.getRunningState(false);
            }
        }
    };

    public static interface mCallback {
        void getRunningState(boolean state);
        void getRunningResult(String result);
    }

    public void setCallback(mCallback callback) {
        this.callback = callback;
    }

    /** 当服务被创建时调用. */
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        startForeground();
        mTimer.schedule(task, 0, 1000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        /*
        @SuppressLint("RemoteViewLayout")
        RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.layout);
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext()).setContent(remoteViews);
        builder.setWhen(System.currentTimeMillis()).setSmallIcon(R.mipmap.ic_launcher);
        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_SOUND;
        */
        //startForeground(1, notification);
        return super.onStartCommand(intent, flags, startId);
    }

    private void startForeground() {
        String channelId = null;
        // 8.0 以上需要特殊处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel("tcpdump_command", "TcpDumpService");
        } else {
            channelId = "";
        }
        RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.layout);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId).setContent(remoteViews);
        builder.setWhen(System.currentTimeMillis()).setSmallIcon(R.mipmap.ic_launcher);
        Notification notification = builder.build();
        startForeground(1, notification);
    }

    /**
     * 创建通知通道
     * @param channelId
     * @param channelName
     * @return
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName){
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;
    }

    /** 通过bindService()绑定到服务的客户端 */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return binder;
    }

    /** 通过unbindService()解除所有客户端绑定时调用 */
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        return mAllowRebind;
    }

    /** 通过bindService()将客户端绑定到服务时调用*/
    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind");
    }

    /** 断开绑定或者停止服务时执行 */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
        stopForeground(true);
        Log.d(TAG, "onDestroy");
        Toast.makeText(this, "服务已经停止", Toast.LENGTH_LONG).show();
    }

    /** 当内存不够是执行该方法 */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.i(TAG, "onLowMemory()");
        onDestroy();
    }
}
