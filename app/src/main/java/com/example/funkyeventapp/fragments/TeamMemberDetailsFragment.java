package com.example.funkyeventapp.fragments;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.adapters.TeamFeeAdapter;
import com.example.funkyeventapp.adapters.TeamPaymentAdapter;
import com.example.funkyeventapp.models.TeamFee;
import com.example.funkyeventapp.models.TeamMember;
import com.example.funkyeventapp.models.TeamPayment;
import com.example.funkyeventapp.repositories.MockDataRepository;
import com.example.funkyeventapp.ui.TeamMemberDialog;
import com.example.funkyeventapp.ui.TeamFinanceDialogs;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TeamMemberDetailsFragment extends Fragment {
    private final MockDataRepository repository = MockDataRepository.getInstance();
    private final DecimalFormat money = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private String memberId;
    private TeamMember member;
    private TeamFeeAdapter feeAdapter;
    private TeamPaymentAdapter paymentAdapter;

    public TeamMemberDetailsFragment() { super(R.layout.fragment_team_member_details); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        memberId = requireArguments().getString("teamMemberId");
        view.findViewById(R.id.buttonBackTeamMember).setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        view.findViewById(R.id.buttonEditTeamMemberDetails).setOnClickListener(v -> {
            if (member != null) TeamMemberDialog.show(this, repository, member, saved -> bind(view));
        });
        feeAdapter = new TeamFeeAdapter(repository, new TeamFeeAdapter.Listener() {
            @Override public void onEdit(TeamFee fee) { TeamFinanceDialogs.showFee(TeamMemberDetailsFragment.this, repository, memberId, fee, () -> bind(view)); }
            @Override public void onDelete(TeamFee fee) { confirmDeleteFee(view, fee); }
        });
        paymentAdapter = new TeamPaymentAdapter(new TeamPaymentAdapter.Listener() {
            @Override public void onEdit(TeamPayment payment) { TeamFinanceDialogs.showPayment(TeamMemberDetailsFragment.this, repository, memberId, payment, () -> bind(view)); }
            @Override public void onDelete(TeamPayment payment) { confirmDeletePayment(view, payment); }
        });
        androidx.recyclerview.widget.RecyclerView feesList = view.findViewById(R.id.recyclerTeamFees);
        feesList.setLayoutManager(new LinearLayoutManager(requireContext())); feesList.setAdapter(feeAdapter); feesList.setNestedScrollingEnabled(false);
        androidx.recyclerview.widget.RecyclerView paymentsList = view.findViewById(R.id.recyclerTeamPayments);
        paymentsList.setLayoutManager(new LinearLayoutManager(requireContext())); paymentsList.setAdapter(paymentAdapter); paymentsList.setNestedScrollingEnabled(false);
        view.findViewById(R.id.buttonAddFee).setOnClickListener(v -> TeamFinanceDialogs.showFee(this, repository, memberId, null, () -> bind(view)));
        view.findViewById(R.id.buttonAddPayment).setOnClickListener(v -> TeamFinanceDialogs.showPayment(this, repository, memberId, null, () -> bind(view)));
        bind(view);
    }

    @Override public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null && memberId != null) bind(view);
    }

    private void bind(View view) {
        member = repository.getTeamMemberById(memberId);
        if (member == null) {
            Toast.makeText(requireContext(), R.string.team_member_not_found, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }
        ((TextView) view.findViewById(R.id.textTeamMemberDetailsName)).setText(member.getFullName());
        TextView status = view.findViewById(R.id.textTeamMemberStatus);
        status.setText(member.isActive() ? R.string.status_active : R.string.status_inactive);
        status.setTextColor(requireContext().getColor(member.isActive() ? R.color.funky_mint : R.color.funky_text_secondary));

        TextView phone = view.findViewById(R.id.rowTeamPhone);
        bindOptional(phone, view.findViewById(R.id.dividerTeamPhone), member.getPhone(), "☎   ");
        phone.setOnClickListener(empty(member.getPhone()) ? null : v -> openIntent(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(member.getPhone())))));

        TextView email = view.findViewById(R.id.rowTeamEmail);
        bindOptional(email, view.findViewById(R.id.dividerTeamEmail), member.getEmail(), "✉   ");
        email.setOnClickListener(empty(member.getEmail()) ? null : v -> openIntent(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(member.getEmail())))));

        bindOptional(view.findViewById(R.id.rowTeamCity), view.findViewById(R.id.dividerTeamCity), member.getCity(), "⌖   ");
        View bankRow = view.findViewById(R.id.rowTeamBank);
        View bankDivider = view.findViewById(R.id.dividerTeamBank);
        boolean hasBank = !empty(member.getBankAccount());
        bankRow.setVisibility(hasBank ? View.VISIBLE : View.GONE);
        bankDivider.setVisibility(hasBank ? View.VISIBLE : View.GONE);
        ((TextView) view.findViewById(R.id.textTeamBank)).setText("▣   " + member.getBankAccount());
        view.findViewById(R.id.buttonCopyTeamBank).setOnClickListener(hasBank ? v -> copyBankAccount() : null);
        bindOptional(view.findViewById(R.id.rowTeamNotes), null, member.getNotes(), getString(R.string.notes_prefix));

        BigDecimal fees = repository.getTotalFeesForMember(memberId);
        BigDecimal paid = repository.getTotalPaidForMember(memberId);
        BigDecimal debt = repository.getDebtForMember(memberId);
        ((TextView) view.findViewById(R.id.textTeamFees)).setText(format(fees));
        ((TextView) view.findViewById(R.id.textTeamPaid)).setText(format(paid));
        TextView saldo = view.findViewById(R.id.textTeamSaldo);
        saldo.setText(format(debt));
        @ColorRes int saldoColor = debt.signum() > 0 ? R.color.funky_expense
                : debt.signum() < 0 ? R.color.funky_mint : R.color.funky_text;
        saldo.setTextColor(requireContext().getColor(saldoColor));

        java.util.List<TeamFee> feesList = repository.getFeesForTeamMember(memberId);
        java.util.List<TeamPayment> paymentsList = repository.getPaymentsForTeamMember(memberId);
        feeAdapter.submitList(feesList); paymentAdapter.submitList(paymentsList);
        ((TextView)view.findViewById(R.id.textFeesSectionTitle)).setText(getString(R.string.fees_count, feesList.size()));
        ((TextView)view.findViewById(R.id.textPaymentsSectionTitle)).setText(getString(R.string.payments_count, paymentsList.size()));
        view.findViewById(R.id.textNoFees).setVisibility(feesList.isEmpty()?View.VISIBLE:View.GONE);
        view.findViewById(R.id.textNoPayments).setVisibility(paymentsList.isEmpty()?View.VISIBLE:View.GONE);
    }

    private void confirmDeleteFee(View view, TeamFee fee) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.delete_fee_question)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete, (dialog, which) -> { repository.deleteTeamFee(fee.getId()); bind(view); }).show();
    }

    private void confirmDeletePayment(View view, TeamPayment payment) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.delete_payment_question)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete, (dialog, which) -> { repository.deleteTeamPayment(payment.getId()); bind(view); }).show();
    }

    private void bindOptional(TextView text, @Nullable View divider, String value, String prefix) {
        boolean visible = !empty(value);
        text.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (divider != null) divider.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) text.setText(prefix + value);
    }

    private void openIntent(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException exception) { Toast.makeText(requireContext(), R.string.no_compatible_app, Toast.LENGTH_SHORT).show(); }
    }

    private void copyBankAccount() {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.bank_account), member.getBankAccount()));
        Toast.makeText(requireContext(), R.string.bank_account_copied, Toast.LENGTH_SHORT).show();
    }

    private String format(BigDecimal amount) { return money.format(amount) + " €"; }
    private boolean empty(String value) { return value == null || value.trim().isEmpty(); }
}
