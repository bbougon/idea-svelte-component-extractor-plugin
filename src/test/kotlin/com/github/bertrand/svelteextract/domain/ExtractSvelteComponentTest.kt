package com.github.bertrand.svelteextract.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains

class ExtractSvelteComponentTest {

    @Test
    fun `should extract simple html to new component`() {
        val sourceCode = """
            <div>
                <h1>Hello</h1>
            </div>
        """.trimIndent()

        val request = ExtractRequest(
            sourceCode = sourceCode,
            selectedContent = "<h1>Hello</h1>",
            newComponentName = "Header"
        )

        val result = ExtractSvelteComponentUseCase().execute(request)

        assertEquals("<h1>Hello</h1>", result.newComponentContent.trim())
        assertContains(result.modifiedSourceCode.trim(), "<div>\n    <Header />\n</div>")
    }

    @Test
    fun `should add import to source file when extracting component`() {
        val sourceCode = """
            <script>
                let name = 'world';
            </script>
            <div>
                <h1>Hello</h1>
            </div>
        """.trimIndent()

        val request = ExtractRequest(
            sourceCode = sourceCode,
            selectedContent = "<h1>Hello</h1>",
            newComponentName = "Header"
        )

        val result = ExtractSvelteComponentUseCase().execute(request)

        val expectedSource = """
            <script>
                import Header from './Header.svelte';
                let name = 'world';
            </script>
            <div>
                <Header />
            </div>
        """.trimIndent()

        assertEquals(expectedSource, result.modifiedSourceCode.trim())
    }

    @Test
    fun `should detect and pass variables as props`() {
        val sourceCode = """
            <script>
                let name = 'world';
                let count = 0;
            </script>
            <div>
                <h1>Hello {name}</h1>
                <p>Count is {count}</p>
            </div>
        """.trimIndent()

        val request = ExtractRequest(
            sourceCode = sourceCode,
            selectedContent = "<h1>Hello {name}</h1>",
            newComponentName = "Title"
        )

        // Stubbing the parser
        val parserStub = object : SvelteParser {
            override fun findUsedVariables(content: String): List<String> = listOf("name")
        }

        val result = ExtractSvelteComponentUseCase(parserStub).execute(request)

        // Attendu dans le nouveau composant
        val expectedNewContent = """
            <script>
                export let name;
            </script>
            <h1>Hello {name}</h1>
        """.trimIndent()

        // Attendu dans le fichier source
        val expectedSource = """
            <script>
                import Title from './Title.svelte';
                let name = 'world';
                let count = 0;
            </script>
            <div>
                <Title name={name} />
                <p>Count is {count}</p>
            </div>
        """.trimIndent()

        assertEquals(expectedNewContent, result.newComponentContent.trim())
        assertEquals(expectedSource, result.modifiedSourceCode.trim())
    }

    @Test
    fun `should add import to lang ts script tag`() {
        val sourceCode = """
            <script lang="ts">
                let name: string = 'world';
            </script>
            <div>
                <h1>Hello</h1>
            </div>
        """.trimIndent()

        val request = ExtractRequest(
            sourceCode = sourceCode,
            selectedContent = "<h1>Hello</h1>",
            newComponentName = "Header"
        )

        val result = ExtractSvelteComponentUseCase().execute(request)

        val expectedSource = """
            <script lang="ts">
                import Header from './Header.svelte';
                let name: string = 'world';
            </script>
            <div>
                <Header />
            </div>
        """.trimIndent()

        assertEquals(expectedSource, result.modifiedSourceCode.trim())
    }

    @Test
    fun `should propagate lang attribute to new component script tag`() {
        val sourceCode = """
            <script lang="ts">
                let name: string = 'world';
            </script>
            <div>
                <h1>Hello {name}</h1>
            </div>
        """.trimIndent()

        val request = ExtractRequest(
            sourceCode = sourceCode,
            selectedContent = "<h1>Hello {name}</h1>",
            newComponentName = "Title"
        )

        val parserStub = object : SvelteParser {
            override fun findUsedVariables(content: String): List<String> = listOf("name")
        }

        val result = ExtractSvelteComponentUseCase(parserStub).execute(request)

        val expectedNewContent = """
            <script lang="ts">
                export let name;
            </script>
            <h1>Hello {name}</h1>
        """.trimIndent()

        assertEquals(expectedNewContent, result.newComponentContent.trim())
    }

