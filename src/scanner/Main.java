package scanner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

/**
 * Counts silhouettes on an image.
 * <p>
 * You can set the image path as the first argument.
 * <p>
 * To scan large images with DFS, you should increase stack size (VM option -Xss256m).
 * <p>
 * To scan images with high level of noise you should change minDeviation parameter.
 * The value can be defined in extension1 or extension2.
 * You can set it as the second argument.
 * <p>
 * To scan small silhouettes you should increase minSizeFactor.
 * You can set it as the third argument.
 * <p>
 * To scan merged silhouettes you should increase cropPower.
 * You can set it as the fourth argument.
 */
public class Main {
	private static final String DEFAULT_FILE_PATH = "assets/mtest5.jpg";

	private static final boolean SCAN_MODE = false; // true - DFS, false - BFS

	private static int minDeviation = 130; // the color sensitivity value (threshold).
	private static int minSizeFactor = 140; // the setting of the size-filter. The bigger the value the smaller silhouettes will be passed though the filter
	private static int cropPower = 0; // the crop power

	public static void main(String[] args) {
		final String filePath;

		if (args.length > 0) {
			filePath = args[0];

			if (args.length > 1) {
				try {
					minDeviation = Integer.parseInt(args[1]);
				} catch (NumberFormatException e) {
					System.out.println("The second argument must be an integer");
				}
			}
			if (args.length > 2) {
				try {
					minSizeFactor = Integer.parseInt(args[2]);
				} catch (NumberFormatException e) {
					System.out.println("The third argument must be an integer");
				}
			}
			if (args.length > 3) {
				try {
					cropPower = Integer.parseInt(args[3]);
				} catch (NumberFormatException e) {
					System.out.println("The fourth argument must be an integer");
				}
			}
		} else {
			filePath = DEFAULT_FILE_PATH;
		}

		try {
			System.out.println("Silhouettes: " + findSilhouettes(ImageIO.read(new File(filePath))));
		} catch (IOException e) {
			System.out.println("Cannot read file \"" + filePath + "\"");
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Wrong image size");
		} catch (StackOverflowError e) {
			System.out.println("Stack overflow. You should increase the stack size");
		}
	}

	/**
	 * Counts silhouettes on an image.
	 *
	 * @param image the image
	 * @return the silhouettes count
	 */
	public static int findSilhouettes(BufferedImage image) {
		int[][][] pixels = getPixels(image);

		boolean[][] map = getSilhouettesMap(pixels, getBackground(pixels), minDeviation); // it is easier to read the code with this variable

		for (int i = 0; i < cropPower; ++i) {
			map = cropMap(map);
		}

		final int minSize = image.getWidth() * image.getHeight() / minSizeFactor; // it is easier to read the code with this variable

		final ImageScanner imageScanner; // it is easier to read the code with this variable

		if (SCAN_MODE) {
			imageScanner = new ImageScannerDFS(map, minSize);
		} else {
			imageScanner = new ImageScannerBFS(map, minSize);
		}

		return imageScanner.scan().size();
	}

	/**
	 * Gets all pixels of an image as an array[x][y][channel].
	 * Channel 0 is red. Channel 1 is green. Channel 2 is blue. Channel 3 is alpha.
	 * It is more optimized than using BufferedImage.getRGB(int, int) method.
	 *
	 * @param image the image
	 * @return the array of pixels with size [width][height][4]
	 */
	public static int[][][] getPixels(BufferedImage image) {
		final int width = image.getWidth(), height = image.getHeight();
		final byte[] pixelsArray = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

		final int[][][] pixelsMap = new int[width][height][4]; // 4 is count of channels (red, green, blue, alpha)

		int i = 0, x = 0, y = 0;

		if (image.getAlphaRaster() == null) { // if the image do not contain the alpha channel
			if (pixelsArray.length == width * height) { // if all channels stored as one integer
				while (i < pixelsArray.length) {
					pixelsMap[x][y][3] = 255; // alpha
					pixelsMap[x][y][2] = (pixelsArray[i] >> 16) & 0xff; // blue
					pixelsMap[x][y][1] = (pixelsArray[i] >> 8) & 0xff; // green
					pixelsMap[x][y][0] = pixelsArray[i++] & 0xff; // red

					++x;

					if (x == width) {
						++y;

						x = 0;
					}
				}
			} else { // if each channel stored in separated integers
				while (i < pixelsArray.length) {
					pixelsMap[x][y][3] = 255; // alpha
					pixelsMap[x][y][2] = pixelsArray[i++] & 0xff; // blue
					pixelsMap[x][y][1] = pixelsArray[i++] & 0xff; // green
					pixelsMap[x][y][0] = pixelsArray[i++] & 0xff; // red

					++x;

					if (x == width) {
						++y;

						x = 0;
					}
				}
			}
		} else { // if the image do contain the alpha channel
			if (pixelsArray.length == width * height) { // if all channels stored as one integer (maybe some images are stored like that)
				while (i < pixelsArray.length) {
					pixelsMap[x][y][3] = (pixelsArray[i] >> 24) & 0xff; // alpha
					pixelsMap[x][y][2] = (pixelsArray[i] >> 16) & 0xff; // blue
					pixelsMap[x][y][1] = (pixelsArray[i] >> 8) & 0xff; // green
					pixelsMap[x][y][0] = pixelsArray[i++] & 0xff; // red

					++x;

					if (x == width) {
						++y;

						x = 0;
					}
				}
			} else { // if each channel stored in separated integers
				while (i < pixelsArray.length) {
					pixelsMap[x][y][3] = pixelsArray[i++] & 0xff; // alpha
					pixelsMap[x][y][2] = pixelsArray[i++] & 0xff; // blue
					pixelsMap[x][y][1] = pixelsArray[i++] & 0xff; // green
					pixelsMap[x][y][0] = pixelsArray[i++] & 0xff; // red

					++x;

					if (x == width) {
						++y;

						x = 0;
					}
				}
			}
		}

		return pixelsMap;
	}

