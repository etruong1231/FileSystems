
public class OFT {

    // directory + up to 3 open files
    public static final int OFT_MAX_SIZE = 4;
    // 64 byte for r/w buffer+4 Byte for current position+4 Byte for index + 4 Byte for length of file
    public static final int OFT_OFF_SIZE = 76;

    private IOSystem IO;
    private BitMap bitmap;
    private byte[][] table;
    // table = block of filename || pos of read || descriptor index

    public OFT(IOSystem IO, BitMap bitmap) {
    	this.IO = IO; this.bitmap = bitmap;
        this.table = new byte[OFT_MAX_SIZE][OFT_OFF_SIZE];
    
        // for every entry in the OFT
        for (int i = 0; i < this.table.length; i++) {
            // for every byte in the r/w buffer
            for (int j = 0; j < 64; j++) {
                this.table[i][j] = IOSystem.EMPTY_BYTE;
            }
        }
        this.openDirectory();
   
    }
    
    //Directory functions
    public void openDirectory()
    {
    	this.setPosition(0, 0);
        this.setDescriptorNumb(0, 0);
        this.setFileLength(0, 0);
    }

   public void updateDir()
   {
	   byte[] directory = IO.read_block(7);
	   for(int i = 0 ; i < 64; i++)
		   this.table[0][i] = directory[i];
	   byte[] dir = IO.read_block(0);
	   this.setPosition(0, (int) dir[7]);
	   this.setFileLength(0, (int) dir[3]);
	  
   }
  
   
   // Editing OFT entries
   public void clearOFTEntry(int index){
	   for(int i= 0; i < 64; i++)
		   this.table[index][i] = -1;
	   for(int i=64; i < 76; i++)
		   this.table[index][i] = 0;
   }
   
   public void setBuffer(int entryIndex, byte[] arr)
   {
	   for(int i = 0 ; i < 64; i++)
		   this.table[entryIndex][i] = arr[i];
   }
   
   public void write(int index, String cha, int count)
   {
	  // get current position of the OFT
	   int desIndex = this.table[index][71];
	   int curr_pos = PackableMemory.intUnpack(this.table[index], 64);
	   byte[] charToBytes = cha.getBytes();
	   byte charByte  = charToBytes[0];
	   
	   //need to check if we need to rewrite the buffer it if it passes 64
	   int curr_block_index = Math.floorDiv(curr_pos,64);
	   for(int i = curr_pos; i < (curr_pos+count);i++)
	   {	//gonna go over the buffer and has no data in other blocks
		   // needs to allocate more space 
		   int total_byte = PackableMemory.intUnpack(table[index], 72);
		   if(((i % 64) == 0 && i >= total_byte) && i != 0 )
		   {
			   byte[] block = new byte[IOSystem.BLOCK_SIZE];
			   fillblock(block,index, curr_block_index);
			   //get next available block for descriptor and update directory
			   curr_block_index++;
			   //check directory
			   int blockIndex = Math.floorDiv((desIndex*16),64)+1;
			   int pos = ((16*(desIndex-1) % 64) + 8 + (4*curr_block_index)-1);
			   byte[] descriptor = IO.read_block(blockIndex);
			   int aval_block = bitmap.closestDataBlock();
			   System.out.println(blockIndex+" "+pos);
			   descriptor[pos] = (byte) aval_block;
			   bitmap.setOne(aval_block);
			   //set the buffer
			   byte[] newBuffer = IO.read_block(aval_block);
			   setBuffer(index, newBuffer);
		   }
		   // needs to move to next buffer without allocating a block
		   else if((i % 64) == 0 && !(i >= total_byte))
		   {
			   byte[] block = new byte[IOSystem.BLOCK_SIZE];
			   fillblock(block,index, curr_block_index);
			   //get next available block for descriptor and update directory
			   curr_block_index++;
			   //check directory
			   byte[] w_block = new byte[IOSystem.BLOCK_SIZE];
			   this.fillblock(w_block, index, curr_block_index-1);
			   
			   
			   // change according to block index
			   //get the descriptor number so we can find the block to put in buffer
			   int blockNum = this.getBlockNum(index, curr_block_index);
			   byte[] new_block = new byte[IOSystem.BLOCK_SIZE];
			   new_block = IO.read_block(blockNum);
			   this.setBuffer(index, new_block);
			   
		   }
		   int buffer_pos = (i % 64);
		   if(table[index][buffer_pos] == -1 )
			   table[index][75] += (byte)(1);
		   table[index][buffer_pos] = charByte;
	   }
	   //update current position
	   table[index][67] += (byte) count;
	  
	   
   }
   
