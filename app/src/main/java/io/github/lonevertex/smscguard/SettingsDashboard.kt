package io.github.lonevertex.smscguard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.lonevertex.smscguard.ui.components.DiagnosticsSection
import io.github.lonevertex.smscguard.ui.components.SlotRoutingCard
import io.github.lonevertex.smscguard.ui.components.StatusHeaderCard
import io.github.lonevertex.smscguard.ui.components.TargetScopeSection
import io.github.lonevertex.smscguard.ui.theme.ElectricTeal
import io.github.lonevertex.smscguard.ui.theme.WarmAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDashboard(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.status) {
        state.status?.let { status ->
            val message = when (status) {
                is UiStatus.ResourceMessage -> context.getString(status.resId)
                is UiStatus.TextMessage -> status.message
            }
            snackbarHostState.showSnackbar(message)
            viewModel.dismissStatus()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "LSPosed Dual-SIM SMSC Guard",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.runSelfCheck() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Self-Check")
                        }

                        Button(
                            onClick = { viewModel.saveSettings() },
                            enabled = !state.isSaving,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving…")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.settings_save))
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Status Header Card
            item {
                StatusHeaderCard(
                    version = state.appVersion,
                    isLsposedBound = state.isLsposedBound,
                    frameworkInfo = state.frameworkInfo,
                    isManaged = state.lsposedManagedPreferences,
                    isReady = state.isReady
                )
            }

            // Dual SIM Slot Routing Header
            item {
                Text(
                    text = stringResource(R.string.settings_section_routing),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.settings_section_routing_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Slot 0 Card (Primary / Vodafone Egypt)
            item {
                SlotRoutingCard(
                    slotTitle = stringResource(R.string.sim_slot_0_badge),
                    carrierName = stringResource(R.string.carrier_vodafone_egypt),
                    accentColor = ElectricTeal,
                    smscValue = state.primarySmsc,
                    isValid = state.isPrimaryValid,
                    onSmscChanged = { viewModel.onPrimarySmscChanged(it) },
                    onReset = { viewModel.resetPrimaryToDefault() }
                )
            }

            // Slot 1 Card (Secondary / Orange Egypt)
            item {
                SlotRoutingCard(
                    slotTitle = stringResource(R.string.sim_slot_1_badge),
                    carrierName = stringResource(R.string.carrier_orange_egypt),
                    accentColor = WarmAmber,
                    smscValue = state.secondarySmsc,
                    isValid = state.isSecondaryValid,
                    onSmscChanged = { viewModel.onSecondarySmscChanged(it) },
                    onReset = { viewModel.resetSecondaryToDefault() }
                )
            }

            // Target Scope Section
            item {
                TargetScopeSection(
                    targetPackages = state.targetPackages,
                    isDefaultScope = state.isDefaultScopeActive,
                    customInput = state.customPackageInput,
                    isCustomValid = state.isCustomPackageValid,
                    onCustomInputChanged = { viewModel.onCustomPackageInputChanged(it) },
                    onAddCustomPackage = { viewModel.addCustomPackage() },
                    onTogglePackage = { viewModel.toggleTargetPackage(it) }
                )
            }

            // Diagnostics & Security Section
            item {
                DiagnosticsSection(
                    diagnosticsEnabled = state.diagnosticsEnabled,
                    onDiagnosticsChanged = { viewModel.onDiagnosticsChanged(it) },
                    recentDecision = state.sanitizedRoutingDecision,
                    onSimulateTest = { viewModel.triggerSanitizedRoutingTest() }
                )
            }

            // Accessibility Live Status Announcement Region
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_sim_mapping_help),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
