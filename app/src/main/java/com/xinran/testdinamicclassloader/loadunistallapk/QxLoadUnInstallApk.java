package com.xinran.testdinamicclassloader.loadunistallapk;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Environment;

import com.xinran.testdinamicclassloader.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dalvik.system.DexClassLoader;

/**
 * * 每个插件apk的shareUserId一定要和host的相同
 * <manifest xmlns:android="http://schemas.android.com/apk/res/android"
 * package="com.xinran.testdinamicclassloader"
 * android:sharedUserId="com.wr.qx">
 *
 * 设计思路：
 通过PackageManager在指定的放插件apk的路径下得到所有插件信息（Package Appicationinfo等），
 然后使用自定义的DexClassLoader进行加载插件，还要在host的Resourse的基础创建包含插件res的新的Resource，
 以便使用插件res
 * Created by qixinh on 16/5/10.
 */
public class QxLoadUnInstallApk {
    private static Context mContext;
    private final static String SHARE_USER_ID = "com.wr.qx";

    private QxLoadUnInstallApk(Context context) {
        mContext = context;
    }

    public static class QxLoadUnInstallApkHelper {

        public static QxLoadUnInstallApk newInstace(Context context) {

            return new QxLoadUnInstallApk(context);

        }
    }

    /**
     * @return 返回未安装apk的路径
     */
    public String getApkDir() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getPath() + File.separator + "plugin";
        } else {
            return mContext.getCacheDir().getPath() + File.separator + "plugin";
        }
    }

    /**
     * @param apkName
     * @return 得到apkName对应apk的Resource
     */
    public Resources getApkResource(String apkName) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.invoke(addAssetPath, getApkDir() + File.separator + apkName);
            Resources hostRes = mContext.getResources();
            Resources apkRes = new Resources(assetManager, hostRes.getDisplayMetrics(), hostRes.getConfiguration());
            return apkRes;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获得协议确定的放在这个getApkDir()路径下所有未安装apk的重要信息比如appName packageName等
     *
     * @return
     */
    public List<HashMap<String, String>> getAllUnIstallApkInfo() {
        List<HashMap<String, String>> list = new ArrayList<>();
        File apkDir = new File(getApkDir());
        if (apkDir.isDirectory()) {
            FilenameFilter filenameFilter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(".apk");
                }
            };
            File[] apks = apkDir.listFiles(filenameFilter);//过滤掉不符合过滤条件的file
            for (File file : apks) {
                HashMap<String, String> fileInfo = getOneUnIstallApkInfo(file.getName());
                list.add(fileInfo);
            }
        }
        return list;
    }

    /**
     * 根据未安装APK的name获得其相关信息，package ApplicationInfo等，这一切都要借助PackageMananger的强大的功能
     *
     * @param name
     * @return
     */
    private HashMap<String, String> getOneUnIstallApkInfo(String name) {
        HashMap<String, String> fileInfo = new HashMap<>();
        PackageManager pm = mContext.getPackageManager();
        PackageInfo packageInfo = pm.getPackageArchiveInfo(getApkDir() + File.separator + name,
                PackageManager.GET_ACTIVITIES);
        if (packageInfo != null) {
            ApplicationInfo appinfo = packageInfo.applicationInfo;
            String viersion = packageInfo.versionName;
            ActivityInfo[] activitysInfo = packageInfo.activities;
            String apkName = pm.getApplicationLabel(appinfo).toString();
            Drawable apkIcon = pm.getApplicationIcon(appinfo);
            String packageName = packageInfo.packageName;
            fileInfo.put("apkName", apkName);
            fileInfo.put("packageName", packageName);
        }
        return fileInfo;
    }

    /**
     * 根据转入的apkName和appPackageName 通过反射获得插件apk相关信息尤其是Resoure资源和Class然后得到插件的资源
     * 给宿主用，本例只是简单的通过反射获得drawable中名字为wr的资源id，然后获得它的drawable，本例虽然简单但是五脏俱全
     *
     * @param apkName
     * @param apkPackageName
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public Drawable dynamicLoadApk(String apkName, String apkPackageName) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        File optimizeDirFile = mContext.getDir("dex", Context.MODE_PRIVATE);//解压优化后插件apk存放地点
        DexClassLoader dexClassLoader = new DexClassLoader(getApkDir() + File.separator + apkName,
                optimizeDirFile.getPath(), null, ClassLoader.getSystemClassLoader());
        //通过使用apk自己的类加载器，反射出R类中相应的内部类进而获取我们需要的资源id
        /**
         * R类的内部类有好多，常见的对应资源都是独立的R的内部类，而且类名都是小写
         * 大家可以进入APP的R类查看，比如mipmap boole integer drawable layout menu demin color string stytle等
         */
        Class<?> cls = dexClassLoader.loadClass(apkPackageName + ".R$mipmap");
        //参数：1、类的全名，2、是否初始化类，3、加载时使用的类加载器
//        Class<?> clazz = Class.forName(apkPackageName + ".R$mipmap", true, dexClassLoader);
        Field field = cls.getDeclaredField("wr");//获取指定属性名字的field
        int resId = field.getInt(R.id.class);//得到field的value   即resid
        Resources apkRes = getApkResource(apkName);//获取插件apk的Resource 然后才能操作它的资源
        return apkRes.getDrawable(resId);


    }

    /**
     * //拷贝apk文件至sd卡plugin目录下保证指定路径下有插件资源，模拟从网络下载后保存到sd卡后的情景
     * @param apkName
     */

    public void copyApkFileToSD(String apkName) {
        File file = new File(getApkDir());
        if (!file.exists()) {
            file.mkdirs();
        }
        File apk = new File(getApkDir() + File.separator + apkName);
        try {
            if(apk.exists()){
                return;
            }
            FileOutputStream fos = new FileOutputStream(apk);
            InputStream is = mContext.getResources().getAssets().open(apkName);
            BufferedInputStream bis = new BufferedInputStream(is);
            int len = -1;
            byte[] by = new byte[1024];
            while ((len = bis.read(by)) != -1) {
                fos.write(by, 0, len);
                fos.flush();
            }
            fos.close();
            is.close();
            bis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
