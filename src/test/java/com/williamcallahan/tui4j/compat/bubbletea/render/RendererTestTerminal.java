package com.williamcallahan.tui4j.compat.bubbletea.render;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;

import org.jline.terminal.Terminal;

/**
 * Captures renderer output while exposing mutable terminal dimensions to tests.
 */
final class RendererTestTerminal {

    private final StringWriter output = new StringWriter();
    private final PrintWriter writer = new PrintWriter(output);
    private final Terminal terminal;
    private int width;
    private int height;

    /**
     * Creates a terminal with the requested dimensions.
     *
     * @param width terminal columns
     * @param height terminal rows
     */
    RendererTestTerminal(int width, int height) {
        this.width = width;
        this.height = height;
        this.terminal = (Terminal) Proxy.newProxyInstance(
            Terminal.class.getClassLoader(),
            new Class<?>[]{Terminal.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "writer" -> writer;
                case "flush" -> null;
                case "getType" -> "xterm-256color";
                case "getWidth" -> this.width;
                case "getHeight" -> this.height;
                case "puts" -> true;
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    /**
     * Returns the terminal proxy consumed by the renderer.
     *
     * @return terminal proxy
     */
    Terminal terminal() {
        return terminal;
    }

    /**
     * Updates the dimensions returned by the terminal proxy.
     *
     * @param width terminal columns
     * @param height terminal rows
     */
    void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Returns output written since the previous drain.
     *
     * @return newly written terminal bytes
     */
    String drain() {
        writer.flush();
        String value = output.toString();
        output.getBuffer().setLength(0);
        return value;
    }

    /**
     * Supplies Java defaults for terminal methods irrelevant to renderer tests.
     *
     * @param returnType invoked method return type
     * @return the corresponding default value
     */
    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) return null;
        if (returnType == boolean.class) return false;
        if (returnType == byte.class) return (byte) 0;
        if (returnType == short.class) return (short) 0;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == float.class) return 0f;
        if (returnType == double.class) return 0d;
        if (returnType == char.class) return (char) 0;
        return null;
    }
}
