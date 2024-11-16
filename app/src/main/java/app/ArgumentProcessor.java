package app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import app.decoder.Decoder;
import app.encoder.Encoder;

public class ArgumentProcessor {
	public static final String INPUT_DEL = "-i";
	public static final String OUTPUT_DEL = "-o";
	
	public static final String PLAYBACK_DEL = "-playback";
	public static final String ENCODE_DEL = "-encode";
	public static final String DECODE_DEL = "-decode";
	
	private static HashMap<String, Consumer<String>> ARGUMENTS = new HashMap<String, Consumer<String>>();
	public static HashMap<String, Supplier<String>> SET_ARG_MAP = new HashMap<String, Supplier<String>>();
	private static HashMap<String, Integer> ARGUMENT_COUNT = new HashMap<String, Integer>();
	
	private static File outputFile = null;
	private static File inputFile = null;
	private static boolean encode = false;
	private static boolean decode = false;
	private static boolean playback = false;
	
	public ArgumentProcessor() {
		initArgs();
	}
	
	private void initArgs() {
		ARGUMENTS.put(INPUT_DEL, c -> this.setInputFile(c));
		SET_ARG_MAP.put(INPUT_DEL, () -> this.getInputFile());
		ARGUMENT_COUNT.put(INPUT_DEL, 1);
		ARGUMENTS.put(OUTPUT_DEL, c -> this.setOutputFile(c));
		SET_ARG_MAP.put(OUTPUT_DEL, () -> this.getOutputFile());
		ARGUMENT_COUNT.put(OUTPUT_DEL, 1);
		ARGUMENTS.put(ENCODE_DEL, c -> this.setEncode());
		ARGUMENT_COUNT.put(ENCODE_DEL, 0);
		ARGUMENTS.put(DECODE_DEL, c -> this.setDecode());
		ARGUMENT_COUNT.put(DECODE_DEL, 0);
		ARGUMENTS.put(PLAYBACK_DEL, c -> this.setPlayback());
		ARGUMENT_COUNT.put(PLAYBACK_DEL, 0);
	}
	
	public void processArgs(String args[]) throws FileNotFoundException {
		for (int i = 0; i < args.length; i++) {
			if (!ARGUMENTS.containsKey(args[i])) {
				throw new IllegalArgumentException("Unknown argument \"" + args[i] + "\".");
			}
			
			Consumer<String> con = ARGUMENTS.get(args[i]);
			int awaitedArgs = ARGUMENT_COUNT.get(args[i]);
			
			if (awaitedArgs > 0) {
				for (int n = 0; n < awaitedArgs; n++) {
					con.accept(args[++i]);
				}
			} else {
				con.accept(null);
			}
		}
	}
	
	public void run() {
		checkArgs();
		
		if (encode) {
			Encoder encoder = new Encoder(app.Main.DCT_ENGINE);
			encoder.encode(inputFile, outputFile);
		}
		
		if (decode) {
			Decoder decoder = new Decoder();
			decoder.decode(inputFile, outputFile, playback);
		}
	}
	
	private void checkArgs() {
		if (encode && decode) {
			exit("Can't encode and decode at the same time!");
		}
		
		if (decode) {
			if (!inputFile.getName().endsWith(".yavcv")) {
				exit("Unsupported file type as input.");
			} else if (outputFile != null) {
				if (!outputFile.isDirectory()) {
					exit("The output must be a directory.");
				}
			}
		}
		
		if (encode) {
			if (!inputFile.isDirectory()) {
				exit("The input must be a directory");
			} else if (!outputFile.getName().endsWith("yavcv")) {
				exit("Currently only the \".yavcv\" file type is supported.");
			}
		}
		
		if (playback && encode) {
			exit("YAVC can only playback decoding videos.");
		}
	}
	
	private void setInputFile(String path) {
		inputFile = new File(path);
		
		if (!inputFile.exists()) {
			exit("The input file \"" + path + "\" is not available.");
		}
	}
	
	private void setOutputFile(String name) {
		if (name.startsWith("C:") || name.startsWith("/")) {
			outputFile = new File(name);
			
			if (!outputFile.exists()) {
				try {
					outputFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			String path = System.getProperty("user.dir");
			outputFile = new File(path + "/" + name);
			
			try {
				outputFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (!outputFile.exists()) {
			exit("The output file \"" + name + "\" is not available.");
		}
	}
	
	private void setEncode() {
		encode = true;
	}
	
	private void setDecode() {
		decode = true;
	}
	
	private void setPlayback() {
		playback = true;
	}
	
	private String getOutputFile() {
		return outputFile.getAbsolutePath();
	}
	
	private String getInputFile() {
		return inputFile.getAbsolutePath();
	}
	
	private void exit(String errorMsg) {
		System.err.println(errorMsg);
		System.exit(0);
	}
}
