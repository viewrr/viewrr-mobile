package com.makd.afinity.shared.ui.series

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val seriesModule = module { viewModelOf(::SeriesViewModel) }
