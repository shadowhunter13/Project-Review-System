package com.example.projectreviewsystem;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Faculty extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private ImageView notificationIcon;
    private TextView notificationCountTextView;
    private BottomSheetDialog requestDialog;
    private List<Request> pendingRequests = new ArrayList<>();
    private int lastNotificationCount = 0;

    private EditText totalPendingEditText;
    private EditText totalProjectsEditText;
    private EditText totalDoneEditText;
    private EditText totalInReviewEditText;
    private Button doneButton;
    private LinearLayout pdfContainer;
    private int pdfCount = 1;
    private int totalProjectsCount = 0;
    private int doneCount = 0;
    private int pendingCount = 0;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty);
        ScrollView scrollView = findViewById(R.id.pdf_scroll_view);
        pdfContainer = findViewById(R.id.pdf_file_list);
        notificationIcon = findViewById(R.id.bell);
        notificationCountTextView = findViewById(R.id.notification_count);
        totalPendingEditText = findViewById(R.id.Total_pending);
        totalProjectsEditText = findViewById(R.id.Total_alloted);
        totalDoneEditText = findViewById(R.id.Total_done);
        doneButton=findViewById(R.id.send_button_1);
        totalInReviewEditText = findViewById(R.id.Total_edit);
        firestore = FirebaseFirestore.getInstance(); // Ensure this is called before using firestore

