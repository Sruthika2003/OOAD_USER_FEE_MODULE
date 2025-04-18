package com.smartcampusmng.campusmanager.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.VaadinSession;

@Route("")
@RouteAlias("home")
public class MainView extends VerticalLayout {

    public MainView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Create header with profile and logout buttons
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.END);
        header.setPadding(true);

        Button profileButton = new Button("Profile", e -> UI.getCurrent().navigate("profile"));
        Button logoutButton = new Button("Logout", e -> {
            VaadinSession.getCurrent().setAttribute("username", null);
            UI.getCurrent().navigate("login");
        });

        header.add(profileButton, logoutButton);

        add(
            header,
            new H1("Welcome to Campus Manager")
        );
    }
} 