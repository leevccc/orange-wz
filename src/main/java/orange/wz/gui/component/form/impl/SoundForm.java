package orange.wz.gui.component.form.impl;

import com.formdev.flatlaf.util.SystemFileChooser;
import lombok.extern.slf4j.Slf4j;
import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.form.data.SoundFormData;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.audio.Mp3FileReader;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
public class SoundForm extends AbstractValueForm {
    private byte[] soundBytes;
    private int totalMs;

    // 播放器核心组件
    private Clip clip;
    private Timer progressTimer;
    private boolean isPaused = false;
    private long clipTimePosition = 0;

    // UI 组件
    private JPanel soundPanel;
    private JButton btnPlay;
    private JSlider slider;
    private JLabel timeLabel;
    private JButton btnLoop; // 新增循环播放按钮
    private boolean loopEnabled = false; // 是否循环

    public SoundForm() {
        super();
        initPlayerUI();

        JButton downloadBtn = new JButton("下载");
        JButton uploadBtn = new JButton("上传");

        downloadBtn.addActionListener(e -> {
            if (soundBytes == null || soundBytes.length == 0) {
                JMessageUtil.error("没有可保存的音频数据");
                return;
            }

            SystemFileChooser chooser = new SystemFileChooser();
            chooser.setDialogTitle("保存音频文件");
            chooser.setSelectedFile(new File(nameInput.getText() + ".mp3"));
            chooser.addChoosableFileFilter(new SystemFileChooser.FileNameExtensionFilter("MP3 文件 (*.mp3)", "mp3"));

            File file = null;
            int res = chooser.showSaveDialog(panel);
            if (res == SystemFileChooser.APPROVE_OPTION) {
                file = chooser.getSelectedFile();
            }

            if (file == null) {
                return;
            }

            if (!file.getName().toLowerCase().endsWith(".mp3")) {
                file = new File(file.getAbsolutePath() + ".mp3");
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(soundBytes);
                JMessageUtil.info("保存成功：\n" + file.getAbsolutePath());
            } catch (IOException ex) {
                log.error(ex.getMessage());
                JMessageUtil.error("保存失败：" + ex.getMessage());
            }
        });

        uploadBtn.addActionListener(e -> {
            File file = FileDialog.chooseOpenFile(new String[]{"mp3"});
            if (file == null) {
                return;
            }

            try {
                setData(Files.readAllBytes(file.toPath()));
            } catch (IOException ex) {
                log.error(ex.getMessage());
                JMessageUtil.error("读取文件失败：" + ex.getMessage());
            }
        });

        addButton(downloadBtn);
        addButton(uploadBtn);
    }

