package examples;

import static com.ericsson.commonlibrary.proxy.Proxy.with;

import com.ericsson.commonlibrary.proxy.ProxyConfiguration;

/**
 * Example demonstrating the pluggable proxy engine architecture. The same API works seamlessly with different bytecode
 * manipulation engines.
 */
public class ByteBuddyEngineExample {

    public static class SomeImpl { // sample class

        public void log(String log) {
            System.out.println(log);
        }
    }

    public static void main(String[] args) throws SecurityException, NoSuchMethodException {

        // Test with default engine (Javassist)
        System.out.println("=== Testing with Javassist engine ===");
        ProxyConfiguration.setEngine(ProxyConfiguration.Engine.JAVASSIST);

        SomeImpl obj1 = with(SomeImpl.class).interceptAll(i -> {
            System.out.println("Javassist - before method: " + i.getMethodName() + " param: " + i.getParameter0());
            return i.invoke();
        }).get();
        obj1.log("Hello Javassist!");

        // Test with ByteBuddy engine - Same API, different implementation!
        System.out.println("\n=== Testing with ByteBuddy engine ===");
        ProxyConfiguration.setEngine(ProxyConfiguration.Engine.BYTEBUDDY);

        SomeImpl obj2 = with(SomeImpl.class).interceptAll(i -> {
            System.out.println("ByteBuddy - before method: " + i.getMethodName() + " param: " + i.getParameter0());
            return i.invoke();
        }).get();
        obj2.log("Hello ByteBuddy!");

        // Test interface proxy with ByteBuddy
        System.out.println("\n=== Testing interface with ByteBuddy ===");
        TestInterface intf = with(TestInterface.class).interceptAll(i -> {
            System.out.println("ByteBuddy Interface - method: " + i.getMethodName());
            return "Intercepted result";
        }).get();
        System.out.println("Result: " + intf.getValue());

        // Demonstrate that users can switch engines at runtime
        System.out.println("\n=== Runtime engine switching ===");
        for (ProxyConfiguration.Engine engine : ProxyConfiguration.Engine.values()) {
            ProxyConfiguration.setEngine(engine);
            SomeImpl obj = with(SomeImpl.class).interceptAll(i -> {
                System.out.println(engine.getName() + " engine - " + i.getMethodName());
                return i.invoke();
            }).get();
            obj.log("Engine: " + engine.getName());
        }

        // Reset to default
        ProxyConfiguration.reset();
    }

    public interface TestInterface {
        String getValue();
    }
}