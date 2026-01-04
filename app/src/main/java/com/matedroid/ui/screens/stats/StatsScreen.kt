package com.matedroid.ui.screens.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.matedroid.domain.model.CarStats
import com.matedroid.domain.model.DeepStats
import com.matedroid.domain.model.QuickStats
import com.matedroid.domain.model.SyncPhase
import com.matedroid.domain.model.YearFilter
import com.matedroid.ui.icons.CustomIcons
import com.matedroid.ui.theme.CarColorPalette
import com.matedroid.ui.theme.CarColorPalettes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    carId: Int,
    exteriorColor: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToDriveDetail: (Int) -> Unit = {},
    onNavigateToChargeDetail: (Int) -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isDarkTheme = isSystemInDarkTheme()
    val palette = CarColorPalettes.forExteriorColor(exteriorColor, isDarkTheme)

    LaunchedEffect(carId) {
        viewModel.setCarId(carId)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats for Nerds") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.carStats == null) {
                EmptyState(
                    message = "No stats available yet.\nSync is in progress...",
                    syncProgress = uiState.deepSyncProgress
                )
            } else {
                StatsContent(
                    stats = uiState.carStats!!,
                    availableYears = uiState.availableYears,
                    selectedYearFilter = uiState.selectedYearFilter,
                    deepSyncProgress = uiState.deepSyncProgress,
                    palette = palette,
                    onYearFilterSelected = { viewModel.setYearFilter(it) },
                    onNavigateToDriveDetail = onNavigateToDriveDetail,
                    onNavigateToChargeDetail = onNavigateToChargeDetail
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    message: String,
    syncProgress: Float
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (syncProgress > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { syncProgress },
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
                Text(
                    text = "${(syncProgress * 100).toInt()}% synced",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsContent(
    stats: CarStats,
    availableYears: List<Int>,
    selectedYearFilter: YearFilter,
    deepSyncProgress: Float,
    palette: CarColorPalette,
    onYearFilterSelected: (YearFilter) -> Unit,
    onNavigateToDriveDetail: (Int) -> Unit,
    onNavigateToChargeDetail: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Year filter chips
        item {
            YearFilterChips(
                availableYears = availableYears,
                selectedFilter = selectedYearFilter,
                palette = palette,
                onFilterSelected = onYearFilterSelected
            )
        }

        // Sync progress indicator if deep sync is ongoing
        if (deepSyncProgress < 1f && deepSyncProgress > 0f) {
            item {
                SyncProgressCard(progress = deepSyncProgress, palette = palette)
            }
        }

        // Records (at the top)
        item {
            RecordsCard(
                quickStats = stats.quickStats,
                deepStats = stats.deepStats,
                palette = palette,
                onDriveClick = onNavigateToDriveDetail,
                onChargeClick = onNavigateToChargeDetail
            )
        }

        // Quick Stats - Drives Overview
        item {
            QuickStatsDrivesCard(quickStats = stats.quickStats, palette = palette)
        }

        // Quick Stats - Charges Overview
        item {
            QuickStatsChargesCard(quickStats = stats.quickStats, palette = palette)
        }

        // Deep Stats - only if available
        stats.deepStats?.let { deepStats ->
            // Elevation Stats
            item {
                ElevationStatsCard(deepStats = deepStats, palette = palette)
            }

            // Temperature Stats
            item {
                TemperatureStatsCard(deepStats = deepStats, palette = palette)
            }

            // Charging Power Stats
            item {
                ChargingPowerCard(deepStats = deepStats, palette = palette)
            }

            // AC/DC Ratio
            item {
                AcDcRatioCard(deepStats = deepStats, palette = palette)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearFilterChips(
    availableYears: List<Int>,
    selectedFilter: YearFilter,
    palette: CarColorPalette,
    onFilterSelected: (YearFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All Time option
        item {
            FilterChip(
                selected = selectedFilter is YearFilter.AllTime,
                onClick = { onFilterSelected(YearFilter.AllTime) },
                label = { Text("All Time") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = palette.surface,
                    selectedLabelColor = palette.onSurface
                )
            )
        }

        // Year options
        items(availableYears) { year ->
            FilterChip(
                selected = selectedFilter is YearFilter.Year && selectedFilter.year == year,
                onClick = { onFilterSelected(YearFilter.Year(year)) },
                label = { Text(year.toString()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = palette.surface,
                    selectedLabelColor = palette.onSurface
                )
            )
        }
    }
}

@Composable
private fun SyncProgressCard(progress: Float, palette: CarColorPalette) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Deep Stats Sync in Progress",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${(progress * 100).toInt()}% complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ======== Quick Stats Cards ========

@Composable
private fun QuickStatsDrivesCard(quickStats: QuickStats, palette: CarColorPalette) {
    StatsCard(
        title = "Drives Overview",
        icon = Icons.Default.DirectionsCar,
        palette = palette
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(
                label = "Total Drives",
                value = quickStats.totalDrives.toString(),
                modifier = Modifier.weight(1f)
            )
            StatItem(
                label = "Driving Days",
                value = quickStats.totalDrivingDays.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(
                label = "Total Distance",
                value = "%.0f km".format(quickStats.totalDistanceKm),
                modifier = Modifier.weight(1f)
            )
            StatItem(
                label = "Energy Used",
                value = "%.0f kWh".format(quickStats.totalEnergyConsumedKwh),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(
                label = "Avg Efficiency",
                value = "%.0f Wh/km".format(quickStats.avgEfficiencyWhKm),
                modifier = Modifier.weight(1f)
            )
            StatItem(
                label = "Top Speed",
                value = quickStats.maxSpeedKmh?.let { "$it km/h" } ?: "N/A",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickStatsChargesCard(quickStats: QuickStats, palette: CarColorPalette) {
    StatsCard(
        title = "Charges Overview",
        icon = Icons.Default.BatteryChargingFull,
        palette = palette
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(
                label = "Total Charges",
                value = quickStats.totalCharges.toString(),
                modifier = Modifier.weight(1f)
            )
            StatItem(
                label = "Energy Added",
                value = "%.0f kWh".format(quickStats.totalEnergyAddedKwh),
                modifier = Modifier.weight(1f)
            )
        }
        if (quickStats.totalCost != null && quickStats.totalCost > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(
                    label = "Total Cost",
                    value = "%.2f €".format(quickStats.totalCost),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Avg Cost/kWh",
                    value = quickStats.avgCostPerKwh?.let { "%.3f €".format(it) } ?: "N/A",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RecordsCard(
    quickStats: QuickStats,
    deepStats: DeepStats?,
    palette: CarColorPalette,
    onDriveClick: (Int) -> Unit,
    onChargeClick: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = CustomIcons.Trophy,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = palette.accent
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.onSurface
            )
        }

        quickStats.longestDrive?.let { drive ->
            RecordCard(
                emoji = "📏",
                label = "Longest Drive",
                value = "%.1f km".format(drive.distance),
                subtext = drive.startDate.take(10),
                palette = palette,
                onClick = { onDriveClick(drive.driveId) }
            )
        }
        quickStats.fastestDrive?.let { drive ->
            RecordCard(
                emoji = "🏎️",
                label = "Top Speed",
                value = "${drive.speedMax} km/h",
                subtext = drive.startDate.take(10),
                palette = palette,
                onClick = { onDriveClick(drive.driveId) }
            )
        }
        quickStats.mostEfficientDrive?.let { drive ->
            RecordCard(
                emoji = "🌱",
                label = "Most Efficient",
                value = "%.0f Wh/km".format(drive.efficiency ?: 0.0),
                subtext = drive.startDate.take(10),
                palette = palette,
                onClick = { onDriveClick(drive.driveId) }
            )
        }
        deepStats?.driveWithMostClimbing?.let { record ->
            RecordCard(
                emoji = "⛰️",
                label = "Most Climbing",
                value = record.elevationGainM?.let { "+$it m" } ?: "N/A",
                subtext = record.date?.take(10) ?: "",
                palette = palette,
                onClick = { onDriveClick(record.driveId) }
            )
        }
        quickStats.biggestCharge?.let { charge ->
            RecordCard(
                emoji = "⚡",
                label = "Biggest Charge",
                value = "%.0f kWh".format(charge.energyAdded),
                subtext = charge.startDate.take(10),
                palette = palette,
                onClick = { onChargeClick(charge.chargeId) }
            )
        }
        quickStats.mostExpensiveCharge?.let { charge ->
            charge.cost?.let { cost ->
                RecordCard(
                    emoji = "💸",
                    label = "Most Expensive",
                    value = "%.2f €".format(cost),
                    subtext = charge.startDate.take(10),
                    palette = palette,
                    onClick = { onChargeClick(charge.chargeId) }
                )
            }
        }
        quickStats.mostExpensivePerKwhCharge?.let { charge ->
            charge.cost?.let { cost ->
                if (charge.energyAdded > 0) {
                    RecordCard(
                        emoji = "📈",
                        label = "Priciest per kWh",
                        value = "%.3f €/kWh".format(cost / charge.energyAdded),
                        subtext = charge.startDate.take(10),
                        palette = palette,
                        onClick = { onChargeClick(charge.chargeId) }
                    )
                }
            }
        }
        quickStats.busiestDay?.let { day ->
            RecordCard(
                emoji = "📅",
                label = "Busiest Day",
                value = "${day.count} drives",
                subtext = day.day,
                palette = palette,
                onClick = null // Not navigable
            )
        }
    }
}

// ======== Deep Stats Cards ========

@Composable
private fun ElevationStatsCard(deepStats: DeepStats, palette: CarColorPalette) {
    if (deepStats.maxElevationM == null && deepStats.minElevationM == null) {
        return // No elevation data
    }

    StatsCard(
        title = "Elevation",
        icon = Icons.Default.Terrain,
        palette = palette
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(
                label = "Highest Point",
                value = deepStats.maxElevationM?.let { "$it m" } ?: "N/A",
                modifier = Modifier.weight(1f)
            )
            StatItem(
                label = "Lowest Point",
                value = deepStats.minElevationM?.let { "$it m" } ?: "N/A",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TemperatureStatsCard(deepStats: DeepStats, palette: CarColorPalette) {
    if (deepStats.maxOutsideTempDrivingC == null && deepStats.minOutsideTempDrivingC == null) {
        return // No temperature data
    }

    StatsCard(
        title = "Temperature Extremes",
        icon = Icons.Default.Thermostat,
        palette = palette
    ) {
        Text(
            text = "While Driving",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = palette.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(
                label = "Hottest",
                value = deepStats.maxOutsideTempDrivingC?.let { "%.1f°C".format(it) } ?: "N/A",
                modifier = Modifier.weight(1f)
            )
            StatItem(
                label = "Coldest",
                value = deepStats.minOutsideTempDrivingC?.let { "%.1f°C".format(it) } ?: "N/A",
                modifier = Modifier.weight(1f)
            )
        }

        if (deepStats.maxCabinTempC != null || deepStats.minCabinTempC != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cabin Temperature",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = palette.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(
                    label = "Hottest",
                    value = deepStats.maxCabinTempC?.let { "%.1f°C".format(it) } ?: "N/A",
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Coldest",
                    value = deepStats.minCabinTempC?.let { "%.1f°C".format(it) } ?: "N/A",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ChargingPowerCard(deepStats: DeepStats, palette: CarColorPalette) {
    if (deepStats.maxChargerPowerKw == null) {
        return // No charging power data
    }

    StatsCard(
        title = "Charging Power",
        icon = Icons.Default.ElectricBolt,
        palette = palette
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(
                label = "Max Power Achieved",
                value = "${deepStats.maxChargerPowerKw} kW",
                modifier = Modifier.weight(1f)
            )
        }
        deepStats.chargeWithMaxPower?.let { record ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "on ${record.date?.take(10) ?: "unknown date"}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AcDcRatioCard(deepStats: DeepStats, palette: CarColorPalette) {
    val total = deepStats.acChargeCount + deepStats.dcChargeCount
    if (total == 0) {
        return // No charge data
    }

    val acPercent = (deepStats.acChargeCount * 100f / total).toInt()
    val dcPercent = (deepStats.dcChargeCount * 100f / total).toInt()

    StatsCard(
        title = "AC/DC Charging Ratio",
        icon = Icons.Default.BatteryChargingFull,
        palette = palette
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(
                label = "AC Charges",
                value = "${deepStats.acChargeCount} ($acPercent%)",
                modifier = Modifier.weight(1f)
            )
            StatItem(
                label = "DC Charges",
                value = "${deepStats.dcChargeCount} ($dcPercent%)",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Progress bar showing ratio
        LinearProgressIndicator(
            progress = { acPercent / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.tertiary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "AC (Home/Destination)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "DC (Supercharger)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

// ======== Reusable Components ========

@Composable
private fun StatsCard(
    title: String,
    icon: ImageVector,
    palette: CarColorPalette,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = palette.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = palette.accent
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecordCard(
    emoji: String,
    label: String,
    value: String,
    subtext: String,
    palette: CarColorPalette,
    onClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = palette.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.onSurface
                )
                if (subtext.isNotEmpty()) {
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.onSurfaceVariant
                    )
                }
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "View details",
                    modifier = Modifier.size(24.dp),
                    tint = palette.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecordItem(
    emoji: String,
    label: String,
    value: String,
    subtext: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (subtext.isNotEmpty()) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
