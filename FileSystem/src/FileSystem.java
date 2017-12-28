import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class FileSystem {
	
	public static IOSystem IO = new IOSystem();
	private static BitMap bitmap;
	public static OFT OFT;
	
	
	public FileSystem(){
		startDirectory();
		this.bitmap = new BitMap();
		OFT = new OFT(IO,bitmap);
	}
	
	
	public byte[] stringToBytes(String input, int size) {
        if (input.length() > size) {
            input = input.substring(0, size);
        }
        byte[] bytes = input.getBytes();
        return bytes;
    }
	
	public String create(String file_name)
	{
		//check the directory if file exists or length greater than 4
		if(fileExists(file_name) || file_name.length() > 4 || checkFull() || file_name.length() < 0)
			return "error";
		//look for emptyDescriptor and empty Directory spot
		createDirectorySpot(file_name);
		createDescriptor(file_name);
		updateDirectoryByte(8);
		OFT.updateDir();
		return file_name+ " created";
	}
	public String destroy(String file_name)
	{
		if(!fileExists(file_name))
			return "error";
		// if destorying a file tat is open, close the file then empty out the blocks
		if(OFT.opened(Math.floorDiv((findDirectoryNum(file_name)),8)+1))
			this.close(OFT.getOFTIndex(Math.floorDiv((findDirectoryNum(file_name)),8)+1));
		removeDirectoryandDescriptor(file_name);
		updateDirectoryByte(-8);
		OFT.updateDir();
		return file_name + " destroyed";
	}
	
	public String open(String file_name)
	{
		if(!fileExists(file_name) || bitmap.closestOFTEntry() == -1 || OFT.opened(Math.floorDiv((findDirectoryNum(file_name)),8)+1))
			return "error";
		//search directory for index
		int desIndex = Math.floorDiv(findDirectoryNum(file_name),8);
		int index = changeToIndex(desIndex*16);
		byte[] descriptors = IO.read_block(index+1);
		int pos = changeNumtoPosition(desIndex*16);
		//allocate a free OFT entry
		int firstBlock = descriptors[pos+7];
		System.out.println("pos === "+pos);
		//System.out.println("desIndex == "+desIndex);
		//System.out.println("block == "+(pos+7));
		byte[] buffer = IO.read_block(firstBlock);
		int OFTEntry = bitmap.closestOFTEntry();
		bitmap.setOne(OFTEntry);
		OFT.setBuffer(OFTEntry, buffer);
		OFT.setPosition(OFTEntry, 0);
		OFT.setDescriptorNumb(OFTEntry, desIndex+1);
		OFT.setFileLength(OFTEntry, descriptors[pos+3]);
		//put in block number  of file || pos || descriptor index
		//return OFT index 
		return file_name+" opened "+OFTEntry;
	}
	
	public String close(int index)
	{
		if(index <= 0 || index > 3 || OFT.OFTopenFile(index))
			return "error";
		//need to write the buffer to the blockNum
		OFT.closeFile(index);
		bitmap.setZero(index);
		OFT.clearOFTEntry(index);
		return index + " closed";
	}
	
	public String read(int index, int count)
	{
		//read from wahtever position the OFT is at 
		// counts up to the count or to EOF
		return OFT.readFile(index,count);
	}
	
	public String write(int index, String cha, int count)
	{
		
		
		//  sequentially write <count> number of <char>s into the specified file <index> at its current position
		//Output: <count> bytes written
		//need to check if its writting more than it can contain
		int new_count = count;
		if(OFT.EmptyOFTEntry(index))
			return "error";
		if(OFT.overFill(index, count))
		{
			int curr_pos = OFT.getCurrentPos(index);
			new_count += (192- (curr_pos+ count));
		}
		OFT.write(index, cha, new_count);
		int desIndex = OFT.getDesIndex(index);
		this.updateBytes(new_count,desIndex);
		
		
		return count+" bytes written";
	}
	
	public String lseek(int index, int pos)
	{
		if(OFT.OFTopenFile(index))
			return "error";
		//GO to OFT see if position is already in buffer, if not gotta move or allocate
		OFT.changePosition(index, pos);
		return "position is "+ pos;
	}
	
	public String directory(){
		String results ="";
		for(int i = 7; i < 10; i ++)
		{
			byte[] directory = IO.read_block(i);
			String words = new String(directory);
			words =  words.replaceAll("[^a-zA-Z\\s\n]", " ");
			String[] names = words.split("\\s+");
			
			for(String fn : names)
				results += fn + " ";
		}
		return results;
		
			
	}
	
	
	public void updateBytes(int bytes, int desIndex)
	{
		int blockIndex = Math.floorDiv((desIndex*16),64)+1;
		System.out.println(blockIndex+" "+desIndex);
		int pos = ((16*desIndex)%64) - 13;
		if(pos < 0)
			pos = 63 - 13;
		byte[] directory = IO.read_block(blockIndex);
		System.out.println(blockIndex+" "+pos);
		directory[pos] += bytes;
	}

	public void printHelp(){
		IO.printHelp();
		//tOFT.printTable();
	}
	
	public void createDirectorySpot(String file_name)
	{
		//get the empty spot and fill in information
		int num = getEmptyDirectoryNum();
		int index = changeToIndex(num);
		System.out.println(num+" "+index);
		int pos = changeNumtoPosition(num);
		byte[] directory = IO.read_block(index+7);
		byte[] stringByte = stringToBytes(file_name,4);
		
		for(int i = 0; i < stringByte.length; i++)
		{
			directory[pos+i] = stringByte[i];
		}
		PackableMemory.intPack(directory, Math.floorDiv(num, 8)+1 ,pos+4);
		IO.write_block(index+7, directory);
		
		
	}
	public void createDescriptor(String file_name)
	{
		//get empty spot and fill in information
		int num = getEmptyDescriptorNum();
		int index = changeToIndex(num)+1;
		int pos = changeNumtoPosition(num);
		//System.out.println(num);
		byte[] descriptor = IO.read_block(index);
		for(int i=0;i <16; i++)
		{	//fill in descriptory
			descriptor[pos+i] = 0;
		}
		int closestBlock = bitmap.closestDataBlock();
		PackableMemory.intPack(descriptor,closestBlock, pos+4);
		bitmap.setOne(closestBlock);
			
	}
	//start the directory when starting a new disk
	
	public void startDirectory(){
		byte[] directory = IO.read_block(0);
		for(int i = 0; i <directory.length; i++)
			directory[i] = 0;
		directory[7] = 7; directory[11] = 8;directory[15] = 9;
		IO.write_block(0, directory);
	}
	
	public void updateDirectoryByte(int bytes){
		byte[] directory = IO.read_block(0);
		
		int curr_byte = (int) directory[3];
		curr_byte += bytes;
		directory[3] = (byte) curr_byte;
		IO.write_block(0, directory);
		
	}
	//change to find index depending on position
	public int changeToIndex(int position)
	{
		return Math.floorDiv(position, 64);
	}
	public int changeNumtoPosition(int position)
	{
		return position % 64;
	}
	
	//find empty spots for directory and descriptor
	
	
	public int getEmptyDirectoryNum(){
		int index = 0;
		for(int i = 7; i < 10; i++)
		{
			byte [] check = IO.read_block(i);
			for(int x = 0 ; x< 64 ; x+=8)
			{	
				if(check[x] == -1)
					return index;
				index+= 8;
			}
			
		}
		return -1;
	}
	public int getEmptyDescriptorNum()
	{
		int count = 0;
		for(int x = 1 ; x < 7; x++){
			byte [] check = IO.read_block(x);
			for(int i = 0; i < 64; i+=16)
			{	
				if (check[i] == -1)
					return count;	
				count += 16;
			
			}
		}
		return -1;
	}
	
	//clearing the directory and descriptor
	
	public void removeDirectoryandDescriptor(String file_name)
	{
		int DirNum = findDirectoryNum(file_name);
		clearDirectorySpot(DirNum);
		clearDescriptorSpot(DirNum);
		//update directory file
		
		
	}
	public void clearDescriptorSpot(int index)
	{
		int dIndex = changeToIndex((index/8) * 16);
		byte[] descriptors = IO.read_block(dIndex+1);
		int pos = changeNumtoPosition((index/8)*16);
		if((index/8) * 16 == 64)
			pos = 48;
		//System.out.println(index+" "+(dIndex+1)+" "+pos);
		
		for(int i = pos+7; i < pos+16; i+=4)
		{
			bitmap.setZero(descriptors[i]);
			if(descriptors[i] == 0)
				continue;
			else
				clearBlock(descriptors[i]);
			
		}
		
		for(int i = pos;i < pos+16 ; i++)
			descriptors[i] = -1;
		IO.write_block(dIndex+1, descriptors);
		
	
	}
	public void clearBlock(int blockIndex)
	{
		//System.out.println(blockIndex);
		byte[] block = new byte[IOSystem.BLOCK_SIZE];
		block = IO.read_block(blockIndex);
		for(int i = 0 ; i < IOSystem.BLOCK_SIZE; i++)
		{
			block[i] = -1;
		}
		IO.write_block(blockIndex, block);
	}
	public void clearDirectorySpot(int index)
	{
		int dIndex = changeToIndex(index);
		byte[] directory = IO.read_block(dIndex+7);
		int pos = index;
		if(index == 64) pos = 0;
		//System.out.println(index+" "+dIndex+" "+pos);
		for(int i = pos; i < (pos)+8; i++)
			directory[i] = -1;
		IO.write_block(dIndex+7, directory);
	}
	public int findDirectoryNum(String file_name)
	{
		byte[] stringByte = stringToBytes(file_name,4);
		int count = 0;
		for(int i=7; i < 10; i++)
		{
			byte [] directory = IO.read_block(i);
			for(int x = 0; x < 64; x+=8)
			{
				boolean found = true;
				for(int y = 0; y < stringByte.length; y++)
				{
					if(found == true)
					{	if(directory[x+y] == stringByte[y])
							found = true;
						else
							found = false;
					}
				}
				
				if(found)
				{
					for(int y = stringByte.length; y < 4; y++)
					{
						if(directory[x+y] == -1)
							found = true;
						else
							found = false;
					}
					if(found)
						return count;
				}
				count += 8;
			}
		}
		return -1;
	}
	
	
	//boolean functions
	
	public boolean fileExists(String file_name)
	{
		byte[] stringByte = stringToBytes(file_name,4);
		for(int i= 7; i < 10; i ++)
		{	
			byte[] directory = IO.read_block(i);
			for(int x = 0; x < 64; x+=8)
			{	
				boolean check = true;
				for(int y =0; y < stringByte.length; y++)
				{
					if(check == true)
					{
						if(directory[x+y] == stringByte[y])
							check = true;
						else
							check = false;
					}
				}
				if(check)
				{
					for(int y = stringByte.length; y < 4; y++)
					{
						//System.out.println(check);
						if(directory[x+y] == -1)
							check = true;
						else
							check = false;
					}
					if(check){
						System.out.println("FILEEXISTS");
						return true;
					}
				}
					
			}
		}
		return false;
	}
	

	
	
	public boolean checkFull()
	{
		byte[] lastFile = IO.read_block(9);
		if(lastFile[63] != -1)
			return true;
		return false;
	}


	// saves and load files
	public void saveFile(String file_name) throws FileNotFoundException
	{
		//need to close all files if any open
		for(int i = 1; i < 4; i++)
		{
			if(OFT.OFTopenFile(i))
				this.close(i);
		}
		IO.save_ldisk(file_name);
	}
	
	public void loadFile(String file_name) throws IOException
	{
		FileReader in = new FileReader(file_name);
        BufferedReader br = new BufferedReader(in);
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            // restore our file system
            String contents = sb.toString();
            byte[][] newLdisk = this.buildLdisk(contents);
            IO.reinit(newLdisk);
            this.fixBitMap();
        } finally {
            br.close();
        }
       
    
	}
	public void fixBitMap()
	{
		this.bitmap = new BitMap();
		byte[] bytes = new byte[IOSystem.BLOCK_SIZE];
		for(int i =1; i<7; i++)
		{
			bytes = IO.read_block(i);
			for(int x= 7; x < 64; x+=4)
			{
				if(bytes[x] == -1)
					continue;
				else
					bitmap.setOne(bytes[x]);
			}
			
		}
	}
private byte[][] buildLdisk(String backup) {
    String[] rows = backup.split(IOSystem.BLOCK_DELIMITER);   
    byte[][] ldisk = new byte[IOSystem.LDISK_SIZE][0];
    
    for (int i=0; i < rows.length; i++) {
        String row = rows[i];
        String[] stringBytes = row.split(IOSystem.BYTE_DELIMITER);

        byte[] bytes = new byte[stringBytes.length];
        for (int j=0; j < bytes.length; j++) {
            bytes[j] = Byte.parseByte(stringBytes[j]);
        }
        ldisk[i] = bytes;
    }
    return ldisk;
}
	
}