   public void changePosition(int OFTindex, int pos)
   {
	   int curr_pos = PackableMemory.intUnpack(table[OFTindex], 64);
	   //System.out.println(curr_pos);
	   int curr_block = Math.floorDiv(curr_pos, 64);
	   
	   int next_block = Math.floorDiv(pos, 64);
	   
	   if(curr_block == 3 )
		   curr_block = 2;
	   if(next_block == 3)
		   next_block = 2;
	   // currently in the same block
	   if(curr_block == next_block)
		   return;
	   //write the OFTentry to block
	   byte[] w_block = new byte[IOSystem.BLOCK_SIZE];
	   //System.out.println(curr_block+ " "+next_block);
	   
	   this.fillblock(w_block, OFTindex, curr_block);
	   
	   
	   // change according to block index
	   //get the descriptor number so we can find the block to put in buffer
	   int blockNum = this.getBlockNum(OFTindex, next_block);
	   byte[] new_block = new byte[IOSystem.BLOCK_SIZE];
	   new_block = IO.read_block(blockNum);
	   
	   this.setBuffer(OFTindex, new_block);
	   this.setPosition(OFTindex, pos);
	   
   }
   
   public String readFile(int OFTindex, int count)
   {
	   int curr_pos = PackableMemory.intUnpack(table[OFTindex], 64);
	   int curr_block_index = Math.floorDiv(curr_pos, 64);
	   
	   String results = "";
	   
	   for(int i = curr_pos; i < curr_pos+count;i++){
		 //reach to the end of file or reach to no more writing
		   if(this.table[OFTindex][i%64] == -1)
		   {
			   int total_byte = PackableMemory.intUnpack(table[OFTindex], 72);
			   table[OFTindex][67] = (byte) total_byte;
			   return results;
		   }
		   if(i > 192)
		   {
			   table[OFTindex][67] = (byte)192;
			   return results;
		   }
		   if((i % 64) == 0 && i != 0)
		   {
	
			   
			   byte[] block = new byte[IOSystem.BLOCK_SIZE];
			   fillblock(block,OFTindex, curr_block_index);
			   //get next available block for descriptor and update directory
			   curr_block_index++;
			   //System.out.println(curr_block_index);
			   //check directory
			   byte[] w_block = new byte[IOSystem.BLOCK_SIZE];
			   this.fillblock(w_block, OFTindex, curr_block_index-1);
			   
			   
			   // change according to block index
			   //get the descriptor number so we can find the block to put in buffer
			   int blockNum = this.getBlockNum(OFTindex, curr_block_index);
			   byte[] new_block = new byte[IOSystem.BLOCK_SIZE];
			   new_block = IO.read_block(blockNum);
			   this.setBuffer(OFTindex, new_block);
			   
		   }
		   byte[] b = this.table[OFTindex];
		   String result = new String(b);
		   String[] str = result.split("");
		   results += str[i%64];
		  
		   
	   }
	   table[OFTindex][67] += (byte) count;
	   return results;
   }
   
