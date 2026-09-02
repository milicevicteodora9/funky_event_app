package com.example.funkyeventapp.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.Client;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.EventStatus;
import com.example.funkyeventapp.models.EventType;
import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.repositories.ClientRepository;
import com.example.funkyeventapp.repositories.EventRepository;
import com.example.funkyeventapp.repositories.UserRepository;
import com.example.funkyeventapp.services.AuthService;
import com.example.funkyeventapp.services.AuthorizationService;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

public class AddEventFragment extends Fragment {
    private final EventRepository eventRepository = EventRepository.getInstance();
    private final ClientRepository clientRepository = ClientRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();
    private final DateTimeFormatter displayDate = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public AddEventFragment() { super(R.layout.fragment_add_event); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        if (!AuthorizationService.canAccessEvents(AuthService.getInstance().getCurrentUser())) {
            Toast.makeText(requireContext(), R.string.module_access_denied, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }
        TextInputEditText name = view.findViewById(R.id.inputNewEventName);
        TextInputEditText startInput = view.findViewById(R.id.inputNewEventStartDate);
        TextInputEditText endInput = view.findViewById(R.id.inputNewEventEndDate);
        TextInputEditText location = view.findViewById(R.id.inputNewEventLocation);
        AutoCompleteTextView clientInput = view.findViewById(R.id.inputNewEventClient);
        TextInputEditText billing = view.findViewById(R.id.inputNewEventBillingEntity);
        TextInputEditText po = view.findViewById(R.id.inputNewEventPoNumber);
        TextInputEditText terms = view.findViewById(R.id.inputNewEventPaymentTerms);
        TextInputEditText notes = view.findViewById(R.id.inputNewEventNotes);
        MaterialButton eventButton = view.findViewById(R.id.buttonNewEventTypeEvent);
        MaterialButton campaignButton = view.findViewById(R.id.buttonNewEventTypeCampaign);
        MaterialButton saveButton = view.findViewById(R.id.buttonSaveNewEvent);
        MaterialButton assignedUsersButton = view.findViewById(R.id.buttonSelectAssignedUsers);
        TextView assignedUsersSummary = view.findViewById(R.id.textAssignedUsersSummary);
        TextView formTitle = view.findViewById(R.id.textEventFormTitle);
        String eventId = getArguments() == null ? null : getArguments().getString("eventId");
        boolean editMode = eventId != null;

        EventType[] selectedType = {EventType.EVENT};
        LocalDate[] start = {LocalDate.now()};
        LocalDate[] end = {LocalDate.now()};
        Event[] existingEvent = {null};
        String[] selectedClientId = {null};
        boolean[] clientsLoaded = {false};
        boolean[] eventLoaded = {!editMode};
        boolean[] usersLoaded = {false};
        List<User> allUsers = new ArrayList<>();
        List<User> activeUsers = new ArrayList<>();
        Set<String> assignedUserIds = new LinkedHashSet<>();
        Runnable updateReadyState = () -> {
            boolean eventAndUsersReady = eventLoaded[0] && usersLoaded[0];
            assignedUsersButton.setEnabled(eventAndUsersReady);
            saveButton.setEnabled(eventLoaded[0] && clientsLoaded[0]);
        };
        startInput.setText(start[0].format(displayDate));
        endInput.setText(end[0].format(displayDate));
        startInput.setOnClickListener(v -> pickDate(start[0], date -> {
            start[0] = date;
            startInput.setText(date.format(displayDate));
            if (end[0].isBefore(date)) { end[0] = date; endInput.setText(date.format(displayDate)); }
        }));
        endInput.setOnClickListener(v -> pickDate(end[0], date -> {
            end[0] = date;
            endInput.setText(date.format(displayDate));
        }));

        List<Client> clients = new ArrayList<>();
        List<String> clientNames = new ArrayList<>();
        ArrayAdapter<String> clientAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, clientNames);
        clientInput.setAdapter(clientAdapter);
        clientInput.setOnClickListener(v -> clientInput.showDropDown());
        clientInput.setOnItemClickListener((parent, row, position, id) -> {
            Client client = clients.get(position);
            selectedClientId[0] = client.getId();
            if (text(billing).isEmpty()) billing.setText(client.getName());
        });

        eventButton.setOnClickListener(v -> { selectedType[0] = EventType.EVENT; styleTypes(eventButton, campaignButton, true); });
        campaignButton.setOnClickListener(v -> { selectedType[0] = EventType.CAMPAIGN; styleTypes(eventButton, campaignButton, false); });
        styleTypes(eventButton, campaignButton, true);
        view.findViewById(R.id.buttonAddEventBack).setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        saveButton.setEnabled(false);
        assignedUsersButton.setEnabled(false);
        assignedUsersButton.setOnClickListener(v -> showAssignedUsersDialog(
                activeUsers, allUsers, assignedUserIds, assignedUsersSummary));

        clientRepository.getAllClients(new ClientRepository.Callback<List<Client>>() {
            @Override public void onSuccess(List<Client> loadedClients) {
                if (!isAdded() || getView() != view) return;
                clients.clear();
                clients.addAll(loadedClients);
                clientNames.clear();
                for (Client client : clients) clientNames.add(client.getName());
                clientAdapter.notifyDataSetChanged();
                clientsLoaded[0] = true;
                if (existingEvent[0] != null) {
                    selectedClientId[0] = existingEvent[0].getClientId();
                    selectClient(clientInput, clients, existingEvent[0].getClientId());
                }
                updateReadyState.run();
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() != view) return;
                Toast.makeText(requireContext(), R.string.clients_load_error,
                        Toast.LENGTH_SHORT).show();
            }
        });

