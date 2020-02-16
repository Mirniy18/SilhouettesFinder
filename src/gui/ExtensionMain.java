package gui;

import scanner.Main;
import scanner.ImageScannerDFS;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Displays an image as silhouettes map or as silhouettes.
 * Helps define min deviation and min size factor.
 * <p>
 * Controls:
 * Change the value of min deviation - mouse wheel or up/down keys.
 * <p>
 * To scan large images with DFS you should increase stack size (VM option -Xss256m).
 */
public class ExtensionMain extends ImagePainter {
	private static final int DEFAULT_MIN_SIZE_FACTOR = 380; // the default value for factor of min size

	private static final int RIGHT_SHIFT = 160; // the width of the menu

	private static final Color[] colors = new Color[]{Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN, new Color(255, 100, 0), new Color(0, 150, 0)};

	private static final int MAX_MIN_SIZE_FACTOR = 5000; // the max value of the min size factor for slider
	private static final int MAX_CROP_POWER = 50; // the max value of the crop power for slider

	private int minDeviation = 130; // the min deviation value

	private int cropPower = 5;

	private BufferedImage originalImage, mapImage, silhouettesImage;

	private boolean[][] map;

	private int[][][] pixels;
	private int[] background;

	private int silhouettesCount;

	private ImageScannerDFS scanner, filler;

	private JPanel menuPanel;

	private JCheckBox checkBoxDrawOriginalImage, checkBoxDrawMap, checkBoxDrawSilhouettes;

	private JLabel labelMinDeviation, labelMinSizeFactor, labelCropPower, labelSilhouettesCount;

	private JSlider sliderMinDeviation, sliderMinSizeFactor, sliderCropPower;

	private int previousWidth;

	public static void main(String[] args) {
		args = new String[]{"assets/mtest5.jpg"}; // uncomment to debug

		try {
			new ExtensionMain(args[0]);
		} catch (IOException | ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();

			System.exit(1);
		}
	}

