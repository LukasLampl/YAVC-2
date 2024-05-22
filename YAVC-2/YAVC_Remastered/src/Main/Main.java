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

package Main;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;

import Decoder.InputProcessor;
import Decoder.InputStream;
import Encoder.Encoder;

public class Main {
	public static void main(String [] args) {
		JFileChooser jfc = new JFileChooser();
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.showDialog(null, null);
		
		if (jfc.getSelectedFile() == null) {
			return;
		}
		
		File in = jfc.getSelectedFile();
		
		jfc.showDialog(null, null);
		
		if (jfc.getSelectedFile() == null) {
			return;
		}
		
		File out = jfc.getSelectedFile();
		
		Encoder encoder = new Encoder();
		encoder.encode(in, out);
		decode(new File(out.getParent() + "/YAVC-Res.yavc.part"), out);
	}
	
	//DEBUG ONLY!
	public static void decode(File input, File output) {
		InputStream inputStream = new InputStream(input);
		InputProcessor processor = new InputProcessor();
		
		String startFrame = inputStream.getStartFrame();
		BufferedImage startFrameImg = processor.constructStartFrame(startFrame, new Dimension(176, 144));
		
		try {
			ImageIO.write(startFrameImg, "png", new File(input.getAbsolutePath() + "/SF.png"));
			ArrayList<BufferedImage> refs = new ArrayList<BufferedImage>();
			refs.add(startFrameImg);
			
			for (int i = 0; i < 100; i++) {
				String frame = inputStream.getFrame(i);
				BufferedImage result = processor.processFrame(frame, refs, new Dimension(176, 144));
				
				ImageIO.write(result, "png", new File(input.getAbsolutePath() + "/R_" + i + ".png"));
				refs.add(result);
				manageReferences(refs);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void manageReferences(ArrayList<?> references) {
		if (references == null) {
			return;
		}
		
		if (references.size() < config.MAX_REFERENCES) {
			return;
		}
		
		references.remove(0);
	}
}