//        loadPdfFilesiles();
        loadAcceptedRequests();
        updateDashboardCounts();
        listenForCurrentRequest();

        notificationIcon.setOnClickListener(v -> {
            if (!pendingRequests.isEmpty()) {
                Request request = pendingRequests.get(0);
                showRequestDialog(request.getDocId(), request.getDescription(), request.getDeadline(), request.getDocumentUrl());
                pendingRequests.clear();
            } else {
                Toast.makeText(this, "No current requests available.", Toast.LENGTH_SHORT).show();
            }
        });

        doneButton.setOnClickListener(v -> {
            if (pendingCount > 0) {
                doneCount++;
                pendingCount--;
                updateDashboardCounts();
                Toast.makeText(this, "Marked request as done.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No pending requests to mark as done.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadAcceptedRequests() {
        firestore.collection("requests")
                .whereEqualTo("status", "accepted")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            String docId = dc.getDocument().getId();
                            String fileName = dc.getDocument().getString("fileName");
                            String deadline = dc.getDocument().getString("deadline");

                            // Add the accepted PDF entry to the layout
                            addAcceptedPdfEntry(fileName, deadline);
                        }
                    }
                });
    }
    private void loadPdfFiles() {
//        LinearLayout pdfFileList = findViewById(R.id.pdf_file_list);
//        for (int i = 1; i <= 5; i++) {
//            TextView pdfTextView = new TextView(this);
//            pdfTextView.setText("Reviewed PDF File " + i);
//            pdfTextView.setLayoutParams(new LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.MATCH_PARENT,
//                    LinearLayout.LayoutParams.WRAP_CONTENT));
//            pdfFileList.addView(pdfTextView);
//        }
    }

    private void listenForCurrentRequest() {
        firestore.collection("requests")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    pendingRequests.clear();
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            String docId = dc.getDocument().getId();
                            String description = dc.getDocument().getString("description");
                            String deadline = dc.getDocument().getString("deadline");
                            String documentUrl = dc.getDocument().getString("fileUri");

                            pendingRequests.add(new Request(docId, description, deadline, documentUrl));
                            pendingCount++;
                        }
                    }

                    Log.d("Faculty", "Pending requests count: " + pendingRequests.size());
                    lastNotificationCount = pendingRequests.size();
                    updateNotificationCount(lastNotificationCount);
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

    private void showRequestDialog(String docId, String description, String deadline, String documentUrl) {
        requestDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.request_dialog, null);
        requestDialog.setContentView(dialogView);

        TextView descriptionTextView = dialogView.findViewById(R.id.text_description);
        TextView deadlineTextView = dialogView.findViewById(R.id.text_deadline);
        Button openDocumentButton = dialogView.findViewById(R.id.button_open_document);
        Button acceptButton = dialogView.findViewById(R.id.button_accept);
        Button rejectButton = dialogView.findViewById(R.id.button_reject);

        descriptionTextView.setText(description);
        deadlineTextView.setText(deadline);

        openDocumentButton.setOnClickListener(v -> openDocument(documentUrl));
        acceptButton.setOnClickListener(v -> updateRequestStatus(docId, "accepted", deadline));
        rejectButton.setOnClickListener(v -> updateRequestStatus(docId, "rejected", null));

        requestDialog.show();
    }


    private void openDocument(String documentUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(documentUrl), "*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No application available to open this document.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateRequestStatus(String docId, String status, @Nullable String deadline) {
        firestore.collection("requests").document(docId)
                .get() // Fetch the document to get the file name
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fileName = documentSnapshot.getString("fileName"); // Get the file name
                        firestore.collection("requests").document(docId)
                                .update("status", status)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(Faculty.this, "Request " + status, Toast.LENGTH_SHORT).show();
                                    if (requestDialog != null && requestDialog.isShowing()) {
                                        requestDialog.dismiss();
                                    }

                                    if (status.equals("accepted")) {
                                        // Update counts
                                        doneCount++;
                                        pendingCount--;
                                        // Add the accepted PDF entry with the file name and deadline
                                        addAcceptedPdfEntry(fileName, deadline); // Pass the file name instead of docId
                                    } else if (status.equals("rejected")) {
                                        pendingCount--;
                                    }

                                    updateDashboardCounts();
                                    updateNotificationCount(lastNotificationCount);
                                    notifyAdminOfResponse(docId, status);
                                })
                                .addOnFailureListener(e -> Toast.makeText(Faculty.this, "Failed to update request", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(Faculty.this, "Failed to retrieve document", Toast.LENGTH_SHORT).show());
    }

    private void addAcceptedPdfEntry(String pdfName, String deadline) {
        // Create a new LinearLayout for the PDF entry
        LinearLayout pdfEntry = new LinearLayout(this);
        pdfEntry.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        pdfEntry.setOrientation(LinearLayout.HORIZONTAL);
        pdfEntry.setPadding(16, 16, 16, 16);

        // Create the vertical LinearLayout for the name, timer, and deadline date
        LinearLayout pdfInfo = new LinearLayout(this);
        pdfInfo.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1)); // Weight of 1 to take up available space
        pdfInfo.setOrientation(LinearLayout.VERTICAL);

        // Create TextView for PDF name
        TextView pdfNameTextView = new TextView(this);
        pdfNameTextView.setText(pdfName);
        pdfNameTextView.setTextColor(getResources().getColor(R.color.colorOnBackground));
        pdfNameTextView.setTextSize(16);
        pdfNameTextView.setVisibility(View.VISIBLE);

        // Create TextView for timer
        TextView timerTextView = new TextView(this);
        timerTextView.setText("00:00:00"); // Initial timer text
        timerTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        timerTextView.setTextSize(14);

        // Create TextView for deadline date
        TextView deadlineTextView = new TextView(this);
        deadlineTextView.setText(formatDeadline(deadline)); // Format the deadline date
        deadlineTextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        deadlineTextView.setTextSize(14);

        // Add the name, timer, and deadline to the vertical layout
        pdfInfo.addView(pdfNameTextView);
        pdfInfo.addView(timerTextView); // Add timer above the deadline
        pdfInfo.addView(deadlineTextView); // Add the deadline TextView below the timer

        // Create the Send button
        Button sendButton = new Button(this);
        sendButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        sendButton.setText("Send");
        sendButton.setTextColor(getResources().getColor(R.color.colorPrimaryDark));

        // Set an onClickListener for the send button
        sendButton.setOnClickListener(v -> {
            // Implement the send functionality here
            Toast.makeText(Faculty.this, "Send functionality not implemented yet.", Toast.LENGTH_SHORT).show();
        });

        // Add the vertical layout and button to the horizontal layout
        pdfEntry.addView(pdfInfo); // Add the name, timer, and deadline layout
        pdfEntry.addView(sendButton); // Add the send button

        // Add the new PDF entry to the container
        pdfContainer.addView(pdfEntry);

        // Start the countdown timer based on the deadline
        startCountdownTimer(deadline, timerTextView);
    }
    private String formatDeadline(String deadline) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()); // Change to your desired format
            return outputFormat.format(inputFormat.parse(deadline)); // Return formatted date
        } catch (ParseException e) {
            e.printStackTrace();
            return "Invalid deadline";
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

                        // Update the timerTextView to show days, hours, minutes, and seconds
                        timerTextView.setText(String.format("%d days %02d:%02d:%02d", days, hours, minutes, seconds));
                    }

                    @Override
                    public void onFinish() {
                        timerTextView.setText("00:00:00"); // Reset timer when finished
                        // Optionally remove the PDF entry or update its status
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

    private void updateDashboardCounts() {
        totalPendingEditText.setText(String.valueOf(pendingCount));
        totalProjectsEditText.setText(String.valueOf(totalProjectsCount));
        totalDoneEditText.setText(String.valueOf(doneCount));
        totalInReviewEditText.setText(String.valueOf(pendingRequests.size()));
    }

    private void notifyAdminOfResponse(String docId, String status) {
        Map<String, Object> responseNotification = new HashMap<>();
        responseNotification.put("requestId", docId);
        responseNotification.put("status", status);
        responseNotification.put("facultyName", "Faculty Name");
        responseNotification.put("facultyIcon", "Faculty Icon URL");

        firestore.collection("admin_notifications").add(responseNotification)
                .addOnSuccessListener(aVoid -> {
                    // Optionally handle success
                })
                .addOnFailureListener(e -> Toast.makeText(Faculty.this, "Failed to notify admin", Toast.LENGTH_SHORT).show());
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

