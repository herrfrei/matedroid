package com.matedroid.ui.screens.charges

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.matedroid.R
import com.matedroid.data.api.models.ChargeDetail
import com.matedroid.data.api.models.ChargePoint
import com.matedroid.data.api.models.Units
import com.matedroid.domain.model.UnitFormatter
import com.matedroid.ui.components.FullscreenDualAxisLineChart
import com.matedroid.ui.components.FullscreenLineChart
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentChargeScreen(
    carId: Int,
    exteriorColor: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: CurrentChargeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(carId) {
        viewModel.loadCurrentCharge(carId)
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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.current_charge_title))
                        if (uiState.chargeDetail != null && !uiState.isNotCharging) {
                            Spacer(modifier = Modifier.width(8.dp))
                            LiveBadge()
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
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
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.isUnsupportedApi -> {
                FallbackMessage(
                    message = stringResource(R.string.current_charge_unsupported),
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.isNotCharging -> {
                FallbackMessage(
                    message = stringResource(R.string.current_charge_not_charging),
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.chargeDetail != null -> {
                CurrentChargeContent(
                    detail = uiState.chargeDetail!!,
                    stats = uiState.stats,
                    units = uiState.units,
                    isDcCharge = uiState.isDcCharge,
                    timeToFullCharge = uiState.timeToFullCharge,
                    chronologicalPoints = uiState.chronologicalPoints,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun LiveBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val badgeColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFE53935),
        targetValue = Color(0xFFFF5252),
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveBadgeColor"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(badgeColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.current_charge_live),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun FallbackMessage(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
private fun CurrentChargeContent(
    detail: ChargeDetail,
    stats: ChargeDetailStats?,
    units: Units?,
    isDcCharge: Boolean,
    timeToFullCharge: Double?,
    chronologicalPoints: List<ChargePoint>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header card
        CurrentChargeHeaderCard(
            detail = detail,
            isDcCharge = isDcCharge,
            timeToFullCharge = timeToFullCharge
        )

        // Stats section
        stats?.let { s ->
            // Energy section
            val energyLabel = stringResource(R.string.energy)
            val addedLabel = stringResource(R.string.energy_added)
            val usedLabel = stringResource(R.string.used)
            val efficiencyLabel = stringResource(R.string.efficiency)

            LiveStatsSectionCard(
                title = energyLabel,
                icon = Icons.Default.Bolt,
                stats = listOf(
                    StatItem(addedLabel, "%.2f kWh".format(s.energyAdded)),
                    StatItem(usedLabel, "%.2f kWh".format(s.energyUsed)),
                    StatItem(efficiencyLabel, "%.1f%%".format(s.efficiency))
                )
            )

            // Power section
            if (s.powerMax > 0) {
                val powerLabel = stringResource(R.string.power)
                val maximumLabel = stringResource(R.string.maximum)
                val minimumLabel = stringResource(R.string.minimum)
                val averageLabel = stringResource(R.string.average)

                LiveStatsSectionCard(
                    title = powerLabel,
                    icon = Icons.Default.Bolt,
                    stats = listOf(
                        StatItem(maximumLabel, "${s.powerMax} kW"),
                        StatItem(minimumLabel, "${s.powerMin} kW"),
                        StatItem(averageLabel, "%.1f kW".format(s.powerAvg))
                    )
                )
            }

            // Voltage & Current section (AC only)
            if (!isDcCharge) {
                val chargerLabel = stringResource(R.string.charger)
                val voltageMaxLabel = stringResource(R.string.voltage_max)
                val voltageMinLabel = stringResource(R.string.voltage_min)
                val voltageAvgLabel = stringResource(R.string.voltage_avg)
                val currentMaxLabel = stringResource(R.string.current_max)
                val currentMinLabel = stringResource(R.string.current_min)
                val currentAvgLabel = stringResource(R.string.current_avg)

                LiveStatsSectionCard(
                    title = chargerLabel,
                    icon = Icons.Default.ElectricalServices,
                    stats = listOf(
                        StatItem(voltageMaxLabel, "${s.voltageMax} V"),
                        StatItem(voltageMinLabel, "${s.voltageMin} V"),
                        StatItem(voltageAvgLabel, "%.0f V".format(s.voltageAvg)),
                        StatItem(currentMaxLabel, "${s.currentMax} A"),
                        StatItem(currentMinLabel, "${s.currentMin} A"),
                        StatItem(currentAvgLabel, "%.1f A".format(s.currentAvg))
                    )
                )
            }

            // Temperature section
            if (s.tempMax > -100) {
                val temperatureLabel = stringResource(R.string.temperature)
                val maximumLabel = stringResource(R.string.maximum)
                val minimumLabel = stringResource(R.string.minimum)
                val averageLabel = stringResource(R.string.average)

                LiveStatsSectionCard(
                    title = temperatureLabel,
                    icon = Icons.Default.DeviceThermostat,
                    stats = listOf(
                        StatItem(maximumLabel, UnitFormatter.formatTemperature(s.tempMax, units)),
                        StatItem(minimumLabel, UnitFormatter.formatTemperature(s.tempMin, units)),
                        StatItem(averageLabel, UnitFormatter.formatTemperature(s.tempAvg, units))
                    )
                )
            }
        }

        // Charts
        if (chronologicalPoints.size > 2) {
            val timeLabels = extractChronoTimeLabels(chronologicalPoints)

            // Power chart (always shown)
            if (chronologicalPoints.any { (it.chargerPower ?: 0) > 0 }) {
                val powers = chronologicalPoints.mapNotNull { it.chargerPower?.toFloat() }
                if (powers.size >= 2) {
                    val powerProfileTitle = stringResource(R.string.power_profile)
                    LiveChartCard(
                        title = powerProfileTitle,
                        icon = Icons.Default.Bolt
                    ) {
                        FullscreenLineChart(
                            data = powers,
                            color = Color(0xFF4CAF50),
                            unit = "kW",
                            timeLabels = timeLabels,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Voltage & Current combined chart (AC only)
            if (!isDcCharge) {
                val voltages = chronologicalPoints.mapNotNull { it.chargerVoltage?.toFloat() }
                val currents = chronologicalPoints.mapNotNull { it.chargerCurrent?.toFloat() }

                if (voltages.size >= 2 && currents.size >= 2) {
                    val vcTitle = stringResource(R.string.voltage_and_current_profile)
                    LiveChartCard(
                        title = vcTitle,
                        icon = Icons.Default.ElectricalServices
                    ) {
                        FullscreenDualAxisLineChart(
                            dataLeft = voltages,
                            dataRight = currents,
                            colorLeft = MaterialTheme.colorScheme.tertiary,
                            colorRight = MaterialTheme.colorScheme.secondary,
                            unitLeft = "V",
                            unitRight = "A",
                            timeLabels = timeLabels,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Battery level chart
            val batteryLevels = chronologicalPoints.mapNotNull { it.batteryLevel?.toFloat() }
            if (batteryLevels.size >= 2) {
                val batteryLevelTitle = stringResource(R.string.battery_level)
                LiveChartCard(
                    title = batteryLevelTitle,
                    icon = Icons.Default.BatteryChargingFull
                ) {
                    FullscreenLineChart(
                        data = batteryLevels,
                        color = MaterialTheme.colorScheme.primary,
                        unit = "%",
                        fixedMinMax = Pair(0f, 100f),
                        timeLabels = timeLabels,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Temperature chart
            val temps = chronologicalPoints.mapNotNull { it.outsideTemp?.toFloat() }
            if (temps.size >= 2) {
                val temperatureLabel = stringResource(R.string.temperature)
                LiveChartCard(
                    title = temperatureLabel,
                    icon = Icons.Default.DeviceThermostat
                ) {
                    FullscreenLineChart(
                        data = temps,
                        color = Color(0xFFFF9800),
                        unit = UnitFormatter.getTemperatureUnit(units),
                        timeLabels = timeLabels,
                        convertValue = { value ->
                            if (units?.unitOfTemperature == "F") (value * 9f / 5f + 32f) else value
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CurrentChargeHeaderCard(
    detail: ChargeDetail,
    isDcCharge: Boolean,
    timeToFullCharge: Double?
) {
    val startLabel = stringResource(R.string.started)
    val unknownLabel = stringResource(R.string.unknown)
    val estimatedEndLabel = stringResource(R.string.current_charge_estimated_end)
    val energyAddedLabel = stringResource(R.string.energy_added_header)
    val locationLabel = stringResource(R.string.location)
    val unknownLocationLabel = stringResource(R.string.unknown_location)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Location
            if (detail.address != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = locationLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = detail.address,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(start = 36.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
            }

            // Start time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = startLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatLiveDateTime(detail.startDate, unknownLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Estimated end time
            timeToFullCharge?.let { hours ->
                if (hours > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = estimatedEndLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = formatEstimatedEnd(hours),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Energy added + battery progress + AC/DC badge
            HorizontalDivider(
                modifier = Modifier.padding(start = 36.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Energy added with AC/DC badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Power,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = energyAddedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "%.2f kWh".format(detail.chargeEnergyAdded ?: 0.0),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            LiveChargeTypeBadge(isDcCharge = isDcCharge)
                        }
                    }
                }

                // Battery progress (start% -> current%)
                val startLevel = detail.startBatteryLevel ?: 0
                val currentLevel = detail.currentOrEndBatteryLevel ?: 0
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$startLevel% \u2192 $currentLevel%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "+${currentLevel - startLevel}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveChargeTypeBadge(isDcCharge: Boolean) {
    val backgroundColor = if (isDcCharge) Color(0xFFFF9800) else Color(0xFF4CAF50)
    val text = if (isDcCharge) stringResource(R.string.charging_dc) else stringResource(R.string.charging_ac)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun LiveStatsSectionCard(
    title: String,
    icon: ImageVector,
    stats: List<StatItem>
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    val columnCount = when {
        screenWidth > 600 -> 4
        screenWidth > 340 -> 3
        else -> 2
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            val chunked = stats.chunked(columnCount)
            chunked.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { stat ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stat.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = stat.value,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    val emptySlots = columnCount - row.size
                    if (emptySlots > 0) {
                        repeat(emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (index < chunked.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun LiveChartCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            content()
        }
    }
}

private fun extractChronoTimeLabels(chargePoints: List<ChargePoint>): List<String> {
    if (chargePoints.isEmpty()) return listOf("", "", "", "", "")

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val times = chargePoints.mapNotNull { point ->
        point.date?.let { dateStr ->
            try {
                val dateTime = try {
                    OffsetDateTime.parse(dateStr).toLocalDateTime()
                } catch (e: DateTimeParseException) {
                    LocalDateTime.parse(dateStr.replace("Z", ""))
                }
                dateTime
            } catch (e: Exception) {
                null
            }
        }
    }

    if (times.isEmpty()) return listOf("", "", "", "", "")

    val indices = listOf(0, times.size / 4, times.size / 2, times.size * 3 / 4, times.size - 1)
    return indices.map { idx ->
        times.getOrNull(idx.coerceIn(0, times.size - 1))?.format(timeFormatter) ?: ""
    }
}

private fun formatLiveDateTime(dateStr: String?, unknownLabel: String = "Unknown"): String {
    if (dateStr == null) return unknownLabel
    return try {
        val dateTime = try {
            OffsetDateTime.parse(dateStr).toLocalDateTime()
        } catch (e: DateTimeParseException) {
            LocalDateTime.parse(dateStr.replace("Z", ""))
        }
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
        dateTime.format(formatter)
    } catch (e: Exception) {
        dateStr
    }
}

private fun formatEstimatedEnd(hoursRemaining: Double): String {
    val totalMinutes = (hoursRemaining * 60).roundToInt()
    val now = java.time.LocalDateTime.now()
    val endTime = now.plusMinutes(totalMinutes.toLong())
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    val durationStr = if (h > 0) "${h}h ${m}m" else "${m}m"
    return "${endTime.format(timeFormatter)} ($durationStr)"
}
