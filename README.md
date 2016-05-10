# QxClassLoaderDynamic
通过java类加载机制的研究，更深入研究Andriod插件化的发展
一切为了插件化的研究
1、什么是动态加载技术？
动态加载技术就是使用类加载器加载相应的apk、dex、jar（必须含有dex文件），再通过反射获得该apk、dex、jar内部的资源（class、图片、color等等）进而供宿主app使用。
2、关于动态加载使用的类加载器
使用动态加载技术时，一般需要用到这两个类加载器：
PathClassLoader - 只能加载已经安装的apk，即/data/app目录下的apk。
DexClassLoader  - 能加载手机中未安装的apk、jar、dex，只要能在找到对应的路径。
这两个加载器分别对应使用的场景各不同，所以接下来，分别讲解它们各自加载相同的插件apk的使用。
3、使用PathClassLoader加载已安装的apk插件，获取相应的资源供宿主app使用
下面通过一个demo来介绍PathClassLoader的使用：
1、首先我们需要知道一个manifest中的属性：SharedUserId。

该属性是用来干嘛的呢？简单的说，应用从一开始安装在Android系统上时，系统都会给它分配一个linux user id，之
后该应用在今后都将运行在独立的一个进程中，其它应用程序不能访问它的资源，那么如果两个应用的sharedUserId相同，那么它们将共同运行在相同的linux进程中，从而便可以数据共享、资源访问了。所以我们在宿主app和插件app的manifest上都定义一个相同的sharedUserId。

2、那么我们将插件apk安装在手机上后，宿主app怎么知道手机内该插件是否是我们应用程序的插件呢？
我们之前是不是定义过插件apk也是使用相同的sharedUserId，那么，我就可以这样思考了，是不是可以得到手机内所有已安装apk的sharedUserId呢，然后通过判断sharedUserId是否和宿主app的相同，如果是，那么该app就是我们的插件app了。确实是这样的思路的，那么有了思路最大的问题就是怎么获取一个应用程序内的sharedUserId了，我们可以通过PackageInfo.sharedUserId来获取，请看代码：
[java] view plain copy print?
/** 
     * 查找手机内所有的插件 
     * @return 返回一个插件List 
     */  
    private List<PluginBean> findAllPlugin() {  
        List<PluginBean> plugins = new ArrayList<>();  
        PackageManager pm = getPackageManager();  
        //通过包管理器查找所有已安装的apk文件  
        List<PackageInfo> packageInfos = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);  
        for (PackageInfo info : packageInfos) {  
            //得到当前apk的包名  
            String pkgName = info.packageName;  
            //得到当前apk的sharedUserId  
            String shareUesrId = info.sharedUserId;  
            //判断这个apk是否是我们应用程序的插件  
            if (shareUesrId != null && shareUesrId.equals("com.sunzxyong.myapp") && !pkgName.equals(this.getPackageName())) {  
                String label = pm.getApplicationLabel(info.applicationInfo).toString();//得到插件apk的名称  
                PluginBean bean = new PluginBean(label,pkgName);  
                plugins.add(bean);  
            }  
        }  
        return plugins;  
    }  
通过这段代码，我们就可以轻松的获取手机内存在的所有插件，其中PluginBean是定义的一个实体类而已，就不贴它的代码了。

3、如果找到了插件，就把可用的插件显示出来了，如果没有找到，那么就可提示用户先去下载插件什么的。
[java] view plain copy print?
List<HashMap<String, String>> datas = new ArrayList<>();  
List<PluginBean> plugins = findAllPlugin();  
if (plugins != null && !plugins.isEmpty()) {  
    for (PluginBean bean : plugins) {  
        HashMap<String, String> map = new HashMap<>();  
        map.put("label", bean.getLabel());  
        datas.add(map);  
    }  
} else {  
    Toast.makeText(this, "没有找到插件，请先下载！", Toast.LENGTH_SHORT).show();  
}  
showEnableAllPluginPopup(datas);  
4、如果找到后，那么我们选择对应的插件时，在宿主app中就加载插件内对应的资源，这个才是PathClassLoader的重点。我们首先看看怎么实现的吧：
[java] view plain copy print?
/** 
     * 加载已安装的apk 
     * @param packageName 应用的包名 
     * @param pluginContext 插件app的上下文 
     * @return 对应资源的id 
     */  
    private int dynamicLoadApk(String packageName, Context pluginContext) throws Exception {  
        //第一个参数为包含dex的apk或者jar的路径，第二个参数为父加载器  
        PathClassLoader pathClassLoader = new PathClassLoader(pluginContext.getPackageResourcePath(),ClassLoader.getSystemClassLoader());  
//        Class<?> clazz = pathClassLoader.loadClass(packageName + ".R$mipmap");//通过使用自身的加载器反射出mipmap类进而使用该类的功能  
        //参数：1、类的全名，2、是否初始化类，3、加载时使用的类加载器  
        Class<?> clazz = Class.forName(packageName + ".R$mipmap", true, pathClassLoader);  
        //使用上述两种方式都可以，这里我们得到R类中的内部类mipmap，通过它得到对应的图片id，进而给我们使用  
        Field field = clazz.getDeclaredField("one");  
        int resourceId = field.getInt(R.mipmap.class);  
        return resourceId;  
    }  


