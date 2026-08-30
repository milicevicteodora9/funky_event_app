package com.example.funkyeventapp.repositories;

import androidx.annotation.NonNull;

import com.example.funkyeventapp.models.Client;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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

    private ClientRepository() { }

    public static ClientRepository getInstance() { return INSTANCE; }

    public Task<Void> createClient(@NonNull Client client) {
        DocumentReference document = firestore.collection("clients").document();
        return saveClient(document, client);
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

    public Task<Void> updateClient(@NonNull Client client) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", client.getName());
        data.put("taxId", client.getTaxId());
        data.put("address", client.getAddress());
        data.put("email", client.getEmail());
        data.put("phone", client.getPhone());
        data.put("contactPerson", client.getContactPerson());
        return firestore.collection("clients").document(client.getId()).update(data);
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
