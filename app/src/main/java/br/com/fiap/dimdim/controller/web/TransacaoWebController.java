package br.com.fiap.dimdim.controller.web;

import br.com.fiap.dimdim.dto.TransacaoRequest;
import br.com.fiap.dimdim.exception.RegraIntegridadeException;
import br.com.fiap.dimdim.service.ClienteService;
import br.com.fiap.dimdim.service.TransacaoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Telas de transacao. O formulario lista os clientes num select: e ali que
 * o relacionamento cliente 1:N transacao aparece para o usuario.
 */
@Controller
@RequestMapping("/transacoes")
public class TransacaoWebController {

    private static final String FORMULARIO = "transacoes/form";
    private static final String REDIRECT_LISTA = "redirect:/transacoes";
    private static final List<String> TIPOS = List.of("CREDITO", "DEBITO");

    private final TransacaoService transacaoService;
    private final ClienteService clienteService;

    public TransacaoWebController(TransacaoService transacaoService, ClienteService clienteService) {
        this.transacaoService = transacaoService;
        this.clienteService = clienteService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("transacoes", transacaoService.listar());
        return "transacoes/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        TransacaoRequest form = new TransacaoRequest();
        form.setTipo("CREDITO");
        model.addAttribute("form", form);
        return exibirFormulario(model, "Nova transação", "/transacoes");
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") TransacaoRequest form, BindingResult resultado,
                        Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return exibirFormulario(model, "Nova transação", "/transacoes");
        }
        try {
            transacaoService.criar(form);
        } catch (RegraIntegridadeException ex) {
            model.addAttribute("erro", ex.getMessage());
            return exibirFormulario(model, "Nova transação", "/transacoes");
        }
        redirect.addFlashAttribute("sucesso", "Transação registrada com sucesso.");
        return REDIRECT_LISTA;
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", TransacaoRequest.de(transacaoService.buscar(id)));
        return exibirFormulario(model, "Editar transação", "/transacoes/" + id);
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") TransacaoRequest form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return exibirFormulario(model, "Editar transação", "/transacoes/" + id);
        }
        try {
            transacaoService.atualizar(id, form);
        } catch (RegraIntegridadeException ex) {
            model.addAttribute("erro", ex.getMessage());
            return exibirFormulario(model, "Editar transação", "/transacoes/" + id);
        }
        redirect.addFlashAttribute("sucesso", "Transação atualizada com sucesso.");
        return REDIRECT_LISTA;
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        transacaoService.excluir(id);
        redirect.addFlashAttribute("sucesso", "Transação excluída com sucesso.");
        return REDIRECT_LISTA;
    }

    private String exibirFormulario(Model model, String titulo, String acao) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("acao", acao);
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("tipos", TIPOS);
        return FORMULARIO;
    }
}