	/**
	 * Defines the background color of an image.
	 * The background color is arithmetic mean of the perimeter pixels of an image.
	 *
	 * @param pixels the image as pixels array[x][y][channel]
	 * @return the background color as array[4] (red, green, blue, alpha)
	 * @throws ArrayIndexOutOfBoundsException if image size is 0
	 */
	public static int[] getBackground(int[][][] pixels) throws ArrayIndexOutOfBoundsException {
		final int width = pixels.length, height = pixels[0].length;

		long red = 0, grn = 0, blu = 0, alp = 0;

		// scan horizontal perimeter lines
		for (int[][] pixel : pixels) {
			red += pixel[0][0];
			grn += pixel[0][1];
			blu += pixel[0][2];
			alp += pixel[0][3];

			final int y = height - 1;

			red += pixel[y][0];
			grn += pixel[y][1];
			blu += pixel[y][2];
			alp += pixel[y][3];
		}

		// scan vertical perimeter lines
		for (int y = 1; y < pixels[0].length - 1; ++y) {
			red += pixels[0][y][0];
			grn += pixels[0][y][1];
			blu += pixels[0][y][2];
			alp += pixels[0][y][3];

			final int x = width - 1;

			red += pixels[x][y][0];
			grn += pixels[x][y][1];
			blu += pixels[x][y][2];
			alp += pixels[x][y][3];
		}

		int count = 2 * (width + height) - 4; // perimeter pixels count (4 is corner pixels)

		red /= count;
		grn /= count;
		blu /= count;
		alp /= count;

		return new int[]{(int) red, (int) grn, (int) blu, (int) alp};
	}

	/**
	 * Generates boolean map[x][y].
	 * Map value is true if the pixel's deviation is bigger or equal to min deviation.
	 *
	 * @param pixels       the image as pixels array[x][y][channel]
	 * @param background   the background color as array[4] (red, green, blue, alpha)
	 * @param minDeviation the min deviation value
	 * @return the map
	 * @throws ArrayIndexOutOfBoundsException if image size is 0
	 */
	public static boolean[][] getSilhouettesMap(int[][][] pixels, int[] background, int minDeviation) throws ArrayIndexOutOfBoundsException {
		final int width = pixels.length, height = pixels[0].length;

		boolean[][] map = new boolean[width][height];

		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				int deviation = 0;

				// sum deviation of each channel
				for (int i = 0; i < 4; ++i) { // 4 is channels count (red, green, blue, alpha)
					deviation += Math.abs(pixels[x][y][i] - background[i]);
				}

				map[x][y] = deviation >= minDeviation;
			}
		}

		return map;
	}

	/**
	 * Deletes the edge pixels of a silhouette, so if several silhouettes are slightly merged, it can split them into separate ones.
	 * In some cases needs to de applied several times to split silhouettes.
	 *
	 * @param silhouettesMap the silhouette to crop
	 * @return the cropped silhouette
	 * @throws ArrayIndexOutOfBoundsException if silhouette size is 0
	 */
	public static boolean[][] cropMap(boolean[][] silhouettesMap) throws ArrayIndexOutOfBoundsException {
		final int width = silhouettesMap.length, height = silhouettesMap[0].length;

		final boolean[][] result = new boolean[width][height];

		// copy the initial map to the result map
		System.arraycopy(silhouettesMap, 0, result, 0, width);

		for (int x = 0; x < width; ++x) { // crop upper and lower pixels
			boolean flag = true; // it is false if an edge pixels has been deleted already

			for (int y = 0; y < height; ++y) { // crop upper pixels
				if (silhouettesMap[x][y]) {
					if (flag) {
						result[x][y] = false;

						flag = false;
					}
				} else {
					flag = true;
				}
			}

			flag = true;

			for (int y = height - 1; y >= 0; --y) { // crop lower pixels
				if (silhouettesMap[x][y]) {
					if (flag) {
						result[x][y] = false;

						flag = false;
					}
				} else {
					flag = true;
				}
			}
		}
		for (int y = 0; y < height; ++y) { // crop left and right pixels
			boolean flag = true; // it is false if an edge pixels has been deleted already

			for (int x = 0; x < width; ++x) { // crop left pixels
				if (silhouettesMap[x][y]) {
					if (flag) {
						result[x][y] = false;

						flag = false;
					}
				} else {
					flag = true;
				}
			}

			flag = true;

			for (int x = width - 1; x >= 0; --x) { // crop right pixels
				if (silhouettesMap[x][y]) {
					if (flag) {
						result[x][y] = false;

						flag = false;
					}
				} else {
					flag = true;
				}
			}
		}

		return result;
	}
}
