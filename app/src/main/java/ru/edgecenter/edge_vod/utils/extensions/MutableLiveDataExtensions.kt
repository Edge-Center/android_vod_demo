package ru.edgecenter.edge_vod.utils.extensions

import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<MutableList<T>>.add(item: T) {
    val updatedItems = value
    updatedItems?.add(item)
    value = updatedItems
}

fun <T> MutableLiveData<MutableList<T>>.remove(item: T) {
    val updatedItems = value
    updatedItems?.remove(item)
    value = updatedItems
}

/**
 * Removes an element at the specified [index] from the list, if it is present.
 */
fun <T> MutableLiveData<MutableList<T>>.replaceAt(index: Int, item: T) {
    val updatedItems = value
    updatedItems?.let {
        if (index < it.size) {
            it[index] = item
            value = it
        }
    }
}