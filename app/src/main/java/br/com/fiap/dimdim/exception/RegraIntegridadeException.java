package br.com.fiap.dimdim.exception;

/**
 * Violacao de regra do banco (FK, UNIQUE, CHECK) ja traduzida para uma
 * mensagem legivel. A API devolve 409; a tela mostra a mensagem.
 */
public class RegraIntegridadeException extends RuntimeException {

    public RegraIntegridadeException(String mensagem) {
        super(mensagem);
    }
}
