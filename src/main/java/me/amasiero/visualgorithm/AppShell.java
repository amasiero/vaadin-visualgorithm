package me.amasiero.visualgorithm;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;

@Push(PushMode.AUTOMATIC)
public class AppShell implements AppShellConfigurator {
    // Keep empty unless you also want to add @PWA, @Meta, @Inline, etc.
}

