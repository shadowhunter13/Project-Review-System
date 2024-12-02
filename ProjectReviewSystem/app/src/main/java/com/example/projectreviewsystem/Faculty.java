package com.example.projectreviewsystem;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Locale;
import java.util.Map;

public class Faculty extends AppCompatActivity implements ReviewedPdfDialogFragment.OnStatusSelectedListener, ReviewedPdfDialogFragment.OnRequestSentListener {

    private FirebaseFirestore firestore;
    private ImageView notificationIcon;
    private TextView notificationCountTextView;
    private BottomSheetDialog requestDialog;
    private List<Request> pendingRequests = new ArrayList<>();
    private int lastNotificationCount = 0;
    private int totalProjects = 0; // Declare totalProjects variable
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 1;
    private static final String PREFS_NAME = "DashboardCounts";
    private static final String KEY_DONE_COUNT = "doneCount";
    private static final String KEY_PENDING_COUNT = "pendingCount";
    private static final String KEY_IN_REVIEW_COUNT = "inReviewCount";
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DatabaseReference databaseReference; // Reference to the Realtime Database


    private EditText totalPendingEditText;
    private EditText totalProjectsEditText;
    private EditText totalDoneEditText;
    private EditText totalInReviewEditText;
    private Button doneButton;
    private SharedPreferences sharedPreferences;
    private LinearLayout pdfContainer;
    private Map<String, View> acceptedPdfEntries = new HashMap<>();
    private int doneCount = 0;
    private int pendingCount = 0;
    private int totalInReviewCount = 0;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty);

        // Initialize SharedPreferences
//        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        databaseReference = FirebaseDatabase.getInstance().getReference("researchers"); // Reference to the "researchers" node


        // Initialize views
        initializeViews();
        firestore = FirebaseFirestore.getInstance();
        String userName = getIntent().getStringExtra("USER_NAME");
        listenForCurrentRequest(userName);
        loadAcceptedRequests(userName);
//        loadCountsFromPreferences();
        updateDashboardCounts();


        // Set up notification icon click listener
        notificationIcon.setOnClickListener(v -> handleNotificationIconClick(userName));

        // Set up done button click listener
        doneButton.setOnClickListener(v -> handleDoneButtonClick());
    }

    private void initializeViews() {
        ScrollView scrollView = findViewById(R.id.pdf_scroll_view);
        pdfContainer = findViewById(R.id.pdf_file_list);
        notificationIcon = findViewById(R.id.bell);
        notificationCountTextView = findViewById(R.id.notification_count);
        totalPendingEditText = findViewById(R.id.Total_pending);
        totalProjectsEditText = findViewById(R.id.Total_alloted);
        totalDoneEditText = findViewById(R.id.Total_done);
        doneButton = findViewById(R.id.send_button_1);
        totalInReviewEditText = findViewById(R.id.Total_edit);
    }

