package orange.wz.gui.utils;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.MainFrame;
import orange.wz.provider.*;
import orange.wz.provider.properties.WzCanvasProperty;
import orange.wz.provider.properties.WzStringProperty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public final class Outlink {
    private static String lastCanvasPath;
    private static final List<WzDirectory> canvasCache = new ArrayList<>();

    record Data(WzCanvasProperty object, List<String> path) {
    }

    public static boolean replace(List<WzObject> objects) {
        if (!loadCanvasFiles(objects.getFirst())) {
            resetCache();
            return false;
        }

        Map<String, List<Data>> collector = new HashMap<>();
        collect(collector, objects);

        int total = collector.values().stream()
                .mapToInt(List::size)
                .sum();

        replace(collector, total);
        return true;
    }

    private static void resetCache() {
        lastCanvasPath = null;
        canvasCache.clear();
    }

    private static boolean loadCanvasFiles(WzObject wzObject) {
        WzFile wzFile = getWzFile(wzObject);
        if (wzFile == null) return false;

        File file = new File(wzFile.getFilePath());
        String dirPath = file.getParent();
        String canvasPath = Path.of(dirPath, "_Canvas").toString();

        if (lastCanvasPath != null && lastCanvasPath.equals(canvasPath)) return true;
        resetCache();

        // 确保version已经生成
        if (!wzFile.parse()) {
            MainFrame.getInstance().setStatusText("文件 %s 解析失败", wzFile.getName());
            throw new RuntimeException();
        }

        short version = wzFile.getHeader().getFileVersion();
        byte[] iv = wzFile.getWzIv();
        byte[] key = wzFile.getUserKey();

        lastCanvasPath = canvasPath;

        try (Stream<Path> pathStream = Files.walk(Path.of(canvasPath))) {
            for (Path path : pathStream.toList()) {
                if (!Files.isRegularFile(path)) continue;

                String fileName = path.getFileName().toString();
                if (!fileName.endsWith(".wz")) continue;
                if (!fileName.startsWith("_Canvas_")) continue;

                WzFile canvasWz = new WzFile(path.toString(), version, iv, key);
                if (!canvasWz.parse()) {
                    MainFrame.getInstance().setStatusText("文件 %s 解析失败", canvasWz.getName());
                    throw new RuntimeException();
                }
                canvasCache.add(canvasWz.getWzDirectory());
            }
            return true;
        } catch (IOException e) {
            log.error("收集Canvas文件时出错: {}", e.getMessage());
            return false;
        }
    }

    private static WzFile getWzFile(WzObject wzObject) {
        if (wzObject instanceof WzFile wz) {
            return wz;
        } else if (wzObject instanceof WzDirectory wzDir) {
            return wzDir.getWzFile();
        } else if (wzObject instanceof WzImage wzImg) {
            return getWzFile(wzImg.getParent());
        } else if (wzObject instanceof WzImageProperty property) {
            return getWzFile(property.getWzImage());
        }

        return null;
    }

    private static void collect(Map<String, List<Data>> collector, List<? extends WzObject> objects) {
        for (WzObject wzObject : objects) {
            if (wzObject instanceof WzCanvasProperty property) {
                List<String> path = getOutlinkString(property);
                if (path == null || path.isEmpty()) return;

                String image = null;
                for (String pathStr : path) {
                    if (pathStr.endsWith(".img")) {
                        image = pathStr;
                        break;
                    }
                }
                if (image == null) return;

                List<Data> dataList = collector.get(image);
                if (dataList == null) {
                    dataList = new ArrayList<>();
                    dataList.add(new Data(property, path));
                    collector.put(image, dataList);
                }
                dataList.add(new Data(property, path));
            } else if (wzObject instanceof WzDirectory wzDir) {
                collect(collector, wzDir.getChildren());
            } else if (wzObject instanceof WzImage wzImg) {
                if (!wzImg.parse()) {
                    MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", wzImg.getName(), wzImg.getStatus().getMessage());
                    throw new RuntimeException();
                }
                collect(collector, wzImg.getChildren());
            } else if (wzObject instanceof WzImageProperty property && property.isListProperty()) {
                collect(collector, property.getChildren());
            }
        }
    }

    private static List<String> getOutlinkString(WzImageProperty property) {
        WzImageProperty outlinkNode = property.getChild("_outlink");
        if (outlinkNode == null) {
            return null;
        }

        String outlink = ((WzStringProperty) outlinkNode).getValue();

        List<String> outlinkPaths =
                Arrays.stream(outlink.split("/"))
                        .map(p -> {
                            if (p.equals("??")) return "碟喻";
                            if (p.equals("奢辨_??00")) return "奢辨_碟喻00";
                            return p;
                        })
                        .collect(Collectors.toList());
        int rootIndex = outlinkPaths.indexOf("_Canvas") + 1;
        if (rootIndex == 0) {
            return new ArrayList<>();
        }

        return outlinkPaths.subList(rootIndex, outlinkPaths.size());
    }

    private static void replace(Map<String, List<Data>> collector, int total) {
        int current = 0;
        for (var entry : collector.entrySet()) {
            String imageStr = entry.getKey();
            List<Data> dataList = entry.getValue();
            WzImage image = getCanvasImage(imageStr, canvasCache);
            if (image == null) {
                log.error("找不到 Canvas {}", imageStr);
                current += dataList.size();
                continue;
            }
            if (!image.parse()) {
                MainFrame.getInstance().setStatusText("文件 %s 解析失败: %s", image.getName(), image.getStatus().getMessage());
                throw new RuntimeException();
            }

            for (Data data : dataList) {
                WzCanvasProperty to = data.object();
                List<String> paths = data.path();

                int step;
                for (step = 0; step < paths.size(); step++) {
                    if (paths.get(step).equals(imageStr)) break;
                }
                step++;

                WzCanvasProperty from = getCanvasProperty(image.getChildren(), paths, step);
                if (from == null) {
                    log.error("对象 {} 找不到 Canvas : {}", to.getPath(), paths);
                    current++;
                    continue;
                }

                to.setFormat(from.getFormat(), from.getFormat2());
                to.setPng(from.getPngImage(false));

                MainFrame.getInstance().updateProgress(++current, total);
            }
        }
    }

    private static WzImage getCanvasImage(String name, List<? extends WzObject> objects) {
        for (WzObject wzObject : objects) {
            if (wzObject instanceof WzDirectory wzDir) {
                WzImage wzImg = getCanvasImage(name, wzDir.getChildren());
                if (wzImg != null) {
                    return wzImg;
                }
            } else if (wzObject instanceof WzImage wzImg) {
                if (wzImg.getName().equals(name)) {
                    return wzImg;
                }
            }
        }
        return null;
    }

    private static WzCanvasProperty getCanvasProperty(List<? extends WzObject> objects, List<String> path, int step) {
        for (WzObject wzObject : objects) {
            if (wzObject.getName().equals(path.get(step))) {
                if (step == path.size() - 1 && wzObject instanceof WzCanvasProperty canvas) {
                    return canvas;
                }
                step++;
                return getCanvasProperty(((WzImageProperty) wzObject).getChildren(), path, step);
            }
        }
        return null;
    }
}
