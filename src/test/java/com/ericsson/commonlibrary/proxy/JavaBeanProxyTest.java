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
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.ericsson.commonlibrary.proxy.helpobjects.PersonBean;
import com.ericsson.commonlibrary.proxy.helpobjects.PersonBeanAbstract;
import com.ericsson.commonlibrary.proxy.helpobjects.PersonBeanClass;

public class JavaBeanProxyTest extends BaseProxyEngineTest {

    @Test(dataProvider = "proxyEngines")
    public void javaBeanTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBean person = Proxy.javaBean(PersonBean.class);
        person.setName("nisse");

        assertEquals(person.getName(), "nisse");
        person.setName("elis");
        assertEquals(person.getName(), "elis");

        assertEquals(person.getAge(), 0);
        person.setAge(20);
        assertEquals(person.getAge(), 20);
    }

    @Test(dataProvider = "proxyEngines")
    public void multipleCallsToGetNameInterfaceTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBean person = Proxy.javaBean(PersonBean.class);
        person.setName("123");
        for (int i = 0; i < 1000; i++) {
            System.out.println(getCurrentEngineName() + " " + i + person.getName());
        }
    }

    @Test(dataProvider = "proxyEngines")
    public void multipleCallsToGetNameAbstractClassTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBeanAbstract person = Proxy.javaBean(PersonBeanAbstract.class);
        person.setName("123");
        for (int i = 0; i < 1000; i++) {
            System.out.println(getCurrentEngineName() + " " + i + person.getName());
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, dataProvider = "proxyEngines")
    public void multipleCallsToGetNameClassTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBeanClass person = Proxy.javaBean(PersonBeanClass.class);
    }

    @Test(dataProvider = "proxyEngines")
    public void multipleCallsToToStringInterfaceTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBean person = Proxy.javaBean(PersonBean.class);

        for (int i = 0; i < 17; i++) {
            System.out.println(getCurrentEngineName() + " " + i + person);
        }
        Thread.sleep(5000);
        for (int i = 0; i < 1000; i++) {
            System.out.println(getCurrentEngineName() + " " + i + person);
        }
    }

    @Test(dataProvider = "proxyEngines")
    public void multipleCallsToToStringAbstractClassTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBeanAbstract person = Proxy.javaBean(PersonBeanAbstract.class);
        for (int i = 0; i < 1000; i++) {
            System.out.println(getCurrentEngineName() + " " + i + person);
        }
    }

    @Test(dataProvider = "proxyEngines")
    public void multipleCallsToHashCodeInterfaceTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBean person = Proxy.javaBean(PersonBean.class);
        for (int i = 0; i < 1000; i++) {
            System.out.println(getCurrentEngineName() + " " + i + person.hashCode());
        }
    }

    @Test(dataProvider = "proxyEngines")
    public void multipleCallsToHashCodeAbstractTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBeanAbstract person = Proxy.javaBean(PersonBeanAbstract.class);
        for (int i = 0; i < 1000; i++) {
            System.out.println(getCurrentEngineName() + " " + i + person.hashCode());
        }
    }

    @Test(dataProvider = "proxyEngines")
    public void multipleCallsToEqualsInterfaceTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBean person = Proxy.javaBean(PersonBean.class);
        for (int i = 0; i < 1000; i++) {
            System.out.println(getCurrentEngineName() + " " + i + person.equals(null));
        }
    }

    @Test(dataProvider = "proxyEngines")
    public void multipleCallsToEqualsAbstractTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBeanAbstract person = Proxy.javaBean(PersonBeanAbstract.class);
        for (int i = 0; i < 1000; i++) {
            System.out.println(getCurrentEngineName() + " " + i + person.equals(null));
        }
    }

    @Test(dataProvider = "proxyEngines")
    public void multipleCallsToToStringHashcodeEqualsInterfaceTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBean person = Proxy.javaBean(PersonBean.class);
        for (int i = 0; i < 10; i++) {
            System.out.println(getCurrentEngineName() + " " + i + person);
        }
        for (int i = 0; i < 10; i++) {
            System.out.println(getCurrentEngineName() + " " + i + person.hashCode());
        }
        for (int i = 0; i < 10; i++) {
            System.out.println(getCurrentEngineName() + " " + i + person.equals(null));
        }

    }

    @Test(dataProvider = "proxyEngines")
    public void javaBeanNullDefaultTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBean person = Proxy.javaBean(PersonBean.class);

        assertNull(person.getName());
        assertNull(person.getPhoneNumber());
        assertNull(person.getAgeInteger());

        person.setName("elis");
        person.setPhoneNumber("123");
        person.setAgeInteger(20);

        assertEquals(person.getName(), "elis");
        assertEquals(person.getPhoneNumber(), "123");
        assertEquals(person.getAgeInteger(), new Integer(20));
    }

    @Test(dataProvider = "proxyEngines")
    public void javaBeanPrimitivesDefaultTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBean person = Proxy.javaBean(PersonBean.class);

        assertEquals(person.getAge(), 0);
        person.setAge(20);
        assertEquals(person.getAge(), 20);
    }

    @Test(dataProvider = "proxyEngines")
    public void javaBeanAbstractTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBeanAbstract person = Proxy.javaBean(PersonBeanAbstract.class);
        person.setName("nisse");
        assertEquals(person.getName(), "nisse");
        person.setName("elis");
        assertEquals(person.getName(), "elis");
    }

    @Test(dataProvider = "proxyEngines")
    public void javaBeanAbstractTest2(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBeanAbstract person = Proxy.javaBean(PersonBeanAbstract.class);

        person.setMale(true);

        assertEquals(person.isMale(), true);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, dataProvider = "proxyEngines")
    public void throwsIllegalArgumentExceptionOnRegularClassesTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final ArrayList list = Proxy.javaBean(ArrayList.class);
    }

    @Test(dataProvider = "proxyEngines")
    public void javaBeanWithInterceptorTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBean person = Proxy.javaBean(PersonBean.class);
        person.setName("emma");
        person.setMale(false);
        assertEquals(person.getName(), "emma");
        assertFalse(person.isMale());

        Proxy.intercept(person, new Interceptor() {

            @Override
            public Object intercept(Invocation data) throws Throwable {

                if (data.getParameters().length > 0 && data.getParameters()[0].equals("elis")) {
                    data.getParameters()[0] = "elis edlund";
                    PersonBean.class.cast(data.getThis()).setMale(true);
                }
                return data.invoke();
            }
        });
        person.setName("elis");
        assertEquals(person.getName(), "elis edlund");
        assertTrue(person.isMale());
        assertFalse(person.toString().isEmpty());
    }

    @Test(dataProvider = "proxyEngines")
    public void javaBeanWithDelegatorTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBean person = Proxy.javaBean(PersonBean.class);
        person.setName("erik");
        person.setMale(true);
        assertEquals(person.getName(), "erik");
        assertTrue(person.isMale());

        // only allowed name is elis
        Proxy.delegate(person, new Object() {

            public String getName() {
                return "elis";
            }
        });
        assertNotEquals(person.getName(), "erik");
        assertEquals(person.getName(), "elis");
    }

    @Test(dataProvider = "proxyEngines")
    public void addJavaBeanToOtherObjectTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBean person = Proxy.javaBean(PersonBean.class);
        person.setName("elis");
        person.setMale(true);
        assertEquals(person.getName(), "elis");
        assertTrue(person.isMale());

        final ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add("hello");
        arrayList.add("world");
        assertEquals(arrayList.size(), 2);

        final List<String> listProxyAndPerson = Proxy.delegate(arrayList, person);

        assertEquals(listProxyAndPerson.size(), 2);

        final PersonBean person2 = (PersonBean) listProxyAndPerson;
        assertEquals(person2.getName(), "elis");
        assertEquals(((List) person2).get(0), "hello");
    }

    @Test(dataProvider = "proxyEngines")
    public void addJavaBeanToExistingProxyTest(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBean person = Proxy.javaBean(PersonBean.class);
        person.setName("elis");
        person.setMale(true);
        assertEquals(person.getName(), "elis");
        assertTrue(person.isMale());

        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add("hello");
        arrayList.add("world");
        assertEquals(arrayList.size(), 2);
        arrayList = Proxy.addTimerToMethods(arrayList);

        final List<String> listProxyAndPerson = Proxy.delegate(arrayList, person);

        assertEquals(listProxyAndPerson.size(), 2);

        final PersonBean person2 = (PersonBean) listProxyAndPerson;
        assertEquals(person2.getName(), "elis");
        assertEquals(((List) person2).get(0), "hello");
    }

    @Test(dataProvider = "proxyEngines")
    public void addJavaBeanToExistingProxyTest2(ProxyConfiguration.Engine engine) throws Exception {
        setEngine(engine);
        final PersonBean person = Proxy.javaBean(PersonBean.class);
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList = Proxy.addTimerToMethods(arrayList);
        final List<String> listProxyAndPerson = Proxy.delegate(arrayList, person);
    }

}