   public void closeFile(int index)
   {
	   //get the data from OFT
	   byte[] blockdata = new byte[IOSystem.BLOCK_SIZE];
	   for(int i = 0; i < 64; i++)
		   blockdata[i] = this.table[index][i];
	   // now need to find where it belongs to put it in the block
	   int blockIndex = PackableMemory.intUnpack(this.table[index], 64);
	   int curr_block = Math.floorDiv(blockIndex,64);
	   if(curr_block == 3)
		   curr_block = 2;
	   int blockNum = getBlockNum(index, curr_block);
	   //System.out.println(blockNum);
	   IO.write_block(blockNum, blockdata);
	   
	   
   }
   
   
   
   // fill information back to block from OFT
   
   

   public void fillblock(byte[] block_arr, int index, int curr_block_index)
   {
	   for(int i = 0 ; i < block_arr.length; i++)
	   {
		   block_arr[i] = table[index][i] ;
	   }
	   int block_index = getBlockNum(index,curr_block_index);
	   //System.out.println(block_index);
	   IO.write_block(block_index, block_arr);
   }
  
   public int getBlockNum(int OFTindex, int curr_block_index)
   {
	   int desIndex = this.table[OFTindex][71];
	   //System.out.println(desIndex);
	   //check directory
	   int blockIndex;
	   if(((desIndex*16) % 64) == 0)
		   blockIndex = Math.floorDiv((desIndex*16),64);
	   else   
		   blockIndex = Math.floorDiv((desIndex*16),64)+1;
	   if(desIndex == 0)
		   blockIndex = 0;
	   int pos = ((16*(desIndex-1) % 64) + 8 + (4*curr_block_index)-1);
	   if(desIndex == 0)
		   pos = 8 + (4 * curr_block_index) - 1;
	   byte[] directory = IO.read_block(blockIndex);
	  // System.out.println(blockIndex+" "+pos);
	   //System.out.println(directory[pos]);
	   return directory[pos]; 
   }
   
   public boolean OFTopenFile(int index)
   {
	   for(int i=1; i< 4; i++){
		   if (this.table[1][71] < 0)
				   return true;
	   }
	   return false;
   }
   
   public int getDesIndex(int OFTindex)
   {
	   return this.table[OFTindex][71];
   }
   
   
   public int getCurrentPos(int index)
   {
	   return PackableMemory.intUnpack(this.table[index], 64);
   }
   
   
   // boolean functions to check OFT 
   
   public boolean EmptyOFTEntry(int index)
   {
	   if (table[index][71] == 0)
		   return true;
	   return false;
   }
   
   public boolean opened(int desIndex){
	   //System.out.println(desIndex);
	   for(int i=1; i< 4; i++){
		   if (this.table[i][71] == desIndex)
				   return true;
	   }
	   return false;
			
   }
   public int getOFTIndex(int desIndex)
   {
	   for(int i=1; i< 4; i++){
		   if (this.table[i][71] == desIndex)
				   return i;
	   }
	   return -1;
   }
   public boolean overFill(int index, int count)
   {
	   int pos = PackableMemory.intUnpack(this.table[index], 64);
	   if(pos+count > 192)
		   return true;
	   return false;
   }
   
   // functions to set up OFT ENTRIES
   public void setPosition(int entryIndex, int posVal) {
       if (posVal > 192) {
           posVal = 192;
       }
       PackableMemory.intPack(this.table[entryIndex], posVal, 64);
   }

   public void setDescriptorNumb(int entryIndex, int descVal) {
       PackableMemory.intPack(this.table[entryIndex], descVal, 68);
   }

   public void setFileLength(int entryIndex, int fileLenVal) {
       PackableMemory.intPack(this.table[entryIndex], fileLenVal, 72);
   }

   /* public void printTable() {
        System.out.println("\n~~~~~~~~~~OFT~~~~~~"
                + "[buffer, pos, desc, length]~~~~");
        
        for (int i = 0; i < this.table.length; i++) {
            System.out.print("|OFT-ROW|");
            for (int j = 0; j < this.table[i].length; j++) {
                System.out.print(" " + this.table[i][j]);
            }
            System.out.print("\r\n");
        }
    }*/

}