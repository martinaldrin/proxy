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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.ericsson.commonlibrary.proxy.helpobjects.ConstructorThrowsException;
import com.ericsson.commonlibrary.proxy.helpobjects.ContainsThrowsException;
import com.ericsson.commonlibrary.proxy.helpobjects.NonEmptyConstructor;
import com.ericsson.commonlibrary.proxy.helpobjects.NonEmptyConstructorWithObject;
import com.ericsson.commonlibrary.proxy.helpobjects.NonEmptyConstructorWithObject.WrapperObject;
import com.ericsson.commonlibrary.proxy.helpobjects.Size10;

public class InterceptObjectTest extends BaseProxyEngineTest {

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

    Interceptor returnFalseInterceptorWithoutMethodFiltering = new Interceptor() {

        @Override
        public Object intercept(Invocation data) throws Throwable {
            return false;
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

    Interceptor emptyInterceptor = new Interceptor() {

        @Override
        public Object intercept(Invocation data) throws Throwable {
            return data.invoke();
        }
    };

    @Test(dataProvider = "proxyEngines")
    public void multipleOfSameInterceptorThree(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final List<Integer> interceptorCreatedList = new ArrayList<Integer>();

        Interceptor addInterceptor = new Interceptor() {

            int i = 2;

            @Override
            public Object intercept(Invocation data) throws Throwable {
                if (data.getMethod().getName().equals("add")) {
                    i *= 2;
                    interceptorCreatedList.add(i);
                    Object returnValue = data.invoke();
                    i -= 1;
                    interceptorCreatedList.add(i);
                    return returnValue;
                }
                return data.invoke();
            }
        };

        List<String> list = Proxy.intercept(new ArrayList<String>(), addInterceptor);
        Proxy.intercept(list, addInterceptor);
        Proxy.intercept(list, addInterceptor);
        list.add("hello");

        List<Integer> correctList = new ArrayList<Integer>();
        correctList.add(4);
        correctList.add(8);
        correctList.add(16);
        correctList.add(15);
        correctList.add(14);
        correctList.add(13);

        assertEquals(interceptorCreatedList, correctList);
    }

    @Test(dataProvider = "proxyEngines")
    public void multipleOfSameInterceptorTwo(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final List<Integer> interceptorCreatedList = new ArrayList<Integer>();

        Interceptor addInterceptor = new Interceptor() {

            int i = 2;

            @Override
            public Object intercept(Invocation data) throws Throwable {
                if (data.getMethod().getName().equals("add")) {
                    i *= 2;
                    interceptorCreatedList.add(i);
                    Object returnValue = data.invoke();
                    i -= 1;
                    interceptorCreatedList.add(i);
                    return returnValue;
                }
                return data.invoke();
            }
        };

        List<String> list = Proxy.intercept(new ArrayList<String>(), addInterceptor);
        Proxy.intercept(list, addInterceptor);

        list.add("hello");

        List<Integer> correctList = new ArrayList<Integer>();
        correctList.add(4);
        correctList.add(8);
        correctList.add(7);
        correctList.add(6);

        assertEquals(interceptorCreatedList, correctList);
    }

    @Test(dataProvider = "proxyEngines")
    public void multipleOfSameInterceptorButAnotherInBetween(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final List<Integer> interceptorCreatedList = new ArrayList<Integer>();

        Interceptor addInterceptor = new Interceptor() {

            int i = 2;

            @Override
            public Object intercept(Invocation data) throws Throwable {
                if (data.getMethod().getName().equals("add")) {
                    i *= 2;
                    interceptorCreatedList.add(i);
                    Object returnValue = data.invoke();
                    i -= 1;
                    interceptorCreatedList.add(i);
                    return returnValue;
                }
                return data.invoke();
            }
        };

        List<String> list = Proxy.intercept(new ArrayList<String>(), addInterceptor);
        Proxy.intercept(list, emptyInterceptor);
        Proxy.intercept(list, addInterceptor);

        list.add("hello");

        List<Integer> correctList = new ArrayList<Integer>();
        correctList.add(4);
        correctList.add(8);
        correctList.add(7);
        correctList.add(6);

        assertEquals(interceptorCreatedList, correctList);
    }

    @Test(dataProvider = "proxyEngines")
    public void oneInterceptor(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        List<String> list = Proxy.intercept(new ArrayList<String>(), size10Interceptor);
        assertEquals(list.size(), 10);
    }

    @Test(dataProvider = "proxyEngines")
    public void proxyOperatesOnPassedObject(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add("hello");
        arrayList.add("world");
        assertEquals(arrayList.size(), 2);

        List<String> listproxy = Proxy.intercept(arrayList, sizeTimesTwoInterceptor);
        assertEquals(arrayList.size(), 2);
        assertEquals(listproxy.size(), 4);

        listproxy.add("hello again");
        assertEquals(arrayList.size(), 3);
        assertEquals(listproxy.size(), 6);

        listproxy.add("hello again");
        assertEquals(arrayList.size(), 4);
        assertEquals(listproxy.size(), 8);

    }

    @Test(dataProvider = "proxyEngines")
    public void ableToInterceptObjectWithNonEmptyConstructor(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        NonEmptyConstructor obj = Proxy.intercept(new NonEmptyConstructor("Hello"), emptyInterceptor);
    }

    @Test(dataProvider = "proxyEngines")
    public void ableToInterceptObjectWithNonEmptyConstructorObjectParam(ProxyConfiguration.Engine engine)
            throws Exception {
        setEngine(engine);
        Size10 size10 = new Size10();
        WrapperObject wrapperObject = new NonEmptyConstructorWithObject.WrapperObject(size10);
        NonEmptyConstructorWithObject nonEmptyConstructorWithObject = new NonEmptyConstructorWithObject(wrapperObject);
        NonEmptyConstructorWithObject obj = Proxy.intercept(nonEmptyConstructorWithObject, emptyInterceptor);
    }

    @Test(dataProvider = "proxyEngines")
    public void defaultValueIfNoInterceptorChangesAnything(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        NonEmptyConstructor obj = Proxy.intercept(new NonEmptyConstructor("Hello1"), emptyInterceptor,
                NonEmptyConstructor.class.getMethod("get"));
        assertEquals(obj.get(), "Hello1");
    }

    @Test(dataProvider = "proxyEngines")
    public void oneSingleMethodInterceptor(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        List<String> list = Proxy.intercept(new ArrayList<String>(), return10InterceptorWithoutMethodFiltering,
                List.class.getMethod("size"));
        assertEquals(list.size(), 10);
        assertTrue(list.add("hello"));
    }

    @Test(dataProvider = "proxyEngines")
    public void twoSingleMethodInterceptor(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        List<String> list = Proxy.intercept(new ArrayList<String>(), return10InterceptorWithoutMethodFiltering,
                List.class.getMethod("size"));
        assertEquals(list.size(), 10);
        assertTrue(list.add("hello"));

        list = Proxy.intercept(list, returnFalseInterceptorWithoutMethodFiltering,
                List.class.getMethod("add", Object.class));

        assertEquals(list.size(), 10);
        assertFalse(list.add("hello"));

    }

    @Test(dataProvider = "proxyEngines")
    public void testAddTimerToMethods(ProxyConfiguration.Engine engine) {
        setEngine(engine);
        List<String> list = Proxy.addTimerToMethods(new ArrayList<String>());
        list.size();
        list.add("shortstring");
        list.add("LOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOONNNNNNNNNNGGGGGGGGGGGGGGGGGGGGG");
        list.get(0);
    }

    @Test(expectedExceptions = ProxyException.class, dataProvider = "proxyEngines")
    public void throwsExceptionFinalObjects(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        String string = Proxy.intercept("FINALSTRING", emptyInterceptor);
        fail();
    }

    @Test(dataProvider = "proxyEngines")
    public void constructorNeverCalledOnConstructionOfProxyObjects(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        ConstructorThrowsException obj = Proxy.intercept(
                new ConstructorThrowsException(new StringBuilder(ConstructorThrowsException.DO_NOT_CRASH)),
                emptyInterceptor);
    }

    @Test(expectedExceptions = NullPointerException.class, dataProvider = "proxyEngines")
    public void objectMethodThrowingException(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        ContainsThrowsException obj = Proxy.intercept(new ContainsThrowsException(), emptyInterceptor);
        obj.contains("");
    }

    @Test(dataProvider = "proxyEngines")
    public void objectMethodThrowingExceptionAndInterceptorTryingToCatchIt(ProxyConfiguration.Engine engine)
            throws Exception {
        setEngine(engine);
        ContainsThrowsException obj = Proxy.intercept(new ContainsThrowsException(), new Interceptor() {

            @Override
            public Object intercept(Invocation invocation) throws Throwable {
                try {
                    return invocation.invoke();
                } catch (Exception e) {
                    return true;
                }
            }
        });
        Assert.assertTrue(obj.contains(""));
    }

    @Test(dataProvider = "proxyEngines")
    public void twoInterceptors(ProxyConfiguration.Engine engine) {
        setEngine(engine);
        List<String> list = Proxy.intercept(new ArrayList<String>(), size10Interceptor);
        Proxy.intercept(list, sizeTimesTwoInterceptor);
        assertEquals(list.size(), 20);

        List<String> list2 = Proxy.intercept(new ArrayList<String>(), sizeTimesTwoInterceptor);
        Proxy.intercept(list2, size10Interceptor);
        assertEquals(list2.size(), 10);
    }

    @Test(dataProvider = "proxyEngines")
    public void SameInterceptorOnTwoObjects(ProxyConfiguration.Engine engine) {
        setEngine(engine);
        List<String> list = Proxy.intercept(new ArrayList<String>(), size10Interceptor);
        assertEquals(list.size(), 10);

        List<String> list2 = Proxy.intercept(new ArrayList<String>(), size10Interceptor);
        assertEquals(list2.size(), 10);
        assertEquals(list.size(), 10);
    }

}
