package net.kimleo.inject.application;

import net.kimleo.inject.context.ApplicationContext;
import net.kimleo.inject.context.ClassUtils;
import net.kimleo.inject.context.DefaultApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationBoot {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationBoot.class);

    public void run(Class<?> appClass, String... args) throws Exception {
        assert (appClass.getAnnotation(net.kimleo.inject.annotation.Application.class) != null);
        LOGGER.debug("Initializing application class {}", appClass);
        ApplicationContext context = new DefaultApplicationContext(ClassUtils.getClasses(appClass.getPackage().getName()));

        context.getInstance(Runner.class).run(args);
    }

}
