import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;

public class IOSystem {
	// ldisk = 64 blocks
	// block = 16 integers
	// ldisk[L][B]

	public static final int LDISK_SIZE = 64;
	public static final int BLOCK_SIZE = 64; 
	public static final int DIRECTORY_DESCRIPTOR_SIZE = 16; 
	public static final byte EMPTY_BYTE = -1;
	public static final int DESCRIPTOR_SIZE = 16;
	public static final int DIRECTORY_INDEX = 7;
	
	
	
	  // For serializing our ldisk to file
    public static final String BLOCK_DELIMITER = "\\$\\$";
    public static final String BYTE_DELIMITER = "\\*\\*";
    public static final String BLOCK_SEP = "$$";
    public static final String BYTE_SEP = "**";
	
	private byte[][] ldisk;
	
	public IOSystem(){
		this.ldisk = new byte[LDISK_SIZE][];

		this.ldisk[0] = new byte[DIRECTORY_DESCRIPTOR_SIZE];
        // each block has 64 bytes
        for (int i= 1; i < this.ldisk.length; i++) {
            this.ldisk[i] = new byte[BLOCK_SIZE]; // 64 bytes 
        }
        
        // fill the ldisk with "empty inputs"
        for (int i=0; i < this.ldisk.length; i++) {
            for (int j=0; j < this.ldisk[i].length; j++) {
                this.ldisk[i][j] = EMPTY_BYTE;
            }
        }
    }
	
	// reads the block from ldisk to main memory
	public byte[] read_block(int index)
	{
		return this.ldisk[index];
	}
	// write the main memory content to ldisk
	public void write_block(int index, byte[] arr)
	{ // never change the original block size
       this.ldisk[index] = arr;
    }
	// saves the content to a text file 
	public void save_ldisk(String text_File) throws FileNotFoundException {
        
        PrintWriter out = new PrintWriter(text_File);
        StringBuilder textForm = new StringBuilder();
        
        for (int i=0; i < LDISK_SIZE; i++) {
            byte[] row = this.read_block(i);      
            for (int j=0; j < row.length; j++) {
                textForm.append(row[j]);
                if (j != row.length - 1) {
                    textForm.append(BYTE_SEP);
                }
            }
            if (i != LDISK_SIZE - 1) {
                textForm.append(BLOCK_SEP);
            }
        }
        
        String output = textForm.toString();
        out.write(output);
        out.close();
    }
	
	public void reinit(byte[][] nLdisk)
	{
		this.ldisk = nLdisk;
	}
	
	public void printHelp()
	{
		for(int x= 0; x < ldisk.length; x++)
		{
			System.out.print("\n[Row "+x+"]: ");
			for(int i = 0; i < ldisk[x].length; i++)
			{
				System.out.print(ldisk[x][i]+" ");
			}
		}
	}

}
