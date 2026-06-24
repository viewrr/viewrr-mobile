package com.makd.afinity.shared.ui.detail

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val detailModule = module { viewModelOf(::DetailViewModel) }
