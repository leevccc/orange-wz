package orange.wz.lib.audio;

import lombok.Getter;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Getter
public class Mp3FileReader {
    private final Mp3WaveFormat waveFormat;
    private final int lenMs;

    public Mp3FileReader(byte[] fileBytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes)) {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bais);
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(audioInputStream);

            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    fileFormat.getFormat().getSampleRate(),
                    16, // 每个sample 16位
                    fileFormat.getFormat().getChannels(),
                    fileFormat.getFormat().getChannels() * 2, // 16bit * 通道数 = 每帧字节数
                    fileFormat.getFormat().getSampleRate(),
                    false
            );
            AudioInputStream decodedAis = AudioSystem.getAudioInputStream(decodedFormat, audioInputStream);

            // 帧数
            double frameLength = decodedAis.getFrameLength();
            // 帧大小
            int frameSize = decodedAis.getFormat().getFrameSize();
            // 采样率
            float sampleRate = decodedAis.getFormat().getSampleRate();
            // 声道数
            int channels = decodedAis.getFormat().getChannels();
            // 帧速率
            float frameRate = decodedAis.getFormat().getFrameRate();
            // 总样本 = 帧数 * 声道数
            if (frameLength == -1) {
                long totalDecodedBytes = 0;
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = decodedAis.read(buffer)) != -1) {
                    totalDecodedBytes += bytesRead;
                }
                double totalSample = (double) totalDecodedBytes / frameSize;
                frameLength = totalSample / channels; // 回算
            }

            // 时长(毫秒)
            lenMs = (int) (frameLength * 1000 / (frameRate / channels));
            // 比特率 = 文件大小 / 时长(秒) * 8
            int bitrate = fileBytes.length * 1000 / lenMs * 8;
            // averageBytesPerSecond = 比特率 / 8
            float averageBytesPerSecond = (float) bitrate / 8;

            short blockSize = (short) (144 * bitrate / sampleRate);

            waveFormat = Mp3WaveFormat.builder()
                    .waveFormatTag(WaveFormatEncoding.MPEGLAYER3)
                    .channels((short) channels)
                    .sampleRate((int) sampleRate)
                    .averageBytesPerSecond((int) averageBytesPerSecond)
                    .blockAlign((short) 1)
                    .bitsPerSample((short) 0)
                    .extraSize((short) 12)
                    .id(Mp3WaveFormatId.Mpeg)
                    .flags(Mp3WaveFormatFlags.PaddingOff)
                    .blockSize(blockSize)
                    .framesPerBlock((short) 1)
                    .codecDelay((short) 0)
                    .build();

        } catch (IOException | UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        }
    }
}
