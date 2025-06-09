/*
Copyright (c) 2018 Ericsson

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE. SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.ericsson.commonlibrary.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.objenesis.ObjenesisHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

/**
 * ByteBuddy-based implementation of ProxyEngineFactory. This provides an alternative to the Javassist-based
 * implementation.
 *
 * @author Proxy Team
 */
final class ByteBuddyProxyEngineFactory implements ProxyEngineFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ByteBuddyProxyEngineFactory.class);

    @Override
    public <T> T createInterfaceProxy(Class<?>... interfaces) {
        try {
            Class<?>[] validInterfaces = makeAValidInterfaceArray(interfaces);

            ByteBuddyInvocationHandler handler = new ByteBuddyInvocationHandler();

            // Use the class loader of the first interface to ensure visibility
            ClassLoader classLoader = validInterfaces.length > 0 ? validInterfaces[0].getClassLoader()
                    : ByteBuddyProxyEngineFactory.class.getClassLoader();

            Class<?> proxyClass = new ByteBuddy().subclass(Object.class).implement(validInterfaces)
                    .method(ElementMatchers.any()).intercept(InvocationHandlerAdapter.of(handler)).make()
                    .load(classLoader, ClassLoadingStrategy.Default.INJECTION).getLoaded();

            T proxy = (T) proxyClass.getDeclaredConstructor().newInstance();
            handler.setProxyInstance(proxy);
            return proxy;
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("Invisible interface type")) {
                throw new ProxyException(
                        "ByteBuddy engine cannot create proxies for private/package-private interfaces. "
                                + "The interface must be public or you should use Javassist engine for private interface proxying. "
                                + "Original error: " + e.getMessage(),
                        e);
            }
            throw new ProxyException("Failed to create interface proxy", e);
        } catch (Exception e) {
            throw new ProxyException("Failed to create interface proxy", e);
        }
    }

    @Override
    public <T> T createInterfaceJavaBeanProxy(Class<?> toJavaBeanify) {
        LOG.debug("Creating JavaBean proxy for interface {}", toJavaBeanify.getName());

        try {
            Class<?>[] validInterfaces = makeAValidInterfaceArray(toJavaBeanify);
            ByteBuddyInvocationHandler handler = new ByteBuddyInvocationHandler();

            // Use the class loader of the interface to ensure visibility
            ClassLoader originalClassLoader = toJavaBeanify.getClassLoader();
            ClassLoader safeClassLoader = getSafeClassLoader(originalClassLoader);
            ClassLoadingStrategy loadingStrategy = getLoadingStrategy(originalClassLoader);

            // Start with basic proxy builder
            ByteBuddy byteBuddy = new ByteBuddy();
            DynamicType.Builder<?> builder = byteBuddy.subclass(Object.class).implement(validInterfaces);

            // Add dynamic setter methods for all getter methods that don't have corresponding setters
            for (Method method : toJavaBeanify.getMethods()) {
                String methodName = method.getName();

                // Generate setter for getter methods
                if ((methodName.startsWith("get") || methodName.startsWith("is")) && method.getParameterCount() == 0
                        && !methodName.equals("getClass")) {

                    String setterName = generateSetterName(methodName);
                    Class<?> returnType = method.getReturnType();

                    // Check if the setter already exists
                    boolean setterExists = false;
                    try {
                        toJavaBeanify.getMethod(setterName, returnType);
                        setterExists = true;
                    } catch (NoSuchMethodException e) {
                        // Setter doesn't exist, we'll create it
                    }

                    if (!setterExists) {
                        // Define setter method: setXxx(Type value) that returns void
                        builder = builder.defineMethod(setterName, void.class, Modifier.PUBLIC)
                                .withParameter(returnType).withoutCode();
                    }
                }
            }

            // Apply the invocation handler to ALL methods at once (including the dynamically added setters)
            Class<?> proxyClass = builder.method(ElementMatchers.any()).intercept(InvocationHandlerAdapter.of(handler))
                    .make().load(safeClassLoader, loadingStrategy).getLoaded();

            T proxy = (T) proxyClass.getDeclaredConstructor().newInstance();
            handler.setProxyInstance(proxy);
            return proxy;
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("Invisible interface type")) {
                throw new ProxyException(
                        "ByteBuddy engine cannot create proxies for private/package-private interfaces. "
                                + "The interface must be public or you should use Javassist engine for private interface proxying. "
                                + "Original error: " + e.getMessage(),
                        e);
            }
            throw new ProxyException("Failed to create interface JavaBean proxy", e);
        } catch (Exception e) {
            throw new ProxyException("Failed to create interface JavaBean proxy", e);
        }
    }

    @Override
    public <T> T createClassProxy(Class<T> classToIntercept, Class<?>... interfaces) {
        try {
            Class<?> superClass = classToIntercept;
            if (InterceptableProxy.class.isAssignableFrom(classToIntercept)) {
                superClass = classToIntercept.getSuperclass();
            }

            Class<?>[] validInterfaces = makeAValidInterfaceArray(interfaces);
            ByteBuddyInvocationHandler handler = new ByteBuddyInvocationHandler();

            // Use the class loader of the target class to ensure visibility
            ClassLoader originalClassLoader = superClass.getClassLoader();
            ClassLoader safeClassLoader = getSafeClassLoader(originalClassLoader);
            ClassLoadingStrategy loadingStrategy = getLoadingStrategy(originalClassLoader);

            // For class proxies, we use InvocationHandlerAdapter but acknowledge its limitations
            // This will work for abstract methods and interface methods, but not for concrete class methods
            Class<?> proxyClass = new ByteBuddy().subclass(superClass).implement(validInterfaces)
                    .method(ElementMatchers.any().and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class)))
                            .or(ElementMatchers.named("toString")).or(ElementMatchers.named("hashCode"))
                            .or(ElementMatchers.named("equals")))
                    .intercept(InvocationHandlerAdapter.of(handler)).make().load(safeClassLoader, loadingStrategy)
                    .getLoaded();

            T proxy;
            try {
                // Try to create with default constructor first
                proxy = (T) proxyClass.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                // If no default constructor, use Objenesis to create without calling constructor
                proxy = (T) ObjenesisHelper.newInstance(proxyClass);
            }

            handler.setProxyInstance(proxy);
            return proxy;
        } catch (Exception e) {
            throw new ProxyException("Failed to create class proxy", e);
        }
    }

    @Override
    public <T> T createClassProxyWithArguments(Class<T> classToIntercept, Object... constructorArgs) {
        try {
            Class<?> superClass = classToIntercept;
            if (InterceptableProxy.class.isAssignableFrom(classToIntercept)) {
                superClass = classToIntercept.getSuperclass();
            }

            Class<?>[] validInterfaces = makeAValidInterfaceArray(new Class<?>[0]);
            ByteBuddyInvocationHandler handler = new ByteBuddyInvocationHandler();

            // Use the class loader of the target class to ensure visibility
            ClassLoader originalClassLoader = superClass.getClassLoader();
            ClassLoader safeClassLoader = getSafeClassLoader(originalClassLoader);
            ClassLoadingStrategy loadingStrategy = getLoadingStrategy(originalClassLoader);

            Class<?> proxyClass = new ByteBuddy().subclass(superClass).implement(validInterfaces)
                    .method(ElementMatchers.any().and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class)))
                            .or(ElementMatchers.named("toString")).or(ElementMatchers.named("hashCode"))
                            .or(ElementMatchers.named("equals")))
                    .intercept(InvocationHandlerAdapter.of(handler)).make().load(safeClassLoader, loadingStrategy)
                    .getLoaded();

            Class<?>[] parameterTypes = findConstructorParameterTypes(superClass, constructorArgs);
            Constructor<?> constructor = proxyClass.getDeclaredConstructor(parameterTypes);
            T proxy = (T) constructor.newInstance(constructorArgs);
            handler.setProxyInstance(proxy);
            return proxy;
        } catch (Exception e) {
            throw new ProxyException("Failed to create class proxy with arguments", e);
        }
    }

    @Override
    public <T> T createClassJavaBeanProxy(Class<?> toJavaBeanify, Class<?>... interfaces) {
        LOG.debug("Creating JavaBean proxy for class {} with dynamic setter methods", toJavaBeanify.getName());

        try {
            Class<?> superClass = toJavaBeanify;
            if (InterceptableProxy.class.isAssignableFrom(toJavaBeanify)) {
                superClass = toJavaBeanify.getSuperclass();
            }

            Class<?>[] validInterfaces = makeAValidInterfaceArray(interfaces);
            ByteBuddyInvocationHandler handler = new ByteBuddyInvocationHandler();

            // Start with basic proxy builder
            ByteBuddy byteBuddy = new ByteBuddy();
            DynamicType.Builder<?> builder = byteBuddy.subclass(superClass).implement(validInterfaces);

            // Add dynamic setter methods for all getter methods
            for (Method method : toJavaBeanify.getMethods()) {
                String methodName = method.getName();

                // Generate setter for getter methods
                if ((methodName.startsWith("get") || methodName.startsWith("is")) && method.getParameterCount() == 0
                        && !methodName.equals("getClass")) {

                    String setterName = generateSetterName(methodName);
                    Class<?> returnType = method.getReturnType();

                    // Define setter method: setXxx(Type value) - but don't apply invocation handler yet
                    builder = builder.defineMethod(setterName, void.class, Modifier.PUBLIC).withParameter(returnType)
                            .withoutCode(); // Define method without implementation yet
                }
            }

            // Use the class loader of the target class to ensure visibility
            ClassLoader originalClassLoader = superClass.getClassLoader();
            ClassLoader safeClassLoader = getSafeClassLoader(originalClassLoader);
            ClassLoadingStrategy loadingStrategy = getLoadingStrategy(originalClassLoader);

            // Apply the invocation handler to ALL methods at once (including the dynamically added setters)
            Class<?> proxyClass = builder.method(ElementMatchers.any()).intercept(InvocationHandlerAdapter.of(handler))
                    .make().load(safeClassLoader, loadingStrategy).getLoaded();

            T proxy;
            try {
                proxy = (T) proxyClass.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                proxy = (T) ObjenesisHelper.newInstance(proxyClass);
            }

            handler.setProxyInstance(proxy);
            return proxy;
        } catch (Exception e) {
            throw new ProxyException("Failed to create class JavaBean proxy", e);
        }
    }

    @Override
    public <T> T createObjectProxyIfNeeded(T objectToIntercept, Class<?>... interfaces) {
        Class<?>[] validInterfaces = makeAValidInterfaceArray(interfaces);
        if (!Util.isNewProxyNeeded(objectToIntercept, validInterfaces)) {
            return objectToIntercept;
        }

        try {
            ByteBuddyInvocationHandler handler = new ByteBuddyInvocationHandler();

            // Use the class loader of the target object's class to ensure visibility
            ClassLoader originalClassLoader = objectToIntercept.getClass().getClassLoader();
            ClassLoader safeClassLoader = getSafeClassLoader(originalClassLoader);
            ClassLoadingStrategy loadingStrategy = getLoadingStrategy(originalClassLoader);

            Class<?> proxyClass = new ByteBuddy().subclass(objectToIntercept.getClass()).implement(validInterfaces)
                    .method(ElementMatchers.any().and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class)))
                            .or(ElementMatchers.named("toString")).or(ElementMatchers.named("hashCode"))
                            .or(ElementMatchers.named("equals")))
                    .intercept(InvocationHandlerAdapter.of(handler)).make().load(safeClassLoader, loadingStrategy)
                    .getLoaded();

            T proxy;
            try {
                proxy = (T) proxyClass.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                proxy = (T) ObjenesisHelper.newInstance(proxyClass);
            }

            handler.setProxyInstance(proxy);
            Proxy.getProxyInterface(proxy).addInterceptor(new InterceptorDelegator(objectToIntercept));
            return proxy;
        } catch (Exception e) {
            throw new ProxyException("Failed to create object proxy", e);
        }
    }

    @Override
    public String getEngineName() {
        return "ByteBuddy";
    }

    /**
     * ByteBuddy invocation handler that handles method calls and forwards them to the interceptor stack.
     */
    public static class ByteBuddyInvocationHandler implements InvocationHandler {

        private final ArrayDeque<Interceptor> interceptorStack = new ArrayDeque<>();
        private Object proxyInstance;

        private static Method addInterceptorMethod;
        private static Method removeInterceptorMethod;
        private static Method getInterceptorListMethod;

        static {
            try {
                addInterceptorMethod = InterceptableProxy.class.getMethod("addInterceptor", Interceptor.class);
                removeInterceptorMethod = InterceptableProxy.class.getMethod("removeInterceptor", Interceptor.class);
                getInterceptorListMethod = InterceptableProxy.class.getMethod("getInterceptorList");
            } catch (SecurityException | NoSuchMethodException e) {
                ProxyException.throwThisShouldNeverHappen(e);
            }
        }

        public void setProxyInstance(Object proxy) {
            this.proxyInstance = proxy;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Handle InterceptableProxy methods
            if (Util.methodSignatureEquals(method, addInterceptorMethod)) {
                interceptorStack.push((Interceptor) args[0]);
                return null;
            }
            if (Util.methodSignatureEquals(method, removeInterceptorMethod)) {
                interceptorStack.remove(args[0]);
                return null;
            }
            if (Util.methodSignatureEquals(method, getInterceptorListMethod)) {
                return interceptorStack;
            }

            // Handle interceptors with ByteBuddy limitations
            if (interceptorStack.isEmpty()) {
                // No interceptors - check if we can handle the method directly
                if (method.getDeclaringClass().equals(Object.class)) {
                    // Handle Object methods
                    switch (method.getName()) {
                    case "toString":
                        return proxy.getClass().getName() + "@" + System.identityHashCode(proxy);
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        Object[] methodArgs = args != null ? args : new Object[0];
                        return methodArgs.length > 0 ? proxy == methodArgs[0] : false;
                    default:
                        throw new UnsupportedOperationException("Cannot proceed to original method implementation");
                    }
                } else {
                    // For non-Object methods, ByteBuddy cannot proceed to original implementation
                    throw new UnsupportedOperationException(
                            "ByteBuddy engine does not support proceed() functionality due to InvocationHandlerAdapter limitations. "
                                    + "The proceed() method cannot access the original method implementation. "
                                    + "Consider using Javassist engine for interceptors that require proceed() functionality, "
                                    + "or modify your interceptor to not call invocation.invoke() when using ByteBuddy.");
                }
            } else {
                // Process interceptors manually with ByteBuddy-compatible invocation
                Object[] methodArgs = args != null ? args : new Object[0];
                return processInterceptorChain(proxy, method, methodArgs, interceptorStack.clone());
            }
        }

        /**
         * Process interceptor chain manually for ByteBuddy since we can't use standard Invocation
         */
        private Object processInterceptorChain(Object proxy, Method method, Object[] methodArgs,
                ArrayDeque<Interceptor> interceptors) throws Throwable {
            if (interceptors.isEmpty()) {
                // No more interceptors - would normally proceed to original method
                if (method.getDeclaringClass().equals(Object.class)) {
                    // Handle Object methods
                    switch (method.getName()) {
                    case "toString":
                        return proxy.getClass().getName() + "@" + System.identityHashCode(proxy);
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return methodArgs.length > 0 ? proxy == methodArgs[0] : false;
                    default:
                        throw new UnsupportedOperationException("Cannot proceed to original method implementation");
                    }
                } else {
                    // For non-Object methods, ByteBuddy cannot proceed to original implementation
                    throw new UnsupportedOperationException(
                            "ByteBuddy engine does not support proceed() functionality due to InvocationHandlerAdapter limitations. "
                                    + "The proceed() method cannot access the original method implementation. "
                                    + "Consider using Javassist engine for interceptors that require proceed() functionality, "
                                    + "or modify your interceptor to not call invocation.invoke() when using ByteBuddy.");
                }
            }

            // Get next interceptor and create a standard invocation for it
            Interceptor nextInterceptor = interceptors.pop();

            // Create a proceed method that throws the limitation error when called
            try {
                ByteBuddyLimitedProceed proceedInstance = new ByteBuddyLimitedProceed();
                // Find a proceed method that matches the original method signature
                Method proceedMethod = findMatchingProceedMethod(method, proceedInstance);
                // Create invocation with proceedInstance as target - this means getThis() returns proceedInstance
                // This is a ByteBuddy limitation: getThis() cannot return the proxy
                Invocation invocation = new Invocation(proceedInstance, method, proceedMethod, methodArgs,
                        interceptors);
                return nextInterceptor.intercept(invocation);
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new ProxyException("Interceptor threw exception", e);
                }
            }
        }

        /**
         * Find a proceed method that matches the signature of the intercepted method
         */
        private Method findMatchingProceedMethod(Method originalMethod, ByteBuddyLimitedProceed proceedInstance) {
            try {
                // Try to find a method with the exact same name and parameter types
                Class<?>[] parameterTypes = originalMethod.getParameterTypes();
                return proceedInstance.getClass().getMethod(originalMethod.getName(), parameterTypes);
            } catch (NoSuchMethodException e) {
                // If no exact match, use the generic proceed method
                try {
                    return proceedInstance.getClass().getMethod("proceed");
                } catch (NoSuchMethodException ex) {
                    throw new ProxyException("Cannot find suitable proceed method", ex);
                }
            }
        }
    }

    /**
     * Helper class for ByteBuddy proceed limitations
     */
    private static class ByteBuddyLimitedProceed {
        /**
         * Generic proceed method for methods with no parameters
         */
        public Object proceed() throws Exception {
            throw new UnsupportedOperationException(
                    "ByteBuddy engine does not support proceed() functionality due to InvocationHandlerAdapter limitations. "
                            + "The proceed() method cannot access the original method implementation. "
                            + "Consider using Javassist engine for interceptors that require proceed() functionality, "
                            + "or modify your interceptor to not call invocation.invoke() when using ByteBuddy.");
        }

        /**
         * Proceed method for common single-parameter methods like add(Object)
         */
        public Object add(Object obj) throws Exception {
            throw new UnsupportedOperationException(
                    "ByteBuddy engine does not support proceed() functionality due to InvocationHandlerAdapter limitations. "
                            + "The proceed() method cannot access the original method implementation. "
                            + "Consider using Javassist engine for interceptors that require proceed() functionality, "
                            + "or modify your interceptor to not call invocation.invoke() when using ByteBuddy.");
        }

        /**
         * Proceed method for common methods like size()
         */
        public Object size() throws Exception {
            throw new UnsupportedOperationException(
                    "ByteBuddy engine does not support proceed() functionality due to InvocationHandlerAdapter limitations. "
                            + "The proceed() method cannot access the original method implementation. "
                            + "Consider using Javassist engine for interceptors that require proceed() functionality, "
                            + "or modify your interceptor to not call invocation.invoke() when using ByteBuddy.");
        }
    }

    private static Class<?>[] findConstructorParameterTypes(Class<?> clazz, Object... args) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == args.length) {
                boolean matchingArgs = true;
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (args[i] == null && !parameterTypes[i].isPrimitive()) {
                        continue; // null should match all Object types
                    } else if (args[i] != null && parameterTypes[i].isAssignableFrom(args[i].getClass())) {
                        continue;
                    } else {
                        matchingArgs = false;
                        break;
                    }
                }
                if (matchingArgs) {
                    return parameterTypes;
                }
            }
        }
        throw new ProxyException(
                "Did not find any constructor matching the provided arguments: " + Arrays.asList(args));
    }

    private static Class<?>[] makeAValidInterfaceArray(Class<?>... interfaces) {
        Set<Class<?>> set = new LinkedHashSet<>(Arrays.asList(interfaces));
        set.remove(InterceptableProxy.class); // remove if exists
        set.add(InterceptableProxy.class); // needed by this library
        return set.toArray(new Class<?>[set.size()]);
    }

    /**
     * Generate setter method name from getter method name. e.g., "getName" -> "setName", "isEnabled" -> "setEnabled"
     */
    private String generateSetterName(String getterName) {
        if (getterName.startsWith("is")) {
            return "set" + getterName.substring(2);
        } else if (getterName.startsWith("get")) {
            return "set" + getterName.substring(3);
        }
        return "set" + getterName;
    }

    /**
     * Get a safe class loader that's never null and can be used with ByteBuddy
     */
    private ClassLoader getSafeClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            // Bootstrap class loader - use the current thread context class loader or this class's loader
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            return contextLoader != null ? contextLoader : ByteBuddyProxyEngineFactory.class.getClassLoader();
        }
        return classLoader;
    }

    /**
     * Get appropriate class loading strategy based on the class loader
     */
    private ClassLoadingStrategy getLoadingStrategy(ClassLoader originalClassLoader) {
        if (originalClassLoader == null) {
            // For bootstrap class loader classes, use child first strategy instead of injection
            return ClassLoadingStrategy.Default.CHILD_FIRST;
        }
        return ClassLoadingStrategy.Default.INJECTION;
    }
}