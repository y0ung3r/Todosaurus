// SPDX-FileCopyrightText: 2024-2025 Todosaurus contributors <https://github.com/ForNeVeR/Todosaurus>
//
// SPDX-License-Identifier: MIT

package me.fornever.todosaurus.core.wizardTests.memoizationTests.readerTests

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import me.fornever.todosaurus.core.git.GitHostingRemote
import me.fornever.todosaurus.core.issues.IssuePlacementDetails
import me.fornever.todosaurus.core.issues.IssuePlacementDetailsType
import me.fornever.todosaurus.core.ui.wizard.memoization.UserChoice
import me.fornever.todosaurus.core.ui.wizard.memoization.UserChoiceReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserChoiceTests {
    companion object {
        private const val CREDENTIALS_IDENTIFIER = "Identifier"

        private val placementDetails: Pair<String, JsonElement>
            = UserChoice::placementDetails.name to JsonObject(mapOf(
                IssuePlacementDetails::type.name to JsonPrimitive(IssuePlacementDetailsType.GitBased.name),
                GitHostingRemote::url.name to JsonPrimitive("https://example.com"),
                GitHostingRemote::rootPath.name to JsonPrimitive("\\home")
            ))

        private const val ISSUE_TRACKER_ID = "GitHub"
    }

    @Test
    fun `Should throws error if issue tracker type is not primitive`() {
        // Arrange
        val actual = UserChoice()
        val sut = UserChoiceReader(
            JsonObject(
                mapOf(
                    UserChoice::issueTrackerId.name to JsonObject(emptyMap()),
                    UserChoice::credentialsId.name to JsonPrimitive(CREDENTIALS_IDENTIFIER),
                    placementDetails
                )
            )
        )

        // Act & Assert
        assertThrows<Exception> {
            sut.visit(actual)
        }
    }

    @Test
    fun `Should throws error if credentials identifier has invalid value`() {
        // Arrange
        val actual = UserChoice()
        val sut = UserChoiceReader(
            JsonObject(
                mapOf(
                    UserChoice::issueTrackerId.name to JsonPrimitive(ISSUE_TRACKER_ID),
                    UserChoice::credentialsId.name to JsonNull,
                    placementDetails
                )
            )
        )

        // Act & Assert
        assertThrows<Exception> {
            sut.visit(actual)
        }
    }

    @Test
    fun `Should throws error if credentials identifier is not primitive`() {
        // Arrange
        val actual = UserChoice()
        val sut = UserChoiceReader(
            JsonObject(
                mapOf(
                    UserChoice::issueTrackerId.name to JsonPrimitive(ISSUE_TRACKER_ID),
                    UserChoice::credentialsId.name to JsonObject(emptyMap()),
                    placementDetails
                )
            )
        )

        // Act & Assert
        assertThrows<Exception> {
            sut.visit(actual)
        }
    }

    @Test
    fun `Should read primary user choice fields properly`() {
        // Arrange
        val actual = UserChoice()
        val sut = UserChoiceReader(
			JsonObject(
				mapOf(
					UserChoice::issueTrackerId.name to JsonPrimitive(ISSUE_TRACKER_ID),
					UserChoice::credentialsId.name to JsonPrimitive(CREDENTIALS_IDENTIFIER),
					placementDetails
				)
			)
		)

        // Act
        sut.visit(actual)

        // Assert
		assertEquals(ISSUE_TRACKER_ID, actual.issueTrackerId)
		assertEquals(CREDENTIALS_IDENTIFIER, actual.credentialsId)
    }
}
