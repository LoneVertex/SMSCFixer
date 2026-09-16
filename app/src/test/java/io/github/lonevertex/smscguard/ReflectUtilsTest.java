package io.github.lonevertex.smscguard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ReflectUtilsTest {

    static class BaseTarget {
        private final String baseField = "fromBase";

        public String getBaseGreeting() {
            return "Hello from base";
        }
    }

    static class DummyTarget extends BaseTarget {
        private final int value = 42;

        public int getValue() {
            return value;
        }

        public static String greeting(String name) {
            return "Hello, " + name;
        }

        public String overloaded(String text) {
            return "String: " + text;
        }

        public String overloaded(Integer number) {
            return "Integer: " + number;
        }
    }

    @Test
    public void testFieldAndMethodReflection() {
        DummyTarget target = new DummyTarget();
        assertEquals(42, ReflectUtils.callMethod(target, "getValue"));
        assertEquals(42, ReflectUtils.getObjectField(target, "value"));
        assertEquals("Hello, World", ReflectUtils.callStaticMethod(DummyTarget.class, "greeting", "World"));
    }

    @Test
    public void testNullSafety() {
        assertNull(ReflectUtils.callMethod(null, "anyMethod"));
        assertNull(ReflectUtils.callStaticMethod(null, "anyStaticMethod"));
        assertNull(ReflectUtils.getObjectField(null, "anyField"));
    }

    @Test
    public void testInheritanceResolution() {
        DummyTarget target = new DummyTarget();
        assertEquals("Hello from base", ReflectUtils.callMethod(target, "getBaseGreeting"));
        assertEquals("fromBase", ReflectUtils.getObjectField(target, "baseField"));
    }

    @Test
    public void testOverloadedMethods() {
        DummyTarget target = new DummyTarget();
        assertEquals("String: test", ReflectUtils.callMethod(target, "overloaded", "test"));
        assertEquals("Integer: 123", ReflectUtils.callMethod(target, "overloaded", 123));
    }

    @Test
    public void testMissingMethodThrowsException() {
        DummyTarget target = new DummyTarget();
        try {
            ReflectUtils.callMethod(target, "nonExistentMethod");
            fail("Expected RuntimeException for missing method");
        } catch (RuntimeException expected) {
            // Success
        }
    }

    @Test
    public void testMissingFieldThrowsException() {
        DummyTarget target = new DummyTarget();
        try {
            ReflectUtils.getObjectField(target, "nonExistentField");
            fail("Expected RuntimeException for missing field");
        } catch (RuntimeException expected) {
            // Success
        }
    }
}
