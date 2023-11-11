package sune.app.mediadown.util;

import java.lang.invoke.MethodHandles;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** @since 00.02.05 */
public final class Opt<T> {
	
	private static final Opt<?> EMPTY = new Opt<>();
	private static final Function<?, ?> FN_NULL = ((v) -> null);
	
	private final Supplier<OptValue<T>> supplier;
	private final Supplier<OptValue<?>> originalSupplier;
	private Function<OptValue<T>, OptValue<T>> pipeline;
	private OptValue<?> value;
	private boolean isEvaluated;
	private Class<?> primitiveClass;
	
	private Opt() {
		this.supplier = nullSupplier();
		this.originalSupplier = typeErasure(this.supplier);
		this.pipeline = fnNull();
	}
	
	private Opt(Supplier<T> supplier) {
		this.supplier = valueSupplier(Objects.requireNonNull(supplier));
		this.originalSupplier = typeErasure(this.supplier);
		this.pipeline = fnIdentity();
	}
	
	// Used for Opt::map and Opt::copy
	private Opt(Supplier<OptValue<?>> originalSupplier, Supplier<OptValue<T>> supplier) {
		this.supplier = Objects.requireNonNull(supplier);
		this.originalSupplier = Objects.requireNonNull(originalSupplier);
		this.pipeline = fnIdentity();
	}
	
	private static final <T> Function<T, T> fnNull() {
		@SuppressWarnings("unchecked")
		Function<T, T> fn = (Function<T, T>) FN_NULL;
		return fn;
	}
	
	private static final <T> Function<T, T> fnIdentity() {
		return Function.identity();
	}
	
	private static final <T> Supplier<OptValue<T>> nullSupplier() {
		return (() -> null);
	}
	
	private static final <T> Supplier<OptValue<T>> valueSupplier(Supplier<T> supplier) {
		return (() -> OptValue.of(supplier.get()));
	}
	
	@SuppressWarnings("unchecked")
	private static final <T> Supplier<OptValue<?>> typeErasure(Supplier<OptValue<T>> supplier) {
		return (Supplier<OptValue<?>>) (Supplier<?>) supplier;
	}
	
	@SuppressWarnings("unchecked")
	private static final <U> Supplier<OptValue<U>> typeCast(Supplier<OptValue<?>> supplier) {
		return (Supplier<OptValue<U>>) (Supplier<?>) supplier;
	}
	
	public static final <T> Opt<T> empty() {
		@SuppressWarnings("unchecked")
		Opt<T> empty = (Opt<T>) EMPTY;
		return empty;
	}
	
	public static final <T> Opt<T> of(T value) {
		return new Opt<>(() -> value);
	}
	
	public static final <T> Opt<T> ofSupplier(Supplier<T> supplier) {
		return new Opt<>(supplier);
	}
	
	public static final <T> OptCondition<T> condition(Predicate<T> condition) {
		return OptCondition.of(condition);
	}
	
	private final void ensureEvaluated() {
		if(!isEvaluated) {
			value = pipeline.apply(supplier.get());
			isEvaluated = true;
		}
	}
	
