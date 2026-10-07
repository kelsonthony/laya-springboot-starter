package io.github.kelsonthony.laya.example;

import io.github.kelsonthony.laya.*;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TriageController {
    private final LayaClient laya;
    public TriageController(LayaClient laya) { this.laya = laya; }

    @PostMapping("/triage")
    public TriageResult triage(@RequestBody Ticket ticket) {
        if (ticket.message() == null || ticket.message().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a mensagem do chamado");
        var result = laya.evaluate(Map.of("message", ticket.message()), Map.of(
            "department", Question.choice("Qual equipe deve atender a mensagem?", Map.of(
                "financeiro", "Cobranças, pagamentos e reembolsos",
                "integracoes", "Integrações e APIs de outros serviços",
                "suporte", "Acesso, erros e ajuda com o produto")),
            "urgent", Question.noul("A mensagem descreve um problema que exige atenção urgente?"),
            "severity", Question.score("Qual a gravidade do problema descrito?",
                "Leve: dúvida ou problema cosmético", "Moderado: existe uma alternativa", "Grave: operação bloqueada")));
        return new TriageResult(result.choice("department").choice(), result.choice("department").confidence(),
            result.noul("urgent").noul(), result.score("severity").score(), result.model());
    }

    public record Ticket(String message) {}
    public record TriageResult(String department, double confidence, double urgency, double severity, String model) {}
}
