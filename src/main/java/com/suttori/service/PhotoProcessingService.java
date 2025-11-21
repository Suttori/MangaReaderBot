package com.suttori.service;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class PhotoProcessingService {


    public List<File> photoProcessing(File folder, File file, Long userId) {
        try {
            FFmpeg ffmpeg = new FFmpeg();
            FFprobe ffprobe = new FFprobe();
            FFmpegStream photoStream = ffprobe.probe(file.getPath()).getStreams().get(0);
            if (photoStream == null || photoStream.width <= 0 || photoStream.height <= 0) {
                return null;
            }
            List<File> slicedFiles = new ArrayList<>();
            int maxSliceHeight = photoStream.width * 2;
            int numberOfSlices = (int) Math.ceil((double) photoStream.height / maxSliceHeight);
            for (int i = 0; i < numberOfSlices; i++) {
                int sliceHeight = (i == numberOfSlices - 1)
                        ? photoStream.height - (i * maxSliceHeight)
                        : maxSliceHeight;
                File outputFile = new File(folder, userId + "_" + UUID.randomUUID() + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH_mm_ss_dd_yyyy")) + "slice_" + "_part_" + (i + 1) + ".png");
                FFmpegBuilder builder = new FFmpegBuilder()
                        .setInput(file.getPath())
                        .overrideOutputFiles(true)
                        .addOutput(outputFile.getPath())
                        .setVideoCodec("png")
                        .setVideoFilter("crop=" + photoStream.width + ":" + sliceHeight + ":0:" + (i * maxSliceHeight))
                        .done();

//                FFmpegBuilder builder = new FFmpegBuilder()
//                        .setInput(file.getPath())
//                        .overrideOutputFiles(true)
//                        .addOutput(outputFile.getPath())
//                        .setVideoCodec("png")
//                        .addExtraArgs("-qscale:v", "1")  // Устанавливаем качество
//                        .addExtraArgs("-compression_level", "0")  // Минимальное сжатие для PNG
//                        .setVideoFilter("crop=" + photoStream.width + ":" + sliceHeight + ":0:" + (i * maxSliceHeight))
//                        .done();

                FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
                executor.createJob(builder).run();
                slicedFiles.add(outputFile);
            }
            return slicedFiles;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