    @Test
    fun `should copy needed imports to new component`() {
        val sourceCode = """
            <script lang="ts">
                import { UnComposant } from '@lucide/svelte';
                import { AutreComposant } from '@lucide/svelte';
                let name = 'world';
            </script>
            <div>
                <div class="wrapper">
                    <UnComposant class="w-5 h-5" />
                </div>
                <AutreComposant />
            </div>
        """.trimIndent()

        val request = ExtractRequest(
            sourceCode = sourceCode,
            selectedContent = """
                <div class="wrapper">
                    <UnComposant class="w-5 h-5" />
                </div>
            """.trimIndent(),
            newComponentName = "Wrapper"
        )

        val result = ExtractSvelteComponentUseCase().execute(request)

        val expectedNewContent = """
            <script lang="ts">
                import { UnComposant } from '@lucide/svelte';
            </script>
            <div class="wrapper">
                <UnComposant class="w-5 h-5" />
            </div>
        """.trimIndent()

        assertEquals(expectedNewContent, result.newComponentContent.trim())
    }

    @Test
    fun `should detect imports regardless of whitespace`() {
        val sourceCode = """
            <script lang="ts">
               import    UnComposant    from    '@lucide/svelte';
                let name = 'world';
            </script>
            <div>
                <UnComposant class="w-5 h-5" />
            </div>
        """.trimIndent()

        val request = ExtractRequest(
            sourceCode = sourceCode,
            selectedContent = """<UnComposant class="w-5 h-5" />""",
            newComponentName = "Wrapper"
        )

        val result = ExtractSvelteComponentUseCase().execute(request)

        assertContains(result.newComponentContent, "import UnComposant from '@lucide/svelte';")
    }

    @Test
    fun `should not add script tag if no props are needed`() {
        val sourceCode = """
            <div>
                <h1>Hello</h1>
            </div>
        """.trimIndent()

        val request = ExtractRequest(
            sourceCode = sourceCode,
            selectedContent = "<h1>Hello</h1>",
            newComponentName = "Header"
        )

        val parserStub = object : SvelteParser {
            override fun findUsedVariables(content: String): List<String> = emptyList()
        }

        val result = ExtractSvelteComponentUseCase(parserStub).execute(request)

        assertEquals("<h1>Hello</h1>", result.newComponentContent.trim())
    }

    @Test
    fun `should add script tag to new component when props are needed`() {
        val sourceCode = "<div>{name}</div>"
        val request = ExtractRequest(
            sourceCode = sourceCode,
            selectedContent = "{name}",
            newComponentName = "Name"
        )

        val parserStub = object : SvelteParser {
            override fun findUsedVariables(content: String): List<String> = listOf("name")
        }

        val result = ExtractSvelteComponentUseCase(parserStub).execute(request)

        val expectedNewContent = """
            <script>
                export let name;
            </script>
            {name}
        """.trimIndent()

        assertEquals(expectedNewContent, result.newComponentContent.trim())
    }

    @Test
    fun `should create script tag in source file if it does not exist`() {
        val sourceCode = """
            <div>
                <h1>Hello</h1>
            </div>
        """.trimIndent()

        val request = ExtractRequest(
            sourceCode = sourceCode,
            selectedContent = "<h1>Hello</h1>",
            newComponentName = "Header"
        )

        val result = ExtractSvelteComponentUseCase().execute(request)

        val expectedSource = """
            <script>
                import Header from './Header.svelte';
            </script>
            <div>
                <Header />
            </div>
        """.trimIndent()

        assertEquals(expectedSource.trim(), result.modifiedSourceCode.trim())
    }
}
