package com.smartcampusmng.campusmanager.views;

import com.smartcampusmng.campusmanager.entity.Payment;
import com.smartcampusmng.campusmanager.entity.User;
import com.smartcampusmng.campusmanager.service.PaymentService;
import com.smartcampusmng.campusmanager.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
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

@Route("generate-receipt")
@PageTitle("Generate Receipt | Campus Manager")
@AnonymousAllowed
public class GenerateReceiptView extends VerticalLayout {

    private final UserService userService;
    private final PaymentService paymentService;
    private Grid<Payment> paymentGrid;
    private ComboBox<String> semesterComboBox;
    private ComboBox<String> academicYearComboBox;
    private User currentUser;

    public GenerateReceiptView(UserService userService, PaymentService paymentService) {
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
            if (currentUser == null) {
                Notification.show("User not found. Please log in again.");
                UI.getCurrent().navigate("login");
                return;
            }

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

            H1 title = new H1("Generate Receipt");
            H2 subtitle = new H2("Select a payment to generate receipt");

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

            // Create grid for payments
            paymentGrid = new Grid<>();
            paymentGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
            paymentGrid.addColumn(payment -> payment.getStudent().getUsername())
                .setHeader("Student ID");
            paymentGrid.addColumn(payment -> payment.getStudent().getFirstName() + " " + payment.getStudent().getLastName())
                .setHeader("Student Name");
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

            // Generate receipt button
            Button generateButton = new Button("Generate Receipt", e -> {
                Payment selectedPayment = paymentGrid.getSelectedItems().stream().findFirst().orElse(null);
                if (selectedPayment == null) {
                    Notification.show("Please select a payment to generate receipt");
                    return;
                }
                showReceiptDialog(selectedPayment);
            });
            generateButton.setWidth("300px");

            // Update grid when semester or academic year changes
            semesterComboBox.addValueChangeListener(e -> updatePaymentGrid());
            academicYearComboBox.addValueChangeListener(e -> updatePaymentGrid());

            // Add all components to main content
            mainContent.add(
                title,
                subtitle,
                semesterComboBox,
                academicYearComboBox,
                paymentGrid,
                generateButton
            );

            add(header, mainContent);
        } catch (Exception e) {
            Notification.show("Error loading view: " + e.getMessage());
            UI.getCurrent().navigate("login");
        }
    }

    private void updatePaymentGrid() {
        if (semesterComboBox.getValue() == null || academicYearComboBox.getValue() == null) {
            return;
        }

        try {
            // Get all payments for the selected semester and academic year
            List<Payment> payments = paymentService.getAllPaymentsBySemesterAndYear(
                semesterComboBox.getValue(), academicYearComboBox.getValue());
            paymentGrid.setItems(payments);
        } catch (Exception e) {
            Notification.show("Error loading payments: " + e.getMessage());
        }
    }

    private void showReceiptDialog(Payment payment) {
        Dialog receiptDialog = new Dialog();
        receiptDialog.setHeaderTitle("Payment Receipt");

        VerticalLayout receiptContent = new VerticalLayout();
        receiptContent.setSpacing(true);
        receiptContent.setPadding(true);

        // Add receipt details
        receiptContent.add(new Paragraph("Receipt Number: " + payment.getReceiptNumber()));
        receiptContent.add(new Paragraph("Date: " + payment.getPaymentDate()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        receiptContent.add(new Paragraph("Student ID: " + payment.getStudent().getUsername()));
        receiptContent.add(new Paragraph("Student Name: " + payment.getStudent().getFirstName() + " " 
            + payment.getStudent().getLastName()));
        receiptContent.add(new Paragraph("Fee Type: " + payment.getStudentFee().getFeeType().getFeeName()));
        receiptContent.add(new Paragraph("Semester: " + payment.getStudentFee().getSemester()));
        receiptContent.add(new Paragraph("Academic Year: " + payment.getStudentFee().getAcademicYear()));
        receiptContent.add(new Paragraph("Amount: ₹" + payment.getAmount()));
        receiptContent.add(new Paragraph("Payment Method: " + payment.getPaymentMethod().toString()));
        receiptContent.add(new Paragraph("Transaction Reference: " + payment.getTransactionReference()));
        receiptContent.add(new Paragraph("Processed By: " + payment.getRecordedBy().getFirstName() + " " 
            + payment.getRecordedBy().getLastName()));

        // Add buttons
        Button printButton = new Button("Print Receipt", e -> {
            // TODO: Implement print functionality
            Notification.show("Print functionality will be implemented");
        });

        Button closeButton = new Button("Close", e -> receiptDialog.close());

        receiptDialog.add(receiptContent);
        receiptDialog.getFooter().add(printButton, closeButton);
        receiptDialog.open();
    }
} 