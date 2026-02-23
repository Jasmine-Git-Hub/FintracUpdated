package com.example.fintrac;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JarFirestoreManager {
    private static final String TAG = "JarFirestoreManager";
    private static final String COLLECTION_JARS = "jars";
    private static final String COLLECTION_TRANSACTIONS = "transactions";
    private static final String COLLECTION_USERS = "users";

    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;
    private final CollectionReference jarsCollection;
    private final CollectionReference transactionsCollection;
    private final CollectionReference usersCollection;

    public JarFirestoreManager() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        jarsCollection = db.collection(COLLECTION_JARS);
        transactionsCollection = db.collection(COLLECTION_TRANSACTIONS);
        usersCollection = db.collection(COLLECTION_USERS);
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    // Create new jar - Properly handles user ID
    public void createJar(JarFirestoreModel jarModel, FirestoreCallback<DocumentReference> callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        String userId = currentUser.getUid();

        // Generate ID if not set
        if (jarModel.getId() == null || jarModel.getId().isEmpty()) {
            jarModel.setId(jarsCollection.document().getId());
        }

        // Set createdBy
        jarModel.setCreatedBy(userId);
        jarModel.setCreatedAt(com.google.firebase.Timestamp.now());

        // Initialize participants list
        List<String> participants = jarModel.getParticipants();
        if (participants == null || participants.isEmpty()) {
            participants = new ArrayList<>();
            participants.add(userId);
            jarModel.setParticipants(participants);
        } else if (!participants.contains(userId)) {
            participants.add(userId);
        }

        // Initialize contributions
        Map<String, Double> contributions = jarModel.getParticipantContributions();
        if (contributions == null || contributions.isEmpty()) {
            contributions = new HashMap<>();
            contributions.put(userId, jarModel.getSavedAmount());
            jarModel.setParticipantContributions(contributions);
        } else if (!contributions.containsKey(userId)) {
            contributions.put(userId, jarModel.getSavedAmount());
        }

        // Initialize transactions list
        if (jarModel.getTransactions() == null) {
            jarModel.setTransactions(new ArrayList<>());
        }

        // Set daysLeft based on type and deadline
        if (jarModel.getDaysLeft() == null || jarModel.getDaysLeft().isEmpty()) {
            jarModel.setDaysLeft("New");
        }

        DocumentReference jarRef = jarsCollection.document(jarModel.getId());

        jarRef.set(jarModel)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Jar created with ID: " + jarModel.getId());

                    // Get user's full name from Firestore
                    usersCollection.document(userId).get()
                            .addOnSuccessListener(userDoc -> {
                                String userName = getUserDisplayName(userDoc, currentUser);

                                // Create transaction record with proper name
                                JarTransaction transaction = new JarTransaction(
                                        jarModel.getId(),
                                        userId,
                                        userName,
                                        jarModel.getSavedAmount(),
                                        "CREATE",
                                        "Created jar: " + jarModel.getName()
                                );
                                transaction.setJarName(jarModel.getName());

                                // Add transaction to separate collection
                                addTransactionToCollection(jarModel.getId(), jarModel.getName(), transaction);
                            })
                            .addOnFailureListener(e -> {
                                // Fallback to basic name
                                JarTransaction transaction = new JarTransaction(
                                        jarModel.getId(),
                                        userId,
                                        getUserName(currentUser),
                                        jarModel.getSavedAmount(),
                                        "CREATE",
                                        "Created jar: " + jarModel.getName()
                                );
                                transaction.setJarName(jarModel.getName());
                                addTransactionToCollection(jarModel.getId(), jarModel.getName(), transaction);
                            });

                    callback.onSuccess(jarRef);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating jar", e);
                    callback.onFailure(e);
                });
    }

    // Get all jars for current user
    public void getAllJars(FirestoreCallback<List<JarFirestoreModel>> callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        String userId = currentUser.getUid();

        jarsCollection
                .whereArrayContains("participants", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<JarFirestoreModel> jarList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                JarFirestoreModel jar = document.toObject(JarFirestoreModel.class);
                                if (jar.getId() == null || jar.getId().isEmpty()) {
                                    jar.setId(document.getId());
                                }
                                jarList.add(jar);
                            } catch (Exception e) {
                                Log.e(TAG, "Error converting document to jar", e);
                            }
                        }

                        jarList.sort((j1, j2) -> {
                            if (j1.getCreatedAt() == null && j2.getCreatedAt() == null) return 0;
                            if (j1.getCreatedAt() == null) return 1;
                            if (j2.getCreatedAt() == null) return -1;
                            return j2.getCreatedAt().compareTo(j1.getCreatedAt());
                        });

                        callback.onSuccess(jarList);
                    } else {
                        Log.e(TAG, "Error getting jars", task.getException());
                        callback.onFailure(task.getException());
                    }
                });
    }

    // Add money to jar
    public void addMoney(String jarId, double amount, String note,
                         FirestoreCallback<Void> callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        String userId = currentUser.getUid();
        DocumentReference jarRef = jarsCollection.document(jarId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(jarRef);
            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException(
                        "Jar not found",
                        FirebaseFirestoreException.Code.NOT_FOUND
                );
            }

            JarFirestoreModel jar = snapshot.toObject(JarFirestoreModel.class);
            if (jar == null) {
                throw new FirebaseFirestoreException(
                        "Failed to parse jar data",
                        FirebaseFirestoreException.Code.UNKNOWN
                );
            }

            double newSavedAmount = jar.getSavedAmount() + amount;
            transaction.update(jarRef, "savedAmount", newSavedAmount);

            Map<String, Double> contributions = jar.getParticipantContributions();
            if (contributions == null) {
                contributions = new HashMap<>();
            }

            Double currentContribution = contributions.get(userId);
            double contributionValue = (currentContribution != null) ? currentContribution : 0.0;
            contributions.put(userId, contributionValue + amount);
            transaction.update(jarRef, "participantContributions", contributions);

            List<String> participants = jar.getParticipants();
            if (participants == null) {
                participants = new ArrayList<>();
            }
            if (!participants.contains(userId)) {
                participants.add(userId);
                transaction.update(jarRef, "participants", participants);
            }

            return jar;
        }).addOnSuccessListener(jar -> {
            usersCollection.document(userId).get()
                    .addOnSuccessListener(userDoc -> {
                        String userName = getUserDisplayName(userDoc, currentUser);
                        String jarName = (jar != null && jar.getName() != null) ? jar.getName() : "Shared Jar";

                        JarTransaction transaction = new JarTransaction(
                                jarId,
                                userId,
                                userName,
                                amount,
                                "ADD_MONEY",
                                note != null ? note : "Added money to jar"
                        );
                        transaction.setJarName(jarName);

                        addTransactionToCollection(jarId, jarName, transaction);

                        callback.onSuccess(null);
                    })
                    .addOnFailureListener(e -> {
                        String userName = getUserName(currentUser);
                        String jarName = (jar != null && jar.getName() != null) ? jar.getName() : "Shared Jar";

                        JarTransaction transaction = new JarTransaction(
                                jarId,
                                userId,
                                userName,
                                amount,
                                "ADD_MONEY",
                                note != null ? note : "Added money to jar"
                        );
                        transaction.setJarName(jarName);
                        addTransactionToCollection(jarId, jarName, transaction);

                        callback.onSuccess(null);
                    });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error adding money", e);
            callback.onFailure(e);
        });
    }

    // Helper method to get user display name from Firestore document
    private String getUserDisplayName(DocumentSnapshot userDoc, FirebaseUser firebaseUser) {
        if (userDoc.exists()) {
            String fullName = userDoc.getString("fullName");
            if (fullName != null && !fullName.trim().isEmpty()) {
                return fullName;
            }

            String userName = userDoc.getString("userName");
            if (userName != null && !userName.trim().isEmpty()) {
                return userName;
            }

            String displayName = userDoc.getString("displayName");
            if (displayName != null && !displayName.trim().isEmpty()) {
                return displayName;
            }

            String name = userDoc.getString("name");
            if (name != null && !name.trim().isEmpty()) {
                return name;
            }
        }

        return getUserName(firebaseUser);
    }

    // Add transaction to separate collection
    private void addTransactionToCollection(String jarId, String jarName, JarTransaction transaction) {
        if (transaction.getId() == null || transaction.getId().isEmpty()) {
            transaction.setId(transactionsCollection.document().getId());
        }

        transaction.setJarId(jarId);
        transaction.setJarName(jarName);
        transaction.setTimestamp(com.google.firebase.Timestamp.now());

        transactionsCollection.document(transaction.getId())
                .set(transaction)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Transaction added to collection: " + transaction.getId() +
                                " with userName: " + transaction.getUserName())
                )
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error adding transaction to collection", e)
                );
    }

    // Get jar by ID
    public void getJarById(String jarId, FirestoreCallback<JarFirestoreModel> callback) {
        jarsCollection.document(jarId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            JarFirestoreModel jar = document.toObject(JarFirestoreModel.class);
                            if (jar != null) {
                                if (jar.getId() == null || jar.getId().isEmpty()) {
                                    jar.setId(document.getId());
                                }
                                callback.onSuccess(jar);
                            } else {
                                callback.onFailure(new Exception("Failed to parse jar data"));
                            }
                        } else {
                            callback.onFailure(new Exception("Jar not found"));
                        }
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    // Get transactions for a jar
    public void getTransactions(String jarId, FirestoreCallback<List<JarTransaction>> callback) {
        transactionsCollection
                .whereEqualTo("jarId", jarId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<JarTransaction> transactionList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        JarTransaction transaction = document.toObject(JarTransaction.class);
                        transactionList.add(transaction);
                    }
                    callback.onSuccess(transactionList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting transactions", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Add participant to shared jar
     */
    public void addParticipant(String jarId, String participantEmail,
                               FirestoreCallback<Void> callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        // First, find user by email
        usersCollection
                .whereEqualTo("email", participantEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String participantId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        // Get participant name
                        DocumentSnapshot participantDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String participantName = getUserDisplayName(participantDoc, null);

                        String safeParticipantName = (participantName != null && !participantName.isEmpty())
                                ? participantName : participantEmail.split("@")[0];

                        DocumentReference jarRef = jarsCollection.document(jarId);

                        // Get jar details first
                        jarRef.get().addOnSuccessListener(documentSnapshot -> {
                            JarFirestoreModel jar = documentSnapshot.toObject(JarFirestoreModel.class);
                            String jarName = jar != null && jar.getName() != null ? jar.getName() : "Shared Jar";

                            // Check if user is already a participant
                            List<String> participants = jar != null ? jar.getParticipants() : new ArrayList<>();
                            if (participants != null && participants.contains(participantId)) {
                                callback.onFailure(new Exception("User is already a participant"));
                                return;
                            }

                            // Get current user's name for notification
                            usersCollection.document(currentUser.getUid()).get()
                                    .addOnSuccessListener(currentUserDoc -> {
                                        String currentUserName = getUserDisplayName(currentUserDoc, currentUser);

                                        // Add the user to the jar's participants
                                        jarRef.update("participants", FieldValue.arrayUnion(participantId))
                                                .addOnSuccessListener(aVoid -> {
                                                    // Initialize contribution for new participant
                                                    Map<String, Double> currentContributions = jar != null ?
                                                            jar.getParticipantContributions() : new HashMap<>();
                                                    if (currentContributions == null) {
                                                        currentContributions = new HashMap<>();
                                                    }

                                                    // Add contribution only if not already present
                                                    if (!currentContributions.containsKey(participantId)) {
                                                        currentContributions.put(participantId, 0.0);
                                                        jarRef.update("participantContributions", currentContributions)
                                                                .addOnSuccessListener(aVoid2 -> {
                                                                    // Successfully added both participant and contribution
                                                                    Log.d(TAG, "Successfully added participant " + participantId + " to jar " + jarId);
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Log.e(TAG, "Error updating contributions", e);
                                                                });
                                                    }

                                                    // Send notification to the invited user
                                                    sendJarInvitationNotification(
                                                            jarId,
                                                            jarName,
                                                            currentUser.getUid(),
                                                            currentUserName,
                                                            participantId,
                                                            safeParticipantName
                                                    );

                                                    // Create transaction record
                                                    JarTransaction transaction = new JarTransaction(
                                                            jarId,
                                                            currentUser.getUid(),
                                                            currentUserName,
                                                            0.0,
                                                            "ADD_PARTICIPANT",
                                                            "invited " + safeParticipantName + " to the jar"
                                                    );
                                                    transaction.setJarName(jarName);
                                                    addTransactionToCollection(jarId, jarName, transaction);

                                                    callback.onSuccess(null);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Error adding participant to jar", e);
                                                    callback.onFailure(e);
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        // Fallback if can't get current user's name
                                        String currentUserName = getUserName(currentUser);

                                        jarRef.update("participants", FieldValue.arrayUnion(participantId))
                                                .addOnSuccessListener(aVoid -> {
                                                    // Initialize contribution
                                                    Map<String, Double> currentContributions = jar != null ?
                                                            jar.getParticipantContributions() : new HashMap<>();
                                                    if (currentContributions == null) {
                                                        currentContributions = new HashMap<>();
                                                    }
                                                    if (!currentContributions.containsKey(participantId)) {
                                                        currentContributions.put(participantId, 0.0);
                                                        jarRef.update("participantContributions", currentContributions);
                                                    }

                                                    sendJarInvitationNotification(
                                                            jarId,
                                                            jarName,
                                                            currentUser.getUid(),
                                                            currentUserName,
                                                            participantId,
                                                            safeParticipantName
                                                    );

                                                    JarTransaction transaction = new JarTransaction(
                                                            jarId,
                                                            currentUser.getUid(),
                                                            currentUserName,
                                                            0.0,
                                                            "ADD_PARTICIPANT",
                                                            "invited " + safeParticipantName + " to the jar"
                                                    );
                                                    transaction.setJarName(jarName);
                                                    addTransactionToCollection(jarId, jarName, transaction);

                                                    callback.onSuccess(null);
                                                })
                                                .addOnFailureListener(callback::onFailure);
                                    });
                        });
                    } else {
                        callback.onFailure(new Exception("User not found with email: " + participantEmail));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Send jar invitation notification to user
     */
    private void sendJarInvitationNotification(String jarId, String jarName,
                                               String inviterId, String inviterName,
                                               String inviteeId, String inviteeName) {
        // Create notification object
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "JAR_INVITE");
        notification.put("title", "Jar Invitation");
        notification.put("message", inviterName + " invited you to join '" + jarName + "'");
        notification.put("jarId", jarId);
        notification.put("jarName", jarName);
        notification.put("inviterId", inviterId);
        notification.put("inviterName", inviterName);
        notification.put("inviteeName", inviteeName);
        notification.put("isRead", false);
        notification.put("timestamp", new Date());
        notification.put("actionType", "ACCEPT/DECLINE");

        // Save to user's notifications subcollection
        usersCollection
                .document(inviteeId)
                .collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Invitation notification sent to: " + inviteeId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending invitation notification", e);
                });
    }

    /**
     * Accept jar invitation - FIXED: Now uses proper Firestore exception
     */
    public void acceptJarInvitation(String jarId, String notificationId,
                                    FirestoreCallback<Void> callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        String userId = currentUser.getUid();
        DocumentReference jarRef = jarsCollection.document(jarId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(jarRef);
            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException(
                        "Jar not found",
                        FirebaseFirestoreException.Code.NOT_FOUND
                );
            }

            // Safe casting with type checking
            Object participantsObj = snapshot.get("participants");
            List<String> participants = new ArrayList<>();
            if (participantsObj instanceof List) {
                for (Object item : (List<?>) participantsObj) {
                    if (item instanceof String) {
                        participants.add((String) item);
                    }
                }
            }

            if (!participants.contains(userId)) {
                participants.add(userId);
                transaction.update(jarRef, "participants", participants);
            }

            // Safe casting for contributions
            Object contributionsObj = snapshot.get("participantContributions");
            Map<String, Double> contributions = new HashMap<>();
            if (contributionsObj instanceof Map) {
                Map<?, ?> rawMap = (Map<?, ?>) contributionsObj;
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    if (entry.getKey() instanceof String && entry.getValue() instanceof Number) {
                        contributions.put((String) entry.getKey(), ((Number) entry.getValue()).doubleValue());
                    }
                }
            }

            if (!contributions.containsKey(userId)) {
                contributions.put(userId, 0.0);
                transaction.update(jarRef, "participantContributions", contributions);
            }

            return null;
        }).addOnSuccessListener(aVoid -> {
            // Delete the notification after accepting
            if (notificationId != null && !notificationId.isEmpty()) {
                usersCollection
                        .document(userId)
                        .collection("notifications")
                        .document(notificationId)
                        .delete()
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Error deleting notification", e)
                        );
            }
            callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error accepting invitation", e);
            callback.onFailure(e);
        });
    }

    /**
     * Decline jar invitation - Delete the notification
     */
    public void declineJarInvitation(String notificationId, FirestoreCallback<Void> callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        String userId = currentUser.getUid();

        usersCollection
                .document(userId)
                .collection("notifications")
                .document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    private String getUserName(FirebaseUser user) {
        if (user == null) return "User";

        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            return user.getDisplayName();
        }
        String email = user.getEmail();
        if (email != null) {
            return email.split("@")[0];
        }
        return "User";
    }

    @SuppressWarnings("unused")
    public void getJarsByType(String type, FirestoreCallback<List<JarFirestoreModel>> callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        String userId = currentUser.getUid();

        jarsCollection
                .whereArrayContains("participants", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<JarFirestoreModel> jarList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            JarFirestoreModel jar = document.toObject(JarFirestoreModel.class);
                            if (jar != null && type.equalsIgnoreCase(jar.getType())) {
                                if (jar.getId() == null || jar.getId().isEmpty()) {
                                    jar.setId(document.getId());
                                }
                                jarList.add(jar);
                            }
                        }
                        callback.onSuccess(jarList);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    @SuppressWarnings("unused")
    public void getTotalSavedAmount(FirestoreCallback<Double> callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        String userId = currentUser.getUid();

        jarsCollection
                .whereArrayContains("participants", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double total = 0.0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        JarFirestoreModel jar = document.toObject(JarFirestoreModel.class);
                        if (jar != null) {
                            total += jar.getSavedAmount();
                        }
                    }
                    callback.onSuccess(total);
                })
                .addOnFailureListener(callback::onFailure);
    }

    @SuppressWarnings("unused")
    public void deleteJar(String jarId, FirestoreCallback<Void> callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        jarsCollection.document(jarId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    transactionsCollection
                            .whereEqualTo("jarId", jarId)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    document.getReference().delete();
                                }
                            });
                    callback.onSuccess(null);
                })
                .addOnFailureListener(callback::onFailure);
    }
}