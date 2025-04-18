package com.smartcampusmng.campusmanager.views;

import com.smartcampusmng.campusmanager.entity.User;
import com.smartcampusmng.campusmanager.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("dashboard")
@PageTitle("Dashboard | Campus Manager")
@AnonymousAllowed
public class DashboardView extends VerticalLayout {

    public DashboardView(UserService userService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Get current user from session
        String currentUsername = (String) VaadinSession.getCurrent().getAttribute("username");
        User.UserRole currentRole = (User.UserRole) VaadinSession.getCurrent().getAttribute("role");
        
        if (currentUsername == null || currentRole == null) {
            Notification.show("Please log in to access the dashboard");
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

            // Create dashboard content
            VerticalLayout dashboardContent = new VerticalLayout();
            dashboardContent.setWidth("80%");
            dashboardContent.setAlignItems(Alignment.CENTER);
            dashboardContent.setSpacing(true);

            H1 dashboardTitle = new H1("Dashboard");
            
            // Create feature cards
            VerticalLayout featureCards = new VerticalLayout();
            featureCards.setWidth("100%");
            featureCards.setSpacing(true);
            featureCards.setAlignItems(Alignment.CENTER);

            // Profile Management Card
            VerticalLayout profileCard = new VerticalLayout();
            profileCard.setWidth("300px");
            profileCard.setPadding(true);
            profileCard.getStyle().set("border", "1px solid #ccc");
            profileCard.getStyle().set("border-radius", "5px");
            profileCard.getStyle().set("background-color", "#f5f5f5");
            profileCard.getStyle().set("cursor", "pointer");
            profileCard.addClickListener(e -> {
                try {
                    UI.getCurrent().navigate("profile");
                } catch (Exception ex) {
                    Notification.show("Error navigating to profile: " + ex.getMessage());
                }
            });

            H2 profileTitle = new H2("Profile Management");
            profileTitle.getStyle().set("margin-top", "0");
            Button profileButton = new Button("Manage Profile", e -> {
                try {
                    UI.getCurrent().navigate("profile");
                } catch (Exception ex) {
                    Notification.show("Error navigating to profile: " + ex.getMessage());
                }
            });
            profileButton.setWidthFull();

            profileCard.add(profileTitle, profileButton);
            featureCards.add(profileCard);

            // Fee Payment Card (for students only)
            if (currentRole == User.UserRole.STUDENT) {
                VerticalLayout feeCard = new VerticalLayout();
                feeCard.setWidth("300px");
                feeCard.setPadding(true);
                feeCard.getStyle().set("border", "1px solid #ccc");
                feeCard.getStyle().set("border-radius", "5px");
                feeCard.getStyle().set("background-color", "#f5f5f5");
                feeCard.getStyle().set("cursor", "pointer");

                H2 feeTitle = new H2("Fee Management");
                feeTitle.getStyle().set("margin-top", "0");

                Button viewPaymentsButton = new Button("View Payment History", e -> {
                    try {
                        UI.getCurrent().navigate("payment-history");
                    } catch (Exception ex) {
                        Notification.show("Error navigating to payment history: " + ex.getMessage());
                    }
                });
                viewPaymentsButton.setWidthFull();

                Button makePaymentButton = new Button("Pay Fees", e -> {
                    try {
                        UI.getCurrent().navigate("make-payment");
                    } catch (Exception ex) {
                        Notification.show("Error navigating to payment: " + ex.getMessage());
                    }
                });
                makePaymentButton.setWidthFull();

                feeCard.add(feeTitle, viewPaymentsButton, makePaymentButton);
                featureCards.add(feeCard);

                // Add fee alerts card
                VerticalLayout feeAlertsCard = new VerticalLayout();
                feeAlertsCard.setWidth("300px");
                feeAlertsCard.setPadding(true);
                feeAlertsCard.getStyle().set("border", "1px solid #ccc");
                feeAlertsCard.getStyle().set("border-radius", "5px");
                feeAlertsCard.getStyle().set("background-color", "#f5f5f5");
                feeAlertsCard.getStyle().set("cursor", "pointer");

                H2 feeAlertsTitle = new H2("Fee Alerts");
                feeAlertsTitle.getStyle().set("margin-top", "0");

                Button viewAlertsButton = new Button("View Alerts", e -> {
                    try {
                        UI.getCurrent().navigate("student-alerts");
                    } catch (Exception ex) {
                        Notification.show("Error navigating to alerts: " + ex.getMessage());
                    }
                });
                viewAlertsButton.setWidthFull();

                feeAlertsCard.add(feeAlertsTitle, viewAlertsButton);
                featureCards.add(feeAlertsCard);
            }

            // Add pending fees management card for accounts users
            if (currentRole == User.UserRole.ACCOUNTS) {
                VerticalLayout pendingFeesCard = new VerticalLayout();
                pendingFeesCard.setWidth("300px");
                pendingFeesCard.setPadding(true);
                pendingFeesCard.getStyle().set("border", "1px solid #ccc");
                pendingFeesCard.getStyle().set("border-radius", "5px");
                pendingFeesCard.getStyle().set("background-color", "#f5f5f5");
                pendingFeesCard.getStyle().set("cursor", "pointer");

                H2 pendingFeesTitle = new H2("Pending Fees");
                pendingFeesTitle.getStyle().set("margin-top", "0");

                Button managePendingFeesButton = new Button("Manage Pending Fees", e -> {
                    try {
                        UI.getCurrent().navigate("pending-fee-alerts");
                    } catch (Exception ex) {
                        Notification.show("Error navigating to pending fees: " + ex.getMessage());
                    }
                });
                managePendingFeesButton.setWidthFull();

                pendingFeesCard.add(pendingFeesTitle, managePendingFeesButton);
                featureCards.add(pendingFeesCard);

                // Add receipt generation card
                VerticalLayout receiptCard = new VerticalLayout();
                receiptCard.setWidth("300px");
                receiptCard.setPadding(true);
                receiptCard.getStyle().set("border", "1px solid #ccc");
                receiptCard.getStyle().set("border-radius", "5px");
                receiptCard.getStyle().set("background-color", "#f5f5f5");
                receiptCard.getStyle().set("cursor", "pointer");

                H2 receiptTitle = new H2("Receipt Generation");
                receiptTitle.getStyle().set("margin-top", "0");

                Button generateReceiptButton = new Button("Generate Receipt", e -> {
                    try {
                        UI.getCurrent().navigate("generate-receipt");
                    } catch (Exception ex) {
                        Notification.show("Error navigating to receipt generation: " + ex.getMessage());
                    }
                });
                generateReceiptButton.setWidthFull();

                receiptCard.add(receiptTitle, generateReceiptButton);
                featureCards.add(receiptCard);
            }

            // Add more feature cards here as needed

            dashboardContent.add(dashboardTitle, featureCards);

            add(header, dashboardContent);
        } catch (Exception e) {
            Notification.show("Error loading dashboard: " + e.getMessage());
            UI.getCurrent().navigate("login");
        }
    }
} 