package org.vaadin.artur.portlet;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.portal.VaadinPortlet;

public class MainView extends VerticalLayout {

    public MainView() {
        VaadinPortlet portlet = VaadinPortlet.getCurrent();
        String name = portlet.getPortletName();
        String serverInfo = portlet.getPortletContext().getServerInfo();
        Button button = new Button("Click me", event -> Notification.show(
                "Hello from " + name + " running in " + serverInfo + "!"));
        add(button);
    }
}
