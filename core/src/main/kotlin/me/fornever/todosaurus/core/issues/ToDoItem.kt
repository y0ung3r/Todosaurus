// SPDX-FileCopyrightText: 2024-2025 Todosaurus contributors <https://github.com/ForNeVeR/Todosaurus>
//
// SPDX-License-Identifier: MIT

package me.fornever.todosaurus.core.issues

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.RangeMarker
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import me.fornever.todosaurus.core.settings.TodosaurusSettings

class ToDoItem(private val settings: TodosaurusSettings.State, val toDoRange: RangeMarker) {
    private companion object {
        val newItemPattern: Regex
            = Regex("\\b(?i)TODO(?-i)\\b:?(?!\\[.*?])") // https://regex101.com/r/lDDqm7/2
    }

    private val text: String
        get() = ReadAction.compute<String, Nothing> {
            toDoRange
                .document
                .getText(toDoRange.textRange)
        }

    var title: String = text
        .substringBefore('\n')
        .replace(newItemPattern, "")
        .trim()

    var description: String = (if (text.contains("\n")) text.substringAfter('\n') + "\n" else "") +
        settings.descriptionTemplate

    val issueNumber: String?
        get() {
            if (isNew)
                return null

            return text
                .substringAfter("[")
                .substringBefore("]")
                .replace("#", "")
        }

    @RequiresWriteLock
    fun markAsReported(issueNumber: String) {
        if (!isNew)
            return

        val previousText = text
        val newText = previousText.replace(newItemPattern, formReportedItemPattern(issueNumber))
        toDoRange.document.replaceString(toDoRange.startOffset, toDoRange.endOffset, newText)
    }

    val isNew: Boolean
        get() = newItemPattern.containsMatchIn(text)

    private fun formReportedItemPattern(issueNumber: String): String {
        // TODO[#134]: Allow to customize template for issue number. This is difficult task because the "newItemPattern" is now linked to a regular [.*?] pattern
        return "TODO${settings.numberPattern}".replace(TodosaurusSettings.ISSUE_NUMBER_REPLACEMENT, issueNumber)
    }
}
