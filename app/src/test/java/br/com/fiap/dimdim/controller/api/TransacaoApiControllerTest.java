package br.com.fiap.dimdim.controller.api;

import br.com.fiap.dimdim.dto.TransacaoRequest;
import br.com.fiap.dimdim.dto.TransacaoResponse;
import br.com.fiap.dimdim.exception.RecursoNaoEncontradoException;
import br.com.fiap.dimdim.service.TransacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransacaoApiController.class)
class TransacaoApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TransacaoService service;

    @Test
    void get_lista_retorna200ComNomeDoCliente() throws Exception {
        when(service.listar()).thenReturn(List.of(new TransacaoResponse(10L, 1L, "Ana Souza", "Depósito inicial",
                new BigDecimal("1500.00"), "CREDITO", LocalDateTime.of(2026, 9, 29, 10, 0))));

        mvc.perform(get("/api/transacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomeCliente").value("Ana Souza"))
                .andExpect(jsonPath("$[0].valor").value(1500.00));
    }

    @Test
    void post_valorZero_retorna400() throws Exception {
        mvc.perform(post("/api/transacoes").contentType(APPLICATION_JSON)
                        .content("{\"idCliente\":1,\"descricao\":\"Teste\",\"valor\":0,\"tipo\":\"CREDITO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.valor").value("O valor deve ser maior que zero."));

        verifyNoInteractions(service);
    }

    @Test
    void post_clienteInexistente_retorna404() throws Exception {
        when(service.criar(any(TransacaoRequest.class)))
                .thenThrow(new RecursoNaoEncontradoException("Cliente não encontrado para o id 42"));

        mvc.perform(post("/api/transacoes").contentType(APPLICATION_JSON)
                        .content("{\"idCliente\":42,\"descricao\":\"Teste\",\"valor\":10.00,\"tipo\":\"CREDITO\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Cliente não encontrado para o id 42"));
    }
}