//    private void loadCountsFromPreferences() {
//        doneCount = sharedPreferences.getInt(KEY_DONE_COUNT, 0);
//        pendingCount = sharedPreferences.getInt(KEY_PENDING_COUNT, 0);
//        totalInReviewCount = sharedPreferences.getInt(KEY_IN_REVIEW_COUNT, 0);
//    }
//
//    private void saveCountsToPreferences() {
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putInt(KEY_DONE_COUNT, doneCount);
//        editor.putInt(KEY_PENDING_COUNT, pendingCount);
//        editor.putInt(KEY_IN_REVIEW_COUNT, totalInReviewCount);
//        editor.apply();
//    }

    private void handleNotificationIconClick(String userName) {
        if (!pendingRequests.isEmpty()) {
            Request request = pendingRequests.get(0);
            showRequestDialog(userName,request.getDocId(), request.getDescription(), request.getDeadline(), request.getDocumentUrl());
            pendingRequests.remove(0); // Remove the request after handling it
        } else {
            Toast.makeText(this, "No current requests available.", Toast.LENGTH_SHORT).show();
        }
    }
    private void handleDoneButtonClick() {
        if (!pendingRequests.isEmpty()) {
            Request request = pendingRequests.get(0);
            showReviewedPdfDialog(request.getDocId(), request.getDescription(), request.getDeadline());
            // Optionally, you might want to remove the request from pendingRequests instead of clearing it
            pendingRequests.remove(0);
        } else {
            Toast.makeText(this, "No pending requests to mark as done.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showReviewedPdfDialog(String docId, String description, String deadline) {
        ReviewedPdfDialogFragment dialogFragment = new ReviewedPdfDialogFragment();
        Bundle args = new Bundle();
        args.putString("docId", docId);
        args.putString("description", description);
        args.putString("deadline", deadline);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "ReviewedPdfDialog");
    }
    @Override
    public void onStatusSelected(String status, long updatedDoneCount , long updatedInReviewCount, long updatedTotalProjectsCount){
        Log.d("Faculty", "Status selected: " + status);

        this.doneCount = (int) updatedDoneCount;

        if (this.totalInReviewCount > 0) {
            this.totalInReviewCount--;
        }

        if (this.totalProjects > 0) {
            this.totalProjects--; // Decrement total projects count
        }

        // Update the dashboard counts (if you still want to show the updated done count)
        updateDashboardCounts();

        // Log the updated counts for debugging
        Log.d("Faculty", "Updated counts - Done: " + this.doneCount +
                ", In Review: " + this.totalInReviewCount +
                ", Total Projects: " + this.totalProjects);
    }
    @Override
    public void onRequestSent(String docId) {
        // Handle request sent logic if needed
    }

    private void loadAcceptedRequests(String userName) {
//        FirebaseUser  user = FirebaseAuth.getInstance().getCurrentUser ();
//        if (user == null) {
//            Toast.makeText(this, "You need to be logged in to access this feature.", Toast.LENGTH_SHORT).show();
//            return; // Exit if the user is not authenticated
//        }
//
//        firestore.collection("requests")
//                .whereEqualTo("status", "accepted")
//                .addSnapshotListener((snapshots, e) -> {
//                    if (e != null || snapshots == null) return;
//
//                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
//                        if (dc.getType() == DocumentChange.Type.ADDED) {
//                            String docId = dc.getDocument().getId();
//                            String fileName = dc.getDocument().getString("fileName");
//                            String deadline = dc.getDocument().getString("deadline");
//                            String documentUrl = dc.getDocument().getString("fileUri");
//
//                            if (!acceptedPdfEntries.containsKey(docId)) {
//                                addAcceptedPdfEntry(fileName, deadline, docId, documentUrl);
//                            }
//                        }
//                    }
//                });
        Log.d("aman", "username: "+ userName);
        String[] s = userName.split(" ");
        String uniqueId = s[0];
        databaseReference.child(uniqueId+"/projects") // Reference to the "researchers" node in the Realtime Database
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            int facultyCount = 1;
                            for (DataSnapshot facultySnapshot : dataSnapshot.getChildren()) {
                                // Extract faculty details
                                String userId = facultySnapshot.getKey();
//                                DataSnapshot projectSnapshot = facultySnapshot.child("projects").child("uhgb");
                                String fileName = facultySnapshot.child("title").getValue(String.class);
                                String deadline =  facultySnapshot.child("deadline").getValue(String.class);
                                String project = facultySnapshot.child("projectUrl").getValue(String.class);

                                String status = facultySnapshot.child("status").getValue(String.class);
                                if(status.equals("accepted")){
                                    Log.e("aman_log", status);
                                    addAcceptedPdfEntry(fileName, deadline, userId, project);
                                }
                                else {
                                    Log.d("aman-log", "No record found");
                                }
//                                if (!acceptedPdfEntries.containsKey(userId)) {
//                               addAcceptedPdfEntry(fileName, deadline, userId, project);
//                                }
                            }
                        } else {
                            Log.e("AdminActivity", "No faculty data found.");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("AdminActivity", "Error fetching faculty data: ", databaseError.toException());
                    }
                });
    }

    private void listenForCurrentRequest(String userName) {
        String[] s = userName.split(" ");
        String uniqueId = s[0];
        databaseReference.child(uniqueId+"/projects")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {


                            pendingRequests.clear(); // Clear the existing list to avoid duplicates
                            pendingCount = 0; // Reset pending count

                            for (DataSnapshot requestSnapshot : dataSnapshot.getChildren()) {
                                String status= requestSnapshot.child("status").getValue(String.class);
                                if(status.equals("pending")){
                                    String docId = requestSnapshot.getKey();
                                    String description = requestSnapshot.child("description").getValue(String.class);
                                    String deadline = requestSnapshot.child("deadline").getValue(String.class);
                                    String documentUrl = requestSnapshot.child("fileUri").getValue(String.class);

                                    // Create a new Request object and add it to the list
                                    Request newRequest = new Request(docId, description, deadline, documentUrl);
                                    pendingRequests.add(newRequest); // Add to the pendingRequests list
                                    pendingCount++; // Increment pending count
                                    lastNotificationCount++; // Increment notification count
                                    updateNotificationCount(lastNotificationCount);
                                    totalPendingEditText.setText(String.valueOf(pendingCount));
                                    Log.d("Faculty", "Pending requests updated. Total: " + pendingCount);
                                }


                            }


                        } else {
                            Log.d("Faculty", "No pending requests found.");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("Faculty", "Error fetching pending requests: ", databaseError.toException());
                    }
                });
    }

    private void updateNotificationCount(int count) {
        Log.d("Faculty", "Updating notification count: " + count);
        if (count > 0) {
            notificationCountTextView.setVisibility(View.VISIBLE);
            notificationCountTextView.setText(String.valueOf(count));
        } else {
            notificationCountTextView.setVisibility(View.GONE);
        }
    }

    private void showRequestDialog(String userName ,String docId, String description, String deadline, String documentUrl) {
        requestDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.request_dialog, null);
        requestDialog.setContentView(dialogView);

        TextView descriptionTextView = dialogView.findViewById(R.id.text_description);
        TextView deadlineTextView = dialogView.findViewById(R.id.text_deadline);
        Button acceptButton = dialogView.findViewById(R.id.button_accept);
        Button rejectButton = dialogView.findViewById(R.id.button_reject);

        descriptionTextView.setText(description);
        deadlineTextView.setText(deadline);

        String[] s = userName.split(" ");
        String userId = s[0];
