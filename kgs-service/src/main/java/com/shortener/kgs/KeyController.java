package com.shortener.kgs;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Internal API. Never expose this through the public gateway. */
@RestController
@RequestMapping("/internal/keys")
@Validated
class KeyController {

    private final KeyRangeAllocator allocator;

    KeyController(KeyRangeAllocator allocator) {
        this.allocator = allocator;
    }

    @GetMapping("/batch")
    List<String> batch(@RequestParam(defaultValue = "1000") @Min(1) @Max(50_000) int size) throws Exception {
        return allocator.nextBatch(size);
    }
}
