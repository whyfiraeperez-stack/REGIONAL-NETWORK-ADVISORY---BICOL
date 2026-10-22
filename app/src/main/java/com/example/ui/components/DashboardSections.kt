package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.AgingBucket
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun ProvincePendingSectionView(
    groups: List<ProvincePendingGroup>,
    expandedProvinces: Set<String>,
    onToggleProvince: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "PENDING BY PROVINCE",
                style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.4.sp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Each Bicol province is separated with its issue breakdown and live pending-record details (sorted oldest ageing first).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (groups.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No pending province records match the current filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return
        }

        groups.forEach { group ->
            val isExpanded = expandedProvinces.contains(group.province)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .testTag("province_card_${group.province.lowercase(Locale.US)}")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                            .clickable { onToggleProvince(group.province) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = BrandTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = group.province,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = AlertCrimsonSoft,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.border(1.dp, AlertCrimsonBorder, RoundedCornerShape(999.dp))
                            ) {
                                Text(
                                    text = "${group.pendingCount} PENDING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AlertCrimsonDark
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "ISSUE BREAKDOWN (${group.province})",
                                style = MaterialTheme.typography.labelMedium,
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
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text("ISSUE", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.6f))
                                    Text("COUNT", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.2f))
                                    Text("SHARE", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.2f))
                                }
                                group.issueSummary.forEach { issue ->
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = issue.label,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            modifier = Modifier.weight(0.6f)
                                        )
                                        Text(
                                            text = "${issue.count}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.weight(0.2f)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1f%%", issue.percentage),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(0.2f)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "LIVE PENDING RECORDS (${group.detailRows.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    HeaderCell("MUNICIPALITY", 130)
                                    HeaderCell("BARANGAY", 110)
                                    HeaderCell("NAP CODE", 150)
                                    HeaderCell("NAP / ISSUE", 150)
                                    HeaderCell("TECHNICIAN", 95)
                                    HeaderCell("DATE ENDORSED", 115)
                                    HeaderCell("AGE DAYS", 85)
                                    HeaderCell("ETR", 100)
                                    HeaderCell("RFO", 130)
                                }
                                group.detailRows.forEach { r ->
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    val ageVal = r.ageDays ?: 0.0
                                    val ageColor = when {
                                        ageVal >= 4.0 -> AlertCrimson
                                        ageVal > 1.0 -> StatusAmber
                                        else -> StatusGreen
                                    }
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BodyCell(r.municipality.ifEmpty { "—" }, 130, bold = true)
                                        BodyCell(r.barangay.ifEmpty { "—" }, 110)
                                        BodyCell(r.facility.ifEmpty { "—" }, 150, mono = true)
                                        BodyCell(r.napStatus.ifEmpty { r.concern.ifEmpty { "—" } }, 150)
                                        BodyCell(r.ospTeam.ifEmpty { "—" }, 95)
                                        BodyCell(r.dateEndorsedDayKey.ifEmpty { "—" }, 115, mono = true)
                                        Text(
                                            text = if (r.ageDays != null) String.format(Locale.US, "%.1fd", r.ageDays) else "—",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = ageColor,
                                            modifier = Modifier.width(85.dp)
                                        )
                                        BodyCell(r.etr.ifEmpty { "—" }, 100)
                                        BodyCell(r.rfo.ifEmpty { "—" }, 130)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, widthDp: Int) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.5.sp),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .width(widthDp.dp)
            .padding(end = 8.dp)
    )
}

@Composable
private fun BodyCell(
    text: String,
    widthDp: Int,
    bold: Boolean = false,
    mono: Boolean = false
) {
    Text(
        text = text,
        style = if (mono) {
            MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily)
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

@Composable
fun OperationsDetailsSectionView(
    pendingByMunicipality: List<IssueCountItem>,
    pendingByIssue: List<IssueCountItem>,
    technicianWorkload: List<TechnicianWorkloadItem>,
    agingBuckets: List<AgingBucket>,
    restoredByTechnician: List<IssueCountItem>,
    restoredByMunicipality: List<IssueCountItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "OPERATIONS DETAILS • 6 ANALYTICAL TABLES",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Real-time operational breakdown derived directly from filtered DATABASE rows.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnalyticalCardFrame(title = "1. AGING MONITOR (PENDING SLA)") {
            agingBuckets.forEach { bucket ->
                val dotColor = when (bucket.severityColor) {
                    "RED" -> AlertCrimson
                    "AMBER" -> StatusAmber
                    else -> StatusGreen
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(0.6f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%,d", bucket.count),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.weight(0.2f)
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%%", bucket.percentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.2f)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }
        }

        AnalyticalCardFrame(title = "2. TECHNICIAN WORKLOAD (ACTIVE PENDING & OLDEST AGE)") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text("TECHNICIAN / OSP TEAM", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.55f))
                Text("ACTIVE", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.2f))
                Text("OLDEST DAYS", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.25f))
            }
            technicianWorkload.forEach { tech ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tech.technician,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(0.55f)
                    )
                    Text(
                        text = "${tech.activeCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AlertCrimson,
                        modifier = Modifier.weight(0.2f)
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f d", tech.oldestDays),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(0.25f)
                    )
                }
            }
        }

        StandardIssueCountTableCard(
            title = "3. PENDING BY ISSUE / FINDING",
            colHeader = "ISSUE",
            items = pendingByIssue
        )

        StandardIssueCountTableCard(
            title = "4. PENDING BY MUNICIPALITY",
            colHeader = "MUNICIPALITY",
            items = pendingByMunicipality
        )

        StandardIssueCountTableCard(
            title = "5. RESTORED BY TECHNICIAN",
            colHeader = "TECHNICIAN",
            items = restoredByTechnician
        )

        StandardIssueCountTableCard(
            title = "6. RESTORED BY MUNICIPALITY (TOP 25)",
            colHeader = "MUNICIPALITY",
            items = restoredByMunicipality.take(25)
        )
    }
}

