package dev.felipe.canchas.adapter.web;

import dev.felipe.canchas.core.BusinessRuleException;
import dev.felipe.canchas.core.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/** Convierte las excepciones del core en páginas de error (para las páginas que no redirigen con un aviso). */
@ControllerAdvice
public class WebErrors {

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView notFound(NotFoundException e) {
        return new ModelAndView("error/404", Map.of("message", e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ModelAndView ruleBroken(BusinessRuleException e) {
        return new ModelAndView("error/400", Map.of("message", e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView badParameter(MethodArgumentTypeMismatchException e) {
        return new ModelAndView("error/400", Map.of("message", "El dato '" + e.getName() + "' no es válido"), HttpStatus.BAD_REQUEST);
    }
}
