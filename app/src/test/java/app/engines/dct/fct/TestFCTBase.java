/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2025  Lukas Nian En Lampl, Hans Lampl

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package app.engines.dct.fct;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Random;

import app.utils.ArrayUtils;

public abstract class TestFCTBase {
	protected static final double DELTA = 0.01;
	private static Random r = new Random();
	
	protected double[][] generateRandomMatrix(final int size, final double scale) {
		double[][] matrix = new double[size][size];
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				matrix[x][y] = r.nextDouble() * scale;
			}
		}
		
		return matrix;
	}
	
	protected double[][] generateFilledMatrix(final int size, final double number) {
		double[][] matrix = new double[size][size];
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				matrix[x][y] = number;
			}
		}
		
		return matrix;
	}
	
	protected double[][] generateXResonatingMatrix(final int size, final double number1, final double number2) {
		double[][] matrix = new double[size][size];
		
		for (int x = 0; x < size; x++) {
			double number = x % 2 == 0 ? number1 : number2;
			
			for (int y = 0; y < size; y++) {
				matrix[x][y] = number;
			}
		}
		
		return matrix;
	}
	
	protected double[][] generateYResonatingMatrix(final int size, final double number1, final double number2) {
		double[][] matrix = new double[size][size];
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				double number = y % 2 == 0 ? number1 : number2;
				matrix[x][y] = number;
			}
		}
		
		return matrix;
	}
	
	protected double[][] generateCheckerboardMatrix(final int size, final double number1, final double number2) {
		double[][] matrix = new double[size][size];
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				double number = (y + x) % 2 == 0 ? number1 : number2;
				matrix[x][y] = number;
			}
		}
		
		return matrix;
	}
	
	protected double[][] getCopy(final double[][] original) {
		final int size = original.length;
		double[][] copy = new double[size][size];
		ArrayUtils.copy2DArray(original, 0, 0, copy, 0, 0, size, size);
		return copy;
	}
	
	protected void assertArray(final double[][] expected, final double[][] actual) {
		for (int y = 0; y < expected.length; y++) {
			assertArrayEquals(expected[y], actual[y], DELTA);
		}
	}
}
