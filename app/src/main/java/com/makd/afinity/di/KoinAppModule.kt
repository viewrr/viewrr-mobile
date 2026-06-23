package com.makd.afinity.di

import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import com.makd.afinity.data.repository.audiobookshelf.AbsProgressSyncScheduler
import com.makd.afinity.data.manager.AdminChangeBroadcaster
import com.makd.afinity.data.repository.audiobookshelf.AudiobookshelfAddressResolver
import com.makd.afinity.player.audiobookshelf.AudiobookshelfEqualizerManager
import com.makd.afinity.player.audiobookshelf.AudiobookshelfPlaybackManager
import com.makd.afinity.player.audiobookshelf.AudiobookshelfPlayer
import com.makd.afinity.player.audiobookshelf.AudiobookshelfProgressSyncer
import com.makd.afinity.player.audiobookshelf.AudiobookshelfSkipSilenceManager
import com.makd.afinity.data.repository.media.BoxSetCache
import com.makd.afinity.cast.CastDeviceProfileFactory
import com.makd.afinity.cast.CastManager
import com.makd.afinity.data.manager.DownloadSemaphoreManager
import com.makd.afinity.data.repository.GenreRepository
import com.makd.afinity.ui.item.delegates.ItemDownloadDelegate
import com.makd.afinity.ui.item.delegates.ItemUserDataDelegate
import com.makd.afinity.util.JellyfinImageUrlBuilder
import com.makd.afinity.data.websocket.JellyfinWebSocketManager
import com.makd.afinity.data.repository.jellyseerr.JellyseerrAddressResolver
import com.makd.afinity.data.manager.MediaChangeManager
import com.makd.afinity.data.manager.MediaRefreshBus
import com.makd.afinity.util.NetworkConnectivityMonitor
import com.makd.afinity.data.manager.OfflineModeManager
import com.makd.afinity.data.repository.PeopleRepository
import com.makd.afinity.ui.player.PlaylistManager
import com.makd.afinity.data.repository.server.ServerAddressResolver
import com.makd.afinity.data.manager.SessionManager
import com.makd.afinity.data.storage.StorageLocationProvider
import com.makd.afinity.data.repository.StudioRepository
import com.makd.afinity.data.syncplay.SyncPlayRawWebSocket
import com.makd.afinity.data.syncplay.SyncPlayTimeSyncEngine
import com.makd.afinity.data.updater.UpdateManager
import com.makd.afinity.data.updater.notification.UpdateNotificationManager
import com.makd.afinity.data.updater.UpdateScheduler
import com.makd.afinity.data.sync.UserDataSyncScheduler
import com.makd.afinity.data.updater.UpdateCheckWorker
import com.makd.afinity.data.workers.AbsProgressSyncWorker
import com.makd.afinity.data.workers.HomeDataReloadWorker
import com.makd.afinity.data.workers.UserDataSyncWorker
import com.makd.afinity.MainViewModel
import com.makd.afinity.navigation.MainNavigationViewModel
import com.makd.afinity.ui.admin.identify.IdentifyViewModel
import com.makd.afinity.ui.admin.images.EditImagesViewModel
import com.makd.afinity.ui.admin.metadata.EditMetadataViewModel
import com.makd.afinity.ui.admin.refresh.RefreshMetadataViewModel
import com.makd.afinity.ui.audiobookshelf.genre.AudiobookshelfGenreResultsViewModel
import com.makd.afinity.ui.audiobookshelf.item.AudiobookshelfItemViewModel
import com.makd.afinity.ui.audiobookshelf.item.series.AudiobookshelfSeriesViewModel
import com.makd.afinity.ui.audiobookshelf.libraries.AudiobookshelfLibrariesViewModel
import com.makd.afinity.ui.audiobookshelf.login.AudiobookshelfLoginViewModel
import com.makd.afinity.ui.audiobookshelf.player.AudiobookshelfPlayerViewModel
import com.makd.afinity.ui.components.AfinityTopAppBarViewModel
import com.makd.afinity.ui.downloads.DownloadsViewModel
import com.makd.afinity.ui.favorites.FavoritesViewModel
import com.makd.afinity.ui.home.HomeViewModel
import com.makd.afinity.ui.item.ItemDetailViewModel
import com.makd.afinity.ui.jellyseerr.JellyseerrLoginViewModel
import com.makd.afinity.ui.libraries.LibrariesViewModel
import com.makd.afinity.ui.library.LibraryContentViewModel
import com.makd.afinity.ui.livetv.LiveTvViewModel
import com.makd.afinity.ui.login.LoginViewModel
import com.makd.afinity.ui.main.MainViewModel as MainViewModelUi
import com.makd.afinity.ui.person.PersonViewModel
import com.makd.afinity.ui.player.PlayerViewModel
import com.makd.afinity.ui.player.PlayerWrapperViewModel
import com.makd.afinity.ui.player.SyncPlayViewModel
import com.makd.afinity.ui.requests.FilteredMediaViewModel
import com.makd.afinity.ui.requests.RequestsViewModel
import com.makd.afinity.ui.search.GenreResultsViewModel
import com.makd.afinity.ui.search.SearchViewModel
import com.makd.afinity.ui.settings.servers.AddEditServerViewModel
import com.makd.afinity.ui.settings.servers.ControlPanelViewModel
import com.makd.afinity.ui.settings.SessionSwitcherViewModel
import com.makd.afinity.ui.settings.SettingsViewModel
import com.makd.afinity.ui.settings.update.UpdateViewModel
import com.makd.afinity.ui.watchlist.WatchlistViewModel

