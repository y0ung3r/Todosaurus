package me.fornever.todosaurus.wizardTests

import me.fornever.todosaurus.ui.wizard.OptionalStepProvider
import me.fornever.todosaurus.ui.wizard.TodosaurusStep
import javax.swing.JComponent

class FakeOptionalStepProvider(override val id: Any, override val optionalSteps: MutableList<TodosaurusStep>) : TodosaurusStep(), OptionalStepProvider {
    override fun getComponent(): JComponent = error("Not implemented.")

    override fun getPreferredFocusedComponent(): JComponent = error("Not implemented.")

    override fun isComplete(): Boolean = error("Not implemented.")

    override fun chooseOptionalStepId(): Any = error("Not implemented.")
}