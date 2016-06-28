package net.kimleo.inject.injection;

import net.kimleo.inject.annotation.Component;
import net.kimleo.inject.annotation.Construct;
import net.kimleo.inject.annotation.Qualified;
import net.kimleo.inject.context.DefaultApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;

import static java.util.Arrays.asList;

public class ConstructorInjector implements Injector {

    private final DefaultApplicationContext context;

    public ConstructorInjector(DefaultApplicationContext context) {
        this.context = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T inject(Class<? extends T> clz) {
        Constructor[] constructors = clz.getDeclaredConstructors();
        Constructor ctor = getInjectedConstructor(constructors);
        try {
            if (ctor != null) {
                Parameter[] parameterTypes = ctor.getParameters();
                if (parameterTypes == null || parameterTypes.length == 0) return null;
                ArrayList<Object> objects = new ArrayList<>();
                for (Parameter param : parameterTypes) {
                    context.addComponent(param.getType());
                    Qualified qualified = param.getAnnotation(Qualified.class);
                    if (qualified != null) {
                        objects.add(context.getInstance(param.getType(), qualified.value()));
                    } else
                        objects.add(context.getInstance(param.getType()));
                }
                Object instance = ctor.newInstance(objects.toArray());
                Component annotation = clz.getAnnotation(Component.class);
                if (!annotation.qualifier().isEmpty()) {
                    context.addQualifiedInstance(clz, annotation.qualifier(), instance);
                }
                return (T) instance;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Constructor getInjectedConstructor(Constructor[] constructors) {
        for (Constructor ctor : constructors) {
            if (!isConstructable(ctor)) continue;
            Class[] parameterTypes = ctor.getParameterTypes();
            if (asList(parameterTypes).stream().allMatch(context::isContextComponent)) {
                return ctor;
            }
        }
        return null;
    }

    private boolean isConstructable(Constructor ctor) {
        return ctor.getAnnotation(Construct.class) != null;
    }

}
