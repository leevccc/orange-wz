package orange.wz.gui.component.dialog;

import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.MainFrame;
import orange.wz.gui.component.key.KeyBox;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.tools.BinaryReader;
import orange.wz.provider.tools.BinaryWriter;
import orange.wz.provider.tools.FileTool;
import orange.wz.provider.tools.wzkey.WzKey;
import orange.wz.provider.tools.wzkey.WzKeyStorage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class ListEditor extends JDialog {

    public ListEditor(String file, WzKey wzKey) {
        setTitle("List.wz");
        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        // 文本编辑器，多行
        JTextArea textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(readFile(file, wzKey));

        // 滚动面板包装文本编辑器
        JScrollPane scrollPane = new JScrollPane(textArea);

        // 密钥选择
        WzKeyStorage wzKeyStorage = new WzKeyStorage();
        WzKey[] keys = wzKeyStorage.loadAll().toArray(new WzKey[0]);
        KeyBox keyBox = new KeyBox(keys);
        int index;
        for (index = 0; index < keys.length; index++) {
            if (keys[index].getName().equals(wzKey.getName())) {
                break;
            }
        }
        keyBox.setSelectedIndex(index);

        // 保存按钮
        JButton saveButton = new JButton(MainFrame.i18n.get("save"));
        saveButton.addActionListener((ActionEvent e) -> {
            String content = textArea.getText();
            if (saveFile(file, (WzKey) keyBox.getSelectedItem(), content)) {
                JMessageUtil.info("文件已保存到 " + file);
            } else {
                JMessageUtil.error("保存失败");
            }
        });

        // 底部按钮面板
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(keyBox);
        bottomPanel.add(saveButton);

        // 主面板布局
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // 显示窗口
        setVisible(true);
    }

    private String readFile(String file, WzKey wzKey) {
        List<String> text = new ArrayList<>();
        BinaryReader reader = new BinaryReader(FileTool.readFile(Path.of(file)), wzKey.getIv(), wzKey.getUserKey());
        while (reader.hasRemaining()) {
            text.add(reader.readListString());
        }

        int lastIndex = text.size() - 1;
        String last = text.get(lastIndex);

        if (last.endsWith("/")) {
            last = last.substring(0, last.length() - 1) + "g";
            text.set(lastIndex, last);
        }

        return String.join("\n", text);
    }

    private boolean saveFile(String file, WzKey wzKey, String context) {
        String[] text = context.split("\n");

        int lastIndex = text.length - 1;
        String last = text[lastIndex];

        if (last.endsWith("g")) {
            last = last.substring(0, last.length() - 1) + "/";
            text[lastIndex] = last;
        }

        BinaryReader reader = new BinaryReader(wzKey.getIv(), wzKey.getUserKey());
        BinaryWriter writer = new BinaryWriter();
        writer.setWzMutableKey(reader.getWzMutableKey());

        for (String s : text) {
            writer.writeListString(s);
        }

        return FileTool.saveFile(Path.of(file), writer.output());
    }
}
