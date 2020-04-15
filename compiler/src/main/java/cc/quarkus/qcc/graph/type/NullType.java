package cc.quarkus.qcc.graph.type;

public class NullType implements ConcreteType<NullValue> {

    public static final NullType INSTANCE = new NullType();

    private NullType() {

    }

    @Override
    public NullValue newInstance(Object... args) {
        return NullValue.NULL;
    }
}
