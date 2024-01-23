/*
 *  Copyright (c) 2021 by Walter Stroebel and InfComTec.
 */
package nl.infcomtec.ffmpeg;

import com.google.gson.Gson;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Walter Stroebel
 */
public class Ffmpeg {

    /**
     * Takes snapshots from a movie file starting at a specified time.
     *
     * @param movie The movie file from which to take the snapshots.
     * @param glob Glob pattern for naming the output image files, for example
     * "snap%04.png".
     * @param h Hour in the movie timeline to start taking snapshots.
     * @param m Minute in the movie timeline to start taking snapshots.
     * @param ms Millisecond in the movie timeline to start taking snapshots.
     * @param numFrames The number of frames to capture.
     * @return BufferedImage array containing the snapshots, or null if an error
     * occurs.
     */
    public static BufferedImage[] takeSnapShots(File movie, String glob, int h, int m, int ms, int numFrames) {
        File tmpDir = new File("/tmp");
        BufferedImage[] snapshots = new BufferedImage[numFrames];

        List<String> args = new ArrayList<>();
        args.add("ffmpeg");
        args.add("-ss");
        args.add(String.format("%02d:%02d:%02d.%03d", h, m, ms / 1000, ms % 1000));
        args.add("-i");
        args.add(movie.getAbsolutePath());
        args.add("-frames:v");
        args.add(String.valueOf(numFrames));
        args.add("-q:v");
        args.add("5");
        args.add("/tmp/" + glob);
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(tmpDir);
        pb.inheritIO();

        try {
            pb.start().waitFor();

            for (int i = 0; i < numFrames; i++) {
                File frameFile = new File(tmpDir, String.format(glob, i + 1));
                if (frameFile.exists()) {
                    snapshots[i] = ImageIO.read(frameFile);
                    boolean weDontCare = frameFile.delete();
                }
            }
            return snapshots;
        } catch (IOException ex) {
            Logger.getLogger(Ffmpeg.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    /**
     * Takes a snapshot from a movie file at a specified time.
     *
     * @param movie The movie file from which to take the snapshot.
     * @param image Name of the image file to be created.
     * @param h Hour in the movie timeline to take the snapshot.
     * @param m Minute in the movie timeline to take the snapshot.
     * @param ms Millisecond in the movie timeline to take the snapshot.
     * @return BufferedImage containing the snapshot, or null if an error
     * occurs.
     */
    public static BufferedImage takeSnapShot(File movie, String image, int h, int m, int ms) {
        File tmpDir = new File("/tmp");
        File tmp = new File(tmpDir, image);
        if (tmp.exists()) {
            tmp.delete();
        }
        List<String> args = new ArrayList<>();
        args.add("ffmpeg");
        args.add("-ss");
        args.add(String.format("%02d:%02d:%02d.%03d", h, m, ms / 1000, ms % 1000));
        args.add("-i");
        args.add(movie.getAbsolutePath());
        args.add("-frames:v");
        args.add("1");
        args.add("-q:v");
        args.add("5");
        args.add(image);
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(tmpDir);
        pb.inheritIO();
        try {
            pb.start().waitFor();
            if (tmp.exists()) {
                BufferedImage ret = ImageIO.read(tmp);
                boolean weDontCare = tmp.delete();
                return ret;
            } else {
                BufferedImage ret = new BufferedImage(160, 80, BufferedImage.TYPE_INT_RGB);
                Graphics2D gr = ret.createGraphics();
                gr.setColor(Color.red.darker());
                gr.fillRect(0, 0, 160, 80);
                gr.setColor(Color.white);
                gr.drawString("No snapshot @", 5, 40);
                gr.drawString(String.format("%02d:%02d:%02d.%03d", h, m, ms / 1000, ms % 1000), 5, 60);
                gr.dispose();
                return ret;
            }
        } catch (IOException ex) {
            Logger.getLogger(Ffmpeg.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    /**
     * Creates a Callable task that takes a snapshot from a movie file at a
     * specified time.
     *
     * @param movie The movie file from which to take the snapshot.
     * @param tu The TimeUnit for the time parameter.
     * @param units Time in the specified TimeUnit to take the snapshot.
     * @return Callable task that returns a BufferedImage of the snapshot.
     */
    public static Callable<BufferedImage> takeSnapShot(File movie, TimeUnit tu, long units) {
        final int ms = (int) (TimeUnit.MILLISECONDS.convert(units, tu) % 60000);
        final int m = (int) TimeUnit.MINUTES.convert(units, tu);
        final int h = (int) TimeUnit.HOURS.convert(units, tu);
        return new Callable<BufferedImage>() {
            @Override
            public BufferedImage call() throws Exception {
                return takeSnapShot(movie, Utils.randomToken() + ".png", h, m, ms);
            }
        };
    }

    /**
     * Creates a Callable task that takes snapshots from a movie file at a
     * specified time.
     *
     * @param movie The movie file from which to take the snapshot.
     * @param numFrames number of frames/images.
     * @param tu The TimeUnit for the time parameter.
     * @param units Time in the specified TimeUnit to take the snapshot.
     * @return BufferedImage array containing the snapshots, or null if an error
     * occurs.
     */
    public static Callable<BufferedImage[]> takeSnapShots(File movie, int numFrames, TimeUnit tu, long units) {
        int ms = (int) (TimeUnit.MILLISECONDS.convert(units, tu) % 60000);
        int m = (int) TimeUnit.MINUTES.convert(units, tu);
        int h = (int) TimeUnit.HOURS.convert(units, tu);
        return new Callable<BufferedImage[]>() {
            @Override
            public BufferedImage[] call() throws Exception {
                return takeSnapShots(movie, Utils.randomToken() + "_%04d.png", h, m, ms, numFrames);
            }
        };
    }

    /**
     * Converts a series of images in a directory to an MP4 video file.
     *
     * @param dir Directory containing the image files.
     * @param fRate Frame rate of the output video.
     * @param glob Glob pattern to match the image files in the directory.
     * @param outF Output file for the MP4 video.
     * @throws Exception If an error occurs during the conversion process.
     */
    public static void imgToMP4(File dir, int fRate, String glob, File outF) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("ffmpeg");
        args.add("-framerate");
        args.add("" + fRate);
        args.add("-pattern_type");
        args.add("glob");
        args.add("-i");
        args.add(glob);
        args.add("-c:v");
        args.add("libx264");
        args.add("-pix_fmt");
        args.add("yuv420p");
        args.add(outF.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(dir);
        System.out.println(InOutErrConsumer.exec(pb.start()));
    }

    /**
     * Runs ffprobe on a movie file to extract multimedia information in JSON
     * format.
     *
     * @param movie The movie file to be probed.
     * @return String containing the multimedia information in JSON format.
     * @throws Exception If an error occurs during the execution of ffprobe.
     */
    public static String ffprobe(File movie) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("ffprobe");
        args.add("-v");
        args.add("error");
        args.add("-hide_banner");
        args.add("-of");
        args.add("default=noprint_wrappers=0");
        args.add("-print_format");
        args.add("json");
        args.add("-select_streams");
        args.add("v:0");
        args.add("-show_entries");
        args.add("stream=r_frame_rate"); // Added this line
        args.add("-show_format");
        args.add(movie.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(args);
        List<String> out = InOutErrConsumer.exec(pb.start());
        return Utils.listToString(out);
    }

    public static P probe(File movie) throws Exception {
        return new Gson().fromJson(ffprobe(movie), P.class);
    }
}
