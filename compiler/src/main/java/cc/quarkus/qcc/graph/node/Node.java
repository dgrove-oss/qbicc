package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.graph.type.Value;

public class Node<T extends Type> {

    protected Node(ControlNode<?> control, T outType) {
        this(outType);
        addPredecessor(control);
    }

    protected Node(T outType) {
        this.outType = outType;
        this.id = COUNTER.incrementAndGet();
    }

    public void addPredecessor(Node<?> in) {
        this.predecessors.add(in);
        in.addSuccessor(this);
    }

    public <T extends Type> Node<T> tryCoerce(Type type) {
        if ( type == getType() ) {
            System.err.println( "tryCoerce 1");
            return (Node<T>) this;
        }
        System.err.println( "tryCoerce 2");
        return type.coerce(this);
    }

    private void addSuccessor(Node<?> out) {
        this.successors.add(out);
    }

    public List<Node<?>> getPredecessors() {
        return this.predecessors;
    }

    public List<ControlNode<?>> getControlPredecessors() {
        return this.predecessors.stream().filter(e->e instanceof ControlNode).map(e->(ControlNode<?>)e).collect(Collectors.toList());
    }

    public List<Node<?>> getSuccessors() {
        return this.successors;
    }

    public List<ControlNode<?>> getControlSuccessors() {
        return this.successors.stream().filter(e->e instanceof ControlNode).map(e->(ControlNode<?>)e).collect(Collectors.toList());
    }

    public T getType() {
        return this.outType;
    }

    public <J extends Type> Node<J> getOut(J type) {
        if ( type == this.outType ) {
            return (Node<J>) this;
        }
        return null;
    }

    public int getId() {
        return this.id;
    }

    public String label() {
        String n = this.id + ": " + getClass().getSimpleName();
        if ( n.endsWith( "Node" ) ) {
            return n.substring(0, n.length() - "node".length()).toLowerCase();
        } else if ( n.endsWith( "Projection") ) {
            return n.substring(0, n.length() - "projection".length()).toLowerCase();
        } else {
            return n;
        }
    }

    @Override
    public String toString() {
        return label();
    }

    protected <T extends ConcreteType<?>> void replacePredecessor(PhiNode target, Node<? extends Type> replacement) {
        System.err.println( this + " >a> " + this.predecessors);

        this.predecessors.replaceAll( (each)->{
            if ( each == target ) {
                System.err.println( "replace with " + replacement);
                replacement.addSuccessor(this);
                return replacement;
            }
            return each;
        });
        System.err.println( this + " >b> " + this.predecessors);

    }

    protected <T extends Type> void removeSuccessor(PhiNode<T> node) {
        System.err.println( this + " remove successor " + node);
        this.successors.remove(node);
    }

    public void receive(Value<?> value) {
        for (Node<?> successor : getSuccessors()) {
            successor.receive(process(value));
        }
    }

    public Value<?> process(Value<?> value) {
        return value;
    }

    private final List<Node<?>> predecessors = new ArrayList<>();
    private final List<Node<?>> successors = new ArrayList<>();
    private final T outType;
    private final int id;

    private static final AtomicInteger COUNTER = new AtomicInteger(0);
}
