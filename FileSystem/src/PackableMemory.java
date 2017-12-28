//This class contains implementations of methods to 
//   -- pack an integer into 4 consecutive bytes of a byte array
//   -- unpack an integer from 4 consecutive bytes of a byte array
//   -- exhaustively test the pack and unpack methods.
// 
// This file should be saved as PackableMemory.java.  Once it has been
//  compiled, the tester can be invoked by typing "java PackableMemory"

class PackableMemory
{
   int size; 
   public byte mem[] = null;

   public PackableMemory(int size)
   {
      this.size = size;
      this.mem = new byte[size];
   }

   // Pack the 4-byte integer val into the four bytes mem[loc]...mem[loc+3].
   // The most significant porion of the integer is stored in mem[loc].
   // Bytes are masked out of the integer and stored in the array, working
   // from right(least significant) to left (most significant).
   public static void intPack(byte[] arr, int val, int loc) {
       final int MASK = 0xff;
       for (int i = 3; i >= 0; i--) {
           arr[loc + i] = (byte) (val & MASK);
           val = val >> 8;
       }
   }

   // Unpack the four bytes mem[loc]...mem[loc+3] into a 4-byte integer,
   //  and return the resulting integer value.
   // The most significant porion of the integer is stored in mem[loc].
   // Bytes are 'OR'ed into the integer, working from left (most significant) 
   //  to right (least significant)
   public static int intUnpack(byte[] arr, int loc) {
       final int MASK = 0xff;
       int v = (int) arr[loc] & MASK;
       for (int i = 1; i < 4; i++) {
           v = v << 8;
           v = v | ((int) arr[loc + i] & MASK);
       }
       return v;
   }



  

}
