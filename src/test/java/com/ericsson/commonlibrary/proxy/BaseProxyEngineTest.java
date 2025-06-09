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

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

/**
 * Base test class that provides data providers for testing both proxy engines. All proxy tests should extend this class
 * to ensure they run with both Javassist and ByteBuddy engines.
 *
 * @author Proxy Team
 */
public abstract class BaseProxyEngineTest {

    private ProxyConfiguration.Engine originalEngine;

    /**
     * Data provider that supplies both proxy engines for parameterized tests.
     *
     * @return array of proxy engines to test with
     */
    @DataProvider(name = "proxyEngines")
    public Object[][] proxyEngines() {
        return new Object[][] { { ProxyConfiguration.Engine.JAVASSIST }, { ProxyConfiguration.Engine.BYTEBUDDY } };
    }

    /**
     * Data provider that supplies engine names for display purposes.
     *
     * @return array of engine names
     */
    @DataProvider(name = "proxyEngineNames")
    public Object[][] proxyEngineNames() {
        return new Object[][] { { "Javassist" }, { "ByteBuddy" } };
    }

    /**
     * Sets up the proxy engine before each test method. Saves the original engine configuration for restoration.
     */
    @BeforeMethod
    protected void setupEngine() {
        originalEngine = ProxyConfiguration.getEngine();
    }

    /**
     * Restores the original proxy engine configuration after each test method.
     */
    @AfterMethod
    protected void restoreEngine() {
        if (originalEngine != null) {
            ProxyConfiguration.setEngine(originalEngine);
        } else {
            ProxyConfiguration.reset();
        }
        ProxyEngineProvider.clearCache();
    }

    /**
     * Sets the proxy engine for the current test.
     *
     * @param engine
     *            the engine to use
     */
    protected void setEngine(ProxyConfiguration.Engine engine) {
        ProxyConfiguration.setEngine(engine);
    }

    /**
     * Gets the name of the currently configured engine.
     *
     * @return the engine name
     */
    protected String getCurrentEngineName() {
        return ProxyConfiguration.getEngine().getName();
    }
}