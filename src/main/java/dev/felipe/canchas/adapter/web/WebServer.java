package dev.felipe.canchas.adapter.web;

import dev.felipe.canchas.core.port.in.UseCases;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Adaptador de entrada web (Spring MVC + Thymeleaf). Antes de arrancar Spring registra los casos de uso como beans,
 * así los controladores los reciben por constructor sin saber quién los implementa.
 */
public final class WebServer {
    private final UseCases useCases;

    public WebServer(UseCases useCases) {
        this.useCases = useCases;
    }

    public void start(String[] args) {
        ApplicationContextInitializer<ConfigurableApplicationContext> registerUseCases = context -> {
            ConfigurableListableBeanFactory beans = context.getBeanFactory();
            beans.registerSingleton("courtCatalog", useCases.courts());
            beans.registerSingleton("teamRegistry", useCases.teams());
            beans.registerSingleton("bookingDesk", useCases.bookings());
            beans.registerSingleton("scheduleBoard", useCases.schedule());
            beans.registerSingleton("rateBook", useCases.rates());
            beans.registerSingleton("revenueAnalyst", useCases.reports());
            beans.registerSingleton("staffDirectory", useCases.staff());
        };
        new SpringApplicationBuilder(WebApplication.class).initializers(registerUseCases).run(args);
    }
}