//        descriptionTextView.setOnClickListener(v -> acceptRequest(docId));
        acceptButton.setOnClickListener(v -> updateRequestStatus(userId ,docId, "accepted", deadline));
        Log.e("aman1", userId+docId);
        rejectButton.setOnClickListener(v -> updateRequestStatus(userId, docId, "rejected", null));

        requestDialog.show();
    }
    private void acceptRequest(String documentUrl) {
        // Retrieve the document URL from Firestore
//        db.collection("requests").document(requestId)
//                .get()
//                .addOnSuccessListener(documentSnapshot -> {
//                    if (documentSnapshot.exists()) {
//                        // Assuming the URL is stored in a field called "fileUrl"
//                        String documentUrl = documentSnapshot.getString("fileUrl");
//                        if (documentUrl != null && !documentUrl.isEmpty()) {
//                            // Convert the string URL to a Uri
//                            // Convert the string URL to a Uri
//                            Uri documentUri = Uri.parse(documentUrl);
//                            openDocumentWithUri(documentUri); // Directly open the document
//                        } else {
//                            Toast.makeText(this, "No document URL found.", Toast.LENGTH_SHORT).show();
//                        }
//                    } else {
//                        Toast.makeText(this, "Request not found.", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.e("Firestore", "Error getting document: ", e);
//                    Toast.makeText(this, "Error retrieving document URL.", Toast.LENGTH_SHORT).show();
//                });
        if (documentUrl != null && !documentUrl.isEmpty()) {
            // Convert the string URL to a Uri
            // Convert the string URL to a Uri
            Uri documentUri = Uri.parse(documentUrl);
            Toast.makeText(this, documentUrl.toString(), Toast.LENGTH_LONG).show();
            Log.d("uri","uri: "+documentUrl.toString());
            openDocumentWithUri(documentUri); // Directly open the document
        } else {
            Toast.makeText(this, "No document URL found.", Toast.LENGTH_SHORT).show();
        }
    }
