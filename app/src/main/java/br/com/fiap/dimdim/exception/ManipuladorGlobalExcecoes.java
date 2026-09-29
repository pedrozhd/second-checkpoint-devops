package br.com.fiap.dimdim.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Erros da API REST em JSON. Restrito ao pacote controller.api: as telas
 * tem o proprio tratamento em ManipuladorExcecoesWeb.
 */
@RestControllerAdvice(basePackages = "br.com.fiap.dimdim.controller.api")
public class ManipuladorGlobalExcecoes {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResposta.de(404, "Não encontrado", ex.getMessage()));
    }

    /**
     * FK, UNIQUE ou CHECK violados no banco -> 409. O caso central e apagar
     * um cliente que possui transacoes.
     */
    @ExceptionHandler(RegraIntegridadeException.class)
    public ResponseEntity<ErroResposta> tratarViolacaoIntegridade(RegraIntegridadeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResposta.de(409, "Conflito de integridade", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.put(erro.getField(), erro.getDefaultMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErroResposta.de(400, "Requisição inválida", "Um ou mais campos estão inválidos.", campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> tratarJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErroResposta.de(400, "Requisição inválida",
                        "Corpo da requisição ausente ou com JSON malformado."));
    }
}