val managerModule = module {
    singleOf(::AbsProgressSyncScheduler)
    singleOf(::AdminChangeBroadcaster)
    singleOf(::AudiobookshelfAddressResolver)
    singleOf(::AudiobookshelfEqualizerManager)
    singleOf(::AudiobookshelfPlaybackManager)
    singleOf(::AudiobookshelfPlayer)
    singleOf(::AudiobookshelfProgressSyncer)
    singleOf(::AudiobookshelfSkipSilenceManager)
    singleOf(::BoxSetCache)
    singleOf(::CastDeviceProfileFactory)
    singleOf(::CastManager)
    singleOf(::DownloadSemaphoreManager)
    singleOf(::GenreRepository)
    singleOf(::ItemDownloadDelegate)
    singleOf(::ItemUserDataDelegate)
    singleOf(::JellyfinImageUrlBuilder)
    singleOf(::JellyfinWebSocketManager)
    singleOf(::JellyseerrAddressResolver)
    singleOf(::MediaChangeManager)
    singleOf(::MediaRefreshBus)
    singleOf(::NetworkConnectivityMonitor)
    singleOf(::OfflineModeManager)
    singleOf(::PeopleRepository)
    singleOf(::PlaylistManager)
    singleOf(::ServerAddressResolver)
    singleOf(::SessionManager)
    singleOf(::StorageLocationProvider)
    singleOf(::StudioRepository)
    singleOf(::SyncPlayRawWebSocket)
    singleOf(::SyncPlayTimeSyncEngine)
    singleOf(::UpdateManager)
    singleOf(::UpdateNotificationManager)
    singleOf(::UpdateScheduler)
    singleOf(::UserDataSyncScheduler)
}

val viewModelModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::MainNavigationViewModel)
    viewModelOf(::IdentifyViewModel)
    viewModelOf(::EditImagesViewModel)
    viewModelOf(::EditMetadataViewModel)
    viewModelOf(::RefreshMetadataViewModel)
    viewModelOf(::AudiobookshelfGenreResultsViewModel)
    viewModelOf(::AudiobookshelfItemViewModel)
    viewModelOf(::AudiobookshelfSeriesViewModel)
    viewModelOf(::AudiobookshelfLibrariesViewModel)
    viewModelOf(::AudiobookshelfLoginViewModel)
    viewModelOf(::AudiobookshelfPlayerViewModel)
    viewModelOf(::AfinityTopAppBarViewModel)
    viewModelOf(::DownloadsViewModel)
    viewModelOf(::FavoritesViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::ItemDetailViewModel)
    viewModelOf(::JellyseerrLoginViewModel)
    viewModelOf(::LibrariesViewModel)
    viewModelOf(::LibraryContentViewModel)
    viewModelOf(::LiveTvViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::MainViewModelUi)
    viewModelOf(::PersonViewModel)
    viewModelOf(::PlayerViewModel)
    viewModelOf(::PlayerWrapperViewModel)
    viewModelOf(::SyncPlayViewModel)
    viewModelOf(::FilteredMediaViewModel)
    viewModelOf(::RequestsViewModel)
    viewModelOf(::GenreResultsViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::AddEditServerViewModel)
    viewModelOf(::ControlPanelViewModel)
    viewModelOf(::SessionSwitcherViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::UpdateViewModel)
    viewModelOf(::WatchlistViewModel)
}

val workerModule = module {
    workerOf(::UpdateCheckWorker)
    workerOf(::AbsProgressSyncWorker)
    workerOf(::HomeDataReloadWorker)
    workerOf(::UserDataSyncWorker)
}
