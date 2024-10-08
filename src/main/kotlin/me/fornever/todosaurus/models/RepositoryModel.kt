// SPDX-FileCopyrightText: 2024 Todosaurus contributors <https://github.com/ForNeVeR/Todosaurus>
//
// SPDX-License-Identifier: MIT

package me.fornever.todosaurus.models

import java.net.URI
import java.nio.file.Path

data class RepositoryModel(val url: URI, val rootPath: Path) {
    val ownerAndName: String
        get() = url.path.removePrefix("/")
    val owner: String
        get() = ownerAndName.substringBefore('/')
    val name: String
        get() = ownerAndName.substringAfter('/')
}
