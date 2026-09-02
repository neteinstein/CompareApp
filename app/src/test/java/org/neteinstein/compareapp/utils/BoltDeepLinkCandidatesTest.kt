package org.neteinstein.compareapp.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies [BoltDeepLinkCandidates.build] produces well-formed, distinct candidates with
 * consistent 6-decimal coordinate formatting - the actual "which one Bolt honors" question can
 * only be answered on-device via [org.neteinstein.compareapp.ui.screens.BoltLinkLabScreen].
 */
class BoltDeepLinkCandidatesTest {

    @Test
    fun testBuild_returnsCandidatesWithUniqueIdsAndNonBlankLabels() {
        val candidates = BoltDeepLinkCandidates.build(
            pickupLat = 40.758896,
            pickupLng = -73.985130,
            destLat = 40.785091,
            destLng = -73.968285
        )

        assertTrue("Should produce at least one candidate", candidates.isNotEmpty())
        assertEquals(
            "Candidate ids should be unique",
            candidates.size,
            candidates.map { it.id }.distinct().size
        )
        candidates.forEach { candidate ->
            assertTrue("Label should not be blank", candidate.label.isNotBlank())
            assertTrue("Web URI should not be blank", candidate.webUri.isNotBlank())
        }
    }

    @Test
    fun testBuild_formatsCoordinatesWithExactly6DecimalPlaces() {
        val candidates = BoltDeepLinkCandidates.build(
            pickupLat = 40.123456789,
            pickupLng = -73.987654321,
            destLat = 41.111111111,
            destLng = -74.999999999
        )

        val baseline = candidates.first { it.id == 1 }
        assertTrue(baseline.nativeUri!!.contains("pickup_lat=40.123457"))
        assertTrue(baseline.nativeUri.contains("pickup_lng=-73.987654"))
        assertTrue(baseline.nativeUri.contains("destination_lat=41.111111"))
        assertTrue(baseline.nativeUri.contains("destination_lng=-75.000000"))
        assertTrue(baseline.webUri.contains("pickup_lat=40.123457"))
    }

    @Test
    fun testBuild_dropoffNamingCandidateUsesDropoffLatAndDropoffLng() {
        val candidates = BoltDeepLinkCandidates.build(
            pickupLat = 40.0,
            pickupLng = -73.0,
            destLat = 41.0,
            destLng = -74.0
        )

        val dropoffCandidate = candidates.first { it.id == 2 }
        assertTrue(dropoffCandidate.nativeUri!!.contains("dropoff_lat=41.000000"))
        assertTrue(dropoffCandidate.nativeUri.contains("dropoff_lng=-74.000000"))
    }

    @Test
    fun testBuild_baselineCandidateUsesTheVerifiedActionHost() {
        val candidates = BoltDeepLinkCandidates.build(
            pickupLat = 40.0,
            pickupLng = -73.0,
            destLat = 41.0,
            destLng = -74.0
        )

        val baseline = candidates.first { it.id == 1 }
        assertTrue(baseline.nativeUri!!.startsWith("bolt://action?"))
    }

    @Test
    fun testBuild_actionTypeCandidateIncludesActionTypeParam() {
        val candidates = BoltDeepLinkCandidates.build(
            pickupLat = 40.0,
            pickupLng = -73.0,
            destLat = 41.0,
            destLng = -74.0
        )

        val actionTypeCandidate = candidates.first { it.id == 3 }
        assertTrue(actionTypeCandidate.nativeUri!!.startsWith("bolt://action?"))
        assertTrue(actionTypeCandidate.nativeUri.contains("action_type=order_ride"))
    }

    @Test
    fun testBuild_rideHostBaselineCandidateIsKeptForComparisonOnly() {
        val candidates = BoltDeepLinkCandidates.build(
            pickupLat = 40.0,
            pickupLng = -73.0,
            destLat = 41.0,
            destLng = -74.0
        )

        val rideHostCandidate = candidates.first { it.id == 7 }
        assertTrue(rideHostCandidate.nativeUri!!.startsWith("bolt://ride?"))
    }
}
