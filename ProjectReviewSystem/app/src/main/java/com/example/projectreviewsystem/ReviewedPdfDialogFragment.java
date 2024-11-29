package com.example.projectreviewsystem;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ReviewedPdfDialogFragment extends BottomSheetDialogFragment {

    private TextView pdfNameTextView;
    private TextView deadlineTextView;
    private EditText commentsEditText;
    private Button approvedButton;
    private Button removedButton;
    private Button changesRequiredButton;
    private OnRequestSentListener requestSentListener;
    private OnStatusSelectedListener statusSelectedListener; // Keep this listener
    private Button sendButton;
    private Button selectDateButton; // Button to open the calendar
    private TextView completionDateTextView; // TextView for completion date
    private FirebaseFirestore firestore;
    private String docId;
    private FirebaseAuth auth;
    private String selectedStatus = "";
    private String selectedDate = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_reviewd_pdf, container, false);
        completionDateTextView = view.findViewById(R.id.completion_date_text_view);
        pdfNameTextView = view.findViewById(R.id.pdf_name_text_view);
        deadlineTextView = view.findViewById(R.id.deadline_text_view);
        commentsEditText = view.findViewById(R.id.comments_edit_text);
        approvedButton = view.findViewById(R.id.accepted_button);
        removedButton = view.findViewById(R.id.rejected_button);
        changesRequiredButton = view.findViewById(R.id.question_button);
        sendButton = view.findViewById(R.id.send_button);
        selectDateButton = view.findViewById(R.id.select_date_button); // Initialize the calendar button
        auth = FirebaseAuth.getInstance();
        isStatusSent = false;
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
        resetButtonBackgrounds();

        approvedButton.setOnClickListener(v -> {
            selectedStatus = "approved"; // Set the selected status
            resetButtonBackgrounds();
            approvedButton.setBackgroundResource(R.drawable.button_accepted); // Change to selected background
        });

        removedButton.setOnClickListener(v -> {
            selectedStatus = "removed"; // Set the selected status
            resetButtonBackgrounds();
            removedButton.setBackgroundResource(R.drawable.button_rejected); // Change to selected background
        });

        changesRequiredButton.setOnClickListener(v -> {
            selectedStatus = "changes required"; // Set the selected status
            resetButtonBackgrounds();
            changesRequiredButton.setBackgroundResource(R.drawable.button_question); // Change to selected background
        });

        sendButton.setOnClickListener(v -> sendRequestStatus());
        selectDateButton.setOnClickListener(v -> openCalendar());
    }
    private void resetButtonBackgrounds() {
        approvedButton.setBackgroundResource(R.drawable.button_background);
        removedButton.setBackgroundResource(R.drawable.button_background);
        changesRequiredButton.setBackgroundResource(R.drawable.button_background);
    }

    private boolean isStatusSent = false; // Flag to prevent multiple sends

    private void sendRequestStatus() {
        if (!selectedStatus.isEmpty() && !isStatusSent) {
            isStatusSent = true; // Set the flag to true to prevent further sends
            Log.d("ReviewedPdfDialog", "Selected Status: " + selectedStatus); // Log the selected status

            // Create a unique identifier for the request
            String requestId = String.valueOf(System.currentTimeMillis()); // Using current time as unique ID

            // Create the request status map
            Map<String, Object> requestStatus = new HashMap<>();
            requestStatus.put("requestId", requestId); // Add unique request ID
            requestStatus.put("status", selectedStatus);
            requestStatus.put("comments", commentsEditText.getText().toString());
            requestStatus.put("completionDate", selectedDate);
            // Add other necessary fields...

            // Get the current user's ID
            String userId = auth.getCurrentUser ().getUid();

            // Fetch user details from Firestore
            firestore.collection("faculty").document(userId).get()
                    .addOnSuccessListener(userDocument -> {
                        if (userDocument.exists()) {
                            // Add user details to the request status
                            String facultyName = userDocument.getString("name");
                            String facultyImageUrl = userDocument.getString("profile_photo");
                            requestStatus.put("facultyName", facultyName);
                            requestStatus.put("facultyIcon", facultyImageUrl);

                            // Reference to the admin notifications collection
                            DocumentReference adminNotificationsRef = firestore.collection("admin_notifications").document(requestId);

                            // Use a transaction to safely check and add the request status
                            firestore.runTransaction(transaction -> {
                                // Check if the request already exists
                                DocumentSnapshot existingRequest = transaction.get(adminNotificationsRef);
                                if (existingRequest.exists()) {
                                    // If it exists, throw an exception to abort the transaction
                                    throw new FirebaseFirestoreException("Request already exists", FirebaseFirestoreException.Code.ABORTED);
                                }

                                // If it doesn't exist, add the new request
                                transaction.set(adminNotificationsRef, requestStatus);
                                return null; // Return null as we are not returning any value from the transaction
                            }).addOnSuccessListener(aVoid -> {
                                Log.d("ReviewedPdfDialog", "Status sent to admin_notifications successfully");
                                Toast.makeText(getContext(), "Request status updated successfully", Toast.LENGTH_SHORT).show();
                                // Notify the Faculty class to refresh counts
                                if (statusSelectedListener != null) {
                                    statusSelectedListener.onStatusSelected(selectedStatus, 0, 0, 0); // Adjust as needed
                                }
                                dismiss(); // Close the dialog
                            }).addOnFailureListener(e -> {
                                Log.w("ReviewedPdfDialog", "Error sending status to admin_notifications", e);
                                Toast.makeText(getContext(), "Error sending status to admin_notifications", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            Log.w("ReviewedPdfDialog", "User  document does not exist");
                            Toast.makeText(getContext(), "User  information could not be retrieved", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(e -> {
                        Log.w("ReviewedPdfDialog", "Error fetching user details", e);
                        Toast.makeText(getContext(), "Error fetching user details", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(getContext(), "Please select a status", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendStatusToAdmin(Map<String, Object> requestStatus) {
        // Send the request status to the admin_notifications collection
        firestore.collection("admin_notifications").add(requestStatus)
                .addOnSuccessListener(documentReference -> {
                    Log.d("ReviewedPdfDialog", "Status sent to admin_notifications successfully: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w("ReviewedPdfDialog", "Error sending status to admin_notifications", e);
                });
    }

    private void openCalendar() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, selectedYear, selectedMonth, selectedDay) -> {
            selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear; // Format the date
            completionDateTextView.setText("Completion Date: " + selectedDate);
        }, year, month, day);
        datePickerDialog.show();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnRequestSentListener) {
            requestSentListener = (OnRequestSentListener) context;
        }
        if (context instanceof OnStatusSelectedListener) {
            statusSelectedListener = (OnStatusSelectedListener) context; // Attach the status listener
        } else {
            throw new RuntimeException(context.toString() + " must implement OnStatusSelectedListener");
        }
    }

    public interface OnRequestSentListener {
        void onRequestSent(String docId);
    }

    public interface OnStatusSelectedListener {
        void onStatusSelected(String status, long doneCount, long inReviewCount, long totalProjectsCount); // Add parameters for updated counts
    }
    // In Faculty class

}