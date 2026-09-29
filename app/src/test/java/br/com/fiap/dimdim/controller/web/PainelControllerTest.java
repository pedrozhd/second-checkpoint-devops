package br.com.fiap.dimdim.controller.web;

import br.com.fiap.dimdim.service.ClienteService;
import br.com.fiap.dimdim.service.TransacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PainelController.class)
class PainelControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ClienteService clienteService;

    @MockBean
    private TransacaoService transacaoService;

    @Test
    void painel_exibeTotaisESaldoFormatado() throws Exception {
        when(clienteService.contar()).thenReturn(2L);
        when(transacaoService.contar()).thenReturn(3L);
        when(transacaoService.saldoGeral()).thenReturn(new BigDecimal("5449.25"));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("painel"))
                .andExpect(content().string(containsString("R$ 5.449,25")));
    }
}
