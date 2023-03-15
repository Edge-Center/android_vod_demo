package ru.edgecenter.edge_vod.utils

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.navGraphViewModels

typealias ViewModelCreator<VM> = () -> VM

@Suppress("UNCHECKED_CAST")
class ViewModelFactory<VM : ViewModel>(
    private val viewModelCreator: ViewModelCreator<VM>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return viewModelCreator() as T
    }
}

inline fun <reified VM : ViewModel> Fragment.viewModelCreator(
    noinline creator: ViewModelCreator<VM>)
: Lazy<VM> {
    return viewModels { ViewModelFactory(creator) }
}

inline fun <reified VM : ViewModel> Fragment.sharedViewModelCreator(
    noinline creator: ViewModelCreator<VM>
): Lazy<VM> {
    return activityViewModels { ViewModelFactory(creator) }
}

inline fun<reified VM: ViewModel> Fragment.navGraphViewModelCreator(
    @IdRes navGraphId: Int,
    noinline creator: ViewModelCreator<VM>
): Lazy<VM> {
    return navGraphViewModels(navGraphId) {
        ViewModelFactory(creator)
    }
}