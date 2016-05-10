package com.xinran.testdinamicclassloader.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Created by qixinh on 16/5/10.
 */
public class QxReflectTest {
    public static void test() {
        try {
            Class c = Class.forName("全路径", false, Thread.currentThread().getContextClassLoader());

            Package pk = c.getPackage();
            Class cParent = c.getSuperclass();

            ClassLoader cLoader = c.getClassLoader();

            ClassLoader pLoader = cLoader.getParent();

            //获得修饰符比如private public protected等，class field method都有Modifers
            int m = c.getModifiers();

            System.out.print(Modifier.toString(m) + " ");

            if (Modifier.isInterface(m)) {
                System.out.print("interface ");
            } else {
                System.out.print("class ");
            }

            //属性====================

            Field[] fields = c.getDeclaredFields();//获取所有包括private的
            Field testInt = c.getField("testInt");//根据属性名字直接得到想要的属性
            for (Field field : fields) {

                if (Integer.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    field.setInt(c.newInstance(), 2);
                }

                System.out.print("\t" +
                        Modifier.toString(field.getModifiers()));
                // field是什么type
                System.out.print(" " +
                        field.getType().getName() + " ");
                // field的name
                System.out.println(field.getName() + ";");
            }

            // 构造器===================

            Constructor[] constructors =
                    c.getDeclaredConstructors();//获得所有的构造器，
            // getDeclaredConstructor(Class<?>... parameterTypes)获取指定参数class类型的那个构造器
            for (Constructor constructor : constructors) {

                System.out.print("\t" +
                        Modifier.toString(
                                constructor.getModifiers()));

                System.out.println(" " +
                        constructor.getName() + "();");
            }

//得到一个有两个参数的构造器，并且产生一个实例

            Class[] params = new Class[2];

            params[0] = String.class;

            params[1] = Integer.TYPE;


            Constructor constructor =
                    c.getConstructor(params);


            Object[] argObjs = new Object[2];
            argObjs[0] = "caterpillar";
            argObjs[1] = new Integer(90);


            Object obj = constructor.newInstance(argObjs);

            System.out.println(obj);


            //方法=================

            Method[] methods = c.getDeclaredMethods();
            for (Method method : methods) {

                System.out.print("\t" +
                        Modifier.toString(
                                method.getModifiers()));

                System.out.print(" " +
                        method.getReturnType().getName() + " ");

                System.out.println(method.getName() + "();");
            }

//调用某个反射得到的方法

            Object targetObj = c.newInstance();

            Class[] param1 = {String.class};

            Method setNameMethod = c.getMethod("setName", param1);//传入指定方法名字和参数的class类型

            Object[] argObjs1 = {"caterpillar"};
            if (Modifier.isPrivate(setNameMethod.getModifiers())) {
                setNameMethod.setAccessible(true);
            }


            setNameMethod.invoke(targetObj, argObjs1);

            //数组 反射的使用==============

            Object objArr = Array.newInstance(c, 5);//一个是数组元素class类型，一个是长度

            for(int i = 0; i < 5; i++) {
                Array.set(objArr, i, i+"");
            }

            for(int i = 0; i < 5; i++) {
                System.out.print(Array.get(objArr, i) + " ");
            }
            System.out.println();

            String[] strs = (String[]) objArr;
            for(String s : strs) {
                System.out.print(s + " ");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
