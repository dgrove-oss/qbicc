package org.qbicc.plugin.intrinsics.core;

import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Phase;
import org.qbicc.machine.arch.ArmCpu;
import org.qbicc.machine.arch.Cpu;
import org.qbicc.machine.arch.OS;
import org.qbicc.machine.arch.ObjectType;
import org.qbicc.machine.arch.Platform;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 * Intrinsics which answer build host and target queries.
 */
final class BuildIntrinsics {
    public static void register(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        registerTopLevelIntrinsics(intrinsics, ctxt);
        registerHostIntrinsics(intrinsics, ctxt);
        registerTargetIntrinsics(intrinsics, ctxt);
    }

    private static void registerTopLevelIntrinsics(final Intrinsics intrinsics, final CompilationContext ctxt) {
        final ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor buildDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/Build");

        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        intrinsics.registerIntrinsic(Phase.ANALYZE, buildDesc, "isHost", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(false)
        );
        intrinsics.registerIntrinsic(Phase.ANALYZE, buildDesc, "isTarget", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(true)
        );
        intrinsics.registerIntrinsic(buildDesc, "isJvm", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(false)
        );
    }

    private static void registerHostIntrinsics(final Intrinsics intrinsics, final CompilationContext ctxt) {
        final ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor hostDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/Build$Host");

        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        final OS os = Platform.HOST_PLATFORM.getOs();

        intrinsics.registerIntrinsic(hostDesc, "isLinux", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == OS.LINUX)
        );
        intrinsics.registerIntrinsic(hostDesc, "isWindows", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == OS.WIN32)
        );
        intrinsics.registerIntrinsic(hostDesc, "isMacOs", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == OS.DARWIN)
        );

        final Cpu cpu = Platform.HOST_PLATFORM.getCpu();

        intrinsics.registerIntrinsic(hostDesc, "isAmd64", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.X86_64)
        );
        intrinsics.registerIntrinsic(hostDesc, "isI386", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.X86)
        );
        intrinsics.registerIntrinsic(hostDesc, "isArm", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu instanceof ArmCpu)
        );
        intrinsics.registerIntrinsic(hostDesc, "isAarch64", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.AARCH64)
        );
    }

    private static void registerTargetIntrinsics(final Intrinsics intrinsics, final CompilationContext ctxt) {
        final ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor targetDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/Build$Target");

        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        intrinsics.registerIntrinsic(targetDesc, "isVirtual", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(true)
        );

        final OS os = ctxt.getPlatform().getOs();

        intrinsics.registerIntrinsic(targetDesc, "isUnix", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == OS.LINUX || os == OS.DARWIN)
        );
        intrinsics.registerIntrinsic(targetDesc, "isLinux", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == OS.LINUX)
        );
        intrinsics.registerIntrinsic(targetDesc, "isWindows", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == OS.WIN32)
        );
        intrinsics.registerIntrinsic(targetDesc, "isApple", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == OS.DARWIN)
        );
        intrinsics.registerIntrinsic(targetDesc, "isMacOs", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == OS.DARWIN)
        );
        intrinsics.registerIntrinsic(targetDesc, "isIOS", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(false)
        );
        intrinsics.registerIntrinsic(targetDesc, "isAix", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(false)
        );
        intrinsics.registerIntrinsic(targetDesc, "isPosix", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == OS.LINUX || os == OS.DARWIN)
        );
        intrinsics.registerIntrinsic(targetDesc, "isWasi", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == OS.WASI)
        );

        final Cpu cpu = ctxt.getPlatform().getCpu();

        intrinsics.registerIntrinsic(targetDesc, "isAmd64", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.X86_64)
        );
        intrinsics.registerIntrinsic(targetDesc, "isI386", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.X86)
        );
        intrinsics.registerIntrinsic(targetDesc, "isArm", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu instanceof ArmCpu)
        );
        intrinsics.registerIntrinsic(targetDesc, "isAarch64", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.AARCH64)
        );
        intrinsics.registerIntrinsic(targetDesc, "isWasm", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.WASM32)
        );

        final ObjectType objectType = ctxt.getPlatform().getObjectType();

        intrinsics.registerIntrinsic(targetDesc, "isElf", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(objectType == ObjectType.ELF)
        );
        intrinsics.registerIntrinsic(targetDesc, "isMachO", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(objectType == ObjectType.MACH_O)
        );
    }
}
