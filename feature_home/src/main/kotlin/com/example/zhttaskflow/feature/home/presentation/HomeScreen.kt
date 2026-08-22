package com.example.zhttaskflow.feature.home.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.zhttaskflow.base.extension.collectUiStateWithLifecycle
import com.example.zhttaskflow.base.ui.StateBox
import com.example.zhttaskflow.base.ui.TaskFlowScaffold
import com.example.zhttaskflow.feature.home.R
import com.example.zhttaskflow.feature.home.domain.HomeEntranceIds

/**
 * 首页纯 UI：订阅 [HomeViewModel] 状态并分发 [HomeUiEvent]，不包含导航与副作用消费。
 */
@Composable
internal fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectUiStateWithLifecycle()

    TaskFlowScaffold(
        modifier = modifier,
        title = stringResource(id = R.string.home_str_home_title),
    ) { innerPadding ->
        StateBox(
            uiState = uiState,
            onRetry = { },
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize(),
        ) { data ->
            HomeEntranceList(
                entrances = data.entrances,
                onEntranceClick = { entranceId ->
                    viewModel.onEvent(HomeUiEvent.EntranceClicked(entranceId = entranceId))
                },
            )
        }
    }
}

@Composable
private fun HomeEntranceList(
    entrances: List<HomeEntrance>,
    onEntranceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        entrances.forEach { entrance ->
            HomeEntryCard(
                title = entrance.title,
                description = entrance.description,
                icon = entranceIcon(entrance.id),
                onClick = { onEntranceClick(entrance.id) },
            )
        }
    }
}

@Composable
private fun HomeEntryCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun entranceIcon(entranceId: String): ImageVector {
    return when (entranceId) {
        HomeEntranceIds.TASK -> Icons.AutoMirrored.Filled.List
        HomeEntranceIds.ARTICLE -> Icons.AutoMirrored.Filled.Article
        else -> Icons.AutoMirrored.Filled.List
    }
}
