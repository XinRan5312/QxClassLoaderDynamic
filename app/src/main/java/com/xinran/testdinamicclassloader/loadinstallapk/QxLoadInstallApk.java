package com.xinran.testdinamicclassloader.loadinstallapk;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.xinran.testdinamicclassloader.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.PathClassLoader;


/**
 * 每个插件apk的shareUserId一定要和host的相同
 * <manifest xmlns:android="http://schemas.android.com/apk/res/android"
 package="com.xinran.testdinamicclassloader"
 android:sharedUserId="com.wr.qx">
设计思路：
    通过PackageManager获得所有已经安装的apk插件，然后根据sharedUserId过滤出我们开发的插件，然后获得相应的Package等信息，
 然后使用自定义的pathClassLoader进行加载插件，还要在host的Resourse的基础创建包含插件res的新的Resource，
 以便使用插件res
 * Created by qixinh on 16/5/10.
 */
public class QxLoadInstallApk {
    private static Context mContext;
    private final static String SHARE_USER_ID = "com.wr.qx";

    private QxLoadInstallApk(Context context) {
        mContext = context;
    }

    public static class QxLoadInstallApkHelper {

        public static QxLoadInstallApk newInstace(Context context) {

            return new QxLoadInstallApk(context);

        }
    }

    public ArrayList<QxApkBean> findAllInstallApks() {
        ArrayList<QxApkBean> list = new ArrayList<>();
        PackageManager pm = mContext.getPackageManager();
        /**
         *  PackageManager.GET_UNINSTALLED_PACKAGES:
         * Flag parameter to retrieve some information about all applications (even
         * uninstalled ones) which have data directories.
         *
         */
        List<PackageInfo> packageInfos = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        if (packageInfos != null && !packageInfos.isEmpty()) {
            for (PackageInfo pi : packageInfos) {
                if (pi.sharedUserId != null && pi.sharedUserId.equals(SHARE_USER_ID) &&
                        pi.packageName.equals(mContext.getPackageName())) {

                    QxApkBean apk = new QxApkBean(pm.getApplicationLabel(pi.applicationInfo).toString(),
                            pi.packageName);

                    list.add(apk);
                }
            }
        }
        return list;
    }

    public Drawable dynamicLoadApkImgDrawable(String packageName, String feildName) throws PackageManager.NameNotFoundException, NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
        Context apkContext = mContext.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
        PathClassLoader pathClassLoader = new PathClassLoader(apkContext.getPackageResourcePath(), ClassLoader.getSystemClassLoader());
        //        Class<?> clazz = pathClassLoader.loadClass(packageName + ".R$mipmap");//通过使用自身的加载器反射出mipmap类进而使用该类的功能
        //参数：1、类的全名，2、是否初始化类，3、加载时使用的类加载器
        //通过使用apk自己的类加载器，反射出R类中相应的内部类进而获取我们需要的资源id
        /**
         * R类的内部类有好多，常见的对应资源都是独立的R的内部类，而且类名都是小写
         * 大家可以进入APP的R类查看，比如mipmap boole integer drawable layout menu demin color string stytle等
         */
        Class<?> clazz = Class.forName(packageName + ".R$mipmap", true, pathClassLoader);
        //使用上述两种方式都可以，这里我们得到R类中的内部类mipmap，通过它得到对应的图片id，进而给我们使用
        Field field = clazz.getDeclaredField(feildName);
        int resourceId = field.getInt(R.mipmap.class);
        return apkContext.getResources().getDrawable(resourceId);//获取插件apk的Resource 然后才能操作它的资源
    }

}
