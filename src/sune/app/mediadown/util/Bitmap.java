package sune.app.mediadown.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

import sune.app.mediadown.concurrent.StateMutex;

/**
 * <p>
 * Simple thread-safe implementation of a bitmap that grows as needed.
 * </p>
 * 
 * <p>
 * The underlying implementation uses long as words, therefore the capacity
 * of a bitmap is always a multiple of the number of bits in a long.
 * </p>
 * 
 * <p>
 * Since this implementation needs to grow as needed, it uses a read-write
 * lock-like mechanism. For simple reads and writes the lock is not used
 * at all, however when the underlying array needs to be resized, it uses
 * a single lock to block all read and write threads from accessing it.
 * This improves performance overall, since the resizing of the array is
 * likely not a frequent event. This mechanism is also used for clearing
 * the bitmap and calculating its size, i.e. the number of bits between
 * the first bit, either 0 or 1, and the last one (including) equal to 1.
 * </p>
 * 
 * <p>
 * By default, all bits in the bitmap are set to 0.
 * </p>
 * 
 * @since 00.02.09
 * @author Sune
 */
public final class Bitmap {
	
	private static final int BITS_PER_WORD = Long.SIZE;
	private static final VarHandle vhArray;
	private static final VarHandle vhWriteLock;
	private static final VarHandle vhReadCtr;
	
