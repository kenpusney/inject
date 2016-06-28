package net.kimleo.inject.context;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.io.IOException;
import java.util.ArrayList;

public class ClassUtils {
    public static Class[] getClasses(String packageName) {
        ArrayList<Class> classes = new ArrayList<>();
        Reflections reflect = new Reflections(packageName, new SubTypesScanner(false));
        classes.addAll(reflect.getSubTypesOf(Object.class));
        return classes.toArray(new Class[classes.size()]);
    }
}
