package com.example.projectreviewsystem;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ReviewedPdfDialogFragment extends DialogFragment {

    private TextView pdfNameTextView;
    private TextView deadlineTextView;
    private EditText commentsEditText;
    private Button acceptedButton;
    private Button rejectedButton;
    private Button changesRequiredButton;
    private Button sendButton;
    private Button selectDateButton; // Button to open the calendar

    private FirebaseFirestore firestore;
    private String docId;
    private String selectedStatus = ""; // Track the selected status

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_reviewd_pdf, container, false);

        pdfNameTextView = view.findViewById(R.id.pdf_name_text_view);
        deadlineTextView = view.findViewById(R.id.deadline_text_view);
        commentsEditText = view.findViewById(R.id.comments_edit_text);
        acceptedButton = view.findViewById(R.id.accepted_button);
        rejectedButton = view.findViewById(R.id.rejected_button);
        changesRequiredButton = view.findViewById(R.id.question_button);
        sendButton = view.findViewById(R.id.send_button);
        selectDateButton = view.findViewById(R.id.select_date_button); // Initialize the calendar button

        firestore = FirebaseFirestore.getInstance();

        // Get data from arguments
        if (getArguments() != null) {
            docId = getArguments().getString("docId");
            String description = getArguments().getString("description");
            String deadline = getArguments().getString("deadline");

            pdfNameTextView.setText(description);
            deadlineTextView.setText("Deadline: " + deadline);
        }

        // Button click listeners
        setupButtonListeners();

        return view;
    }

    private void setupButtonListeners() {
        acceptedButton.setOnClickListener(v -> {
            selectedStatus = "accepted"; // Set the selected status
            resetButtonBackgrounds();
            acceptedButton.setPressed(true); // This will trigger the state list drawable
        });
        rejectedButton.setOnClickListener(v -> {
            selectedStatus = "rejected"; // Set the selected status
            resetButtonBackgrounds();
            rejectedButton.setPressed(true); // This will trigger the state list drawable
        });
        changesRequiredButton.setOnClickListener(v -> {
            selectedStatus = "changes required"; // Set the selected status
            resetButtonBackgrounds();
            changesRequiredButton.setPressed(true); // This will trigger the state list drawable
        });

        sendButton.setOnClickListener(v -> sendRequestStatus());

        // Open Calendar functionality
        selectDateButton.setOnClickListener(v -> openCalendar());
    }

    private void resetButtonBackgrounds() {
        acceptedButton.setPressed(false);
        rejectedButton.setPressed(false);
        changesRequiredButton.setPressed(false);
    }

    private void sendRequestStatus() {
        if (!selectedStatus.isEmpty()) {
            Map<String, Object> requestStatus = new HashMap<>();
            requestStatus.put("requestId", docId);
            requestStatus.put("status", selectedStatus);
            requestStatus.put("comments", commentsEditText.getText().toString());

            firestore.collection("admin_notifications")
                    .add(requestStatus)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getActivity(), "Request sent to admin.", Toast.LENGTH_SHORT).show();
                        dismiss(); // Close the dialog
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getActivity(), "Failed to send request.", Toast.LENGTH_SHORT). show());
        } else {
            Toast.makeText(getActivity(), "Please select a status before sending.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCalendar() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), (view, selectedYear, selectedMonth, selectedDay) -> {
            // Handle the selected date here
            String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
            deadlineTextView.setText("Deadline: " + selectedDate); // Update the deadline text view
        }, year, month, day);
        datePickerDialog.show();
    }
}