package com.xinran.testdinamicclassloader.droidplugin360;

import android.os.RemoteException;

import com.morgoo.droidplugin.pm.PluginManager;

/**
 * 启动插件：启动插件的Activity、Service等都和你启动一个以及安装在系统中的app一样，
 * 使用系统提供的相关API即可。组件间通讯也是如此。
 *
 * Created by qixinh on 16/5/12.
 */
public class QxDroidPluginUtils {
    /**
     * 安装一个插件，
     * pluginPath为插件apk路径，flag可以设置为0，如果要更新插件，
     *
     * 则设置为PackageManagerCompat.INSTALL_REPLACE_EXISTING
     *
     * 返回值及其含义请参见PackageManagerCompat类中的相关字段
     *
     * @param pluginPath
     * @param flag
     * @return
     * @throws RemoteException
     */
    public static int installPlugin(String pluginPath,int flag) throws RemoteException {
        return PluginManager.getInstance().installPackage(pluginPath,flag);
    }

    /**
     * 卸载一个插件：
     *
     * @param pluginPath
     * @param flag
     * @throws RemoteException
     */
    public static void unstallPlugin(String pluginPath,int flag) throws RemoteException {
      PluginManager.getInstance().deletePackage(pluginPath,flag);
    }
}