//        private void openDocumentPicker() {
//            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//            intent.addCategory(Intent.CATEGORY_OPENABLE);
//            intent.setType("*/*"); // You can specify a specific MIME type if needed
//            startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT);
//        }

        // This method will be called when the user selects a document
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_CODE_OPEN_DOCUMENT && resultCode == RESULT_OK) {
                if (data != null) {
                    Uri documentUri = data.getData(); // Get the URI of the selected document
                    openDocumentWithUri(documentUri);
                } else {
                    Toast.makeText(this, "Invalid document URI.", Toast.LENGTH_SHORT).show();
                }
            }
        }

        // Method to open the document with the given URI
        private void openDocumentWithUri(Uri documentUri) {
            if (documentUri == null) {
                Toast.makeText(this, "Invalid document URI.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(documentUri, "application/pdf"); // Change MIME type based on the file type
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Create a chooser to let the user select an application to open the document
            Intent chooser = Intent.createChooser(intent, "Open document with");

            // Check if there is an application that can handle the intent
            try {
                startActivity(chooser);
            } catch (ActivityNotFoundException e) {
                Log.e("Faculty", "ActivityNotFoundException: " + e.getMessage());
                Toast.makeText(this, "No application available to open this document.", Toast.LENGTH_SHORT).show();
            } catch (SecurityException e) {
                Log.e("Faculty", "SecurityException: " + e.getMessage());
                Toast.makeText(this, "Permission denied to access this document.", Toast.LENGTH_SHORT).show();
            }
        }

      @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == 100) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, you can now open the document
                    // You may want to call openDocument again if needed
                    // For example, you can store the document URL in a variable and call openDocument(documentUrl, true);
                } else {
                    Toast.makeText(this, "Permission denied to read external storage.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    private void updateRequestStatus(String userName, String docId, String status, @Nullable String deadline) {
        Log.d("Faculty", "Attempting to update request status for Document ID: " + docId + " with deadline: " + deadline);

        DatabaseReference requestRef = databaseReference.child(userName+"/projects/"+docId);
        Log.e("alokik", userName+"/projects/"+docId );

        requestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d("Faculty", "Document found: " + docId);
                    String currentStatus = dataSnapshot.child("status").getValue(String.class);
                    if (!currentStatus.equals(status)) {
                        requestRef.child("status").setValue(status)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Faculty", "Successfully updated Document ID: " + docId + " to status: " + status);
                                    Toast.makeText(Faculty.this, "Request " + status, Toast.LENGTH_SHORT).show();

                                    // Remove the request from pendingRequests
                                    removePendingRequest(docId);

                                    // Decrement pending count only if the request is accepted or rejected
                                    if (status.equals("accepted") || status.equals("rejected")) {
                                        pendingCount--; // Decrement pending count
                                        totalPendingEditText.setText(String.valueOf(pendingCount)); // Update the EditText
                                        lastNotificationCount--; // Decrement notification count
                                        updateNotificationCount(lastNotificationCount); // Update notification count
                                    }

                                    if (status.equals("accepted")) {
                                        totalInReviewCount++;
                                        totalProjects++;
                                    }

                                    updateDashboardCounts();
                                    // Optionally notify the admin of the response
//                                    notifyAdminOfResponse(docId, status);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Faculty", "Failed to update request status: " + e.getMessage());
                                    Toast.makeText(Faculty.this, "Failed to update request status. Please try again.", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.d("Faculty", "Document ID: " + docId + " already has status: " + status);
                    }
                } else {
                    Log.e("Faculty", "Document does not exist: " + docId);
                    Toast.makeText(Faculty.this, "Request not found. Please refresh and try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Faculty", "Failed to retrieve document: " + databaseError.getMessage());
                Toast.makeText(Faculty.this, "Failed to retrieve request. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void removePendingRequest(String docId) {
        boolean removed = pendingRequests.removeIf(request -> request.getDocId().equals(docId));
        if (removed) {
            Log.d("Faculty", "Removed pending request: " + docId);
        } else {
            Log.d("Faculty", "Request not found in pending list: " + docId);
        }
    }

    private void addAcceptedPdfEntry(String pdfName, String deadline, String docId, String status) {
        LinearLayout pdfEntry = new LinearLayout(this);
        pdfEntry.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        pdfEntry.setOrientation(LinearLayout.HORIZONTAL);
        pdfEntry.setPadding(16, 16, 16, 16);

        LinearLayout pdfInfo = new LinearLayout(this);
        pdfInfo.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1));
        pdfInfo.setOrientation(LinearLayout.VERTICAL);

        TextView pdfNameTextView = new TextView(this);
        pdfNameTextView.setText(pdfName);
        pdfNameTextView.setTextColor(getResources().getColor(R.color.colorOnBackground));
        pdfNameTextView.setTextSize(16);
        pdfNameTextView.setVisibility(View.VISIBLE);

        pdfNameTextView.setOnClickListener(v -> {
//            acceptRequest(documentUrl);
            fetchAndOpenPdf(docId);
        });

        TextView timerTextView = new TextView(this);
        timerTextView.setText("00:00:00");
        timerTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        timerTextView.setTextSize(14);

        TextView deadlineTextView = new TextView(this);
        if (deadline != null) {
            deadlineTextView.setText(formatDeadline(deadline));
        } else {
            deadlineTextView.setText("No deadline set");
        }
        deadlineTextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        deadlineTextView.setTextSize(14);

        pdfInfo.addView(pdfNameTextView);
        pdfInfo.addView(timerTextView);
        pdfInfo.addView(deadlineTextView);

        Button sendButton = new Button(this);
        sendButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        sendButton.setText("Send");
        sendButton.setTextColor(getResources().getColor(R.color.colorPrimaryDark));

        sendButton.setOnClickListener(v -> {
            // Open the ReviewedPdfDialogFragment when the send button is clicked
            showReviewedPdfDialog(docId, pdfName, deadline);
        });

        pdfEntry.addView(pdfInfo);
        pdfEntry.addView(sendButton);
        pdfContainer.addView(pdfEntry);
        acceptedPdfEntries.put(docId, pdfEntry);
        startCountdownTimer(deadline, timerTextView);
    }

    // added new for 439
    private void fetchAndOpenPdf(String reqId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("requests").document(reqId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String url = document.getString("fileUri");
                            Log.w("Url-aman", url);
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Document does not exist", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to fetch PDF: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private String formatDeadline(String deadline) {
        if (deadline == null) {
            return "No deadline set"; // Handle null case
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(deadline));
        } catch (ParseException e) {
            e.printStackTrace();
            return "Invalid deadline"; // Handle parsing error
        }
    }

    private void startCountdownTimer(String deadline, TextView timerTextView) {
        if (deadline == null) {
            timerTextView.setText("Invalid deadline");
            return;
        }

        try {
            long endTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(deadline).getTime();
            long currentTime = System.currentTimeMillis();
            long timeLeft = endTime - currentTime;

            if (timeLeft > 0) {
                CountDownTimer countDownTimer = new CountDownTimer(timeLeft, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        int days = (int) (millisUntilFinished / (1000 * 60 * 60 * 24));
                        int hours = (int) (millisUntilFinished % (1000 * 60 * 60 * 24) / (1000 * 60 * 60));
                        int minutes = (int) ((millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60));
                        int seconds = (int) (millisUntilFinished % (1000 * 60) / 1000);
                        timerTextView.setText(String.format("%d days %02d:%02d:%02d", days, hours, minutes, seconds));
                    }

                    @Override
                    public void onFinish() {
                        timerTextView.setText("00:00:00");
                    }
                }.start();
            } else {
                timerTextView.setText("Deadline has passed");
            }
        } catch (ParseException e) {
            e.printStackTrace();
            timerTextView.setText("Invalid deadline");
        }
    }
