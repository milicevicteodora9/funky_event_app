package com.example.funkyeventapp.repositories;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.funkyeventapp.models.Client;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Firestore access for clients. */
public final class ClientRepository {
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception error);
    }

    private static final ClientRepository INSTANCE = new ClientRepository();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private ClientRepository() { }

    public static ClientRepository getInstance() { return INSTANCE; }

    public Task<Void> createClient(@NonNull Client client) {
        return createClient(client, null);
    }

    public Task<Void> createClient(@NonNull Client client, @Nullable Uri localLogoUri) {
        DocumentReference document = firestore.collection("clients").document();
        if (localLogoUri == null) {
            return saveClient(document, client);
        }

        StorageReference logo = storage.getReference()
                .child("client-logos")
                .child(document.getId())
                .child("logo");
        Task<Void> createTask = logo.putFile(localLogoUri)
                .continueWithTask(upload -> {
                    if (!upload.isSuccessful()) {
                        Exception error = upload.getException();
                        if (error != null) throw error;
                        throw new IllegalStateException("Client logo upload failed");
                    }
                    client.setLogoUri(logo.toString());
                    return saveClient(document, client);
                });
        createTask.addOnFailureListener(error -> logo.delete());
        return createTask;
    }

    private Task<Void> saveClient(DocumentReference document, Client client) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", client.getName());
        data.put("logoUri", client.getLogoUri());
        data.put("taxId", client.getTaxId());
        data.put("address", client.getAddress());
        data.put("email", client.getEmail());
        data.put("phone", client.getPhone());
        data.put("contactPerson", client.getContactPerson());
        return document.set(data)
                .addOnSuccessListener(unused -> client.setId(document.getId()));
    }

    public void getAllClients(@NonNull Callback<List<Client>> callback) {
        firestore.collection("clients").get()
                .addOnSuccessListener(snapshot -> {
                    try {
                        List<Client> clients = new ArrayList<>();
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            Client client = document.toObject(Client.class);
                            if (client == null) {
                                throw new IllegalStateException(
                                        "Could not map client document: " + document.getId());
                            }
                            client.setId(document.getId());
                            clients.add(client);
                        }
                        callback.onSuccess(clients);
                    } catch (IllegalArgumentException | IllegalStateException error) {
                        callback.onError(error);
                    }
                })
                .addOnFailureListener(callback::onError);
    }
}
