// SPDX-FileCopyrightText: 2024–2025 Todosaurus contributors <https://github.com/ForNeVeR/Todosaurus>
//
// SPDX-License-Identifier: MIT

package me.fornever.todosaurus.core.git

import com.intellij.dvcs.repo.VcsRepositoryManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import git4idea.repo.GitRepository
import me.fornever.todosaurus.core.issueTrackers.IssueTrackerConnectionDetails
import java.net.URI

@Service(Service.Level.PROJECT)
class GitHostingRemoteProvider(private val project: Project) {
    companion object {
        fun getInstance(project: Project): GitHostingRemoteProvider = project.service()
    }

    fun provideAll(connectionDetails: IssueTrackerConnectionDetails): Array<GitHostingRemote>
        = VcsRepositoryManager
            .getInstance(project)
            .getRepositories()
            .asSequence()
            .filterIsInstance<GitRepository>()
            .flatMap { mapToRemotes(it, connectionDetails) }
            .distinct()
            .toList()
            .toTypedArray()

    private fun mapToRemotes(repository: GitRepository, connectionDetails: IssueTrackerConnectionDetails): Sequence<GitHostingRemote>
        = repository
            .remotes
            .asSequence()
            .flatMap { it.urls }
            .flatMap { getRepositoryUrls(it, connectionDetails) }
            .map { GitHostingRemote(it, repository.root.toNioPath()) }

    private fun getRepositoryUrls(remoteUrl: String, connectionDetails: IssueTrackerConnectionDetails): List<URI> {
        val serverHost = connectionDetails.serverHost ?: return emptyList()

        return when {
            remoteUrl.startsWith("https://${serverHost}/") -> listOf(fromHttps(remoteUrl))
            remoteUrl.startsWith("git@${serverHost}:") -> listOf(fromSsh(remoteUrl))
            else -> emptyList()
        }
    }

    private fun fromHttps(remoteUrl: String)
        = URI(remoteUrl.removeSuffix(".git"))

    private fun fromSsh(remoteUrl: String)
        = URI("https://" + remoteUrl
            .removePrefix("git@")
            .replace(":", "/")
            .removeSuffix(".git"))
}
