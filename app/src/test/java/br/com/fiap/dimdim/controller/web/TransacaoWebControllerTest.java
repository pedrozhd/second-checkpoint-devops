package br.com.fiap.dimdim.controller.web;

import br.com.fiap.dimdim.dto.ClienteResponse;
import br.com.fiap.dimdim.dto.TransacaoRequest;
import br.com.fiap.dimdim.dto.TransacaoResponse;
import br.com.fiap.dimdim.service.ClienteService;
import br.com.fiap.dimdim.service.TransacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TransacaoWebController.class)
class TransacaoWebControllerTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 29, 10, 30);
    private static final ClienteResponse ANA = new ClienteResponse(1L, "Ana Souza", "11122233344", "ana@exemplo.com", AGORA);
    private static final TransacaoResponse DEPOSITO = new TransacaoResponse(10L, 1L, "Ana Souza", "Depósito inicial",
            new BigDecimal("1500.00"), "CREDITO", AGORA);

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TransacaoService transacaoService;

    @MockBean
    private ClienteService clienteService;

    @Test
    void lista_exibeNomeDoClienteEValorFormatado() throws Exception {
        when(transacaoService.listar()).thenReturn(List.of(DEPOSITO));

        mvc.perform(get("/transacoes"))
                .andExpect(status().isOk())
                .andExpect(view().name("transacoes/lista"))
                .andExpect(content().string(containsString("Ana Souza")))
                .andExpect(content().string(containsString("Depósito inicial")))
                .andExpect(content().string(containsString("R$ 1.500,00")));
    }

    @Test
    void nova_semClientesCadastrados_exibeAviso() throws Exception {
        when(clienteService.listar()).thenReturn(List.of());

        mvc.perform(get("/transacoes/nova"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cadastre um cliente antes de registrar transações.")));
    }

    @Test
    void criar_valorNaoNumerico_exibeMensagemAmigavel() throws Exception {
        when(clienteService.listar()).thenReturn(List.of(ANA));

        mvc.perform(post("/transacoes")
                        .param("idCliente", "1").param("descricao", "Teste")
                        .param("valor", "abc").param("tipo", "CREDITO"))
                .andExpect(status().isOk())
                .andExpect(view().name("transacoes/form"))
                .andExpect(content().string(containsString("Informe um valor numérico válido.")));

        verifyNoInteractions(transacaoService);
    }

    @Test
    void criar_dadosValidos_redirecionaComSucesso() throws Exception {
        when(transacaoService.criar(any(TransacaoRequest.class))).thenReturn(DEPOSITO);

        mvc.perform(post("/transacoes")
                        .param("idCliente", "1").param("descricao", "Depósito inicial")
                        .param("valor", "1500.00").param("tipo", "CREDITO"))
                .andExpect(redirectedUrl("/transacoes"))
                .andExpect(flash().attribute("sucesso", "Transação registrada com sucesso."));
    }
}
