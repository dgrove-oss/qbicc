package org.qbicc.plugin.llvm;

import static org.qbicc.machine.llvm.Types.array;
import static org.qbicc.machine.llvm.Types.*;
import static org.qbicc.machine.llvm.Values.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.ArrayLiteral;
import org.qbicc.graph.literal.BitCastLiteral;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.ByteArrayLiteral;
import org.qbicc.graph.literal.CompoundLiteral;
import org.qbicc.graph.literal.ElementOfLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.FunctionLiteral;
import org.qbicc.graph.literal.GlobalVariableLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralVisitor;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.graph.literal.PointerLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.graph.literal.ValueConvertLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.machine.llvm.Array;
import org.qbicc.machine.llvm.Function;
import org.qbicc.machine.llvm.FunctionAttributes;
import org.qbicc.machine.llvm.IdentifiedType;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Module;
import org.qbicc.machine.llvm.ParameterAttributes;
import org.qbicc.machine.llvm.Struct;
import org.qbicc.machine.llvm.StructType;
import org.qbicc.machine.llvm.Types;
import org.qbicc.machine.llvm.Values;
import org.qbicc.machine.llvm.impl.LLVM;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.pointer.ElementPointer;
import org.qbicc.pointer.IntegerAsPointer;
import org.qbicc.pointer.MemberPointer;
import org.qbicc.pointer.OffsetPointer;
import org.qbicc.pointer.Pointer;
import org.qbicc.pointer.ProgramObjectPointer;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BlockType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FloatType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.InvokableType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.Type;
import org.qbicc.type.UnresolvedType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VariadicType;
import org.qbicc.type.VoidType;
import org.qbicc.type.WordType;

final class LLVMModuleNodeVisitor implements LiteralVisitor<Void, LLValue>, Pointer.Visitor<PointerLiteral, LLValue> {
    final LLVMModuleGenerator generator;
    final Module module;
    final CompilationContext ctxt;
    final LLVMReferencePointerFactory refFactory;

    final Map<Type, LLValue> types = new HashMap<>();
    final Map<CompoundType, Map<CompoundType.Member, LLValue>> structureOffsets = new HashMap<>();
    final Map<Value, LLValue> globalValues = new HashMap<>();

    final Map<FunctionType, LLValue> statepointDecls = new HashMap<>();
    final Map<String, LLValue> statepointDeclsByName = new HashMap<>();
    final Map<FunctionType, LLValue> statepointTypes = new HashMap<>();
    final Map<ValueType, LLValue> resultDecls = new HashMap<>();
    final Map<String, LLValue> resultDeclsByName = new HashMap<>();
    final Map<ValueType, LLValue> resultDeclTypes = new HashMap<>();
    LLValue relocateDecl;

    LLVMModuleNodeVisitor(final LLVMModuleGenerator generator, final Module module, final CompilationContext ctxt, LLVMReferencePointerFactory refFactory) {
        this.generator = generator;
        this.module = module;
        this.ctxt = ctxt;
        this.refFactory = refFactory;
    }

