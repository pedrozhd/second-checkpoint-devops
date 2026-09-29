package br.com.fiap.dimdim.controller.web;

import br.com.fiap.dimdim.dto.ClienteRequest;
import br.com.fiap.dimdim.exception.RegraIntegridadeException;
import br.com.fiap.dimdim.service.ClienteService;
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

/**
 * Telas de cliente. Toda gravacao segue Post/Redirect/Get: recarregar a
 * pagina depois de salvar nao reenvia o formulario.
 */
@Controller
@RequestMapping("/clientes")
public class ClienteWebController {

    private static final String FORMULARIO = "clientes/form";
    private static final String REDIRECT_LISTA = "redirect:/clientes";

    private final ClienteService service;

    public ClienteWebController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", service.listar());
        return "clientes/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new ClienteRequest());
        return exibirFormulario(model, "Novo cliente", "/clientes");
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") ClienteRequest form, BindingResult resultado,
                        Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return exibirFormulario(model, "Novo cliente", "/clientes");
        }
        try {
            service.criar(form);
        } catch (RegraIntegridadeException ex) {
            model.addAttribute("erro", ex.getMessage());
            return exibirFormulario(model, "Novo cliente", "/clientes");
        }
        redirect.addFlashAttribute("sucesso", "Cliente cadastrado com sucesso.");
        return REDIRECT_LISTA;
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", ClienteRequest.de(service.buscar(id)));
        return exibirFormulario(model, "Editar cliente", "/clientes/" + id);
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") ClienteRequest form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return exibirFormulario(model, "Editar cliente", "/clientes/" + id);
        }
        try {
            service.atualizar(id, form);
        } catch (RegraIntegridadeException ex) {
            model.addAttribute("erro", ex.getMessage());
            return exibirFormulario(model, "Editar cliente", "/clientes/" + id);
        }
        redirect.addFlashAttribute("sucesso", "Cliente atualizado com sucesso.");
        return REDIRECT_LISTA;
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addFlashAttribute("sucesso", "Cliente excluído com sucesso.");
        } catch (RegraIntegridadeException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return REDIRECT_LISTA;
    }

    private String exibirFormulario(Model model, String titulo, String acao) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("acao", acao);
        return FORMULARIO;
    }
}
