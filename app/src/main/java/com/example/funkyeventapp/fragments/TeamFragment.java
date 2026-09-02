package com.example.funkyeventapp.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.adapters.TeamMemberAdapter;
import com.example.funkyeventapp.models.TeamMember;
import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.repositories.TeamRepository;
import com.example.funkyeventapp.services.AuthService;
import com.example.funkyeventapp.services.AuthorizationService;
import com.example.funkyeventapp.ui.TeamMemberDialog;
import com.example.funkyeventapp.ui.AuthenticatedHeader;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class TeamFragment extends Fragment {
    private final TeamRepository repository = TeamRepository.getInstance();
    private final DecimalFormat money = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private final List<TeamMember> allMembers = new ArrayList<>();
    private final Map<String, java.math.BigDecimal> debtByMember = new HashMap<>();
    private TeamMemberAdapter adapter;
    private TextView title, totalDebt;
    private TextInputEditText search;

    public TeamFragment() { super(R.layout.fragment_team); }

    @Override public void onResume() {
        super.onResume();
        if (adapter != null) refresh();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!AuthenticatedHeader.bind(this, view)) return;
        User currentUser = AuthService.getInstance().getCurrentUser();
        if (!AuthorizationService.canAccessTeam(currentUser)) {
            Toast.makeText(requireContext(), R.string.access_denied, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }
        title = view.findViewById(R.id.textTeamTitle);
        totalDebt = view.findViewById(R.id.textTotalTeamDebt);
        search = view.findViewById(R.id.inputSearchTeam);
        adapter = new TeamMemberAdapter(new TeamMemberAdapter.Listener() {
            @Override public void onOpen(TeamMember member) {
                Bundle arguments = new Bundle();
                arguments.putString("teamMemberId", member.getId());
                Navigation.findNavController(view).navigate(R.id.action_teamFragment_to_teamMemberDetailsFragment, arguments);
            }
            @Override public void onEdit(TeamMember member) { TeamMemberDialog.show(TeamFragment.this, repository, member, saved -> refresh()); }
        });
        androidx.recyclerview.widget.RecyclerView list = view.findViewById(R.id.recyclerTeam);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) { }
        });
        view.findViewById(R.id.buttonAddTeamMember).setOnClickListener(v ->
                TeamMemberDialog.show(this, repository, null, saved -> refresh()));
        view.findViewById(R.id.buttonEvents).setOnClickListener(this::openEvents);
        view.findViewById(R.id.buttonClients).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.clientsFragment));
        view.findViewById(R.id.buttonCashbox).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.cashboxFragment));
        view.findViewById(R.id.buttonAdmin).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.userManagementFragment));
        view.findViewById(R.id.buttonUsers).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_teamFragment_to_allCashboxesFragment));
        int[] informational = {};
        for (int id : informational) view.findViewById(id).setOnClickListener(this::showComingLater);
        refresh();
    }

    private void refresh() {
        repository.getTeamOverview(new TeamRepository.Callback<TeamRepository.TeamOverview>() {
            @Override public void onSuccess(TeamRepository.TeamOverview overview) {
                if (!isAdded() || getView() == null) return;
                allMembers.clear();
                allMembers.addAll(overview.getMembers());
                debtByMember.clear();
                debtByMember.putAll(overview.getDebtByMember());
                title.setText(getString(R.string.team_count, allMembers.size()));
                totalDebt.setText(money.format(overview.getTotalDebt()) + " €");
                filter(search == null || search.getText() == null
                        ? "" : search.getText().toString());
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() == null) return;
                Toast.makeText(requireContext(), R.string.team_load_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void filter(String rawQuery) {
        String query = rawQuery.trim().toLowerCase(Locale.ROOT);
        List<TeamMember> result = new ArrayList<>();
        for (TeamMember member : allMembers) {
            if (query.isEmpty() || contains(member.getFullName(), query) || contains(member.getPhone(), query)
                    || contains(member.getCity(), query) || contains(member.getEmail(), query)) result.add(member);
        }
        adapter.submitList(result, debtByMember);
    }

    private boolean contains(String value, String query) { return value != null && value.toLowerCase(Locale.ROOT).contains(query); }

    private void openEvents(View view) {
        NavController controller = Navigation.findNavController(view);
        if (!controller.popBackStack(R.id.eventsFragment, false)) controller.navigate(R.id.eventsFragment);
    }
    private void showComingLater(View view) {
        CharSequence label = view.getContentDescription();
        Toast.makeText(requireContext(), getString(R.string.coming_later, label == null ? "" : label), Toast.LENGTH_SHORT).show();
    }
}
