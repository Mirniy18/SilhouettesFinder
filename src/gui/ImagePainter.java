package gui;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * Creates a window and draws an image.
 */
public abstract class ImagePainter extends JFrame {
	private static final int WIDTH = 880, HEIGHT = 405; // the default window size

	private static final boolean RESIZABLE = true; // is window can be resized
	private static final boolean ALWAYS_RESIZE_IMAGE = true; // image resize mode (if false image will be scaled only by integer value)

	protected static final Color BACKGROUND = Color.WHITE;

	private JPanel panel;

	protected BufferedImage imageToDraw;

	protected ImagePainter(String name) {
		this(name, 0, 0);
	}

	protected ImagePainter(String name, int leftShift, int rightShift) {
		super(name);

		setResizable(RESIZABLE);

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		panel = new JPanel() {
			@Override
			public void paint(Graphics g) {
				super.paint(g);

				// draw an image
				if (imageToDraw != null) {
					float scale = Math.min((float) (getWidth() - leftShift - rightShift) / imageToDraw.getWidth(), (float) getHeight() / imageToDraw.getHeight());

					int scaledWidth, scaledHeight;

					if (ALWAYS_RESIZE_IMAGE || scale < 1.0f) { // if image is always resized or window size is less than the image size
						scaledWidth = (int) (imageToDraw.getWidth() * scale);
						scaledHeight = (int) (imageToDraw.getHeight() * scale);
					} else {
						scaledWidth = imageToDraw.getWidth() * (int) scale;
						scaledHeight = imageToDraw.getHeight() * (int) scale;
					}

					// draw scaled image at center
					g.drawImage(imageToDraw, (leftShift - rightShift) / 2 + (getWidth() - scaledWidth) / 2, (getHeight() - scaledHeight) / 2, scaledWidth, scaledHeight, null);
				}
			}
		};

		panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));

		panel.setBackground(BACKGROUND);

		add(panel);

		pack();

		setLocationRelativeTo(null);

		setVisible(true);
	}

	/**
	 * Sets color to the specified pixel of the image.
	 *
	 * @param x     the x-coordinate of the pixel
	 * @param y     the x-coordinate of the pixel
	 * @param color the new color of the pixel
	 * @throws NullPointerException if the image is null
	 */
	protected void setPixel(int x, int y, Color color) throws NullPointerException {
		imageToDraw.setRGB(x, y, color.getRGB());
	}

	public JPanel getPanel() {
		return panel;
	}
}
