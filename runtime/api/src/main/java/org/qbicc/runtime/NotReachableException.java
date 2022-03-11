package org.qbicc.runtime;

/**
 * An exception thrown when a part of the program is expected to be unreachable (e.g. due to {@link NoReturn}).
 */
public final class NotReachableException extends IllegalStateException {
    private static final long serialVersionUID = -2319522867660912494L;

    public NotReachableException() {
    }

    public NotReachableException(String s) {
        super(s);
    }
}
