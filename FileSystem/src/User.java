import java.util.ArrayList;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

//driver for user
public class User {
	
	public static FileSystem FS;
	public static Scanner sc = new Scanner(System.in);
	
	public static final String outputfile = "18063651.txt";
	
	public void readFile(String inputfile) throws IOException
	{
		// reads the file and takes the command
		PrintWriter out = new PrintWriter(outputfile);
		FileReader in = new FileReader(inputfile);
	    BufferedReader br = new BufferedReader(in);
	    String inputs;
	    // gets command and print inputs
	    while ((inputs = br.readLine()) != null) {
	    	System.out.println("input >>>> " + inputs);
	    	String output = handleCommand(inputs);
	    	System.out.println("output >>>> "+output);
	    	//writes to the output file
	    	FS.printHelp();
	    	out.println(output);
	    }
	    out.close();
	}
	public String handleCommand(String inputs) throws FileNotFoundException
	{
		String[] command = inputs.split(" ");
		if(!command[0].equals("in") && FS.equals(null)){
			// no file system is active
			return "error";
		}
		if(command[0].equals("cr") && command.length == 2){
			//need to check if the file exists
			return FS.create(command[1]);
		}else if(command[0].equals("de") && command.length == 2){
			//destroy the file, close the file, make sure file exists to destroy
			return FS.destroy(command[1]);
		}else if(command[0].equals("op") && command.length == 2){
			// need to get index of an avaliable OFT
			return FS.open(command[1]);
		}else if(command[0].equals("cl") && command.length == 2){
			//close the file at a specific index from the OFT
			return FS.close(Integer.parseInt(command[1]));
		}else if(command[0].equals("rd") && command.length == 3){
			// read a file at a specific point of the file index
			//need to get the text of amount of count from pointer and on
			return FS.read(Integer.parseInt(command[1]), Integer.parseInt(command[2]));
		}else if(command[0].equals("wr") && command.length == 4){
			// sequentially write <count> number of <char>s into the specified file <index> at its current position
			return FS.write(Integer.parseInt(command[1]), command[2], Integer.parseInt(command[3]));
		}else if(command[0].equals("sk") && command.length == 3){
			// seek: set the current position of the specified file <index> to <pos>
			return FS.lseek(Integer.parseInt(command[1]), Integer.parseInt(command[2]));
		}else if(command[0].equals("dr") && command.length == 1){
			// list the names of all files
			return FS.directory();
		}else if(command[0].equals("in") && command.length <= 2){
			try{
				FS.loadFile(command[1]);
			    return command[1] + " restored";
			}catch(Exception e){
				
			}
			FS = new FileSystem();
			//create a disk using the prescribed dimension parameters and initialize it; also open directory
			//If file does not exist, output: disk initialized
			//If file does exist, output: disk restored
			//need to check if file exists or not
			return "disk intialized";
		}else if(command[0].equals("sv") && command.length == 2){
			save_file(command[1]);
			return "disk saved";
		}else if(inputs.isEmpty()){
			return "";
		}else
			return "error";
	}
	public void save_file(String file_name) throws FileNotFoundException
	{
		FS.saveFile(file_name);
	}
	
	
	
	
}
