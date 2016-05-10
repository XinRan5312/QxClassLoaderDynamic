package com.xinran.testdinamicclassloader.loadinstallapk;

/**
 * Created by qixinh on 16/5/10.
 */
public class QxApkBean {
    private String apkName;
    private String apkPackageName;

    public QxApkBean(String apkName, String apkPackageName) {
        this.apkName = apkName;
        this.apkPackageName = apkPackageName;
    }

    public String getApkName() {
        return apkName;
    }

    public void setApkName(String apkName) {
        this.apkName = apkName;
    }

    public String getApkPackageName() {
        return apkPackageName;
    }

    public void setApkPackageName(String apkPackageName) {
        this.apkPackageName = apkPackageName;
    }
}
