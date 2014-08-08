package com.smu.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

public class Update extends CordovaPlugin {
	
	private final static String UPDATE = "update";
	
	
	//标题
	private String AppName = "";
    private final static int DOWNLOAD_COMPLETE = 0;  
    private final static int DOWNLOAD_FAIL = 1;   
    //文件路径
    private File updateDir = null;  
    private File updateFile = null;  
       
    //ͨ通知栏控制
    private NotificationManager updateNotificationManager = null;  
    private Notification updateNotification = null;  
    //ͨ通知栏用Intent
    private PendingIntent updatePendingIntent = null; 
    
    private String url = null;

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		// TODO Auto-generated method stub
		if (UPDATE.equals(action)) {
			return doUpdate(args.getString(0), callbackContext);
		}
		return false;
	}

	private boolean doUpdate(String url, CallbackContext callbackContext) {
		
		this.url = url;
		int titleId = cordova.getActivity().getApplicationInfo().labelRes;
		AppName = cordova.getActivity().getResources().getString(titleId);
		if(android.os.Environment.MEDIA_MOUNTED.equals(android.os.Environment.getExternalStorageState())){  
	        updateDir = new File(Environment.getExternalStorageDirectory(),"app/download/");  
	         updateFile = new File(updateDir.getPath(),AppName+".apk");  
	    }
		 
		this.updateNotificationManager = (NotificationManager)cordova.getActivity().getSystemService(cordova.getActivity().NOTIFICATION_SERVICE);  
	    this.updateNotification = new Notification();
	     
	    updateNotification.icon = cordova.getActivity().getApplicationInfo().icon;;
        updateNotification.tickerText = "开始下载";
        updateNotification.setLatestEventInfo(cordova.getActivity(),AppName,"0%",updatePendingIntent);  
        //发送通知
        updateNotificationManager.notify(0,updateNotification);
        new Thread(new updateRunnable()).start();//启动线程下载
        callbackContext.success();
        return true;		
	}
	
	

	private Handler updateHandler = new  Handler(){
        @Override  
        public void handleMessage(Message msg) {
             switch(msg.what){  
                case DOWNLOAD_COMPLETE:
                    updateNotification.flags|=updateNotification.FLAG_AUTO_CANCEL;  
                    //点击安装 
                    Uri uri = Uri.fromFile(updateFile);  
                    Intent installIntent = new Intent(Intent.ACTION_VIEW);  
                    installIntent.setDataAndType(uri, "application/vnd.android.package-archive");  
                    updatePendingIntent = PendingIntent.getActivity(cordova.getActivity(), 0, installIntent, 0);  
                       
                    updateNotification.defaults = Notification.DEFAULT_SOUND;//声音通知   
                    updateNotification.setLatestEventInfo(cordova.getActivity(), AppName, "下载完成,点击安装", updatePendingIntent);  
                    updateNotificationManager.notify(0, updateNotification);  
                    break;
                case DOWNLOAD_FAIL:  
                    //下载失败
                    updateNotification.setLatestEventInfo(cordova.getActivity(), AppName, "下载失败", updatePendingIntent);  
                    updateNotificationManager.notify(0, updateNotification);  
                    break;
            }    
        }  
    };
	
    class updateRunnable implements Runnable {
        Message message = updateHandler.obtainMessage();  
        public void run() {  
            message.what = DOWNLOAD_COMPLETE;  
            try{  
                //SD卡权限<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE">;  
                if(!updateDir.exists()){  
                    updateDir.mkdirs();  
                }  
                if(!updateFile.exists()){  
                    updateFile.createNewFile();  
                }  
                //下载方法
                //网络权限<uses-permission android:name="android.permission.INTERNET">;  
                long downloadSize = downloadUpdateFile(url, updateFile);
                if(downloadSize>0){
                    //下载成功
                    updateHandler.sendMessage(message);  
                }  
            }catch(Exception ex){
                ex.printStackTrace();  
                message.what = DOWNLOAD_FAIL;  
                //下载失败
                updateHandler.sendMessage(message);  
            }
        }  
    }  
    

    /**
     * 下载方法
     * @param downloadUrl
     * @param saveFile
     * @return
     * @throws Exception
     */
    public long downloadUpdateFile(String downloadUrl, File saveFile) throws Exception {  
    
        int currentSize = 0;  
        long totalSize = 0;  
        int updateTotalSize = 0;  
        
        HttpURLConnection httpConnection = null;  
        InputStream is = null;  
        FileOutputStream fos = null;  
           
        try {
            URL url = new URL(downloadUrl);  
            httpConnection = (HttpURLConnection)url.openConnection();  
            httpConnection.setRequestProperty("User-Agent", "PacificHttpClient");
            if(currentSize > 0) {  
                httpConnection.setRequestProperty("RANGE", "bytes=" + currentSize + "-");  
            }  
            httpConnection.setConnectTimeout(10000);
            httpConnection.setReadTimeout(20000);  
            updateTotalSize = httpConnection.getContentLength();  
            if (httpConnection.getResponseCode() == 404) {
                throw new Exception("fail!");  
            }  
            is = httpConnection.getInputStream();                     
            fos = new FileOutputStream(saveFile, false);  
            byte buffer[] = new byte[4096];  
            int readsize = 0;
            int a = 0;
            while((readsize = is.read(buffer)) > 0){
                fos.write(buffer, 0, readsize);  
                totalSize += readsize;

                int persent = (int)totalSize*100/updateTotalSize;
                a++;
                if(persent<=100 && a % 10 == 0){
                    updateNotification.setLatestEventInfo(cordova.getActivity(), AppName+"正在下载", persent+"%", updatePendingIntent);  
                    updateNotificationManager.notify(0, updateNotification);  
                }                          
            }  
        } finally {  
            if(httpConnection != null) {  
                httpConnection.disconnect();
            }  
            if(is != null) {
                is.close();  
            }  
            if(fos != null) {  
                fos.close();  
            }  
        }  
        return totalSize;
    }  
}
