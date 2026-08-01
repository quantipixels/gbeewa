package com.quantipixels.gbeewa.showcase

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DecomposeRootTest {

    @Test
    fun `each destination is a child stack entry and back pops one entry`() {
        withRoot { root ->
            assertEquals(1, root.stack.value.items.size)
            assertIs<DecomposeChild.Home>(root.stack.value.active.instance)

            root.openNameEditor("Ada")
            assertEquals(2, root.stack.value.items.size)
            val editor = assertIs<DecomposeChild.NameEditor>(root.stack.value.active.instance)
            assertEquals("Ada", editor.initialName)

            root.pop()
            assertEquals(1, root.stack.value.items.size)
            assertIs<DecomposeChild.Home>(root.stack.value.active.instance)
        }
    }

    @Test
    fun `color picker entry keeps its receiver discriminator`() {
        withRoot { root ->
            root.openColorPicker(SelectionResultKey("accent-color"))

            val child = assertIs<DecomposeChild.ColorPicker>(root.stack.value.active.instance)
            assertEquals("accent-color", child.resultKey.receiverId)
        }
    }

    private fun withRoot(block: (DecomposeRoot) -> Unit) {
        val lifecycle = LifecycleRegistry().apply { resume() }
        try {
            block(DecomposeRoot(DefaultComponentContext(lifecycle = lifecycle)))
        } finally {
            lifecycle.destroy()
        }
    }
}
