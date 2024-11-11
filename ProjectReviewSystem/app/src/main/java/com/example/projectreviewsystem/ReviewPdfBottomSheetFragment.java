package com.example.projectreviewsystem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ReviewPdfBottomSheetFragment extends BottomSheetDialogFragment {

    private EditText pdfNameEditText;
    private EditText pdfDescriptionEditText;
    private Button sendButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_review, container, false);

        pdfNameEditText = view.findViewById(R.id.pdf_name);
        pdfDescriptionEditText = view.findViewById(R.id.pdf_description);
        sendButton = view.findViewById(R.id.send_button);

        sendButton.setOnClickListener(v -> {
            String pdfName = pdfNameEditText.getText().toString();
            String description = pdfDescriptionEditText.getText().toString();
            // Handle sending the data to the admin here
            // For example, you can call a method in the Faculty activity to send the data

            // Dismiss the bottom sheet after sending
            dismiss();
        });

        return view;
    }
}