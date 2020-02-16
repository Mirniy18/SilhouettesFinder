package scanner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ImageScannerBFS extends ImageScanner {
	private Queue<Integer> queueX, queueY;

	public ImageScannerBFS(boolean[][] map, int minSize) {
		super(map, minSize);
	}

	/**
	 * Scans the image for silhouettes.
	 *
	 * @return the list of the left-upper cells of silhouettes. Cell coordinates as array[]{x, y}
	 */
	@Override
	public List<Integer[]> scan() {
		final int width = map.length, height = map[0].length; // the dimensions of the image

		checked = new boolean[map.length][map[0].length]; // reset the checked array

		queueX = new ArrayDeque<>();
		queueY = new ArrayDeque<>();

		size = 0;

		final List<Integer[]> result = new ArrayList<>(); // list of the left-upper cells of silhouettes

		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				if (map[x][y] && !checked[x][y]) {
					scan(x, y);

					if (size >= minSize) { // filter noise and small objects
						result.add(new Integer[]{x, y});
					}

					size = 0;
				}
			}
		}

		return result;
	}

	/**
	 * Scan a cell and all nearest silhouette's cells using breadth-first search.
	 *
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @throws ArrayIndexOutOfBoundsException if image size is 0
	 */
	public void scan(Integer x, Integer y) throws ArrayIndexOutOfBoundsException {
		addToQueue(x, y);

		while ((x = queueX.poll()) != null) {
			y = queueY.poll();

			// scan left cell
			if (x > 0) {
				addToQueue(x - 1, y);
			}
			// scan right cell
			if (x < map.length - 1) {
				addToQueue(x + 1, y);
			}
			// scan upper cell
			if (y > 0) {
				addToQueue(x, y - 1);
			}
			// scan lower cell
			if (y < map[0].length - 1) {
				addToQueue(x, y + 1);
			}
		}
	}

	/**
	 * Adds a cell to the queue.
	 * It is easier to read the code with this method
	 *
	 * @param x the x-coordinate of a cell
	 * @param y the y-coordinate of a cell
	 */
	private void addToQueue(int x, int y) {
		if (map[x][y] && !checked[x][y]) {
			queueX.offer(x);
			queueY.offer(y);

			checked[x][y] = true;

			++size;
		}
	}
}
