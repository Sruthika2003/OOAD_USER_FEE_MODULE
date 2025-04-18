package com.smartcampusmng.campusmanager.views;

import com.smartcampusmng.campusmanager.entity.User;
import com.smartcampusmng.campusmanager.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

@Route("login")
@PageTitle("Login | Campus Manager")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

    public LoginView(UserService userService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        ComboBox<User.UserRole> role = new ComboBox<>("Role");
        role.setItems(User.UserRole.values());

        FormLayout formLayout = new FormLayout();
        formLayout.add(username, password, role);
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );

        Button loginButton = new Button("Login", e -> {
            try {
                if (userService.authenticate(username.getValue(), password.getValue(), role.getValue())) {
                    // Create authentication token
                    PreAuthenticatedAuthenticationToken auth = new PreAuthenticatedAuthenticationToken(
                        username.getValue(),
                        null,
                        null
                    );
                    
                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    
                    // Store user info in Vaadin session
                    VaadinSession.getCurrent().setAttribute("username", username.getValue());
                    VaadinSession.getCurrent().setAttribute("role", role.getValue());
                    
                    Notification.show("Login successful!");
                    // Navigate to dashboard
                    UI.getCurrent().navigate("dashboard");
                } else {
                    Notification.show("Invalid credentials or role!");
                }
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        Button registerButton = new Button("Register", e -> UI.getCurrent().navigate("register"));

        add(
            new H1("Campus Manager"),
            formLayout,
            loginButton,
            registerButton
        );
    }
} 