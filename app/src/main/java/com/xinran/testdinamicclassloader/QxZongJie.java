package com.xinran.testdinamicclassloader;

/**
 * Created by qixinh on 16/5/12.
 */
public class QxZongJie {
    //DynamicLoadApk插件框架里面有例子Sample:https://github.com/XinRan5312/QxClassLoaderDynamic
    /**
     * 总体思想：从SDK级别上实现插件化，自定义DexClassLoader和Resourse把插件dex加载进来，并且记录，然后利用代理启动插件类
     * DLPluginManager:负责加载dex 启动插件类的入口
     * HashMap<String, DLPluginPackage> mPackagesHolder = new HashMap<String, DLPluginPackage>():
     * DexClassLoader:每个dex对应一个DLPluginPackage并且每个DLPluginPackage对应一个DexClassLoader，达到
     * 加载各个插件类的时候不能加载错误
     *
     * DLProxyActivity和DLProxyImpl:This is a plugin activity proxy, the proxy will create the plugin activity
     * with reflect, and then call the plugin activity's attach、onCreate method, at
     * this time, the plugin activity is running.
     */
}
