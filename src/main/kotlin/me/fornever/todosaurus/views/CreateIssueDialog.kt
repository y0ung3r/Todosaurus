// SPDX-FileCopyrightText: 2024 Todosaurus contributors <https://github.com/ForNeVeR/Todosaurus>
//
// SPDX-License-Identifier: MIT

package me.fornever.todosaurus.views

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import kotlinx.coroutines.*
import me.fornever.todosaurus.TodosaurusBundle
import me.fornever.todosaurus.models.CreateIssueModel
import me.fornever.todosaurus.models.RepositoryModel
import me.fornever.todosaurus.services.GitHubService
import me.fornever.todosaurus.services.ToDoService
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import java.awt.event.ActionEvent
import javax.swing.Action

class CreateIssueDialog(
    private val project: Project,
    parentScope: CoroutineScope,
    private val accounts: Array<GithubAccount>,
    private val repositories: Array<RepositoryModel>,
    private val initialData: CreateIssueModel
) : DialogWrapper(null) {

    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob())

    init {
        isModal = false
        title = TodosaurusBundle.message("createIssueDialog.title")
        setSize(600, 400)
        init()
    }

    private lateinit var repositoryChooser: RepositoryChooser
    private lateinit var accountChooser: GitHubAccountChooser
    private lateinit var issueTitleField: JBTextField
    private lateinit var issueDescriptionField: JBTextArea

    override fun createCenterPanel() = panel {
        row(TodosaurusBundle.message("createIssueDialog.chooseRepository")) {
            repositoryChooser = RepositoryChooser(repositories).also {
                cell(it).align(AlignX.FILL)
            }
        }
        row(TodosaurusBundle.message("createIssueDialog.chooseAccount")) {
            accountChooser = GitHubAccountChooser(accounts).also {
                cell(it).align(AlignX.FILL)
            }
        }
        row(TodosaurusBundle.message("createIssueDialog.issueTitle")) {
            issueTitleField = textField()
                .align(AlignX.FILL)
                .text(initialData.toDoItem.title).component
        }
        row(TodosaurusBundle.message("createIssueDialog.issueDescription")) {
            issueDescriptionField = textArea()
                .align(Align.FILL)
                .text(initialData.toDoItem.description).component
        }.resizableRow()
    }

    override fun createActions(): Array<Action> = arrayOf(CreateIssueAction())

    override fun dispose() {
        scope.cancel()
        super.dispose()
    }

    private inner class CreateIssueAction : DialogWrapperAction(TodosaurusBundle.message("createIssueDialog.createIssue")) {

        @Volatile
        private var isInProgress: Boolean = false

        override fun isEnabled() =
            !isInProgress
                && accountChooser.selectedItem != null
                && repositoryChooser.selectedItem != null

        override fun doAction(e: ActionEvent?) {
            val model = CreateIssueModel(
                repositoryChooser.selectedItem as RepositoryModel?,
                accountChooser.selectedItem as GithubAccount?,
                initialData.toDoItem
            )
            scope.launch {
                isInProgress = true
                try {
                    val newIssue = GitHubService.getInstance(project).createIssue(model)
                    Notifications.CreateIssue.success(newIssue, project)
                    ToDoService.getInstance(project).updateDocumentText(model.toDoItem, newIssue)
                    withContext(Dispatchers.EDT) {
                        doOKAction()
                    }
                } finally {
                    isInProgress = false
                }
            }
            // TODO[#15]: Process IO and unknown errors
        }
    }
}

