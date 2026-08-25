package com.example.funkyeventapp.ui;

import android.app.DatePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.Currency;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.PaymentMethod;
import com.example.funkyeventapp.models.TeamFee;
import com.example.funkyeventapp.models.TeamPayment;
import com.example.funkyeventapp.repositories.MockDataRepository;
import com.google.android.material.textfield.TextInputEditText;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class TeamFinanceDialogs {
    public interface OnSavedListener { void onSaved(); }
    private TeamFinanceDialogs() { }

    public static void showFee(Fragment fragment, MockDataRepository repository, String memberId,
                               @Nullable TeamFee existing, OnSavedListener listener) {
        View view=LayoutInflater.from(fragment.requireContext()).inflate(R.layout.dialog_team_fee,null);
        Spinner eventSpinner=view.findViewById(R.id.spinnerFeeEvent), currency=view.findViewById(R.id.spinnerFeeCurrency);
        TextInputEditText description=view.findViewById(R.id.inputFeeDescription), amount=view.findViewById(R.id.inputFeeAmount), date=view.findViewById(R.id.inputFeeDate), notes=view.findViewById(R.id.inputFeeNotes);
        List<Event> events=repository.getAllEvents(); List<String> names=new ArrayList<>(); names.add(fragment.getString(R.string.select_event)); for(Event event:events)names.add(event.getName());
        eventSpinner.setAdapter(adapter(fragment,names)); currency.setAdapter(adapter(fragment,java.util.Collections.singletonList(Currency.EUR.name())));
        LocalDate selected=existing==null?LocalDate.now():existing.getDate(); date.setText(selected.toString()); bindDate(fragment,date,selected);
        if(existing!=null){description.setText(existing.getDescription());amount.setText(existing.getAmount().toPlainString());notes.setText(existing.getNotes());for(int i=0;i<events.size();i++)if(events.get(i).getId().equals(existing.getEventId()))eventSpinner.setSelection(i+1);}
        AlertDialog dialog=new com.google.android.material.dialog.MaterialAlertDialogBuilder(fragment.requireContext()).setTitle(existing==null?R.string.add_fee:R.string.edit_fee).setView(view).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.save,null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            BigDecimal parsed=parse(amount); if(eventSpinner.getSelectedItemPosition()==0||value(description).isEmpty()||parsed==null||parsed.signum()<=0){amount.setError(fragment.getString(R.string.invalid_team_fee));return;}
            Event event=events.get(eventSpinner.getSelectedItemPosition()-1); TeamFee fee=new TeamFee(existing==null?null:existing.getId(),memberId,event.getId(),value(description),parsed,Currency.EUR,LocalDate.parse(value(date)),value(notes));
            if(existing==null)repository.addTeamFee(fee);else repository.updateTeamFee(fee);dialog.dismiss();listener.onSaved();
        }));dialog.show();
    }

    public static void showPayment(Fragment fragment, MockDataRepository repository, String memberId,
                                   @Nullable TeamPayment existing, OnSavedListener listener) {
        View view=LayoutInflater.from(fragment.requireContext()).inflate(R.layout.dialog_team_payment,null);
        Spinner currency=view.findViewById(R.id.spinnerPaymentCurrency),method=view.findViewById(R.id.spinnerPaymentMethod);
        TextInputEditText amount=view.findViewById(R.id.inputPaymentAmount),date=view.findViewById(R.id.inputPaymentDate),notes=view.findViewById(R.id.inputPaymentNotes);
        currency.setAdapter(adapter(fragment,java.util.Collections.singletonList(Currency.EUR.name()))); method.setAdapter(adapter(fragment,java.util.Arrays.asList("CASH","CARD","BANK_TRANSFER","OTHER")));
        LocalDate selected=existing==null?LocalDate.now():existing.getPaymentDate();date.setText(selected.toString());bindDate(fragment,date,selected);
        if(existing!=null){amount.setText(existing.getAmount().toPlainString());notes.setText(existing.getNotes());try{method.setSelection(PaymentMethod.valueOf(existing.getPaymentMethod().toUpperCase().replace(' ','_')).ordinal());}catch(Exception ignored){method.setSelection(PaymentMethod.OTHER.ordinal());}}
        AlertDialog dialog=new com.google.android.material.dialog.MaterialAlertDialogBuilder(fragment.requireContext()).setTitle(existing==null?R.string.add_payment:R.string.edit_payment).setView(view).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.save,null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{BigDecimal parsed=parse(amount);if(parsed==null||parsed.signum()<=0||value(date).isEmpty()){amount.setError(fragment.getString(R.string.invalid_team_payment));return;}TeamPayment payment=new TeamPayment(existing==null?null:existing.getId(),memberId,parsed,Currency.EUR,LocalDate.parse(value(date)),method.getSelectedItem().toString(),value(notes));if(existing==null)repository.addTeamPayment(payment);else repository.updateTeamPayment(payment);dialog.dismiss();listener.onSaved();}));dialog.show();
    }

    private static ArrayAdapter<String> adapter(Fragment f,List<String> values){ArrayAdapter<String>a=new ArrayAdapter<>(f.requireContext(),android.R.layout.simple_spinner_item,values);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);return a;}
    private static void bindDate(Fragment f,TextInputEditText input,LocalDate initial){input.setOnClickListener(v->{LocalDate current;try{current=LocalDate.parse(value(input));}catch(Exception e){current=initial;}new DatePickerDialog(f.requireContext(),(picker,y,m,d)->input.setText(LocalDate.of(y,m+1,d).toString()),current.getYear(),current.getMonthValue()-1,current.getDayOfMonth()).show();});}
    private static String value(TextInputEditText input){return input.getText()==null?"":input.getText().toString().trim();}
    private static BigDecimal parse(TextInputEditText input){try{return new BigDecimal(value(input).replace(',','.'));}catch(Exception e){return null;}}
}
