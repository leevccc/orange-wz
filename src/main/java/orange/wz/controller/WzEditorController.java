package orange.wz.controller;

import lombok.RequiredArgsConstructor;
import orange.wz.model.ResultBody;
import orange.wz.model.WzNode;
import orange.wz.model.WzNodeValueDto;
import orange.wz.service.WzEditorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/wzEditor")
@RequiredArgsConstructor
public class WzEditorController {
    private final WzEditorService wzEditorService;

    @DeleteMapping("/cache")
    public ResultBody<String> deleteCache() {
        return ResultBody.success(wzEditorService.gc(true));
    }

    @DeleteMapping("/gc")
    public ResultBody<String> gc() {
        return ResultBody.success(wzEditorService.gc(false));
    }

    @GetMapping("/views")
    public ResultBody<Set<Integer>> getViews() {
        return ResultBody.success(wzEditorService.getViews());
    }

    @PostMapping("/views")
    public ResultBody<Integer> addView() {
        return ResultBody.success(wzEditorService.addView());
    }

    @PostMapping("/views/{node}/move/{view}")
    public ResultBody<Void> moveView(@PathVariable int node, @PathVariable int view) {
        wzEditorService.moveView(node, view);
        return ResultBody.success();
    }

    @DeleteMapping("/views/{view}")
    public ResultBody<Void> removeView(@PathVariable int view) {
        wzEditorService.removeView(view);
        return ResultBody.success();
    }

    @GetMapping("/folder/{id}")
    public ResultBody<List<WzNode>> getFolder(@PathVariable int id) {
        return ResultBody.success(wzEditorService.getFolder(id));
    }

    @DeleteMapping("/folder")
    public ResultBody<Void> reloadFolder() {
        wzEditorService.reloadFolder();
        return ResultBody.success();
    }

    @GetMapping("/load/{id}")
    public ResultBody<Void> load(@PathVariable int id, @RequestParam("view") int view, @RequestParam("v") Short v, @RequestParam("k") String k) {
        wzEditorService.load(id, view, v, k);
        return ResultBody.success();
    }

    @DeleteMapping("/unload/{id}")
    public ResultBody<Void> unload(@PathVariable int id) {
        wzEditorService.unload(id, true);
        return ResultBody.success();
    }

    @GetMapping("/node/{id}")
    public ResultBody<List<WzNode>> getNode(@PathVariable int id) {
        return ResultBody.success(wzEditorService.getNode(id));
    }

    @GetMapping("/node/{id}/value")
    public ResultBody<WzNodeValueDto> getValue(@PathVariable int id) {
        return ResultBody.success(wzEditorService.getValue(id));
    }

    @PostMapping("/node/{id}/value")
    public ResultBody<Void> updateValue(@PathVariable int id, @RequestBody WzNodeValueDto data) {
        wzEditorService.updateValue(id, data);
        return ResultBody.success();
    }

    @GetMapping("/copy")
    public ResultBody<Void> copy(@RequestParam("key") int[] key) {
        wzEditorService.copy(key);
        return ResultBody.success();
    }

    @GetMapping("/paste/{id}")
    public ResultBody<Void> paste(@PathVariable int id) {
        wzEditorService.paste(id);
        return ResultBody.success();
    }

    @PostMapping("/node/{id}/sub")
    public ResultBody<WzNode> addNode(@PathVariable int id, @RequestBody WzNodeValueDto data) {
        return ResultBody.success(wzEditorService.addNode(id, data));
    }

    @DeleteMapping("/node/{id}")
    public ResultBody<Void> deleteNode(@PathVariable int id) {
        wzEditorService.deleteNode(id);
        return ResultBody.success();
    }

    @GetMapping("/node/{id}/save")
    public ResultBody<Void> saveNode(@PathVariable int id) {
        wzEditorService.saveNode(id);
        return ResultBody.success();
    }

    @GetMapping("/tools/export/img/{id}")
    public ResultBody<Void> exportWzFileToImg(@PathVariable int id) {
        wzEditorService.exportWzFileToImg(id, null);
        return ResultBody.success();
    }

    @GetMapping("/tools/export/xml/{id}")
    public ResultBody<Void> exportWzFileToXml(@PathVariable int id, @RequestParam(value = "indent", required = false) boolean indent) {
        wzEditorService.exportWzFileToXml(id, null, indent);
        return ResultBody.success();
    }

    @GetMapping("/tools/outlink")
    public ResultBody<Void> fixOutlink(@RequestParam("id") int[] id) {
        wzEditorService.fixOutLink(id);
        return ResultBody.success();
    }

    @GetMapping("/tools/updateKey/{id}")
    public ResultBody<Void> updateKey(@PathVariable int id, @RequestParam("v") short v, @RequestParam("k") String k) {
        wzEditorService.updateKey(id, v, k);
        return ResultBody.success();
    }

    @GetMapping("/tools/localization")
    public ResultBody<Void> localization(@RequestParam("from") int from, @RequestParam("to") int to) {
        wzEditorService.localization(from, to);
        return ResultBody.success();
    }

    @GetMapping("/tools/packet")
    public ResultBody<Void> packet(@RequestParam("fileVersion") short fileVersion, @RequestParam("id") int id) {
        wzEditorService.packet(fileVersion, id);
        return ResultBody.success();
    }

    @GetMapping("/tools/canvas")
    public ResultBody<List<WzNodeValueDto>> packet(@RequestParam("id") int id) {
        return ResultBody.success(wzEditorService.getAllCanvas(id));
    }
}
