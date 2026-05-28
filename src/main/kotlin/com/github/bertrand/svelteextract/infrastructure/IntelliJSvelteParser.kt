package com.github.bertrand.svelteextract.infrastructure

import com.github.bertrand.svelteextract.domain.SvelteParser
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiRecursiveElementVisitor

class IntelliJSvelteParser(
    private val psiFile: PsiFile,
    private val selectionStart: Int,
    private val selectionEnd: Int
) : SvelteParser {

    override fun findUsedVariables(content: String): List<String> {
        val usedVariables = mutableSetOf<String>()
        val selectionRange = TextRange(selectionStart, selectionEnd)

        psiFile.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (!selectionRange.intersects(element.textRange)) return

                for (ref in element.references) {
                    val resolved = ref.resolve() as? PsiNamedElement ?: continue
                    val resolvedRange = resolved.textRange ?: continue
                    if (!selectionRange.contains(resolvedRange)) {
                        val name = resolved.name ?: continue
                        if (name.isNotBlank()) usedVariables.add(name)
                    }
                }
            }
        })

        return usedVariables.toList().sorted()
    }
}
