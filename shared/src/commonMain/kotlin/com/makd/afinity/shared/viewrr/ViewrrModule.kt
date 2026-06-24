package com.makd.afinity.shared.viewrr

import com.makd.afinity.shared.ui.home.HomeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module exposing the viewrr data layer + commonMain feature stack.
 * Call from app start: modules(viewrrModule(baseUrl) { currentToken }).
 */
fun viewrrModule(baseUrl: String, tokenProvider: () -> String?): Module = module {
    single<ViewrrApi> { ViewrrClient(baseUrl, tokenProvider) }
    single<MediaRepository> { ViewrrMediaRepository(get()) }
    viewModelOf(::HomeViewModel)
}
