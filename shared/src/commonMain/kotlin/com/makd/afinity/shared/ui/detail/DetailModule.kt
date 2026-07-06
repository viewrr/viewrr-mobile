package com.makd.afinity.shared.ui.detail

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// Explicit lambda (not viewModelOf) so DetailViewModel's `debug` param uses its default;
// the constructor DSL would otherwise try to resolve a Boolean from the graph and crash.
val detailModule = module { viewModel { DetailViewModel(get()) } }
