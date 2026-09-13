/*
 *
 *  * Copyright (C) 2025 Kaori (original author)
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */


package com.kaori.circletosearch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.kaori.circletosearch.ui.theme.CircleToSearchTheme
import com.kaori.circletosearch.ui.components.AccessibilityDisclosureDialog
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLanguage.wrapBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, getString(R.string.toast_getting_started), Toast.LENGTH_LONG).show()
        setContent {
            CircleToSearchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("home") }
                    androidx.compose.animation.Crossfade(targetState = currentScreen) { screen ->
                        when (screen) {
                            "settings" -> com.kaori.circletosearch.ui.OverlaySettingsScreen(onBack = { currentScreen = "home" })
                            else -> SetupScreen(
                                onSettingsClick = { currentScreen = "settings" }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(onSettingsClick: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    
    // Permission States
    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isDefaultAssistant by remember { mutableStateOf(isDefaultAssistant(context)) }
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }
    
    // Check permissions on Resume
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
                isDefaultAssistant = isDefaultAssistant(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // 1. Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.header_title),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                LanguageSwitcher()
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_overlay_settings), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.subtitle_main),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // 2. REQUIRED: Accessibility Service
            Text(
                text = stringResource(R.string.label_required),
                style = MaterialTheme.typography.labelSmall,
                color = if (isAccessibilityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )
            
            if (isAccessibilityEnabled) {
                // Granted State
                Card(
                     colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.cd_granted),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.label_accessibility_active),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.label_accessibility_active_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                 // Action Needed State
                Card(
                    onClick = {
                        showAccessibilityDisclosure = true
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.label_enable_accessibility),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                             Text(
                                text = stringResource(R.string.label_enable_accessibility_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // 3. OPTIONAL: Default Assistant
            Text(
                text = stringResource(R.string.label_optional_assistant),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )

            if (isDefaultAssistant) {
                 // Granted State
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.cd_active),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                         Text(
                            text = stringResource(R.string.label_default_assistant_set),
                            style = MaterialTheme.typography.titleMedium,
                             color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            } else {
                 // Action State
                Card(
                    onClick = {
                        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.label_set_default_assistant),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.label_set_default_assistant_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                }
            Spacer(modifier = Modifier.height(24.dp))


        }
    }


    if (showAccessibilityDisclosure) {
        AccessibilityDisclosureDialog(
            onAccept = {
                showAccessibilityDisclosure = false
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, context.getString(R.string.toast_accessibility_settings_error), android.widget.Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = {
                showAccessibilityDisclosure = false
            }
        )
    }
}

// Helper Functions
fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val expectedComponentName = android.content.ComponentName(context, CircleToSearchAccessibilityService::class.java)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    
    val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledComponent = android.content.ComponentName.unflattenFromString(componentNameString)
        if (enabledComponent != null && enabledComponent == expectedComponentName)
            return true
    }
    return false
}

fun isDefaultAssistant(context: android.content.Context): Boolean {
    // Basic check: triggers the settings intent, but actual "is default" check is complex
    // on some Android versions. For simplified UI, we might relay on user return, 
    // or use RoleManager on Android 10+. 
    // For now, let's keep it simple or check specific secure settings if possible.
    // A reliable check is to see if we are arguably the voice interaction service.
    
    val assistant = Settings.Secure.getString(context.contentResolver, "voice_interaction_service")
    val component = android.content.ComponentName(context, CircleToSearchVoiceService::class.java)
    val myComponentString = component.flattenToString()
    
    // Check if our VoiceInteractionService is the one currently set as default
    return assistant == myComponentString
}

