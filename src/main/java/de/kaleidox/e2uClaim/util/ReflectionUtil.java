package de.kaleidox.e2uClaim.util;

import java.io.PrintStream;
import java.lang.reflect.Method;

public final class ReflectionUtil {
    public static void printMethods(Object obj, PrintStream out) {
        final Class<?> aClass = obj.getClass();

        out.printf("Printing methods of class %s...\n", aClass.toGenericString());

        for (Method method : aClass.getDeclaredMethods()) {
            out.printf("-\tMethod Signature: %s\n", method.toGenericString());
        }

        out.println("- End of output");
    }
}