这个方法就是加载包名为packageName的插件，然后获得插件内名为one.png的图片的资源id，进而供宿主app使用该图片。现在我们一步一步来讲解一下：
首先就是new出一个PathClassLoader对象，它的构造方法为：
[java] view plain copy print?
public PathClassLoader(String dexPath, ClassLoader parent)  
中其中第一个参数是通过插件的上下文来获取插件apk的路径，其实获取到的就是/data/app/apkthemeplugin.apk，那么插件的上下文怎么获取呢？在宿主app中我们只有本app的上下文啊，答案就是为插件app创建一个上下文：
[java] view plain copy print?
//获取对应插件中的上下文,通过它可得到插件的Resource  
           Context plugnContext = this.createPackageContext(packageName, CONTEXT_IGNORE_SECURITY | CONTEXT_INCLUDE_CODE);  
通过插件的包名来创建上下文，不过这种方法只适合获取已安装的app上下文。或者不需要通过反射直接通过插件上下文getResource().getxxx(R.*.*);也行，而这里用的是反射方法。
第二个参数是父加载器，都是ClassLoader.getSystemClassLoader()。
好了，插件app的类加载器我们创建出来了,接下来就是通过反射获取对应类的资源了，这里我是获取R类中的内部类mipmap类，然后通过反射得到mipmap类中名为one的字段的值，，然后通过
[java] view plain copy print?
plugnContext.getResources().getDrawable(resouceId)  
就可以获取对应id的Drawable得到该图片资源进而宿主app的可用它设置背景等。
当然也可以获取到其它的资源或者获取Acitivity类等，这里只是做一个示例。
备：关于R类，在AS中的目录为：/build/generated/source/r/debug/<- packageName ->。它的内部类有：脑洞大的可以尽可能的利用这些资源吧！！！
下面演示下该demo效果，在没有插件情况下会提示请先下载插件，有插件时候就选择对应的插件而供宿主app使用，本demo是换背景的功能演示，我来看宿主app中mipmap文件夹下并没有one.png这张图片，截图为证：

在没有安装插件情况下：

安装插件后：

可以看到，宿主app使用了插件中的图片资源。

这时，有的人就会想，这个插件需要下载下来还需要安装到手机中去，这不就是又安装了一个apk啊，只是没显示出来而已，这种方式不太友好，那么，可不可以只下载下来，不用安装，也能供宿主app使用呢？像微信上可以运行没有安装的飞机大战这样的，这当然可以的。这就需要用到另外一个加载器DexClassLoader。
4、DexClassLoader加载未安装的apk，提供资源供宿主app使用
关于动态加载未安装的apk，我先描述下思路：首先我们得到事先知道我们的插件apk存放在哪个目录下，然后分别得到插件apk的信息（名称、包名等），然后显示可用的插件，最后动态加载apk获得资源。
按照上面这个思路，我们需要解决几个问题：
1、怎么得到未安装的apk的信息
2、怎么得到插件的context或者Resource，因为它是未安装的不可能通过createPackageContext(...);方法来构建出一个context，所以这时只有在Resource上下功夫。
现在我们就一一来解答这些问题吧：
1、得到未安装的apk信息可以通过mPackageManager.getPackageArchiveInfo()方法获得，
[java] view plain copy print?
public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags)  
它的参数刚好是传入一个FilePath，然后返回apk文件的PackageInfo信息：
[java] view plain copy print?
/** 
     * 获取未安装apk的信息 
     * @param context 
     * @param archiveFilePath apk文件的path 
     * @return 
     */  
    private String[] getUninstallApkInfo(Context context, String archiveFilePath) {  
        String[] info = new String[2];  
        PackageManager pm = context.getPackageManager();  
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_ACTIVITIES);  
        if (pkgInfo != null) {  
            ApplicationInfo appInfo = pkgInfo.applicationInfo;  
            String versionName = pkgInfo.versionName;//版本号  
            Drawable icon = pm.getApplicationIcon(appInfo);//图标  
            String appName = pm.getApplicationLabel(appInfo).toString();//app名称  
            String pkgName = appInfo.packageName;//包名  
            info[0] = appName;  
            info[1] = pkgName;  
        }  
        return info;  
    }  

