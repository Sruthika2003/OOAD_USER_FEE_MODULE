package com.smartcampusmng.campusmanager.views;

import com.smartcampusmng.campusmanager.entity.StudentFee;
import com.smartcampusmng.campusmanager.entity.User;
import com.smartcampusmng.campusmanager.entity.FeeAlert;
import com.smartcampusmng.campusmanager.service.StudentFeeService;
import com.smartcampusmng.campusmanager.service.UserService;
import com.smartcampusmng.campusmanager.service.FeeAlertService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route("pending-fee-alerts")
@PageTitle("Pending Fee Alerts | Campus Manager")
@AnonymousAllowed
public class OverdueFeesView extends VerticalLayout {

    private final UserService userService;
    private final StudentFeeService studentFeeService;
    private final FeeAlertService feeAlertService;
    private Grid<StudentFee> pendingFeesGrid;
    private Grid<FeeAlert> sentAlertsGrid;
    private User currentUser;
    private ComboBox<String> semesterComboBox;
    private ComboBox<String> academicYearComboBox;
    private VerticalLayout pendingFeesLayout;
    private VerticalLayout sentAlertsLayout;

    public OverdueFeesView(UserService userService, StudentFeeService studentFeeService,
                          FeeAlertService feeAlertService) {
        this.userService = userService;
        this.studentFeeService = studentFeeService;
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

        // Only allow accounts users to access this view
        if (currentRole != User.UserRole.ACCOUNTS) {
            Notification.show("This feature is only available for accounts users");
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

            H1 title = new H1("Pending Fee Alerts");
            H2 subtitle = new H2("Students with Pending Fees");

            // Create tabs
            Tab pendingFeesTab = new Tab("Pending Fees");
            Tab sentAlertsTab = new Tab("Sent Alerts");
            Tabs tabs = new Tabs(pendingFeesTab, sentAlertsTab);

            // Create layouts for each tab
            pendingFeesLayout = createPendingFeesLayout();
            sentAlertsLayout = createSentAlertsLayout();

            // Show pending fees layout by default
            pendingFeesLayout.setVisible(true);
            sentAlertsLayout.setVisible(false);

            // Handle tab selection
            tabs.addSelectedChangeListener(event -> {
                pendingFeesLayout.setVisible(tabs.getSelectedTab() == pendingFeesTab);
                sentAlertsLayout.setVisible(tabs.getSelectedTab() == sentAlertsTab);
            });

            // Add components to main content
            mainContent.add(title, subtitle, tabs, pendingFeesLayout, sentAlertsLayout);

            // Add header and main content to view
            add(header, mainContent);

            // Load initial data
            loadPendingFees();
            loadSentAlerts();

        } catch (Exception e) {
            Notification.show("Error loading pending fees: " + e.getMessage());
            UI.getCurrent().navigate("login");
        }
    }

    private VerticalLayout createPendingFeesLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();
        layout.setAlignItems(Alignment.CENTER);

        // Create filter layout
        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setWidthFull();
        filterLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        filterLayout.setSpacing(true);

        // Create semester combo box
        semesterComboBox = new ComboBox<>("Select Semester");
        semesterComboBox.setItems("First Semester (Aug-Dec)", "Second Semester (Jan-May)");
        semesterComboBox.setPlaceholder("Select Semester");
        semesterComboBox.addValueChangeListener(e -> loadPendingFees());

        // Create academic year combo box
        academicYearComboBox = new ComboBox<>("Select Academic Year");
        academicYearComboBox.setItems("2023-24", "2024-25", "2025-26");
        academicYearComboBox.setPlaceholder("Select Academic Year");
        academicYearComboBox.addValueChangeListener(e -> loadPendingFees());

        filterLayout.add(semesterComboBox, academicYearComboBox);

        // Create grid for pending fees
        pendingFeesGrid = new Grid<>();
        pendingFeesGrid.setWidthFull();
        pendingFeesGrid.setHeight("500px");
        
        // Add columns to the grid
        pendingFeesGrid.addColumn(fee -> fee.getStudent().getFirstName() + " " + fee.getStudent().getLastName())
            .setHeader("Student Name")
            .setSortable(true);
            
        pendingFeesGrid.addColumn(fee -> fee.getStudent().getEmail())
            .setHeader("Email")
            .setSortable(true);
            
        pendingFeesGrid.addColumn(fee -> fee.getFeeType().getFeeName())
            .setHeader("Fee Type")
            .setSortable(true);
            
        pendingFeesGrid.addColumn(fee -> fee.getSemester())
            .setHeader("Semester")
            .setSortable(true);
            
        pendingFeesGrid.addColumn(fee -> fee.getAcademicYear())
            .setHeader("Academic Year")
            .setSortable(true);
            
        pendingFeesGrid.addColumn(fee -> "₹" + String.format("%,.2f", fee.getAmount()))
            .setHeader("Amount")
            .setSortable(true);
            
        pendingFeesGrid.addColumn(StudentFee::getDueDate)
            .setHeader("Due Date")
            .setSortable(true);
            
        pendingFeesGrid.addColumn(fee -> fee.getStatus().toString())
            .setHeader("Status")
            .setSortable(true);

        // Add selection mode to grid
        pendingFeesGrid.setSelectionMode(Grid.SelectionMode.MULTI);

