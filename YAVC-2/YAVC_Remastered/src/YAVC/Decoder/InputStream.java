package YAVC.Decoder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class InputStream {
	private File cache = null;
	
	public InputStream(File file) {
		this.cache = file;
	}
	
	public String getMetadata() {
		String content = null;
		File metaFile = new File(this.cache.getAbsolutePath() + "/META.yavc");
		
		if (!metaFile.exists()) {
			System.err.println("No metadata found! > Abort process");
			System.exit(0);
		}
		
		try {
			content = new String(Files.readAllBytes(Path.of(metaFile.getAbsolutePath())), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return content;
	}
	
	public String getStartFrame() {
		String content = null;
		File sf_file = new File(this.cache.getAbsolutePath() + "/SF.yavc");

		if (!sf_file.exists()) {
			System.err.println("No start frame found! > Abort process");
			System.exit(0);
		}
		
		try {
			content = new String(Files.readAllBytes(Path.of(sf_file.getAbsolutePath())), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return content;
	}
	
	public String getFrame(int frame) {
		String content = null;
		
		File f_file = new File(this.cache.getAbsolutePath() + "/F" + frame + ".yavcf");
		
		if (!f_file.exists()) {
			System.err.println("No frame found! > Abort process");
			System.exit(0);
		}
		
		try {
			content = new String(Files.readAllBytes(Path.of(f_file.getAbsolutePath())), StandardCharsets.ISO_8859_1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return content;
	}
}
