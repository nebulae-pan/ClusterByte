package io.nebula.platform.clusterbyte

import org.junit.Assert.assertEquals
import org.junit.Test
import org.objectweb.asm.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
        val classReader =
            ClassReader("io.nebula.platform.clusterbyte.core.BaseIncrementalTransform")
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val classVisitor = object : ClassVisitor(Opcodes.ASM9, classWriter) {
            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                println(
                    "visitMethod() called with: access = $access, name = $name, descriptor = $descriptor, signature = $signature, exceptions = $exceptions"
                )
                var mv = super.visitMethod(access, name, descriptor, signature, exceptions)
                mv = object : MethodVisitor(Opcodes.ASM7, mv) {
                    override fun visitCode() {
                        super.visitCode()
                    }

                    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                        println(
                            "visitMaxs() called with: maxStack = $maxStack, maxLocals = $maxLocals"
                        )
                    }

                    override fun visitEnd() {
                        super.visitEnd()
                    }
                }
                return mv
            }
        }
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)

        classWriter.toByteArray()

    }
}
