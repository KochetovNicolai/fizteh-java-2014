package ru.fizteh.fivt.students.kochetovnicolai.proxy;

import ru.fizteh.fivt.proxy.LoggingProxyFactory;

import java.io.Writer;
import java.lang.reflect.Proxy;



public class LoggingProxyFactoryImpl implements LoggingProxyFactory {
    @Override
    public Object wrap(Writer writer, Object implementation, Class<?> interfaceClass) {
        if (writer == null) {
            throw new IllegalArgumentException("writer shouldn't be null");
        }
        if (implementation == null) {
            throw new IllegalArgumentException("implementation shouldn't be null");
        }
        if (interfaceClass == null) {
            throw new IllegalArgumentException("interfaceClass shouldn't be null");
        }
        if (!interfaceClass.isAssignableFrom(implementation.getClass())) {
            throw new IllegalArgumentException(implementation + " doesn't implements " + interfaceClass);
        }
        return Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{interfaceClass},
                 new ConsoleLoggerInvocationHandler(writer, implementation));
    }
}