2、得到对应未安装apk的Resource对象，我们需要通过反射来获得：
[java] view plain copy print?
/** 
     * @param apkName  
     * @return 得到对应插件的Resource对象 
     */  
    private Resources getPluginResources(String apkName) {  
        try {  
            AssetManager assetManager = AssetManager.class.newInstance();  
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);//反射调用方法addAssetPath(String path)  
            //第二个参数是apk的路径：Environment.getExternalStorageDirectory().getPath()+File.separator+"plugin"+File.separator+"apkplugin.apk"  
            addAssetPath.invoke(assetManager, apkDir+File.separator+apkName);//将未安装的Apk文件的添加进AssetManager中，第二个参数为apk文件的路径带apk名  
            Resources superRes = this.getResources();  
            Resources mResources = new Resources(assetManager, superRes.getDisplayMetrics(),  
                    superRes.getConfiguration());  
            return mResources;  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
        return null;  
    }  

通过得到AssetManager中的内部的方法addAssetPath，将未安装的apk路径传入从而添加进assetManager中，然后通过new Resource把assetManager传入构造方法中，进而得到未安装apk对应的Resource对象。

好了！上面两个问题解决了，那么接下来就是加载未安装的apk获得它的内部资源。
[java] view plain copy print?
/** 
     * 加载apk获得内部资源 
     * @param apkDir apk目录 
     * @param apkName apk名字,带.apk 
     * @throws Exception 
     */  
    private void dynamicLoadApk(String apkDir, String apkName, String apkPackageName) throws Exception {  
        File optimizedDirectoryFile = getDir("dex", Context.MODE_PRIVATE);//在应用安装目录下创建一个名为app_dex文件夹目录,如果已经存在则不创建  
        Log.v("zxy", optimizedDirectoryFile.getPath().toString());// /data/data/com.example.dynamicloadapk/app_dex  
        //参数：1、包含dex的apk文件或jar文件的路径，2、apk、jar解压缩生成dex存储的目录，3、本地library库目录，一般为null，4、父ClassLoader  
        DexClassLoader dexClassLoader = new DexClassLoader(apkDir+File.separator+apkName, optimizedDirectoryFile.getPath(), null, ClassLoader.getSystemClassLoader());  
        Class<?> clazz = dexClassLoader.loadClass(apkPackageName + ".R$mipmap");//通过使用apk自己的类加载器，反射出R类中相应的内部类进而获取我们需要的资源id  
        Field field = clazz.getDeclaredField("one");//得到名为one的这张图片字段  
        int resId = field.getInt(R.id.class);//得到图片id  
        Resources mResources = getPluginResources(apkName);//得到插件apk中的Resource  
        if (mResources != null) {  
            //通过插件apk中的Resource得到resId对应的资源  
            findViewById(R.id.background).setBackgroundDrawable(mResources.getDrawable(resId));  
        }  
    }  
其中通过new DexClassLoader()来创建未安装apk的类加载器，我们来看看它的参数：
[java] view plain copy print?
public DexClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent)  
dexPath - 就是apk文件的路径
optimizedDirectory - apk解压缩后的存放dex的目录，值得注意的是，在4.1以后该目录不允许在sd卡上，看官方文档：
[plain] view plain copy print?
A class loader that loads classes from .jar and .apk files containing a classes.dex entry. This can be used to execute code not installed as part of an application.  
  
This class loader requires an application-private, writable directory to cache optimized classes. Use Context.getDir(String, int) to create such a directory:  
  
   File dexOutputDir = context.getDir("dex", 0);  
  
Do not cache optimized classes on external storage. External storage does not provide access controls necessary to protect your application from code injection atta  
，所以我们用getDir()方法在应用内部创建一个dexOutputDir。
libraryPath - 本地的library，一般为null
parent - 父加载器
接下来，就是通过反射的方法，获取出需要的资源。

