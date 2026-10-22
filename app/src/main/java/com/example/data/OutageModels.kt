package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OutageSnapshotResponse(
    val ok: Boolean = true,
    val version: String? = null,
    val sourceMode: String? = null,
    val syncState: String? = null,
    val rows: List<DatabaseRowDto> = emptyList(),
    val napDownRows: List<NapDownRowDto> = emptyList(),
    val statusCounts: Map<String, Int>? = null,
    val napStatusCounts: Map<String, Int>? = null,
    val pendingCount: Int? = null,
    val restoredCount: Int? = null,
    val napDownSheetCount: Int? = null,
    val options: FilterOptionsDto? = null,
    val diagnostics: SnapshotDiagnosticsDto? = null,
    val napDownHeaderRow: Int? = null,
    val checkedAt: String? = null,
    val source: String? = null,
    val credential: CredentialInfoDto? = null
)

@JsonClass(generateAdapter = true)
data class DatabaseRowDto(
    val rowNumber: Int = 0,
    val timestamp: String? = "",
    val concern: String? = "",
    val province: String? = "",
    val municipality: String? = "",
    val barangay: String? = "",
    val facility: String? = "",
    val napStatus: String? = "",
    val finding: String? = "",
    val rfo: String? = "",
    val ospTeam: String? = "",
    val etr: String? = "",
    val joNumber: String? = "",
    val dateEndorsed: String? = "",
    val dateRestored: String? = "",
    val sourceStatus: String? = "",
    val finalStatus: String? = "",
    val status: String? = ""
)

@JsonClass(generateAdapter = true)
data class NapDownRowDto(
    val sourceRow: Int = 0,
    val province: String? = "",
    val municipality: String? = "",
    val barangay: String? = "",
    val napCode: String? = "",
    val coordinates: String? = "",
    val dateEndorsed: String? = "",
    val dateEndorsedDisplay: String? = "",
    val duration: String? = "",
    val finding: String? = "",
    val sourceStatus: String? = ""
)

@JsonClass(generateAdapter = true)
data class FilterOptionsDto(
    val province: List<String> = emptyList(),
    val status: List<String> = emptyList(),
    val finding: List<String> = emptyList(),
    val rfo: List<String> = emptyList(),
    val concern: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SnapshotDiagnosticsDto(
    val dbHeaderRow: Int? = 1,
    val napDownHeaderRow: Int? = 13,
    val databaseRows: Int? = 0,
    val napDownRows: Int? = 0,
    val formulaErrors: Int? = 0,
    val droppedRows: Int? = 0
)

@JsonClass(generateAdapter = true)
data class CredentialInfoDto(
    val source: String? = "",
    val clientEmail: String? = "",
    val projectId: String? = ""
)

// Cleaned & Validated Domain Models after Agent Repair
data class CleanedDatabaseRow(
    val rowNumber: Int,
    val timestamp: String,
    val concern: String,
    val province: String,
    val municipality: String,
    val barangay: String,
    val facility: String,
    val napStatus: String,
    val finding: String,
    val rfo: String,
    val ospTeam: String,
    val etr: String,
    val joNumber: String,
    val dateEndorsed: String,
    val dateEndorsedDayKey: String,
    val dateRestored: String,
    val dateRestoredDayKey: String,
    val sourceStatus: String,
    val finalStatus: String,
    val ageDays: Double?,
    val wasRepairedByAgent: Boolean = false,
    val repairNote: String = ""
)

data class CleanedNapDownRow(
    val sourceRow: Int,
    val province: String,
    val municipality: String,
    val barangay: String,
    val napCode: String,
    val coordinates: String,
    val dateEndorsed: String,
    val dateEndorsedDisplay: String,
    val duration: String,
    val durationHours: Double,
    val finding: String,
    val sourceStatus: String,
    val wasRepairedByAgent: Boolean = false
)

data class AgentRepairLog(
    val id: String,
    val category: String,
    val severity: String, // "REPAIRED", "VERIFIED", "WARNING"
    val description: String,
    val affectedCount: Int,
    val formulaUsed: String
)

data class AgentHealthSummary(
    val isLiveConnected: Boolean,
    val syncState: String,
    val sourceName: String,
    val serviceAccountEmail: String,
    val checkedAtDisplay: String,
    val totalDbRowsScanned: Int,
    val totalNapSheetRowsScanned: Int,
    val validNapDownCount: Int,
    val pendingFinalStatusCount: Int,
    val parityVerified: Boolean,
    val formulaErrorsRepaired: Int,
    val blankNapGhostRowsDropped: Int,
    val swappedDatesNormalized: Int,
    val coordinatesMisplacedFixed: Int,
    val repairLogs: List<AgentRepairLog>
)

data class ProvincePendingGroup(
    val province: String,
    val pendingCount: Int,
    val issueSummary: List<IssueCountItem>,
    val detailRows: List<CleanedDatabaseRow>
)

data class IssueCountItem(
    val label: String,
    val count: Int,
    val percentage: Double
)

data class TechnicianWorkloadItem(
    val technician: String,
    val activeCount: Int,
    val oldestDays: Double
)

data class NapDownPivotSummaryRow(
    val province: String,
    val finding: String,
    val count: Int
)
