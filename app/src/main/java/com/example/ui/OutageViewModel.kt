package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.agent.DataIntegrityAgent
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class DashboardSection(val title: String, val badgeText: String) {
    LIVE_DASHBOARD("Live Dashboard", "SYNC"),
    NAP_DOWN_SHEET("NAP DOWN Sheet", "62"),
    PROVINCE_PENDING("Province Pending", "BY PROV"),
    OPERATIONS_DETAILS("Ops Details", "6 TABLES"),
    AGENT_AND_FORMULAS("Agent & Formulas", "AUTO-FIX"),
    WEB_BROWSER_VIEW("Live Web & Vercel", "ONLINE")
}

data class DatabaseFilters(
    val month: String = "ALL",
    val date: String = "ALL",
    val province: String = "ALL",
    val status: String = "ALL",
    val finding: String = "ALL",
    val rfo: String = "ALL",
    val concern: String = "ALL",
    val searchQuery: String = ""
)

data class NapDownFilters(
    val province: String = "ALL",
    val municipality: String = "ALL",
    val barangay: String = "ALL",
    val searchQuery: String = ""
)

data class AgingBucket(
    val label: String,
    val count: Int,
    val percentage: Double,
    val severityColor: String // "GREEN", "AMBER", "RED"
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val activeSection: DashboardSection = DashboardSection.LIVE_DASHBOARD,
    val isLeftDrawerOpen: Boolean = false,
    val autoRefreshIntervalSec: Int = 15,
    val isAutoRefreshEnabled: Boolean = true,
    val syncErrorBanner: String? = null,

    // Agent & Health
    val agentHealth: AgentHealthSummary? = null,

    // Global DB Filters & Options
    val dbFilters: DatabaseFilters = DatabaseFilters(),
    val availableMonths: List<String> = emptyList(),
    val availableDatesForMonth: List<String> = emptyList(),
    val provinceOptions: List<String> = emptyList(),
    val statusOptions: List<String> = emptyList(),
    val findingOptions: List<String> = emptyList(),
    val rfoOptions: List<String> = emptyList(),
    val concernOptions: List<String> = emptyList(),

    // KPIs (computed using exact spreadsheet formulas)
    val totalFilteredRecords: Int = 0,
    val pendingCount: Int = 0,
    val napDownCount: Int = 0,
    val restoredCount: Int = 0,
    val pendingRatePercent: Double = 0.0,

    // Status Distribution & Table
    val statusCountsList: List<IssueCountItem> = emptyList(),

    // Pending vs Restored of the Day
    val selectedRestoredDayLabel: String = "Today",
    val pendingForDayChart: Int = 0,
    val restoredOfDayCount: Int = 0,

    // NAP DOWN Sheet Filters & Data
    val napFilters: NapDownFilters = NapDownFilters(),
    val napProvinceOptions: List<String> = emptyList(),
    val napMunicipalityOptions: List<String> = emptyList(),
    val napBarangayOptions: List<String> = emptyList(),
    val filteredNapDownRows: List<CleanedNapDownRow> = emptyList(),
    val napDownPivotSummary: List<NapDownPivotSummaryRow> = emptyList(),

    // Province Pending Groups
    val provincePendingGroups: List<ProvincePendingGroup> = emptyList(),
    val expandedProvinces: Set<String> = setOf("ALBAY", "CAMARINES NORTE", "CAMARINES SUR"),

    // Operations Details (6 Analytical Tables)
    val pendingByMunicipality: List<IssueCountItem> = emptyList(),
    val pendingByIssue: List<IssueCountItem> = emptyList(),
    val technicianWorkload: List<TechnicianWorkloadItem> = emptyList(),
    val agingBuckets: List<AgingBucket> = emptyList(),
    val restoredByTechnician: List<IssueCountItem> = emptyList(),
    val restoredByMunicipality: List<IssueCountItem> = emptyList()
)

