package com.kaori.circletosearch

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import java.util.Locale

object AppLanguage {
    private const val PREFERENCES_NAME = "app_language"
    private const val LANGUAGE_KEY = "selected_language"
    private const val LANGUAGE_MIGRATION_KEY = "language_migration_version"
    private const val CURRENT_LANGUAGE_MIGRATION = 1

    const val ENGLISH = "en"
    const val SIMPLIFIED_CHINESE = "zh-CN"

    fun current(context: Context): String {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (preferences.getInt(LANGUAGE_MIGRATION_KEY, 0) < CURRENT_LANGUAGE_MIGRATION) {
            preferences.edit()
                .putString(LANGUAGE_KEY, SIMPLIFIED_CHINESE)
                .putInt(LANGUAGE_MIGRATION_KEY, CURRENT_LANGUAGE_MIGRATION)
                .commit()
            return SIMPLIFIED_CHINESE
        }
        return preferences.getString(LANGUAGE_KEY, SIMPLIFIED_CHINESE) ?: SIMPLIFIED_CHINESE
    }

    fun set(context: Context, languageTag: String) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_KEY, languageTag)
            .putInt(LANGUAGE_MIGRATION_KEY, CURRENT_LANGUAGE_MIGRATION)
            .apply()
    }

    fun wrapBaseContext(context: Context): Context {
        val locale = Locale.forLanguageTag(current(context))
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }
}

@Composable
fun LanguageSwitcher(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = stringResource(R.string.cd_language)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.language_english)) },
                onClick = {
                    AppLanguage.set(context, AppLanguage.ENGLISH)
                    expanded = false
                    (context as? Activity)?.recreate()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.language_simplified_chinese)) },
                onClick = {
                    AppLanguage.set(context, AppLanguage.SIMPLIFIED_CHINESE)
                    expanded = false
                    (context as? Activity)?.recreate()
                }
            )
        }
    }
}
