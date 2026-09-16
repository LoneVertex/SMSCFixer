package io.github.lonevertex.smscguard;

import io.github.libxposed.api.XposedInterface;

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.List;

public class TestHookChain implements XposedInterface.Chain {
    private final Executable executable;
    private final Object thisObject;
    private final Object[] args;
    private boolean proceedCalledWithoutArgs;
    private Object[] proceedArgs;

    public TestHookChain(Executable executable, Object thisObject, Object[] args) {
        this.executable = executable;
        this.thisObject = thisObject;
        this.args = args;
    }

    @Override
    public Executable getExecutable() {
        return executable;
    }

    @Override
    public Object getThisObject() {
        return thisObject;
    }

    @Override
    public List<Object> getArgs() {
        return Arrays.asList(args);
    }

    @Override
    public Object getArg(int index) {
        return args[index];
    }

    @Override
    public Object proceed() throws Throwable {
        proceedCalledWithoutArgs = true;
        return null;
    }

    @Override
    public Object proceed(Object[] newArgs) throws Throwable {
        this.proceedArgs = newArgs;
        return null;
    }

    @Override
    public Object proceedWith(Object newThis) throws Throwable {
        return proceed();
    }

    @Override
    public Object proceedWith(Object newThis, Object[] newArgs) throws Throwable {
        return proceed(newArgs);
    }

    public boolean isProceedCalledWithoutArgs() {
        return proceedCalledWithoutArgs;
    }

    public Object[] getProceedArgs() {
        return proceedArgs;
    }
}
