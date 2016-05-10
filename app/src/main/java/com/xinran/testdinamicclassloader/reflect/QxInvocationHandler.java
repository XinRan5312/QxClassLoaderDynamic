package com.xinran.testdinamicclassloader.reflect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by qixinh on 16/5/10.
 */
public class QxInvocationHandler implements InvocationHandler {
    private DynamicMethod mDynamicMethod;
    private Object mDelegate;

    public QxInvocationHandler(Object delegate) {
        this.mDelegate = delegate;
    }

    public QxInvocationHandler(Object delegate, DynamicMethod dynamicMethod) {
        this.mDelegate = delegate;
        this.mDynamicMethod = dynamicMethod;
    }

    public Object bind() {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                mDelegate.getClass().getInterfaces(), this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object obj = null;
        if (mDynamicMethod != null) {
            mDynamicMethod.beforeInvokeMothed();
        }
        obj = method.invoke(mDelegate, args);
        if (mDynamicMethod != null) {
            mDynamicMethod.afterInvokeMehod();
        }
        return obj;
    }

    public void setmDynamicMethod(DynamicMethod mDynamicMethod) {
        this.mDynamicMethod = mDynamicMethod;
    }

    public interface DynamicMethod {
        void beforeInvokeMothed();

        void afterInvokeMehod();
    }
}
