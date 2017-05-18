package com.agilie.sample

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.RelativeLayout


import com.agilie.RotatableAutofitEditText
import kotlinx.android.synthetic.main.activity_drawing.*

import java.util.ArrayList

class DrawingActivity : AppCompatActivity() {

    private val editTexts = ArrayList<RotatableAutofitEditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawing)
        addTextButton!!.setOnClickListener { addTextButton() }

        addInsomniaRegularTextButton!!.typeface = Typeface.createFromAsset(assets, "fonts/android_insomnia_regular.ttf")
        addInsomniaRegularTextButton!!.setOnClickListener { addTextButton(Typeface.createFromAsset(assets, "fonts/android_insomnia_regular.ttf")) }

        addDroidRobotTextButton!!.typeface = Typeface.createFromAsset(assets, "fonts/doridrobot.ttf")
        addDroidRobotTextButton!!.setOnClickListener { addTextButton(Typeface.createFromAsset(assets, "fonts/doridrobot.ttf")) }

    }

    private fun addTextButton(typeface: Typeface) {
        val newEditText = LayoutInflater.from(this@DrawingActivity).inflate(R.layout.view_autoresize_edittext, container, false) as RotatableAutofitEditText
        container!!.addView(newEditText)
        newEditText.requestLayout()
        newEditText.typeface = typeface
        editTexts.add(newEditText)
        showSoftKeyboard(newEditText)
    }

    private fun addTextButton() {
        val newEditText = LayoutInflater.from(this@DrawingActivity).inflate(R.layout.view_autoresize_edittext, container, false) as RotatableAutofitEditText
        container!!.addView(newEditText)
        newEditText.requestLayout()
        editTexts.add(newEditText)
        showSoftKeyboard(newEditText)
    }

    private fun showSoftKeyboard(view: View) {
        if (view.requestFocus()) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

}
