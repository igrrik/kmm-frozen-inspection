package com.github.igrrik.kmmfrozeninspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtVisitorVoid

class NativeVarInEnumInspection : AbstractKotlinInspection() {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {
        return object : KtVisitorVoid() {

            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)
                if (!klass.isEnum()) return

                klass.primaryConstructorParameters
                    .filter { it.isMutable }
                    .forEach { parameter ->
                        holder.registerProblem(
                            parameter.psiOrParent,
                            "Enums are frozen by default, change to val",
                            ProblemHighlightType.WARNING
                        )
                    }
            }
        }
    }
}