package net.kimleo.inject.context;

public interface ApplicationContext {
    <T> T getInstance(Class<? extends T> clz);

    <T> T getInstance(Class<? extends T> clz, String qualifier);
}
