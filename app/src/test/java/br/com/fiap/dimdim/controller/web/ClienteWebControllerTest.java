package br.com.fiap.dimdim.controller.web;

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
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ClienteWebController.class)
class ClienteWebControllerTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 29, 10, 30);

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ClienteService service;

    @Test
    void lista_comClientes_exibeNomesComAcentoEData() throws Exception {
        when(service.listar()).thenReturn(List.of(
                new ClienteResponse(1L, "João Conceição", "11122233344", "joao@exemplo.com", AGORA)));

        mvc.perform(get("/clientes"))
                .andExpect(status().isOk())
                .andExpect(view().name("clientes/lista"))
                .andExpect(content().string(containsString("João Conceição")))
                .andExpect(content().string(containsString("29/09/2026 10:30")));
    }

    @Test
    void criar_cpfInvalido_reexibeFormularioComErro() throws Exception {
        mvc.perform(post("/clientes")
                        .param("nome", "Ana Souza").param("cpf", "123").param("email", "ana@exemplo.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("clientes/form"))
                .andExpect(model().attributeHasFieldErrors("form", "cpf"))
                .andExpect(content().string(containsString("O CPF deve conter exatamente 11 dígitos numéricos.")));

        verifyNoInteractions(service);
    }

    @Test
    void criar_dadosValidos_redirecionaComMensagemDeSucesso() throws Exception {
        when(service.criar(any(ClienteRequest.class))).thenReturn(
                new ClienteResponse(1L, "Ana Souza", "11122233344", "ana@exemplo.com", AGORA));

        mvc.perform(post("/clientes")
                        .param("nome", "Ana Souza").param("cpf", "11122233344").param("email", "ana@exemplo.com"))
                .andExpect(redirectedUrl("/clientes"))
                .andExpect(flash().attribute("sucesso", "Cliente cadastrado com sucesso."));
    }

    @Test
    void criar_cpfDuplicado_reexibeFormularioComErroDoBanco() throws Exception {
        when(service.criar(any(ClienteRequest.class)))
                .thenThrow(new RegraIntegridadeException("Já existe um cliente cadastrado com este CPF."));

        mvc.perform(post("/clientes")
                        .param("nome", "Ana Souza").param("cpf", "11122233344").param("email", "ana@exemplo.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("clientes/form"))
                .andExpect(content().string(containsString("Já existe um cliente cadastrado com este CPF.")));
    }

    @Test
    void excluir_clienteComTransacoes_redirecionaComErro() throws Exception {
        doThrow(new RegraIntegridadeException("Não é possível excluir o cliente: existem transações vinculadas a ele."))
                .when(service).excluir(1L);

        mvc.perform(post("/clientes/1/excluir"))
                .andExpect(redirectedUrl("/clientes"))
                .andExpect(flash().attribute("erro", containsString("transações vinculadas")));
    }

    @Test
    void editar_idInexistente_exibePagina404() throws Exception {
        when(service.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Cliente não encontrado para o id 99"));

        mvc.perform(get("/clientes/99/editar"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("erro"))
                .andExpect(content().string(containsString("Cliente não encontrado para o id 99")));
    }
}
