package org.vaadin.artur.portlet;

import com.vaadin.flow.portal.VaadinPortlet;

public class MyPortlet extends VaadinPortlet {
    @Override
    public String getName() {
        // This corresponds to the war filename
        return "vaadin-test-portlet";
    }

}
