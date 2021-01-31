package com.github.igrrik.kmmfrozeninspection

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtVisitorVoid

class ObjectWithVariablesInspection: AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
                super.visitObjectDeclaration(declaration)
                val properties = declaration.body?.properties
                val objectKeyword = declaration.getObjectKeyword()
                if (objectKeyword != null && properties != null && properties.any { it.isVar }) {
                    holder.registerProblem(
                        objectKeyword,
                        "Using variables will cause exception",
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
    }
}