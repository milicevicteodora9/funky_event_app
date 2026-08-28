package com.example.funkyeventapp.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.example.funkyeventapp.repositories.MockDataRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AddEventFragment extends Fragment {
    private final MockDataRepository repository = MockDataRepository.getInstance();
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

        EventType[] selectedType = {EventType.EVENT};
        LocalDate[] start = {LocalDate.now()};
        LocalDate[] end = {LocalDate.now()};
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

        List<Client> clients = repository.getClients();
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
        view.findViewById(R.id.buttonSaveNewEvent).setOnClickListener(v -> {
            int clientIndex = clientNames.indexOf(clientInput.getText().toString());
            if (text(name).isEmpty() || text(location).isEmpty() || clientIndex < 0 || end[0].isBefore(start[0])) {
                Toast.makeText(requireContext(), R.string.invalid_new_event, Toast.LENGTH_SHORT).show();
                return;
            }
            Client client = clients.get(clientIndex);
            Event saved = repository.addEvent(new Event(null, text(name), selectedType[0], start[0], end[0],
                    text(location), EventStatus.CURRENT, client.getId(), text(billing), text(po), text(terms), text(notes), false));
            Toast.makeText(requireContext(), R.string.new_event_saved, Toast.LENGTH_SHORT).show();
            Bundle args = new Bundle(); args.putString("eventId", saved.getId());
            Navigation.findNavController(view).navigate(R.id.action_addEventFragment_to_eventDetailsFragment, args);
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
