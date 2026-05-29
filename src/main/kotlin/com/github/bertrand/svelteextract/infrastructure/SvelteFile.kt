package com.github.bertrand.svelteextract.infrastructure

data class ElementRange(val start: Int, val end: Int) {
    fun intersects(other: ElementRange) = start < other.end && other.start < end
    fun contains(other: ElementRange) = start <= other.start && other.end <= end
}

interface SvelteNamedElement {
    val range: ElementRange?
    val name: String?
}

interface SvelteReference {
    fun resolve(): SvelteNamedElement?
}

interface SvelteElement {
    val range: ElementRange
    val references: List<SvelteReference>
}

fun interface SvelteFile {
    fun visitElements(visitor: (SvelteElement) -> Unit)
}
