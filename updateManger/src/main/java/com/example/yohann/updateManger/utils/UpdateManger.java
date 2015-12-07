package com.example.yohann.getnews.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.yohann.getnews.R;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Yohann on 2015/12/7.
 */
public class UpdateManger {


    private Context mContext;
    private String URL = "http://111.0.228.16:8080/news/getVersion.php";
    private String version_name;
    private String version_code;
    private String version_desc;
    private String version_url;
    private String savePath; //保存路径
    private String packageName; //Manifest 包名
    private int count;  //计算当前下载数
    private int progress; //计算当前进度
    private int downLoadflag = 1; //判断下载状态
    private static final int DOWNLOADING = 1;
    private static final int DOWNLOAD_OVER = 2;

    private ProgressBar progressBar;
    private AlertDialog alertDialog;
    private TextView textView;
    RequestQueue requestQueue ;

    /**
     *  在构造方法中 传递一个context，manifests中的包名(要在manifests中加入versionCode标签),和获json格式取版本信息的url
     *  version_name    新版本文件的名字 xxx.apk
     *  version_code    新版本的版本号（一个整型数字）
     *  version_desc    新版本的更新日志
     *  version_url     新版本的URL
     */
    public UpdateManger(Context context,String packageName,String versionInfoUrl){
        this.mContext = context;
        this.URL = versionInfoUrl;
        this.packageName = packageName;
    }

    public void checkUpdate(){


        JsonObjectRequest jsonObjectRequest  = new JsonObjectRequest(URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    version_code = jsonObject.getString("version_code");
                    version_name = jsonObject.getString("version_name");
                    version_desc = jsonObject.getString("version_desc");
                    version_url = jsonObject.getString("version_url");
                    System.out.println(version_code);
                    if (isUpdate()){
                        Toast.makeText(mContext, "需要版本更新！", Toast.LENGTH_SHORT).show();
                        String msg = "软件有更新，需要下载安装吗？\n\n"+"更新日志：\n\n"+version_desc;
                        new  AlertDialog.Builder(mContext)
                                .setMessage(msg)
                                .setTitle("更新提示")
                                .setPositiveButton("更新", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        showDownLoadAlert();
                                    }
                                })
                                .setNegativeButton("下次再说", null)
                                .show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(mContext,"检查版本发生错误！",Toast.LENGTH_SHORT).show();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    public boolean isUpdate(){
        boolean flag = false;
        try {
            int local_vesion = mContext.getPackageManager().getPackageInfo(packageName,0).versionCode;
            if (local_vesion<Integer.parseInt(version_code)){
                flag = true;
            }else {
                flag =  false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return flag;
    }
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case DOWNLOADING:
                    progressBar.setProgress(progress);
                    textView.setText(progress+"%");
                    break;
                case DOWNLOAD_OVER:
                    new AlertDialog.Builder(mContext)
                            .setTitle("提示")
                            .setMessage("下载完成是否安装?")
                            .setPositiveButton("安装", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    installAPK();


                                }
                            })
                            .setNegativeButton("取消",null)
                            .show();
                    alertDialog.dismiss();
                    break;
                case 0:
                    alertDialog.dismiss();
                    break;
            }
        }
    };
    public  void installAPK(){
        File apkfile = new File(savePath,version_name);
        if (!apkfile.exists()){
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri= Uri.parse("file://"+apkfile.toString());
        intent.setDataAndType(uri,"application/vnd.android.package-archive");
        mContext.startActivity(intent);
    }
    public void  showDownLoadAlert(){
        View view = LayoutInflater.from(mContext).inflate(R.layout.download_alert, null);
        progressBar = (ProgressBar) view.findViewById(R.id.proBar);
        textView = (TextView) view.findViewById(R.id.pst);
        alertDialog = new AlertDialog.Builder(mContext)
                .setTitle("下载中")
                .setView(view)
                .setPositiveButton("取消下载", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downLoadflag = 0;
                    }
                })
                .show();
        downloadAPK();
    }

    public void downloadAPK(){
        new Thread(){
            @Override
            public void run() {
                try {
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                        String sdpath = Environment.getExternalStorageDirectory() + "/";
                        savePath = sdpath+"News";
                        System.out.println(sdpath);
                        File dir = new File(savePath);
                        if (!dir.exists()) {
                            dir.mkdir();
                        }
                        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(version_url).openConnection();
                        httpURLConnection.connect();
                        InputStream is = httpURLConnection.getInputStream();
                        final  int flength =  httpURLConnection.getContentLength();
                        System.out.println("flength"+flength);
                        System.out.println(sdpath+version_name);
                        File apkfile = new File(savePath,version_name);
                        FileOutputStream fos = new FileOutputStream(apkfile);
                        byte[] buffer = new byte[1024];
                        while (true){
                            int num = is.read(buffer);
                            count +=num;
                            if (num<0){
                                handler.sendEmptyMessage(DOWNLOAD_OVER);
                                break;
                            }
                            if(downLoadflag==0){
                                //取消下载
                                apkfile.delete();
                                handler.sendEmptyMessage(0);
                                break;
                            }
                            progress =(int) (((float)count/flength)*100);
                            handler.sendEmptyMessage(DOWNLOADING);
                            fos.write(buffer,0,num);
                        }
                        fos.close();
                        is.close();
                    }else {
                        Toast.makeText(mContext,"路径不存在！",Toast.LENGTH_SHORT).show();
                    }


                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }.start();
    }
}
