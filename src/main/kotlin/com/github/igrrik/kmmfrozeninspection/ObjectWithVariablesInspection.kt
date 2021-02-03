package com.github.igrrik.kmmfrozeninspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtVisitorVoid

class ObjectWithVariablesInspection: AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
                super.visitObjectDeclaration(declaration)

                val threadLocalName = FqName("kotlin.native.concurrent.ThreadLocal")
                if (declaration.findAnnotation(threadLocalName) != null) return

                val properties = declaration.body?.properties ?: return

                properties
                    .filter { it.isVar }
                    .forEach { property ->
                        holder.registerProblem(
                            property.psiOrParent,
                            "Add @ThreadLocal annotation to object or replace var with val",
                            ProblemHighlightType.WARNING
                        )
                    }
            }
        }
    }
}