//    private void notifyAdminOfResponse(String docId, String status) {
//        // Get the current user's ID
//        String facultyId = FirebaseAuth.getInstance().getCurrentUser ().getUid(); // Fetch the current user's ID
//
//        // Fetch faculty details from the "faculty" collection using the faculty ID
//        firestore.collection("faculty").document(facultyId)
//                .get()
//                .addOnSuccessListener(documentSnapshot -> {
//                    if (documentSnapshot.exists()) {
//                        String facultyName = documentSnapshot.getString("name");
//                        String facultyIcon = documentSnapshot.getString("iconUrl");
//
//                        // Prepare the notification data
//                        Map<String, Object> responseNotification = new HashMap<>();
//                        responseNotification.put("requestId", docId);
//                        responseNotification.put("status", status);
//                        responseNotification.put("facultyName", facultyName); // Use actual faculty name
//                        responseNotification.put("facultyIcon", facultyIcon); // Use actual faculty icon URL
//
//                        // Send notification to admin
//                        firestore.collection("admin_notifications").add(responseNotification)
//                                .addOnSuccessListener(aVoid -> {
//                                    Log.d("Faculty", "Notification sent to admin for Document ID: " + docId + " with status: " + status);
//                                })
//                                .addOnFailureListener(e -> Toast.makeText(Faculty.this, "Failed to notify admin", Toast.LENGTH_SHORT).show());
//                    } else {
//                        Log.e("Faculty", "Faculty document does not exist.");
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.e("Faculty", "Failed to retrieve faculty details: " + e.getMessage());
//                });
//    }


