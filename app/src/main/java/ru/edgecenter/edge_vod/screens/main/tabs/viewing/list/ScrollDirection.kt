package ru.edgecenter.edge_vod.screens.main.tabs.viewing.list

sealed class ScrollDirection(val displayedPosition: Int) {
    class Back(displayedPosition: Int) : ScrollDirection(displayedPosition)
    class Forward(displayedPosition: Int) : ScrollDirection(displayedPosition)
}
