package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.AdminResetSecurityStore
import com.example.data.AdminSecurityConfig
import com.example.data.AdminVerificationMethod
import com.example.data.PinVerifyResult
import com.example.firebase.verifyGoogleAdminCredential
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.OnStatusWatchContainer
import com.example.ui.theme.StatusCriticalTextDark
import com.example.ui.theme.StatusHealthyContainer
import com.example.ui.theme.StatusWatchContainer
import kotlinx.coroutines.launch

@Composable
fun AdminResetVerificationCard(
    config: AdminSecurityConfig,
    currentUserEmail: String,
    isPinVerified: Boolean,
    isGoogleVerified: Boolean,
    onPinVerified: () -> Unit,
    onGoogleVerified: () -> Unit,
    onConfigUpdated: (method: AdminVerificationMethod, pinVerifiedNow: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSettingsEditor by remember(config.isConfigured) { mutableStateOf(!config.isConfigured) }
    var draftAdminEmail by remember(config.adminEmail, currentUserEmail) {
        mutableStateOf(config.adminEmail.ifBlank { currentUserEmail })
    }
    var draftMethod by remember(config.method) { mutableStateOf(config.method) }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var setupError by remember { mutableStateOf<String?>(null) }
    var isSavingSetup by remember { mutableStateOf(false) }

    var pinEntry by remember { mutableStateOf("") }
    var verifyError by remember { mutableStateOf<String?>(null) }
    var isVerifyingGoogle by remember { mutableStateOf(false) }

    val isMethodSatisfied = (!config.requiresPin || isPinVerified) &&
        (!config.requiresGoogleReauth || isGoogleVerified)

    Surface(
        color = if (isMethodSatisfied && !showSettingsEditor) {
            StatusHealthyContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isMethodSatisfied && !showSettingsEditor) {
                ForestEmerald
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_security_verification_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isMethodSatisfied && !showSettingsEditor) {
                            Icons.Filled.VerifiedUser
                        } else {
                            Icons.Filled.AdminPanelSettings
                        },
                        contentDescription = null,
                        tint = if (isMethodSatisfied && !showSettingsEditor) {
                            OnStatusHealthyContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (!config.isConfigured) {
                            "Admin Security Setup"
                        } else if (showSettingsEditor) {
                            "Admin Verification Settings"
                        } else {
                            "Admin Security Verification"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                if (config.isConfigured && !showSettingsEditor) {
                    Surface(
                        color = if (isMethodSatisfied) StatusHealthyContainer else StatusWatchContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = if (isMethodSatisfied) "Verified" else "Required",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isMethodSatisfied) OnStatusHealthyContainer else OnStatusWatchContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (!config.isConfigured || showSettingsEditor) {
                Text(
                    text = if (!config.isConfigured) {
                        "Select the security verification method required before anyone can reset plant data:"
                    } else {
                        "Update which verification method Admin requires for resets:"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = draftAdminEmail,
                    onValueChange = {
                        draftAdminEmail = it
                        setupError = null
                    },
                    label = { Text("Authorized Admin Email") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_security_email_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AdminVerificationMethod.entries.forEach { methodOption ->
                        val selected = draftMethod == methodOption
                        Surface(
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable {
                                    draftMethod = methodOption
                                    setupError = null
                                }
                                .testTag("admin_method_${methodOption.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = {
                                        draftMethod = methodOption
                                        setupError = null
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = methodOption.label,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = methodOption.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (draftMethod == AdminVerificationMethod.ADMIN_PIN ||
                    draftMethod == AdminVerificationMethod.PIN_AND_GOOGLE
                ) {
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = {
                            newPinInput = it
                            setupError = null
                        },
                        label = {
                            Text(
                                if (config.isConfigured) {
                                    "New Admin PIN (leave blank to keep current)"
                                } else {
                                    "Create Admin PIN (4–12 chars)"
                                }
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_setup_pin_input")
                    )
                    if (newPinInput.isNotEmpty() || !config.isConfigured) {
                        OutlinedTextField(
                            value = confirmPinInput,
                            onValueChange = {
                                confirmPinInput = it
                                setupError = null
                            },
                            label = { Text("Confirm Admin PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_setup_pin_confirm_input")
                        )
                    }
                }

                if (setupError != null) {
                    Text(
                        text = setupError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusCriticalTextDark
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (config.isConfigured) {
                        OutlinedButton(
                            onClick = {
                                showSettingsEditor = false
                                setupError = null
                                newPinInput = ""
                                confirmPinInput = ""
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Text("Cancel")
                        }
                    }
                    Button(
                        onClick = {
                            val needsPin = draftMethod == AdminVerificationMethod.ADMIN_PIN ||
                                draftMethod == AdminVerificationMethod.PIN_AND_GOOGLE
                            val pinTrimmed = newPinInput.trim()
                            val confirmTrimmed = confirmPinInput.trim()
                            if (needsPin && (pinTrimmed.isNotEmpty() || !config.isConfigured)) {
                                if (pinTrimmed.length !in 4..12) {
                                    setupError = "Admin PIN must be 4–12 characters."
                                    return@Button
                                }
                                if (pinTrimmed != confirmTrimmed) {
                                    setupError = "Admin PIN entries do not match."
                                    return@Button
                                }
                            }
                            isSavingSetup = true
                            scope.launch {
                                val res = AdminResetSecurityStore.configureAdminSecurity(
                                    context = context,
                                    adminEmail = draftAdminEmail,
                                    method = draftMethod,
                                    newPinOrNull = pinTrimmed.takeIf { it.isNotEmpty() }
                                )
                                isSavingSetup = false
                                res.onSuccess {
                                    val wasFreshSetup = !config.isConfigured
                                    val pinSetNow = pinTrimmed.isNotEmpty()
                                    newPinInput = ""
                                    confirmPinInput = ""
                                    setupError = null
                                    showSettingsEditor = false
                                    onConfigUpdated(draftMethod, wasFreshSetup && pinSetNow)
                                }.onFailure { err ->
                                    setupError = err.localizedMessage ?: "Failed to save admin security settings."
                                }
                            }
                        },
                        enabled = !isSavingSetup,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestEmerald,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("save_admin_security_button")
                    ) {
                        Text(if (isSavingSetup) "Saving..." else "Save Admin Security")
                    }
                }
            } else {
                Text(
                    text = "Admin: ${config.adminEmail.ifBlank { currentUserEmail }} • Method: ${config.method.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (config.requiresPin) {
                    if (isPinVerified) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = OnStatusHealthyContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Admin PIN verified",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = OnStatusHealthyContainer
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = pinEntry,
                                onValueChange = {
                                    pinEntry = it
                                    verifyError = null
                                },
                                label = { Text("Enter Admin PIN") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reset_admin_pin_input")
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        when (val res = AdminResetSecurityStore.verifyPin(context, pinEntry)) {
                                            is PinVerifyResult.Verified -> {
                                                pinEntry = ""
                                                verifyError = null
                                                onPinVerified()
                                            }
                                            is PinVerifyResult.Invalid -> {
                                                verifyError = "Incorrect Admin PIN (${res.attemptsRemaining} attempt(s) left)."
                                            }
                                            is PinVerifyResult.LockedOut -> {
                                                verifyError = "Too many failed attempts. Try again in ${res.secondsRemaining}s."
                                            }
                                            is PinVerifyResult.NotConfigured -> {
                                                showSettingsEditor = true
                                            }
                                        }
                                    }
                                },
                                enabled = pinEntry.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForestEmerald,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reset_verify_pin_button")
                            ) {
                                Text("Verify")
                            }
                        }
                    }
                }

                if (config.requiresGoogleReauth) {
                    if (isGoogleVerified) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = OnStatusHealthyContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Google Admin verified (${config.adminEmail})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = OnStatusHealthyContainer
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                isVerifyingGoogle = true
                                verifyError = null
                                scope.launch {
                                    val res = verifyGoogleAdminCredential(
                                        context = context,
                                        expectedAdminEmail = config.adminEmail.ifBlank { currentUserEmail }
                                    )
                                    isVerifyingGoogle = false
                                    res.onSuccess {
                                        onGoogleVerified()
                                    }.onFailure { err ->
                                        verifyError = err.localizedMessage ?: "Google Admin verification failed."
                                    }
                                }
                            },
                            enabled = !isVerifyingGoogle,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestEmerald,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("reset_verify_google_admin_button")
                        ) {
                            if (isVerifyingGoogle) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying Google Admin...")
                            } else {
                                Text("Verify Google Admin (${config.adminEmail.ifBlank { currentUserEmail }})")
                            }
                        }
                    }
                }

                if (verifyError != null) {
                    Text(
                        text = verifyError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusCriticalTextDark
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            if (isMethodSatisfied) {
                                showSettingsEditor = true
                            } else {
                                // Require Google Admin proof if changing security settings while unverified
                                isVerifyingGoogle = true
                                verifyError = null
                                scope.launch {
                                    val res = verifyGoogleAdminCredential(
                                        context = context,
                                        expectedAdminEmail = config.adminEmail.ifBlank { currentUserEmail }
                                    )
                                    isVerifyingGoogle = false
                                    res.onSuccess {
                                        onGoogleVerified()
                                        showSettingsEditor = true
                                    }.onFailure { err ->
                                        verifyError = err.localizedMessage
                                            ?: "Verify current PIN or Google Admin to edit Admin security."
                                    }
                                }
                            }
                        },
                        label = {
                            Text(
                                if (isMethodSatisfied) {
                                    "Admin Settings (Change Method / PIN)"
                                } else {
                                    "Google Re-Auth to Change Method"
                                }
                            )
                        },
                        modifier = Modifier.testTag("open_admin_security_settings_button")
                    )
                }
            }
        }
    }
}
