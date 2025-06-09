# Proxy Engine Switching

The proxy library now supports multiple bytecode manipulation engines. You can choose between **Javassist** (default) and **ByteBuddy** engines while keeping the exact same API.

## Configuration Options

### 1. System Property (Global)
```bash
# Use ByteBuddy engine for the entire application
java -Dproxy.engine=bytebuddy your.Application

# Use Javassist engine (default)
java -Dproxy.engine=javassist your.Application
```

### 2. Programmatic Configuration (Runtime)
```java
import com.ericsson.commonlibrary.proxy.ProxyConfiguration;

// Switch to ByteBuddy engine
ProxyConfiguration.setEngine(ProxyConfiguration.Engine.BYTEBUDDY);

// Switch to Javassist engine
ProxyConfiguration.setEngine(ProxyConfiguration.Engine.JAVASSIST);

// Reset to system property or default
ProxyConfiguration.reset();
```

## Usage Examples

The API remains exactly the same regardless of the engine:

```java
import static com.ericsson.commonlibrary.proxy.Proxy.with;

// Works with both engines!
SomeClass obj = with(SomeClass.class)
    .interceptAll(i -> {
        System.out.println("Method: " + i.getMethodName());
        return i.invoke();
    }).get();
```

## Engine Comparison

| Feature | Javassist | ByteBuddy |
|---------|-----------|-----------|
| **Performance** | Good | Excellent |
| **Memory Usage** | Lower | Higher |
| **Compatibility** | Excellent | Excellent |
| **JavaBean Enhancement** | Full | Simplified* |
| **Mature/Stable** | Very High | High |

*ByteBuddy implementation currently has simplified JavaBean support. This can be enhanced in future versions.

## Architecture Benefits

- **Zero API Changes**: Existing code works without modification
- **Runtime Switching**: Change engines dynamically based on conditions
- **Easy Testing**: Compare engine behaviors side-by-side
- **Future-Proof**: Easy to add new engines (e.g., ASM, CGLIB)
- **Clean Code**: No more scattered if-statements in core logic

## Migration Guide

No code changes needed! Just add engine selection:

```java
// Before (always used Javassist)
SomeClass proxy = Proxy.with(SomeClass.class).interceptAll(...).get();

// After (choose your engine)
ProxyConfiguration.setEngine(ProxyConfiguration.Engine.BYTEBUDDY);
SomeClass proxy = Proxy.with(SomeClass.class).interceptAll(...).get(); // Same API!
``` 