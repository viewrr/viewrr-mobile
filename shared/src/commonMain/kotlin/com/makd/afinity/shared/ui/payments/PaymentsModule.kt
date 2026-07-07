package com.makd.afinity.shared.ui.payments

import com.makd.afinity.shared.payments.PaymentsPreferences
import com.russhwolf.settings.Settings
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Payments opt-in DI (p2p-0020 / #3). [PaymentsPreferences] gets its own [Settings] instance,
 * matching how [com.makd.afinity.shared.viewrr.SessionStore] is wired in `viewrrModule` — the
 * underlying platform store (SharedPreferences/NSUserDefaults) is shared, keys don't collide.
 */
val paymentsModule = module {
    single { PaymentsPreferences(Settings()) }
    viewModelOf(::PaymentsViewModel)
}