    LLValue map(Type type) {
        LLValue res = types.get(type);
        if (res != null) {
            return res;
        }
        if (type instanceof VoidType) {
            res = void_;
        } else if (type instanceof FunctionType) {
            FunctionType fnType = (FunctionType) type;
            int cnt = fnType.getParameterCount();
            List<LLValue> argTypes = cnt == 0 ? List.of() : new ArrayList<>(cnt);
            boolean variadic = false;
            for (int i = 0; i < cnt; i ++) {
                ValueType parameterType = fnType.getParameterType(i);
                if (parameterType instanceof VariadicType) {
                    if (i < cnt - 1) {
                        throw new IllegalStateException("Variadic type as non-final parameter type");
                    }
                    variadic = true;
                } else {
                    argTypes.add(map(parameterType));
                }
            }
            res = Types.function(map(fnType.getReturnType()), argTypes, variadic);
        } else if (type instanceof BooleanType) {
            // todo: sometimes it's one byte instead
            res = i1;
        } else if (type instanceof FloatType) {
            int bytes = (int) ((FloatType) type).getSize();
            if (bytes == 4) {
                res = float32;
            } else if (bytes == 8) {
                res = float64;
            } else {
                throw Assert.unreachableCode();
            }
        } else if (type instanceof InvokableType it) {
            // LLVM does not have an equivalent to method types
            res = map(ctxt.getFunctionTypeForInvokableType(it));
        } else if (type instanceof PointerType) {
            Type pointeeType = ((PointerType) type).getPointeeType();
            res = ptrTo(pointeeType instanceof VoidType ? i8 : map(pointeeType), 0);
        } else if (type instanceof ReferenceType || type instanceof UnresolvedType) {
            // References can be used as different types in the IL without manually casting them, so we need to
            // represent all reference types as being the same LLVM type. We will cast to and from the actual type we
            // use the reference as when needed.
            res = refFactory.makeReferencePointer();
        } else if (type instanceof WordType) {
            // all other words are integers
            // LLVM doesn't really care about signedness
            int bytes = (int) ((WordType) type).getSize();
            if (bytes == 1) {
                res = i8;
            } else if (bytes == 2) {
                res = i16;
            } else if (bytes == 4) {
                res = i32;
            } else if (bytes == 8) {
                res = i64;
            } else {
                throw Assert.unreachableCode();
            }
        } else if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            Type elementType = arrayType.getElementType();
            long size = arrayType.getElementCount();
            res = array((int) size, map(elementType));
        } else if (type instanceof CompoundType) {
            // Compound types are special in that they can be self-referential by containing pointers to themselves. To
            // handle this, we must do two special things:
            //   - Use an identified type in the module to avoid infinite recursion when printing the type
            //   - Add the mapping to types early to avoid infinite recursion when mapping self-referential member types
            CompoundType compoundType = (CompoundType) type;
            HashMap<CompoundType.Member, LLValue> offsets = new HashMap<>();
            boolean isIdentified = !compoundType.isAnonymous();

            structureOffsets.putIfAbsent(compoundType, offsets);

            IdentifiedType identifiedType = null;
            if (isIdentified) {
                String compoundName = compoundType.getName();
                String name;
                if (compoundType.getTag() == CompoundType.Tag.NONE) {
                    String outputName = "type." + compoundName;
                    name = LLVM.needsQuotes(compoundName) ? LLVM.quoteString(outputName) : outputName;
                } else {
                    String outputName = compoundType.getTag() + "." + compoundName;
                    name = LLVM.needsQuotes(compoundName) ? LLVM.quoteString(outputName) : outputName;
                }
                identifiedType = module.identifiedType(name);
                types.put(type, identifiedType.asTypeRef());
            }

            StructType struct = structType(isIdentified);
            int index = 0;
            for (CompoundType.Member member : compoundType.getPaddedMembers()) {
                ValueType memberType = member.getType();
                struct.member(map(memberType), member.getName());
                // todo: cache these ints
                offsets.put(member, Values.intConstant(index));
                index ++;
                // the target will already pad out for normal alignment
            }

            if (isIdentified) {
                identifiedType.type(struct);
                res = identifiedType.asTypeRef();
            } else {
                res = struct;
            }
        } else if (type instanceof BlockType) {
            res = label;
        } else {
            throw new IllegalStateException("Can't map Type("+ type.toString() + ")");
        }
        types.put(type, res);
        return res;
    }

    LLValue map(final CompoundType compoundType, final CompoundType.Member member) {
        // populate map
        map(compoundType);
        return structureOffsets.get(compoundType).get(member);
    }

    LLValue map(final Literal value) {
        LLValue mapped = globalValues.get(value);
        if (mapped != null) {
            return mapped;
        }
        mapped = value.accept(this, null);
        globalValues.put(value, mapped);
        return mapped;
    }

    public LLValue visit(final Void param, final ArrayLiteral node) {
        List<Literal> values = node.getValues();
        Array array = Values.array(map(node.getType().getElementType()));
        for (int i = 0; i < values.size(); i++) {
            array.item(map(values.get(i)));
        }
        return array;
    }

    public LLValue visit(final Void param, final BitCastLiteral node) {
        LLValue input = map(node.getValue());
        LLValue fromType = map(node.getValue().getType());
        LLValue toType = map(node.getType());
        if (fromType.equals(toType)) {
            return input;
        }

        return Values.bitcastConstant(input, fromType, toType);
    }

    public LLValue visit(final Void param, final ValueConvertLiteral node) {
        LLValue input = map(node.getValue());
        ValueType inputType = node.getValue().getType();
        LLValue fromType = map(inputType);
        WordType outputType = node.getType();
        LLValue toType = map(outputType);
        if (fromType.equals(toType)) {
            return input;
        }

        if (inputType instanceof IntegerType && outputType instanceof PointerType) {
            return Values.inttoptrConstant(input, fromType, toType);
        } else if (inputType instanceof PointerType && outputType instanceof IntegerType) {
            return Values.ptrtointConstant(input, fromType, toType);
        } else if (inputType instanceof ReferenceType && outputType instanceof PointerType) {
            return refFactory.cast(input, fromType, toType);
        } else if (inputType instanceof PointerType && outputType instanceof ReferenceType) {
            return refFactory.cast(input, fromType, toType);
        }
        // todo: add signed/unsigned int <-> fp
        return visitAny(param, node);
    }

    public LLValue visit(final Void param, final ByteArrayLiteral node) {
        return Values.byteArray(node.getValues());
    }

    public LLValue visit(final Void param, final CompoundLiteral node) {
        CompoundType type = node.getType();
        Map<CompoundType.Member, Literal> values = node.getValues();
        // very similar to emitting a struct type, but we don't have to cache the structure offsets
        Struct struct = struct();
        for (CompoundType.Member member : type.getPaddedMembers()) {
            Literal literal = values.get(member);
            ValueType memberType = member.getType();
            if (literal == null) {
                // no value for this member
                struct.item(map(memberType), zeroinitializer);
            } else {
                struct.item(map(memberType), map(literal));
            }
        }
        return struct;
    }

    public LLValue visit(final Void param, final ElementOfLiteral node) {
        PointerType pointerType = (PointerType) node.getType();
        return Values.gepConstant(map(pointerType.getPointeeType()), map(pointerType), map(node.getValue()), map(node.getIndex().getType()), map(node.getIndex()));
    }

    public LLValue visit(final Void param, final FloatLiteral node) {
        if (node.getType().getMinBits() == 32) {
            return Values.floatConstant(node.floatValue());
        } else { // Should be 64
            return Values.floatConstant(node.doubleValue());
        }
    }

    public LLValue visit(Void unused, FunctionLiteral literal) {
        return Values.global(literal.getExecutable().getName());
    }

    public LLValue visit(final Void unused, final GlobalVariableLiteral node) {
        return Values.global(node.getVariableElement().getName());
    }

    public LLValue visit(final Void param, final IntegerLiteral node) {
        return Values.intConstant(node.longValue());
    }

    public LLValue visit(final Void param, final NullLiteral node) {
        return NULL;
    }

    public LLValue visit(final Void param, final PointerLiteral node) {
        // see below for pointer visitor implementations
        return node.getPointer().accept(this, node);
    }

    public LLValue visit(final Void param, final ZeroInitializerLiteral node) {
        return Values.zeroinitializer;
    }

    public LLValue visit(final Void param, final BooleanLiteral node) {
        return node.booleanValue() ? TRUE : FALSE;
    }

    public LLValue visit(Void param, UndefinedLiteral node) {
        return Values.UNDEF;
    }

    public LLValue visit(Void param, TypeLiteral node) {
        ValueType type = node.getValue();
        // common cases first
        int typeId;
        if (type instanceof ArrayObjectType) {
            typeId = CoreClasses.get(ctxt).getArrayContentField((ArrayObjectType) type).getEnclosingType().load().getTypeId();
        } else if (type instanceof ObjectType) {
            typeId = ((ObjectType) type).getDefinition().load().getTypeId();
        } else if (type instanceof WordType) {
            typeId = ((WordType) type).asPrimitive().getTypeId();
        } else if (type instanceof VoidType) {
            typeId = ((VoidType) type).asPrimitive().getTypeId();
        } else {
            // not a valid type literal
            ctxt.error("llvm: cannot lower type literal %s", node);
            return Values.intConstant(0);
        }
        if (typeId <= 0) {
            ctxt.error("llvm: type %s has invalid type ID %d", type, typeId);
        }
        return Values.intConstant(typeId);
    }

    @Override
    public LLValue visitAny(Void unused, Literal literal) {
        ctxt.error(Location.builder().setNode(literal).build(), "llvm: Unrecognized literal type %s", literal.getClass());
        return LLVM.FALSE;
    }

    @Override
    public LLValue visitAny(PointerLiteral pointerLiteral, Pointer pointer) {
        ctxt.error(Location.builder().setNode(pointerLiteral).build(), "llvm: Unrecognized pointer value %s", pointer.getClass());
        return LLVM.FALSE;
    }

    @Override
    public LLValue visit(PointerLiteral pointerLiteral, ElementPointer pointer) {
        // todo: we can merge GEPs
        return Values.gepConstant(
            map(pointer.getPointeeType()),
            map(pointer.getType()),
            pointer.getArrayPointer().accept(this, pointerLiteral),
            ZERO,
            Values.intConstant(pointer.getIndex())
        );
    }

    @Override
    public LLValue visit(PointerLiteral pointerLiteral, MemberPointer pointer) {
        // todo: we can merge GEPs
        return Values.gepConstant(
            map(pointer.getPointeeType()),
            map(pointer.getType()),
            pointer.getStructurePointer().accept(this, pointerLiteral),
            ZERO,
            map(pointer.getPointeeType(), pointer.getMember())
        );
    }

    @Override
    public LLValue visit(PointerLiteral pointerLiteral, OffsetPointer pointer) {
        return Values.gepConstant(
            map(pointer.getPointeeType()),
            map(pointer.getType()),
            pointer.getBasePointer().accept(this, pointerLiteral),
            Values.intConstant(pointer.getOffset())
        );
    }

    @Override
    public LLValue visit(PointerLiteral pointerLiteral, IntegerAsPointer pointer) {
        return Values.inttoptrConstant(Values.intConstant(pointer.getValue()), i64, map(pointer.getType()));
    }

    @Override
    public LLValue visit(PointerLiteral pointerLiteral, ProgramObjectPointer pointer) {
        return Values.global(pointer.getProgramObject().getName());
    }

    public LLVMModuleGenerator getGenerator() {
        return generator;
    }

    public LLValue mapStatepointType(final FunctionType functionType) {
        LLValue statepointType = statepointTypes.get(functionType);
        if (statepointType == null) {
            statepointType = Types.function(token, List.of(i64, i32, map(functionType.getPointer()), i32, i32), true);
            statepointTypes.put(functionType, statepointType);
        }
        return statepointType;
    }

    public LLValue generateStatepointDecl(final FunctionType functionType) {
        LLValue statepointDecl = statepointDecls.get(functionType);
        if (statepointDecl == null) {
            StringBuilder b = new StringBuilder(64);
            b.append("llvm.experimental.gc.statepoint.");
            mapTypeSuffix(b, functionType.getPointer());
            final String name = b.toString();
            statepointDecl = statepointDeclsByName.get(name);
            if (statepointDecl == null) {
                final Function decl = module.declare(name);
                decl.param(i64).immarg(); // statepoint ID
                decl.param(i32).immarg(); // patch byte count
                // pointer to the function
                final Function.Parameter param = decl.param(map(functionType.getPointer()));
                if (generator.getLlvmMajor() >= 15) {
                    param.attribute(ParameterAttributes.elementtype(map(functionType)));
                }
                decl.param(i32).immarg(); // call arg count
                decl.param(i32).immarg(); // flags
                decl.variadic();
                decl.returns(token);
                statepointDecl = decl.asGlobal();
                statepointDeclsByName.put(name, statepointDecl);
            }
            statepointDecls.put(functionType, statepointDecl);
        }
        return statepointDecl;
    }

    private void mapTypeSuffix(final StringBuilder b, final ValueType type) {
        if (type instanceof BooleanType) {
            b.append("i1");
        } else if (type instanceof IntegerType it) {
            b.append('i').append(it.getMinBits());
        } else if (type instanceof PointerType pt) {
            b.append("p0");
            mapTypeSuffix(b, pt.getPointeeType());
        } else if (type instanceof ReferenceType) {
            b.append("p1i8");
        } else if (type instanceof FloatType ft) {
            b.append('f').append(ft.getMinBits());
        } else if (type instanceof VoidType) {
            b.append("isVoid");
        } else if (type instanceof WordType wt) {
            b.append('i').append(wt.getMinBits());
        } else if (type instanceof CompoundType ct) {
            b.append("s_");
            final String compoundName = ct.getName();
            String name;
            if (ct.getTag() == CompoundType.Tag.NONE) {
                String outputName = "type." + compoundName;
                name = LLVM.needsQuotes(compoundName) ? LLVM.quoteString(outputName) : outputName;
            } else {
                String outputName = ct.getTag() + "." + compoundName;
                name = LLVM.needsQuotes(compoundName) ? LLVM.quoteString(outputName) : outputName;
            }
            b.append(name);
            b.append('s');
        } else if (type instanceof VariadicType) {
            // ignore
        } else if (type instanceof FunctionType ft) {
            b.append("f_");
            mapTypeSuffix(b, ft.getReturnType());
            for (ValueType parameterType : ft.getParameterTypes()) {
                mapTypeSuffix(b, parameterType);
            }
            b.append('f');
        } else {
            throw new IllegalStateException("Unexpected type " + type);
        }
    }

    public LLValue generateStatepointResultDecl(final ValueType returnType) {
        LLValue resultDecl = resultDecls.get(returnType);
        if (resultDecl == null) {
            StringBuilder b = new StringBuilder(64);
            b.append("llvm.experimental.gc.result.");
            mapTypeSuffix(b, returnType);
            final String name = b.toString();
            resultDecl = resultDeclsByName.get(name);
            if (resultDecl == null) {
                final Function decl = module.declare(b.toString());
                decl.param(token);
                decl.returns(map(returnType));
                decl.attribute(FunctionAttributes.nounwind).attribute(FunctionAttributes.readnone);
                resultDecl = decl.asGlobal();
                resultDeclsByName.put(name, resultDecl);
            }
            resultDecls.put(returnType, resultDecl);
        }
        return resultDecl;
    }

    public LLValue generateStatepointResultDeclType(final ValueType returnType) {
        LLValue resultDeclType = resultDeclTypes.get(returnType);
        if (resultDeclType == null) {
            resultDeclType = Types.function(map(returnType), List.of(token), false);
            resultDeclTypes.put(returnType, resultDeclType);
        }
        return resultDeclType;
    }
}
