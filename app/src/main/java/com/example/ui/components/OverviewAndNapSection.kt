package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CleanedNapDownRow
import com.example.data.IssueCountItem
import com.example.data.NapDownPivotSummaryRow
import com.example.ui.DashboardUiState
import com.example.ui.OutageViewModel
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun GlobalDatabaseFiltersPanel(
    state: DashboardUiState,
    onMonthChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onProvinceChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onFindingChange: (String) -> Unit,
    onRfoChange: (String) -> Unit,
    onConcernChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filtersExpanded by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Brand Accent Bar (3dp solid teal like the web CSS `border-top: 3px solid var(--brand)`)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(BrandTeal)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable { filtersExpanded = !filtersExpanded }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = BrandTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "FILTER & FOCUS • LIVE SPREADSHEET QUERY",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            Text(
                                text = "All dashboard counts, charts, and tables update from the same filtered DATABASE rows.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onResetFilters,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .testTag("reset_filters_button")
                        ) {
                            Text(
                                text = "RESET FILTERS",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                        IconButton(
                            onClick = { filtersExpanded = !filtersExpanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (filtersExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Filters"
                            )
                        }
                    }
                }

                if (filtersExpanded) {
                    // Quick Search Input
                    OutlinedTextField(
                        value = state.dbFilters.searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = {
                            Text(
                                "Search municipality, barangay, NAP code, technician, finding, or RFO…",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (state.dbFilters.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("global_search_input")
                    )

                    // Row 1: Month, Date, Province
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NocFilterDropdown(
                            label = "MONTH",
                            selectedValue = state.dbFilters.month,
                            displayValue = { OutageViewModel.formatMonthLabel(it) },
                            options = state.availableMonths,
                            onSelect = onMonthChange,
                            testTagName = "filter_month",
                            modifier = Modifier.weight(1f)
                        )
                        NocFilterDropdown(
                            label = "DATE",
                            selectedValue = state.dbFilters.date,
                            displayValue = { OutageViewModel.formatDayLabel(it) },
                            options = state.availableDatesForMonth,
                            onSelect = onDateChange,
                            testTagName = "filter_date",
                            modifier = Modifier.weight(1f)
                        )
                        NocFilterDropdown(
                            label = "PROVINCE",
                            selectedValue = state.dbFilters.province,
                            options = state.provinceOptions,
                            onSelect = onProvinceChange,
                            testTagName = "filter_province",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Final Status, Findings, RFO, Concern
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NocFilterDropdown(
                            label = "FINAL STATUS",
                            selectedValue = state.dbFilters.status,
                            options = state.statusOptions,
                            onSelect = onStatusChange,
                            testTagName = "filter_status",
                            modifier = Modifier.weight(1f)
                        )
                        NocFilterDropdown(
                            label = "FINDINGS",
                            selectedValue = state.dbFilters.finding,
                            options = state.findingOptions,
                            onSelect = onFindingChange,
                            testTagName = "filter_finding",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NocFilterDropdown(
                            label = "RFO",
                            selectedValue = state.dbFilters.rfo,
                            options = state.rfoOptions,
                            onSelect = onRfoChange,
                            testTagName = "filter_rfo",
                            modifier = Modifier.weight(1f)
                        )
                        NocFilterDropdown(
                            label = "CONCERN",
                            selectedValue = state.dbFilters.concern,
                            options = state.concernOptions,
                            onSelect = onConcernChange,
                            testTagName = "filter_concern",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KpiSummaryGrid(
    totalRecords: Int,
    pendingCount: Int,
    napDownCount: Int,
    restoredCount: Int,
    pendingRatePercent: Double,
    onPendingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. TOTAL RECORDS
            KpiCardItem(
                label = "TOTAL RECORDS",
                valueText = String.format(Locale.US, "%,d", totalRecords),
                noteText = "CURRENT FILTERED DATABASE",
                containerColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline,
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_total_records")
            )

            // 2. PENDING (= NAP DOWN)
            KpiCardItem(
                label = "PENDING (NAP DOWN)",
                valueText = String.format(Locale.US, "%,d", pendingCount),
                noteText = "FINAL STATUS • NAP DOWN = $napDownCount",
                containerColor = AlertCrimsonSoft,
                borderColor = AlertCrimsonBorder,
                valueColor = AlertCrimsonDark,
                labelColor = AlertCrimsonDark,
                onClick = onPendingClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_pending")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 3. RESTORED
            KpiCardItem(
                label = "RESTORED",
                valueText = String.format(Locale.US, "%,d", restoredCount),
                noteText = "FINAL STATUS • VERIFIED",
                containerColor = StatusGreenSoft,
                borderColor = Color(0xFFBDE5D4),
                valueColor = StatusGreen,
                labelColor = StatusGreen,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_restored")
            )

            // 4. PENDING RATE
            KpiCardItem(
                label = "PENDING RATE",
                valueText = String.format(Locale.US, "%.1f%%", pendingRatePercent),
                noteText = "PENDING ÷ TOTAL RECORDS",
                containerColor = BrandTealSoft,
                borderColor = BrandTealLine,
                valueColor = BrandTealDeep,
                labelColor = BrandTealDeep,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_pending_rate")
            )
        }

        // Action Required Crimson Pending Alert Banner (matches `.pending-alert` in spreadsheet dashboard)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AlertCrimsonSoft),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AlertCrimsonBorder, RoundedCornerShape(12.dp))
                .clickable { onPendingClick() }
                .testTag("pending_alert_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AlertCrimson.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AlertCrimson,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PENDING FINAL STATUS == NAP DOWN FORMULA PARITY",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = AlertCrimsonDark,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text(
                        text = "$pendingCount record(s) marked PENDING in FINAL STATUS (${String.format(Locale.US, "%.1f%%", pendingRatePercent)} of filtered DATABASE) • Matches $napDownCount active NAP DOWN rows.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AlertCrimsonDark.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    )
                }

                Surface(
                    color = AlertCrimson,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$pendingCount DOWN",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiCardItem(
    label: String,
    valueText: String,
    noteText: String,
    containerColor: Color,
    borderColor: Color,
    valueColor: Color,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = labelColor
                )
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = valueColor
                )
            )
            Text(
                text = noteText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor.copy(alpha = 0.8f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DatabaseFinalStatusPanel(
    statusCounts: List<IssueCountItem>,
    totalFiltered: Int,
    pendingForDayChart: Int,
    restoredOfDayCount: Int,
    selectedRestoredDayLabel: String,
    dbRowsTotal: Int,
    formulaErrorsFixed: Int,
    lastVerifiedTime: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Panel 1: DATABASE FINAL STATUS
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "DATABASE FINAL STATUS",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Counts are taken directly from the DATABASE FINAL STATUS column (Column Q).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                NocDonutChartWithLegend(
                    items = statusCounts,
                    centerSmallLabel = "Total filtered",
                    centerBigValue = String.format(Locale.US, "%,d", totalFiltered)
                )

                // Full Status Table with PENDING highlighted in Crimson
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "FINAL STATUS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(0.54f)
                        )
                        Text(
                            "COUNT",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.23f)
                        )
                        Text(
                            "SHARE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.23f)
                        )
                    }

                    statusCounts.forEach { item ->
                        val isPending = item.label.equals("PENDING", ignoreCase = true)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isPending) AlertCrimsonSoft else Color.Transparent)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(0.54f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isPending) AlertCrimson else BrandTeal)
                                )
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isPending) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isPending) AlertCrimsonDark else MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isPending) {
                                    Surface(
                                        color = AlertCrimson.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "ACTION REQUIRED",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = AlertCrimsonDark
                                            ),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = String.format(Locale.US, "%,d", item.count),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = if (isPending) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isPending) AlertCrimsonDark else MaterialTheme.colorScheme.onSurface
                                ),
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.23f)
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f%%", item.percentage),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = if (isPending) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isPending) AlertCrimsonDark else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.23f)
                            )
                        }
                    }
                }

                // Data Health Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HealthMiniCard("DATABASE ROWS", String.format(Locale.US, "%,d", dbRowsTotal), Modifier.weight(1f))
                    HealthMiniCard("ERRORS REPAIRED", "$formulaErrorsFixed FIXED", Modifier.weight(1f))
                    HealthMiniCard("LAST VERIFIED", lastVerifiedTime.take(12), Modifier.weight(1f))
                }
            }
        }

        // Panel 2: PENDING vs RESTORED OF THE DAY
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "PENDING VS RESTORED OF THE DAY ($selectedRestoredDayLabel)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
                val totalDay = (pendingForDayChart + restoredOfDayCount).coerceAtLeast(1)
                val pShare = (pendingForDayChart.toDouble() / totalDay.toDouble()) * 100.0
                val rShare = 100.0 - pShare
                val dayItems = listOf(
                    IssueCountItem("PENDING", pendingForDayChart, pShare),
                    IssueCountItem("RESTORED ($selectedRestoredDayLabel)", restoredOfDayCount, rShare)
                )
                NocDonutChartWithLegend(
                    items = dayItems,
                    centerSmallLabel = "PENDING SHARE",
                    centerBigValue = String.format(Locale.US, "%.1f%%", pShare),
                    palette = listOf(AlertCrimson, BrandTeal)
                )
            }
        }
    }
}

