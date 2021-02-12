package com.github.igrrik.kmmfrozeninspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor

class NativeWorkerCapturingOuterScopeInspection: AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {

            private val workerExecuteFqName = FqName("kotlin.native.concurrent.Worker.execute")
            private val threadLocalFqName = FqName("kotlin.native.concurrent.ThreadLocal")
            private val sharedImmutableFqName = FqName("kotlin.native.concurrent.SharedImmutable")

            override fun visitDoubleColonExpression(expression: KtDoubleColonExpression) {
                super.visitDoubleColonExpression(expression)

                if (expression.isEmptyLHS) return

                val callExpression = expression
                    .getParentOfType<KtDotQualifiedExpression>(false)
                    ?.callExpression ?: return

                if (!callExpression.isWorkerExecuteFun()) return

                val lastArgument = callExpression.valueArguments.lastOrNull() ?: return

                if (lastArgument.text != expression.text) return

                holder.registerProblem(
                    expression,
                    "Only global functions may be passed as argument to execute job lambda",
                    ProblemHighlightType.WARNING
                )
            }

            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)

                if (!expression.isWorkerExecuteFun()) return

                val lambdaArgument = expression.valueArguments[2] as? KtLambdaArgument ?: return
                val lambdaExpression = lambdaArgument.getLambdaExpression() ?: return

                val references = lambdaExpression.bodyExpression?.collectDescendantsOfType<KtNameReferenceExpression>() ?: return

                val parentBlock = lambdaExpression.children.first().children.last()

                val allowedAnnotations = listOf(sharedImmutableFqName, threadLocalFqName)

                loop@ for (reference in references) {
                    val resolvedReference = reference.resolveReference() ?: continue@loop

                    val property = resolvedReference as? KtProperty ?: continue@loop

                    if (property.isTopLevel) {
                        if (allowedAnnotations.all { property.findAnnotation(it) == null }) {
                            holder.registerProblem(
                                reference,
                                "Accessing global properties not marked with @ThreadLocal or @SharedImmutable will cause exception",
                                ProblemHighlightType.WARNING
                            )
                        }
                    } else if (!parentBlock.isAncestor(property.originalElement)) {
                        holder.registerProblem(
                            reference,
                            "Capturing outer scope will cause exception",
                            ProblemHighlightType.WARNING
                        )
                    }
                }
            }

            private fun KtCallExpression.isWorkerExecuteFun(): Boolean {
                val resolvedReference = calleeExpression?.resolveReference() ?: return false
                return resolvedReference.getKotlinFqName() == workerExecuteFqName
            }

            private fun KtExpression.resolveReference(): PsiElement? {
                return references
                    .first { it is KtSimpleNameReference }
                    .resolve()
            }
        }
    }
}