@Composable
private fun AnalyticalCardFrame(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            content()
        }
    }
}

@Composable
private fun StandardIssueCountTableCard(
    title: String,
    colHeader: String,
    items: List<IssueCountItem>
) {
    AnalyticalCardFrame(title = title) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(colHeader, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.6f))
            Text("COUNT", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.2f))
            Text("SHARE", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.2f))
        }
        if (items.isEmpty()) {
            Text(
                text = "No matching rows.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            items.forEach { item ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.weight(0.6f)
                    )
                    Text(
                        text = String.format(Locale.US, "%,d", item.count),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(0.2f)
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%%", item.percentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.2f)
                    )
                }
            }
        }
    }
}

@Composable
fun AgentAndFormulasSectionView(
    health: AgentHealthSummary?,
    onRunVerificationNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BrandTealDeep),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF99F6E4),
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "SELF-HEALING SPREADSHEET AGENT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            Text(
                                text = "Live Formula Inspector • Error Sanitizer • Parity Verifier",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB4F0EC))
                            )
                        }
                    }

                    Button(
                        onClick = onRunVerificationNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandTeal,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("run_agent_audit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RE-VERIFY NOW", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (health != null) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AgentMetricMini("DB ROWS SCANNED", String.format(Locale.US, "%,d", health.totalDbRowsScanned))
                        AgentMetricMini("PENDING = NAP DOWN", "${health.pendingFinalStatusCount} == ${health.validNapDownCount}")
                        AgentMetricMini("#REF! REPAIRED", "${health.formulaErrorsRepaired}")
                        AgentMetricMini("GHOST ROWS DROPPED", "${health.blankNapGhostRowsDropped}")
                    }
                }
            }
        }

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
                    text = "EXACT SPREADSHEET FORMULAS DISCOVERED & ENFORCED",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )

                FormulaItemRow(
                    title = "1. NAP DOWN Count == Pending Final Status Formula",
                    explanation = "Every active row in the NAP DOWN tab (starting at Row 14) is dynamically pulled from DATABASE rows where Column Q (FINAL STATUS) equals \"PENDING\". Row 1000 in the NAP DOWN sheet is an unpopulated template row and must be excluded.",
                    formulaCode = "=COUNTIFS(DATABASE!Q3:Q, \"PENDING\")  // Equals 62\n=COUNTIFS('NAP DOWN'!B14:B1000, \"<>\", 'NAP DOWN'!I14:I1000, \"<>\")  // Equals 62"
                )

                FormulaItemRow(
                    title = "2. NAP DOWN Sheet Extraction & Sorting Formula",
                    explanation = "Extracts active pending outages and sorts by Province (A–Z), Municipality (A–Z), Barangay (A–Z), Duration Hours (High → Low), and NAP Code (A–Z).",
                    formulaCode = "=SORT(FILTER({DATABASE!D3:F, DATABASE!G3:G, DATABASE!N3:N, NOW()-DATABASE!N3:N, DATABASE!I3:I}, DATABASE!Q3:Q=\"PENDING\"), 1, TRUE, 2, TRUE, 7, FALSE)"
                )

                FormulaItemRow(
                    title = "3. Pending Rate & Daily Restoration Share Formula",
                    explanation = "Calculates percentage of active pending tickets against total filtered records, and compares active pending tickets against restorations completed on the selected Manila date.",
                    formulaCode = "Pending Rate = COUNTIF(FilteredDB[FINAL_STATUS], \"PENDING\") / COUNTA(FilteredDB[ROW])\nPending vs Restored Share = Pending / (Pending + COUNTIFS(DATABASE!Q:Q, \"RESTORED\", INT(DATABASE!O:O), SelectedDate))"
                )

                FormulaItemRow(
                    title = "4. Ageing SLA & Turnaround Hours Formula",
                    explanation = "Categorizes pending tickets into NEW (0–1 Day), AGING (2–3 Days), and CRITICAL (4+ Days) based on Manila timezone elapsed time.",
                    formulaCode = "AgeDays = MAX(0, (IF(ISBLANK(DateRestored), NOW(), DateRestored) - DateEndorsed))\nSLA_Bucket = IF(AgeDays <= 1, \"NEW (0-1d)\", IF(AgeDays < 4, \"AGING (2-3d)\", \"CRITICAL (4+d)\"))"
                )
            }
        }

        Text(
            text = "ACTIVE AGENT DIAGNOSTICS & REPAIR LOGS",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary
        )

        health?.repairLogs?.forEach { log ->
            val badgeBg = if (log.severity == "VERIFIED") StatusGreenSoft else BrandTealSoft
            val badgeFg = if (log.severity == "VERIFIED") StatusGreen else BrandTealDeep
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = log.category,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = badgeBg,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = "${log.severity} • ${log.affectedCount}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = badgeFg,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = log.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = log.formulaUsed,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    copyToClipboard(context, "Formula", log.formulaUsed)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Copy formula",
                                    tint = BrandTeal,
                                    modifier = Modifier.size(15.dp)
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
private fun AgentMetricMini(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 8.5.sp, color = Color(0xFF99F6E4))
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        )
    }
}

