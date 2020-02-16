package scanner;

import java.util.ArrayList;
import java.util.List;

/**
 * Scan an image using depth-first search.
 */
public class ImageScannerDFS extends ImageScanner {
	public ImageScannerDFS(boolean[][] map, int minSize) {
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

		size = 0; // reset the size of the current silhouette

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
	 * Scan a cell and all nearest silhouette's cells using recursion and depth-first search.
	 *
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @throws ArrayIndexOutOfBoundsException if image size is 0
	 */
	public void scan(int x, int y) throws ArrayIndexOutOfBoundsException {
		if (map[x][y] && !checked[x][y]) { // if the cell is a silhouette's cell and it is unchecked
			checked[x][y] = true; // mark the cell as checked

			// scan left cell
			if (x > 0) {
				scan(x - 1, y);
			}
			// scan right cell
			if (x < map.length - 1) {
				scan(x + 1, y);
			}
			// scan upper cell
			if (y > 0) {
				scan(x, y - 1);
			}
			// scan lower cell
			if (y < map[0].length - 1) {
				scan(x, y + 1);
			}

			++size;
		}
	}
}