//    @Override
//    public void onStatusSelected(String status, String comments) {
//        Log.d("Faculty", "Status selected: " + status);
//
//        // Check if there are any pending requests
//        if (!pendingRequests.isEmpty()) {
//            Request request = pendingRequests.get(0);
//            String deadline = request.getDeadline();
//
//            // Update the request status with the document ID, status, and deadline
//            updateRequestStatus(request.getDocId(), status, deadline);
//            Log.d("Faculty", "Before update - Done: " + doneCount + ", In Review: " + totalInReviewCount);
//
//            // Update the counts based on the selected status
//            if (status.equals("accepted")) {
//                doneCount++; // Increment done count
//                totalInReviewCount--; // Decrement in review count
//            } else if (status.equals("rejected")) {
//                doneCount++;
//                totalInReviewCount--; // Decrement in review count
//            }
//
//            // Save the updated counts to SharedPreferences
//            saveCountsToPreferences();
//            Log.d("Faculty", "After update - Done: " + doneCount + ", In Review: " + totalInReviewCount);
//
//            // Update the dashboard counts
//            updateDashboardCounts();
//        } else {
//            Log.w("Faculty", "No pending requests available to update.");
//            Toast.makeText(this, "No pending requests available to update.", Toast.LENGTH_SHORT).show();
//        }
//    }
private void updateDashboardCounts() {
    totalPendingEditText.setText(String.valueOf(pendingCount));
    totalProjectsEditText.setText(String.valueOf(totalProjects)); // Update to show current total projects
    totalDoneEditText.setText(String.valueOf(doneCount)); // Show updated done count
    totalInReviewEditText.setText(String.valueOf(totalInReviewCount)); // Total in review
}


    public class Request {
        private String docId;
        private String description;
        private String deadline;
        private String documentUrl;

        public Request(String docId, String description, String deadline, String documentUrl) {
            this.docId = docId;
            this.description = description;
            this.deadline = deadline;
            this.documentUrl = documentUrl;
        }

        public String getDocId() {
            return docId;
        }

        public String getDescription() {
            return description;
        }

        public String getDeadline() {
            return deadline;
        }

        public String getDocumentUrl() {
            return documentUrl;
        }
    }
}