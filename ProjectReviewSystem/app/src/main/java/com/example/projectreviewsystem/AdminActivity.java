package com.example.projectreviewsystem;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminActivity extends AppCompatActivity implements ReviewedPdfDialogFragment.OnStatusSelectedListener {
    private Uri selectedFileUri;
    private ImageView bellIcon;
    private String selectedDeadline;
    private TextView fileNameTextView;
    private TextView deadlineTextView;
    private EditText pendingReviewsCount;
    private EditText inProgresssCount;
    private EditText completedReviewsCount;
    private EditText rejectedReviewsCount;
    private FirebaseFirestore firestore;
    private Set<String> processedNotificationIds = new HashSet<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        firestore = FirebaseFirestore.getInstance();

        initializeViews();
        loadCountsFromFirestore();
        createDashboardCountsDocument();
        fetchFacultyData();
        listenForDashboardCounts();
        listenForNotifications();
//        listenForRequestUpdates();
        initializeCountersIfNeeded();

    }

    @Override
    public void onStatusSelected(String status, long doneCount, long inReviewCount, long totalProjectsCount) {
        Log.d("AdminActivity", "Status selected: " + status + ", Done Count: " + doneCount +
                ", In Review Count: " + inReviewCount + ", Total Projects Count: " + totalProjectsCount);
        Toast.makeText(this, "Status selected: " + status + ", Done Count: " + doneCount, Toast.LENGTH_SHORT).show();

        updateCountsBasedOnStatus(status); // Call the method here

        if (status.equals("approved")) {
            incrementCompletedCount(); // Increase the count of completed reviews
        } else if (status.equals("removed")) {
            incrementRejectedCount(); // Increase the count of rejected reviews
        }

        decrementInReviewCount(); // Decrease the in-review count
    }
    private void incrementCompletedCount() {
        DocumentReference countsRef = firestore.collection("dashboard_counts").document("counts");
        countsRef.update("completed", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    Log.d("AdminActivity", "Completed count incremented successfully");
                    // Update the UI after a successful increment
                    countsRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            long completedCount = documentSnapshot.getLong("completed") != null ? documentSnapshot.getLong("completed") : 0;
                            completedReviewsCount.setText(String.valueOf(completedCount));
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e("AdminActivity", "Error incrementing completed count", e));
    }

    private void incrementRejectedCount() {
        DocumentReference countsRef = firestore.collection("dashboard_counts").document("counts");
        countsRef.update("rejected", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    Log.d("AdminActivity", "Rejected count incremented successfully");
                    // Update the UI after a successful increment
                    countsRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            long rejectedCount = documentSnapshot.getLong("rejected") != null ? documentSnapshot.getLong("rejected") : 0;
                            rejectedReviewsCount.setText(String.valueOf(rejectedCount));
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e("AdminActivity", "Error incrementing rejected count", e));
    }

    private void decrementInReviewCount() {
        DocumentReference countsRef = firestore.collection("dashboard_counts").document("counts");
        countsRef.update("inProgress", FieldValue.increment(-1))
                .addOnSuccessListener(aVoid -> Log.d("AdminActivity", "In Review count decremented successfully"))
                .addOnFailureListener(e -> Log.e("AdminActivity", "Error decrementing in review count", e));
    }

    private void initializeViews() {
        bellIcon = findViewById(R.id.bell_icon);
        bellIcon.setOnClickListener(v -> showNotificationsDialog());
        pendingReviewsCount = findViewById(R.id.Pending_Reviews);
        inProgresssCount = findViewById(R.id.in_progress_count);
        completedReviewsCount = findViewById(R.id.Completed_Reviews);
        rejectedReviewsCount = findViewById(R.id.Rejected_Reviews);

        // Initialize buttons and set click listeners
        for (int i = 1; i <= 6; i++) {
            int buttonId = getResources().getIdentifier("s" + i, "id", getPackageName());
            Button sendButton = findViewById(buttonId);

            // Assuming you have a way to get the researcherId for each button
            String researcherId = getResearcherIdForButton(i); // Implement this method to get the correct researcherId

            sendButton.setOnClickListener(v -> showBottomSheetDialog(researcherId)); // Pass the researcherId
        }
    }

    private String getResearcherIdForButton(int index) {
        
        return "researcherId_" + index; // Example: return a dummy researcher ID
    }

    private void listenForDashboardCounts() {
        DocumentReference countsRef = firestore.collection("dashboard_counts").document("counts");

        countsRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w("AdminActivity", "Listen failed.", e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                int completedCount = snapshot.getLong("completed") != null ? snapshot.getLong("completed").intValue() : 0;
                int rejectedCount = snapshot.getLong("rejected") != null ? snapshot.getLong("rejected").intValue() : 0;
                int pendingCount = snapshot.getLong("pending") != null ? snapshot.getLong("pending").intValue() : 0;
                int inProgressCount = snapshot.getLong("inProgress") != null ? snapshot.getLong("inProgress").intValue() : 0;

                updateCounts(pendingCount, inProgressCount, completedCount, rejectedCount);
            } else {
                Log.d("AdminActivity", "Current data: null");
            }
        });
    }


    private void createDashboardCountsDocument() {
        DocumentReference countsRef = firestore.collection("dashboard_counts").document("counts");

        countsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                if (!document.exists()) {
                    // Document does not exist, create it with initial counts
                    Map<String, Object> initialCounts = new HashMap<>();
                    initialCounts.put("pending", 0);
                    initialCounts.put("inProgress", 0);
                    initialCounts.put("completed", 0);
                    initialCounts.put("rejected", 0);

                    countsRef.set(initialCounts)
                            .addOnSuccessListener(aVoid -> Log.d("AdminActivity", "Dashboard counts document created successfully"))
                            .addOnFailureListener(e -> Log.e("AdminActivity", "Error creating dashboard counts document", e));
                } else {
                    Log.d("AdminActivity", "Dashboard counts document already exists.");
                }
            } else {
                Log.e("AdminActivity", "Error checking for dashboard counts document: ", task.getException());
            }
        });
    }


    private void loadCountsFromFirestore() {
        firestore.collection("dashboard_counts").document("counts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        int pending = document.getLong("pending") != null ? document.getLong("pending").intValue() : 0;
                        int inProgress = document.getLong("inProgress") != null ? document.getLong("inProgress").intValue() : 0;
                        int completed = document.getLong("completed") != null ? document.getLong("completed").intValue() : 0;
                        int rejected = document.getLong("rejected") != null ? document.getLong("rejected").intValue() : 0;

                        // Update the UI with the loaded counts
                        updateCounts(pending, inProgress, completed, rejected);
                    } else {
                        Log.e("AdminActivity", "Error fetching counts: ", task.getException());
                    }
                });
    }

    private void listenForRequestUpdates() {
        String researcherId = FirebaseAuth.getInstance().getCurrentUser ().getUid(); // Get the current user's ID

        firestore.collection("requests")
                .whereEqualTo("researcherId", researcherId) // Filter requests by researcher ID
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        Log.e("AdminActivity", "Error fetching requests: ", e);
                        return;
                    }

                    int pendingCount = 0;
                    int inProgressCount = 0;
                    int completedCount = 0;
                    int rejectedCount = 0;

                    for (QueryDocumentSnapshot document : snapshots) {
                        String status = document.getString("status");
                        switch (status) {
                            case "pending":
                                pendingCount++;
                                break;
                            case "in progress":
                                inProgressCount++;
                                break;
                            case "completed":
                                completedCount++;
                                break;
                            case "rejected":
                                rejectedCount++;
                                break;
                        }
                    }

                    updateCountsInFirestore(pendingCount, inProgressCount, completedCount, rejectedCount);
                    updateReviewCounts(pendingCount, inProgressCount, completedCount, rejectedCount);
                });
    }

    private void updateCountsInFirestore(int pending, int inProgress, int completed, int rejected) {
        Map<String, Object> counts = new HashMap<>();
        counts.put("pending", pending);
        counts.put("inProgress", inProgress);
        counts.put("completed", completed);
        counts.put("rejected", rejected);

        firestore.collection("dashboard_counts").document("counts")
                .update(counts)
                .addOnSuccessListener(aVoid -> Log.d("AdminActivity", "Counts updated in Firestore"))
                .addOnFailureListener(e -> Log.e("AdminActivity", "Error updating counts in Firestore", e));
    }



    public void updateReviewCounts(int newPending, int newInProgress, int newCompleted, int newRejected) {
        updateCounts(newPending, newInProgress, newCompleted, newRejected);
    }

    private void fetchFacultyData() {
        firestore.collection("faculty")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int facultyCount = 1;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String facultyName = document.getString("name");
                            String facultyImageUrl = document.getString("profile_photo");
                            String userId = document.getId(); // Get the unique ID of the faculty member

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
                        DocumentSnapshot designationDocument = task.getResult();
                        String facultyDesignation = designationDocument.getString("designation");
                        String facultyDepartment = designationDocument.getString("department");

                        populateFacultyLayout(facultyCount, facultyName, facultyImageUrl, facultyDesignation, facultyDepartment, userId);
                    } else {
                        populateFacultyLayout(facultyCount, facultyName, facultyImageUrl, null, null, userId);
                        Log.e("AdminActivity", "Error fetching designation data: ", task.getException());
                    }
                });
    }

    private void populateFacultyLayout(int facultyIndex, String name, String imageUrl, String designation, String department, String researcherId) {
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
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("GlideSuccess", "Image loaded successfully for URL: " + imageUrl);
                            return false;
                        }
                    })
                    .into(facultyImageView);

            TextView facultyDesignationTextView = facultyLayout.findViewById(getResources().getIdentifier("designation" + facultyIndex, "id", getPackageName()));
            TextView facultyDepartmentTextView = facultyLayout.findViewById(getResources().getIdentifier("department" + facultyIndex, "id", getPackageName()));

            if (designation != null && department != null) {
                facultyDesignationTextView.setText(designation);
                facultyDepartmentTextView.setText(department);
            } else {
                facultyDesignationTextView.setText("N/A");
                facultyDepartmentTextView.setText("N/A");
            }

            Button sendButton = facultyLayout.findViewById(getResources().getIdentifier("s" + facultyIndex, "id", getPackageName()));
            sendButton.setOnClickListener(v -> showBottomSheetDialog(researcherId)); // Pass the researcherId
        } else {
            Log.e("AdminActivity", "Faculty layout not found for index: " + facultyIndex);
        }
    }

    private void showBottomSheetDialog(String researcherId) { // Accept researcherId as a parameter
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_dialog, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        Button uploadButton = bottomSheetView.findViewById(R.id.button_upload_file);
        EditText descriptionEditText = bottomSheetView.findViewById(R.id.edit_text_description);
        deadlineTextView = bottomSheetDialog.findViewById(R.id.text_view_deadline);
        Button selectDateButton = bottomSheetView.findViewById(R.id.button_select_date);
        Button sendRequestButton = bottomSheetView.findViewById(R.id.button_send_request);

        fileNameTextView = bottomSheetView.findViewById(R.id.text_view_file_name);

        uploadButton.setOnClickListener(v -> openFileSelector());

        selectDateButton.setOnClickListener(v -> showDatePickerDialog());

        sendRequestButton.setOnClickListener(v -> {
            String description = descriptionEditText.getText().toString();
            if (description.isEmpty() || selectedDeadline == null || selectedFileUri == null) {
                Toast.makeText(this, "Please fill in all fields and select a file.", Toast.LENGTH_SHORT).show();
                return;
            }
            sendNewRequest(description, selectedDeadline, selectedFileUri.toString(), researcherId); // Pass researcherId
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
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(selectedYear, selectedMonth, selectedDay);
            selectedDeadline = sdf.format(selectedDate.getTime());
            deadlineTextView.setText("Selected Deadline: " + selectedDeadline);
        }, year, month, day);

        datePickerDialog.show();
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
            if (selectedFileUri != null) {
                String fileName = getFileName(selectedFileUri);
                fileNameTextView.setText(fileName != null ? fileName : "File selected");
            } else {
                Toast.makeText(this, "Failed to get file URI", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getFileName(Uri uri) {
        String fileName = null;
        if (uri != null) {
            if ("content".equals(uri.getScheme())) {
                String[] projection = {DocumentsContract.Document.COLUMN_DISPLAY_NAME};
                try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        fileName = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                    }
                }
            } else if ("file".equals(uri.getScheme())) {
                fileName = uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
            }
        }
        return fileName;
    }

    private boolean isRequestInProgress = false; // Flag to prevent multiple submissions

    private void sendNewRequest(String description, String deadline, String documentUrl, String researcherId) { // Add researcherId parameter
        if (isRequestInProgress) {
            Toast.makeText(this, "Request is already being processed. Please wait.", Toast.LENGTH_SHORT).show();
            return; // Prevent multiple submissions
        }

        isRequestInProgress = true; // Set the flag to true

        SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        String formattedDeadline;

        try {
            Date date = inputFormat.parse(deadline);
            formattedDeadline = outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Invalid deadline format", Toast.LENGTH_SHORT).show();
            isRequestInProgress = false; // Reset the flag
            return;
        }

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("description", description);
        requestData.put("deadline", formattedDeadline);
        requestData.put("fileUri", documentUrl);
                requestData.put("fileName", getFileName(selectedFileUri));
        requestData.put("status", "pending");
        requestData.put("researcherId", researcherId); // Add researcher ID to the request data

        firestore.collection("requests").add(requestData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Request sent successfully", Toast.LENGTH_SHORT).show();
                    incrementPendingAndInProgressCounts(); // Increment both counts together
                    isRequestInProgress = false; // Reset the flag after processing
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show();
                    Log.e("AdminActivity", "Error sending request: ", e);
                    isRequestInProgress = false; // Reset the flag on failure
                });
    }
    private void incrementPendingAndInProgressCounts() {
        DocumentReference countsRef = firestore.collection("dashboard_counts").document("counts");

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(countsRef);
                    if (snapshot.exists()) {
                        // Increment both pending and in-progress counts
                        long pendingCount = snapshot.getLong("pending") != null ? snapshot.getLong("pending") : 0;
                        long inProgressCount = snapshot.getLong("inProgress") != null ? snapshot.getLong("inProgress") : 0;

                        transaction.update(countsRef, "pending", pendingCount + 1);
                        transaction.update(countsRef, "inProgress", inProgressCount + 1);
                    }
                    return null;
                }).addOnSuccessListener(aVoid -> Log.d("AdminActivity", "Pending and In Progress counts incremented successfully"))
                .addOnFailureListener(e -> Log.e("AdminActivity", "Error incrementing counts", e));
    }