	private ExtensionMain(String path) throws IOException, ArrayIndexOutOfBoundsException {
		super("Silhouettes Finder", 0, RIGHT_SHIFT);

		pixels = Main.getPixels(originalImage = ImageIO.read(new File(path)));

		mapImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

		background = Main.getBackground(pixels);

		imageToDraw = new BufferedImage(pixels.length, pixels[0].length, BufferedImage.TYPE_INT_ARGB);

		addMouseWheelListener(e -> setMinDeviation(minDeviation - (int) e.getPreciseWheelRotation()));

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_UP:
						setMinDeviation(minDeviation + 1);
						break;
					case KeyEvent.VK_DOWN:
						setMinDeviation(minDeviation - 1);
						break;
				}
			}
		});

		checkBoxDrawOriginalImage = new JCheckBox("Draw original image");
		checkBoxDrawMap = new JCheckBox("Draw map");
		checkBoxDrawSilhouettes = new JCheckBox("Draw silhouettes");

		labelMinDeviation = new JLabel("Min deviation (" + minDeviation + "):");
		labelMinSizeFactor = new JLabel("Min size factor (" + DEFAULT_MIN_SIZE_FACTOR + "):");
		labelCropPower = new JLabel("Crop power (" + cropPower + "):");
		labelSilhouettesCount = new JLabel();

		sliderMinDeviation = new JSlider(1, 1020, minDeviation); // 1020 is the max value of deviation (1020 = 255 * 4) (255 is max channel value, 4 is channels count)
		sliderMinSizeFactor = new JSlider(1, MAX_MIN_SIZE_FACTOR, DEFAULT_MIN_SIZE_FACTOR);
		sliderCropPower = new JSlider(0, MAX_CROP_POWER, cropPower);

		checkBoxDrawOriginalImage.setSelected(true);
		checkBoxDrawSilhouettes.setSelected(true);

		final JPanel panel = getPanel();
		menuPanel = new JPanel();

		panel.setLayout(null);

		menuPanel.setSize(RIGHT_SHIFT, panel.getHeight());

		menuPanel.setLocation(panel.getWidth() - RIGHT_SHIFT, 0);

		final BoxLayout layout = new BoxLayout(menuPanel, BoxLayout.PAGE_AXIS);

		menuPanel.setLayout(layout);

		ItemListener itemListener = e -> updateImage();

		checkBoxDrawOriginalImage.addItemListener(itemListener);
		checkBoxDrawMap.addItemListener(itemListener);
		checkBoxDrawSilhouettes.addItemListener(itemListener);

		sliderMinDeviation.addChangeListener(e -> setMinDeviation(sliderMinDeviation.getValue()));
		sliderMinSizeFactor.addChangeListener(e -> {
			final int minSizeFactor = sliderMinSizeFactor.getValue();

			scanner.minSize = filler.minSize = originalImage.getWidth() * originalImage.getHeight() / minSizeFactor;

			labelMinSizeFactor.setText("Min size factor (" + minSizeFactor + "):");

			updateMap();
		});
		sliderCropPower.addChangeListener(e -> {
			cropPower = sliderCropPower.getValue();

			labelCropPower.setText("Crop power (" + cropPower + "):");

			map = Main.getSilhouettesMap(pixels, background, minDeviation);

			for (int i = 0; i < cropPower; ++i) {
				map = Main.cropMap(map);
			}

			scanner.map = map;
			filler.map = map;

			updateMap();
		});

		menuPanel.add(checkBoxDrawOriginalImage);
		menuPanel.add(checkBoxDrawMap);
		menuPanel.add(checkBoxDrawSilhouettes);
		menuPanel.add(labelMinDeviation);
		menuPanel.add(sliderMinDeviation);
		menuPanel.add(labelMinSizeFactor);
		menuPanel.add(sliderMinSizeFactor);
		menuPanel.add(labelCropPower);
		menuPanel.add(sliderCropPower);
		menuPanel.add(labelSilhouettesCount);

		panel.add(menuPanel);

		map = Main.getSilhouettesMap(pixels, background, minDeviation);

		for (int i = 0; i < cropPower; ++i) {
			map = Main.cropMap(map);
		}

		final int minSize = map.length * map[0].length / DEFAULT_MIN_SIZE_FACTOR;

		scanner = new ImageScannerDFS(map, minSize);

		filler = new ImageScannerDFS(map, minSize) { // override scanner to make it fill
			@Override
			public void scan(int x, int y) throws ArrayIndexOutOfBoundsException {
				if (map[x][y] && !checked[x][y]) {
					silhouettesImage.setRGB(x, y, colors[silhouettesCount % colors.length].getRGB());
				}

				super.scan(x, y);
			}
		};

		updateMap();

		pack();

		previousWidth = getWidth();
	}

	private void updateMap() {
		// draw map
		for (int x = 0; x < imageToDraw.getWidth(); ++x) {
			for (int y = 0; y < imageToDraw.getHeight(); ++y) {
				mapImage.setRGB(x, y, map[x][y] ? -16777216 : 0); // -16777216 is black; 0 is transparent
			}
		}

		// draw silhouettes
		final List<Integer[]> silhouettes = scanner.scan();

		silhouettesCount = 0;

		silhouettesImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

		filler.checked = new boolean[map.length][map[0].length]; // reset array of checked cells of the filler

		// fill each silhouette
		for (Integer[] silhouette : silhouettes) {
			filler.scan(silhouette[0], silhouette[1]);

			++silhouettesCount;
		}

		labelSilhouettesCount.setText("Silhouettes count: " + silhouettesCount);

		updateImage();
	}

	private void updateImage() {
		// draw the original image
		if (checkBoxDrawOriginalImage.isSelected()) {
			ColorModel colorModel = originalImage.getColorModel();

			imageToDraw = new BufferedImage(colorModel, originalImage.copyData(null), colorModel.isAlphaPremultiplied(), null);
		} else { // fill with the background color
			Graphics g = imageToDraw.getGraphics();

			g.setColor(BACKGROUND);

			g.fillRect(0, 0, imageToDraw.getWidth(), imageToDraw.getHeight());
		}

		// draw map
		if (checkBoxDrawMap.isSelected()) {
			Graphics g = imageToDraw.getGraphics();

			g.drawImage(mapImage, 0, 0, null);
		}

		if (checkBoxDrawSilhouettes.isSelected()) {
			Graphics g = imageToDraw.getGraphics();

			g.drawImage(silhouettesImage, 0, 0, null);
		}

		repaint();
	}

	private void setMinDeviation(int value) {
		if (value < 1) {
			value = 1;
		} else if (value > 1020) { // 1020 is the max value of deviation (1020 = 255 * 4) (255 is max channel value, 4 is channels count)
			value = 1020;
		}

		if (minDeviation != value) { // if min deviation is changed
			minDeviation = value;

			labelMinDeviation.setText("Min deviation (" + minDeviation + "):");

			sliderMinDeviation.setValue(minDeviation);

			map = Main.getSilhouettesMap(pixels, background, minDeviation);

			for (int i = 0; i < cropPower; ++i) {
				map = Main.cropMap(map);
			}

			scanner.map = map;
			filler.map = map;

			updateMap();
		}
	}

	@Override
	public void validate() {
		super.validate();

		if (getWidth() != previousWidth && menuPanel != null) {
			menuPanel.setLocation(getPanel().getWidth() - RIGHT_SHIFT, 0);
		}
	}
}
