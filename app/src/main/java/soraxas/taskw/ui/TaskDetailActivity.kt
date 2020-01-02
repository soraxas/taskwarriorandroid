package soraxas.taskw.ui

import android.content.Context
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import soraxas.taskw.R

fun showD(context: Context, view: View) {
    MaterialDialog(context, BottomSheet()).show {
        //        title(text = "Your Title")
//        message(text = "Your Message")


//        customView(R.layout.item_one_task)


        customView(view = view)
        // Using a dimen instead is encouraged as it's easier to have all instances changeable from one place
        cornerRadius(res = R.dimen.bottomsheets_corner_radius)
    }
}