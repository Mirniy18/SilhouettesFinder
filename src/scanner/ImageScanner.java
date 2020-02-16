package scanner;

import java.util.List;

/**
 * Contains the similarities of the DFS and BDF algorithm's implementations.
 */
public abstract class ImageScanner {
	public int minSize; // the min size of the silhouette

	public boolean[][] map, checked; // the map to scan and the array of checked cells

	protected int size; // the size of the current silhouette

	public ImageScanner(boolean[][] map, int minSize) {
		this.map = map;
		this.minSize = minSize;
	}

	public abstract List<Integer[]> scan();
}
