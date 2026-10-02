package li.cil.oc.server.machine;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class CallbackWrapper {
    private CallbackWrapper() {
    }

    private static final String ObjectNameASM = Object.class.getName().replace('.', '/');
    private static final String CallbackCallDesc;
    private static final String[] CallbackCallInterface = new String[]{CallbackCall.class.getName().replace('.', '/')};
    private static final Map<Method, String> MethodIdCache = new HashMap<>();
    private static final Map<Method, CallbackCall> CallbackWrapperCache = new HashMap<>();

    static {
        try {
            CallbackCallDesc = Type.getMethodDescriptor(CallbackCall.class.getMethod("call", Object.class, Context.class, Arguments.class));
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static synchronized CallbackCall createCallbackWrapper(Method method) {
        return CallbackWrapperCache.computeIfAbsent(method, m -> (CallbackCall) createWrapper(m, CallbackCallInterface));
    }

    private static Object createWrapper(Method m, String[] interfaces) {
        final String className = "generated.li.cil.oc.CallWrapper_" + generateId(m);
        if (!GeneratedClassLoader.INSTANCE.containsClass(className)) {
            final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, className.replace('.', '/'), null, ObjectNameASM, interfaces);
            emitConstructor(cw);
            emitCallbackCall(m, cw);
            cw.visitEnd();
            GeneratedClassLoader.INSTANCE.addClass(className, cw.toByteArray());
        }

        try {
            return GeneratedClassLoader.INSTANCE.findClass(className).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void emitConstructor(ClassWriter cw) {
        final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, ObjectNameASM, "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private static void emitCallbackCall(Method m, ClassWriter cw) {
        final String className = m.getDeclaringClass().getName().replace('.', '/');
        final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "call", CallbackCallDesc, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, className);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        if (m.getDeclaringClass().isInterface()) {
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, className, m.getName(), Type.getMethodDescriptor(m), true);
        } else {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, m.getName(), Type.getMethodDescriptor(m), false);
        }
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    private static String generateId(Method m) {
        return MethodIdCache.computeIfAbsent(m, k -> k.getDeclaringClass().getName().replace('.', '_') + "_" + k.getName());
    }

    private static final class GeneratedClassLoader extends ClassLoader {
        static final GeneratedClassLoader INSTANCE = new GeneratedClassLoader();

        private final Map<String, Class<?>> GeneratedClasses = new HashMap<>();

        private GeneratedClassLoader() {
            // Parent must be able to see both OC's classes and any addon classes declaring callbacks.
            super(OpenComputers.class.getClassLoader());
        }

        boolean containsClass(String name) {
            return GeneratedClasses.containsKey(name);
        }

        void addClass(String name, byte[] bytes) {
            GeneratedClasses.put(name, defineClass(name, bytes, 0, bytes.length));
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            final Class<?> clazz = GeneratedClasses.get(name);
            if (clazz != null) return clazz;
            return super.findClass(name);
        }
    }
}
