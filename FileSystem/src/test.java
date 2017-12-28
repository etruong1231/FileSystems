import java.nio.ByteBuffer;

public class test {

	
	public static void main(String[] args) 
	{
		int x = 192;
		byte arr[] = new byte[4];
		byte change = (byte) (x);
		arr[1] = change;
		byte[] change1 =ByteBuffer.allocate(4).putInt(64).array();
		System.out.println(change1[2]);
	}
}
