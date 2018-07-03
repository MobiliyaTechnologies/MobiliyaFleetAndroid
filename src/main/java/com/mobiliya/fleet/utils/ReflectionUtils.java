package com.mobiliya.fleet.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ReflectionUtils {
    public static String GetClassName(Class<?> c) {
        String name = null;
        if (c != null) {
            name = c.getName();
            if (name.lastIndexOf('.') > 0) {
                name = name.substring(name.lastIndexOf('.') + 1);
            }
        }
        return name;
    }

    public static Object GetInstance(Class<?> c) {
        Object object = null;
        if (c != null) {
            Constructor<?> cons;
            try {
                cons = c.getConstructor();
                object = cons.newInstance();
            } catch (SecurityException e1) {
            } catch (NoSuchMethodException e1) {
            } catch (IllegalArgumentException e) {
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
        }

        return object;
    }
}
