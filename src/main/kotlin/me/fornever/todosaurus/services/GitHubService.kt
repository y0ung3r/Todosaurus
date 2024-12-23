// SPDX-FileCopyrightText: 2024 Todosaurus contributors <https://github.com/ForNeVeR/Todosaurus>
//
// SPDX-License-Identifier: MIT

package me.fornever.todosaurus.services

import com.intellij.dvcs.repo.VcsRepositoryManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.concurrency.annotations.RequiresReadLock
import git4idea.repo.GitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.fornever.todosaurus.models.CreateIssueModel
import me.fornever.todosaurus.models.GetIssueModel
import me.fornever.todosaurus.models.RepositoryModel
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.api.GithubApiRequests
import org.jetbrains.plugins.github.api.GithubServerPath
import org.jetbrains.plugins.github.api.data.GithubIssue
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.util.GHCompatibilityUtil

@Service(Service.Level.PROJECT)
class GitHubService(private val project: Project) {

    companion object {
        const val GITHUB_CODE_URL_REPLACEMENT = "\${GITHUB_CODE_URL}"

        fun getInstance(project: Project): GitHubService = project.service()
    }

    suspend fun getIssue(model: GetIssueModel): GithubIssue? {
        val repository = model.repository ?: error("Repository for this project not found.")
        val issueNumber = readAction {
            model.issueNumber.toString()
        }

        val executorFactory = GithubApiRequestExecutor.Factory.getInstance()
        val executor = if (model.account != null) {
            executorFactory.create(model.account.server, getApiToken(model.account))
        } else executorFactory.create()

        val request = GithubApiRequests.Repos.Issues.get(GithubServerPath.DEFAULT_SERVER, repository.owner, repository.name, issueNumber)

        return withContext(Dispatchers.IO) {
            executor.execute(request)
        }
    }

    suspend fun createIssue(model: CreateIssueModel): GithubIssue {
        val repository = model.selectedRepository ?: error("Repository is not selected.")
        val account = model.selectedAccount ?: error("Account is not selected.")

        val token = getApiToken(account)
        val executor = GithubApiRequestExecutor.Factory.getInstance().create(account.server, token)

        val issueBody = readAction {
            replacePatterns(repository, model.toDoItem)
        }

        val request = GithubApiRequests.Repos.Issues.create(
            GithubServerPath.DEFAULT_SERVER,
            repository.owner,
            repository.name,
            model.toDoItem.title,
            issueBody
        )

        return withContext(Dispatchers.IO) {
            executor.execute(request)
        }
    }

    private fun getApiToken(account: GithubAccount): String {
        return GHCompatibilityUtil.getOrRequestToken(account, project) ?: error("Token is not found.")
    }

    @RequiresReadLock
    private fun replacePatterns(repository: RepositoryModel, toDoItem: ToDoItem): String {
        val rootPath = repository.rootPath
        val filePath = FileDocumentManager.getInstance().getFile(toDoItem.range.document)?.toNioPath()
            ?: error("Cannot find file for the requested document.")
        val path = FileUtil.getRelativePath(rootPath.toFile(), filePath.toFile())?.replace('\\', '/')
            ?: error("Cannot calculate relative path between \"${repository.rootPath}\" and \"${filePath}\".")

        val currentCommit = getCurrentCommitHash(repository)
        val startLineNumber = toDoItem.range.document.getLineNumber(toDoItem.range.startOffset) + 1
        val endLineNumber = toDoItem.range.document.getLineNumber(toDoItem.range.endOffset) + 1
        val lineDesignator = if (startLineNumber == endLineNumber) "L$startLineNumber" else "L$startLineNumber-L$endLineNumber"
        val linkText =
            "https://github.com/${repository.owner}/${repository.name}/blob/$currentCommit/$path#$lineDesignator"

        return toDoItem.description.replace(GITHUB_CODE_URL_REPLACEMENT, linkText)
    }

    private fun getCurrentCommitHash(model: RepositoryModel): String {
        val manager = VcsRepositoryManager.getInstance(project)
        val virtualRoot = LocalFileSystem.getInstance().findFileByNioFile(model.rootPath)
            ?: error("Cannot find virtual file for \"${model.rootPath}\".")

        val repository = manager.repositories
            .asSequence()
            .filterIsInstance<GitRepository>()
            .filter { it.root == virtualRoot }
            .singleOrNull()
            ?: error("Cannot find a Git repository for \"${model.rootPath}\".")
        return repository.info.currentRevision
            ?: error("Cannot determine the current revision for \"${model.rootPath}\".")
    }
}
