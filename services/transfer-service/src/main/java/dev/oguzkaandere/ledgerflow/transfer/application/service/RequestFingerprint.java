package dev.oguzkaandere.ledgerflow.transfer.application.service;

import dev.oguzkaandere.ledgerflow.transfer.application.command.CreateTransferCommand;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class RequestFingerprint {
    private RequestFingerprint() {}

    public static String forCommand(CreateTransferCommand command) {
        String canonical = String.join(
                "\n",
                command.sourceAccountId().toString().toLowerCase(),
                command.destinationAccountId().toString().toLowerCase(),
                command.money().canonicalAmount(),
                command.money().currency().name(),
                command.reference().value());
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", impossible);
        }
    }
}
