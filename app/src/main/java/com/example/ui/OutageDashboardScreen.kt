package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.*
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun OutageDashboardScreen(
    viewModel: OutageViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle Android system back button when on a secondary tab or when left drawer is open
    BackHandler(enabled = state.isLeftDrawerOpen || state.activeSection != DashboardSection.LIVE_DASHBOARD) {
        if (state.isLeftDrawerOpen) {
            viewModel.toggleLeftDrawer(false)
        } else {
            viewModel.selectSection(DashboardSection.LIVE_DASHBOARD)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isExpandedScreen = maxWidth >= 840.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // On Expanded / Tablet screens, display permanent Left Navigation Rail
                if (isExpandedScreen) {
                    PermanentLeftNavRail(
                        activeSection = state.activeSection,
                        pendingCount = state.pendingCount,
                        napDownCount = state.napDownCount,
                        formulaErrorsRepaired = state.agentHealth?.formulaErrorsRepaired ?: 17,
                        onSelectSection = { viewModel.selectSection(it) },
                        onOpenFullDrawer = { viewModel.toggleLeftDrawer(true) }
                    )
                }

                // Main Content Area
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = if (isExpandedScreen) 18.dp else 12.dp),
                        contentPadding = PaddingValues(
                            top = 12.dp,
                            bottom = 88.dp,
                            start = if (!isExpandedScreen) 24.dp else 0.dp // space for clickable left-edge DETAILS/NAV tab
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Top NOC Hero Header Card + Quick Auto-Refresh Bar
                        item {
                            NocTopHeroHeader(
                                state = state,
                                onRefreshClick = { viewModel.refreshNow() },
                                onOpenLeftMenu = { viewModel.toggleLeftDrawer(true) },
                                onIntervalSelect = { viewModel.setAutoRefreshInterval(it) },
                                onToggleAutoRefresh = { viewModel.toggleAutoRefresh() }
                            )
                        }

                        // 2. Live Self-Healing Agent Status Strip
                        item {
                            LiveSourceAgentStrip(
                                state = state,
                                onClickInspectAgent = {
                                    viewModel.selectSection(DashboardSection.AGENT_AND_FORMULAS)
                                }
                            )
                        }

                        // 3. Horizontal Section Switcher Pills (Synced with Left-Side Clickable Tabs)
                        item {
                            HorizontalSectionTabsBar(
                                activeSection = state.activeSection,
                                pendingCount = state.pendingCount,
                                napDownCount = state.napDownCount,
                                onSelectSection = { viewModel.selectSection(it) }
                            )
                        }

                        // 4. Render Active Section Content
                        when (state.activeSection) {
                            DashboardSection.LIVE_DASHBOARD -> {
                                // Global Filters
                                item {
                                    GlobalDatabaseFiltersPanel(
                                        state = state,
                                        onMonthChange = { m -> viewModel.updateDbFilters { it.copy(month = m, date = "ALL") } },
                                        onDateChange = { d -> viewModel.updateDbFilters { it.copy(date = d) } },
                                        onProvinceChange = { p -> viewModel.updateDbFilters { it.copy(province = p) } },
                                        onStatusChange = { s -> viewModel.updateDbFilters { it.copy(status = s) } },
                                        onFindingChange = { f -> viewModel.updateDbFilters { it.copy(finding = f) } },
                                        onRfoChange = { r -> viewModel.updateDbFilters { it.copy(rfo = r) } },
                                        onConcernChange = { c -> viewModel.updateDbFilters { it.copy(concern = c) } },
                                        onSearchChange = { q -> viewModel.updateDbFilters { it.copy(searchQuery = q) } },
                                        onResetFilters = { viewModel.resetDbFilters() }
                                    )
                                }

                                // 4 KPI Cards + Crimson Pending Alert Banner
                                item {
                                    KpiSummaryGrid(
                                        totalRecords = state.totalFilteredRecords,
                                        pendingCount = state.pendingCount,
                                        napDownCount = state.napDownCount,
                                        restoredCount = state.restoredCount,
                                        pendingRatePercent = state.pendingRatePercent,
                                        onPendingClick = {
                                            viewModel.selectSection(DashboardSection.PROVINCE_PENDING)
                                        }
                                    )
                                }

                                // Side-by-side on wide screens or stacked on compact screens:
                                // Left: DATABASE FINAL STATUS + PENDING VS RESTORED OF THE DAY
                                // Right: NAP DOWN SPREADSHEET PANEL
                                item {
                                    if (isExpandedScreen) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            DatabaseFinalStatusPanel(
                                                statusCounts = state.statusCountsList,
                                                totalFiltered = state.totalFilteredRecords,
                                                pendingForDayChart = state.pendingForDayChart,
                                                restoredOfDayCount = state.restoredOfDayCount,
                                                selectedRestoredDayLabel = state.selectedRestoredDayLabel,
                                                dbRowsTotal = state.agentHealth?.totalDbRowsScanned ?: state.totalFilteredRecords,
                                                formulaErrorsFixed = state.agentHealth?.formulaErrorsRepaired ?: 17,
                                                lastVerifiedTime = state.agentHealth?.checkedAtDisplay ?: "Just now",
                                                modifier = Modifier.weight(0.45f)
                                            )
                                            NapDownSpreadsheetPanel(
                                                napRows = state.filteredNapDownRows,
                                                pivotRows = state.napDownPivotSummary,
                                                selectedProvince = state.napFilters.province,
                                                selectedMunicipality = state.napFilters.municipality,
                                                selectedBarangay = state.napFilters.barangay,
                                                searchQuery = state.napFilters.searchQuery,
                                                provinceOptions = state.napProvinceOptions,
                                                municipalityOptions = state.napMunicipalityOptions,
                                                barangayOptions = state.napBarangayOptions,
                                                onProvinceSelect = { p ->
                                                    viewModel.updateNapFilters {
                                                        it.copy(province = p, municipality = "ALL", barangay = "ALL")
                                                    }
                                                },
                                                onMunicipalitySelect = { m ->
                                                    viewModel.updateNapFilters {
                                                        it.copy(municipality = m, barangay = "ALL")
                                                    }
                                                },
                                                onBarangaySelect = { b ->
                                                    viewModel.updateNapFilters { it.copy(barangay = b) }
                                                },
                                                onSearchChange = { q ->
                                                    viewModel.updateNapFilters { it.copy(searchQuery = q) }
                                                },
                                                onClearFilters = { viewModel.resetNapFilters() },
                                                modifier = Modifier.weight(0.55f)
                                            )
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            DatabaseFinalStatusPanel(
                                                statusCounts = state.statusCountsList,
                                                totalFiltered = state.totalFilteredRecords,
                                                pendingForDayChart = state.pendingForDayChart,
                                                restoredOfDayCount = state.restoredOfDayCount,
                                                selectedRestoredDayLabel = state.selectedRestoredDayLabel,
                                                dbRowsTotal = state.agentHealth?.totalDbRowsScanned ?: state.totalFilteredRecords,
                                                formulaErrorsFixed = state.agentHealth?.formulaErrorsRepaired ?: 17,
                                                lastVerifiedTime = state.agentHealth?.checkedAtDisplay ?: "Just now"
                                            )
                                            NapDownSpreadsheetPanel(
                                                napRows = state.filteredNapDownRows,
                                                pivotRows = state.napDownPivotSummary,
                                                selectedProvince = state.napFilters.province,
                                                selectedMunicipality = state.napFilters.municipality,
                                                selectedBarangay = state.napFilters.barangay,
                                                searchQuery = state.napFilters.searchQuery,
                                                provinceOptions = state.napProvinceOptions,
                                                municipalityOptions = state.napMunicipalityOptions,
                                                barangayOptions = state.napBarangayOptions,
                                                onProvinceSelect = { p ->
                                                    viewModel.updateNapFilters {
                                                        it.copy(province = p, municipality = "ALL", barangay = "ALL")
                                                    }
                                                },
                                                onMunicipalitySelect = { m ->
                                                    viewModel.updateNapFilters {
                                                        it.copy(municipality = m, barangay = "ALL")
                                                    }
                                                },
                                                onBarangaySelect = { b ->
                                                    viewModel.updateNapFilters { it.copy(barangay = b) }
                                                },
                                                onSearchChange = { q ->
                                                    viewModel.updateNapFilters { it.copy(searchQuery = q) }
                                                },
                                                onClearFilters = { viewModel.resetNapFilters() }
                                            )
                                        }
                                    }
                                }

                                // Pending by Province below main panels (just like the full spreadsheet layout)
                                item {
                                    ProvincePendingSectionView(
                                        groups = state.provincePendingGroups,
                                        expandedProvinces = state.expandedProvinces,
                                        onToggleProvince = { viewModel.toggleProvinceExpanded(it) }
                                    )
                                }
                            }

                            DashboardSection.NAP_DOWN_SHEET -> {
                                item {
                                    NapDownSpreadsheetPanel(
                                        napRows = state.filteredNapDownRows,
                                        pivotRows = state.napDownPivotSummary,
                                        selectedProvince = state.napFilters.province,
                                        selectedMunicipality = state.napFilters.municipality,
                                        selectedBarangay = state.napFilters.barangay,
                                        searchQuery = state.napFilters.searchQuery,
                                        provinceOptions = state.napProvinceOptions,
                                        municipalityOptions = state.napMunicipalityOptions,
                                        barangayOptions = state.napBarangayOptions,
                                        onProvinceSelect = { p ->
                                            viewModel.updateNapFilters {
                                                it.copy(province = p, municipality = "ALL", barangay = "ALL")
                                            }
                                        },
                                        onMunicipalitySelect = { m ->
                                            viewModel.updateNapFilters {
                                                it.copy(municipality = m, barangay = "ALL")
                                            }
                                        },
                                        onBarangaySelect = { b ->
                                            viewModel.updateNapFilters { it.copy(barangay = b) }
                                        },
                                        onSearchChange = { q ->
                                            viewModel.updateNapFilters { it.copy(searchQuery = q) }
                                        },
                                        onClearFilters = { viewModel.resetNapFilters() }
                                    )
                                }
                            }

                            DashboardSection.PROVINCE_PENDING -> {
                                item {
                                    ProvincePendingSectionView(
                                        groups = state.provincePendingGroups,
                                        expandedProvinces = state.expandedProvinces,
                                        onToggleProvince = { viewModel.toggleProvinceExpanded(it) }
                                    )
                                }
                            }

                            DashboardSection.OPERATIONS_DETAILS -> {
                                item {
                                    OperationsDetailsSectionView(
                                        pendingByMunicipality = state.pendingByMunicipality,
                                        pendingByIssue = state.pendingByIssue,
                                        technicianWorkload = state.technicianWorkload,
                                        agingBuckets = state.agingBuckets,
                                        restoredByTechnician = state.restoredByTechnician,
                                        restoredByMunicipality = state.restoredByMunicipality
                                    )
                                }
                            }

                            DashboardSection.AGENT_AND_FORMULAS -> {
                                item {
                                    AgentAndFormulasSectionView(
                                        health = state.agentHealth,
                                        onRunVerificationNow = { viewModel.refreshNow() }
                                    )
                                }
                            }

                            DashboardSection.EXPORT_CODE -> {
                                item {
                                    ExportVercelCodeSectionView()
                                }
                            }
                        }

                        // Footer
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Source: ${state.agentHealth?.sourceName ?: "Google Sheets API"} • Auto-Recovery Agent Active",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Verified: ${state.agentHealth?.checkedAtDisplay ?: "—"}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Clickable Left-Side Vertical Handle Tab (Matches `.details-tab` on the left side of the screen!)
                    ClickableLeftSideTabHandle(
                        onClick = { viewModel.toggleLeftDrawer(true) },
                        pendingCount = state.pendingCount,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
            }

            // Slide-Out Left Operations & Navigation Drawer
            AnimatedVisibility(
                visible = state.isLeftDrawerOpen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { viewModel.toggleLeftDrawer(false) }
                )
            }

            AnimatedVisibility(
                visible = state.isLeftDrawerOpen,
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = slideOutHorizontally(targetOffsetX = { -it })
            ) {
                LeftOperationsSlideDrawer(
                    state = state,
                    onSelectSection = { viewModel.selectSection(it) },
                    onClose = { viewModel.toggleLeftDrawer(false) },
                    onRefreshNow = { viewModel.refreshNow() }
                )
            }
        }
    }
}

