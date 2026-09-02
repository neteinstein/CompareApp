package org.neteinstein.compareapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.neteinstein.compareapp.R
import org.neteinstein.compareapp.utils.FoodDeliveryProvider

/**
 * Stateful entry point: wires [SettingsViewModel] to [SettingsScreen].
 *
 * [onChangeLanguageClicked] is `null` below API 33 - there is no system per-app-language screen to
 * deep-link into on older versions - so [SettingsScreen] hides the whole Language section in that
 * case.
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenBoltLinkLab: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val onChangeLanguageClicked: (() -> Unit)? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        {
            context.startActivity(
                Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            )
        }
    } else {
        null
    }

    LaunchedEffect(Unit) {
        viewModel.onScreenEntered()
    }

    SettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onTitleClicked = viewModel::onTitleClicked,
        onUpdateClicked = viewModel::onUpdateClicked,
        onEnableSideloadingClicked = viewModel::onEnableSideloadingClicked,
        onChangeLanguageClicked = onChangeLanguageClicked,
        onFoodProviderToggled = viewModel::onFoodProviderToggled,
        onOpenBoltLinkLab = onOpenBoltLinkLab,
        modifier = modifier
    )
}

/**
 * Stateless, preview-friendly screen: title bar up top, a scrollable body ordered
 * Language -> Comparison configuration -> Updates -> (hidden) Diagnostics. Language is omitted
 * entirely when [onChangeLanguageClicked] is `null` (below API 33 - see [SettingsRoute]).
 * Diagnostics only appears once [uiState].diagnosticsUnlocked is true, which [onTitleClicked]
 * (wired to the title text below) works towards - see [SettingsViewModel.onTitleClicked].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onTitleClicked: () -> Unit = {},
    onUpdateClicked: () -> Unit = {},
    onEnableSideloadingClicked: () -> Unit = {},
    onChangeLanguageClicked: (() -> Unit)? = null,
    onFoodProviderToggled: (FoodDeliveryProvider) -> Unit = {},
    onOpenBoltLinkLab: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        modifier = Modifier.clickable(onClick = onTitleClicked)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back)
                        )
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
                .padding(16.dp)
        ) {
            if (onChangeLanguageClicked != null) {
                Text(
                    text = stringResource(R.string.settings_language_section_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                LanguageSection(onChangeLanguageClicked = onChangeLanguageClicked)
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = stringResource(R.string.settings_comparison_section_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            ComparisonConfigurationSection(
                selectedFoodProviders = uiState.selectedFoodProviders,
                onFoodProviderToggled = onFoodProviderToggled
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.settings_update_section_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            UpdateSection(
                status = uiState.updateStatus,
                onUpdateClicked = onUpdateClicked,
                onEnableSideloadingClicked = onEnableSideloadingClicked
            )

            if (uiState.diagnosticsUnlocked) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Diagnostics",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Try out candidate Bolt deep-link formats against the installed Bolt app.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TextButton(onClick = onOpenBoltLinkLab) {
                        Text(text = "Open Bolt Link Lab")
                    }
                }
            }
        }
    }
}

/**
 * Which pair of food delivery apps "Search Food" compares - see
 * [SettingsViewModel.onFoodProviderToggled] for the exactly-2 swap behavior each checkbox drives.
 */
@Composable
private fun ComparisonConfigurationSection(
    selectedFoodProviders: List<FoodDeliveryProvider>,
    onFoodProviderToggled: (FoodDeliveryProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_comparison_food_subtitle),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.settings_comparison_food_info),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        FoodDeliveryProvider.entries.forEach { provider ->
            val isSelected = provider in selectedFoodProviders
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFoodProviderToggled(provider) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = isSelected, onCheckedChange = { onFoodProviderToggled(provider) })
                Text(text = provider.displayName, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** Info text + button deep-linking into the system per-app language screen (see [SettingsRoute]). */
@Composable
private fun LanguageSection(
    onChangeLanguageClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_language_info),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(onClick = onChangeLanguageClicked) {
            Text(text = stringResource(R.string.settings_language_button))
        }
    }
}

/**
 * Button + live status for [UpdateStatus] - checks GitHub Releases and, if allowed, downloads and
 * installs a newer build (see [SettingsViewModel.onUpdateClicked]). [KeepScreenOnWhile] keeps the
 * screen awake while a check/download is in flight - both run on a plain coroutine, not a
 * WorkManager job, so nothing here survives the app leaving the foreground; keeping the screen on
 * for that whole stretch is what actually prevents that, right up through the moment
 * [SettingsViewModel] fires the install prompt.
 */
@Composable
private fun UpdateSection(
    status: UpdateStatus,
    onUpdateClicked: () -> Unit,
    onEnableSideloadingClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val isBusy = status is UpdateStatus.Checking || status is UpdateStatus.Downloading
        KeepScreenOnWhile(keepOn = isBusy)
        Text(
            text = stringResource(R.string.settings_update_info),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(onClick = onUpdateClicked, enabled = !isBusy) {
            Text(text = stringResource(R.string.settings_update_button))
        }

        when (status) {
            is UpdateStatus.Idle -> Unit
            is UpdateStatus.Checking -> UpdateStatusRow(text = stringResource(R.string.settings_update_checking))
            is UpdateStatus.Downloading -> UpdateStatusRow(text = stringResource(R.string.settings_update_downloading))
            is UpdateStatus.UpToDate ->
                Text(
                    text = stringResource(R.string.settings_update_up_to_date, status.currentVersionName),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            is UpdateStatus.UpdateAvailable ->
                Text(
                    text = stringResource(R.string.settings_update_available, status.update.versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            is UpdateStatus.Failed ->
                Text(
                    text = stringResource(R.string.settings_update_failed, status.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            is UpdateStatus.SideloadingBlocked ->
                SideloadingWarning(onActionClick = onEnableSideloadingClicked)
        }
    }
}

@Composable
private fun SideloadingWarning(
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_update_sideloading_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onActionClick) {
                Text(text = stringResource(R.string.settings_update_enable_sideloading_cta))
            }
        }
    }
}

@Composable
private fun UpdateStatusRow(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp)
        Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp))
    }
}

/**
 * Sets [android.view.View.setKeepScreenOn] for as long as [keepOn] is true, and always clears it
 * on dispose - including when this leaves composition entirely (e.g. the user backs out of
 * Settings mid-download) - so the flag can never get stuck on past the update flow that requested
 * it.
 */
@Composable
private fun KeepScreenOnWhile(keepOn: Boolean) {
    val view = LocalView.current
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }
}
