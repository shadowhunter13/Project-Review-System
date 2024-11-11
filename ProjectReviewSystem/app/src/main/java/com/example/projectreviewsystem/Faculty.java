package com.example.projectreviewsystem;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private int totalProjectsCount = 0;
    private int doneCount = 0;
    private int pendingCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty);

        firestore = FirebaseFirestore.getInstance();
        notificationIcon = findViewById(R.id.bell);
        notificationCountTextView = findViewById(R.id.notification_count);
        doneButton = findViewById(R.id.done_button);

        totalPendingEditText = findViewById(R.id.Total_pending);
        totalProjectsEditText = findViewById(R.id.Total_alloted);
        totalDoneEditText = findViewById(R.id.Total_done);
        totalInReviewEditText = findViewById(R.id.Total_edit);

        loadPdfFiles();
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

    private void loadPdfFiles() {
        LinearLayout pdfFileList = findViewById(R.id.pdf_file_list);
        for (int i = 1; i <= 5; i++) {
            TextView pdfTextView = new TextView(this);
            pdfTextView.setText("Reviewed PDF File " + i);
            pdfTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            pdfFileList.addView(pdfTextView);
        }
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
        acceptButton.setOnClickListener(v -> updateRequestStatus(docId, "accepted"));
        rejectButton.setOnClickListener(v -> updateRequestStatus(docId, "rejected"));

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

    private void updateRequestStatus(String docId, String status) {
        firestore.collection("requests").document(docId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Faculty.this, "Request " + status, Toast.LENGTH_SHORT).show();
                    if (requestDialog != null && requestDialog.isShowing()) {
                        requestDialog.dismiss();
                    }

                    if (status.equals("accepted")) {
                        doneCount++;
                        pendingCount--;
                    } else if (status.equals("rejected")) {
                        pendingCount--;
                    }

                    updateDashboardCounts();
                    updateNotificationCount(lastNotificationCount);
                    notifyAdminOfResponse(docId, status);
                })
                .addOnFailureListener(e -> Toast.makeText(Faculty.this, "Failed to update request", Toast.LENGTH_SHORT).show());
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
