package com.hao.mvi.feature.counter.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hao.mvi.core.base.ObserveAsEvents
import com.hao.mvi.core.ui.theme.MviTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun CounterScreen(
    viewModel: CounterViewModel = koinViewModel(),
    onNavigateToDetail: (Int) -> Unit = {}
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.effect) { effect ->
        when (effect) {
            is CounterEffect.ShowToast -> {
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
            is CounterEffect.NavigateToDetail -> {
                onNavigateToDetail(effect.count)
            }
        }
    }

    CounterContent(
        state = state,
        onIncrement = { viewModel.sendEvent(CounterEvent.Increment) },
        onDecrement = { viewModel.sendEvent(CounterEvent.Decrement) },
        onReset = { viewModel.sendEvent(CounterEvent.Reset) },
        onViewDetail = { viewModel.sendEvent(CounterEvent.NavigateToDetail) }
    )
}

@Composable
fun CounterContent(
    state: CounterState,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
    onViewDetail: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "MVI Counter",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Text(
                    text = "${state.count}",
                    fontSize = 72.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = onDecrement) {
                    Text("-", fontSize = 24.sp)
                }
                Button(onClick = onIncrement) {
                    Text("+", fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(onClick = onReset) {
                Text("Reset")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = onViewDetail) {
                Text("View Detail →")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CounterContentPreview() {
    MviTheme {
        CounterContent(
            state = CounterState(count = 42),
            onIncrement = {},
            onDecrement = {},
            onReset = {}
        )
    }
}
