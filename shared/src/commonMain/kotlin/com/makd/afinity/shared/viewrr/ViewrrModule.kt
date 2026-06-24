package com.makd.afinity.shared.viewrr

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module exposing the viewrr data layer in commonMain.
 * Call from app start: modules(viewrrModule(baseUrl) { currentToken }).
 */
fun viewrrModule(baseUrl: String, tokenProvider: () -> String?): Module = module {
    single<ViewrrApi> { ViewrrClient(baseUrl, tokenProvider) }
}
