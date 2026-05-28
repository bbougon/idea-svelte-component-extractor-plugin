package com.github.bertrand.svelteextract.domain

data class ExtractRequest(
    val sourceCode: String,
    val selectedContent: String,
    val newComponentName: String
)

data class ExtractResult(
    val newComponentContent: String,
    val modifiedSourceCode: String
)

class ExtractSvelteComponentUseCase(private val parser: SvelteParser? = null) {
    fun execute(request: ExtractRequest): ExtractResult {
        val usedVariables = parser?.findUsedVariables(request.selectedContent) ?: emptyList()
        
        val propsString = usedVariables.joinToString(" ") { "$it={$it}" }
        val componentCall = if (propsString.isEmpty()) {
            "<${request.newComponentName} />"
        } else {
            "<${request.newComponentName} $propsString />"
        }

        var modifiedSourceCode = request.sourceCode.replace(
            request.selectedContent,
            componentCall
        )

        val importStatement = "import ${request.newComponentName} from './${request.newComponentName}.svelte';"
        
        modifiedSourceCode = if (modifiedSourceCode.contains(Regex("""<script[^>]*>"""))) {
            modifiedSourceCode.replace(Regex("""<script[^>]*>""")) { match ->
                "${match.value}\n    $importStatement"
            }
        } else {
            "<script>\n    $importStatement\n</script>\n$modifiedSourceCode"
        }

        val scriptTag = Regex("""<script([^>]*)>""").find(request.sourceCode)
        val scriptAttributes = scriptTag?.groupValues?.get(1) ?: ""

        val importRegex = Regex("""import\s+\{?\s*(\w+)[\w\s,]*\}?\s+from\s+['"][^'"]+['"];?""")
        val neededImports = importRegex.findAll(request.sourceCode)
            .filter { match ->
                val identifier = match.groupValues[1]
                identifier.isNotEmpty() && request.selectedContent.contains(identifier)
            }
            .map { match ->
                val normalized = match.value.trim().replace(Regex("""\s+"""), " ").trimEnd(';')
                "    $normalized;"
            }
            .toList()

        val propsDeclarations = usedVariables.map { "    export let $it;" }
        val newComponentContent = when {
            usedVariables.isNotEmpty() || neededImports.isNotEmpty() -> {
                val scriptBody = (neededImports + propsDeclarations).joinToString("\n")
                "<script$scriptAttributes>\n$scriptBody\n</script>\n${request.selectedContent}"
            }
            else -> request.selectedContent
        }

        return ExtractResult(
            newComponentContent = newComponentContent,
            modifiedSourceCode = modifiedSourceCode
        )
    }
}
