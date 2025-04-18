package com.smartcampusmng.campusmanager.views;

import com.smartcampusmng.campusmanager.entity.*;
import com.smartcampusmng.campusmanager.service.*;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Route("make-payment")
@PageTitle("Make Payment | Campus Manager")
@AnonymousAllowed
public class MakePaymentView extends VerticalLayout {

    private final UserService userService;
    private final FeeTypeService feeTypeService;
    private final StudentFeeService studentFeeService;
    private final PaymentService paymentService;
    private Grid<StudentFee> pendingFeesGrid;
    private NumberField totalAmount;
    private ComboBox<String> semesterComboBox;
    private ComboBox<String> academicYearComboBox;
    private User currentUser;

    public MakePaymentView(UserService userService, FeeTypeService feeTypeService,
                         StudentFeeService studentFeeService, PaymentService paymentService) {
        this.userService = userService;
        this.feeTypeService = feeTypeService;
        this.studentFeeService = studentFeeService;
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
            Notification.show("Please log in to make payments");
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

            // Create payment form
            VerticalLayout paymentForm = new VerticalLayout();
            paymentForm.setWidth("80%");
            paymentForm.setAlignItems(Alignment.CENTER);
            paymentForm.setSpacing(true);

            H1 paymentTitle = new H1("Make Payment");

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

            // Total amount field
            totalAmount = new NumberField("Total Selected Amount");
            totalAmount.setValue(0.0);
            totalAmount.setReadOnly(true);
            totalAmount.setSuffixComponent(new Span("₹"));

            // Create grid for pending fees
            pendingFeesGrid = new Grid<>();
            pendingFeesGrid.setSelectionMode(Grid.SelectionMode.MULTI);
            pendingFeesGrid.addColumn(fee -> fee.getFeeType().getFeeName()).setHeader("Fee Type");
            pendingFeesGrid.addColumn(fee -> fee.getFeeType().getDescription()).setHeader("Description");
            pendingFeesGrid.addColumn(fee -> "₹" + fee.getAmount()).setHeader("Amount");
            pendingFeesGrid.addColumn(fee -> fee.getFeeType().getFrequency().toString()).setHeader("Frequency");
            pendingFeesGrid.addColumn(StudentFee::getDueDate).setHeader("Due Date");

            // Update grid when semester or academic year changes
            semesterComboBox.addValueChangeListener(e -> updateFeesGrid());
            academicYearComboBox.addValueChangeListener(e -> updateFeesGrid());

            // Handle grid selection changes
            pendingFeesGrid.addSelectionListener(e -> {
                double total = e.getAllSelectedItems().stream()
                    .mapToDouble(StudentFee::getAmount)
                    .sum();
                totalAmount.setValue(total);
            });

            // Payment button
            Button payButton = new Button("Proceed to Payment", e -> {
                if (pendingFeesGrid.getSelectedItems().isEmpty()) {
                    Notification.show("Please select at least one fee to pay");
                    return;
                }

                showPaymentDialog();
            });
            payButton.setWidth("300px");

            // Add all components to the form
            paymentForm.add(
                paymentTitle,
                semesterComboBox,
                academicYearComboBox,
                new H2("Pending Fees"),
                pendingFeesGrid,
                totalAmount,
                payButton
            );

            add(header, paymentForm);
        } catch (Exception e) {
            Notification.show("Error loading payment form: " + e.getMessage());
            UI.getCurrent().navigate("login");
        }
    }

    private void updateFeesGrid() {
        if (semesterComboBox.getValue() == null || academicYearComboBox.getValue() == null) {
            return;
        }

        try {
            // Get fees for the selected semester and academic year
            List<StudentFee> allFees = studentFeeService.getFeesForStudentAndSemester(
                currentUser, semesterComboBox.getValue(), academicYearComboBox.getValue());
            
            // Filter for pending fees
            List<StudentFee> pendingFees = allFees.stream()
                .filter(fee -> fee.getStatus() == StudentFee.PaymentStatus.PENDING)
                .collect(Collectors.toList());

            // Update the grid with pending fees
            pendingFeesGrid.setItems(pendingFees);
            
            // Reset total amount
            totalAmount.setValue(0.0);
            
            // Show notification if no pending fees found
            if (pendingFees.isEmpty()) {
                Notification.show("No pending fees found for the selected semester and academic year");
            }
        } catch (Exception e) {
            Notification.show("Error loading fees: " + e.getMessage());
        }
    }

    private void showPaymentDialog() {
        Dialog paymentDialog = new Dialog();
        paymentDialog.setHeaderTitle("Payment Details");

        FormLayout paymentForm = new FormLayout();
        
        ComboBox<Payment.PaymentMethod> paymentMethod = new ComboBox<>("Payment Method");
        paymentMethod.setItems(Payment.PaymentMethod.values());
        paymentMethod.setWidthFull();

        TextField transactionRef = new TextField("Transaction Reference");
        transactionRef.setWidthFull();

        Button confirmButton = new Button("Confirm Payment", e -> {
            if (paymentMethod.getValue() == null) {
                Notification.show("Please select a payment method");
                return;
            }

            try {
                // Process each selected fee
                for (StudentFee fee : pendingFeesGrid.getSelectedItems()) {
                    paymentService.processPayment(
                        fee,
                        currentUser,
                        currentUser, // For now, student is recording their own payment
                        paymentMethod.getValue(),
                        transactionRef.getValue()
                    );
                }

                Notification.show("Payment processed successfully!");
                paymentDialog.close();
                updateFeesGrid();
                totalAmount.setValue(0.0); // Reset total after successful payment
            } catch (Exception ex) {
                Notification.show("Error processing payment: " + ex.getMessage());
            }
        });

        Button cancelButton = new Button("Cancel", e -> paymentDialog.close());

        paymentForm.add(paymentMethod, transactionRef);
        paymentDialog.add(paymentForm);
        paymentDialog.getFooter().add(cancelButton, confirmButton);
        paymentDialog.open();
    }
} 