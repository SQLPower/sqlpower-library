package ca.sqlpower.util;

public class CacheStats {
	protected int totalInserted;
	protected int totalRequested;
	protected int totalHits;
	protected int totalMisses;

	public CacheStats() {
	}

	public void cacheFlush() {
		totalInserted = 0;
		totalRequested = 0;
		totalHits = 0;
		totalMisses = 0;
	}

	public int getTotalInserted() {
		return totalInserted;
	}

	public int getTotalRequested() {
		return totalRequested;
	}

	public int getTotalHits() {
		return totalHits;
	}

	public int getTotalMisses() {
		return totalMisses;
	}

	/**
	 * Returns a number between 0 and 1 indicating the cache hit
	 * ratio.  0 is worst (no hits); 1 is best but unachievable unless
	 * the cache is pre-populated.
	 */
	public double getHitRatio() {
		if (totalHits == 0) return 0.0;
		else return ((double) totalHits) / ((double) totalRequested);
	}
}
