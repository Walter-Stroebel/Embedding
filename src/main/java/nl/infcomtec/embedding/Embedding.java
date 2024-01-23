package nl.infcomtec.embedding;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import nl.infcomtec.jllama.AvailableModels;
import nl.infcomtec.jllama.Embeddings;
import nl.infcomtec.jllama.Ollama;
import nl.infcomtec.jllama.OllamaEmbeddings;

/**
 *
 * @author walter
 */
public class Embedding {

    private final JFrame frame;
    private final JToolBar buttons;
    private final JComboBox<String> models;
    private final JComboBox<String> hosts;
    private OllamaEmbeddings client;
    private final JPanel center;
    private final LinkedList<BufferedImage> embs = new LinkedList<>();
    private final LinkedList<String> jTxt = new LinkedList<>();
    private final LinkedList<Rectangle> clks = new LinkedList<>();
    private final JLabel lLabel;
    private final JScrollPane lPane;
    private final JLabel rLabel;
    private final JScrollPane rPane;

    public Embedding() {
        frame = new JFrame("Ollama embeddings");
        buttons = new JToolBar();
        models = new JComboBox<>();
        hosts = new JComboBox<>();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container cont = frame.getContentPane();
        cont.setLayout(new BorderLayout());
        buttonBar();
        cont.add(buttons, BorderLayout.NORTH);
        center = new JPanel() {
            @Override
            public void paint(Graphics g) {
                clks.clear();
                int dw = getWidth();
                int dh = getHeight();
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, dw - 1, dh - 1);
                int x = 0;
                int y = 0;
                for (BufferedImage bi : embs) {
                    int iw = bi.getWidth();
                    int ih = bi.getHeight();
                    g.drawImage(bi, x, y, null);
                    clks.add(new Rectangle(x, y, iw, ih));
                    x += iw;
                    if (x > dw - iw) {
                        x = 0;
                        y += ih;
                        if (y > dh - ih) {
                            break;
                        }
                    }
                }
            }
            private final Dimension DIM = new Dimension(3480, 1080);

            @Override
            public Dimension getMinimumSize() {
                return DIM;
            }

            @Override
            public Dimension getMaximumSize() {
                return DIM;
            }

            @Override
            public Dimension getPreferredSize() {
                return DIM;
            }
        };
        center.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();
                for (int i = 0; i < clks.size(); i++) {
                    if (clks.get(i).contains(p)) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            lLabel.setIcon(new ImageIcon(embs.get(i).getScaledInstance(320, 320, BufferedImage.SCALE_DEFAULT)));
                            lPane.setViewportView(new JTextArea(jTxt.get(i)));
                        } else {
                            rLabel.setIcon(new ImageIcon(embs.get(i).getScaledInstance(320, 320, BufferedImage.SCALE_DEFAULT)));
                            rPane.setViewportView(new JTextArea(jTxt.get(i)));
                        }
                        frame.repaint();
                        break;
                    }
                }
            }
        });
        cont.add(center, BorderLayout.SOUTH);
        Box hor = Box.createHorizontalBox();
        BufferedImage b = new BufferedImage(320, 320, BufferedImage.TYPE_BYTE_BINARY);
        ImageIcon bi = new ImageIcon(b);
        {
            Box ver = Box.createVerticalBox();
            ver.add(new JLabel("Left-click"));
            ver.add(lLabel = new JLabel(bi));
            ver.add(lPane = new JScrollPane());
            hor.add(ver);
        }
        {
            Box ver = Box.createVerticalBox();
            ver.add(new JLabel("Right-click"));
            ver.add(rLabel = new JLabel(bi));
            ver.add(rPane = new JScrollPane());
            hor.add(ver);
        }
        cont.add(hor, BorderLayout.CENTER);
        frame.pack();
        if (EventQueue.isDispatchThread()) {
            finishInit();
        } else {
            try {
                EventQueue.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        finishInit();
                    }
                });
            } catch (Exception ex) {
                Logger.getLogger(Embedding.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(1);
            }
        }
    }

    private void finishInit() {
        frame.setVisible(true);
        frame.setBounds(Ollama.config.x, Ollama.config.y, Ollama.config.w, Ollama.config.h);
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                Ollama.config.update(frame.getBounds());
            }

            @Override
            public void componentResized(ComponentEvent e) {
                Ollama.config.update(frame.getBounds());
            }
        });
    }

    private void buttonBar() {
        buttons.add(new AbstractAction("Exit") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                System.exit(0);
            }
        });
        buttons.add(new JToolBar.Separator());
        String lsHost = Ollama.config.getLastEndpoint();
        for (String e : Ollama.getAvailableModels().keySet()) {
            addToHosts(e);
        }
        models.removeAllItems();
        for (AvailableModels.AvailableModel am : Ollama.getAvailableModels().get(lsHost).models) {
            models.addItem(am.name);
        }
        if (null != Ollama.config.lastModel) {
            models.setSelectedItem(Ollama.config.lastModel);
        }
        models.invalidate();
        hosts.setSelectedItem(lsHost);
        hosts.addActionListener(new AddSelectHost());
        hosts.setEditable(true);
        buttons.add(new JLabel("Hosts:"));
        buttons.add(hosts);
        buttons.add(new JToolBar.Separator());
        buttons.add(new JLabel("Models:"));
        buttons.add(models);
        buttons.add(new JButton(new AbstractAction("Scan Java") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int ans = jfc.showOpenDialog(frame);
                SwingWorker<Void, BufferedImage> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        if (ans == JFileChooser.APPROVE_OPTION) {
                            try {
                                OllamaEmbeddings em = new OllamaEmbeddings(
                                        hosts.getSelectedItem().toString(),
                                        models.getSelectedItem().toString());
                                embs.clear();
                                jTxt.clear();
                                final BufferedImage blank = new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_BINARY);

                                Files.walkFileTree(jfc.getSelectedFile().toPath(), new SimpleFileVisitor<>() {
                                    @Override
                                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                        if (file.toString().endsWith(".java")) {
                                            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                                            if (content.length() > 1024) {
                                                for (int ofs = 0; ofs < content.length() - 512; ofs += 256) {
                                                    String frag = content.substring(ofs, ofs + 512);
                                                    System.out.println(frag);
                                                    strToImg(em, frag);
                                                }
                                            } else {
                                                System.out.println(content);
                                                strToImg(em, content);
                                            }
                                            // separator
                                            publish(blank);
                                        }
                                        return super.visitFile(file, attrs);
                                    }

                                    private void strToImg(OllamaEmbeddings em, String frag) {
                                        try {
                                            Embeddings embeddings = em.getEmbeddings(frag);
                                            BufferedImage img = toImage(embeddings);
                                            embs.add(img);
                                            jTxt.add(frag);
                                            publish(img);
                                        } catch (Exception ex) {
                                            Logger.getLogger(Embedding.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                });
                            } catch (IOException ex) {
                                Logger.getLogger(Embedding.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void process(List<BufferedImage> chunks) {
                        frame.repaint();
                    }

                    @Override
                    protected void done() {
                        // Final updates or cleanup after background work is done.
                    }
                };

                worker.execute();

            }
        }));
    }

    public static BufferedImage toImage(Embeddings em) {
        int w = (int) Math.round(Math.sqrt(em.response.embedding.length));
        int h = em.response.embedding.length / w;
        if (w * h < em.response.embedding.length) {
            h++;
        }
        BufferedImage ret = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double d : em.response.embedding) {
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        min = Math.log(-min);
        max = Math.log(max);
//        System.out.format("WxH=%dx%d, MiniMax=%.2f - %.2f\n", w, h, min, max);
        double rf = 255.0 / min;
        double bf = 255 / max;
        int x = 0;
        int y = 0;
        for (double d : em.response.embedding) {
            double r, b;
            if (d < 0) {
                r = Math.log(-d) * rf;
                b = 0;
            } else {
                r = 0;
                b = Math.log(d) * bf;
            }
            r = Math.max(0, Math.min(255, r));
            b = Math.max(0, Math.min(255, b));
            Color c = new Color((int) r, 64 + (int) ((r + b) / 4), (int) b, 255);
            ret.setRGB(x, y, c.getRGB());
            x++;
            if (x >= w) {
                x = 0;
                y++;
            }
        }
        return ret;
    }

    private class AddSelectHost implements ActionListener {

        public AddSelectHost() {
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            String selHost = (String) hosts.getEditor().getItem();
            if (!selHost.isEmpty()) {
                addToHosts(selHost);
                int n = hosts.getItemCount();
                Ollama.config.ollamas = new String[n];
                for (int i = 0; i < n; i++) {
                    Ollama.config.ollamas[i] = hosts.getItemAt(i);
                }
                hosts.setSelectedItem(selHost);
                Ollama.config.update();
                String fmod = null;
                for (Map.Entry<String, AvailableModels> e : Ollama.fetchAvailableModels().entrySet()) {
                    addToHosts(e.getKey());
                    if (e.getKey().equals(selHost)) {
                        models.removeAllItems();
                        for (AvailableModels.AvailableModel am : e.getValue().models) {
                            models.addItem(am.name);
                            if (null == fmod) {
                                fmod = am.name;
                            }
                        }
                        models.setSelectedItem(fmod);
                    }
                }
                models.invalidate();
            }
        }
    }

    private void addToHosts(String host) {
        for (int i = 0; i < hosts.getItemCount(); i++) {
            if (hosts.getItemAt(i).equalsIgnoreCase(host)) {
                return;
            }
        }
        hosts.addItem(host);
        if (1 == hosts.getItemCount()) {
            hosts.setSelectedItem(host);
        }
    }

    public static void main(String[] args) {
        Ollama.init();
        Ollama.setupGUI();
        new Embedding();
    }
}
