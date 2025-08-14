package org.example.controller;

import org.example.entity.User;
import org.example.service.UserService;
import org.example.util.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final Map<String, String> nonceStorage = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    @GetMapping("/nonce/{walletAddress}")
    public ResponseEntity<Map<String, String>> getNonce(@PathVariable String walletAddress) {
        if (!Keys.isValidAddress(walletAddress)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid wallet address"));
        }

        String nonce = new BigInteger(130, random).toString(32);
        nonceStorage.put(walletAddress.toLowerCase(), nonce);

        return ResponseEntity.ok(Map.of("nonce", nonce));
    }

    @PostMapping("/metamask")
    public ResponseEntity<Map<String, String>> loginWithMetaMask(@RequestBody Map<String, String> payload) {
        String walletAddress = payload.get("walletAddress");
        String signature = payload.get("signature");

        if (walletAddress == null || signature == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Missing parameters"));

        walletAddress = walletAddress.toLowerCase();
        String nonce = nonceStorage.get(walletAddress);

        if (nonce == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Nonce not found. Request a new one."));

        try {
            String message = "\u0019Ethereum Signed Message:\n" + nonce.length() + nonce;
            byte[] msgHash = Sign.getEthereumMessageHash(message.getBytes(StandardCharsets.UTF_8));

            Sign.SignatureData sigData = parseSignature(signature);
            String recoveredAddress = "0x" + Keys.getAddress(Sign.signedMessageToKey(msgHash, sigData));

            if (!walletAddress.equals(recoveredAddress.toLowerCase())) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid signature"));
            }

            Optional<User> optionalUser = userService.findByUsername(walletAddress);
            User user = optionalUser.orElseGet(() -> {
                User newUser = new User();
                newUser.setUsername(walletAddress);
                newUser.setEmail(walletAddress + "@metamask.local");
                return userService.save(newUser);
            });

            String token = jwtTokenProvider.createToken(user.getUsername(), user.getId());

            nonceStorage.remove(walletAddress);

            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Authentication failed"));
        }
    }

    private Sign.SignatureData parseSignature(String signatureHex) {
        byte[] sig = hexStringToByteArray(signatureHex.replace("0x", ""));
        byte v = sig[64];
        if (v < 27) v += 27;
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(sig, 0, r, 0, 32);
        System.arraycopy(sig, 32, s, 0, 32);
        return new Sign.SignatureData(v, r, s);
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