class OutageViewModel(
    private val repository: OutageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Separate lightweight flow for the countdown timer so it NEVER triggers full-screen recomposition!
    private val _secondsRemaining = MutableStateFlow(15)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    private var currentProcessed: DataIntegrityAgent.ProcessedSnapshot? = null
    private var rawStatusCountsFromSnapshot: Map<String, Int>? = null
    private var rawTotalDatabaseRows: Int = 0
    private var timerJob: Job? = null
    private var filterJob: Job? = null

    init {
        loadInitialAndSync()
        startAutoRefreshLoop()
    }

    private fun loadInitialAndSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Load seed/cache on Dispatchers.Default so UI renders immediately without blocking Main thread
            val seedSnap = repository.loadCachedOrSeedSnapshot()
            applySnapshotAsync(seedSnap, isLive = true, errorMsg = null)
            _uiState.update { it.copy(isLoading = false) }

            // Then trigger live network sync in background
            refreshNow()
        }
    }

    private fun startAutoRefreshLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val state = _uiState.value
                if (!state.isAutoRefreshEnabled || state.isSyncing) continue
                val nextSec = _secondsRemaining.value - 1
                if (nextSec <= 0) {
                    _secondsRemaining.value = state.autoRefreshIntervalSec
                    refreshNow()
                } else {
                    _secondsRemaining.value = nextSec
                }
            }
        }
    }

    fun refreshNow() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            _secondsRemaining.value = _uiState.value.autoRefreshIntervalSec

            val result = repository.fetchLiveOrFallbackSnapshot()
            applySnapshotAsync(
                snapshot = result.snapshot,
                isLive = result.isLiveFromNetwork,
                errorMsg = result.errorMessage
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSyncing = false
                )
            }
            _secondsRemaining.value = _uiState.value.autoRefreshIntervalSec
        }
    }

    private suspend fun applySnapshotAsync(
        snapshot: OutageSnapshotResponse,
        isLive: Boolean,
        errorMsg: String?
    ) {
        val currentState = _uiState.value
        val newState = withContext(Dispatchers.Default) {
            rawStatusCountsFromSnapshot = snapshot.statusCounts
            rawTotalDatabaseRows = snapshot.diagnostics?.databaseRows ?: snapshot.rows.size
            val processed = DataIntegrityAgent.inspectAndRepair(snapshot, isLive)
            currentProcessed = processed
            computeFilteredState(
                baseState = currentState,
                processed = processed,
                dbFilters = currentState.dbFilters,
                napFilters = currentState.napFilters,
                syncErrorBanner = errorMsg
            )
        }
        _uiState.value = newState
    }

    fun selectSection(section: DashboardSection) {
        _uiState.update {
            it.copy(
                activeSection = section,
                isLeftDrawerOpen = false
            )
        }
    }

    fun toggleLeftDrawer(open: Boolean? = null) {
        _uiState.update {
            it.copy(isLeftDrawerOpen = open ?: !it.isLeftDrawerOpen)
        }
    }

    fun setAutoRefreshInterval(seconds: Int) {
        _secondsRemaining.value = seconds
        _uiState.update {
            it.copy(
                autoRefreshIntervalSec = seconds,
                isAutoRefreshEnabled = true
            )
        }
    }

    fun toggleAutoRefresh() {
        _uiState.update {
            it.copy(isAutoRefreshEnabled = !it.isAutoRefreshEnabled)
        }
    }

    fun toggleProvinceExpanded(province: String) {
        _uiState.update { state ->
            val next = state.expandedProvinces.toMutableSet()
            if (next.contains(province)) next.remove(province) else next.add(province)
            state.copy(expandedProvinces = next)
        }
    }

    fun updateDbFilters(transform: (DatabaseFilters) -> DatabaseFilters) {
        val nextFilters = transform(_uiState.value.dbFilters)
        _uiState.update { it.copy(dbFilters = nextFilters) }
        val proc = currentProcessed ?: return
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            val base = _uiState.value
            val updated = withContext(Dispatchers.Default) {
                computeFilteredState(
                    baseState = base,
                    processed = proc,
                    dbFilters = nextFilters,
                    napFilters = base.napFilters,
                    syncErrorBanner = base.syncErrorBanner
                )
            }
            _uiState.value = updated
        }
    }

    fun resetDbFilters() {
        updateDbFilters { DatabaseFilters() }
    }

    fun updateNapFilters(transform: (NapDownFilters) -> NapDownFilters) {
        val nextNap = transform(_uiState.value.napFilters)
        _uiState.update { it.copy(napFilters = nextNap) }
        val proc = currentProcessed ?: return
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            val base = _uiState.value
            val updated = withContext(Dispatchers.Default) {
                computeFilteredState(
                    baseState = base,
                    processed = proc,
                    dbFilters = base.dbFilters,
                    napFilters = nextNap,
                    syncErrorBanner = base.syncErrorBanner
                )
            }
            _uiState.value = updated
        }
    }

    fun resetNapFilters() {
        updateNapFilters { NapDownFilters() }
    }

    private fun computeFilteredState(
        baseState: DashboardUiState,
        processed: DataIntegrityAgent.ProcessedSnapshot,
        dbFilters: DatabaseFilters,
        napFilters: NapDownFilters,
        syncErrorBanner: String?
    ): DashboardUiState {
        val allDb = processed.cleanedDbRows
        val allNap = processed.cleanedNapDownRows

        val validMonth = if (dbFilters.month == "ALL" || processed.availableMonths.contains(dbFilters.month)) {
            dbFilters.month
        } else "ALL"

        val allowedDates = if (validMonth == "ALL") {
            processed.availableDates
        } else {
            processed.availableDates.filter { it.startsWith(validMonth) }
        }

        val validDate = if (dbFilters.date == "ALL" || allowedDates.contains(dbFilters.date)) {
            dbFilters.date
        } else "ALL"

        val normalizedDbFilters = dbFilters.copy(month = validMonth, date = validDate)
        val isUnfilteredGlobal = normalizedDbFilters.month == "ALL" &&
            normalizedDbFilters.date == "ALL" &&
            normalizedDbFilters.province == "ALL" &&
            normalizedDbFilters.status == "ALL" &&
            normalizedDbFilters.finding == "ALL" &&
            normalizedDbFilters.rfo == "ALL" &&
            normalizedDbFilters.concern == "ALL" &&
            normalizedDbFilters.searchQuery.isBlank()

        val queryUpper = normalizedDbFilters.searchQuery.trim().uppercase(Locale.US)

        val filteredDb = if (isUnfilteredGlobal) {
            allDb
        } else {
            allDb.filter { r ->
                if (normalizedDbFilters.province != "ALL" && r.province != normalizedDbFilters.province) return@filter false
                if (normalizedDbFilters.status != "ALL" && r.finalStatus != normalizedDbFilters.status.uppercase(Locale.US)) return@filter false
                if (normalizedDbFilters.finding != "ALL" && r.finding != normalizedDbFilters.finding) return@filter false
                if (normalizedDbFilters.rfo != "ALL" && r.rfo != normalizedDbFilters.rfo) return@filter false
                if (normalizedDbFilters.concern != "ALL" && r.concern != normalizedDbFilters.concern) return@filter false
                if (normalizedDbFilters.date != "ALL" && r.dateEndorsedDayKey != normalizedDbFilters.date) return@filter false
                if (normalizedDbFilters.month != "ALL" && !r.dateEndorsedDayKey.startsWith(normalizedDbFilters.month)) return@filter false
                if (queryUpper.isNotEmpty()) {
                    val hay = "${r.province} ${r.municipality} ${r.barangay} ${r.facility} ${r.finding} ${r.ospTeam} ${r.rfo} ${r.concern}".uppercase(Locale.US)
                    if (!hay.contains(queryUpper)) return@filter false
                }
                true
            }
        }

        val useServerSummaryCounts = isUnfilteredGlobal && rawStatusCountsFromSnapshot != null

        val statusMap: Map<String, Int> = if (useServerSummaryCounts) {
            rawStatusCountsFromSnapshot ?: emptyMap()
        } else {
            val m = HashMap<String, Int>()
            for (r in filteredDb) {
                val s = r.finalStatus.ifEmpty { "BLANK" }
                m[s] = (m[s] ?: 0) + 1
            }
            m
        }

        val totalRecords = if (useServerSummaryCounts) {
            statusMap.values.sum()
        } else {
            filteredDb.size
        }

        val pendingCount = statusMap["PENDING"] ?: filteredDb.count { it.finalStatus == "PENDING" }
        val restoredCount = statusMap["RESTORED"] ?: filteredDb.count { it.finalStatus == "RESTORED" }
        val pendingRate = if (totalRecords > 0) (pendingCount.toDouble() / totalRecords.toDouble()) * 100.0 else 0.0

        val statusCountsList = statusMap.entries
            .sortedWith { a, b ->
                val aPend = a.key.equals("PENDING", ignoreCase = true)
                val bPend = b.key.equals("PENDING", ignoreCase = true)
                when {
                    aPend && !bPend -> -1
                    bPend && !aPend -> 1
                    b.value != a.value -> b.value.compareTo(a.value)
                    else -> a.key.compareTo(b.key, ignoreCase = true)
                }
            }
            .map { (status, count) ->
                val share = if (totalRecords > 0) (count.toDouble() / totalRecords.toDouble()) * 100.0 else 0.0
                IssueCountItem(label = status, count = count, percentage = share)
            }

        val selectedDayKey = if (normalizedDbFilters.date != "ALL") {
            normalizedDbFilters.date
        } else {
            DataIntegrityAgent.manilaTodayKey()
        }

        val restoredOfDay = allDb.count { r ->
            if (r.finalStatus != "RESTORED") return@count false
            if (normalizedDbFilters.province != "ALL" && r.province != normalizedDbFilters.province) return@count false
            if (normalizedDbFilters.finding != "ALL" && r.finding != normalizedDbFilters.finding) return@count false
            if (normalizedDbFilters.rfo != "ALL" && r.rfo != normalizedDbFilters.rfo) return@count false
            if (normalizedDbFilters.concern != "ALL" && r.concern != normalizedDbFilters.concern) return@count false
            r.dateRestoredDayKey == selectedDayKey
        }

        val napProvinces = allNap.map { it.province }.filter { it.isNotEmpty() }.distinct().sorted()
        val validNapProvince = if (napFilters.province == "ALL" || napProvinces.contains(napFilters.province)) {
            napFilters.province
        } else "ALL"

        val napMunicipalities = allNap
            .filter { validNapProvince == "ALL" || it.province == validNapProvince }
            .map { it.municipality }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
        val validNapMunicipality = if (napFilters.municipality == "ALL" || napMunicipalities.contains(napFilters.municipality)) {
            napFilters.municipality
        } else "ALL"

        val napBarangays = allNap
            .filter {
                (validNapProvince == "ALL" || it.province == validNapProvince) &&
                    (validNapMunicipality == "ALL" || it.municipality == validNapMunicipality)
            }
            .map { it.barangay }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
        val validNapBarangay = if (napFilters.barangay == "ALL" || napBarangays.contains(napFilters.barangay)) {
            napFilters.barangay
        } else "ALL"

        val normalizedNapFilters = napFilters.copy(
            province = validNapProvince,
            municipality = validNapMunicipality,
            barangay = validNapBarangay
        )
        val napQueryUpper = normalizedNapFilters.searchQuery.trim().uppercase(Locale.US)

        val filteredNap = allNap.filter { r ->
            if (normalizedNapFilters.province != "ALL" && r.province != normalizedNapFilters.province) return@filter false
            if (normalizedNapFilters.municipality != "ALL" && r.municipality != normalizedNapFilters.municipality) return@filter false
            if (normalizedNapFilters.barangay != "ALL" && r.barangay != normalizedNapFilters.barangay) return@filter false
            if (napQueryUpper.isNotEmpty()) {
                val hay = "${r.province} ${r.municipality} ${r.barangay} ${r.napCode} ${r.finding} ${r.coordinates}".uppercase(Locale.US)
                if (!hay.contains(napQueryUpper)) return@filter false
            }
            true
        }

        val napSummaryMap = HashMap<Pair<String, String>, Int>()
        for (r in filteredNap) {
            val key = Pair(r.province.ifEmpty { "UNKNOWN" }, r.finding.ifEmpty { "BLANK" })
            napSummaryMap[key] = (napSummaryMap[key] ?: 0) + 1
        }
        val napPivotRows = napSummaryMap.entries
            .map { (pair, cnt) -> NapDownPivotSummaryRow(pair.first, pair.second, cnt) }
            .sortedWith(compareBy<NapDownPivotSummaryRow> { it.province }.thenBy { it.finding })

        val pendingRows = filteredDb.filter { it.finalStatus == "PENDING" }
        val restoredRows = filteredDb.filter { it.finalStatus == "RESTORED" }

        val napByProvMunFind = allNap.groupBy { Triple(it.province, it.municipality, it.finding) }
        val enrichedPendingRows = pendingRows.mapIndexed { idx, row ->
            if (row.facility.isNotEmpty()) {
                row
            } else {
                val candidates = napByProvMunFind[Triple(row.province, row.municipality, row.finding)]
                val matchedNapCode = candidates?.getOrNull(idx % (candidates.size.coerceAtLeast(1)))?.napCode ?: ""
                if (matchedNapCode.isNotEmpty()) row.copy(facility = matchedNapCode) else row
            }
        }

        val provinceGroups = enrichedPendingRows
            .groupBy { it.province.ifEmpty { "UNSPECIFIED" } }
            .entries
            .sortedBy { it.key }
            .map { (prov, rowsInProv) ->
                val issues = groupAndCount(rowsInProv) {
                    it.napStatus.ifEmpty { it.concern.ifEmpty { it.rfo.ifEmpty { "UNSPECIFIED" } } }
                }
                val sortedDetails = rowsInProv.sortedByDescending { it.ageDays ?: 0.0 }
                ProvincePendingGroup(
                    province = prov,
                    pendingCount = rowsInProv.size,
                    issueSummary = issues,
                    detailRows = sortedDetails
                )
            }

        val pendingByMun = groupAndCount(enrichedPendingRows) { it.municipality.ifEmpty { "UNSPECIFIED" } }
        val pendingByIss = groupAndCount(enrichedPendingRows) {
            it.napStatus.ifEmpty { it.concern.ifEmpty { it.rfo.ifEmpty { "UNSPECIFIED" } } }
        }

        val techWorkload = enrichedPendingRows
            .groupBy { it.ospTeam.ifEmpty { "UNSPECIFIED" } }
            .map { (tech, list) ->
                val oldest = list.mapNotNull { it.ageDays }.maxOrNull() ?: 0.0
                TechnicianWorkloadItem(
                    technician = tech,
                    activeCount = list.size,
                    oldestDays = oldest
                )
            }
            .sortedWith(compareByDescending<TechnicianWorkloadItem> { it.activeCount }.thenBy { it.technician })

        val ageNew = enrichedPendingRows.count { (it.ageDays ?: 0.0) <= 1.0 }
        val ageAging = enrichedPendingRows.count { val d = it.ageDays ?: 0.0; d > 1.0 && d < 4.0 }
        val ageCritical = enrichedPendingRows.count { (it.ageDays ?: 0.0) >= 4.0 }
        val pTotal = enrichedPendingRows.size.coerceAtLeast(1).toDouble()

        val agingBuckets = listOf(
            AgingBucket("NEW (0–1 DAY)", ageNew, if (enrichedPendingRows.isEmpty()) 0.0 else (ageNew / pTotal) * 100.0, "GREEN"),
            AgingBucket("AGING (2–3 DAYS)", ageAging, if (enrichedPendingRows.isEmpty()) 0.0 else (ageAging / pTotal) * 100.0, "AMBER"),
            AgingBucket("CRITICAL (4+ DAYS)", ageCritical, if (enrichedPendingRows.isEmpty()) 0.0 else (ageCritical / pTotal) * 100.0, "RED")
        )

        val restoredByTech = groupAndCount(restoredRows) { it.ospTeam.ifEmpty { "UNSPECIFIED" } }
        val restoredByMun = groupAndCount(restoredRows) { it.municipality.ifEmpty { "UNSPECIFIED" } }

        return baseState.copy(
            syncErrorBanner = syncErrorBanner,
            agentHealth = processed.agentHealth,
            dbFilters = normalizedDbFilters,
            availableMonths = processed.availableMonths,
            availableDatesForMonth = allowedDates,
            provinceOptions = processed.filterOptions.province,
            statusOptions = processed.filterOptions.status,
            findingOptions = processed.filterOptions.finding,
            rfoOptions = processed.filterOptions.rfo,
            concernOptions = processed.filterOptions.concern,
            totalFilteredRecords = totalRecords,
            pendingCount = pendingCount,
            napDownCount = filteredNap.size,
            restoredCount = restoredCount,
            pendingRatePercent = pendingRate,
            statusCountsList = statusCountsList,
            selectedRestoredDayLabel = formatDayLabel(selectedDayKey),
            pendingForDayChart = pendingCount,
            restoredOfDayCount = restoredOfDay,
            napFilters = normalizedNapFilters,
            napProvinceOptions = napProvinces,
            napMunicipalityOptions = napMunicipalities,
            napBarangayOptions = napBarangays,
            filteredNapDownRows = filteredNap,
            napDownPivotSummary = napPivotRows,
            provincePendingGroups = provinceGroups,
            pendingByMunicipality = pendingByMun,
            pendingByIssue = pendingByIss,
            technicianWorkload = techWorkload,
            agingBuckets = agingBuckets,
            restoredByTechnician = restoredByTech,
            restoredByMunicipality = restoredByMun
        )
    }

    private fun <T> groupAndCount(rows: List<T>, selector: (T) -> String): List<IssueCountItem> {
        if (rows.isEmpty()) return emptyList()
        val counts = HashMap<String, Int>()
        for (r in rows) {
            val k = selector(r).trim().ifEmpty { "UNSPECIFIED" }
            counts[k] = (counts[k] ?: 0) + 1
        }
        val total = rows.size.toDouble()
        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { (k, v) ->
                IssueCountItem(
                    label = k,
                    count = v,
                    percentage = (v.toDouble() / total) * 100.0
                )
            }
    }

    companion object {
        fun formatDayLabel(dayKey: String): String {
            if (dayKey.isBlank() || dayKey == "ALL") return "All dates"
            return try {
                val ld = LocalDate.parse(dayKey)
                ld.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))
            } catch (_: Exception) {
                dayKey
            }
        }

        fun formatMonthLabel(monthKey: String): String {
            if (monthKey.isBlank() || monthKey == "ALL") return "All months"
            return try {
                val ld = LocalDate.parse("$monthKey-01")
                ld.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
            } catch (_: Exception) {
                monthKey
            }
        }

        fun provideFactory(repository: OutageRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OutageViewModel(repository) as T
                }
            }
    }
}
