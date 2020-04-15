package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.graph.ParseException;

public interface ConcreteType<T extends Value<?>> extends Type<T> {

    @Override
    default String label() {
        String n = getClass().getSimpleName();
        return n.substring(0, n.length() - "type".length()).toLowerCase();
    }
}
