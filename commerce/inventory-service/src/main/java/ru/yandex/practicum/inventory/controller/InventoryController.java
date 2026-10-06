package ru.yandex.practicum.inventory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.service.InventoryService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ControllerConstants.URL_API + ControllerConstants.URL_INVENTORY)
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    public List<InventoryDto> getAllInventoryUnits() {
        return inventoryService.getAllInventoryUnits();
    }

    @GetMapping(ControllerConstants.ID_PRODUCT)
    public InventoryDto getRemainingProductQuantities(@PathVariable long productId) {
        return inventoryService.getRemainingProductQuantities(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryDto createInventoryUnit(@Valid @RequestBody ReserveRequest request) {
        return inventoryService.createInventoryUnit(request);
    }

    @PostMapping(ControllerConstants.URL_RESERVE)
    public ReserveResponse createProductReservation(@Valid @RequestBody ReserveRequest request) {
        return inventoryService.createProductReservation(request);
    }

    @PutMapping
    public InventoryDto updateInventoryUnit(@Valid @RequestBody UpdateInventoryRequest request) {
        return inventoryService.updateInventoryUnit(request);
    }
}
