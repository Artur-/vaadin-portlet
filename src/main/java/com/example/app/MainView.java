package com.example.app;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.webcomponent.WebComponent;

public class MainView extends VerticalLayout {

    public static class Exporter extends WebComponentExporter<MainView> {

        public Exporter() {
            super("main-view");
        }

        @Override
        protected void configureInstance(WebComponent<MainView> webComponent,
                MainView component) {

        }

    }

    public MainView() {
        Button button = new Button("Click me",
                event -> Notification.show("Clicked!"));
        add(button);
    }
}
