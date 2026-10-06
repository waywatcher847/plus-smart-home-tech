package ru.yandex.practicum.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.InventoryUnit;
import ru.yandex.practicum.inventory.exception.AlreadyExistsException;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.NotFoundException;
import ru.yandex.practicum.inventory.repository.InventoryMapper;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;

    @Override
    public List<InventoryDto> getAllInventoryUnits() {
        log.trace("getAllInventoryUnits");
        List<InventoryUnit> inventoryUnits = inventoryRepository.findAll();
        log.debug("{}", inventoryUnits);
        return inventoryUnits.stream()
                .map(InventoryMapper::toInventoryDto)
                .toList();
    }

    @Override
    public InventoryDto updateInventoryUnit(UpdateInventoryRequest request) {
        log.trace("updateInventoryUnit {}", request);
        InventoryUnit inventoryUnit = downloadInventoryUnit(request.productId());

        int updatedAvailableQuantity = request.quantity() - inventoryUnit.getReservedQuantity();
        if (updatedAvailableQuantity < 0) {
            log.warn("request.quantity({}) < inventoryUnit.getReservedQuantity({})", request.quantity(), inventoryUnit.getReservedQuantity());
            throw new InsufficientStockException(String.format("\"CANCELED request.quantity(%d)<" +
                    " inventoryUnit.getReservedQuantity (%d)", request.quantity(), inventoryUnit.getReservedQuantity()));
        }

        inventoryUnit.setQuantity(request.quantity());
        inventoryUnit.setAvailableQuantity(updatedAvailableQuantity);
        InventoryUnit updatedInventoryUnit = inventoryRepository.save(inventoryUnit);
        log.debug("OK {}", updatedInventoryUnit);
        return InventoryMapper.toInventoryDto(updatedInventoryUnit);
    }

    @Override
    public InventoryDto createInventoryUnit(ReserveRequest request) {
        log.trace("createInventoryUnit {}", request);
        boolean isProductAlreadyExists = inventoryRepository.existsByProductId(request.productId());

        if (isProductAlreadyExists) {
            log.warn("CANCELED request.productId({}) AlreadyExistsException", request.productId());
            throw new AlreadyExistsException(String.format("CANCELED request.productId(%d) AlreadyExistsException", request.productId()));
        }

        InventoryUnit inventoryUnit = InventoryUnit.builder()
                .productId(request.productId())
                .quantity(request.quantity())
                .availableQuantity(request.quantity())
                .build();
        InventoryUnit savedInventoryUnit = inventoryRepository.save(inventoryUnit);
        log.debug("OK {}", savedInventoryUnit);
        return InventoryMapper.toInventoryDto(savedInventoryUnit);
    }

    @Override
    public ReserveResponse createProductReservation(ReserveRequest request) {
        log.trace("createProductReservation {}", request);
        InventoryUnit inventoryUnit = downloadInventoryUnit(request.productId());

        if (request.quantity() > inventoryUnit.getAvailableQuantity()) {
            log.warn("CANCELED ID({}) request.quantity({}) > inventoryUnit.getAvailableQuantity({})", inventoryUnit.getProductId(), request.quantity(), inventoryUnit.getAvailableQuantity());
            throw new InsufficientStockException(String.format("CANCELED ID(%d) request.quantity(%d) > inventoryUnit.getAvailableQuantity(%d)", inventoryUnit.getProductId(), request.quantity(), inventoryUnit.getAvailableQuantity()));
        }

        inventoryUnit.setReservedQuantity(inventoryUnit.getReservedQuantity() + request.quantity());
        inventoryUnit.setAvailableQuantity(inventoryUnit.getAvailableQuantity() - request.quantity());
        InventoryUnit savedInventoryUnit = inventoryRepository.save(inventoryUnit);
        inventoryRepository.flush();
        log.debug("OK, {}", savedInventoryUnit);
        return new ReserveResponse(true, savedInventoryUnit.getAvailableQuantity(), "OK");
    }

    @Override
    public InventoryDto getRemainingProductQuantities(long productId) {
        log.trace("getRemainingProductQuantities {}", productId);
        InventoryUnit inventoryUnit = downloadInventoryUnit(productId);
        log.debug("OK {}", inventoryUnit);
        return InventoryMapper.toInventoryDto(inventoryUnit);
    }

    private InventoryUnit downloadInventoryUnit(long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> {
                    log.warn("{} NotFoundException", productId);
                    return new NotFoundException(String.format("NotFoundException %d ", productId));
                });
    }
}