        userRepository.getAllUsers(new UserRepository.Callback<List<User>>() {
            @Override public void onSuccess(List<User> loadedUsers) {
                if (!isAdded() || getView() != view) return;
                allUsers.clear();
                allUsers.addAll(loadedUsers);
                activeUsers.clear();
                for (User user : loadedUsers) if (user.isActive()) activeUsers.add(user);
                usersLoaded[0] = true;
                updateAssignedUsersSummary(allUsers, assignedUserIds, assignedUsersSummary);
                updateReadyState.run();
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() != view) return;
                Toast.makeText(requireContext(), R.string.users_load_error,
                        Toast.LENGTH_SHORT).show();
            }
        });

        if (editMode) {
            formTitle.setText(R.string.edit_event);
            eventRepository.getEventById(eventId, new EventRepository.Callback<Event>() {
                @Override public void onSuccess(Event loadedEvent) {
                    if (!isAdded() || getView() != view) return;
                    if (loadedEvent == null) {
                        Toast.makeText(requireContext(), R.string.event_not_found,
                                Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(view).popBackStack();
                        return;
                    }
                    existingEvent[0] = loadedEvent;
                    eventLoaded[0] = true;
                    assignedUserIds.clear();
                    assignedUserIds.addAll(loadedEvent.getAssignedUserIds());
                    updateAssignedUsersSummary(allUsers, assignedUserIds, assignedUsersSummary);
                    selectedClientId[0] = loadedEvent.getClientId();
                    selectedType[0] = loadedEvent.getType();
                    start[0] = loadedEvent.getStartDate();
                    end[0] = loadedEvent.getEndDate() == null
                            ? loadedEvent.getStartDate() : loadedEvent.getEndDate();
                    name.setText(loadedEvent.getName());
                    startInput.setText(start[0].format(displayDate));
                    endInput.setText(end[0].format(displayDate));
                    location.setText(loadedEvent.getLocation());
                    if (clientsLoaded[0]) {
                        selectClient(clientInput, clients, loadedEvent.getClientId());
                    }
                    billing.setText(loadedEvent.getBillingEntity());
                    po.setText(loadedEvent.getPoNumber());
                    terms.setText(loadedEvent.getPaymentTerms());
                    notes.setText(loadedEvent.getNotes());
                    styleTypes(eventButton, campaignButton,
                            selectedType[0] == EventType.EVENT);
                    updateReadyState.run();
                }

                @Override public void onError(@NonNull Exception error) {
                    if (!isAdded() || getView() != view) return;
                    Toast.makeText(requireContext(), R.string.events_load_error,
                            Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).popBackStack();
                }
            });
        }

        saveButton.setOnClickListener(v -> {
            if (text(name).isEmpty() || text(location).isEmpty() || selectedClientId[0] == null
                    || end[0].isBefore(start[0])) {
                Toast.makeText(requireContext(), R.string.invalid_new_event, Toast.LENGTH_SHORT).show();
                return;
            }
            EventStatus status = existingEvent[0] == null
                    ? EventStatus.CURRENT : existingEvent[0].getStatus();
            boolean completed = existingEvent[0] != null && existingEvent[0].isCompleted();
            Event event = new Event(eventId, text(name), selectedType[0], start[0], end[0],
                    text(location), status, selectedClientId[0], text(billing), text(po),
                    text(terms), text(notes), completed);
            event.setAssignedUserIds(new ArrayList<>(assignedUserIds));
            saveButton.setEnabled(false);
            Task<Void> saveTask = editMode
                    ? eventRepository.updateEvent(event)
                    : eventRepository.createEvent(event);
            saveTask
                    .addOnSuccessListener(unused -> {
                        if (!isAdded() || getView() != view) return;
                        Toast.makeText(requireContext(), editMode
                                        ? R.string.event_updated : R.string.new_event_saved,
                                Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(view).popBackStack();
                    })
                    .addOnFailureListener(error -> {
                        if (!isAdded() || getView() != view) return;
                        saveButton.setEnabled(true);
                        Toast.makeText(requireContext(), editMode
                                        ? R.string.event_update_error : R.string.event_save_error,
                                Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void showAssignedUsersDialog(List<User> activeUsers, List<User> allUsers,
                                         Set<String> assignedUserIds, TextView summary) {
        Set<String> draftAssignments = new LinkedHashSet<>(assignedUserIds);
        String[] labels = new String[activeUsers.size()];
        boolean[] checked = new boolean[activeUsers.size()];
        for (int index = 0; index < activeUsers.size(); index++) {
            User user = activeUsers.get(index);
            labels[index] = user.getFullName() + " — " + user.getRole().name();
            checked[index] = draftAssignments.contains(user.getId());
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.assigned_users)
                .setMultiChoiceItems(labels, checked, (dialog, which, selected) -> {
                    String userId = activeUsers.get(which).getId();
                    if (selected) draftAssignments.add(userId);
                    else draftAssignments.remove(userId);
                })
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.done, (dialog, which) -> {
                    assignedUserIds.clear();
                    assignedUserIds.addAll(draftAssignments);
                    updateAssignedUsersSummary(allUsers, assignedUserIds, summary);
                })
                .show();
    }

    private void updateAssignedUsersSummary(List<User> users, Set<String> assignedUserIds,
                                            TextView summary) {
        if (assignedUserIds.isEmpty()) {
            summary.setText(R.string.no_assigned_users);
            return;
        }
        List<String> names = new ArrayList<>();
        for (String userId : assignedUserIds) {
            String label = getString(R.string.unknown_user);
            for (User user : users) {
                if (userId.equals(user.getId())) {
                    label = user.getFullName();
                    break;
                }
            }
            names.add(label);
        }
        summary.setText(android.text.TextUtils.join(", ", names));
    }

    private void pickDate(LocalDate initial, DateConsumer consumer) {
        new DatePickerDialog(requireContext(), (picker, year, month, day) ->
                consumer.accept(LocalDate.of(year, month + 1, day)), initial.getYear(),
                initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void styleTypes(MaterialButton event, MaterialButton campaign, boolean isEvent) {
        event.setBackgroundTintList(requireContext().getColorStateList(isEvent ? R.color.funky_completed : R.color.funky_surface));
        event.setStrokeColorResource(isEvent ? R.color.funky_mint : R.color.funky_border);
        event.setTextColor(requireContext().getColor(isEvent ? R.color.funky_completed_text : R.color.funky_text_secondary));
        campaign.setBackgroundTintList(requireContext().getColorStateList(isEvent ? R.color.funky_surface : R.color.funky_completed));
        campaign.setStrokeColorResource(isEvent ? R.color.funky_border : R.color.funky_mint);
        campaign.setTextColor(requireContext().getColor(isEvent ? R.color.funky_text_secondary : R.color.funky_completed_text));
    }

    private void selectClient(AutoCompleteTextView input, List<Client> clients, String clientId) {
        for (Client client : clients) {
            if (client.getId().equals(clientId)) {
                input.setText(client.getName(), false);
                return;
            }
        }
    }

    private String text(TextInputEditText input) { return input.getText() == null ? "" : input.getText().toString().trim(); }
    private interface DateConsumer { void accept(LocalDate date); }
}
