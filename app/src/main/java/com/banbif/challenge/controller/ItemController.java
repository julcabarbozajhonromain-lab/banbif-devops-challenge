package com.banbif.challenge.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/v1")
public class ItemController {

    private final List<Item> items = new CopyOnWriteArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public ItemController() {
        items.add(new Item(idGenerator.getAndIncrement(), "Cuenta Digital BanBif", "Productos Bancarios", Instant.now().toString()));
        items.add(new Item(idGenerator.getAndIncrement(), "Tarjeta de Crédito Signature", "Tarjetas", Instant.now().toString()));
    }

    @GetMapping("/hello")
    public ResponseEntity<Map<String, Object>> hello() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "Bienvenido al API de BanBif DevOps Challenge",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/items")
    public ResponseEntity<List<Item>> getItems() {
        return ResponseEntity.ok(items);
    }

    @PostMapping("/items")
    public ResponseEntity<Item> createItem(@RequestBody ItemRequest request) {
        String name = (request != null && request.name() != null && !request.name().isBlank())
                ? request.name().trim()
                : "Unnamed Item";
        String category = (request != null && request.category() != null && !request.category().isBlank())
                ? request.category().trim()
                : "General";

        Item created = new Item(
                idGenerator.getAndIncrement(),
                name,
                category,
                Instant.now().toString()
        );
        items.add(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    public record Item(Long id, String name, String category, String createdAt) {}

    public record ItemRequest(String name, String category) {}
}
