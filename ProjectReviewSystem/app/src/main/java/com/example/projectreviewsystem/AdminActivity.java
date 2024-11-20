package com.example.projectreviewsystem;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminActivity extends AppCompatActivity {

    private Uri selectedFileUri;
    private ImageView bellIcon;
    private String selectedDeadline;
    private TextView fileNameTextView;  // TextView for file name
    private TextView deadlineTextView;
    private FirebaseFirestore firestore;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        firestore = FirebaseFirestore.getInstance();

        bellIcon = findViewById(R.id.bell_icon);
        bellIcon.setOnClickListener(v -> showNotificationsDialog());

        // Initialize buttons and set click listeners
        for (int i = 1; i <= 6; i++) {
            int buttonId = getResources().getIdentifier("s" + i, "id", getPackageName());
            Button sendButton = findViewById(buttonId);
            sendButton.setOnClickListener(v -> showBottomSheetDialog());
        }

        fetchFacultyData(); // Call to fetch faculty data
        listenForNotifications(); // Call to listen for notifications
    }

    private void fetchFacultyData() {
        firestore.collection("faculty")
                .get() // Fetch all documents in the "faculty" collection
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int facultyCount = 1; // Counter for faculty layouts
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String facultyName = document.getString("name");
                            String facultyImageUrl = document.getString("profile_photo"); // Use profile_photo field
                            String userId = document.getId(); // Get the user ID

                            // Fetch designation and department from the "designation" collection
                            fetchDesignationData(userId, facultyCount, facultyName, facultyImageUrl);
                            facultyCount++;
                        }
                    } else {
                        Log.e("AdminActivity", "Error fetching faculty data: ", task.getException());
                    }
                });
    }

    private void fetchDesignationData(String userId, int facultyCount, String facultyName, String facultyImageUrl) {
        firestore.collection("designation").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot designationDocument = task.getResult(); // Use DocumentSnapshot
                        String facultyDesignation = designationDocument.getString("designation"); // Fetch designation
                        String facultyDepartment = designationDocument.getString("department"); // Fetch department

                        // Populate the existing faculty layout based on the count
                        populateFacultyLayout(facultyCount, facultyName, facultyImageUrl, facultyDesignation, facultyDepartment);
                    } else {
                        // If the designation document does not exist, populate with null values
                        populateFacultyLayout(facultyCount, facultyName, facultyImageUrl, null, null);
                        Log.e("AdminActivity", "Error fetching designation data: ", task.getException());
                    }
                });
    }

    private void populateFacultyLayout(int facultyIndex, String name, String imageUrl, String designation, String department) {
        int layoutId = getResources().getIdentifier("faculty_" + facultyIndex, "id", getPackageName());
        LinearLayout facultyLayout = findViewById(layoutId);

        if (facultyLayout != null) {
            TextView facultyNameTextView = facultyLayout.findViewById(getResources().getIdentifier("textView" + facultyIndex, "id", getPackageName()));
            facultyNameTextView.setText(name);

            CircleImageView facultyImageView = facultyLayout.findViewById(getResources().getIdentifier("imageView" + facultyIndex, "id", getPackageName()));
            Glide.with(this)
                    .load(imageUrl)
                    .error(R.drawable.logo)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e("GlideError", "Image load failed for URL: " + imageUrl, e);
                            return false; // Return false to allow Glide to handle the error placeholder
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("GlideSuccess", "Image loaded successfully for URL: " + imageUrl);
                            return false; // Return false if you want Glide to handle the resource
                        }
                    })
                    .into(facultyImageView);

            // Set designation and department or default values
            TextView facultyDesignationTextView = facultyLayout.findViewById(getResources().getIdentifier("designation" + facultyIndex, "id", getPackageName()));
            TextView facultyDepartmentTextView = facultyLayout.findViewById(getResources().getIdentifier("department" + facultyIndex, "id", getPackageName()));

            if (designation != null && department != null) {
                facultyDesignationTextView.setText(designation); // Set the designation
                facultyDepartmentTextView.setText(department); // Set the department
            } else {
                facultyDesignationTextView.setText("N/A"); // Set to "N/A" if not available
                facultyDepartmentTextView.setText("N/A"); // Set to "N/A" if not available
            }

            // Set up the button click listener
            Button sendButton = facultyLayout.findViewById(getResources().getIdentifier("s" + facultyIndex, "id", getPackageName()));
            sendButton.setOnClickListener(v -> showBottomSheetDialog());
        } else {
            Log.e("AdminActivity", "Faculty layout not found for index: " + facultyIndex);
        }
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
            // Check if description, deadline, and file URI are not null or empty
            if (description.isEmpty() || selectedDeadline == null || selectedFileUri == null) {
                Toast.makeText(this, "Please fill in all fields and select a file.", Toast.LENGTH_SHORT).show();
                return;
            }
            sendNewRequest(description, selectedDeadline, selectedFileUri.toString());
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            // Format the date correctly
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(selectedYear, selectedMonth, selectedDay);
            selectedDeadline = sdf.format(selectedDate.getTime());
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
            if (uri.getScheme().equals("content")) {
                // If the URI is a content URI, use the DocumentsContract to get the file name
                String[] projection = { DocumentsContract.Document.COLUMN_DISPLAY_NAME };
                try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        fileName = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                    }
                }
            } else if (uri.getScheme().equals("file")) {
                // If the URI is a file URI, get the file name from the path
                fileName = uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
            }
        }
        return fileName;
    }

    private void sendNewRequest(String description, String deadline, String documentUrl) {
        // Convert the deadline to the required format
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        String formattedDeadline = "";

        try {
            Date date = inputFormat.parse(deadline);
            formattedDeadline = outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Invalid deadline format", Toast.LENGTH_SHORT).show();
            return; // Exit if the format is invalid
        }

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("description", description);
        requestData.put("deadline", formattedDeadline); // Use the formatted deadline
        requestData.put("fileUri", documentUrl);
        requestData.put("fileName", getFileName(selectedFileUri));
        requestData.put("status", "pending");

        firestore.collection("requests").add(requestData)
                .addOnSuccessListener(documentReference -> {
                    // Request successfully sent
                    Toast.makeText(this, "Request sent successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show();
                    Log.e("AdminActivity", "Error sending request: ", e);
                });
    }
    private void listenForNotifications() {
        firestore.collection("admin_notifications")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        Log.e("AdminActivity", "Error fetching notifications: ", e);
                        return; // Handle errors
                    }

                    // Clear previous notifications
                    // This is no longer needed since we are handling notifications in the dialog

                    for (QueryDocumentSnapshot document : snapshots) {
                        String requestId = document.getString("requestId");
                        String status = document.getString("status");
                        String facultyName = document.getString("facultyName");
                        String facultyIcon = document.getString("facultyIcon");

                        // Display the notification to the admin
                        displayNotification(facultyName, status, facultyIcon);
                    }
                });
    }

    private void displayNotification(String facultyName, String status, String facultyIcon) {
        // This method is no longer needed since we are handling notifications in the dialog
        // You can remove this method if you are not using it elsewhere
    }

    private void showNotificationsDialog() {
        BottomSheetDialog notificationsDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notifications, null);
        notificationsDialog.setContentView(dialogView);

        LinearLayout notificationContainer = dialogView.findViewById(R.id.notification_container);
        TextView noNotificationsTextView = dialogView.findViewById(R.id.no_notifications_text);

        // Clear previous notifications
        notificationContainer.removeAllViews();

        // Fetch notifications from Firestore and display them
        firestore.collection("admin_notifications")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int notificationCount = 0; // Counter for notifications
                        if (task.getResult().isEmpty()) {
                            // Show the "No notifications available" message
                            noNotificationsTextView.setVisibility(View.VISIBLE);
                        } else {
                            noNotificationsTextView.setVisibility(View.GONE); // Hide the no notifications text
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String facultyName = document.getString("facultyName");
                                String status = document.getString("status");
                                String facultyIcon = document.getString("facultyIcon");

                                // Create a notification view
                                View notificationView = getLayoutInflater().inflate(R.layout.notification_item, null);
                                CircleImageView facultyIconView = notificationView.findViewById(R.id.faculty_icon);
                                TextView notificationMessageView = notificationView.findViewById(R.id.notification_message);
                                TextView notificationTimeView = notificationView.findViewById(R.id.notification_time);

                                // Set the notification message
                                notificationMessageView.setText(facultyName + " has " + status + " the request.");

                                // Load the faculty icon using Glide
                                Glide.with(this)
                                        .load(facultyIcon)
                                        .error(R.drawable.ic_launcher_background) // Optional: Default icon if loading fails
                                        .into(facultyIconView);

                                // Set the current time as the notification time
                                String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
                                notificationTimeView.setText(currentTime);

                                // Add the notification view to the notification container
                                notificationContainer.addView(notificationView);
                                notificationCount++; // Increment the notification count
                            }
                        }

                        // Update the notification count on the bell icon
                        updateNotificationCount(notificationCount);
                    } else {
                        Log.e("AdminActivity", "Error fetching notifications: ", task.getException());
                    }
                });

        notificationsDialog.show();
    }

    private void updateNotificationCount(int count) {
        TextView notificationCountTextView = findViewById(R.id.notification_count);
        if (count > 0) {
            notificationCountTextView.setText(String.valueOf(count));
            notificationCountTextView.setVisibility(View.VISIBLE); // Show the count
        } else {
            notificationCountTextView.setVisibility(View.GONE); // Hide if no notifications
        }
    }
}