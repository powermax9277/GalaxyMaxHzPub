package com.tribalfs.gmh.helpers

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.databinding.InverseBindingMethod
import androidx.databinding.InverseBindingMethods
import com.google.android.material.chip.ChipGroup

@InverseBindingMethods(
    InverseBindingMethod(type = ChipGroup::class,
    attribute = "android:checkedButton",
    method = "getCheckedChipId"))
class ChipGroupBindingAdapter {
    companion object {
        @BindingAdapter("android:checkedButton")
        fun setCheckedChip(view: ChipGroup?, id: Int) {
            if (id != view?.checkedChipId) {
                view?.check(id)
            }
        }

        @BindingAdapter(
            value = ["android:onCheckedChanged", "android:checkedButtonAttrChanged"],
            requireAll = false)
        fun setChipsListeners(view: ChipGroup?, listener: ChipGroup.OnCheckedChangeListener?,
                              attrChange: InverseBindingListener?) {
            if (attrChange == null) {
                view?.setOnCheckedChangeListener(listener)
            } else {
                view?.setOnCheckedChangeListener { group, checkedId ->
                    listener?.onCheckedChanged(group, checkedId)
                    attrChange.onChange()
                }
            }
        }
    }
}