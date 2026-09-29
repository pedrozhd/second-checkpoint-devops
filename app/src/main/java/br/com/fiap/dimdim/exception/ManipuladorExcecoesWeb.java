package br.com.fiap.dimdim.exception;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Erros das telas Thymeleaf: um id inexistente na URL mostra uma pagina 404
 * legivel, e nao a pagina de erro padrao com stack trace.
 */
@ControllerAdvice(basePackages = "br.com.fiap.dimdim.controller.web")
public class ManipuladorExcecoesWeb {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String tratarNaoEncontrado(RecursoNaoEncontradoException ex, Model model) {
        model.addAttribute("mensagem", ex.getMessage());
        return "erro";
    }
}
