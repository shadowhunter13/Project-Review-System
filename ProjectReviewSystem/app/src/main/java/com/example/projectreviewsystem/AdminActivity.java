package com.example.projectreviewsystem;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private Uri selectedFileUri;
    private String selectedDeadline;
    private TextView fileNameTextView;  // TextView for file name
    private TextView deadlineTextView;
    private FirebaseFirestore firestore;

    // Arrays to hold IDs of layout elements
    private final int[] facultyLayoutIds = {
            R.id.faculty_1, R.id.faculty_2, R.id.faculty_3,
            R.id.faculty_4, R.id.faculty_5, R.id.faculty_6
    };
    private final int[] facultyImageIds = {
            R.id.imageView1, R.id.imageView2, R.id.imageView3,
            R.id.imageView4, R.id.imageView5, R.id.imageView6
    };
    private final int[] facultyNameIds = {
            R.id.textView1, R.id.textView2, R.id.textView3,
            R.id.textView4, R.id.textView5, R.id.textView6
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        firestore = FirebaseFirestore.getInstance();

        Button sendButton1 = findViewById(R.id.s1);
        Button sendButton2 = findViewById(R.id.s2);
        Button sendButton3 = findViewById(R.id.s3);
        Button sendButton4 = findViewById(R.id.s4);
        Button sendButton5 = findViewById(R.id.s5);
        Button sendButton6 = findViewById(R.id.s6);

        sendButton1.setOnClickListener(v -> showBottomSheetDialog());
        sendButton2.setOnClickListener(v -> showBottomSheetDialog());
        sendButton3.setOnClickListener(v -> showBottomSheetDialog());
        sendButton4.setOnClickListener(v -> showBottomSheetDialog());
        sendButton5.setOnClickListener(v -> showBottomSheetDialog());
        sendButton6.setOnClickListener(v -> showBottomSheetDialog());

        fetchFacultyData(); // Call to fetch faculty data
    }

    private void fetchFacultyData() {
        firestore.collection("faculty")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int facultyIndex = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String facultyName = document.getString("name");
                            String facultyImageUrl = document.getString("imageUrl");

                            // Update the faculty layout if the index is valid
                            if (facultyIndex < facultyLayoutIds.length) {
                                LinearLayout facultyLayout = findViewById(facultyLayoutIds[facultyIndex]);
                                facultyLayout.setVisibility(View.VISIBLE); // Make the layout visible

                                // Update the faculty image
                                ImageView facultyImageView = facultyLayout.findViewById(facultyImageIds[facultyIndex]);
                                Glide.with(this)
                                        .load(facultyImageUrl)
                                        .into(facultyImageView); // Load the image

                                // Update the faculty name
                                TextView facultyNameTextView = facultyLayout.findViewById(facultyNameIds[facultyIndex]);
                                facultyNameTextView.setText(facultyName); // Set the name

                                facultyIndex++;
                            } else {
                                Log.w("AdminActivity", "Too many faculty members. Only displaying the first " + facultyLayoutIds.length);
                                break; // Prevent index out of bounds
                            }
                        }
                    } else {
                        Log.e("AdminActivity", "Error fetching faculty data: ", task.getException());
                    }
                });
    }

    private void showBottomSheetDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_dialog, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        Button uploadButton = bottomSheetView.findViewById(R.id.button_upload_file);
        EditText descriptionEditText = bottomSheetView.findViewById(R.id.edit_text_description);
        deadlineTextView = bottomSheetView.findViewById(R.id.text_view_deadline); // Initialize deadlineTextView
        Button selectDateButton = bottomSheetView.findViewById(R.id.button_select_date); // Get the select date button
        Button sendRequestButton = bottomSheetView.findViewById(R.id.button_send_request);

        fileNameTextView = bottomSheetView.findViewById(R.id.text_view_file_name);  // Get the TextView reference

        uploadButton.setOnClickListener(v -> openFileSelector());

        // Set onClickListener for the date button
        selectDateButton.setOnClickListener(v -> showDatePickerDialog());

        sendRequestButton.setOnClickListener(v -> {
            String description = descriptionEditText.getText().toString();
            sendRequestToFaculty(description);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void showDatePickerDialog() {
        // Get the current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create a DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            selectedDeadline = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
            deadlineTextView.setText("Selected Deadline: " + selectedDeadline); // Update the TextView with selected date
        }, year, month, day);

        datePickerDialog.show(); // Show the DatePickerDialog
    }

    private void openFileSelector() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        });
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            // Get file name and display it
            String fileName = getFileName(selectedFileUri);
            fileNameTextView.setText(fileName != null ? fileName : "File selected");  // Update the TextView with file name
        }
    }

    // Helper method to get file name from Uri
    private String getFileName(Uri uri) {
        String fileName = null;
        if (uri != null) {
            String[] projection = {DocumentsContract.Document.COLUMN_DISPLAY_NAME};
            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                }
            }
        }
        return fileName;
    }

    private void sendRequestToFaculty(String description) {
        Map<String, Object> request = new HashMap<>();
        request.put("description", description);
        request.put("deadline", selectedDeadline);
        request.put("fileUri", selectedFileUri != null ? selectedFileUri.toString() : ""); // Ensure the URI is not null
        request.put("status", "pending");

        firestore.collection("requests")
                .add(request)
                .addOnSuccessListener(documentReference -> {
                    // Notify admin of success
                    Log.d("AdminActivity", "Request sent successfully!");
                })
                .addOnFailureListener(e -> {
                    // Notify admin of failure
                    Log.e("AdminActivity", "Error sending request: ", e);
                });
    }
}
