package fumi.day.literalagenda.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val SHOW_MINI_CALENDAR = booleanPreferencesKey("show_mini_calendar")
        private val CONTROLS_ON_LEFT = booleanPreferencesKey("controls_on_left")
        private val GITHUB_REPO = stringPreferencesKey("github_repo")
        private val BG_COLOR = stringPreferencesKey("bg_color")
        private val TEXT_COLOR = stringPreferencesKey("text_color")
        private val ACCENT_COLOR = stringPreferencesKey("accent_color")
        private val FONT_CHOICE = stringPreferencesKey("font_choice")
        private val FONT_SIZE = floatPreferencesKey("font_size")
        private val DATE_FORMAT = stringPreferencesKey("date_format")
        private val LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
        private val LAST_SYNCED_SHAS = stringPreferencesKey("last_synced_shas")
        private const val GITHUB_TOKEN_KEY = "github_token"
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    val githubToken: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == GITHUB_TOKEN_KEY) {
                trySend(encryptedPrefs.getString(GITHUB_TOKEN_KEY, "") ?: "")
            }
        }
        encryptedPrefs.registerOnSharedPreferenceChangeListener(listener)
        send(encryptedPrefs.getString(GITHUB_TOKEN_KEY, "") ?: "")
        awaitClose { encryptedPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val showMiniCalendar: Flow<Boolean> = context.dataStore.data
        .map { it[SHOW_MINI_CALENDAR] ?: false }

    val controlsOnLeft: Flow<Boolean> = context.dataStore.data
        .map { it[CONTROLS_ON_LEFT] ?: false }

    val githubRepo: Flow<String> = context.dataStore.data
        .map { it[GITHUB_REPO] ?: "" }

    val bgColor: Flow<String> = context.dataStore.data
        .map { it[BG_COLOR] ?: "" }

    val textColor: Flow<String> = context.dataStore.data
        .map { it[TEXT_COLOR] ?: "" }

    val accentColor: Flow<String> = context.dataStore.data
        .map { it[ACCENT_COLOR] ?: "" }

    val fontChoice: Flow<String> = context.dataStore.data
        .map { it[FONT_CHOICE] ?: "system" }

    val fontSize: Flow<Float> = context.dataStore.data
        .map { it[FONT_SIZE] ?: 16f }

    val dateFormat: Flow<String> = context.dataStore.data
        .map { it[DATE_FORMAT] ?: "wmd" }

    val lastSyncedAt: Flow<Long> = context.dataStore.data
        .map { it[LAST_SYNCED_AT] ?: 0L }

    val lastSyncedShas: Flow<Map<String, String>> = context.dataStore.data
        .map { prefs ->
            val json = prefs[LAST_SYNCED_SHAS] ?: return@map emptyMap()
            try {
                val obj = JSONObject(json)
                obj.keys().asSequence().associateWith { obj.getString(it) }
            } catch (e: Exception) {
                emptyMap()
            }
        }

    suspend fun setShowMiniCalendar(show: Boolean) {
        context.dataStore.edit { it[SHOW_MINI_CALENDAR] = show }
    }

    suspend fun setControlsOnLeft(left: Boolean) {
        context.dataStore.edit { it[CONTROLS_ON_LEFT] = left }
    }

    suspend fun setGitHubToken(token: String) {
        withContext(Dispatchers.IO) {
            encryptedPrefs.edit().putString(GITHUB_TOKEN_KEY, token).apply()
        }
    }

    suspend fun setGitHubRepo(repo: String) {
        context.dataStore.edit { it[GITHUB_REPO] = repo }
    }

    suspend fun setBgColor(color: String) {
        context.dataStore.edit { it[BG_COLOR] = color }
    }

    suspend fun setTextColor(color: String) {
        context.dataStore.edit { it[TEXT_COLOR] = color }
    }

    suspend fun setAccentColor(color: String) {
        context.dataStore.edit { it[ACCENT_COLOR] = color }
    }

    suspend fun setFontChoice(font: String) {
        context.dataStore.edit { it[FONT_CHOICE] = font }
    }

    suspend fun setFontSize(size: Float) {
        context.dataStore.edit { it[FONT_SIZE] = size }
    }

    suspend fun setDateFormat(format: String) {
        context.dataStore.edit { it[DATE_FORMAT] = format }
    }

    suspend fun setLastSyncedAt(time: Long) {
        context.dataStore.edit { it[LAST_SYNCED_AT] = time }
    }

    suspend fun setLastSyncedShas(shas: Map<String, String>) {
        val json = JSONObject(shas).toString()
        context.dataStore.edit { it[LAST_SYNCED_SHAS] = json }
    }
}