下面我们来看看demo演示的效果，我是把三个apk插件先放在assets目录下，然后copy到sd上来模仿下载过程，然后加载出相应插件的资源：
先只拷贝一个插件：
[java] view plain copy print?
copyApkFile("apkthemeplugin-1.apk");  

可以看到正常的获取到了未安装apk的资源。
再看看拷贝了三个插件：
[java] view plain copy print?
copyApkFile("apkthemeplugin-1.apk");  
copyApkFile("apkthemeplugin-2.apk");  
copyApkFile("apkthemeplugin-3.apk");  

可以看到只要一有插件下载，就能显示出来并使用它。

当然插件化开发并不只是像只有这种换肤那么简单的用途，这只是个demo，学习这种插件化开发思想的。由此可以联想，这种插件化的开发，是不是像QQ里的表情包啊、背景皮肤啊，通过线上下载线下维护的方式，可以在线下载使用相应的皮肤，不使用时候就可以删了，所以插件化开发是插件与宿主app进行解耦了，即使在没有插件情况下，也不会对宿主app有任何影响，而有的话就供用户选择性使用了。

1.Java 类加载器

类加载器（class loader）是 Java™中的一个很重要的概念。类加载器负责加载 Java 类的字节代码到 Java 虚拟机中。本文首先详细介绍了 Java 类加载器的基本概念，包括代理模式、加载类的具体过程和线程上下文类加载器等，接着介绍如何开发自己的类加载器，最后介绍了类加载器在 Web 容器和 OSGi™中的应用。

2.反射原理

Java 提供的反射機制允許您於執行時期動態載入類別、檢視類別資訊、生成物件或操作生成的物件，要舉反射機制的一個應用實例，就是在整合式開發環境中所提供的方法提示或是類別檢視工具，另外像 JSP 中的 JavaBean 自動收集請求資訊也使用到反射，而一些軟體開發框架（Framework）也常見到反射機制的使用，以達到動態載入使用者自訂類別的目的。

3.代理模式及Java实现动态代理

定义：给某个对象提供一个代理对象，并由代理对象控制对于原对象的访问，即客户不直接操控原对象，而是通过代理对象间接地操控原对象。

入门
1.Android动态加载dex技术初探

Android使用Dalvik虚拟机加载可执行程序，所以不能直接加载基于class的jar，而是需要将class转化为dex字节码，从而执行代码。优化后的字节码文件可以存在一个*.jar中，只要其内部存放的是*.dex即可使用。

2.Android插件化入门

开发者将插件代码封装成Jar或者APK。宿主下载或者从本地加载Jar或者APK到宿主中。将宿主调用插件中的算法或者Android特定的Class（如Activity）

3.插件化开发—动态加载技术加载已安装和未安装的apk

动态加载技术就是使用类加载器加载相应的apk、dex、jar（必须含有dex文件），再通过反射获得该apk、dex、jar内部的资源（class、图片、color等等）进而供宿主app使用。

4.Android动态加载技术三个关键问题详解

动态加载技术（也叫插件化技术）在技术驱动型的公司中扮演着相当重要的角色，当项目越来越庞大的时候，需要通过插件化来减轻应用的内存和CPU占用，还可以实现热插拔，即在不发布新版本的情况下更新某些模块。

进阶
1.携程Android App插件化和动态加载实践

携程Android App的插件化和动态加载框架已上线半年，经历了初期的探索和持续的打磨优化，新框架和工程配置经受住了生产实践的考验。本文将详细介绍Android平台插件式开发和动态加载技术的原理和实现细节，回顾携程Android App的架构演化过程，期望我们的经验能帮助到更多的Android工程师。

2.动态加载APK原理分享

被加载的apk称之为插件，因为机制类似于生物学的"寄生"，加载了插件的应用也被称为宿主。 往往不是所有的apk都可作为插件被加载，往往需要遵循一定的"开发规范"，还需要插件项目引入某种api类库，业界通常都是这么做的。

3.Android插件化的一种实现

Android的插件化已经是老生常谈的话题了，插件化的好处有很多：解除代码耦合，插件支持热插拔，静默升级，从根本上解决65K属性和方法的bug等等。下面给大家介绍一下我们正在用的差价化框架。本片主要以类图的方式向大家介绍插件话框架的实现。

4.蘑菇街 App 的组件化之路

