package com.example.projectreviewsystem;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class Faculty extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private ImageView notificationTextView;
    private BottomSheetDialog requestDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty);

        firestore = FirebaseFirestore.getInstance();
        notificationTextView = findViewById(R.id.bell); // Assume this is the bell icon

        listenForRequests();

        notificationTextView.setOnClickListener(v -> {
            if (requestDialog != null && requestDialog.isShowing()) {
                requestDialog.dismiss();
            } else {
                Toast.makeText(this, "No requests available.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForRequests() {
        firestore.collection("requests")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return; // Added null check

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            String docId = dc.getDocument().getId();
                            String description = dc.getDocument().getString("description");
                            String deadline = dc.getDocument().getString("deadline");
                            String documentUrl = dc.getDocument().getString("documentUrl"); // Assuming the URL is stored

                            showRequestDialog(docId, description, deadline, documentUrl);
                        }
                    }
                });
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

        // Set up the button to open the document
        openDocumentButton.setOnClickListener(v -> openDocument(documentUrl));

        acceptButton.setOnClickListener(v -> updateRequestStatus(docId, "accepted"));
        rejectButton.setOnClickListener(v -> updateRequestStatus(docId, "rejected"));

        requestDialog.show();
    }

    private void openDocument(String documentUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(documentUrl), "*/*"); // Use "*/*" to allow any type of document
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
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Faculty.this, "Failed to update request", Toast.LENGTH_SHORT).show();
                });
    }
}
