package com.github.bertrand.svelteextract.infrastructure

import com.github.bertrand.svelteextract.domain.ExtractRequest
import com.github.bertrand.svelteextract.domain.ExtractSvelteComponentUseCase
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiUtilBase
import dev.blachut.svelte.lang.SvelteHTMLLanguage

class ExtractSvelteComponentAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val isSvelteFile = psiFile?.virtualFile?.extension == "svelte"

        e.presentation.isEnabledAndVisible = e.project != null &&
                editor != null &&
                isSvelteFile &&
                editor.selectionModel.hasSelection()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText ?: return

        // 1. Demander le nom du composant
        val componentName = Messages.showInputDialog(
            project,
            "Nom du nouveau composant :",
            "Extraire le composant Svelte",
            Messages.getQuestionIcon()
        ) ?: return

        if (componentName.isBlank()) return

        // 2. Préparer l'extraction via le domaine
        // On récupère l'élément PSI à la position du curseur pour l'analyse des variables
        val elementAtCaret = psiFile.findElementAt(selectionModel.selectionStart) ?: return
        val parser = IntelliJSvelteParser(elementAtCaret)
        val useCase = ExtractSvelteComponentUseCase(parser)
        
        val request = ExtractRequest(
            sourceCode = psiFile.text,
            selectedContent = selectedText,
            newComponentName = componentName
        )

        val result = useCase.execute(request)

        // 3. Appliquer les modifications (Write Action)
        WriteCommandAction.runWriteCommandAction(project) {
            // A. Créer le nouveau fichier
            val directory = psiFile.containingDirectory ?: return@runWriteCommandAction
            val newFileName = "$componentName.svelte"
            
            val newFile = PsiFileFactory.getInstance(project).createFileFromText(
                newFileName,
                SvelteHTMLLanguage.INSTANCE,
                result.newComponentContent
            )
            directory.add(newFile)

            // B. Modifier le fichier source
            val document = editor.document
            document.setText(result.modifiedSourceCode)
        }
    }
}
