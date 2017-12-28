import java.io.IOException;
import java.util.Scanner;

public class project1 {

	public static User user = new User();
	private static Scanner sc;
	
	public static void main(String[] args) throws IOException 
	{
		sc = new Scanner(System.in);
		
		System.out.println("Please enter command File: ");
		String file_name = sc.nextLine();
		user.readFile(file_name);
	}
}
