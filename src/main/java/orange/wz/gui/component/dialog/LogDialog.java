package orange.wz.gui.component.dialog;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.MainFrame;
import orange.wz.log.LogAppender;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public final class LogDialog extends JDialog {

    private static final int MAX_LINES = 5_000;

    private final JTextPane textPane = new JTextPane();
    private final StyledDocument doc = textPane.getStyledDocument();
    private final JTextField filterField = new JTextField(20);

    private final Style infoStyle;
    private final Style warnStyle;
    private final Style errorStyle;
    private final Style debugStyle;
    private final Style highlightStyle;

    private boolean paused = false;
    private String filterText = "";
    private Pattern filterPattern = null;

    private final List<ILoggingEvent> allEvents = new ArrayList<>();
    private final List<ILoggingEvent> pausedBuffer = new ArrayList<>();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final JLabel logCountLabel = new JLabel(MainFrame.i18n.get("test.temp0167"));

    public LogDialog(JFrame owner) {
        super(owner, MainFrame.i18n.get("test.temp0168"), false);

        textPane.setEditable(false);
        textPane.setFont(new Font("Consolas", Font.PLAIN, 13));
        textPane.setBackground(Color.decode("#1e1f22"));

        JScrollPane scroll = new JScrollPane(textPane);

        JButton filterBtn = new JButton(MainFrame.i18n.get("test.temp0169"));
        JButton pauseBtn = new JButton(MainFrame.i18n.get("test.temp0170"));
        JButton clearBtn = new JButton(MainFrame.i18n.get("test.temp0171"));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel(MainFrame.i18n.get("test.temp0172")));
        top.add(filterField);
        top.add(filterBtn);
        top.add(pauseBtn);
        top.add(clearBtn);
        top.add(logCountLabel);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        setSize(820, 500);
        setLocationRelativeTo(owner);

        infoStyle = createStyle(Color.decode("#E6E6E6"));
        warnStyle = createStyle(Color.decode("#FFA500"));
        errorStyle = createStyle(Color.decode("#FF4C4C"));
        debugStyle = createStyle(Color.decode("#8AB4F8"));

        highlightStyle = textPane.addStyle("HIGHLIGHT", null);
        StyleConstants.setBackground(highlightStyle, Color.YELLOW);
        StyleConstants.setForeground(highlightStyle, Color.BLACK);

        // ===== 按钮 =====
        pauseBtn.addActionListener(e -> {
            paused = !paused;
            pauseBtn.setText(paused ? MainFrame.i18n.get("test.temp0173") : MainFrame.i18n.get("test.temp0170"));
            if (!paused) flushPaused();
        });

        clearBtn.addActionListener(e -> {
            clear();
            allEvents.clear();
        });

        // ===== 过滤 =====
        filterBtn.addActionListener(e -> {
            filterText = filterField.getText().trim();
            try {
                filterPattern = filterText.isEmpty() ? null : Pattern.compile(filterText);
            } catch (Exception ex) {
                filterPattern = null; // 如果正则错误则忽略
            }
            rebuildView();
        });

        // ===== 定时刷新 =====
        new Timer(100, e -> {
            pollQueue();
            // 更新日志计数
            updateLogCount();
        }).start();
    }

    private Style createStyle(Color color) {
        Style style = textPane.addStyle(null, null);
        StyleConstants.setForeground(style, color);
        return style;
    }

    // ================== Log Flow ==================

    private void pollQueue() {
        ILoggingEvent event;
        while ((event = LogAppender.QUEUE.poll()) != null) {
            allEvents.add(event);
            enforceMaxLines();

            if (paused) {
                pausedBuffer.add(event);
            } else {
                if (matchFilter(event)) appendEvent(event);
            }
        }
    }

    private void enforceMaxLines() {
        while (allEvents.size() > MAX_LINES) {
            allEvents.removeFirst();
        }
    }

    private boolean matchFilter(ILoggingEvent event) {
        if (filterPattern == null) return true;

        // 先格式化时间
        String time = TIME_FMT.format(Instant.ofEpochMilli(event.getTimeStamp()));

        // 日志级别
        String level = event.getLevel().toString();

        // 消息
        String msg = event.getFormattedMessage();

        // 拼成一条完整字符串进行匹配
        String full = time + " " + level + " " + msg;

        return filterPattern.matcher(full).find();
    }

    private void rebuildView() {
        SwingUtilities.invokeLater(() -> {
            clear();
            for (ILoggingEvent e : allEvents) {
                if (matchFilter(e)) appendEvent(e);
            }
            updateLogCount(); // 刷新总数 / 显示数
        });
    }

    private void appendEvent(ILoggingEvent event) {
        try {
            int startOffset = doc.getLength();

            String time = TIME_FMT.format(Instant.ofEpochMilli(event.getTimeStamp()));
            String level = event.getLevel().toString();
            String msg = event.getFormattedMessage();

            String fullLine = String.format("%s %-5s - %s%n", time, level, msg);

            // 根据日志级别设置底色字体
            Style baseStyle = switch (event.getLevel().toInt()) {
                case Level.ERROR_INT -> errorStyle;
                case Level.WARN_INT -> warnStyle;
                case Level.DEBUG_INT -> debugStyle;
                default -> infoStyle;
            };

            // 先插入整行
            doc.insertString(doc.getLength(), fullLine, baseStyle);

            // === 高亮过滤词 ===
            if (filterPattern != null) {
                var matcher = filterPattern.matcher(fullLine); // 匹配整行
                while (matcher.find()) {
                    int highlightStart = startOffset + matcher.start();
                    int length = matcher.end() - matcher.start();

                    // 高亮叠加（保持原来底色的同时显示黄色背景）
                    SimpleAttributeSet highlightAttr = new SimpleAttributeSet();
                    highlightAttr.addAttributes(baseStyle);
                    StyleConstants.setBackground(highlightAttr, Color.YELLOW);
                    StyleConstants.setForeground(highlightAttr, Color.BLACK);

                    doc.setCharacterAttributes(highlightStart, length, highlightAttr, false);
                }
            }

            textPane.setCaretPosition(doc.getLength());

        } catch (BadLocationException e) {
            log.error(MainFrame.i18n.get("test.temp0175", e.getMessage()));
        }
    }

    private void flushPaused() {
        for (ILoggingEvent e : pausedBuffer) {
            if (matchFilter(e)) appendEvent(e);
        }
        pausedBuffer.clear();
        updateLogCount();
    }

    private void clear() {
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException ignored) {
        }
        updateLogCount();
    }

    private abstract static class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        public abstract void update();

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            update();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            update();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
        }
    }

    private void updateLogCount() {
        SwingUtilities.invokeLater(() -> {
            int total = allEvents.size();
            int visible = 0;
            if (filterPattern != null) {
                for (ILoggingEvent e : allEvents) {
                    if (matchFilter(e)) visible++;
                }
            } else {
                visible = total;
            }
            logCountLabel.setText(MainFrame.i18n.get("test.temp0174", visible, total));
        });
    }
}
