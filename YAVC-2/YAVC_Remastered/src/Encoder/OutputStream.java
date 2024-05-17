package Encoder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import Main.config;
import Utils.QueueObject;
import Utils.Vector;

public class OutputStream {
	private File OUTPUT_FILE = null;
	private boolean isWriterActive = false;
	private ArrayList<QueueObject> QUEUE = new ArrayList<QueueObject>();
	private int fileCounter = 0;
	
	public OutputStream(File file) {
		File out = new File(file.getAbsolutePath() + "/YAVC-Res.yavc.part");
		out.mkdir();
		this.OUTPUT_FILE = out;
	}
	
	private void writeVectors(File file, ArrayList<Vector> vecs) {
		StringBuilder vectors = new StringBuilder(vecs.size() * 4);
		vectors.append(config.VECTOR_START);
		int offset = config.CODING_OFFSET;
		
		for (Vector v : vecs) {
			char posX = (char)(v.getPosition().x + offset), posY = (char)(v.getPosition().y + offset);
			char span = (char)((getVectorSpanChar(v.getSpanX(), offset) << 8) | getVectorSpanChar(v.getSpanY(), offset));
			char refAndSize = (char)((getReferenceChar(v.getReference(), offset)) << 8 | (v.getSize() & 0xFF));
			
			vectors.append(posX);
			vectors.append(posY);
			vectors.append(span);
			vectors.append(refAndSize);
		}
		
		try {
			Files.write(Path.of(file.getAbsolutePath()), vectors.toString().getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int getReferenceChar(int reference, int offset) {
		int res = Math.abs(reference);
		
		if (reference < 0) {
			res = (1 << 7) | res;
		}
		
		return res;
	}
	
	private int getVectorSpanChar(int span, int offset) {
		int res = Math.abs(span) + offset;
		
		if (span < 0) {
			res = (1 << 7) | res;
		}
		
		return res & 0xFF;
	}
	
	//Inverse of function above
	private int[] getVectorSpanInt(char span, int offset) {
		int x = (span >> 8) & 0x7F, y = span & 0x7F;
		
		if (((int)(span >> 7) & 0x1) == 1) {
			y *= (-1);
		}
		
		if (((int)(span >> 15) & 0x1) == 1) {
			x *= (-1);
		}
		
		return new int[] {x - offset, y - offset};
	}
	
	public void addObjectToOutputQueue(QueueObject obj) {
		this.QUEUE.add(obj);
	}
	
	public void activate() {
		if (this.OUTPUT_FILE == null) {
			System.err.println("No output defined!");
			System.exit(0);
		}
		
		this.isWriterActive = true;
		
		Thread writer = new Thread(() -> {
			try {
				while (isWriterActive == true) {
					if (QUEUE.size() == 0) {
						Thread.sleep(300);
					} else {
						QueueObject obj = QUEUE.get(0);
						File f = new File(OUTPUT_FILE.getAbsolutePath() + "/F" + fileCounter++ + ".yavcf");
						f.createNewFile();
						
						writeVectors(f, obj.getVectors());
						
						QUEUE.remove(0);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		writer.start();
	}
	
	public void shutdown() {
		this.isWriterActive = false;
	}
}
