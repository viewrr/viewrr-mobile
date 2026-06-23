package com.makd.afinity.util

import androidx.compose.runtime.Composable
import com.makd.afinity.data.repository.PreferencesRepository
import com.makd.afinity.ui.item.delegates.ItemDownloadDelegate
import org.koin.compose.koinInject

@Composable
fun rememberPreferencesRepository(): PreferencesRepository = koinInject()

@Composable
fun rememberItemDownloadDelegate(): ItemDownloadDelegate = koinInject()