	private final T primitiveZeroValue() {
		try {
			return (T) MethodHandles.zero(primitiveClass).invoke();
		} catch(Throwable ex) {
			// Should not happen
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private final T value() {
		T val = (T) value.value();
		return primitiveClass != null && val == null ? primitiveZeroValue() : val;
	}
	
	public final Opt<T> ifTrue(OptPredicate<T> condition) {
		Objects.requireNonNull(condition);
		final Function<OptValue<T>, OptValue<T>> ref = this.pipeline;
		this.pipeline = ((v) -> {
			OptValue<T> t = ref.apply(v);
			return !t.isEmpty() && condition.test(t.value()) ? t : OptValue.empty();
		});
		return this;
	}
	
	public final Opt<T> ifFalse(OptPredicate<T> condition) {
		Objects.requireNonNull(condition);
		return ifTrue(condition.negate());
	}
	
	public final Opt<T> filter(OptPredicate<T> filter) {
		Objects.requireNonNull(filter);
		final Function<OptValue<T>, OptValue<T>> ref = this.pipeline;
		this.pipeline = ((v) -> {
			OptValue<T> t = ref.apply(v);
			return !t.isEmpty() && filter.test(t.value()) ? t : OptValue.empty();
		});
		return this;
	}
	
	public final <U> Opt<U> map(Function<T, U> mapper) {
		Objects.requireNonNull(mapper);
		final Function<OptValue<T>, OptValue<T>> ref = this.pipeline;
		final Supplier<OptValue<T>> sup = this.supplier;
		return new Opt<>(originalSupplier, () -> {
			OptValue<T> t = ref.apply(sup.get());
			return !t.isEmpty() ? OptValue.of(mapper.apply(t.value())) : OptValue.empty();
		});
	}
	
	public final Opt<T> ifEmpty(Consumer<? super T> action) {
		if(isEmpty()) Objects.requireNonNull(action).accept(value());
		return this;
	}
	
	public final Opt<T> ifEmptyOrElse(Consumer<? super T> action, Consumer<? super T> orElse) {
		if(isEmpty()) Objects.requireNonNull(action).accept(value());
		else          Objects.requireNonNull(orElse).accept(value());
		return this;
	}
	
	public final Opt<T> ifPresent(Consumer<? super T> action) {
		if(isPresent()) Objects.requireNonNull(action).accept(value());
		return this;
	}
	
	public final Opt<T> ifPresentOrElse(Consumer<? super T> action, Consumer<? super T> orElse) {
		if(isPresent()) Objects.requireNonNull(action).accept(value());
		else            Objects.requireNonNull(orElse).accept(value());
		return this;
	}
	
	public final <S extends T> Opt<S> cast() {
		@SuppressWarnings("unchecked")
		Opt<S> casted = (Opt<S>) this;
		return casted;
	}
	
	public final <S> Opt<S> castAny() {
		@SuppressWarnings("unchecked")
		Opt<S> casted = (Opt<S>) this;
		return casted;
	}
	
	public final T get() {
		ensureEvaluated();
		return value();
	}
	
	public final T orNull() {
		return orElse(null);
	}
	
	public final T orElse(T other) {
		ensureEvaluated();
		return !isEmpty() ? value() : other;
	}
	
	public final T orElseGet(Supplier<T> supplier) {
		ensureEvaluated();
		return !isEmpty() ? value() : supplier.get();
	}
	
	public final T orElseThrow() throws NoSuchElementException {
		ensureEvaluated();
		if(isEmpty())
			throw new NoSuchElementException("No value present");
		return value();
	}
	
	public final <X extends Throwable> T orElseThrow(Supplier<? extends X> supplier) throws X {
		ensureEvaluated();
		if(isEmpty()) throw supplier.get();
		return value();
	}
	
	public final Opt<T> unbox(Class<?> clazz) {
		if(!Objects.requireNonNull(clazz).isPrimitive())
			throw new IllegalArgumentException("Not a primitive class");
		primitiveClass = clazz;
		return this;
	}
	
	public final Opt<T> box() {
		primitiveClass = null;
		return this;
	}
	
	public final <U> Opt<T> or(Function<Opt<U>, Opt<T>> transformer) {
		Opt<U> opt = new Opt<>(originalSupplier, typeCast(originalSupplier));
		return isPresent() ? this : Objects.requireNonNull(Objects.requireNonNull(transformer).apply(opt));
	}
	
	public final Opt<T> or(Supplier<Opt<T>> supplier) {
		return isPresent() ? this : Objects.requireNonNull(Objects.requireNonNull(supplier).get());
	}
	
	public final Opt<T> copy() {
		Opt<T> copy = new Opt<>(originalSupplier, supplier);
		copy.pipeline = pipeline;
		copy.value = value;
		copy.isEvaluated = isEvaluated;
		copy.primitiveClass = primitiveClass;
		return copy;
	}
	
	public final boolean isEmpty() {
		ensureEvaluated();
		return value.isEmpty();
	}
	
	public final boolean isPresent() {
		ensureEvaluated();
		return !value.isEmpty();
	}
	
	public final boolean isEvaluated() {
		return isEvaluated;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(isEvaluated, value);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Opt<?> other = (Opt<?>) obj;
		return isEvaluated == other.isEvaluated && Objects.equals(value, other.value);
	}
	
	@Override
	public String toString() {
		return "Opt[value=" + value + ", isEvaluated=" + isEvaluated + "]";
	}
	
	private static final class OptValue<T> {
		
		private static final OptValue<?> EMPTY = new OptValue<>();
		
		private final T value;
		private final boolean isEmpty;
		
		private OptValue() {
			this.value = null;
			this.isEmpty = true;
		}
		
		private OptValue(T value) {
			this.value = value;
			this.isEmpty = false;
		}
		
		public static final <T> OptValue<T> of(T value) {
			return new OptValue<>(value);
		}
		
		public static final <T> OptValue<T> empty() {
			@SuppressWarnings("unchecked")
			OptValue<T> v = (OptValue<T>) EMPTY;
			return v;
		}
		
		public T value() {
			return value;
		}
		
		public boolean isEmpty() {
			return isEmpty;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(isEmpty, value);
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			OptValue<?> other = (OptValue<?>) obj;
			return isEmpty == other.isEmpty && Objects.equals(value, other.value);
		}
		
		@Override
		public String toString() {
			return "OptValue[value=" + value + ", isEmpty=" + isEmpty + "]";
		}
	}
	
	@FunctionalInterface
	public static interface OptPredicate<T> {
		
		boolean test(T value);
		
		default OptPredicate<T> negate() {
			return ((t) -> !test(t));
		}
		
		default Predicate<T> predicate() {
			return ((t) -> test(t));
		}
	}
	
	public static final class OptCondition<T> implements OptPredicate<T> {
		
		private static final Predicate<?> TRUE  = ((t) -> true);
		private static final Predicate<?> FALSE = ((t) -> false);
		
		private Predicate<T> condition;
		private boolean result;
		private boolean evaluated;
		
		private OptCondition(Predicate<T> condition) {
			this.condition = Objects.requireNonNull(condition);
		}
		
		public static final <T> OptCondition<T> of(Predicate<T> condition) {
			return new OptCondition<>(condition);
		}
		
		public static final <T> OptCondition<T> ofTrue() {
			@SuppressWarnings("unchecked")
			OptCondition<T> cond = of((Predicate<T>) TRUE);
			return cond;
		}
		
		public static final <T> OptCondition<T> ofFalse() {
			@SuppressWarnings("unchecked")
			OptCondition<T> cond = of((Predicate<T>) FALSE);
			return cond;
		}
		
		// Can be used as a method reference for predicates
		public static final <T> boolean returnTrue(T t) {
			return ofTrue().test(t);
		}
		
		// Can be used as a method reference for predicates
		public static final <T> boolean returnFalse(T t) {
			return ofFalse().test(t);
		}
		
		private final void ensureEvaluated() {
			if(!evaluated)
				throw new IllegalStateException("Not evaluated yet");
		}
		
		private final OptCondition<T> merge(Predicate<T> condition,
				BiFunction<Predicate<T>, Predicate<T>, Predicate<T>> merger) {
			final Predicate<T> ref = this.condition;
			this.condition = merger.apply(ref, condition);
			return this;
		}
		
		public final OptCondition<T> and(Predicate<T> condition) {
			return merge(Objects.requireNonNull(condition), (a, b) -> ((t) -> a.test(t) && b.test(t)));
		}
		
		public final OptCondition<T> andOpt(OptPredicate<T> condition) {
			return and(condition.predicate());
		}
		
		public final OptCondition<T> or(Predicate<T> condition) {
			return merge(Objects.requireNonNull(condition), (a, b) -> ((t) -> a.test(t) || b.test(t)));
		}
		
		public final OptCondition<T> orOpt(OptPredicate<T> condition) {
			return or(condition.predicate());
		}
		
		public final OptCondition<T> not() {
			return merge(null, (a, b) -> ((t) -> !a.test(t)));
		}
		
		public final OptCondition<T> evaluate(T value) {
			result = condition.test(value);
			evaluated = true;
			return this;
		}
		
		@Override
		public final boolean test(T value) {
			evaluate(value);
			return isTrue();
		}
		
		public final boolean isTrue() {
			ensureEvaluated();
			return result;
		}
		
		public final boolean isFalse() {
			ensureEvaluated();
			return !result;
		}
	}
	
	public static final class OptMapper<A, B> implements Function<A, B> {
		
		private static final Function<?, ?> IDENTITY = Function.identity();
		
		private final Function<A, B> mapper;
		
		private OptMapper(Function<A, B> mapper) {
			this.mapper = Objects.requireNonNull(mapper);
		}
		
		/** @since 00.02.09 */
		public static final <A> OptMapper<A, A> identity() {
			@SuppressWarnings("unchecked")
			Function<A, A> mapper = (Function<A, A>) IDENTITY;
			return new OptMapper<>(mapper);
		}
		
		public static final <A, B> OptMapper<A, B> of(Function<A, B> mapper) {
			return new OptMapper<>(mapper);
		}
		
		@Override
		public B apply(A a) {
			return mapper.apply(a);
		}
		
		/** @since 00.02.09 */
		public <C> OptMapper<A, C> then(Function<B, C> then) {
			return new OptMapper<>((a) -> then.apply(mapper.apply(a)));
		}
	}
}