//    private void incrementPendingCounter() {
//        DocumentReference docRef = firestore.collection("counters").document("yourDocumentId");
//        docRef.update("pending", FieldValue.increment(1))
//                .addOnSuccessListener(aVoid -> {
//                    Log.d("AdminActivity", "Pending counter incremented successfully");
//                })
//                .addOnFailureListener(e -> {
//                    Log.e("AdminActivity", "Error incrementing pending counter", e);
//                });
//    }


    private void initializeCountersIfNeeded() {
        DocumentReference docRef = firestore.collection("counters").document("yourDocumentId");
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                initializeCountersInFirestore();
            } else {
                // Fetch existing counter values
                int pending = documentSnapshot.getLong("pending").intValue();
                int inProgress = documentSnapshot.getLong("inProgress").intValue();
                int completed = documentSnapshot.getLong("completed").intValue();
                int rejected = documentSnapshot.getLong("rejected").intValue();

                // Update your UI or local variables with these values
                updateCounts(pending, inProgress, completed, rejected);
            }
        }).addOnFailureListener(e -> {
            Log.e("AdminActivity", "Error fetching counters", e);
        });
    }

    private void initializeCountersInFirestore() {
        DocumentReference docRef = firestore.collection("counters").document("yourDocumentId");
        Map<String, Object> initialCounts = new HashMap<>();
        initialCounts.put("pending", 0);
        initialCounts.put("inProgress", 0);
        initialCounts.put("completed", 0);
        initialCounts.put("rejected", 0);

        docRef.set(initialCounts)
                .addOnSuccessListener(aVoid -> Log.d("AdminActivity", "Counters initialized successfully"))
                .addOnFailureListener(e -> Log.e("AdminActivity", "Error initializing counters", e));
    }

    private void listenForNotifications() {
        firestore.collection("admin_notifications")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        Log.e("AdminActivity", "Error fetching notifications: ", e);
                        return;
                    }

                    for (QueryDocumentSnapshot document : snapshots) {
                        String requestId = document.getString("requestId"); // Get the unique request ID
                        String status = document.getString("status");

                        // Check if this notification has already been processed
                        if (requestId != null && !processedNotificationIds.contains(requestId)) {
                            processedNotificationIds.add(requestId); // Mark this notification as processed
                            if (status != null) {
                                updateCountsBasedOnStatus(status); // Process the status
                                displayNotification(document); // Display the notification
                            }
                        }
                    }

                    // Optionally, you can fetch completed and rejected counts directly from Firestore
                    fetchCompletedAndRejectedCounts();
                });
    }
    private void displayNotification(QueryDocumentSnapshot document) {
        String facultyName = document.getString("facultyName");
        String facultyIcon = document.getString("facultyIcon");

        // Create a view for the notification
        View notificationView = getLayoutInflater().inflate(R.layout.notification_item, null);
        CircleImageView facultyIconView = notificationView.findViewById(R.id.faculty_icon);
        TextView notificationMessageView = notificationView.findViewById(R.id.notification_message);
        TextView notificationTimeView = notificationView.findViewById(R.id.notification_time);

        // Set the notification message
        notificationMessageView.setText(facultyName + " has " + document.getString("status") + " the request.");

        // Load the faculty icon using Glide
        Glide.with(this)
                .load(facultyIcon)
                .error(R.drawable.ic_launcher_background)
                .into(facultyIconView);

        // Get the current time and set it to the notification time view
        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        notificationTimeView.setText(currentTime);

        // Add the new notification view to the container
        LinearLayout notificationContainer = findViewById(R.id.notification_container);
        if (notificationContainer != null) {
            notificationContainer.addView(notificationView);
        } else {
            Log.e("AdminActivity", "Notification container is null when trying to add a view.");
        }
    }
    private void fetchCompletedAndRejectedCounts() {
        DocumentReference countsRef = firestore.collection("dashboard_counts").document("counts");

        countsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    long completedCount = document.getLong("completed") != null ? document.getLong("completed") : 0;
                    long rejectedCount = document.getLong("rejected") != null ? document.getLong("rejected") : 0;

                    // Update the UI with the fetched counts
                    completedReviewsCount.setText(String.valueOf(completedCount));
                    rejectedReviewsCount.setText(String.valueOf(rejectedCount));
                }
            } else {
                Log.e("AdminActivity", "Error fetching completed and rejected counts: ", task.getException());
            }
        });
    }
    private void updateCountsBasedOnStatus(String status) {
        DocumentReference countsRef = firestore.collection("dashboard_counts").document("counts");

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(countsRef);
            if (snapshot.exists()) {
                // Get current counts
                int completed = snapshot.getLong("completed") != null ? snapshot.getLong("completed").intValue() : 0;
                int rejected = snapshot.getLong("rejected") != null ? snapshot.getLong("rejected").intValue() : 0;
                int inProgress = snapshot.getLong("inProgress") != null ? snapshot.getLong("inProgress").intValue() : 0;
                int pending = snapshot.getLong("pending") != null ? snapshot.getLong("pending").intValue() : 0;

                // Update counts based on the notification status
                switch (status) {
                    case "approved":
                        completed++;
                        if (inProgress > 0) inProgress--;
                        break;
                    case "removed":
                    case "changes required":
                        rejected++;
                        if (inProgress > 0) inProgress--;
                        break;
                    default:
                        Log.w("AdminActivity", "Received unknown status: " + status);
                        return null; // Skip unknown statuses
                }

                transaction.update(countsRef, "completed", completed);
                transaction.update(countsRef, "rejected", rejected);
                transaction.update(countsRef, "inProgress", inProgress);
                transaction.update(countsRef, "pending", pending);
            } else {
                Log.e("AdminActivity", "Counts document does not exist");
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d("AdminActivity", "Counts updated successfully based on notification status");
            // Fetch updated counts to update UI
            countsRef.get().addOnSuccessListener(updatedSnapshot -> {
                if (updatedSnapshot.exists()) {
                    int updatedCompleted = updatedSnapshot.getLong("completed") != null ? updatedSnapshot.getLong("completed").intValue() : 0;
                    int updatedRejected = updatedSnapshot.getLong("rejected") != null ? updatedSnapshot.getLong("rejected").intValue() : 0;
                    int updatedInProgress = updatedSnapshot.getLong("inProgress") != null ? updatedSnapshot.getLong("inProgress").intValue() : 0;
                    int updatedPending = updatedSnapshot.getLong("pending") != null ? updatedSnapshot.getLong("pending").intValue() : 0;

                    updateCounts(updatedPending, updatedInProgress, updatedCompleted, updatedRejected);
                }
            }).addOnFailureListener(e -> {
                Log.e("AdminActivity", "Error fetching updated counts", e);
            });
        }).addOnFailureListener(e -> {
            Log.e("AdminActivity", "Error updating counts based on notification status", e);
        });
    }
    private void updateCounts(int pending, int inProgress, int completed, int rejected) {
        pendingReviewsCount.setText(String.valueOf(pending));
        inProgresssCount.setText(String.valueOf(inProgress));
        completedReviewsCount.setText(String.valueOf(completed));
        rejectedReviewsCount.setText(String.valueOf(rejected));
    }

    private void showNotificationsDialog() {
        BottomSheetDialog notificationsDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notifications, null);
        notificationsDialog.setContentView(dialogView);

        LinearLayout notificationContainer = dialogView.findViewById(R.id.notification_container);
        TextView noNotificationsTextView = dialogView.findViewById(R.id.no_notifications_text);

        // Clear the existing notifications
        notificationContainer.removeAllViews();

        firestore.collection("admin_notifications")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int notificationCount = 0;
                        if (task.getResult().isEmpty()) {
                            noNotificationsTextView.setVisibility(View.VISIBLE);
                        } else {
                            noNotificationsTextView.setVisibility(View.GONE);
                            // Iterate through the latest notifications and add them to the container
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String facultyName = document.getString("facultyName");
                                String status = document.getString("status");
                                String facultyIcon = document.getString("facultyIcon");

                                View notificationView = getLayoutInflater().inflate(R.layout.notification_item, null);
                                CircleImageView facultyIconView = notificationView.findViewById(R.id.faculty_icon);
                                TextView notificationMessageView = notificationView.findViewById(R.id.notification_message);
                                TextView notificationTimeView = notificationView.findViewById(R.id.notification_time);

                                notificationMessageView.setText(facultyName + " has " + status + " the request.");

                                Glide.with(this)
                                        .load(facultyIcon)
                                        .error(R.drawable.ic_launcher_background)
                                        .into(facultyIconView);

                                String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
                                notificationTimeView.setText(currentTime);

                                // Add the new notification view to the container
                                notificationContainer.addView(notificationView);
                                notificationCount++;
                            }
                        }

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
            notificationCountTextView.setVisibility(View.VISIBLE);
        } else {
            notificationCountTextView.setVisibility(View.GONE);
        }
    }
}