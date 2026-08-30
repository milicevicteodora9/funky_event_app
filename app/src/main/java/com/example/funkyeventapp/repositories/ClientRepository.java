package com.example.funkyeventapp.repositories;

import androidx.annotation.NonNull;

import com.example.funkyeventapp.models.Client;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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