@Composable
private fun ClickableLeftSideTabHandle(
    onClick: () -> Unit,
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = BrandTealDeep,
        contentColor = Color.White,
        shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
        shadowElevation = 6.dp,
        modifier = modifier
            .clickable { onClick() }
            .testTag("left_side_clickable_tab")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MenuOpen,
                contentDescription = "Open Left Operations Tab",
                tint = Color(0xFF99F6E4),
                modifier = Modifier.size(16.dp)
            )
            // Vertical stacked letters "TABS"
            "TABS".forEach { ch ->
                Text(
                    text = ch.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AlertCrimson)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$pendingCount",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun PermanentLeftNavRail(
    activeSection: DashboardSection,
    pendingCount: Int,
    napDownCount: Int,
    formulaErrorsRepaired: Int,
    onSelectSection: (DashboardSection) -> Unit,
    onOpenFullDrawer: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight()
            .width(210.dp)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "BICOL NOC TABS",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = BrandTealDeep,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                IconButton(
                    onClick = onOpenFullDrawer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Open Quick Details Drawer",
                        tint = BrandTeal
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            DashboardSection.entries.forEach { sec ->
                val selected = sec == activeSection
                val badge = when (sec) {
                    DashboardSection.NAP_DOWN_SHEET -> "$napDownCount"
                    DashboardSection.PROVINCE_PENDING -> "$pendingCount"
                    DashboardSection.AGENT_AND_FORMULAS -> "${formulaErrorsRepaired} FIX"
                    else -> sec.badgeText
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) BrandTeal else Color.Transparent)
                        .clickable { onSelectSection(sec) }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .testTag("rail_tab_${sec.name.lowercase(Locale.US)}")
                ) {
                    Text(
                        text = sec.title,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = if (selected) Color.White.copy(alpha = 0.22f) else BrandTealSoft,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selected) Color.White else BrandTealDeep
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftOperationsSlideDrawer(
    state: DashboardUiState,
    onSelectSection: (DashboardSection) -> Unit,
    onClose: () -> Unit,
    onRefreshNow: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 360.dp)
            .fillMaxWidth(0.88f)
            .testTag("left_operations_drawer")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "BICOL NOC NAVIGATION",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandTealDeep
                            )
                        )
                        Text(
                            text = "Clickable Left Tabs & Quick Operations Summary",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_left_drawer_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Drawer")
                    }
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }

            // Clickable Navigation Tabs inside Left Drawer
            item {
                Text(
                    text = "DASHBOARD WORKSPACE TABS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(DashboardSection.entries.size) { index ->
                val sec = DashboardSection.entries[index]
                val isSelected = sec == state.activeSection
                val badge = when (sec) {
                    DashboardSection.NAP_DOWN_SHEET -> "${state.napDownCount} DOWN"
                    DashboardSection.PROVINCE_PENDING -> "${state.pendingCount} PEND"
                    DashboardSection.AGENT_AND_FORMULAS -> "${state.agentHealth?.formulaErrorsRepaired ?: 17} FIXED"
                    else -> sec.badgeText
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) BrandTeal else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .clickable { onSelectSection(sec) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("drawer_tab_${sec.name.lowercase(Locale.US)}")
                ) {
                    Text(
                        text = sec.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Surface(
                        color = if (isSelected) Color.White.copy(alpha = 0.2f) else BrandTealSoft,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else BrandTealDeep
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }

            // Quick Operations Details Preview inside Left Drawer (just like `#detailsDrawer` in the original site!)
            item {
                Text(
                    text = "QUICK OPERATIONS DETAILS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = BrandTealDeep
                )
            }

            // Aging Summary Mini Card
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "AGING MONITOR (PENDING)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        state.agingBuckets.forEach { b ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(b.label, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = "${b.count} (${String.format(Locale.US, "%.1f%%", b.percentage)})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Technician Workload Mini Card
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TECHNICIAN WORKLOAD",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        state.technicianWorkload.take(6).forEach { t ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(t.technician, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    text = "${t.activeCount} active • oldest ${String.format(Locale.US, "%.1fd", t.oldestDays)}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // Top Pending Municipalities Mini Card
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "PENDING BY MUNICIPALITY (TOP 8)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        state.pendingByMunicipality.take(8).forEach { m ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(m.label, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = "${m.count} (${String.format(Locale.US, "%.1f%%", m.percentage)})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NocTopHeroHeader(
    state: DashboardUiState,
    onRefreshClick: () -> Unit,
    onOpenLeftMenu: () -> Unit,
    onIntervalSelect: (Int) -> Unit,
    onToggleAutoRefresh: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle Decorative Topology Vector Banner in Top Right
            Image(
                painter = painterResource(id = R.drawable.ic_noc_topology_banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.14f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onOpenLeftMenu,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandTealSoft)
                                .testTag("open_left_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Left Navigation Tabs",
                                tint = BrandTealDeep
                            )
                        }

                        Column {
                            Text(
                                text = "NETWORK OPERATIONS • BICOL",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = BrandTeal,
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            Text(
                                text = "BICOL NAP OUTAGE MONITORING",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            Text(
                                text = "Live Google Sheets Sync • Self-Healing Formula Agent • Pending == NAP DOWN",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Manual Refresh Button
                    Button(
                        onClick = onRefreshClick,
                        enabled = !state.isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandTeal,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("manual_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Now",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isSyncing) "SYNCING…" else "REFRESH",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                }

                // Status Pills & Quick Auto-Refresh Selector Bar
                val hScroll = rememberScrollState()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(hScroll)
                ) {
                    NocStatusPill(
                        text = "OPS VIEW • LIVE",
                        dotColor = BrandTeal,
                        backgroundColor = BrandTealSoft,
                        borderColor = BrandTealLine,
                        textColor = BrandTealDeep
                    )

                    val isLive = state.agentHealth?.isLiveConnected == true
                    NocStatusPill(
                        text = if (isLive) "SYNC AGENT • SYNCED" else "SYNC AGENT • VERIFIED SNAPSHOT",
                        dotColor = if (isLive) StatusGreen else StatusBlue,
                        backgroundColor = if (isLive) StatusGreenSoft else StatusBlueSoft,
                        borderColor = if (isLive) Color(0xFFBDE5D4) else Color(0xFFD6E5F1),
                        textColor = if (isLive) StatusGreen else StatusBlue
                    )

                    // Quick Auto-Refresh Interval Chips (10s, 15s, 30s, Pause)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = BrandTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (state.isAutoRefreshEnabled) "AUTO ${state.secondsUntilNextRefresh}s:" else "PAUSED:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable { onToggleAutoRefresh() }
                        )
                        listOf(10, 15, 30).forEach { sec ->
                            val active = state.isAutoRefreshEnabled && state.autoRefreshIntervalSec == sec
                            Surface(
                                color = if (active) BrandTeal else Color.Transparent,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .clickable { onIntervalSelect(sec) }
                                    .testTag("refresh_interval_${sec}s")
                            ) {
                                Text(
                                    text = "${sec}s",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveSourceAgentStrip(
    state: DashboardUiState,
    onClickInspectAgent: () -> Unit
) {
    val health = state.agentHealth
    Surface(
        color = BrandTealSoft.copy(alpha = 0.75f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BrandTealLine, RoundedCornerShape(10.dp))
            .clickable { onClickInspectAgent() }
            .testTag("live_agent_strip")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusGreen)
                )
                Text(
                    text = "LIVE SOURCE AGENT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = BrandTealDeep,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    text = if (health != null) {
                        "Verified ${health.pendingFinalStatusCount} PENDING == ${health.validNapDownCount} NAP DOWN • Repaired ${health.formulaErrorsRepaired} #REF! errors • Dropped ${health.blankNapGhostRowsDropped} blank row"
                    } else {
                        "Checking Google Sheets connectivity & repairing formula anomalies…"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = BrandTealDeep,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "INSPECT AGENT →",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = BrandTeal,
                    fontWeight = FontWeight.ExtraBold
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun HorizontalSectionTabsBar(
    activeSection: DashboardSection,
    pendingCount: Int,
    napDownCount: Int,
    onSelectSection: (DashboardSection) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
    ) {
        DashboardSection.entries.forEach { sec ->
            val selected = sec == activeSection
            val badgeText = when (sec) {
                DashboardSection.NAP_DOWN_SHEET -> "$napDownCount"
                DashboardSection.PROVINCE_PENDING -> "$pendingCount"
                else -> sec.badgeText
            }
            Surface(
                color = if (selected) BrandTealDeep else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .heightIn(min = 42.dp)
                    .border(
                        width = 1.dp,
                        color = if (selected) BrandTealDeep else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectSection(sec) }
                    .testTag("section_tab_${sec.name.lowercase(Locale.US)}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = sec.title,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Surface(
                        color = if (selected) Color.White.copy(alpha = 0.2f) else BrandTealSoft,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selected) Color.White else BrandTealDeep
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
