package com.example.funkyeventapp.ui;
import android.os.Bundle; import android.view.View; import android.widget.TextView;
import androidx.fragment.app.Fragment; import androidx.navigation.NavOptions; import androidx.navigation.Navigation;
import com.example.funkyeventapp.R; import com.example.funkyeventapp.models.User; import com.example.funkyeventapp.services.AuthService; import com.example.funkyeventapp.services.AuthorizationService;
public final class AuthenticatedHeader {
 private AuthenticatedHeader() { }
 public static boolean bind(Fragment fragment, View root) { AuthService auth=AuthService.getInstance(); User user=auth.getCurrentUser(); if(user==null||!user.isActive()){openLogin(root);return false;} TextView name=root.findViewById(R.id.textHeaderUserName),role=root.findViewById(R.id.textHeaderUserRole); if(name!=null)name.setText(user.getFullName()); if(role!=null)role.setText(user.getRole().name()); View admin=root.findViewById(R.id.buttonAdmin),team=root.findViewById(R.id.buttonTeam),allCashboxes=root.findViewById(R.id.buttonUsers); boolean adminAccess=AuthorizationService.canAccessUserManagement(user); if(admin!=null)admin.setVisibility(adminAccess?View.VISIBLE:View.GONE); if(allCashboxes!=null)allCashboxes.setVisibility(adminAccess?View.VISIBLE:View.GONE); if(team!=null)team.setVisibility(AuthorizationService.canAccessTeam(user)?View.VISIBLE:View.GONE); View logout=root.findViewById(R.id.buttonLogout); if(logout!=null)logout.setOnClickListener(v->{auth.logout();openLogin(v);}); return true; }
 public static void openLogin(View view){Navigation.findNavController(view).navigate(R.id.loginFragment,new Bundle(),new NavOptions.Builder().setPopUpTo(R.id.nav_graph,true).build());}
}
