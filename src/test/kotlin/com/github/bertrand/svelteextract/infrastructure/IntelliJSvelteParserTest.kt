package com.github.bertrand.svelteextract.infrastructure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntelliJSvelteParserTest {

    private fun namedElement(name: String, range: ElementRange): SvelteNamedElement =
        object : SvelteNamedElement {
            override val range = range
            override val name = name
        }

    private fun reference(resolvedTo: SvelteNamedElement?): SvelteReference =
        object : SvelteReference {
            override fun resolve() = resolvedTo
        }

    private fun element(range: ElementRange, vararg refs: SvelteReference): SvelteElement =
        object : SvelteElement {
            override val range = range
            override val references = refs.toList()
        }

    private fun fakeFile(vararg elements: SvelteElement): SvelteFile =
        SvelteFile { visitor -> elements.forEach(visitor) }

    private fun parser(file: SvelteFile, selectionStart: Int, selectionEnd: Int) =
        IntelliJSvelteParser(file, selectionStart, selectionEnd)

    @Test
    fun `returns empty list when no elements in file`() {
        val result = parser(fakeFile(), 0, 100).findUsedVariables("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns empty list when element is outside selection range`() {
        val outsideElement = element(ElementRange(200, 300))
        val result = parser(fakeFile(outsideElement), 0, 100).findUsedVariables("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns empty list when element has no references`() {
        val elementWithNoRefs = element(ElementRange(10, 50))
        val result = parser(fakeFile(elementWithNoRefs), 0, 100).findUsedVariables("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns empty list when reference resolves to null`() {
        val nullRef = reference(resolvedTo = null)
        val e = element(ElementRange(10, 50), nullRef)
        val result = parser(fakeFile(e), 0, 100).findUsedVariables("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns empty list when resolved element range is inside selection`() {
        val declared = namedElement("localVar", ElementRange(20, 30))
        val ref = reference(declared)
        val e = element(ElementRange(20, 80), ref)
        val result = parser(fakeFile(e), 0, 100).findUsedVariables("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `detects variable declared outside selection`() {
        val external = namedElement("name", ElementRange(0, 20))   // declared before selection
        val ref = reference(external)
        val e = element(ElementRange(50, 80), ref)                  // element within selection
        val result = parser(fakeFile(e), 40, 100).findUsedVariables("")
        assertEquals(listOf("name"), result)
    }

    @Test
    fun `returns multiple external variables sorted alphabetically`() {
        val beta = namedElement("beta", ElementRange(0, 10))
        val alpha = namedElement("alpha", ElementRange(10, 20))
        val e = element(
            ElementRange(50, 90),
            reference(beta),
            reference(alpha)
        )
        val result = parser(fakeFile(e), 40, 100).findUsedVariables("")
        assertEquals(listOf("alpha", "beta"), result)
    }

    @Test
    fun `deduplicates variable referenced multiple times`() {
        val external = namedElement("name", ElementRange(0, 10))
        val e = element(
            ElementRange(50, 90),
            reference(external),
            reference(external)
        )
        val result = parser(fakeFile(e), 40, 100).findUsedVariables("")
        assertEquals(listOf("name"), result)
    }

    @Test
    fun `ignores resolved elements with blank name`() {
        val blank = namedElement("   ", ElementRange(0, 10))
        val e = element(ElementRange(50, 80), reference(blank))
        val result = parser(fakeFile(e), 40, 100).findUsedVariables("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `ignores resolved elements with null range`() {
        val noRange = object : SvelteNamedElement {
            override val range: ElementRange? = null
            override val name = "orphan"
        }
        val e = element(ElementRange(50, 80), reference(noRange))
        val result = parser(fakeFile(e), 40, 100).findUsedVariables("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `content parameter has no effect on result`() {
        val external = namedElement("name", ElementRange(0, 20))
        val e = element(ElementRange(50, 80), reference(external))
        val file = fakeFile(e)
        val p = parser(file, 40, 100)
        assertEquals(p.findUsedVariables(""), p.findUsedVariables("anything"))
    }

    @Test
    fun `collects variables from multiple elements`() {
        val alpha = namedElement("alpha", ElementRange(0, 10))
        val beta = namedElement("beta", ElementRange(10, 20))
        val e1 = element(ElementRange(50, 60), reference(alpha))
        val e2 = element(ElementRange(60, 70), reference(beta))
        val result = parser(fakeFile(e1, e2), 40, 100).findUsedVariables("")
        assertEquals(listOf("alpha", "beta"), result)
    }
}
