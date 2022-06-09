package org.qbicc.graph;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.Literal;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.Element;
import org.qbicc.type.definition.element.ExecutableElement;

public final class PhiValue extends AbstractValue implements PinnedNode {
    private static final boolean DEBUG_PHIS = Boolean.parseBoolean(System.getProperty("qbicc.debug.phis", "false"));

    static class DebugHelper {
        static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

        static StackTraceElement getCreatingFrame() {
            return STACK_WALKER.walk(stream -> stream.skip(2).dropWhile(sf -> BasicBlockBuilder.class.isAssignableFrom(sf.getDeclaringClass())).findFirst().orElseThrow()).toStackTraceElement();
        }
    }

    private final ValueType type;
    private final BlockLabel blockLabel;
    private final boolean nullable;
    // for debugger
    private final StackTraceElement creatingFrame;

    PhiValue(final Node callSite, final ExecutableElement element, final int line, final int bci, final ValueType type, final BlockLabel blockLabel, boolean nullable) {
        super(callSite, element, line, bci);
        this.type = type;
        this.blockLabel = blockLabel;
        this.nullable = nullable;
        creatingFrame = DEBUG_PHIS ? DebugHelper.getCreatingFrame() : null;
    }

    public Value getValueForInput(final Terminator input) {
        return Assert.checkNotNullParam("input", input).getOutboundValue(this);
    }

    public void setValueForTerminator(final CompilationContext ctxt, final Element element, final Terminator input, Value value) {
        Assert.checkNotNullParam("value", value);
        ValueType expected = getType();
        ValueType actual = value.getType();
        if (! expected.isImplicitlyConvertibleFrom(actual)) {
            if (value instanceof Literal && ((Literal) value).isZero()) {
                value = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(expected);
            } else {
                if (DEBUG_PHIS) {
                    ctxt.debug(element, this, "Invalid input value for phi: expected %s, got %s", expected, actual);
                }
            }
        }
        if (! nullable && value.isNullable()) {
            ctxt.error(element, this, "Cannot set nullable value %s for phi", value);
        }
        if (! input.registerValue(this, value)) {
            ctxt.error(element, this, "Phi already has a value for block %s", input.getTerminatedBlock());
            return;
        }
    }

    public void setValueForBlock(final CompilationContext ctxt, final Element element, final BasicBlock input, final Value value) {
        setValueForTerminator(ctxt, element, input.getTerminator(), value);
    }

    public void setValueForBlock(final CompilationContext ctxt, final Element element, final BlockLabel input, final Value value) {
        setValueForBlock(ctxt, element, BlockLabel.getTargetOf(input), value);
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    /**
     * Get all of the possible non-phi values for this phi.
     *
     * @return the set of possible values (not {@code null})
     */
    public Set<Value> getPossibleValues() {
        LinkedHashSet<Value> possibleValues = new LinkedHashSet<>();
        getPossibleValues(possibleValues, new HashSet<>(), false);
        return possibleValues;
    }

    public Set<Value> getPossibleValuesIncludingPhi() {
        LinkedHashSet<Value> possibleValues = new LinkedHashSet<>();
        getPossibleValues(possibleValues, new HashSet<>(), true);
        return possibleValues;
    }

    public boolean possibleValuesAreNullable() {
        for (Value value : getPossibleValues()) {
            if (value.isNullable()) {
                return true;
            }
        }
        return false;
    }

    private void getPossibleValues(Set<Value> current, Set<PhiValue> visited, boolean includePhis) {
        if (visited.add(this)) {
            BasicBlock pinnedBlock = getPinnedBlock();
            Set<BasicBlock> incoming = pinnedBlock.getIncoming();
            for (BasicBlock basicBlock : incoming) {
                if (basicBlock.isReachable()) {
                    Value value = getValueForInput(basicBlock.getTerminator());
                    if (!includePhis && value instanceof PhiValue) {
                        ((PhiValue) value).getPossibleValues(current, visited, false);
                    } else {
                        current.add(value);
                    }
                }
            }
        }
    }

    public ValueType getType() {
        return type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public BlockLabel getPinnedBlockLabel() {
        return blockLabel;
    }

    int calcHashCode() {
        // every phi is globally unique
        return System.identityHashCode(this);
    }

    @Override
    String getNodeName() {
        return "Phi";
    }

    public boolean equals(final Object other) {
        // every phi is globally unique
        return this == other;
    }

    public enum Flag {
        NOT_NULL,
        ;
    }
}
