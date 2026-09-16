package io.github.lonevertex.smscguard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lonevertex.smscguard.ui.theme.ElectricTeal
import io.github.lonevertex.smscguard.ui.theme.StatusSuccess
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

@Composable
fun StatusHeaderCard(
    version: String,
    isLsposedBound: Boolean,
    frameworkInfo: String?,
    isManaged: Boolean = isLsposedBound,
    isReady: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isReady) ElectricTeal else MaterialTheme.colorScheme.error)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.module_ready),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = version,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.settings_header_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time framework connection status badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isLsposedBound) StatusSuccess.copy(alpha = 0.15f) else WarmAmber.copy(alpha = 0.15f),
                modifier = Modifier.border(
                    1.dp,
                    if (isLsposedBound) StatusSuccess.copy(alpha = 0.4f) else WarmAmber.copy(alpha = 0.4f),
                    RoundedCornerShape(8.dp)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isLsposedBound) StatusSuccess else WarmAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLsposedBound) {
                            stringResource(R.string.status_lsposed_active)
                        } else {
                            stringResource(R.string.status_lsposed_pending)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLsposedBound) StatusSuccess else WarmAmber
                    )
                }
            }

            if (isLsposedBound && !frameworkInfo.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = frameworkInfo,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(6.dp)
                    )
                ) {
                    Text(
                        text = if (isLsposedBound || isManaged) {
                            stringResource(R.string.module_managed_storage)
                        } else {
                            stringResource(R.string.module_legacy_storage)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(6.dp)
                    )
                ) {
                    Text(
                        text = "Android 15 (API 36)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Deprecated("Use StatusHeaderCard", ReplaceWith("StatusHeaderCard(version, isManaged, null, isManaged, isReady, modifier)"))
@Composable
fun ModuleHealthCard(
    version: String,
    isManaged: Boolean,
    isReady: Boolean,
    modifier: Modifier = Modifier
) {
    StatusHeaderCard(
        version = version,
        isLsposedBound = isManaged,
        frameworkInfo = null,
        isManaged = isManaged,
        isReady = isReady,
        modifier = modifier
    )
}

@Composable
fun SlotRoutingCard(
    slotTitle: String,
    carrierName: String,
    accentColor: androidx.compose.ui.graphics.Color,
    smscValue: String,
    isValid: Boolean,
    onSmscChanged: (String) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SimCard,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = slotTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = carrierName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = onReset,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.reset_to_default),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = smscValue,
                onValueChange = onSmscChanged,
                label = { Text("SMSC Address (E.164)") },
                placeholder = { Text("+20100000000") },
                isError = !isValid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = if (isValid) accentColor else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                },
                supportingText = {
                    if (!isValid) {
                        Text(
                            text = "Must begin with + and contain 5–20 digits",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.settings_smsc_helper),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                trailingIcon = {
                    if (!isValid) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Invalid format",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TargetScopeSection(
    targetPackages: Set<String>,
    isDefaultScope: Boolean,
    customInput: String,
    isCustomValid: Boolean,
    onCustomInputChanged: (String) -> Unit,
    onAddCustomPackage: () -> Unit,
    onTogglePackage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_section_scope),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_scope_helper),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDefaultScope) {
                        ElectricTeal.copy(alpha = 0.15f)
                    } else {
                        WarmAmber.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = if (isDefaultScope) {
                            stringResource(R.string.default_scope_active)
                        } else {
                            stringResource(R.string.custom_scope_active)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDefaultScope) ElectricTeal else WarmAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "com.google.android.apps.messaging",
                    "com.android.mms"
                ).forEach { pkg ->
                    val isSelected = targetPackages.contains(pkg)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTogglePackage(pkg) },
                        label = {
                            Text(
                                text = if (pkg.contains("google")) "Google Messages" else "AOSP MMS",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                targetPackages.filter {
                    it != "com.google.android.apps.messaging" && it != "com.android.mms"
                }.forEach { customPkg ->
                    FilterChip(
                        selected = true,
                        onClick = { onTogglePackage(customPkg) },
                        label = {
                            Text(
                                text = customPkg,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove package",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = onCustomInputChanged,
                    label = { Text(stringResource(R.string.add_package_label)) },
                    placeholder = { Text("com.vendor.messaging") },
                    isError = !isCustomValid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAddCustomPackage() }),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onAddCustomPackage,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.add_action),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticsSection(
    diagnosticsEnabled: Boolean,
    onDiagnosticsChanged: (Boolean) -> Unit,
    recentDecision: String?,
    onSimulateTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.redacted_diagnostics_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.redacted_diagnostics_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = diagnosticsEnabled,
                    onCheckedChange = onDiagnosticsChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSimulateTest,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.simulate_route_test))
            }

            AnimatedVisibility(visible = recentDecision != null) {
                recentDecision?.let { decision ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = ElectricTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = decision,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
