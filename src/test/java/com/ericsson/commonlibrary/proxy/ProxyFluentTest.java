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

import static com.ericsson.commonlibrary.proxy.Proxy.with;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.ericsson.commonlibrary.proxy.helpobjects.AddBlocker;
import com.ericsson.commonlibrary.proxy.helpobjects.NonEmptyConstructor;
import com.ericsson.commonlibrary.proxy.helpobjects.NonEmptyConstructorCharSequence;
import com.ericsson.commonlibrary.proxy.helpobjects.NonEmptyConstructorThrowingException;
import com.ericsson.commonlibrary.proxy.helpobjects.NonEmptyConstructorWithObject;
import com.ericsson.commonlibrary.proxy.helpobjects.NonEmptyConstructorWithObject.WrapperObject;
import com.ericsson.commonlibrary.proxy.helpobjects.Size10;

public class ProxyFluentTest extends BaseProxyEngineTest {

    Interceptor emptyInterceptor = new Interceptor() {

        @Override
        public Object intercept(Invocation data) throws Throwable {
            return data.invoke();
        }
    };

    @Test(dataProvider = "proxyEngines")
    public void constructorArgumentTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        String string = "someString";
        NonEmptyConstructor nonEmptyConstructor = with(NonEmptyConstructor.class, string).interceptAll(emptyInterceptor)
                .get();
        Assert.assertEquals(nonEmptyConstructor.get(), string);
    }

    @Test(dataProvider = "proxyEngines")
    public void constructorArgumentHandlingCompatibleButMissmatchingTypesTest(ProxyConfiguration.Engine engine)
            throws Exception {
        setEngine(engine);

        String string = "someString";
        NonEmptyConstructorCharSequence nonEmptyConstructor = with(NonEmptyConstructorCharSequence.class, string)
                .interceptAll(emptyInterceptor).get();
        Assert.assertEquals(nonEmptyConstructor.get(), string);
    }

    @Test(dataProvider = "proxyEngines")
    public void constructorArgumentHandlingCompatibleButMissmatchingTypesPackagePrivateTest(
            ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);

        ArrayList<Object> list = new ArrayList<Object>();
        NonEmptyConstructorCharSequence nonEmptyConstructor = with(NonEmptyConstructorCharSequence.class, list)
                .interceptAll(emptyInterceptor).get();
        Assert.assertEquals(nonEmptyConstructor.get(), "waslist");
    }

    @Test(dataProvider = "proxyEngines")
    public void notCallingConstructorWithArgumentTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        NonEmptyConstructor nonEmptyConstructor = with(NonEmptyConstructor.class).interceptAll(emptyInterceptor).get();
        Assert.assertEquals(nonEmptyConstructor.get(), null);
    }

    @Test(dataProvider = "proxyEngines")
    public void NotCallingConstructorWithArgument2Test(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        NonEmptyConstructorThrowingException nonEmptyConstructor = with(NonEmptyConstructorThrowingException.class)
                .interceptAll(emptyInterceptor).get();
        Assert.assertEquals(nonEmptyConstructor.get(), null);
    }

    @Test(dataProvider = "proxyEngines")
    public void constructorObjectArgumentTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        Size10 size10 = new Size10();
        WrapperObject wrapperObject = new NonEmptyConstructorWithObject.WrapperObject(size10);
        NonEmptyConstructorWithObject object = with(NonEmptyConstructorWithObject.class, wrapperObject)
                .interceptAll(emptyInterceptor).get();
        Assert.assertEquals(object.getWrapperObject(), wrapperObject);
    }

    @Test(expectedExceptions = ProxyException.class, dataProvider = "proxyEngines")
    public void constructorArgumentWithInvalidArgTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        NonEmptyConstructor nonEmptyConstructor = with(NonEmptyConstructor.class, 1).interceptAll(emptyInterceptor)
                .get();
    }

    Interceptor size10Interceptor = new Interceptor() {

        @Override
        public Object intercept(Invocation data) throws Throwable {
            if (data.getMethod().getName().equals("size")) {
                return 10;
            }
            return data.invoke();
        }
    };
    Interceptor return10InterceptorWithoutMethodFiltering = new Interceptor() {

        @Override
        public Object intercept(Invocation data) throws Throwable {
            return 10;
        }
    };

    Interceptor sizeTimesTwoInterceptor = new Interceptor() {

        @Override
        public Object intercept(Invocation data) throws Throwable {
            if (data.getMethod().getName().equals("size")) {
                return ((Integer) data.invoke()) * 2;
            }
            return data.invoke();
        }
    };

    @Test(dataProvider = "proxyEngines")
    public void oneInterceptor(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        List<String> list = with(new ArrayList<String>()).interceptAll(size10Interceptor).get();
        assertEquals(list.size(), 10);
    }

    @Test(dataProvider = "proxyEngines")
    public void twoInterceptors(ProxyConfiguration.Engine engine) {
        setEngine(engine);
        List<String> list = with(new ArrayList<String>()).interceptAll(size10Interceptor)
                .interceptAll(sizeTimesTwoInterceptor).get();
        assertEquals(list.size(), 20);

        List<String> list2 = with(new ArrayList<String>()).interceptAll(sizeTimesTwoInterceptor)
                .interceptAll(size10Interceptor).get();
        assertEquals(list2.size(), 10);
    }

    @Test(dataProvider = "proxyEngines")
    public void oneSingleMethodInterceptor(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        List<String> list = with(new ArrayList<String>())
                .interceptMethod(return10InterceptorWithoutMethodFiltering, List.class.getMethod("size")).get();
        assertEquals(list.size(), 10);
        assertTrue(list.add("hello"));
    }

    private class MyInvoctionHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return "" + method.getName() + args[0];
        }
    }

    @Test(dataProvider = "proxyEngines")
    public void invocationHandlerAdapterTest(ProxyConfiguration.Engine engine) {
        setEngine(engine);
        List<String> list = with(new ArrayList<String>()).interceptAll(new MyInvoctionHandler()).get();
        assertEquals(list.get(5), "get5");
        assertEquals(list.remove(0), "remove0");
    }

    @Test
    public void invocationHandlerAdapterMethodTest() throws SecurityException, NoSuchMethodException {
        List<String> list = with(new ArrayList<String>())
                .interceptMethod(new MyInvoctionHandler(), List.class.getMethod("get", int.class)).get();
        assertEquals(list.get(5), "get5");
        try {
            list.remove(0);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // this is expected!
        }
    }

    @Test(dataProvider = "proxyEngines")
    public void delegateBlockAdd(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        List<String> list = with(new ArrayList<String>()).delegate(new AddBlocker()).get();
        list.add("hello");
        assertEquals(list.size(), 0);
    }

    @Test(dataProvider = "proxyEngines")
    public void delegateSize10(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        List<String> list = with(new ArrayList<String>()).delegate(new Size10()).get();
        assertEquals(list.size(), 10);
    }

    @Test(dataProvider = "proxyEngines")
    public void interfaceInterception(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        List<String> list = with(List.class).interceptAll(size10Interceptor).get();
        assertEquals(list.size(), 10);
    }

    @Test(expectedExceptions = ProxyException.class, expectedExceptionsMessageRegExp = ".*List does not need constructor arguments.*", dataProvider = "proxyEngines")
    public void interfaceInterceptionWithConstructorArgs(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        with(List.class, "");
    }
}
