/**
 * @author Mahmoud Shokry and Mohammad Yasser
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;

import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.LinkOption.*;


public class CLI {
	private static final String[] validCommands = { "pwd", "ls", "cd", "cp",
		"mv", "rm", "rmdir", "mkdir", "cat", "more", "date", "clear"
		, "find", "grep"};

	private static File currentPath;

	private static abstract class Executable {
		public abstract void execute(ArrayList<String> args);
	}

	public static Executable[] methods = {
			new Executable() {public void execute(ArrayList<String> args) {pwd();}}, 
			new Executable() {public void execute(ArrayList<String> args) {ls();}}, 
			new Executable() {public void execute(ArrayList<String> args) {cd(args);}}, 
			new Executable() {public void execute(ArrayList<String> args) {cp(args);}},
			new Executable() {public void execute(ArrayList<String> args) {mv(args);}},
			new Executable() {public void execute(ArrayList<String> args) {rm(args);}},
			new Executable() {public void execute(ArrayList<String> args) {rmdir(args);}},
			new Executable() {public void execute(ArrayList<String> args) {mkdir(args);}},
			new Executable() {public void execute(ArrayList<String> args) {cat(args);}},
			new Executable() {public void execute(ArrayList<String> args) {more(args);}},
			new Executable() {public void execute(ArrayList<String> args) {date();}},
			new Executable() {public void execute(ArrayList<String> args) {clear();}},
			new Executable() {public void execute(ArrayList<String> args) {find(args);}},
			new Executable() {public void execute(ArrayList<String> args) {grep(args);}}
	};

	public static void run() {
		final String PROMPT = "$ ";
		Scanner keyboard = new Scanner(System.in);
		currentPath = new File(System.getProperty("user.home"));
		while (true) {
			System.out.print(PROMPT);
			String line = keyboard.nextLine();

			if (line.trim().compareTo("exit") == 0)
				break;

			String[] commands = line.split(";");

			for (int i = 0; i < commands.length; ++i) {
				execute(commands[i]);
			}
		}
		keyboard.close();
	}

	private static void execute(String commandLine) {
		writeToConsole();
		ArrayList<String> args = new ArrayList<String>();
		String command = parse(commandLine, args);
		boolean found = false;
		for (int i = 0; i < validCommands.length; i++) {
			if (command.compareTo(validCommands[i]) == 0) {
				methods[i].execute(args);
				found = true;
				break;
			}
		}
		if (!found && command.length() > 0)
			System.out.println(command + ": command not found");
		writeToConsole();
	}

	// TODO: handle quoted space-separated arguments
	private static String parse(String commandLine, ArrayList<String> args) {
		String[] splitted = commandLine.split(" ");
		boolean commandFound = false, stop = false;
		String command = new String();
		for (int i = 0; i < splitted.length; ++i) {
			splitted[i] = splitted[i].trim();
			if (splitted[i].length() > 0) {
				if (commandFound) {
					if (splitted[i].equals(">") || splitted[i].equals(">>")) {
						if (i + 1 == splitted.length)
							System.out.println("Error redirecting output");
						else
							writeToFile(splitted[i + 1], splitted[i].equals(">>"));
						stop = true;
					}
					if (!stop)
						args.add(splitted[i]);
				} else {
					command = splitted[i];
					commandFound = true;
				}
			}
		}
		return command;
	}
	
	//////////////////// commands //////////////////////////////////////
	
	private static boolean relativePath(String path) {
		return path.charAt(0) != '/';
	}

	private static String getFullPath(String path) {
		if (relativePath(path))
			path = currentPath.getPath() + "/" + path;
		return path;
	}
	
	private static void pwd() {
		System.out.println(currentPath.getAbsolutePath());
	}
	
	private static void cd(ArrayList<String> args) {
		if (args.size() == 0) {
			currentPath = new File(System.getProperty("user.home"));
			return;
		}
		File temp = new File(getFullPath(args.get(0)));
		if (temp.isDirectory())
			currentPath = new File(getFullPath(args.get(0)));
		else
			System.out.println("Error! Not a directory.");
		String editedPath = currentPath.getAbsolutePath();
		while (editedPath.contains("/..")) {
			int idx = editedPath.indexOf("/..");
			String firstHalf = editedPath.substring(0, idx);
			String secondHalf = editedPath.substring(idx + 3);
			editedPath = firstHalf.substring(0, firstHalf.lastIndexOf('/')) + secondHalf;
		}
		while (editedPath.contains("/.") && (editedPath.indexOf("/.") + 2 == 
				editedPath.length() || editedPath.charAt(editedPath.indexOf("/.") + 2) == '/')) {
			int idx = editedPath.indexOf("/.");
			String firstHalf = editedPath.substring(0, idx);
			String secondHalf = editedPath.substring(idx + 2);
			editedPath = firstHalf + secondHalf;
		}
		currentPath = new File(editedPath);
	}

	private static void ls() {
		File[] list = currentPath.listFiles();
		for (int i = 0; i < list.length; ++i)
			if (list[i].isFile())
				System.out.println(list[i].getName());
			else
				System.out.println(list[i].getName());
	}
	
	private static void cp(ArrayList<String> args) {
		if (args.size() < 2) {
			System.out.println("cp: missing file operand\nTry 'help cp' for more information.");
			return;
		}
		
		String destinationPath = getFullPath(args.get(args.size() - 1));
		for (int i = 0; i + 1 < args.size(); ++i) {
			String sourcePath = args.get(i);
			File sourceFile = new File(getFullPath(sourcePath));
	
			if (sourceFile.isDirectory()) {
				System.out.println("Failed to copy \'" + sourcePath + "\': Is a directory");
				continue;
			}
			
			cp(sourceFile, new File(destinationPath));
		}
	}
	
	private static void cp(File sourceFile, File destinationFile) {
		String destinationPath = destinationFile.getAbsolutePath();
		String tempDest = destinationFile.getAbsolutePath() + "/" + sourceFile.getName();
		destinationFile = new File(tempDest);

		if (!sourceFile.isDirectory() || !destinationFile.exists()) {
			try {
				Files.copy(sourceFile.toPath(), destinationFile.toPath(), REPLACE_EXISTING, NOFOLLOW_LINKS);
			} catch (IOException e) {
				System.out.println("Invalid path.");
			}
		}
		
		if (sourceFile.isDirectory()) {
			File[] list = sourceFile.listFiles();
			for (int i = 0; i < list.length; ++i) {
				cp(list[i], new File(destinationPath + "/" + sourceFile.getName()));
			}
		}
	}

	private static void mv(ArrayList<String> args) {
		if (args.size() < 2) {
			System.out.println("mv: missing file operand\nTry 'help mv' for more information.");
			return;
		}
		File destination = new File(getFullPath(args.get(args.size() - 1)));
		for (int i = 0; i + 1 < args.size(); i++) {
			File toMove = new File(getFullPath(args.get(i)));
			cp(toMove, destination);
			rm(toMove);
		}
	}

	private static void rm(File targetFile) {
		if (targetFile.isDirectory()) {
			File[] listOfFiles = targetFile.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				rm(listOfFiles[i]);
			}
		}

		try {
			Files.delete(targetFile.toPath());
		} catch (IOException e) {
			System.out.println("Invalid path.");
		}
	}

	private static void rm(ArrayList<String> args) {
		if (args.size() == 0) {
			System.out.println("rm: missing operand\nTry 'help rm' for more information.");
			return;
		}
		for (int i = 0; i < args.size(); ++i) {
			String path = args.get(i);
			File targetFile = new File(getFullPath(path));
			if (!targetFile.isFile()) {
				System.out.println("Failed to remove \'" + path + "\': Is a directory");
				continue;
			}
			rm(targetFile);
		}
	}
	
	private static void rmdir(ArrayList<String> args) {
		if (args.size() == 0) {
			System.out.println("rmdir: missing operand\nTry 'help rmdir' for more information.");
			return;
		}
		for (int i = 0; i < args.size(); ++i) {
			String path = args.get(i);
			File f = new File(getFullPath(path));
			if (!f.isDirectory()) {
				System.out.println("Failed to remove \'" + path + "\': Not a directory");
				continue;
			}
			if (f.listFiles().length != 0) {
				System.out.println("Failed to remove \'" + path + "\': Directory not empty");
				continue;
			}
			rm(f);
		}
	}

	private static void mkdir(ArrayList<String> args) {
		if (args.size() == 0) {
			System.out.println("mkdir: missing operand\nTry 'help mkdir' for more information.");
			return;
		}
		String dir = getFullPath(args.get(0));
		new File(dir).mkdirs();
	}

	private static void cat(ArrayList<String> args) {
		for (int i = 0; i < args.size(); ++i) {
			BufferedReader reader;
			try {
				reader = new BufferedReader(new FileReader(getFullPath(args.get(i))));
			} catch (FileNotFoundException e) {
				System.out.println("Error! File not found");
				continue;
			}
			String line = new String("");
			while (true) {
				try {
					line = reader.readLine();
				} catch (IOException e) {
					System.out.println("Error while reading file.");
				}
				if (line == null)
					break;
				System.out.println(line);
			}
			try {
				reader.close();
			} catch (IOException e) {
				System.out.println("Error while closing the reader.");
			}
		}
	}

	private static void more(ArrayList<String> args) {
		for (int j = 0; j < args.size(); ++j) {
			File file = new File(getFullPath(args.get(j)));
			if (args.size() > 1)
				System.out.println(file.getName() + "\n" + "::::::::::::::::::::::::");
			BufferedReader reader;
			try {
				reader = new BufferedReader(new FileReader(file.getAbsolutePath()));
			} catch (FileNotFoundException e) {
				System.out.println("Error! File not found");
				return;
			}
			String line = null, inputLine = new String();
			final int numberOfLines = 32;
	
			Scanner input = new Scanner(System.in);
	
			do {
				for (int i = 0; i < numberOfLines; ++i) {
					try {
						line = reader.readLine();
					} catch (IOException e) {
						System.out.println("Error!");
						break;
					}
					if (line != null)
						System.out.println(line);
				}
	
				if (line != null)
					inputLine = input.nextLine();
	
			} while (line != null && (inputLine.length() == 0));
			if (args.size() > 1)
				System.out.println("::::::::::::::::::::::::");
			
			// TODO: Check if these cause any errors..
			input.close();
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
		
	private static void writeToFile(String filePath, boolean append) {
		try {
			System.setOut(new PrintStream(new FileOutputStream(getFullPath(filePath), append)));
		} catch (FileNotFoundException e) {
			System.out.println("File not found.");
		}
	}

	private static void writeToConsole() {
		System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
	}
	
	private static void date() {
		DateFormat dateFormat = new SimpleDateFormat("E yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		System.out.println(dateFormat.format(cal.getTime()));
	}
	
	private static void clear() {
		for (int i = 0; i < 64; ++i)
			System.out.print("\n");
	}
	
	private static void find(ArrayList<String> args) {
		if (args.size() < 2) {
			System.out.println("Too few arguments.");
			return;
		}
		String name = args.get(0);
		String path = getFullPath(args.get(1));
		find(name, new File(path));
	}

	private static void find(String name, File directory) {
		// TODO: Validate
		File[] list = directory.listFiles();
		for (int i = 0; i < list.length; i++) {
			if (list[i].isFile()) {
				if (list[i].getName().equals(name))
					System.out.println(list[i].getAbsolutePath());
			} else if (list[i].isDirectory()) {
				find(name, new File(list[i].getAbsolutePath()));
			}
		}
	}
	
	private static void grep(ArrayList<String> args) {
		if (args.size() < 2) {
			System.out.println("Too few arguments");
			return;
		}
		String text = args.get(0);
		String fileName = args.get(1);
		Scanner reader;
		try {
			reader = new Scanner(new File(getFullPath(fileName)));
		} catch (FileNotFoundException e) {
			System.out.println("File not found.");
			return;
		}
		while (reader.hasNextLine()) {
			String line = reader.nextLine();
			if (line.contains(text)) {
				System.out.println(line);
			}
		}
		reader.close();
	}
	
	//////////////////// main //////////////////////////////////////
	public static void main(String[] args) {
		CLI.run();
	}
}
