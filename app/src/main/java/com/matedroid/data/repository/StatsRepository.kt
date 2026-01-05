package com.matedroid.data.repository

import com.matedroid.data.local.dao.AggregateDao
import com.matedroid.data.local.dao.ChargeSummaryDao
import com.matedroid.data.local.dao.DriveSummaryDao
import com.matedroid.data.local.entity.SchemaVersion
import com.matedroid.data.sync.SyncManager
import com.matedroid.domain.model.CarStats
import com.matedroid.domain.model.ChargePowerRecord
import com.matedroid.domain.model.ChargeTempRecord
import com.matedroid.domain.model.DeepStats
import com.matedroid.domain.model.DriveElevationRecord
import com.matedroid.domain.model.DriveTempRecord
import com.matedroid.domain.model.QuickStats
import com.matedroid.domain.model.YearFilter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for computing and retrieving stats for a car.
 */
@Singleton
class StatsRepository @Inject constructor(
    private val driveSummaryDao: DriveSummaryDao,
    private val chargeSummaryDao: ChargeSummaryDao,
    private val aggregateDao: AggregateDao,
    private val syncManager: SyncManager
) {

    /**
     * Get complete stats for a car with the given year filter.
     */
    suspend fun getStats(carId: Int, yearFilter: YearFilter): CarStats {
        val quickStats = getQuickStats(carId, yearFilter)
        val deepStats = getDeepStats(carId, yearFilter)
        val syncProgress = syncManager.getProgressForCar(carId)

        return CarStats(
            carId = carId,
            yearFilter = yearFilter,
            quickStats = quickStats,
            deepStats = deepStats,
            syncProgress = syncProgress
        )
    }

    /**
     * Get quick stats (from summary tables, instant).
     */
    suspend fun getQuickStats(carId: Int, yearFilter: YearFilter): QuickStats {
        return when (yearFilter) {
            is YearFilter.AllTime -> getQuickStatsAllTime(carId)
            is YearFilter.Year -> getQuickStatsForYear(carId, yearFilter.year)
        }
    }

    private suspend fun getQuickStatsAllTime(carId: Int): QuickStats {
        return QuickStats(
            totalDrives = driveSummaryDao.count(carId),
            totalDistanceKm = driveSummaryDao.sumDistance(carId),
            totalEnergyConsumedKwh = driveSummaryDao.sumEnergyConsumed(carId),
            avgEfficiencyWhKm = driveSummaryDao.avgEfficiency(carId),
            maxSpeedKmh = driveSummaryDao.maxSpeed(carId),
            avgDriveMinutes = driveSummaryDao.avgDuration(carId),
            totalDrivingDays = driveSummaryDao.countDrivingDays(carId),

            totalCharges = chargeSummaryDao.count(carId),
            totalEnergyAddedKwh = chargeSummaryDao.sumEnergyAdded(carId),
            totalCost = chargeSummaryDao.sumCost(carId).takeIf { it > 0 },
            avgCostPerKwh = chargeSummaryDao.avgCostPerKwh(carId).takeIf { it > 0 },
            avgChargeMinutes = chargeSummaryDao.avgDuration(carId),

            longestDrive = driveSummaryDao.longestDrive(carId),
            fastestDrive = driveSummaryDao.fastestDrive(carId),
            mostEfficientDrive = driveSummaryDao.mostEfficientDrive(carId),
            leastEfficientDrive = driveSummaryDao.leastEfficientDrive(carId),
            biggestCharge = chargeSummaryDao.biggestCharge(carId),
            mostExpensiveCharge = chargeSummaryDao.mostExpensiveCharge(carId),
            mostExpensivePerKwhCharge = chargeSummaryDao.mostExpensivePerKwhCharge(carId),

            firstDriveDate = driveSummaryDao.firstDriveDate(carId),
            firstChargeDate = chargeSummaryDao.firstChargeDate(carId),
            busiestDay = driveSummaryDao.busiestDay(carId)
        )
    }

    private suspend fun getQuickStatsForYear(carId: Int, year: Int): QuickStats {
        val startDate = "$year-01-01T00:00:00"
        val endDate = "${year + 1}-01-01T00:00:00"

        return QuickStats(
            totalDrives = driveSummaryDao.countInRange(carId, startDate, endDate),
            totalDistanceKm = driveSummaryDao.sumDistanceInRange(carId, startDate, endDate),
            totalEnergyConsumedKwh = driveSummaryDao.sumEnergyConsumedInRange(carId, startDate, endDate),
            avgEfficiencyWhKm = driveSummaryDao.avgEfficiencyInRange(carId, startDate, endDate),
            maxSpeedKmh = driveSummaryDao.maxSpeedInRange(carId, startDate, endDate),
            avgDriveMinutes = driveSummaryDao.avgDuration(carId), // No range version
            totalDrivingDays = driveSummaryDao.countDrivingDays(carId), // No range version

            totalCharges = chargeSummaryDao.countInRange(carId, startDate, endDate),
            totalEnergyAddedKwh = chargeSummaryDao.sumEnergyAddedInRange(carId, startDate, endDate),
            totalCost = chargeSummaryDao.sumCostInRange(carId, startDate, endDate).takeIf { it > 0 },
            avgCostPerKwh = chargeSummaryDao.avgCostPerKwh(carId), // No range version
            avgChargeMinutes = chargeSummaryDao.avgDuration(carId), // No range version

            longestDrive = driveSummaryDao.longestDriveInRange(carId, startDate, endDate),
            fastestDrive = driveSummaryDao.fastestDrive(carId), // No range version
            mostEfficientDrive = driveSummaryDao.mostEfficientDrive(carId), // No range version
            leastEfficientDrive = driveSummaryDao.leastEfficientDrive(carId), // No range version
            biggestCharge = chargeSummaryDao.biggestChargeInRange(carId, startDate, endDate),
            mostExpensiveCharge = chargeSummaryDao.mostExpensiveChargeInRange(carId, startDate, endDate),
            mostExpensivePerKwhCharge = chargeSummaryDao.mostExpensivePerKwhChargeInRange(carId, startDate, endDate),

            firstDriveDate = driveSummaryDao.firstDriveDate(carId), // Always show first ever
            firstChargeDate = chargeSummaryDao.firstChargeDate(carId), // Always show first ever
            busiestDay = driveSummaryDao.busiestDayInRange(carId, startDate, endDate)
        )
    }

    /**
     * Get deep stats (from aggregate tables, requires sync).
     * Returns null if no aggregates exist yet.
     */
    suspend fun getDeepStats(carId: Int, yearFilter: YearFilter): DeepStats? {
        val driveAggregates = aggregateDao.countDriveAggregates(carId)
        val chargeAggregates = aggregateDao.countChargeAggregates(carId)

        // Return null if no aggregates exist at all
        if (driveAggregates == 0 && chargeAggregates == 0) {
            return null
        }

        return when (yearFilter) {
            is YearFilter.AllTime -> getDeepStatsAllTime(carId, driveAggregates, chargeAggregates)
            is YearFilter.Year -> getDeepStatsForYear(carId, yearFilter.year, driveAggregates, chargeAggregates)
        }
    }

    private suspend fun getDeepStatsAllTime(carId: Int, driveCount: Int, chargeCount: Int): DeepStats {
        // Elevation records
        val driveWithMaxElev = aggregateDao.driveWithMaxElevation(carId)
        val driveWithMinElev = aggregateDao.driveWithMinElevation(carId)
        val driveWithMostGain = aggregateDao.driveWithMostElevationGain(carId)

        // Temperature records (driving)
        val hottestDriveAgg = aggregateDao.hottestDrive(carId)
        val coldestDriveAgg = aggregateDao.coldestDrive(carId)

        // Temperature records (charging)
        val hottestChargeAgg = aggregateDao.hottestCharge(carId)
        val coldestChargeAgg = aggregateDao.coldestCharge(carId)

        // Power record
        val chargeWithMaxPowerAgg = aggregateDao.chargeWithMaxPower(carId)

        return DeepStats(
            maxElevationM = aggregateDao.maxElevation(carId),
            minElevationM = aggregateDao.minElevation(carId),
            driveWithMaxElevation = driveWithMaxElev?.let { agg ->
                val drive = driveSummaryDao.get(agg.driveId)
                DriveElevationRecord(
                    driveId = agg.driveId,
                    elevationM = agg.maxElevation ?: 0,
                    elevationGainM = agg.elevationGain,
                    date = drive?.startDate
                )
            },
            driveWithMinElevation = driveWithMinElev?.let { agg ->
                val drive = driveSummaryDao.get(agg.driveId)
                DriveElevationRecord(
                    driveId = agg.driveId,
                    elevationM = agg.minElevation ?: 0,
                    elevationGainM = agg.elevationGain,
                    date = drive?.startDate
                )
            },
            driveWithMostClimbing = driveWithMostGain?.let { agg ->
                val drive = driveSummaryDao.get(agg.driveId)
                // Net elevation gain = end elevation - start elevation
                val netElevationGain = if (agg.startElevation != null && agg.endElevation != null) {
                    agg.endElevation - agg.startElevation
                } else null
                DriveElevationRecord(
                    driveId = agg.driveId,
                    elevationM = agg.maxElevation ?: 0,
                    elevationGainM = netElevationGain,
                    date = drive?.startDate
                )
            },

            maxOutsideTempDrivingC = aggregateDao.maxOutsideTempDriving(carId),
            minOutsideTempDrivingC = aggregateDao.minOutsideTempDriving(carId),
            maxCabinTempC = aggregateDao.maxInsideTemp(carId),
            minCabinTempC = aggregateDao.minInsideTemp(carId),
            hottestDrive = hottestDriveAgg?.let { agg ->
                val drive = driveSummaryDao.get(agg.driveId)
                DriveTempRecord(
                    driveId = agg.driveId,
                    tempC = agg.maxOutsideTemp ?: 0.0,
                    date = drive?.startDate
                )
            },
            coldestDrive = coldestDriveAgg?.let { agg ->
                val drive = driveSummaryDao.get(agg.driveId)
                DriveTempRecord(
                    driveId = agg.driveId,
                    tempC = agg.minOutsideTemp ?: 0.0,
                    date = drive?.startDate
                )
            },

            maxOutsideTempChargingC = aggregateDao.maxOutsideTempCharging(carId),
            minOutsideTempChargingC = aggregateDao.minOutsideTempCharging(carId),
            hottestCharge = hottestChargeAgg?.let { agg ->
                val charge = chargeSummaryDao.get(agg.chargeId)
                ChargeTempRecord(
                    chargeId = agg.chargeId,
                    tempC = agg.maxOutsideTemp ?: 0.0,
                    date = charge?.startDate
                )
            },
            coldestCharge = coldestChargeAgg?.let { agg ->
                val charge = chargeSummaryDao.get(agg.chargeId)
                ChargeTempRecord(
                    chargeId = agg.chargeId,
                    tempC = agg.minOutsideTemp ?: 0.0,
                    date = charge?.startDate
                )
            },

            maxChargerPowerKw = aggregateDao.maxChargerPower(carId),
            chargeWithMaxPower = chargeWithMaxPowerAgg?.let { agg ->
                val charge = chargeSummaryDao.get(agg.chargeId)
                ChargePowerRecord(
                    chargeId = agg.chargeId,
                    powerKw = agg.maxChargerPower ?: 0,
                    date = charge?.startDate
                )
            },

            acChargeCount = aggregateDao.countAcCharges(carId),
            dcChargeCount = aggregateDao.countDcCharges(carId),

            driveDetailsProcessed = driveCount,
            chargeDetailsProcessed = chargeCount
        )
    }

    private suspend fun getDeepStatsForYear(
        carId: Int,
        year: Int,
        driveCount: Int,
        chargeCount: Int
    ): DeepStats {
        val startDate = "$year-01-01T00:00:00"
        val endDate = "${year + 1}-01-01T00:00:00"

        // For year-filtered deep stats, we use range queries where available
        // and fall back to all-time for records

        return DeepStats(
            maxElevationM = aggregateDao.maxElevationInRange(carId, startDate, endDate),
            minElevationM = aggregateDao.minElevation(carId), // No range version
            driveWithMaxElevation = aggregateDao.driveWithMaxElevation(carId)?.let { agg ->
                val drive = driveSummaryDao.get(agg.driveId)
                DriveElevationRecord(
                    driveId = agg.driveId,
                    elevationM = agg.maxElevation ?: 0,
                    elevationGainM = agg.elevationGain,
                    date = drive?.startDate
                )
            },
            driveWithMinElevation = aggregateDao.driveWithMinElevation(carId)?.let { agg ->
                val drive = driveSummaryDao.get(agg.driveId)
                DriveElevationRecord(
                    driveId = agg.driveId,
                    elevationM = agg.minElevation ?: 0,
                    elevationGainM = agg.elevationGain,
                    date = drive?.startDate
                )
            },
            driveWithMostClimbing = aggregateDao.driveWithMostElevationGain(carId)?.let { agg ->
                val drive = driveSummaryDao.get(agg.driveId)
                val netElevationGain = if (agg.startElevation != null && agg.endElevation != null) {
                    agg.endElevation - agg.startElevation
                } else null
                DriveElevationRecord(
                    driveId = agg.driveId,
                    elevationM = agg.maxElevation ?: 0,
                    elevationGainM = netElevationGain,
                    date = drive?.startDate
                )
            },

            maxOutsideTempDrivingC = aggregateDao.maxOutsideTempDrivingInRange(carId, startDate, endDate),
            minOutsideTempDrivingC = aggregateDao.minOutsideTempDriving(carId), // No range version
            maxCabinTempC = aggregateDao.maxInsideTemp(carId), // No range version
            minCabinTempC = aggregateDao.minInsideTemp(carId), // No range version
            hottestDrive = aggregateDao.hottestDrive(carId)?.let { agg ->
                val drive = driveSummaryDao.get(agg.driveId)
                DriveTempRecord(
                    driveId = agg.driveId,
                    tempC = agg.maxOutsideTemp ?: 0.0,
                    date = drive?.startDate
                )
            },
            coldestDrive = aggregateDao.coldestDrive(carId)?.let { agg ->
                val drive = driveSummaryDao.get(agg.driveId)
                DriveTempRecord(
                    driveId = agg.driveId,
                    tempC = agg.minOutsideTemp ?: 0.0,
                    date = drive?.startDate
                )
            },

            maxOutsideTempChargingC = aggregateDao.maxOutsideTempChargingInRange(carId, startDate, endDate),
            minOutsideTempChargingC = aggregateDao.minOutsideTempCharging(carId), // No range version
            hottestCharge = aggregateDao.hottestCharge(carId)?.let { agg ->
                val charge = chargeSummaryDao.get(agg.chargeId)
                ChargeTempRecord(
                    chargeId = agg.chargeId,
                    tempC = agg.maxOutsideTemp ?: 0.0,
                    date = charge?.startDate
                )
            },
            coldestCharge = aggregateDao.coldestCharge(carId)?.let { agg ->
                val charge = chargeSummaryDao.get(agg.chargeId)
                ChargeTempRecord(
                    chargeId = agg.chargeId,
                    tempC = agg.minOutsideTemp ?: 0.0,
                    date = charge?.startDate
                )
            },

            maxChargerPowerKw = aggregateDao.maxChargerPowerInRange(carId, startDate, endDate),
            chargeWithMaxPower = aggregateDao.chargeWithMaxPower(carId)?.let { agg ->
                val charge = chargeSummaryDao.get(agg.chargeId)
                ChargePowerRecord(
                    chargeId = agg.chargeId,
                    powerKw = agg.maxChargerPower ?: 0,
                    date = charge?.startDate
                )
            },

            acChargeCount = aggregateDao.countAcChargesInRange(carId, startDate, endDate),
            dcChargeCount = aggregateDao.countDcChargesInRange(carId, startDate, endDate),

            driveDetailsProcessed = driveCount,
            chargeDetailsProcessed = chargeCount
        )
    }

    /**
     * Get available years for the year filter dropdown.
     */
    suspend fun getAvailableYears(carId: Int): List<Int> {
        val driveYears = driveSummaryDao.getYears(carId)
        val chargeYears = chargeSummaryDao.getYears(carId)
        return (driveYears + chargeYears).distinct().sortedDescending()
    }

    /**
     * Check if any data is available for stats.
     */
    suspend fun hasData(carId: Int): Boolean {
        return driveSummaryDao.count(carId) > 0 || chargeSummaryDao.count(carId) > 0
    }

    /**
     * Check if deep stats are being processed.
     */
    suspend fun isDeepSyncInProgress(carId: Int): Boolean {
        val progress = syncManager.getProgressForCar(carId)
        return progress != null && progress.phase.isProcessing()
    }

    /**
     * Get the sync completion percentage for deep stats.
     */
    suspend fun getDeepSyncProgress(carId: Int): Float {
        val totalDrives = driveSummaryDao.count(carId)
        val totalCharges = chargeSummaryDao.count(carId)
        val processedDrives = aggregateDao.countDriveAggregates(carId)
        val processedCharges = aggregateDao.countChargeAggregates(carId)

        val total = totalDrives + totalCharges
        val processed = processedDrives + processedCharges

        return if (total > 0) processed.toFloat() / total else 0f
    }
}

private fun com.matedroid.domain.model.SyncPhase.isProcessing(): Boolean {
    return this == com.matedroid.domain.model.SyncPhase.SYNCING_SUMMARIES ||
            this == com.matedroid.domain.model.SyncPhase.SYNCING_DRIVE_DETAILS ||
            this == com.matedroid.domain.model.SyncPhase.SYNCING_CHARGE_DETAILS
}