随着我街业务的蓬勃发展，产品和运营随时上新功能新活动的需求越来越强烈，经常可以听到“有个功能我想周x上，行不行”。行么？当然是不行啦，上新功能得发新版本啊，到时候费时费力打乱开发节奏不说，覆盖率也是个问题。

5.DynamicLoadApk 源码解析

DynamicLoadApk 是一个开源的 Android 插件化框架。插件化的优点包括：(1) 模块解耦，(2) 动态升级，(3) 高效并行开发(编译速度更快) (4) 按需加载，内存占用更低等等DynamicLoadApk 提供了 3 种开发方式，让开发者在无需理解其工作原理的情况下快速的集成插件化功能。

6.Android apk动态加载机制的研究

问题是这样的：我们知道，apk必须安装才能运行，如果不安装要是也能运行该多好啊，事实上，这不是完全不可能的，尽管它比较难实现。在理论层面上，我们可以通过一个宿主程序来运行一些未安装的apk，当然，实践层面上也能实现，不过这对未安装的apk有要求。我们的想法是这样的，首先要明白apk未安装是不能被直接调起来.

7.美团Android DEX自动拆包及动态加载简介

作为一个android开发者，在开发应用时，随着业务规模发展到一定程度，不断地加入新功能、添加新的类库，代码在急剧的膨胀，相应的apk包的大小也急剧增加， 那么终有一天，你会不幸遇到这个错误.

8.途牛原创|途牛Android App的插件实现

途牛的插件化是基于dynamic-load-apk（github）实现的。定义了宿主和插件的通信方式，使得两者能够互起对方的页面，调用彼此的功能。同时对activity的启动方式singletask等进行了模式实现，并增加了对Service的支持等。总之使得插件开发最大限度的保持着原有的Android开发习惯。

9. Android apk资源加载和activity生命周期管理

博主分析了Android中apk的动态加载机制，并在文章的最后指出需要解决的两个复杂问题：资源的访问和activity生命周期的管理，而本文将会分析这两个复杂问题的解决方法。

10.APK动态加载框架（DL）解析

首先要说的是动态加载技术（或者说插件化）在技术驱动型的公司中扮演这相当重要的角色，当项目越来越庞大的时候，需要通过插件化来减轻应用的内存和cpu占用，还可以实现热插拔，即在不发布新版本的情况下更新某些模块。

系列
1.Kaedea---Android动态加载技术 简单易懂的介绍

我们很早开始就在Android项目中采用了动态加载技术，主要目的是为了达到让用户不用重新安装APK就能升级应用的功能（特别是 SDK项目），这样一来不但可以大大提高应用新版本的覆盖率，也减少了服务器对旧版本接口兼容的压力，同时如果也可以快速修复一些线上的BUG。

2.Kaedea---Android动态加载基础 ClassLoader的工作机制

早期使用过Eclipse等Java编写的软件的同学可能比较熟悉，Eclipse可以加载许多第三方的插件（或者叫扩展），这就是动态加载。这些插件大多是一些Jar包，而使用插件其实就是动态加载Jar包里的Class进行工作。

3.Kaedea---Android动态加载补充 加载SD卡的SO库

Android中JNI的使用其实就包含了动态加载，APP运行时动态加载.so库并通过JNI调用其封装好的方法。后者一般是使用NDK工具从C/C++代码编译而成，运行在Native层，效率会比执行在虚拟机的Java代码高很多，所以Android中经常通过动态加载.so库来完成一些对性能比较有需求的工作（比如T9搜索、或者Bitmap的解码、图片高斯模糊处理等）。

4.Kaedea---Android动态加载入门 简单加载模式

Java程序中，JVM虚拟机是通过类加载器ClassLoader加载.jar文件里面的类的。Android也类似，不过Android用的是Dalvik/ART虚拟机，不是JVM，也不能直接加载.jar文件，而是加载dex文件。

5.Kaedea---Android动态加载进阶 代理Activity模式

简单模式中，使用ClassLoader加载外部的Dex或Apk文件，可以加载一些本地APP不存在的类，从而执行一些新的代码逻辑。但是使用这种方法却不能直接启动插件里的Activity。

6.Kaedea---Android动态加载黑科技 动态创建Activity模式

还记得我们在代理Activity模式里谈到启动插件APK里的Activity的两个难题吗，由于插件里的Activity没在主项目的Manifest里面注册，所以无法经历系统Framework层级的一系列初始化过程，最终导致获得的Activity实例并没有生命周期和无法使用res资源。

