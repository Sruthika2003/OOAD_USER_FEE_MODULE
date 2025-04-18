package com.smartcampusmng.campusmanager.views;

import com.smartcampusmng.campusmanager.entity.FeeAlert;
import com.smartcampusmng.campusmanager.entity.User;
import com.smartcampusmng.campusmanager.service.FeeAlertService;
import com.smartcampusmng.campusmanager.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.List;

@Route("student-alerts")
@PageTitle("Fee Alerts | Campus Manager")
@AnonymousAllowed
public class StudentAlertsView extends VerticalLayout {

    private final UserService userService;
    private final FeeAlertService feeAlertService;
    private Grid<FeeAlert> alertsGrid;
    private User currentUser;

    public StudentAlertsView(UserService userService, FeeAlertService feeAlertService) {
        this.userService = userService;
        this.feeAlertService = feeAlertService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        initializeView();
    }

    private void initializeView() {
        // Get current user from session
        String currentUsername = (String) VaadinSession.getCurrent().getAttribute("username");
        User.UserRole currentRole = (User.UserRole) VaadinSession.getCurrent().getAttribute("role");
        
        if (currentUsername == null || currentRole == null) {
            Notification.show("Please log in to access this feature");
            UI.getCurrent().navigate("login");
            return;
        }

        // Only allow students to access this view
        if (currentRole != User.UserRole.STUDENT) {
            Notification.show("This feature is only available for students");
            UI.getCurrent().navigate("dashboard");
            return;
        }

        try {
            currentUser = userService.findByUsername(currentUsername);

            // Create header with back button
            HorizontalLayout header = new HorizontalLayout();
            header.setWidthFull();
            header.setJustifyContentMode(JustifyContentMode.BETWEEN);
            header.setAlignItems(Alignment.CENTER);

            Button backButton = new Button("Back to Dashboard", e -> {
                UI.getCurrent().navigate("dashboard");
            });

            header.add(backButton);

            // Create main content
            VerticalLayout mainContent = new VerticalLayout();
            mainContent.setWidth("80%");
            mainContent.setAlignItems(Alignment.CENTER);

            H1 title = new H1("Fee Alerts");
            H2 subtitle = new H2("Your Pending Fee Notifications");

            // Create grid for alerts
            alertsGrid = new Grid<>();
            alertsGrid.setWidthFull();
            alertsGrid.setHeight("500px");
            
            // Add columns to the grid
            alertsGrid.addColumn(alert -> alert.getStudentFee().getFeeType().getFeeName())
                .setHeader("Fee Type")
                .setSortable(true);
                
            alertsGrid.addColumn(alert -> alert.getStudentFee().getSemester())
                .setHeader("Semester")
                .setSortable(true);
                
            alertsGrid.addColumn(alert -> alert.getStudentFee().getAcademicYear())
                .setHeader("Academic Year")
                .setSortable(true);
                
            alertsGrid.addColumn(alert -> "₹" + String.format("%,.2f", alert.getStudentFee().getAmount()))
                .setHeader("Amount")
                .setSortable(true);
                
            alertsGrid.addColumn(alert -> alert.getStudentFee().getDueDate())
                .setHeader("Due Date")
                .setSortable(true);
                
            alertsGrid.addColumn(FeeAlert::getAlertDate)
                .setHeader("Alert Date")
                .setSortable(true);
                
            alertsGrid.addColumn(FeeAlert::getMessage)
                .setHeader("Alert Message")
                .setSortable(true);

            // Add components to main content
            mainContent.add(title, subtitle, alertsGrid);

            // Add header and main content to view
            add(header, mainContent);

            // Load alerts
            loadAlerts();

        } catch (Exception e) {
            Notification.show("Error loading alerts: " + e.getMessage());
            UI.getCurrent().navigate("login");
        }
    }

    private void loadAlerts() {
        try {
            List<FeeAlert> alerts = feeAlertService.getAlertsForStudent(currentUser);
            alertsGrid.setItems(alerts);
            
            if (alerts.isEmpty()) {
                Notification.show("No fee alerts found");
            } else {
                Notification.show("Found " + alerts.size() + " fee alerts");
            }
        } catch (Exception e) {
            Notification.show("Error loading alerts: " + e.getMessage());
        }
    }
} 