	static {
		vhArray = MethodHandles.arrayElementVarHandle(long[].class);
		
		try {
			MethodHandles.Lookup lookup = MethodHandles.lookup();
			vhWriteLock = lookup.findVarHandle(Bitmap.class, "writeLock", boolean.class);
			vhReadCtr = lookup.findVarHandle(Bitmap.class, "readCtr", int.class);
		} catch(NoSuchFieldException | IllegalAccessException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}
	
	private volatile long[] words;
	@SuppressWarnings("unused")
	private volatile int readCtr;
	@SuppressWarnings("unused")
	private volatile boolean writeLock;
	private final StateMutex lock;
	
	public Bitmap(int numBits) {
		if(numBits < 0) {
			throw new IllegalArgumentException("Number of bits < 0");
		}
		
		this.words = new long[numberOfWords(numBits)];
		this.lock = new StateMutex();
	}
	
	private static final int numberOfWords(int numBits) {
		return bitToWordIndex(numBits) + (bitToBitIndex(numBits) > 0 ? 1 : 0);
	}
	
	private static final int bitToWordIndex(int bit) {
		return bit / BITS_PER_WORD;
	}
	
	private static final int bitToBitIndex(int bit) {
		return bit % BITS_PER_WORD;
	}
	
	private static final int ceilDiv(int x, int y) {
		return -Math.floorDiv(-x, y);
	}
	
	private final void readLock() {
		while((boolean) vhWriteLock.getVolatile(this)) {
			lock.await();
		}
		
		vhReadCtr.getAndAdd(this, 1);
	}
	
	private final void readUnlock() {
		vhReadCtr.getAndAdd(this, -1);
	}
	
	private final void writeLock() {
		while(!vhWriteLock.compareAndSet(this, false, true)) {
			lock.await();
		}
		
		// Use busy-waiting since the reads should be fast
		while((int) vhReadCtr.getVolatile(this) > 0) {
			Thread.onSpinWait();
		}
	}
	
	private final void writeUnlock() {
		vhWriteLock.setVolatile(this, false);
		lock.unlock();
	}
	
	private final void ensureCapacityFor(int wordIndex, int bitIndex) {
		final int numWords = wordIndex + 1;
		readLock();
		
		try {
			final long[] ref = words;
			
			if(numWords <= ref.length) {
				return;
			}
		} finally {
			readUnlock();
		}
		
		writeLock();
		
		try {
			final long[] ref = words;
			final int length = ref.length;
			final int half = length / 2;
			
			int newLength = Math.multiplyExact(length, 3) / 2;
			for(;
				newLength - numWords < half;
				newLength = Math.multiplyExact(newLength, 3) / 2
			);
			
			long[] newWords = new long[newLength];
			System.arraycopy(ref, 0, newWords, 0, length);
			words = newWords;
		} finally {
			writeUnlock();
		}
	}
	
	private final void ensureIndex(int wordIndex, int bitIndex) {
		if(wordIndex < 0) {
			throw new IndexOutOfBoundsException("Word index < 0");
		}
		
		if(bitIndex < 0) {
			throw new IndexOutOfBoundsException("Bit index < 0");
		}
		
		ensureCapacityFor(wordIndex, bitIndex);
	}
	
	private final boolean get(int wordIndex, int bitIndex) {
		readLock();
		
		try {
			final long[] ref = words;
			return ((((long) vhArray.getVolatile(ref, wordIndex)) >> bitIndex) & 0b1L) != 0L;
		} finally {
			readUnlock();
		}
	}
	
	public void set(int bit) {
		final int wordIndex = bitToWordIndex(bit);
		final int bitIndex  = bitToBitIndex(bit);
		ensureIndex(wordIndex, bitIndex);
		readLock();
		
		try {
			final long[] ref = words;
			for(
				long oldValue = (long) vhArray.getAcquire(ref, wordIndex),
				     value    = 1L << bitIndex,
				     newValue = oldValue | value;
				(oldValue = (long) vhArray.compareAndExchangeRelease(ref, wordIndex, oldValue, newValue)) != newValue;
				newValue = oldValue | value
			);
		} finally {
			readUnlock();
		}
	}
	
	public void set(int bit, boolean value) {
		if(value)   set(bit);
		else      unset(bit);
	}
	
	public void unset(int bit) {
		final int wordIndex = bitToWordIndex(bit);
		final int bitIndex  = bitToBitIndex(bit);
		ensureIndex(wordIndex, bitIndex);
		readLock();
		
		try {
			final long[] ref = words;
			for(
				long oldValue = (long) vhArray.getAcquire(ref, wordIndex),
				     value    = ~(1L << bitIndex),
				     newValue = oldValue & value;
				(oldValue = (long) vhArray.compareAndExchangeRelease(ref, wordIndex, oldValue, newValue)) != newValue;
				newValue = oldValue & value
			);
		} finally {
			readUnlock();
		}
	}
	
	public void setWord(int index) {
		ensureIndex(index, 0);
		readLock();
		
		try {
			final long[] ref = words;
			vhArray.setVolatile(ref, index, 0xffffffffffffffffL);
		} finally {
			readUnlock();
		}
	}
	
	public void setWord(int index, boolean value) {
		if(value)   setWord(index);
		else      unsetWord(index);
	}
	
	public void unsetWord(int index) {
		ensureIndex(index, 0);
		readLock();
		
		try {
			final long[] ref = words;
			vhArray.setVolatile(ref, index, 0x0000000000000000L);
		} finally {
			readUnlock();
		}
	}
	
	public boolean get(int bit) {
		final int wordIndex = bitToWordIndex(bit);
		final int bitIndex  = bitToBitIndex(bit);
		ensureIndex(wordIndex, bitIndex);
		return get(wordIndex, bitIndex);
	}
	
	public long getWord(int index) {
		ensureIndex(index, 0);
		readLock();
		
		try {
			final long[] ref = words;
			return (long) vhArray.getVolatile(ref, index);
		} finally {
			readUnlock();
		}
	}
	
	public void clear() {
		writeLock();
		
		try {
			final long[] ref = words;
			Arrays.fill(ref, 0L);
		} finally {
			writeUnlock();
		}
	}
	
	public int size() {
		writeLock();
		
		try {
			final long[] ref = words;
			
			for(int i = ref.length - 1; i >= 0; --i) {
				long word = ref[i];
				
				if(word != 0L) {
					long mask = 1L << (BITS_PER_WORD - 1);
					
					for(int k = BITS_PER_WORD - 1; k >= 0; --k, mask >>>= 1) {
						if((word & mask) != 0) {
							return i * BITS_PER_WORD + k + 1;
						}
					}
				}
			}
			
			return 0;
		} finally {
			writeUnlock();
		}
	}
	
	public int sizeWords() { return ceilDiv(size(), BITS_PER_WORD); }
	
	public int countZeros() {
		writeLock();
		
		try {
			final long[] ref = words;
			int count = 0;
			
			for(int i = 0, l = words.length; i < l; ++i) {
				count += BITS_PER_WORD - Long.bitCount(ref[i]);
			}
			
			return count;
		} finally {
			writeUnlock();
		}
	}
	
	public int countOnes() {
		writeLock();
		
		try {
			final long[] ref = words;
			int count = 0;
			
			for(int i = 0, l = words.length; i < l; ++i) {
				count += Long.bitCount(ref[i]);
			}
			
			return count;
		} finally {
			writeUnlock();
		}
	}
	
	public int capacityWords() { return words.length; }
	public int capacity() { return words.length * BITS_PER_WORD; }
	public int bitsPerWord() { return BITS_PER_WORD; }
	
	// TODO: Just use a simple Iterator
	
	public Generator<Integer> zeros() { return new Gen.Zeros(this); }
	public Generator<Integer> ones() { return new Gen.Ones(this); }
	
	private static abstract class Gen extends Generator<Integer> {
		
		private final Bitmap bm;
		
		protected Gen(Bitmap bm) {
			this.bm = bm;
		}
		
		protected abstract boolean canAccept(boolean value);
		
		@Override
		protected void run() {
			for(int i = 0, l = bm.capacity(); i < l; ++i) {
				if(canAccept(bm.get(i))) {
					yield(i);
				}
			}
		}
		
		private static final class Zeros extends Bitmap.Gen {
			
			public Zeros(Bitmap bm) { super(bm); }
			@Override protected boolean canAccept(boolean value) { return !value; }
		}
		
		private static final class Ones extends Bitmap.Gen {
			
			public Ones(Bitmap bm) { super(bm); }
			@Override protected boolean canAccept(boolean value) { return value; }
		}
	}
}