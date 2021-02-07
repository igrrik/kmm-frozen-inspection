package com.github.igrrik.kmmfrozeninspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

class NativeVariablesInFrozenTypesInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
                super.visitObjectDeclaration(declaration)

                val threadLocalName = FqName("kotlin.native.concurrent.ThreadLocal")
                if (declaration.findAnnotation(threadLocalName) != null) return

                declaration.body?.getVarsViolatingFrozen()?.forEach { property ->
                    holder.registerProblem(
                        property.psiOrParent,
                        "Add @ThreadLocal annotation to object or replace var with val",
                        ProblemHighlightType.WARNING
                    )
                }
            }

            override fun visitEnumEntry(enumEntry: KtEnumEntry) {
                super.visitEnumEntry(enumEntry)

                enumEntry.body?.getVarsViolatingFrozen()?.forEach { property ->
                    holder.registerProblem(
                        property.psiOrParent,
                        "Enums are frozen by default, change var to val",
                        ProblemHighlightType.WARNING
                    )
                }
            }

            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)
                if (!klass.isEnum()) return

                klass.primaryConstructorParameters
                    .filter { it.isMutable }
                    .forEach { parameter ->
                        holder.registerProblem(
                            parameter.psiOrParent,
                            "Enums are frozen by default, change var to val",
                            ProblemHighlightType.WARNING
                        )
                    }
            }
        }
    }

    private fun KtClassBody.getVarsViolatingFrozen(): List<KtProperty> {
        return properties.filter { it.isVar && it.delegate == null && it.hasBackingField() }
    }

    private fun KtProperty.hasBackingField(): Boolean {
        // check if at least one default accessor is used
        if (accessors.size < 2) return true

        val childrenOfAccessors = accessors.flatMap { it.children.asList() }

        var result = false
        for (child in childrenOfAccessors) {
            if (result) break

            result = child.hasReferenceWithName("field")
        }

        return result
    }

    private fun PsiElement.hasReferenceWithName(name: String): Boolean {
        if (children.isEmpty()) return false

        val hasReference = children
            .mapNotNull { it as? KtNameReferenceExpression }
            .any { return it.text == name }

        return if (hasReference) {
            true
        } else {
            children.any { it.hasReferenceWithName(name) }
        }
    }
}