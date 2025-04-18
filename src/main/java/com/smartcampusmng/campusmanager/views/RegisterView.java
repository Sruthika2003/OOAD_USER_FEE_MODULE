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
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("register")
@PageTitle("Register | Campus Manager")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {

    public RegisterView(UserService userService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        TextField username = new TextField("Username");
        TextField firstName = new TextField("First Name");
        TextField lastName = new TextField("Last Name");
        EmailField email = new EmailField("Email");
        PasswordField password = new PasswordField("Password");
        ComboBox<User.UserRole> role = new ComboBox<>("Role");
        role.setItems(User.UserRole.values());

        FormLayout formLayout = new FormLayout();
        formLayout.add(
            username, firstName, lastName,
            email, password, role
        );
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );

        Button registerButton = new Button("Register", e -> {
            try {
                User user = new User();
                user.setUsername(username.getValue());
                user.setFirstName(firstName.getValue());
                user.setLastName(lastName.getValue());
                user.setEmail(email.getValue());
                user.setPassword(password.getValue());
                user.setRole(role.getValue());

                userService.registerUser(user);
                Notification.show("Registration successful!");
                UI.getCurrent().navigate("login");
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        Button backButton = new Button("Back to Login", e -> UI.getCurrent().navigate("login"));

        add(
            new H1("Register"),
            formLayout,
            registerButton,
            backButton
        );
    }
} 