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
import com.example.funkyeventapp.repositories.EventRepository;
import com.example.funkyeventapp.repositories.MockDataRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AddEventFragment extends Fragment {
    private final EventRepository eventRepository = EventRepository.getInstance();
    private final MockDataRepository mockRepository = MockDataRepository.getInstance();
    private final DateTimeFormatter displayDate = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public AddEventFragment() { super(R.layout.fragment_add_event); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
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
        TextView formTitle = view.findViewById(R.id.textEventFormTitle);
        String eventId = getArguments() == null ? null : getArguments().getString("eventId");
        boolean editMode = eventId != null;

        EventType[] selectedType = {EventType.EVENT};
        LocalDate[] start = {LocalDate.now()};
        LocalDate[] end = {LocalDate.now()};
        Event[] existingEvent = {null};
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

        List<Client> clients = mockRepository.getClients();
        List<String> clientNames = new ArrayList<>();
        for (Client client : clients) clientNames.add(client.getName());
        clientInput.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, clientNames));
        clientInput.setOnClickListener(v -> clientInput.showDropDown());
        clientInput.setOnItemClickListener((parent, row, position, id) -> {
            Client client = clients.get(position);
            if (text(billing).isEmpty()) billing.setText(client.getName());
        });

        eventButton.setOnClickListener(v -> { selectedType[0] = EventType.EVENT; styleTypes(eventButton, campaignButton, true); });
        campaignButton.setOnClickListener(v -> { selectedType[0] = EventType.CAMPAIGN; styleTypes(eventButton, campaignButton, false); });
        styleTypes(eventButton, campaignButton, true);
        view.findViewById(R.id.buttonAddEventBack).setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        if (editMode) {
            formTitle.setText(R.string.edit_event);
            saveButton.setEnabled(false);
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
                    selectedType[0] = loadedEvent.getType();
                    start[0] = loadedEvent.getStartDate();
                    end[0] = loadedEvent.getEndDate() == null
                            ? loadedEvent.getStartDate() : loadedEvent.getEndDate();
                    name.setText(loadedEvent.getName());
                    startInput.setText(start[0].format(displayDate));
                    endInput.setText(end[0].format(displayDate));
                    location.setText(loadedEvent.getLocation());
                    for (int i = 0; i < clients.size(); i++) {
                        if (clients.get(i).getId().equals(loadedEvent.getClientId())) {
                            clientInput.setText(clientNames.get(i), false);
                            break;
                        }
                    }
                    billing.setText(loadedEvent.getBillingEntity());
                    po.setText(loadedEvent.getPoNumber());
                    terms.setText(loadedEvent.getPaymentTerms());
                    notes.setText(loadedEvent.getNotes());
                    styleTypes(eventButton, campaignButton,
                            selectedType[0] == EventType.EVENT);
                    saveButton.setEnabled(true);
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
            int clientIndex = clientNames.indexOf(clientInput.getText().toString());
            String clientId = clientIndex >= 0
                    ? clients.get(clientIndex).getId()
                    : existingEvent[0] == null ? null : existingEvent[0].getClientId();
            if (text(name).isEmpty() || text(location).isEmpty() || clientId == null
                    || end[0].isBefore(start[0])) {
                Toast.makeText(requireContext(), R.string.invalid_new_event, Toast.LENGTH_SHORT).show();
                return;
            }
            EventStatus status = existingEvent[0] == null
                    ? EventStatus.CURRENT : existingEvent[0].getStatus();
            boolean completed = existingEvent[0] != null && existingEvent[0].isCompleted();
            Event event = new Event(eventId, text(name), selectedType[0], start[0], end[0],
                    text(location), status, clientId, text(billing), text(po),
                    text(terms), text(notes), completed);
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

    private String text(TextInputEditText input) { return input.getText() == null ? "" : input.getText().toString().trim(); }
    private interface DateConsumer { void accept(LocalDate date); }
}
