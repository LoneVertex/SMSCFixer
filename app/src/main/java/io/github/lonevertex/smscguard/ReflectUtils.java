package io.github.lonevertex.smscguard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ReflectUtils {
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    private ReflectUtils() {}

    static Object callMethod(Object obj, String methodName, Object... args) {
        if (obj == null) return null;
        try {
            Class<?>[] parameterTypes = getParameterTypes(args);
            Method method = getMethod(obj.getClass(), methodName, parameterTypes);
            return method.invoke(obj, args);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        if (clazz == null) return null;
        try {
            Class<?>[] parameterTypes = getParameterTypes(args);
            Method method = getMethod(clazz, methodName, parameterTypes);
            return method.invoke(null, args);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static Object getObjectField(Object obj, String fieldName) {
        if (obj == null) return null;
        try {
            Field field = getField(obj.getClass(), fieldName);
            return field.get(obj);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static Class<?>[] getParameterTypes(Object... args) {
        if (args == null || args.length == 0) return new Class<?>[0];
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = (args[i] != null) ? args[i].getClass() : Object.class;
        }
        return types;
    }

    private static Method getMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException {
        String cacheKey = getMethodCacheKey(clazz, methodName, parameterTypes);
        Method cached = METHOD_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Method found = findMethodBestMatch(clazz, methodName, parameterTypes);
        found.setAccessible(true);
        METHOD_CACHE.put(cacheKey, found);
        return found;
    }

    private static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException {
        Class<?> current = clazz;
        Method fallback = null;
        while (current != null) {
            for (Method m : current.getDeclaredMethods()) {
                if (m.getName().equals(methodName) && m.getParameterTypes().length == parameterTypes.length) {
                    if (isCompatible(m.getParameterTypes(), parameterTypes)) {
                        return m;
                    }
                    if (fallback == null) {
                        fallback = m;
                    }
                }
            }
            current = current.getSuperclass();
        }
        if (fallback != null) {
            return fallback;
        }
        throw new NoSuchMethodException(clazz.getName() + "#" + methodName);
    }

    private static boolean isCompatible(Class<?>[] methodParams, Class<?>[] argParams) {
        for (int i = 0; i < methodParams.length; i++) {
            if (argParams[i] != null && !isAssignable(methodParams[i], argParams[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAssignable(Class<?> targetType, Class<?> fromType) {
        if (targetType.isAssignableFrom(fromType)) {
            return true;
        }
        if (targetType.isPrimitive()) {
            if (targetType == int.class && (fromType == Integer.class || fromType == int.class)) return true;
            if (targetType == boolean.class && (fromType == Boolean.class || fromType == boolean.class)) return true;
            if (targetType == long.class && (fromType == Long.class || fromType == Integer.class || fromType == long.class)) return true;
            if (targetType == double.class && (fromType == Double.class || fromType == Float.class || fromType == double.class)) return true;
            if (targetType == float.class && (fromType == Float.class || fromType == float.class)) return true;
            if (targetType == byte.class && (fromType == Byte.class || fromType == byte.class)) return true;
            if (targetType == short.class && (fromType == Short.class || fromType == short.class)) return true;
            if (targetType == char.class && (fromType == Character.class || fromType == char.class)) return true;
        }
        return false;
    }

    private static String getMethodCacheKey(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        StringBuilder sb = new StringBuilder(clazz.getName()).append('#').append(methodName);
        for (Class<?> p : parameterTypes) {
            sb.append(':').append(p != null ? p.getName() : "null");
        }
        return sb.toString();
    }

    private static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        String cacheKey = clazz.getName() + '#' + fieldName;
        Field cached = FIELD_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Field found = findField(clazz, fieldName);
        found.setAccessible(true);
        FIELD_CACHE.put(cacheKey, found);
        return found;
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(clazz.getName() + "#" + fieldName);
    }
}
