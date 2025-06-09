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

/**
 * Interface for proxy creation engines. Implementations provide different bytecode manipulation strategies.
 *
 * @author Proxy Team
 */
interface ProxyEngineFactory {

    /**
     * Creates a new interface proxy that implements one or more interfaces.
     *
     * @param <T>
     *            the type of the proxy
     * @param interfaces
     *            the interfaces to implement
     *
     * @return a new proxy instance
     */
    <T> T createInterfaceProxy(Class<?>... interfaces);

    /**
     * Creates a new interface proxy that acts as a JavaBean.
     *
     * @param <T>
     *            the type of the proxy
     * @param toJavaBeanify
     *            the interface to convert to a JavaBean
     *
     * @return a new JavaBean proxy instance
     */
    <T> T createInterfaceJavaBeanProxy(Class<?> toJavaBeanify);

    /**
     * Creates a new class proxy that extends a concrete class.
     *
     * @param <T>
     *            the type of the proxy
     * @param classToIntercept
     *            the class to extend
     * @param interfaces
     *            additional interfaces to implement
     *
     * @return a new proxy instance
     */
    <T> T createClassProxy(Class<T> classToIntercept, Class<?>... interfaces);

    /**
     * Creates a new class proxy with specific constructor arguments.
     *
     * @param <T>
     *            the type of the proxy
     * @param classToIntercept
     *            the class to extend
     * @param constructorArgs
     *            arguments to pass to the constructor
     *
     * @return a new proxy instance
     */
    <T> T createClassProxyWithArguments(Class<T> classToIntercept, Object... constructorArgs);

    /**
     * Creates a new class proxy that acts as a JavaBean.
     *
     * @param <T>
     *            the type of the proxy
     * @param toJavaBeanify
     *            the class to convert to a JavaBean
     * @param interfaces
     *            additional interfaces to implement
     *
     * @return a new JavaBean proxy instance
     */
    <T> T createClassJavaBeanProxy(Class<?> toJavaBeanify, Class<?>... interfaces);

    /**
     * Creates a new object proxy if needed.
     *
     * @param <T>
     *            the type of the object
     * @param objectToIntercept
     *            the object to potentially proxy
     * @param interfaces
     *            additional interfaces to implement
     *
     * @return a proxy instance or the original object
     */
    <T> T createObjectProxyIfNeeded(T objectToIntercept, Class<?>... interfaces);

    /**
     * Returns the name of this proxy engine.
     *
     * @return the engine name
     */
    String getEngineName();
}