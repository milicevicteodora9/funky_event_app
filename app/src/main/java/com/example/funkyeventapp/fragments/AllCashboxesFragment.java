package com.example.funkyeventapp.fragments;

import android.os.Bundle; import android.view.View; import android.widget.Toast;
import androidx.annotation.NonNull; import androidx.annotation.Nullable; import androidx.fragment.app.Fragment; import androidx.navigation.Navigation; import androidx.recyclerview.widget.LinearLayoutManager; import androidx.recyclerview.widget.RecyclerView;
import com.example.funkyeventapp.R; import com.example.funkyeventapp.adapters.CashboxOverviewAdapter; import com.example.funkyeventapp.models.Cashbox; import com.example.funkyeventapp.models.User; import com.example.funkyeventapp.repositories.MockDataRepository; import com.example.funkyeventapp.services.AuthService; import com.example.funkyeventapp.services.AuthorizationService; import com.example.funkyeventapp.ui.AuthenticatedHeader;
import java.util.ArrayList; import java.util.List;

public class AllCashboxesFragment extends Fragment {
 private final MockDataRepository repository=MockDataRepository.getInstance();
 public AllCashboxesFragment(){super(R.layout.fragment_all_cashboxes);}
 @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state){super.onViewCreated(view,state);if(!AuthenticatedHeader.bind(this,view))return;User current=AuthService.getInstance().getCurrentUser();if(!AuthorizationService.canAccessUserManagement(current)){Toast.makeText(requireContext(),R.string.access_denied,Toast.LENGTH_SHORT).show();Navigation.findNavController(view).navigateUp();return;}
  CashboxOverviewAdapter company=new CashboxOverviewAdapter(repository,box->open(view,box)),personal=new CashboxOverviewAdapter(repository,box->open(view,box)); RecyclerView companyList=view.findViewById(R.id.recyclerCompanyCashbox),personalList=view.findViewById(R.id.recyclerPersonalCashboxes);companyList.setLayoutManager(new LinearLayoutManager(requireContext()));companyList.setAdapter(company);personalList.setLayoutManager(new LinearLayoutManager(requireContext()));personalList.setAdapter(personal);
  List<Cashbox> companyItems=new ArrayList<>(),personalItems=new ArrayList<>();for(Cashbox box:repository.getCashboxes())if(box.getUserId()==null)companyItems.add(box);else personalItems.add(box);company.submitList(companyItems);personal.submitList(personalItems);
  view.findViewById(R.id.buttonAllCashboxesBack).setOnClickListener(v->Navigation.findNavController(v).navigateUp());view.findViewById(R.id.buttonEvents).setOnClickListener(v->Navigation.findNavController(v).navigate(R.id.eventsFragment));view.findViewById(R.id.buttonClients).setOnClickListener(v->Navigation.findNavController(v).navigate(R.id.clientsFragment));view.findViewById(R.id.buttonTeam).setOnClickListener(v->Navigation.findNavController(v).navigate(R.id.teamFragment));view.findViewById(R.id.buttonCashbox).setOnClickListener(v->Navigation.findNavController(v).navigate(R.id.cashboxFragment));view.findViewById(R.id.buttonAdmin).setOnClickListener(v->Navigation.findNavController(v).navigate(R.id.userManagementFragment));
 }
 private void open(View view,Cashbox box){Bundle args=new Bundle();args.putString("cashboxId",box.getId());Navigation.findNavController(view).navigate(R.id.action_allCashboxesFragment_to_cashboxFragment,args);}
}
