package com.makd.afinity.shared.ui.history

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Koin module wiring the watch-history ViewModel into the graph. */
val historyModule = module { viewModelOf(::HistoryViewModel) }
