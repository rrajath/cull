package com.rrajath.cull.feature.wizard

import android.content.Context
import com.rrajath.cull.CullApplication
import com.rrajath.cull.core.database.ImmichAssetMappingDb
import com.rrajath.cull.core.datastore.SettingsRepository
import com.rrajath.cull.core.datastore.UnifiedPhotoRepository
import com.rrajath.cull.core.model.UnifiedPhotoItem
import com.rrajath.cull.core.network.ImmichApi
import com.rrajath.cull.core.network.ImmichRepository
import com.rrajath.cull.ui.component.SourceMode
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
        CullApplication.setImmichCredentials(immichApiKey, immichUrl)
        val mappingDb = ImmichAssetMappingDb.getInstance(context)
        ImmichRepository(ImmichApi(immichUrl, immichApiKey), mappingDb)
    } else {
        null
    }

    UnifiedPhotoRepository(context, immichRepo).loadPhotos(sourceMode, folderUri)
}
