package com.makd.afinity.shared.viewrr

import com.makd.afinity.shared.ui.auth.AuthViewModel
import com.makd.afinity.shared.ui.home.HomeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module exposing the viewrr data layer + commonMain feature stack.
 * Call from app start: modules(viewrrModule(baseUrl)).
 * The HTTP client reads the bearer from [SessionStore] on every request.
 */
fun viewrrModule(baseUrl: String): Module = module {
    single { SessionStore() }
    single<ViewrrApi> {
        val session = get<SessionStore>()
        ViewrrClient(baseUrl) { session.token }
    }
    single<MediaRepository> { ViewrrMediaRepository(get()) }
    viewModelOf(::HomeViewModel)
    viewModelOf(::AuthViewModel)
}
