package com.rrajath.occullt.feature.wizard

import android.content.Context
import com.rrajath.occullt.OcculltApplication
import com.rrajath.occullt.core.database.ImmichAssetMappingDb
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.datastore.UnifiedPhotoRepository
import com.rrajath.occullt.core.model.UnifiedPhotoItem
import com.rrajath.occullt.core.network.ImmichApi
import com.rrajath.occullt.core.network.ImmichRepository
import com.rrajath.occullt.ui.component.SourceMode
import kotlinx.coroutines.flow.first

/**
 * Loads the full photo library for the active source mode. Abstracted so the
 * wizard ViewModels can be unit-tested with a fake loader.
 */
fun interface WizardPhotoLoader {
    suspend fun load(sourceMode: SourceMode, folderUri: String?): Result<List<UnifiedPhotoItem>>
}

fun defaultWizardPhotoLoader(
    context: Context,
    settingsRepository: SettingsRepository,
): WizardPhotoLoader = WizardPhotoLoader { sourceMode, folderUri ->
    val immichUrl = settingsRepository.immichUrl.first()
    val immichApiKey = settingsRepository.immichApiKey.first()

    val immichRepo = if (sourceMode != SourceMode.Local && !immichUrl.isNullOrBlank() && !immichApiKey.isNullOrBlank()) {
        OcculltApplication.setImmichCredentials(immichApiKey, immichUrl)
        val mappingDb = ImmichAssetMappingDb.getInstance(context)
        ImmichRepository(ImmichApi(immichUrl, immichApiKey), mappingDb)
    } else {
        null
    }

    UnifiedPhotoRepository(context, immichRepo).loadPhotos(sourceMode, folderUri)
}
