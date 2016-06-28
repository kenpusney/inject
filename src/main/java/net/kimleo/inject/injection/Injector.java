package net.kimleo.inject.injection;

public interface Injector {
    <T> T inject(Class<? extends T> clz);
}
