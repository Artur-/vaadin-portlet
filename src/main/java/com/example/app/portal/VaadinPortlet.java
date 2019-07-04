package com.example.app.portal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortalContext;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import com.example.app.portal.impl.VaadinGateInRequest;
import com.example.app.portal.impl.VaadinLiferayRequest;
import com.example.app.portal.impl.VaadinWebLogicPortalRequest;
import com.example.app.portal.impl.VaadinWebSpherePortalRequest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;

public class VaadinPortlet extends GenericPortlet {

    private VaadinPortletService vaadinService;

    @Override
    public void init(PortletConfig config) throws PortletException {
        CurrentInstance.clearAll();
        super.init(config);
        Properties initParameters = new Properties();

        // Read default parameters from the context
        final PortletContext context = config.getPortletContext();
        for (final Enumeration<String> e = context.getInitParameterNames(); e
                .hasMoreElements();) {
            final String name = e.nextElement();
            initParameters.setProperty(name, context.getInitParameter(name));
        }

        // Override with application settings from portlet.xml
        for (final Enumeration<String> e = config.getInitParameterNames(); e
                .hasMoreElements();) {
            final String name = e.nextElement();
            initParameters.setProperty(name, config.getInitParameter(name));
        }

        DeploymentConfiguration deploymentConfiguration = createDeploymentConfiguration(
                initParameters);
        try {
            vaadinService = createPortletService(deploymentConfiguration);
        } catch (ServiceException e) {
            throw new PortletException("Could not initialized VaadinPortlet",
                    e);
        }
        // Sets current service even though there are no request and response
        VaadinService.setCurrent(null);

        portletInitialized();

        CurrentInstance.clearAll();
    }

    protected DeploymentConfiguration createDeploymentConfiguration(
            Properties initParameters) {
        return new DefaultDeploymentConfiguration(getClass(), initParameters);
    }

    protected VaadinPortletService createPortletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        VaadinPortletService service = new VaadinPortletService(this,
                deploymentConfiguration);
        service.init();
        return service;
    }

    protected VaadinPortletService getService() {
        return vaadinService;
    }

    protected void portletInitialized() throws PortletException {

    }

    @Override
    protected void doDispatch(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        try {
            // try to let super handle - it'll call methods annotated for
            // handling, the default doXYZ(), or throw if a handler for the mode
            // is not found
            super.doDispatch(request, response);

        } catch (PortletException e) {
            if (e.getCause() == null) {
                // No cause interpreted as 'unknown mode' - pass that trough
                // so that the application can handle
                handleRequest(request, response);

            } else {
                // Something else failed, pass on
                throw e;
            }
        }
    }

    /**
     * Wraps the request in a (possibly portal specific) Vaadin portlet request.
     *
     * @param request
     *            The original PortletRequest
     * @return A wrapped version of the PortletRequest
     */
    protected VaadinPortletRequest createVaadinRequest(PortletRequest request) {
        PortalContext portalContext = request.getPortalContext();
        String portalInfo = portalContext.getPortalInfo()
                .toLowerCase(Locale.ROOT).trim();
        VaadinPortletService service = getService();

        if (portalInfo.contains("gatein")) {
            return new VaadinGateInRequest(request, service);
        }

        if (portalInfo.contains("liferay")) {
            return new VaadinLiferayRequest(request, service);
        }

        if (portalInfo.contains("websphere portal")) {
            return new VaadinWebSpherePortalRequest(request, service);
        }
        if (portalInfo.contains("weblogic portal")) {
            return new VaadinWebLogicPortalRequest(request, service);
        }

        return new VaadinPortletRequest(request, service);
    }

    private VaadinPortletResponse createVaadinResponse(
            PortletResponse response) {
        return new VaadinPortletResponse(response, getService());
    }

    @Override
    public void serveResource(ResourceRequest request,
            ResourceResponse response) throws PortletException, IOException {
        handleRequest(request, response);
    }

    @Override
    public void render(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        String tag = getMainComponentTag();
        PrintWriter writer = response.getWriter();
        writer.write(
                "<script src='/myportlet/web-component/main-view.js'></script>");
        writer.write("<" + tag + "></" + tag + ">");
    }

    @Override
    public void processAction(ActionRequest request, ActionResponse response)
            throws PortletException, IOException {
        handleRequest(request, response);
    }

    @Override
    public void processEvent(EventRequest request, EventResponse response)
            throws PortletException, IOException {
        handleRequest(request, response);
    }

    protected void handleRequest(PortletRequest request,
            PortletResponse response) throws PortletException, IOException {

        CurrentInstance.clearAll();
        try {
            getService().handleRequest(createVaadinRequest(request),
                    createVaadinResponse(response));
        } catch (ServiceException e) {
            throw new PortletException(e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        getService().destroy();
    }

    /**
     * Gets the currently used Vaadin portlet. The current portlet is
     * automatically defined when processing requests related to the service
     * (see {@link ThreadLocal}) and in {@link VaadinSession#access(Runnable)}
     * and {@link UI#access(Runnable)}. In other cases, (e.g. from background
     * threads, the current service is not automatically defined.
     *
     * The current portlet is derived from the current service using
     * {@link VaadinService#getCurrent()}
     *
     * @return the current vaadin portlet instance if available, otherwise
     *         <code>null</code>
     *
     * @since 7.0
     */
    public static VaadinPortlet getCurrent() {
        VaadinService vaadinService = CurrentInstance.get(VaadinService.class);
        if (vaadinService instanceof VaadinPortletService) {
            VaadinPortletService vps = (VaadinPortletService) vaadinService;
            return vps.getPortlet();
        } else {
            return null;
        }
    }

    /**
     * Gets the tag for the main component in the portlet.
     * <p>
     * By default uses the one and only exported web component.
     *
     * @return the tag of the main component to use
     * @throws PortletException
     *             if the main component could not be detected
     */
    protected String getMainComponentTag() throws PortletException {
        WebComponentConfigurationRegistry registry = WebComponentConfigurationRegistry
                .getInstance(getService().getContext());
        int exportedComponents = registry.getConfigurations().size();
        if (exportedComponents == 0) {
            throw new PortletException("No web components exported. Add a "
                    + WebComponentExporter.class.getSimpleName()
                    + " which exports your main component");
        } else if (exportedComponents > 1) {
            String definedComponents = registry.getConfigurations().stream()
                    .map(conf -> conf.getTag())
                    .collect(Collectors.joining(", "));
            throw new PortletException("Multiple web components are exported: "
                    + definedComponents
                    + ". Export only one web component or override getMainComponentTag in the portlet class");
        }
        return registry.getConfigurations().iterator().next().getTag();
    }

}