        // Create send alert button
        Button sendAlertButton = new Button("Send Alert to Selected Students", e -> {
            if (pendingFeesGrid.getSelectedItems().isEmpty()) {
                Notification.show("Please select at least one student to send an alert");
                return;
            }
            showAlertDialog(pendingFeesGrid.getSelectedItems().stream().collect(Collectors.toSet()));
        });
        sendAlertButton.setWidth("300px");

        layout.add(filterLayout, pendingFeesGrid, sendAlertButton);
        return layout;
    }

    private VerticalLayout createSentAlertsLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();
        layout.setAlignItems(Alignment.CENTER);

        // Create grid for sent alerts
        sentAlertsGrid = new Grid<>();
        sentAlertsGrid.setWidthFull();
        sentAlertsGrid.setHeight("500px");
        
        // Add columns to the grid
        sentAlertsGrid.addColumn(alert -> alert.getStudent().getFirstName() + " " + alert.getStudent().getLastName())
            .setHeader("Student Name")
            .setSortable(true);
            
        sentAlertsGrid.addColumn(alert -> alert.getStudent().getEmail())
            .setHeader("Email")
            .setSortable(true);
            
        sentAlertsGrid.addColumn(alert -> alert.getStudentFee().getFeeType().getFeeName())
            .setHeader("Fee Type")
            .setSortable(true);
            
        sentAlertsGrid.addColumn(alert -> alert.getStudentFee().getSemester())
            .setHeader("Semester")
            .setSortable(true);
            
        sentAlertsGrid.addColumn(alert -> alert.getStudentFee().getAcademicYear())
            .setHeader("Academic Year")
            .setSortable(true);
            
        sentAlertsGrid.addColumn(alert -> "₹" + String.format("%,.2f", alert.getStudentFee().getAmount()))
            .setHeader("Amount")
            .setSortable(true);
            
        sentAlertsGrid.addColumn(alert -> alert.getStudentFee().getDueDate())
            .setHeader("Due Date")
            .setSortable(true);
            
        sentAlertsGrid.addColumn(FeeAlert::getAlertDate)
            .setHeader("Alert Sent Date")
            .setSortable(true);
            
        sentAlertsGrid.addColumn(FeeAlert::getMessage)
            .setHeader("Alert Message")
            .setSortable(true);

        layout.add(sentAlertsGrid);
        return layout;
    }

    private void loadPendingFees() {
        try {
            String semester = semesterComboBox.getValue();
            String academicYear = academicYearComboBox.getValue();

            if (semester == null || academicYear == null) {
                Notification.show("Please select both semester and academic year");
                return;
            }

            // Get all pending fees for the selected semester and academic year
            List<StudentFee> pendingFees = studentFeeService.getPendingFeesBySemesterAndYear(semester, academicYear);
            
            // Filter out fees that are already alerted
            List<StudentFee> feesWithoutAlerts = pendingFees.stream()
                .filter(fee -> !fee.isAlerted())
                .collect(Collectors.toList());
            
            if (feesWithoutAlerts.isEmpty()) {
                Notification.show("No pending fees found for the selected semester and academic year");
            } else {
                pendingFeesGrid.setItems(feesWithoutAlerts);
                Notification.show("Found " + feesWithoutAlerts.size() + " pending fees");
            }
        } catch (Exception e) {
            Notification.show("Error loading pending fees: " + e.getMessage());
        }
    }

    private void loadSentAlerts() {
        try {
            List<FeeAlert> sentAlerts = feeAlertService.getAlertsBySentBy(currentUser);
            sentAlertsGrid.setItems(sentAlerts);
        } catch (Exception e) {
            Notification.show("Error loading sent alerts: " + e.getMessage());
        }
    }

    private void showAlertDialog(Set<StudentFee> selectedFees) {
        Dialog alertDialog = new Dialog();
        alertDialog.setHeaderTitle("Send Alert to Students");

        TextArea messageArea = new TextArea("Alert Message");
        messageArea.setWidthFull();
        messageArea.setValue("Dear Student,\n\nThis is a reminder that you have pending fees. Please make the payment at the earliest to avoid any inconvenience.\n\nRegards,\nAccounts Department");

        Button sendButton = new Button("Send Alert", e -> {
            if (messageArea.getValue().trim().isEmpty()) {
                Notification.show("Please enter an alert message");
                return;
            }

            try {
                // Send alerts to selected students
                for (StudentFee fee : selectedFees) {
                    try {
                        feeAlertService.createAlert(
                            fee.getStudent(),
                            fee,
                            currentUser,
                            messageArea.getValue()
                        );
                    } catch (Exception ex) {
                        Notification.show("Error sending alert to " + 
                            fee.getStudent().getFirstName() + " " + 
                            fee.getStudent().getLastName() + ": " + ex.getMessage());
                    }
                }

                Notification.show("Alerts sent successfully to " + selectedFees.size() + " students");
                alertDialog.close();
                loadPendingFees(); // Refresh the grid
                loadSentAlerts(); // Refresh sent alerts
            } catch (Exception ex) {
                Notification.show("Error sending alerts: " + ex.getMessage());
            }
        });

        Button cancelButton = new Button("Cancel", e -> alertDialog.close());

        alertDialog.add(messageArea);
        alertDialog.getFooter().add(cancelButton, sendButton);
        alertDialog.open();
    }
} 