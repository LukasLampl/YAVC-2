/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2024  Lukas Nian En Lampl

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

package app;

import java.io.File;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import app.encoder.DCTEngine;

public class Main {
	public static DCTEngine DCT_ENGINE = new DCTEngine();
	
	public static void main(String [] args) {
		System.out.println("Args: " + Arrays.toString(args));
		JOptionPane.showConfirmDialog(null, "Start on confirmation");
		
		ArgumentProcessor processor = new ArgumentProcessor();
		
		try {
			processor.processArgs(args);
			processor.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
