package error.pirate.backend.item.query.controller;

import error.pirate.backend.item.query.dto.ItemFilterDTO;
import error.pirate.backend.item.query.dto.ItemSearchDTO;
import error.pirate.backend.item.query.service.ItemQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
@Tag(name = "품목 관리", description = "품목 관리 API")
public class ItemQueryContoller {

    private final ItemQueryService itemQueryService;

    @GetMapping("/read")
    @Operation(summary = "품목 검색", description = "품목을 검색한다.")
    public ResponseEntity<List<ItemSearchDTO>> searchItem(ItemFilterDTO itemFilterDTO) {
        List<ItemSearchDTO> itemSearchDTO = itemQueryService.findItem(itemFilterDTO);
        return ResponseEntity.ok(itemSearchDTO);
    }
}

