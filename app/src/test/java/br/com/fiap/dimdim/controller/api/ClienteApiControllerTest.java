package br.com.fiap.dimdim.controller.api;

import br.com.fiap.dimdim.dto.ClienteRequest;
import br.com.fiap.dimdim.dto.ClienteResponse;
import br.com.fiap.dimdim.exception.RecursoNaoEncontradoException;
import br.com.fiap.dimdim.exception.RegraIntegridadeException;
import br.com.fiap.dimdim.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteApiController.class)
class ClienteApiControllerTest {

    private static final ClienteResponse ANA = new ClienteResponse(
            1L, "Ana Souza", "11122233344", "ana@exemplo.com", LocalDateTime.of(2026, 9, 29, 10, 0));

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ClienteService service;

    @Test
    void post_dadosValidos_retorna201ComLocation() throws Exception {
        when(service.criar(any(ClienteRequest.class))).thenReturn(ANA);

        mvc.perform(post("/api/clientes").contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Souza\",\"cpf\":\"11122233344\",\"email\":\"ana@exemplo.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/clientes/1")))
                .andExpect(jsonPath("$.idCliente").value(1));
    }

    @Test
    void post_cpfComLetras_retorna400ComErroNoCampo() throws Exception {
        mvc.perform(post("/api/clientes").contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Souza\",\"cpf\":\"123abc\",\"email\":\"ana@exemplo.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cpf").value("O CPF deve conter exatamente 11 dígitos numéricos."));

        verifyNoInteractions(service);
    }

    @Test
    void post_jsonMalformado_retorna400() throws Exception {
        mvc.perform(post("/api/clientes").contentType(APPLICATION_JSON).content("{\"nome\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Corpo da requisição ausente ou com JSON malformado."));
    }

    @Test
    void get_idInexistente_retorna404() throws Exception {
        when(service.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Cliente não encontrado para o id 99"));

        mvc.perform(get("/api/clientes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Cliente não encontrado para o id 99"));
    }

    @Test
    void delete_clienteComTransacoes_retorna409() throws Exception {
        doThrow(new RegraIntegridadeException("Não é possível excluir o cliente: existem transações vinculadas a ele."))
                .when(service).excluir(1L);

        mvc.perform(delete("/api/clientes/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
