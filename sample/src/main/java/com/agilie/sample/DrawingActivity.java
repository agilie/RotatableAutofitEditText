package com.agilie.sample;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.agilie.RotatableAutofitEditText;

import java.util.ArrayList;

public class DrawingActivity extends AppCompatActivity {

    private Button addTextButton;
    private RelativeLayout container;
    private ArrayList<RotatableAutofitEditText> editTexts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing);

        container = (RelativeLayout) findViewById(R.id.container);
        addTextButton = (Button) findViewById(R.id.addTextButton);

        addTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RotatableAutofitEditText newEditText = (RotatableAutofitEditText) LayoutInflater.from(DrawingActivity.this).inflate(R.layout.view_autoresize_edittext, container, false);
                container.addView(newEditText);
                newEditText.requestLayout();
                editTexts.add(newEditText);
                showSoftKeyboard(newEditText);
            }
        });

    }

    private void showSoftKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

}
