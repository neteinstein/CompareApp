package org.neteinstein.compareapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.neteinstein.compareapp.launchBoltWithFallback
import org.neteinstein.compareapp.openBoltWebLink
import org.neteinstein.compareapp.utils.BoltLinkCandidate

private const val BOLT_PACKAGE = "ee.mtakso.client"
private const val LAUNCH_DELAY_MS = 500L

@Composable
fun BoltLinkLabRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BoltLinkLabViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    BoltLinkLabScreen(
        uiState = uiState,
        onBack = onBack,
        onPickupChange = viewModel::updatePickup,
        onDropoffChange = viewModel::updateDropoff,
        onBuildCandidates = viewModel::buildCandidates,
        modifier = modifier
    )
}

/**
 * Diagnostic screen: Bolt publishes no deep-link API, so [BoltLinkCandidate] is a list of
 * unverified guesses at the URL format that sets a ride's pickup/destination. This screen lets
 * someone type a real pickup/dropoff, then fire each candidate at the actual installed Bolt app
 * one at a time - whichever one actually lands with the destination set is the one to keep;
 * see [org.neteinstein.compareapp.utils.BoltDeepLinkCandidates].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoltLinkLabScreen(
    uiState: BoltLinkLabUiState,
    onBack: () -> Unit,
    onPickupChange: (String) -> Unit,
    onDropoffChange: (String) -> Unit,
    onBuildCandidates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Bolt Link Lab") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Bolt has no published deep-link API, so the exact URL format that sets a " +
                    "ride's destination is unverified. Enter a real pickup and dropoff below, build " +
                    "the candidates, then tap Launch on each one - whichever candidate actually opens " +
                    "Bolt with the destination set is the one to report back.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = uiState.pickup,
                onValueChange = onPickupChange,
                label = { Text("Pickup address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.dropoff,
                onValueChange = onDropoffChange,
                label = { Text("Dropoff address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = onBuildCandidates, enabled = !uiState.isLoading, modifier = Modifier.fillMaxWidth()) {
                Text(text = if (uiState.isLoading) "Geocoding..." else "Build candidates")
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
            }

            uiState.error?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            uiState.candidates.forEach { candidate ->
                CandidateCard(
                    candidate = candidate,
                    onLaunch = {
                        scope.launch {
                            launchCandidate(context = context, candidate = candidate)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CandidateCard(
    candidate: BoltLinkCandidate,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Candidate ${candidate.id}: ${candidate.label}",
                style = MaterialTheme.typography.titleSmall
            )
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    candidate.nativeUri?.let { uri ->
                        Text(text = uri, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(text = candidate.webUri, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = onLaunch, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Launch")
            }
        }
    }
}

/**
 * Fires one [BoltLinkCandidate] the same way the real app does: the native `bolt://` intent first
 * (when the candidate has one), a delay, then the HTTPS web link targeted at Bolt's package with a
 * browser fallback - see [launchBoltWithFallback] and [openBoltWebLink] in MainActivity, reused
 * here so a candidate that "works in the lab" behaves identically once promoted into the real flow.
 */
private suspend fun launchCandidate(context: Context, candidate: BoltLinkCandidate) {
    fun startExplicit(uri: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setPackage(BOLT_PACKAGE)
        context.startActivity(intent)
    }

    fun startBrowser(uri: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    try {
        val nativeUri = candidate.nativeUri
        if (nativeUri != null) {
            launchBoltWithFallback(
                startNative = { startExplicit(nativeUri) },
                startWeb = {
                    openBoltWebLink(
                        startWebApp = { startExplicit(candidate.webUri) },
                        startWebBrowser = { startBrowser(candidate.webUri) }
                    )
                },
                delayMs = LAUNCH_DELAY_MS
            )
        } else {
            openBoltWebLink(
                startWebApp = { startExplicit(candidate.webUri) },
                startWebBrowser = { startBrowser(candidate.webUri) }
            )
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Could not launch candidate ${candidate.id}: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
