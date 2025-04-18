package com.smartcampusmng.campusmanager.views;

import com.smartcampusmng.campusmanager.entity.User;
import com.smartcampusmng.campusmanager.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("profile")
@PageTitle("Profile | Campus Manager")
@AnonymousAllowed
public class ProfileView extends VerticalLayout {

    public ProfileView(UserService userService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Get current user from session
        String currentUsername = (String) VaadinSession.getCurrent().getAttribute("username");
        User.UserRole currentRole = (User.UserRole) VaadinSession.getCurrent().getAttribute("role");
        
        if (currentUsername == null || currentRole == null) {
            Notification.show("Please log in to access your profile");
            UI.getCurrent().navigate("login");
            return;
        }

        try {
            User currentUser = userService.findByUsername(currentUsername);
            if (currentUser == null) {
                Notification.show("User not found. Please log in again.");
                UI.getCurrent().navigate("login");
                return;
            }

            // Create header with welcome message and logout button
            HorizontalLayout header = new HorizontalLayout();
            header.setWidthFull();
            header.setJustifyContentMode(JustifyContentMode.END);
            header.setAlignItems(Alignment.CENTER);
            header.setPadding(true);

            H2 welcomeMessage = new H2("Welcome, " + currentUser.getFirstName() + " (" + currentRole + ")");
            Button logoutButton = new Button("Logout", e -> {
                SecurityContextHolder.clearContext();
                VaadinSession.getCurrent().setAttribute("username", null);
                VaadinSession.getCurrent().setAttribute("role", null);
                UI.getCurrent().navigate("login");
            });

            header.add(welcomeMessage, logoutButton);

            TextField username = new TextField("Username");
            username.setValue(currentUser.getUsername());
            username.setReadOnly(true);

            TextField firstName = new TextField("First Name");
            firstName.setValue(currentUser.getFirstName());

            TextField lastName = new TextField("Last Name");
            lastName.setValue(currentUser.getLastName());

            EmailField email = new EmailField("Email");
            email.setValue(currentUser.getEmail());

            PasswordField password = new PasswordField("New Password");
            password.setPlaceholder("Leave empty to keep current password");

            FormLayout formLayout = new FormLayout();
            formLayout.add(
                username, firstName, lastName,
                email, password
            );
            formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
            );

            Button saveButton = new Button("Save Changes", e -> {
                try {
                    User updatedUser = new User();
                    updatedUser.setUsername(currentUser.getUsername());
                    updatedUser.setFirstName(firstName.getValue());
                    updatedUser.setLastName(lastName.getValue());
                    updatedUser.setEmail(email.getValue());
                    updatedUser.setPassword(password.getValue());

                    userService.updateUserProfile(currentUsername, updatedUser);
                    Notification.show("Profile updated successfully!");
                } catch (Exception ex) {
                    Notification.show(ex.getMessage());
                }
            });

            Button backButton = new Button("Back to Dashboard", e -> {
                try {
                    UI.getCurrent().navigate("dashboard");
                } catch (Exception ex) {
                    Notification.show("Error navigating to dashboard: " + ex.getMessage());
                }
            });
            backButton.getStyle().set("margin-top", "20px");

            VerticalLayout buttonLayout = new VerticalLayout(saveButton, backButton);
            buttonLayout.setAlignItems(Alignment.CENTER);
            buttonLayout.setSpacing(true);

            add(
                header,
                new H1("Profile Management"),
                formLayout,
                buttonLayout
            );
        } catch (Exception e) {
            Notification.show("Error loading profile: " + e.getMessage());
            UI.getCurrent().navigate("login");
        }
    }
} 