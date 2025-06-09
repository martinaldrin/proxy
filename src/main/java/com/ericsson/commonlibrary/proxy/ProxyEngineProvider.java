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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for proxy engine factories. Manages the selection and instantiation of different proxy engines.
 *
 * @author Proxy Team
 */
final class ProxyEngineProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyEngineProvider.class);

    private static volatile ProxyEngineFactory javassistFactory;
    private static volatile ProxyEngineFactory byteBuddyFactory;

    private ProxyEngineProvider() {
        // Utility class
    }

    /**
     * Gets the proxy engine factory for the currently configured engine type.
     *
     * @return the current proxy engine factory
     */
    static ProxyEngineFactory getCurrentFactory() {
        ProxyConfiguration.Engine engineType = ProxyConfiguration.getEngine();
        return getFactory(engineType);
    }

    /**
     * Gets a specific proxy engine factory.
     *
     * @param engineType
     *            the type of engine to get
     *
     * @return the proxy engine factory
     */
    static ProxyEngineFactory getFactory(ProxyConfiguration.Engine engineType) {
        switch (engineType) {
        case JAVASSIST:
            return getJavassistFactory();
        case BYTEBUDDY:
            return getByteBuddyFactory();
        default:
            throw new ProxyException("Unsupported engine type: " + engineType);
        }
    }

    private static ProxyEngineFactory getJavassistFactory() {
        if (javassistFactory == null) {
            synchronized (ProxyEngineProvider.class) {
                if (javassistFactory == null) {
                    try {
                        javassistFactory = new JavassistProxyEngineFactory();
                        LOG.debug("Initialized Javassist proxy engine factory");
                    } catch (Exception e) {
                        throw new ProxyException("Failed to initialize Javassist proxy engine factory", e);
                    }
                }
            }
        }
        return javassistFactory;
    }

    private static ProxyEngineFactory getByteBuddyFactory() {
        if (byteBuddyFactory == null) {
            synchronized (ProxyEngineProvider.class) {
                if (byteBuddyFactory == null) {
                    try {
                        byteBuddyFactory = new ByteBuddyProxyEngineFactory();
                        LOG.debug("Initialized ByteBuddy proxy engine factory");
                    } catch (Exception e) {
                        throw new ProxyException("Failed to initialize ByteBuddy proxy engine factory", e);
                    }
                }
            }
        }
        return byteBuddyFactory;
    }

    /**
     * Clears all cached factory instances. Useful for testing or when configuration changes.
     */
    static void clearCache() {
        synchronized (ProxyEngineProvider.class) {
            javassistFactory = null;
            byteBuddyFactory = null;
            LOG.debug("Cleared proxy engine factory cache");
        }
    }
}