@Composable
private fun FormulaItemRow(
    title: String,
    explanation: String,
    formulaCode: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formulaCode,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
            color = BrandTealDeep,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(BrandTealSoft)
                .padding(8.dp)
        )
    }
}

@Composable
fun ExportVercelCodeSectionView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vercelSnippet = remember {
        """
// Vercel / GitHub Ready Self-Healing Snapshot Agent & Left-Tab Layout
// File: api/snapshot-agent.js
export function cleanAndVerifyOutageSnapshot(rawSnapshot) {
  const FORMULA_ERR = /(#REF!|#VALUE!|#N\/A|#NAME\?|#ERROR!)/i;
  const COORD_IN_FINDING = /^\s*\d{1,2}\.\d+\s*[, ]\s*\d{2,3}\.\d+/;
  const clean = v => String(v ?? '').replace(/\s+/g, ' ').trim();
  const norm = v => clean(v).toUpperCase();

  let repairedCells = 0;
  const rows = (rawSnapshot.rows || []).map(r => {
    const brgy = FORMULA_ERR.test(r.barangay) ? (repairedCells++, '') : clean(r.barangay);
    const finding = COORD_IN_FINDING.test(r.finding) ? norm(r.napStatus) : norm(r.finding);
    const finalStatus = norm(r.finalStatus) || (norm(r.sourceStatus) === 'PENDING' ? 'PENDING' : 'BLANK');
    return { ...r, barangay: brgy, finding, finalStatus };
  });

  // Count NAP DOWN by counting PENDING in FINAL STATUS & excluding blank template Row 1000
  const pendingRows = rows.filter(r => r.finalStatus === 'PENDING');
  const validNapDownRows = (rawSnapshot.napDownRows || []).filter(
    r => clean(r.province) !== '' && clean(r.finding) !== ''
  );

  return {
    ...rawSnapshot,
    rows,
    napDownRows: validNapDownRows,
    pendingCount: pendingRows.length,
    napDownSheetCount: validNapDownRows.length,
    agentDiagnostics: {
      parityVerified: pendingRows.length === validNapDownRows.length,
      repairedFormulaCells: repairedCells,
      droppedBlankTemplateRows: (rawSnapshot.napDownRows || []).length - validNapDownRows.length
    }
  };
}
        """.trimIndent()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Online & GitHub / Vercel Deployment Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BrandTealSoft),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BrandTealLine, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ONLINE DEPLOYMENT & LIVE VERCEL FEED",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandTealDeep
                    )
                )
                Text(
                    text = "• Connected Live Endpoint: https://almasor-outage-monitoring.vercel.app/api/snapshot\n• To push to GitHub or export APK/AAB from AI Studio: use the Settings / Export menu in the top-right toolbar.",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrandTealDeep)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://almasor-outage-monitoring.vercel.app/"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                copyToClipboard(context, "Live URL", "https://almasor-outage-monitoring.vercel.app/")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTealDeep),
                        modifier = Modifier.testTag("open_live_vercel_url_button")
                    ) {
                        Text("OPEN LIVE VERCEL SITE", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GITHUB & VERCEL SERVERLESS AGENT CODE",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ready-to-push Vercel serverless agent & spreadsheet formula parity module for almasor-outage-monitoring.vercel.app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            copyToClipboard(context, "Vercel Agent Code", vercelSnippet)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        modifier = Modifier.testTag("copy_vercel_code_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("COPY CODE", style = MaterialTheme.typography.labelMedium)
                    }
                }

                val hScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B1717))
                        .padding(12.dp)
                        .horizontalScroll(hScroll)
                ) {
                    Text(
                        text = vercelSnippet,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = Color(0xFFD5F2F0),
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}