@Composable
private fun HealthMiniCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NapDownSpreadsheetPanel(
    napRows: List<CleanedNapDownRow>,
    pivotRows: List<NapDownPivotSummaryRow>,
    selectedProvince: String,
    selectedMunicipality: String,
    selectedBarangay: String,
    searchQuery: String,
    provinceOptions: List<String>,
    municipalityOptions: List<String>,
    barangayOptions: List<String>,
    onProvinceSelect: (String) -> Unit,
    onMunicipalitySelect: (String) -> Unit,
    onBarangaySelect: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "NAP DOWN SHEET • LIVE SPREADSHEET LAYOUT",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Mirrors the Google Sheets NAP DOWN tab (Row 13 Header) & matches PENDING Final Status count.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // NAP DOWN Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NocFilterDropdown(
                        label = "PROVINCE",
                        selectedValue = selectedProvince,
                        displayValue = { if (it == "ALL") "All provinces" else it },
                        options = provinceOptions,
                        onSelect = onProvinceSelect,
                        testTagName = "nap_filter_province",
                        modifier = Modifier.weight(1f)
                    )
                    NocFilterDropdown(
                        label = "MUNICIPALITY",
                        selectedValue = selectedMunicipality,
                        displayValue = { if (it == "ALL") "All municipalities" else it },
                        options = municipalityOptions,
                        onSelect = onMunicipalitySelect,
                        testTagName = "nap_filter_municipality",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    NocFilterDropdown(
                        label = "BARANGAY",
                        selectedValue = selectedBarangay,
                        displayValue = { if (it == "ALL") "All barangays" else it },
                        options = barangayOptions,
                        onSelect = onBarangaySelect,
                        testTagName = "nap_filter_barangay",
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = onClearFilters,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("nap_clear_filters_button")
                    ) {
                        Text(
                            text = "CLEAR FILTERS",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                }
            }

            // NAP DOWN Count Bar (Exact match to `.nap-countbar`)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandTealSoft)
                    .border(1.dp, BrandTealLine, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "NAP DOWN",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = BrandTealDeep,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text(
                        text = String.format(Locale.US, "%,d", napRows.size),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandTealDeep
                        ),
                        modifier = Modifier.testTag("nap_down_count_value")
                    )
                }
                Text(
                    text = "Excludes blank Province/Findings • Sorted Province A–Z • Ageing High → Low",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 9.5.sp,
                        color = BrandTealDeep.copy(alpha = 0.8f)
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }

            // 1. NAP DOWN Pivot Summary Table (Province | Findings | Count)
            Text(
                text = "NAP DOWN SUMMARY PIVOT (PROVINCE × FINDINGS)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandTealSoft)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text("PROVINCE", style = MaterialTheme.typography.labelMedium.copy(color = BrandTealDeep), modifier = Modifier.weight(0.32f))
                    Text("FINDINGS", style = MaterialTheme.typography.labelMedium.copy(color = BrandTealDeep), modifier = Modifier.weight(0.50f))
                    Text("COUNT", style = MaterialTheme.typography.labelMedium.copy(color = BrandTealDeep), modifier = Modifier.weight(0.18f))
                }
                if (pivotRows.isEmpty()) {
                    Text(
                        text = "No NAP DOWN summary rows match the current filters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    pivotRows.forEach { row ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = row.province,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(0.32f)
                            )
                            Text(
                                text = row.finding,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(0.50f)
                            )
                            Text(
                                text = String.format(Locale.US, "%,d", row.count),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                modifier = Modifier.weight(0.18f)
                            )
                        }
                    }
                }
            }

            // 2. Full 8-Column NAP DOWN Spreadsheet Table
            Text(
                text = "FULL NAP DOWN SPREADSHEET TABLE (${napRows.size} ACTIVE OUTAGES)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val hScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(hScroll)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier
                        .background(BrandTealSoft)
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    NapColHeader("PROVINCE", 125)
                    NapColHeader("MUNICIPALITY", 130)
                    NapColHeader("BARANGAY", 110)
                    NapColHeader("NAP CODE", 155)
                    NapColHeader("COORDINATES", 155)
                    NapColHeader("DATE ENDORSED", 135)
                    NapColHeader("DURATION", 125)
                    NapColHeader("FINDINGS", 150)
                }

                if (napRows.isEmpty()) {
                    Text(
                        text = "No NAP DOWN rows match the current filters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    napRows.forEach { r ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        val durColor = when {
                            r.durationHours >= 96.0 -> AlertCrimson
                            r.durationHours >= 24.0 -> StatusAmber
                            else -> BrandTealDeep
                        }
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NapColCell(r.province.ifEmpty { "—" }, 125, bold = true)
                            NapColCell(r.municipality.ifEmpty { "—" }, 130)
                            NapColCell(r.barangay.ifEmpty { "—" }, 110)
                            NapColCell(r.napCode.ifEmpty { "—" }, 155, mono = true, bold = true)
                            NapColCell(r.coordinates.ifEmpty { "—" }, 155, mono = true)
                            NapColCell(r.dateEndorsedDisplay.ifEmpty { "—" }, 135, mono = true)
                            Text(
                                text = r.duration.ifEmpty { "—" },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = durColor
                                ),
                                modifier = Modifier
                                    .width(125.dp)
                                    .padding(end = 8.dp)
                            )
                            NapColCell(r.finding.ifEmpty { "—" }, 150, bold = true)
                        }
                    }
                }
            }

            Text(
                text = "Displayed fields are read from the NAP DOWN sheet and verified against DATABASE PENDING Final Status.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NapColHeader(text: String, widthDp: Int) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 9.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BrandTealDeep
        ),
        modifier = Modifier
            .width(widthDp.dp)
            .padding(end = 8.dp)
    )
}

@Composable
private fun NapColCell(
    text: String,
    widthDp: Int,
    bold: Boolean = false,
    mono: Boolean = false
) {
    Text(
        text = text,
        style = if (mono) {
            MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
            )
        } else {
            MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
            )
        },
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .width(widthDp.dp)
            .padding(end = 8.dp)
    )
}
