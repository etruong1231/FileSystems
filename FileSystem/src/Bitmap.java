
class BitMap {

   
    private int[] bitmap;
    private int[] mask;

    public BitMap() {
    	
        this.bitmap = new int[2]; // 2 ints -> 8 bytes -> 64 bits == # of blocks
        this.mask = new int[32]; // size of int

        this.mask[31] = 1;
        for (int i=30; i >= 0; i--) {
            this.mask[i] = this.mask[i + 1] << 1;
        }
    }

  
    public int normIndex(int index) {
        return (index % 32);
    }

    public int arrIndex(int index) {
        return (index / 32);
    }

 
    public int invIndex(int arrIndex, int normIndex) {
        return (arrIndex * 32) + normIndex;
    }

    public void setZero(int index) {
        int arrIndex = this.arrIndex(index);
        int normIndex = this.normIndex(index);

        int invMask = ~this.mask[normIndex];
        this.bitmap[arrIndex] = this.bitmap[arrIndex] & invMask;
    }

    public void setOne(int index) {
        int arrIndex = this.arrIndex(index);
        int normIndex = this.normIndex(index);

        this.bitmap[arrIndex] = this.bitmap[arrIndex] | this.mask[normIndex];
    }

    /**
     * slots 1-3 are reserved for the OFT
     */
    public int closestOFTEntry() {
        int row = 0;
        for (int i=1; i <= 3; i++) {
            int isZeroBit = (this.bitmap[row] & this.mask[i]);
            if (isZeroBit == 0) {
                return this.invIndex(0, i);
            }
        }
        return -1;
    }
    
    public int closestDataBlock() {
        for (int i=0; i < 2; i++) {
            for (int j=0; j < 32; j++) {
                int tempJ = j;
                if (i == 0) {
                    tempJ = j + 10; // skip the first 10 slots!
                }
                if (tempJ >= 32) {
                    continue;
                }
                int isZeroBit = (this.bitmap[i] & this.mask[tempJ]);
                if (isZeroBit == 0) {
                    // bit j of bitmap[i] is zero
                    return this.invIndex(i, tempJ);
                }
            }
        }
        return -1;
    }
    
    public int closestZero() {
        for (int i=0; i < 2; i++) {
            for (int j=0; j < 32; j++) {
                int isZeroBit = (this.bitmap[i] & this.mask[j]);
                if (isZeroBit == 0) {
                    // bit j of bitmap[i] is zero
                    return this.invIndex(i, j);
                }
            }
        }
        return -1;
    }

    public int closestOne() {
        for (int i=0; i < 2; i++) {
            for (int j=0; j < 32; j++) {
                int isOneBit = (this.bitmap[i] | ~this.mask[j]);
                if (isOneBit == 1) {
                    // bit j of bitmap[i] is one
                    return this.invIndex(i, j);
                }
            }
        }
        return -1;
    }


    
}