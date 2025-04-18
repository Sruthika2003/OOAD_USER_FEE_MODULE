package com.smartcampusmng.campusmanager.views;

import com.smartcampusmng.campusmanager.entity.Payment;
import com.smartcampusmng.campusmanager.entity.User;
import com.smartcampusmng.campusmanager.service.PaymentService;
import com.smartcampusmng.campusmanager.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
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

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Route("payment-history")
@PageTitle("Payment History | Campus Manager")
@AnonymousAllowed
public class PaymentHistoryView extends VerticalLayout {

    private final UserService userService;
    private final PaymentService paymentService;
    private Grid<Payment> paymentGrid;
    private ComboBox<String> semesterComboBox;
    private ComboBox<String> academicYearComboBox;
    private User currentUser;

    public PaymentHistoryView(UserService userService, PaymentService paymentService) {
        this.userService = userService;
        this.paymentService = paymentService;

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
            Notification.show("Please log in to view payment history");
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
            if (currentUser == null) {
                Notification.show("User not found. Please log in again.");
                UI.getCurrent().navigate("login");
                return;
            }

            // Create header with welcome message and navigation buttons
            HorizontalLayout header = new HorizontalLayout();
            header.setWidthFull();
            header.setJustifyContentMode(JustifyContentMode.BETWEEN);
            header.setAlignItems(Alignment.CENTER);
            header.setPadding(true);

            H2 welcomeMessage = new H2("Welcome, " + currentUser.getFirstName());
            
            Button backButton = new Button("Back to Dashboard", e -> {
                try {
                    UI.getCurrent().navigate("dashboard");
                } catch (Exception ex) {
                    Notification.show("Error navigating to dashboard: " + ex.getMessage());
                }
            });

            header.add(welcomeMessage, backButton);

            // Create payment history form
            VerticalLayout paymentHistoryForm = new VerticalLayout();
            paymentHistoryForm.setWidth("80%");
            paymentHistoryForm.setAlignItems(Alignment.CENTER);
            paymentHistoryForm.setSpacing(true);

            H1 paymentHistoryTitle = new H1("Payment History");

            // Semester selection
            semesterComboBox = new ComboBox<>("Select Semester");
            List<String> semesters = Arrays.asList(
                "First Semester (Aug-Dec)",
                "Second Semester (Jan-May)"
            );
            semesterComboBox.setItems(semesters);
            semesterComboBox.setWidth("300px");

            // Academic year selection
            academicYearComboBox = new ComboBox<>("Select Academic Year");
            academicYearComboBox.setItems("2023-24", "2024-25");
            academicYearComboBox.setWidth("300px");

            // Create grid for payment history
            paymentGrid = new Grid<>();
            paymentGrid.addColumn(payment -> payment.getStudentFee().getFeeType().getFeeName())
                .setHeader("Fee Type");
            paymentGrid.addColumn(payment -> payment.getStudentFee().getSemester())
                .setHeader("Semester");
            paymentGrid.addColumn(payment -> payment.getStudentFee().getAcademicYear())
                .setHeader("Academic Year");
            paymentGrid.addColumn(payment -> "₹" + payment.getAmount())
                .setHeader("Amount");
            paymentGrid.addColumn(payment -> payment.getPaymentMethod().toString())
                .setHeader("Payment Method");
            paymentGrid.addColumn(payment -> payment.getPaymentDate()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setHeader("Payment Date");
            paymentGrid.addColumn(Payment::getReceiptNumber)
                .setHeader("Receipt Number");
            paymentGrid.addColumn(Payment::getTransactionReference)
                .setHeader("Transaction Reference");

            // Update grid when semester or academic year changes
            semesterComboBox.addValueChangeListener(e -> updatePaymentGrid());
            academicYearComboBox.addValueChangeListener(e -> updatePaymentGrid());

            // Add all components to the form
            paymentHistoryForm.add(
                paymentHistoryTitle,
                semesterComboBox,
                academicYearComboBox,
                new H2("Payment History"),
                paymentGrid
            );

            add(header, paymentHistoryForm);
        } catch (Exception e) {
            Notification.show("Error loading payment history: " + e.getMessage());
            UI.getCurrent().navigate("login");
        }
    }

    private void updatePaymentGrid() {
        if (semesterComboBox.getValue() == null || academicYearComboBox.getValue() == null) {
            return;
        }

        List<Payment> payments = paymentService.getPaymentsForStudentAndSemester(
            currentUser, semesterComboBox.getValue(), academicYearComboBox.getValue());
        paymentGrid.setItems(payments);
    }
} 