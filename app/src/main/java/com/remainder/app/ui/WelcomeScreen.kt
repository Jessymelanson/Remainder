package com.remainder.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** One screen, once, explaining what the app is for before it asks for a number. */
@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(24.dp)),
            verticalArrangement = Arrangement.Center
        ) {
            Text("💖", style = MaterialTheme.typography.displayMedium)
            VSpace(12)
            Text("Remainder", style = MaterialTheme.typography.headlineLarge)
            VSpace(8)
            Text(
                "Type in one bi-weekly paycheck. Remainder takes the bills off it, " +
                    "then the shopping, then what you are putting away, and tells " +
                    "you what is actually left.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            VSpace(28)
            Point("🧾", "Bills first", "Because those are already promised.")
            Point("🛒", "Then living costs", "Groceries and fuel, the part you steer.")
            Point("🏦", "Then yourself", "Savings treated as a bill, not an afterthought.")
            Point(
                "🔒", "Nothing leaves the phone",
                "No account, no server, and no internet permission to use one."
            )

            VSpace(32)
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text("Let's go")
            }
            VSpace(20)
        }
    }
}

@Composable
private fun Point(icon: String, title: String, body: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon)
        Box(Modifier.size(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
