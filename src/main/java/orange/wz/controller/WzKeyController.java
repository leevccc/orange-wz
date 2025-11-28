package orange.wz.controller;

import lombok.RequiredArgsConstructor;
import orange.wz.model.ResultBody;
import orange.wz.model.WzKey;
import orange.wz.service.WzKeyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/wzKey")
@RequiredArgsConstructor
public class WzKeyController {
    private final WzKeyService wzKeyService;

    @GetMapping
    public ResultBody<List<WzKey>> findAll() {
        return ResultBody.success(wzKeyService.findAll());
    }

    @PostMapping
    public ResultBody<WzKey> save(@RequestBody WzKey wzKey) {
        return ResultBody.success(wzKeyService.save(wzKey));
    }

    @PostMapping("/{id}")
    public ResultBody<WzKey> update(@PathVariable int id, @RequestBody WzKey wzKey) {
        wzKeyService.update(id, wzKey);
        return ResultBody.success();
    }

    @DeleteMapping("/{id}")
    public ResultBody<WzKey> delete(@PathVariable int id) {
        wzKeyService.delete(id);
        return ResultBody.success();
    }
}
