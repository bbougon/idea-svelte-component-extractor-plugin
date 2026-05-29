package com.github.bertrand.svelteextract.infrastructure

import com.github.bertrand.svelteextract.domain.SvelteParser

class IntelliJSvelteParser(
    private val svelteFile: SvelteFile,
    private val selectionStart: Int,
    private val selectionEnd: Int
) : SvelteParser {

    override fun findUsedVariables(content: String): List<String> {
        val usedVariables = mutableSetOf<String>()
        val selectionRange = ElementRange(selectionStart, selectionEnd)

        svelteFile.visitElements { element ->
            if (!selectionRange.intersects(element.range)) return@visitElements

            for (ref in element.references) {
                val resolved = ref.resolve() ?: continue
                val resolvedRange = resolved.range ?: continue
                if (!selectionRange.contains(resolvedRange)) {
                    val name = resolved.name ?: continue
                    if (name.isNotBlank()) usedVariables.add(name)
                }
            }
        }

        return usedVariables.toList().sorted()
    }
}
