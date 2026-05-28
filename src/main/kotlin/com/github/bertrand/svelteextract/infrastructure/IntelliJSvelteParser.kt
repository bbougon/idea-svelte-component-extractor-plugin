package com.github.bertrand.svelteextract.infrastructure

import com.github.bertrand.svelteextract.domain.SvelteParser
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil

class IntelliJSvelteParser(private val contextElement: PsiElement) : SvelteParser {

    override fun findUsedVariables(content: String): List<String> {
        val usedVariables = mutableSetOf<String>()
        
        contextElement.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                
                // On cherche les références (variables, fonctions, etc.)
                val references = element.references
                for (ref in references) {
                    val resolved = ref.resolve()
                    if (resolved != null) {
                        // Si l'élément résolu est EN DEHORS de notre sélection, c'est une prop potentielle
                        if (!PsiTreeUtil.isAncestor(contextElement, resolved, false)) {
                            // On récupère le nom de la variable
                            val name = resolved.text // Simplification, idéalement via PsiNamedElement
                            usedVariables.add(name)
                        }
                    }
                }
            }
        })
        
        return usedVariables.toList().sorted()
    }
}
