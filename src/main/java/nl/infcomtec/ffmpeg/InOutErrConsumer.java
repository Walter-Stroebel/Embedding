/*
 *  Copyright (c) 2021 by Walter Stroebel and InfComTec.
 */
package nl.infcomtec.ffmpeg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Whenever you need to consume an InputStream, OutputStream and/or
 * "ErrorStream".
 *
 * @author Walter Stroebel
 */
public class InOutErrConsumer implements AutoCloseable {

    private ByteArrayInputStream bais;

    private final InputStream err;
    private final ByteArrayOutputStream fromErr = new ByteArrayOutputStream();
    private final ByteArrayOutputStream fromIn = new ByteArrayOutputStream();
    private final InputStream in;
    private final OutputStream out;
    private List<String> outLines;

    public InOutErrConsumer(InputStream in, InputStream err, OutputStream out) {
        this.in = in;
        this.out = out;
        this.err = err;
    }

    public InOutErrConsumer(Process p, List<String> thingsToSay) {
        this(p.getInputStream(), p.getErrorStream(), p.getOutputStream());
        setOutLines(thingsToSay);
    }

    public InOutErrConsumer(Process p) {
        this(p.getInputStream(), p.getErrorStream(), p.getOutputStream());
    }

    /** Common usage: Execute something and get the result as a list of strings.
     * 
     * @param p Process to run.
     * @return List of Strings.
     * @throws Exception If something went wrong.
     */
    public static List<String> exec(Process p) throws Exception {
        try (InOutErrConsumer ioec = new InOutErrConsumer(p)) {
            ioec.start();
            p.waitFor();
            return ioec.getInputLines();
        }
    }

    @Override
    public void close() {
        try {
            if (null != bais) {
                bais.close();
            }
            if (null != err) {
                err.close();
            }
            if (null != fromErr) {
                fromErr.close();
            }
            if (null != fromIn) {
                fromIn.close();
            }
            if (null != in) {
                in.close();
            }
            if (null != out) {
                out.close();
            }
        } catch (Exception any) {
            // we tried
        }
    }

    public List<String> getInputLines() {
        return getInputLines(StandardCharsets.UTF_8);
    }

    public List<String> getInputLines(Charset cs) {
        ArrayList<String> ret = new ArrayList<>();
        return Utils.BaosToList(fromIn, cs);
    }

    public int outByte() {
        if (null == bais) {
            return -1;
        }
        int ch = bais.read();
        if (ch < 0) {
            if (null != outLines && !outLines.isEmpty()) {
                String s = outLines.remove(0);
                bais = new ByteArrayInputStream(s.getBytes());
            } else {
                bais = null;
                return -1;
            }
        }
        return ch;
    }

    /**
     * @param outLines the outLines to set
     */
    public void setOutLines(List<String> outLines) {
        this.outLines = outLines;
        if (!outLines.isEmpty()) {
            String s = outLines.remove(0);
            bais = new ByteArrayInputStream(s.getBytes());
        }
    }

    public void start() {
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    int ch;
                    try {
                        ch = err.read();
                    } catch (IOException ex) {
                        Logger.getLogger(InOutErrConsumer.class.getName()).log(Level.SEVERE, null, ex);
                        break;
                    }
                    if (ch < 0) {
                        break;
                    }
                    fromErr.write(ch);
                }
            }
        }.start();
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    int ch;
                    try {
                        ch = in.read();
                    } catch (IOException ex) {
                        Logger.getLogger(InOutErrConsumer.class.getName()).log(Level.SEVERE, null, ex);
                        break;
                    }
                    if (ch < 0) {
                        break;
                    }
                    fromIn.write(ch);
                }
            }
        }.start();
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        int ch;
                        out.write(ch = outByte());
                        if (ch < 0) {
                            break;
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(InOutErrConsumer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }.start();
    }
}
