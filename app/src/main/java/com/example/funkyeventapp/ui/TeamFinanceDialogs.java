package com.example.funkyeventapp.ui;

import android.app.DatePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.Currency;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.PaymentMethod;
import com.example.funkyeventapp.models.TeamFee;
import com.example.funkyeventapp.models.TeamPayment;
import com.example.funkyeventapp.repositories.EventRepository;
import com.example.funkyeventapp.repositories.TeamRepository;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class TeamFinanceDialogs {
    public interface OnSavedListener { void onSaved(); }
    private TeamFinanceDialogs() { }

    public static void showFee(Fragment fragment, TeamRepository repository, String memberId,
                               @Nullable TeamFee existing, OnSavedListener listener) {
        EventRepository.getInstance().getAllEvents(new EventRepository.Callback<List<Event>>() {
            @Override public void onSuccess(List<Event> events) {
                if (fragment.isAdded()) {
                    showFeeDialog(fragment, repository, memberId, existing, listener, events);
                }
            }

            @Override public void onError(@NonNull Exception error) {
                if (!fragment.isAdded()) return;
                Toast.makeText(fragment.requireContext(), R.string.events_load_error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private static void showFeeDialog(Fragment fragment, TeamRepository repository,
                                      String memberId, @Nullable TeamFee existing,
                                      OnSavedListener listener, List<Event> events) {
        View view = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.dialog_team_fee, null);
        Spinner eventSpinner = view.findViewById(R.id.spinnerFeeEvent);
        Spinner currency = view.findViewById(R.id.spinnerFeeCurrency);
        TextInputEditText description = view.findViewById(R.id.inputFeeDescription);
        TextInputEditText amount = view.findViewById(R.id.inputFeeAmount);
        TextInputEditText date = view.findViewById(R.id.inputFeeDate);
        TextInputEditText notes = view.findViewById(R.id.inputFeeNotes);
        List<String> names = new ArrayList<>();
        names.add(fragment.getString(R.string.no_event));
        for (Event event : events) names.add(event.getName());
        eventSpinner.setAdapter(adapter(fragment, names));
        currency.setAdapter(adapter(fragment, Collections.singletonList(Currency.EUR.name())));
        LocalDate selected = existing == null ? LocalDate.now() : existing.getDate();
        date.setText(selected.toString());
        bindDate(fragment, date, selected);
        if (existing != null) {
            description.setText(existing.getDescription());
            amount.setText(existing.getAmount().toPlainString());
            notes.setText(existing.getNotes());
            for (int index = 0; index < events.size(); index++) {
                if (events.get(index).getId().equals(existing.getEventId())) {
                    eventSpinner.setSelection(index + 1);
                    break;
                }
            }
        }
        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                fragment.requireContext())
                .setTitle(existing == null ? R.string.add_fee : R.string.edit_fee)
                .setView(view).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    BigDecimal parsed = parse(amount);
                    if (value(description).isEmpty() || parsed == null || parsed.signum() <= 0) {
                        amount.setError(fragment.getString(R.string.invalid_team_fee));
                        return;
                    }
                    int eventPosition = eventSpinner.getSelectedItemPosition();
                    String eventId = eventPosition == 0 ? null : events.get(eventPosition - 1).getId();
                    TeamFee fee = new TeamFee(existing == null ? null : existing.getId(), memberId,
                            eventId, value(description), parsed, Currency.EUR,
                            LocalDate.parse(value(date)), value(notes));
                    setSaving(dialog, true);
                    TeamRepository.Callback<TeamFee> callback = new TeamRepository.Callback<TeamFee>() {
                        @Override public void onSuccess(TeamFee saved) {
                            if (!fragment.isAdded()) return;
                            dialog.dismiss();
                            listener.onSaved();
                        }

                        @Override public void onError(@NonNull Exception error) {
                            if (!fragment.isAdded()) return;
                            setSaving(dialog, false);
                            Toast.makeText(fragment.requireContext(), R.string.team_fee_save_error,
                                    Toast.LENGTH_LONG).show();
                        }
                    };
                    if (existing == null) repository.addTeamFee(fee, callback);
                    else repository.updateTeamFee(fee, callback);
                }));
        dialog.show();
    }

    public static void showPayment(Fragment fragment, TeamRepository repository, String memberId,
                                   @Nullable TeamPayment existing, OnSavedListener listener) {
        View view = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.dialog_team_payment, null);
        Spinner currency = view.findViewById(R.id.spinnerPaymentCurrency);
        Spinner method = view.findViewById(R.id.spinnerPaymentMethod);
        TextInputEditText description = view.findViewById(R.id.inputPaymentDescription);
        TextInputEditText amount = view.findViewById(R.id.inputPaymentAmount);
        TextInputEditText date = view.findViewById(R.id.inputPaymentDate);
        TextInputEditText notes = view.findViewById(R.id.inputPaymentNotes);
        currency.setAdapter(adapter(fragment, Collections.singletonList(Currency.EUR.name())));
        method.setAdapter(adapter(fragment, Arrays.asList("CASH", "CARD", "BANK_TRANSFER", "OTHER")));
        LocalDate selected = existing == null ? LocalDate.now() : existing.getPaymentDate();
        date.setText(selected.toString());
        bindDate(fragment, date, selected);
        if (existing != null) {
            description.setText(existing.getDescription());
            amount.setText(existing.getAmount().toPlainString());
            notes.setText(existing.getNotes());
            try {
                method.setSelection(PaymentMethod.valueOf(existing.getPaymentMethod()
                        .toUpperCase().replace(' ', '_')).ordinal());
            } catch (Exception ignored) {
                method.setSelection(PaymentMethod.OTHER.ordinal());
            }
        }
        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                fragment.requireContext())
                .setTitle(existing == null ? R.string.add_payment : R.string.edit_payment)
                .setView(view).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    BigDecimal parsed = parse(amount);
                    if (parsed == null || parsed.signum() <= 0 || value(date).isEmpty()) {
                        amount.setError(fragment.getString(R.string.invalid_team_payment));
                        return;
                    }
                    TeamPayment payment = new TeamPayment(existing == null ? null : existing.getId(),
                            memberId, value(description), parsed, Currency.EUR,
                            LocalDate.parse(value(date)), method.getSelectedItem().toString(),
                            value(notes));
                    setSaving(dialog, true);
                    TeamRepository.Callback<TeamPayment> callback =
                            new TeamRepository.Callback<TeamPayment>() {
                                @Override public void onSuccess(TeamPayment saved) {
                                    if (!fragment.isAdded()) return;
                                    dialog.dismiss();
                                    listener.onSaved();
                                }

                                @Override public void onError(@NonNull Exception error) {
                                    if (!fragment.isAdded()) return;
                                    setSaving(dialog, false);
                                    Toast.makeText(fragment.requireContext(),
                                            R.string.team_payment_save_error,
                                            Toast.LENGTH_LONG).show();
                                }
                            };
                    if (existing == null) repository.addTeamPayment(payment, callback);
                    else repository.updateTeamPayment(payment, callback);
                }));
        dialog.show();
    }

    private static void setSaving(AlertDialog dialog, boolean saving) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!saving);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(!saving);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(
                saving ? R.string.cashbox_saving : R.string.save);
    }

    private static ArrayAdapter<String> adapter(Fragment fragment, List<String> values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(fragment.requireContext(),
                android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private static void bindDate(Fragment fragment, TextInputEditText input, LocalDate initial) {
        input.setOnClickListener(view -> {
            LocalDate current;
            try { current = LocalDate.parse(value(input)); }
            catch (Exception error) { current = initial; }
            new DatePickerDialog(fragment.requireContext(), (picker, year, month, day) ->
                    input.setText(LocalDate.of(year, month + 1, day).toString()),
                    current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth()).show();
        });
    }

    private static String value(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private static BigDecimal parse(TextInputEditText input) {
        try { return new BigDecimal(value(input).replace(',', '.')); }
        catch (Exception error) { return null; }
    }
}
