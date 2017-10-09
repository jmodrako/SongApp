package com.tooploox.songapp.common

import android.support.design.widget.BottomSheetBehavior
import android.view.View

fun setupInitialBottomSheet(allBottomSheets: List<BottomSheetBehavior<*>>, bottomSheet: BottomSheetBehavior<*>, sheetTrigger: View) {
    bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN

    sheetTrigger.click {
        bottomSheet.state =
            if (bottomSheet.state != BottomSheetBehavior.STATE_HIDDEN) BottomSheetBehavior.STATE_HIDDEN
            else BottomSheetBehavior.STATE_EXPANDED

        hideAllSheets(allBottomSheets, bottomSheet)
    }
}

fun hideAllSheets(allBottomSheets: List<BottomSheetBehavior<*>>, bottomSheetToStayOpen: BottomSheetBehavior<*>? = null) {
    allBottomSheets.filter { it != bottomSheetToStayOpen }.forEach { it.state = BottomSheetBehavior.STATE_HIDDEN }
}

fun hideBottomSheet(bottomSheet: BottomSheetBehavior<*>) {
    bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
}

fun hideBottomSheetsIfNeeded(allBottomSheets: List<BottomSheetBehavior<*>>): Boolean {
    val isAnySheetOpened = allBottomSheets.any { it.state == BottomSheetBehavior.STATE_EXPANDED }
    hideAllSheets(allBottomSheets)
    return isAnySheetOpened
}