7.尼古拉斯---插件开发基础篇：动态加载技术解读

在目前的软硬件环境下，Native App与Web App在用户体验上有着明显的优势，但在实际项目中有些会因为业务的频繁变更而频繁的升级客户端，造成较差的用户体验，而这也恰恰是Web App的优势。本文对网上Android动态加载jar的资料进行梳理和实践在这里与大家一起分享，试图改善频繁升级这一弊病。

8.尼古拉斯---插件开发开篇：类加载器分析

这篇文章主要介绍了Android中主要的两个类加载器：PathClassLoader和DexClassLoader,他们的区别，联系，用法等问题，以及我们在制作插件的过程中会遇到哪些常见的问题。这篇文章也是后续两篇文章的基础，因为如果不了解这两个类的话，我们将无法进行后续的操作。

9.尼古拉斯---插件开发中篇：资源加载问题(换肤原理解析)

这篇文章主要通过现在一些应用自带的换肤技术的解读来看看，在开发插件的过程中如何解决一些资源加载上的问题，这个问题为何要单独拿出来解释，就是因为他涉及的知识很多，也是后面一篇文章的基础，我们在需要加载插件中的资源文件的时候。

10.尼古拉斯---插件开发终极篇：动态加载Activity(免安装运行程序)

这篇文章主要是讲解了如何加载插件中的Activity。从而实现免安装运行程序，同时这篇文章也是对前三篇文章知识的综合使用。下载很多应用都会使用到插件技术，因为包的大小和一些功能的优先级来决定哪些模块可以制作成插件。

11.Weishu---Android插件化原理解析——概要

类的加载可以使用Java的ClassLoader机制，但是对于Android来说，并不是说类加载进来就可以用了，很多组件都是有“生命”的；因此对于这些有血有肉的类，必须给它们注入活力，也就是所谓的组件生命周期管理.

12.Weishu---Android插件化原理解析——Hook机制之动态代理

使用代理机制进行API Hook进而达到方法增强是框架的常用手段，比如J2EE框架Spring通过动态代理优雅地实现了AOP编程，极大地提升了Web开发效率；同样，插件框架也广泛使用了代理机制来增强系统API从而达到插件化的目的.

13.Weishu---Android插件化原理解析——Hook机制之Binder Hook

Android系统通过Binder机制给应用程序提供了一系列的系统服务，诸如ActivityManagerService，ClipboardManager， AudioManager等；这些广泛存在系统服务给应用程序提供了诸如任务管理，音频，视频等异常强大的功能。

14.Weishu---Android 插件化原理解析——Hook机制之AMS&PMS

在前面的文章中我们介绍了DroidPlugin的Hook机制，也就是代理方式和Binder Hook；插件框架通过AOP实现了插件使用和开发的透明性。在讲述DroidPlugin如何实现四大组件的插件化之前，有必要说明一下它对AMS以及PMS的Hook方式。

15.Weishu---Android 插件化原理解析——Activity生命周期管理

之前的 Android插件化原理解析 系列文章揭开了Hook机制的神秘面纱，现在我们手握倚天屠龙，那么如何通过这种技术完成插件化方案呢？具体来说，插件中的Activity，Service等组件如何在Android系统上运行起来？

16.Weishu---Android 插件化原理解析——插件加载机制

上文 Activity生命周期管理 中我们地完成了『启动没有在AndroidManifest.xml中显式声明的Activity』的任务；通过Hook AMS和拦截ActivityThread中H类对于组件调度我们成功地绕过了AndroidMAnifest.xml的限制。

类库
1.Small

世界那么大，组件那么小。Small，做最轻巧的跨平台插件化框架。里面有很详细的文档

2.Android-Plugin-Framework

此项目是Android插件开发框架完整源码及示例。用来通过动态加载的方式在宿主程序中运行插件APK。

3.DynamicAPK

实现Android App多apk插件化和动态加载，支持资源分包和热修复.携程App的插件化和动态加载框架.

4.DroidPlugin

是360手机助手在Android系统上实现了一种新的插件机制

5.android-pluginmgr

不需要插件规范的apk动态加载框架。

6.dynamic-load-apk

Android 使用动态加载框架DL进行插件化开发

7.AndroidDynamicLoader

Android 动态加载框架，他不是用代理 Activity 的方式实现而是用 Fragment 以及 Schema 的方式实现

8.ACDD

非代理Android动态部署框架
