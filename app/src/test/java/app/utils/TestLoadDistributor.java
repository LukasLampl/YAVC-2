package app.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import app.engines.prediction.interprediction.EncodingVector;
import app.engines.prediction.interprediction.Vector;
import app.managers.LoadDistributor;

public class TestLoadDistributor {
	private Random random = new Random();
	private int[] sizes = {4, 8, 16, 32, 64, 128};
	
	@Test
	public void testLoadDistributor001() {
		List<Vector> l = new ArrayList<Vector>();
		
		for (int i = 0; i < 16383; i++) {
			l.add(generateVector());
		}
		
		LoadDistributor<Vector> dist = new LoadDistributor<Vector>();
		dist.setAllAndCompute(l);
		
		for (List<Vector> batch : dist.getIterable()) {
			assertTrue(batch.size() > 0);
		}
	}
	
	private Vector generateVector() {
		return new EncodingVector(random.nextInt() + 1, random.nextInt() + 1, sizes[(int)(MathUtils.abs(random.nextInt()) % sizes.length)]);
	}
	
	
	public void testLoadDistributor002() {
		int size = 32768;
		int sizeOfBlock = 8;
		int batches = 8;
		List<Vector> l = new ArrayList<Vector>();
		
		for (int i = 0; i < size; i++) {
			l.add(new EncodingVector(1, 1, sizeOfBlock));
		}
		
		LoadDistributor<Vector> dist = new LoadDistributor<Vector>(batches);
		dist.setAllAndCompute(l);
		
		int sizePerBatch = size / batches;
		
		for (List<Vector> batch : dist.getIterable()) {
			assertEquals(sizePerBatch, batch.size());
		}
	}
}