    private void initPlayerUI() {
        // soundPanel 使用水平 BoxLayout
        soundPanel = new JPanel();
        soundPanel.setLayout(new BoxLayout(soundPanel, BoxLayout.X_AXIS));
        soundPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // --- 播放按钮 ---
        btnPlay = new JButton("播放");
        btnPlay.setEnabled(false);
        btnPlay.addActionListener(e -> togglePlay());
        soundPanel.add(btnPlay);
        soundPanel.add(Box.createHorizontalStrut(5)); // 间距

        // --- 循环按钮 ---
        btnLoop = new JButton("循环: 关");
        btnLoop.setEnabled(false);
        btnLoop.addActionListener(e -> {
            loopEnabled = !loopEnabled;
            btnLoop.setText(loopEnabled ? "循环: 开" : "循环: 关");
        });
        soundPanel.add(btnLoop);
        soundPanel.add(Box.createHorizontalStrut(5));

        // --- 进度条 ---
        slider = new JSlider(0, 100, 0);
        slider.setEnabled(false);
        slider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (clip != null && clip.isRunning()) clip.stop();
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (clip != null) {
                    long newMicro = slider.getValue() * 1000000L;
                    if (newMicro > clip.getMicrosecondLength()) newMicro = clip.getMicrosecondLength();
                    clip.setMicrosecondPosition(newMicro);
                    clipTimePosition = newMicro;
                    if (btnPlay.getText().equals("暂停")) clip.start();
                }
            }
        });
        soundPanel.add(slider);
        soundPanel.add(Box.createHorizontalStrut(5));

        // --- 时间标签 ---
        timeLabel = new JLabel("00:00 / 00:00");
        soundPanel.add(timeLabel);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(soundPanel, BorderLayout.NORTH);
        // --- 添加到父 panel ---
        panel.add(topPanel, BorderLayout.CENTER);

        // --- 初始化定时器 ---
        progressTimer = new Timer(200, e -> {
            if (clip != null && clip.isRunning()) {
                long currentMicro = clip.getMicrosecondPosition();
                int currentSec = (int) (currentMicro / 1000000);
                slider.setValue(currentSec);
                updateTimeLabel(currentSec, totalMs / 1000);
            }
        });
    }

    public void setData(String name, String type, byte[] bytes) {
        super.setData(name, type);
        setData(bytes);
    }

    private void setData(byte[] bytes) {
        cleanOldPlayer();

        this.soundBytes = bytes;
        this.totalMs = computeTotalMs(bytes);

        resetUIState();

        slider.setMaximum(totalMs / 1000);
        updateTimeLabel(0, totalMs / 1000);

        if (this.soundBytes != null && this.soundBytes.length > 0) {
            loadAudioClip();
        }
    }

    private int computeTotalMs(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return 0;
        }
        Mp3FileReader reader = new Mp3FileReader(bytes);

        return reader.getLenMs();
    }

    private void loadAudioClip() {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(soundBytes)) {
            AudioInputStream sourceStream = AudioSystem.getAudioInputStream(bais);
            AudioFormat sourceFormat = sourceStream.getFormat();
            // --- 核心转码逻辑：MP3 -> PCM ---
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sourceFormat.getSampleRate(),
                    16,
                    sourceFormat.getChannels(),
                    sourceFormat.getChannels() * 2,
                    sourceFormat.getSampleRate(),
                    false
            );

            AudioInputStream pcmStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
            DataLine.Info info = new DataLine.Info(Clip.class, targetFormat);

            if (!AudioSystem.isLineSupported(info)) {
                // 如果还报错，说明没有 PCM 输出设备，极少见
                return;
            }

            clip = (Clip) AudioSystem.getLine(info);
            clip.open(pcmStream);
            // 监听播放结束
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    if (Math.abs(clip.getMicrosecondPosition() - clip.getMicrosecondLength()) < 100000) {
                        SwingUtilities.invokeLater(() -> {
                            if (loopEnabled) {
                                clip.setMicrosecondPosition(0);
                                clip.start();
                            } else {
                                btnPlay.setText("播放");
                                isPaused = false;
                                clip.setMicrosecondPosition(0);
                                slider.setValue(0);
                                progressTimer.stop();
                            }
                        });
                    }
                }
            });
            // 准备就绪，启用按钮
            btnPlay.setEnabled(true);
            btnLoop.setEnabled(true);
            slider.setEnabled(true);
        } catch (Exception e) {
            log.error(e.getMessage());
            JMessageUtil.error("无法加载音频: " + e.getMessage());
        }
    }

    private void togglePlay() {
        if (clip == null) return;

        if (clip.isRunning()) {
            // 暂停
            clipTimePosition = clip.getMicrosecondPosition();
            clip.stop();
            progressTimer.stop();
            btnPlay.setText("播放");
            isPaused = true;
        } else {
            // 播放
            if (isPaused) {
                clip.setMicrosecondPosition(clipTimePosition);
            }
            clip.start();
            progressTimer.start();
            btnPlay.setText("暂停");
            isPaused = false;
        }
    }

    private void resetUIState() {
        if (progressTimer != null) progressTimer.stop();
        btnPlay.setText("播放");
        btnPlay.setEnabled(false);
        slider.setValue(0);
        slider.setEnabled(false);
        isPaused = false;
        clipTimePosition = 0;
    }

    private void cleanOldPlayer() {
        if (progressTimer != null) progressTimer.stop();
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }

    private void updateTimeLabel(int currentSec, int totalSec) {
        String cur = String.format("%02d:%02d", currentSec / 60, currentSec % 60);
        String tot = String.format("%02d:%02d", totalSec / 60, totalSec % 60);
        timeLabel.setText(cur + " / " + tot);
    }

    @Override
    public SoundFormData getData() {
        return new SoundFormData(
                nameInput.getText(),
                typeInput.getText(),
                soundBytes
        );
    }
}
