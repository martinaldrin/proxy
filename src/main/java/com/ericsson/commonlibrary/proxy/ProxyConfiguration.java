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
 * Configuration class for the proxy library. Manages the selection of proxy engines and other library-wide settings.
 *
 * @author Proxy Team
 */
public final class ProxyConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyConfiguration.class);

    private static final String ENGINE_PROPERTY = "proxy.engine";
    private static final String DEFAULT_ENGINE = "javassist";

    /**
     * Supported proxy engines.
     */
    public enum Engine {
        JAVASSIST("javassist"), BYTEBUDDY("bytebuddy");

        private final String name;

        Engine(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Engine fromName(String name) {
            if (name == null) {
                return JAVASSIST; // default
            }
            for (Engine engine : values()) {
                if (engine.name.equalsIgnoreCase(name.trim())) {
                    return engine;
                }
            }
            LOG.warn("Unknown proxy engine '{}', falling back to default '{}'", name, DEFAULT_ENGINE);
            return JAVASSIST;
        }
    }

    private static volatile Engine currentEngine;

    static {
        // Initialize from system property
        String engineName = System.getProperty(ENGINE_PROPERTY, DEFAULT_ENGINE);
        currentEngine = Engine.fromName(engineName);
        LOG.debug("Proxy engine initialized to: {}", currentEngine.getName());
    }

    private ProxyConfiguration() {
        // Utility class
    }

    /**
     * Gets the currently configured proxy engine.
     *
     * @return the current engine
     */
    public static Engine getEngine() {
        return currentEngine;
    }

    /**
     * Sets the proxy engine to use. This affects all subsequent proxy creations.
     *
     * @param engine
     *            the engine to use
     */
    public static void setEngine(Engine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("Engine cannot be null");
        }
        currentEngine = engine;
        LOG.debug("Proxy engine changed to: {}", engine.getName());
    }

    /**
     * Sets the proxy engine using its name.
     *
     * @param engineName
     *            the name of the engine to use
     */
    public static void setEngine(String engineName) {
        setEngine(Engine.fromName(engineName));
    }

    /**
     * Resets the configuration to use the default engine or the one specified by the system property.
     */
    public static void reset() {
        String engineName = System.getProperty(ENGINE_PROPERTY, DEFAULT_ENGINE);
        setEngine(Engine.fromName(engineName));
    }
}