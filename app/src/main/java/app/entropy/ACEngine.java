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

package app.entropy;

public class ACEngine {

	final private static double[] probability = { 0.5, 0.5 };

	static class Intervall {
		private double start;
		private double end;

		public Intervall(double start, double end) {
			this.start = start;
			this.end = end;
		}

		public Intervall[] divide(double percentage) {
			double range = this.end - this.start;
			double splitPoint = this.start + (range * percentage);
			return new Intervall[] { new Intervall(this.start, splitPoint), new Intervall(splitPoint, this.end) };
		}
		
		public boolean contains(double tag) {
			return (this.start <= tag && tag < this.end); 
		}

		public double getTag() {
			return (this.start + this.end) * 0.5;
		}
		
		@Override
		public String toString() {
			return "Intervall [" + this.start + "; " + this.end + "]";
		}
	}

	public static double encode(byte[] data) {
		Intervall intervall = new Intervall(0.0, 1.0);
		for (byte b : data) {
			for (int i = 0; i < Byte.SIZE; i++) {
				int bit = b & 0x01;
				b >>= 1;
				intervall = intervall.divide(probability[0])[bit];
			}
		}
		return intervall.getTag();
	}

	public static byte[] decode(double tag, int length) {
		Intervall intervall = new Intervall(0.0, 1.0);
		byte[] data = new byte[length];
		for (int j = 0; j < data.length; j++) {
			byte b = 0;
			for (int i = 0; i < Byte.SIZE; i++) {
				Intervall [] intervalls = intervall.divide(probability[0]);
				int bit;
				if (intervalls[0].contains(tag)) {
					bit = 0;
					intervall = intervalls[0];
				} else {
					bit = 1;
					intervall = intervalls[1];
				}
				b |= (bit << i);
			}
			data[j] = b;
		}
		return data;
	}
}
