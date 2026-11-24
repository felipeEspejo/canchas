package dev.felipe.canchas.adapter.web.controller;

import dev.felipe.canchas.core.BusinessRuleException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.function.Supplier;

/**
 * Ejecuta una acción del usuario y deja el aviso para la siguiente página:
 * "ok" (verde) si salió bien o "problem" (rojo) con el mensaje de la regla que no se cumplió.
 */
final class Feedback {
    private Feedback() {
    }

    /** Devuelve el resultado de la acción, o null si una regla del negocio la rechazó. */
    static <T> T result(RedirectAttributes redirect, String okMessage, Supplier<T> action) {
        try {
            T value = action.get();
            redirect.addFlashAttribute("ok", okMessage);
            return value;
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("problem", e.getMessage());
            return null;
        }
    }

    /** true si la acción se hizo; false si una regla del negocio la rechazó. */
    static boolean done(RedirectAttributes redirect, String okMessage, Runnable action) {
        return result(redirect, okMessage, () -> {
            action.run();
            return Boolean.TRUE;
        }) != null;
    }
}
