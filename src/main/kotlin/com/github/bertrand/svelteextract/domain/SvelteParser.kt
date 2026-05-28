package com.github.bertrand.svelteextract.domain

interface SvelteParser {
    fun findUsedVariables(content: String): List<String>
}
