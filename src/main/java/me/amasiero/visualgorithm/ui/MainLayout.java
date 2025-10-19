package me.amasiero.visualgorithm.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.UIScope;
import me.amasiero.visualgorithm.graph.GraphView;
import me.amasiero.visualgorithm.sorting.SortingView;
import org.springframework.stereotype.Component;

public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
    }

    private void createHeader() {
        var title = new H1("Visualgorithm");
        title.getStyle().set("fontSize", "var(--lumo-font-size-l)");

        var bar = new HorizontalLayout(
                new RouterLink("Sorting", SortingView.class)
//                new RouterLink("Graph", GraphView.class)
        );
        bar.setSpacing(true);

        var header = new HorizontalLayout(title, bar);
        header.setWidthFull();
        header.setSpacing(true);
        header.setMargin(true);
        header.expand(title);

        addToNavbar(header);
    }
}
