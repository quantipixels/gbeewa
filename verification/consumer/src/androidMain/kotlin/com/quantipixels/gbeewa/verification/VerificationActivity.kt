package com.quantipixels.gbeewa.verification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.quantipixels.gbeewa.*

private object Pick : ResultKey<String>
private object Handoff : ResultKey<String>

class VerificationActivity : ComponentActivity() {
    private lateinit var currentStore: ResultStore
    fun capturePickerCallback(): (String) -> Unit {
        val owner = currentStore
        return { owner.put(Pick, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var workspace by remember { mutableIntStateOf(0) }
            var route by rememberSaveable { mutableStateOf("home") }
            var deliveries by remember { mutableIntStateOf(0) }
            var dependentWork by remember { mutableIntStateOf(0) }
            var status by remember { mutableStateOf("home") }
            val store = remember(workspace) { ResultStore() }
            val consumer = remember(store) { ResultConsumer(store) }
            SideEffect { currentStore = store }
            CompositionLocalProvider(LocalResultConsumer provides consumer) {
                ResultEffect(Pick) { deliveries++ }
            }
            if (route == "detail") {
                LaunchedEffect(consumer, route) {
                    when (consumer.consume(Handoff)) {
                        PendingResult.Missing -> status = "fallback"
                        is PendingResult.Value -> {
                            dependentWork++
                            status = "detail"
                        }
                    }
                }
            }
            Column {
                Button(onClick = { workspace++ }, Modifier.testTag("replace-owner")) { Text("Replace workspace") }
                Button(onClick = { store.put(Pick, "current") }, Modifier.testTag("publish")) { Text("Publish") }
                Button(onClick = {
                    store.put(Handoff, "required-input")
                    route = "detail"
                }, Modifier.testTag("open-detail")) { Text("Open detail") }
                Button(onClick = { consumer.consume(Pick) { deliveries++ } }, Modifier.testTag("consume-again")) {
                    Text("Consume again")
                }
                Text("deliveries:$deliveries", Modifier.testTag("deliveries"))
                Text("work:$dependentWork", Modifier.testTag("work"))
                Text(status, Modifier.testTag("status"))
            }
        }
    }
}
