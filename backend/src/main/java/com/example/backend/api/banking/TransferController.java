package com.example.backend.api.banking;

import com.example.backend.core.banking.BankingService;
import com.example.backend.core.fraud.FraudDecision;
import com.example.backend.core.fraud.FraudService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/banking")
public class TransferController {

    private final BankingService bankingService;
    private final FraudService fraudService;

    public TransferController(BankingService bankingService, FraudService fraudService) {
        this.bankingService = bankingService;
        this.fraudService = fraudService;
    }

    public record TransferRequest(
            @NotBlank
            @Pattern(regexp = "^IT\\d{2}[A-Z]\\d{5}\\d{5}\\d{12}$", message = "Invalid Italian IBAN format")
            String toIban,

            @DecimalMin(value = "0.01", message = "Amount must be >= 0.01")
            @Digits(integer = 17, fraction = 2, message = "Amount must have max 2 decimals")
            BigDecimal amount,

            // causale FACOLTATIVA
            @Size(max = 140, message = "Causal max 140 chars")
            String causal
    ) {}

    public record TransferResponse(UUID id, Instant createdAt, BigDecimal newBalance) {}

    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('SCOPE_write')")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest req, Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID uid = UUID.fromString(jwt.getClaimAsString("uid"));
        Instant now = Instant.now();

        // 0) se già lockato -> blocco temporaneo
        FraudService.LockStatus ls = fraudService.checkAndClearLock(uid, now);
        if (ls.locked()) {
            throw new FraudExceptions.FraudBlockedException(
                    List.of("Account temporarily locked"),
                    ls.lockedUntil()
            );
        }

        // 1) Valuta rischio
        FraudDecision decision = fraudService.evaluate(uid, req.toIban(), req.amount(), now);

        boolean hasStepUp = Boolean.TRUE.equals(jwt.getClaimAsBoolean("stepup"));

        // Se NON è ALLOW e non hai step-up -> chiedi reinserimento password
        if (decision.action() != FraudDecision.Action.ALLOW && !hasStepUp) {
            throw new FraudExceptions.FraudStepUpRequiredException(decision.reasons());
        }

        // 2) Esegui bonifico
        BankingService.TransferResult result =
                bankingService.transfer(uid, req.toIban(), req.amount(), req.causal());

        return new TransferResponse(result.transferId(), result.createdAt(), result.newBalance());
    }

    // Movimenti
    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    public List<BankingService.MovementItem> movements(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID uid = UUID.fromString(jwt.getClaimAsString("uid"));
        return bankingService.latestMovements(uid);
    }

    // Alias compatibilità
    @GetMapping("/transfers")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    public List<BankingService.MovementItem> transfers(Authentication authentication) {
        return movements(authentication);
    }
}
