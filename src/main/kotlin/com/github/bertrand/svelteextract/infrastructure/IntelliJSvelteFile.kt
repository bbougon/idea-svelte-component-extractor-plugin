package com.github.bertrand.svelteextract.infrastructure

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.PsiReference

class IntelliJSvelteFile(private val psiFile: PsiFile) : SvelteFile {
    override fun visitElements(visitor: (SvelteElement) -> Unit) {
        psiFile.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                visitor(IntelliJElement(element))
            }
        })
    }
}

private class IntelliJElement(private val element: PsiElement) : SvelteElement {
    override val range = ElementRange(element.textRange.startOffset, element.textRange.endOffset)
    override val references = element.references.map { IntelliJReference(it) }
}

private class IntelliJReference(private val ref: PsiReference) : SvelteReference {
    override fun resolve(): SvelteNamedElement? {
        val resolved = ref.resolve() as? PsiNamedElement ?: return null
        return IntelliJNamedElement(resolved)
    }
}

private class IntelliJNamedElement(private val element: PsiNamedElement) : SvelteNamedElement {
    override val range = element.textRange?.let { ElementRange(it.startOffset, it.endOffset) }
    override val name = element.name
}
