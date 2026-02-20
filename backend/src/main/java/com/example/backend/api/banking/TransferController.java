package com.example.backend.api.banking;

import com.example.backend.core.banking.BankingService;
import com.example.backend.core.transfer.TransferEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    public TransferController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    public record TransferRequest(
            @NotBlank
            @Pattern(regexp = "^IT\\d{2}[A-Z]\\d{5}\\d{5}\\d{12}$", message = "Invalid Italian IBAN format")
            String toIban,

            @DecimalMin(value = "0.01", message = "Amount must be >= 0.01")
            @Digits(integer = 17, fraction = 2, message = "Amount must have max 2 decimals")
            BigDecimal amount
    ) {}

    public record TransferResponse(UUID id, Instant createdAt, BigDecimal newBalance) {}

    public record TransferItem(UUID id, String toIban, BigDecimal amount, Instant createdAt) {}

    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('SCOPE_write')")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest req, Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID uid = UUID.fromString(jwt.getClaimAsString("uid"));

        BankingService.TransferResult result = bankingService.transfer(uid, req.toIban(), req.amount());
        return new TransferResponse(result.transferId(), result.createdAt(), result.newBalance());
    }

    @GetMapping("/transfers")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    public List<TransferItem> latest(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID uid = UUID.fromString(jwt.getClaimAsString("uid"));

        return bankingService.latestTransfers(uid).stream()
                .map(t -> new TransferItem(t.getId(), t.getToIban(), t.getAmount(), t.getCreatedAt()))
                .toList();
    }
}
