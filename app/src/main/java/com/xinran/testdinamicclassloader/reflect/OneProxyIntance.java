package com.xinran.testdinamicclassloader.reflect;

/**
 * Created by qixinh on 16/5/10.
 */
public class OneProxyIntance implements IProxy {
    @Override
    public void wr() {
        System.out.println("一个人的生活");
    }
}
