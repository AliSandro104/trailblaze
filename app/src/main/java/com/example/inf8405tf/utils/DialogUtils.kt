package com.example.inf8405tf.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.inf8405tf.R
import com.google.android.material.card.MaterialCardView
import jakarta.inject.Inject
import javax.inject.Singleton

@Singleton
class DialogUtils @Inject constructor() {

    fun showBottomDialog(context: Context, dialogText: String, dialogColor: Int, dialogIcon: Int) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_page_dialog)

        val text = dialog.findViewById<TextView>(R.id.bottom_page_dialog_text)
        text.text = dialogText

        val cardView = dialog.findViewById<MaterialCardView>(R.id.bottom_page_dialog)
        cardView.setCardBackgroundColor(ContextCompat.getColor(context, dialogColor))

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

            val icon = dialog.findViewById<ImageView>(R.id.bottom_page_dialog_icon)

            icon.setImageResource(dialogIcon)
            icon.setColorFilter(ContextCompat.getColor(context, R.color.white))

            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // Set the dialog to appear at the bottom
            setGravity(Gravity.BOTTOM)
        }

        dialog.show()

        // Dismiss after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }, 2000)
    }
}