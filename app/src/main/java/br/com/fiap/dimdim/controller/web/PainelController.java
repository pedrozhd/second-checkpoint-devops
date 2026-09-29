package br.com.fiap.dimdim.controller.web;

import br.com.fiap.dimdim.dto.PainelResumo;
import br.com.fiap.dimdim.service.ClienteService;
import br.com.fiap.dimdim.service.TransacaoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PainelController {

    private final ClienteService clienteService;
    private final TransacaoService transacaoService;

    public PainelController(ClienteService clienteService, TransacaoService transacaoService) {
        this.clienteService = clienteService;
        this.transacaoService = transacaoService;
    }

    @GetMapping("/")
    public String painel(Model model) {
        model.addAttribute("resumo", new PainelResumo(
                clienteService.contar(), transacaoService.contar(), transacaoService.saldoGeral()));
        return "painel";
    }
}
