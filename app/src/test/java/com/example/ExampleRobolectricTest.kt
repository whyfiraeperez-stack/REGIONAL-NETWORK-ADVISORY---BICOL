package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.agent.DataIntegrityAgent
import com.example.data.DatabaseRowDto
import com.example.data.NapDownRowDto
import com.example.data.OutageSnapshotResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Bicol NAP Monitor", appName)
    }

    @Test
    fun `agent repairs formula errors and verifies pending equals nap down parity`() {
        val sampleSnapshot = OutageSnapshotResponse(
            ok = true,
            syncState = "SYNCED",
            rows = listOf(
                DatabaseRowDto(
                    rowNumber = 1,
                    province = "ALBAY",
                    municipality = "OAS",
                    barangay = "#REF!",
                    finding = "HIGH LOSS",
                    finalStatus = "PENDING"
                ),
                DatabaseRowDto(
                    rowNumber = 2,
                    province = "CAMARINES SUR",
                    municipality = "NAGA CITY",
                    barangay = "#REF!",
                    finding = "13.63656,123.208013",
                    napStatus = "NAP NO POWER",
                    finalStatus = "RESTORED"
                )
            ),
            napDownRows = listOf(
                NapDownRowDto(
                    sourceRow = 14,
                    province = "ALBAY",
                    municipality = "OAS",
                    napCode = "OSA001 LP55 NP7-8",
                    duration = "1 days 3 hours",
                    finding = "HIGH LOSS"
                ),
                // Ghost row 1000 with blank province/finding
                NapDownRowDto(
                    sourceRow = 1000,
                    province = "",
                    municipality = "",
                    finding = ""
                )
            )
        )

        val processed = DataIntegrityAgent.inspectAndRepair(sampleSnapshot, isLiveConnection = true)
        assertEquals(1, processed.agentHealth.pendingFinalStatusCount)
        assertEquals(1, processed.agentHealth.validNapDownCount)
        assertTrue(processed.agentHealth.parityVerified)
        assertEquals("", processed.cleanedDbRows[0].barangay)
        assertEquals("NAP NO POWER", processed.cleanedDbRows[1].finding)
        assertEquals(27.0, processed.cleanedNapDownRows[0].durationHours, 0.01)
    }
}
