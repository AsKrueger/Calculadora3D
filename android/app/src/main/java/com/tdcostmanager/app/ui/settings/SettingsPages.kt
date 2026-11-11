package com.tdcostmanager.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tdcostmanager.app.BuildConfig
import com.tdcostmanager.app.R
import com.tdcostmanager.app.ui.common.LocalFmt
import com.tdcostmanager.app.ui.common.PieceImage
import com.tdcostmanager.app.ui.common.Section
import com.tdcostmanager.app.ui.common.TextInput

@Composable
fun BusinessScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val b = s?.business ?: return
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(vm::setLogo) }

    var name by remember { mutableStateOf(b.name) }
    var taxId by remember { mutableStateOf(b.taxId) }
    var email by remember { mutableStateOf(b.email) }
    var phone by remember { mutableStateOf(b.phone) }
    var address by remember { mutableStateOf(b.address) }
    var footer by remember { mutableStateOf(b.quoteFooter) }

    fun save() = vm.update {
        it.copy(business = it.business.copy(name = name.trim(), taxId = taxId.trim(), email = email.trim(),
            phone = phone.trim(), address = address.trim(), quoteFooter = footer.trim()))
    }

    SubScreen(stringResource(R.string.settings_business_row), onBack) {
        Text(stringResource(R.string.business_help), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Section(stringResource(R.string.business_logo)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PieceImage(b.logo, 72.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    OutlinedButton(onClick = {
                        logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(stringResource(if (b.logo == null) R.string.business_add_logo else R.string.business_change_logo)) }
                    if (b.logo != null) TextButton(onClick = vm::removeLogo) { Text(stringResource(R.string.business_remove_logo)) }
                }
            }
        }
        Section(stringResource(R.string.business_data)) {
            TextInput(stringResource(R.string.business_name), name, { name = it; save() })
            TextInput(stringResource(R.string.business_tax_id), taxId, { taxId = it; save() })
            TextInput(stringResource(R.string.business_email), email, { email = it; save() }, keyboardType = KeyboardType.Email)
            TextInput(stringResource(R.string.business_phone), phone, { phone = it; save() }, keyboardType = KeyboardType.Phone)
            TextInput(stringResource(R.string.business_address), address, { address = it; save() }, singleLine = false)
        }
        Section(stringResource(R.string.business_footer)) {
            TextInput(stringResource(R.string.business_footer_field), footer, { footer = it; save() }, singleLine = false,
                supporting = stringResource(R.string.business_footer_help))
        }
    }
}

private enum class FeedbackType(val label: Int) { BUG(R.string.feedback_bug), IDEA(R.string.feedback_idea), OTHER(R.string.feedback_other) }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val fmt = LocalFmt.current
    var type by rememberSaveable { mutableStateOf(FeedbackType.IDEA) }
    var message by rememberSaveable { mutableStateOf("") }
    var includeInfo by rememberSaveable { mutableStateOf(true) }
    val typeLabel = stringResource(type.label)

    SubScreen(stringResource(R.string.settings_feedback), onBack) {
        Text(stringResource(R.string.feedback_intro), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeedbackType.entries.forEach { t ->
                FilterChip(selected = type == t, onClick = { type = t }, label = { Text(stringResource(t.label)) })
            }
        }
        TextInput(stringResource(R.string.feedback_message), message, { message = it }, singleLine = false,
            supporting = stringResource(R.string.feedback_message_help))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = includeInfo, onCheckedChange = { includeInfo = it })
            Column {
                Text(stringResource(R.string.feedback_include_info), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.feedback_include_info_help),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Button(
            onClick = {
                val info = if (includeInfo) "\n\n---\nApp ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                    "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                    "${Build.MANUFACTURER} ${Build.MODEL}\n${fmt.locale.toLanguageTag()} · ${fmt.currency.currencyCode}" else ""
                val subject = context.getString(R.string.app_name) + " · " + typeLabel
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(BuildConfig.FEEDBACK_EMAIL))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, message.trim() + info)
                }
                runCatching { context.startActivity(intent) }.onFailure {
                    context.startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("message/rfc822")
                            .putExtra(Intent.EXTRA_EMAIL, arrayOf(BuildConfig.FEEDBACK_EMAIL))
                            .putExtra(Intent.EXTRA_SUBJECT, subject)
                            .putExtra(Intent.EXTRA_TEXT, message.trim() + info), null))
                }
            },
            enabled = message.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Outlined.Send, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.feedback_send))
        }
        Text(stringResource(R.string.feedback_footer), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Política de privacidad pública (la misma URL que se declara en Google Play). */
const val PRIVACY_URL = "https://askrueger.github.io/3d-cost-manager/privacy.html"

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    SubScreen(stringResource(R.string.settings_privacy), onBack) {
        listOf(
            R.string.privacy_local_title to R.string.privacy_local_body,
            R.string.privacy_ads_title to R.string.privacy_ads_body,
            R.string.privacy_network_title to R.string.privacy_network_body,
            R.string.privacy_feedback_title to R.string.privacy_feedback_body,
            R.string.privacy_delete_title to R.string.privacy_delete_body
        ).forEach { (t, b) ->
            Section(stringResource(t)) { Text(stringResource(b), style = MaterialTheme.typography.bodyMedium) }
        }
        val context = LocalContext.current
        OutlinedButton(
            onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL))) }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.privacy_full)) }
    }
}
