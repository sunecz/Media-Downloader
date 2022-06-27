package sune.app.mediadown.util;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

public class AtomicDouble extends Number implements Serializable {
	
	// Serialization UID
	private static final long serialVersionUID = 6911457895534106002L;
	
	// Fields
	private final AtomicLong value;
	
	public AtomicDouble() {
		this(0.0);
	}
	
	public AtomicDouble(double value) {
		this.value = new AtomicLong(double2long(value));
	}
	
	private static final long double2long(double value) {
		return Double.doubleToLongBits(value);
	}
	
	private static final double long2double(long value) {
		return Double.longBitsToDouble(value);
	}
	
	private static final LongUnaryOperator duop2luop(DoubleUnaryOperator duop) {
		return new LongUnaryOperator() {
			
			private final DoubleUnaryOperator op = duop;
			
			@Override
			public long applyAsLong(long operand) {
				return double2long(op.applyAsDouble(long2double(operand)));
			}
		};
	}
	
	private static final LongBinaryOperator dbop2lbop(DoubleBinaryOperator dbop) {
		return new LongBinaryOperator() {
			
			private final DoubleBinaryOperator op = dbop;
			
			@Override
			public long applyAsLong(long left, long right) {
				return double2long(op.applyAsDouble(long2double(left), long2double(right)));
			}
		};
	}
	
	public final double get() {
	    return long2double(value.get());
	}
	
	public final void set(double newValue) {
		value.set(double2long(newValue));
	}
	
	public final void lazySet(double newValue) {
		value.lazySet(double2long(newValue));
	}
	
	public final double getAndSet(double newValue) {
		return long2double(value.getAndSet(double2long(newValue)));
	}
	
	public final boolean compareAndSet(double expect, double update) {
		return value.compareAndSet(double2long(expect), double2long(update));
	}
	
	public final double getAndIncrement() {
		return long2double(value.getAndIncrement());
	}
	
	public final double getAndDecrement() {
		return long2double(value.getAndDecrement());
	}
	
	public final double getAndAdd(double delta) {
		return long2double(value.getAndAdd(double2long(delta)));
	}
	
	public final double incrementAndGet() {
		return long2double(value.incrementAndGet());
	}
	
	public final double decrementAndGet() {
		return long2double(value.decrementAndGet());
	}
	
	public final double addAndGet(double delta) {
		return long2double(value.addAndGet(double2long(delta)));
	}
	
	public final double getAndUpdate(DoubleUnaryOperator updateFunction) {
		return long2double(value.getAndUpdate(duop2luop(updateFunction)));
	}
	
	public final double updateAndGet(DoubleUnaryOperator updateFunction) {
		return long2double(value.updateAndGet(duop2luop(updateFunction)));
	}
	
	public final double getAndAccumulate(double x, DoubleBinaryOperator accumulatorFunction) {
		return long2double(value.getAndAccumulate(double2long(x), dbop2lbop(accumulatorFunction)));
	}
	
	public final double accumulateAndGet(double x, DoubleBinaryOperator accumulatorFunction) {
		return long2double(value.accumulateAndGet(double2long(x), dbop2lbop(accumulatorFunction)));
	}
	
	public String toString() {
	    return Double.toString(get());
	}
	
	@Override
	public int intValue() {
	    return (int) get();
	}
	
	@Override
	public long longValue() {
	    return (long) get();
	}
	
	@Override
	public float floatValue() {
	    return (float) get();
	}
	
	@Override
	public double doubleValue() {
	    return get();
	}
}