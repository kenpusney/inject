package net.kimleo.inject.context;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.kimleo.inject.annotation.*;
import net.kimleo.inject.injection.ConstructorInjector;
import net.kimleo.inject.injection.FieldInjector;
import net.kimleo.inject.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;

public class DefaultApplicationContext implements ApplicationContext {


    private static final String SINGLETON_CREATION_METHOD = "getInstance";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultApplicationContext.class);
    private final Map<Class, Object> context = new ConcurrentHashMap<>();
    private final Map<Class, Class> components = new ConcurrentHashMap<>();
    private final List<Injector> injectors = asList(new ConstructorInjector(this), new FieldInjector(this));

    private final Multimap<Class, QualifiedComponent> qualifiedContext = HashMultimap.create();

    private final Map<Class, Method> configurations = new ConcurrentHashMap<>();

    public DefaultApplicationContext(Class... classes) {
        addComponents(classes);
    }

    private void addComponents(Class... classes) {
        for (Class aClass : classes) {
            if (aClass.getAnnotation(Config.class) != null) {
                LOGGER.info("Configuration found {}", aClass);
                Method[] methods = aClass.getMethods();
                for (Method method : methods) {
                    if (method.getAnnotation(Bean.class) != null) {
                        Class<?> type = method.getReturnType();
                        LOGGER.debug("Configuration for bean type {} with dependency {}", type, method);
                        configurations.put(type, method);
                        components.put(type, type);
                    }
                }
            }
            addToComponentMappings(aClass);
        }
        initializeContext();
    }

    private boolean isComponentClass(Class clz) {
        return clz.getAnnotation(Component.class) != null || clz.getAnnotation(Application.class) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<? extends T> aClass) {
        LOGGER.debug("Try get instance of {}", aClass);
        if (isContextComponent(aClass)) {
            addComponent(aClass);
            return (T) context.get(getRealComponent(aClass));
        }
        return null;
    }

    private Class getRealComponent(Class param) {
        Class realComponent = components.get(param);
        LOGGER.debug("Using {} instead of {}", realComponent, param);
        return realComponent;
    }

    public boolean isContextComponent(Class param) {
        return components.containsKey(param);
    }

    public void addComponent(Class component) {
        addToContextIfItIsAComponent(getRealComponent(component));
    }

    public <T> void addQualifiedInstance(Class<? extends T> clz, String qualifier, T instance) {
        qualifiedContext.put(clz, new QualifiedComponent(qualifier, instance));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<? extends T> type, String qualified) {
        Collection<QualifiedComponent> components = qualifiedContext.get(getRealComponent(type));
        for (QualifiedComponent component : components) {
            if (component.getQualifier().equals(qualified)) {
                return (T) component.getObject();
            }
        }
        return null;
    }

    private void addToComponentMappings(Class clz) {
        if (isComponentClass(clz) && !clz.isInterface()) {
            LOGGER.debug("Component {} found", clz);
            components.put(clz, clz);
            Class[] interfaces = clz.getInterfaces();
            for (Class theInterface : interfaces) {
                if (isComponentClass(theInterface)) {
                    LOGGER.debug("Component {} registered for interface {}", clz, theInterface);
                    components.put(theInterface, clz);
                }
            }
        }
    }

    private void initializeContext() {
        configurations.keySet().forEach(this::addToContextIfItIsAComponent);
        components.keySet().forEach(this::addToContextIfItIsAComponent);
    }

    private void addToContextIfItIsAComponent(Class clz) {
        if (configurations.containsKey(clz) || !clz.isInterface() && isComponentClass(clz)) {
            context.put(clz, createInstance(clz));
        }
    }

    private Object createInstance(Class clz) {
        if (context.containsKey(clz)) {
            LOGGER.debug("{} instance existed, no need to create", clz);
            return context.get(clz);
        }
        if (configurations.containsKey(clz)) {
            LOGGER.debug("{} instance was configured, creating from configuration", clz);
            return createFromConfiguration(clz);
        }
        if (clz.getAnnotation(Factory.class) != null) {
            LOGGER.debug("{} is a factory, creating using factory specified method", clz);
            Object object = createNewFactory(clz);
            Factory annotation = (Factory) clz.getAnnotation(Factory.class);
            if (annotation.qualifier().isEmpty()) {
                qualifiedContext.put(clz, new QualifiedComponent("", object));
            } else {
                qualifiedContext.put(clz, new QualifiedComponent(annotation.qualifier(), object));
            }
            return object;
        }
        for (Injector injector : injectors) {
            Object instance = injector.inject(clz);
            LOGGER.debug("Trying injector {} to create instance of {}", injector.getClass(), clz);
            if (instance != null) {
                LOGGER.debug("Injected {} successfully with injector {}", clz, injector.getClass());
                return instance;
            }
        }
        return null;
    }

    private Object createFromConfiguration(Class clz) {
        Method method = configurations.get(clz);
        Bean annotation = method.getAnnotation(Bean.class);
        Object configuration = createInstance(method.getDeclaringClass());
        ArrayList<Object> params = new ArrayList<>();
        LOGGER.debug("Creating {} from configuration {}", clz, configuration.getClass());
        for (Class<?> paramType : method.getParameterTypes()) {
            params.add(getInstance(paramType));
        }
        Object[] paramArray = params.toArray();
        try {
            Object object = method.invoke(configuration, paramArray);
            LOGGER.debug("Add default qualifier {} for {}", method.getName(), clz);
            qualifiedContext.put(clz, new QualifiedComponent(method.getName(), object));
            if (!annotation.qualifier().isEmpty()) {
                LOGGER.debug("Qualifier {} found for {}", annotation.qualifier(), clz);
                qualifiedContext.put(clz, new QualifiedComponent(annotation.qualifier(), object));
            }
            return object;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object createNewFactory(Class clz) {
        try {
            return clz.getDeclaredMethod(SINGLETON_CREATION_METHOD).invoke(clz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
