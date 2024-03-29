package sune.app.mediadown.media;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sune.app.mediadown.util.ComparatorCombiner;
import sune.app.mediadown.util.Opt.OptCondition;
import sune.app.mediadown.util.Opt.OptPredicate;
import sune.app.mediadown.util.Utils;

/** @since 00.02.05 */
public final class MediaFilter {
	
	private static final Predicate<Media> ALWAYS_TRUE = ((t) -> true);
	private static MediaFilter NONE;
	
	private final Comparator<Media> comparator;
	private final Predicate<Media> filter;
	
	private MediaFilter(Comparator<Media> comparator, Predicate<Media> filter) {
		this.comparator = comparator; // Can be null
		this.filter = filter != null ? filter : ALWAYS_TRUE;
	}
	
	public static final MediaFilter.Builder builder() {
		return new Builder();
	}
	
	public static final MediaFilter of(Comparator<Media> comparator) {
		return of(comparator, null);
	}
	
	public static final MediaFilter of(Comparator<Media> comparator, Predicate<Media> filter) {
		return comparator != null ? new MediaFilter(comparator, filter) : none();
	}
	
	public static final MediaFilter none() {
		return NONE == null ? (NONE = new MediaFilter(null, null)) : NONE;
	}
	
	public List<Media> sort(List<Media> media) {
		Objects.requireNonNull(media);
		Stream<Media> stream = media.stream().filter(filter);
		
		if(comparator != null) {
			stream = stream.sorted(comparator);
		}
		
		return stream.collect(Collectors.toList());
	}
	
	public Media filter(List<Media> media, Function<List<Media>, Media> filter) {
		return !media.isEmpty() ? filter.apply(sort(media)) : null;
	}
	
	public Media filter(List<Media> media) {
		return filter(media, (l) -> l.get(0));
	}
	
	public static final class Builder {
		
		private List<ComparisonOrder> comparisonOrder;
		private PriorityHolder<MediaFormat> formatPriority;
		private boolean bestQuality;
		private PriorityHolder<MediaQuality> qualityPriority;
		private MediaLanguage audioLanguage;
		/** @since 00.02.08 */
		private Predicate<Media> filter;
		
		private Builder() {
			comparisonOrder = ComparisonOrder.defaultOrder();
			formatPriority = new PriorityHolder<>();
			bestQuality = false;
			qualityPriority = new PriorityHolder<>();
			audioLanguage = MediaLanguage.UNKNOWN;
			filter = null;
		}
		
		private final List<ComparisonOrder> mergedComparisonOrder() {
			LinkedHashSet<ComparisonOrder> merged = new LinkedHashSet<>(comparisonOrder);
			merged.addAll(ComparisonOrder.defaultOrder());
			return merged.stream().collect(Collectors.toList());
		}
		
		public MediaFilter build() {
			ComparatorCombiner<Media> comparator = ComparatorCombiner.empty();
			OptCondition<Media> predicate = filter != null ? OptCondition.of(filter) : OptCondition.ofTrue();
			
			for(ComparisonOrder order : mergedComparisonOrder()) {
				switch(order) {
					case FORMAT:
						if(!formatPriority.isEmpty())
							comparator.combine(MediaFormatComparator.ofPriority(formatPriority));
						break;
					case BEST_QUALITY:
						if(bestQuality)
							comparator.combine(MediaQualityComparator.naturalReversed());
						break;
					case QUALITY:
						if(!qualityPriority.isEmpty())
							comparator.combine(MediaQualityComparator.ofPriority(qualityPriority));
						break;
					case AUDIO_LANGUAGE:
						if(!audioLanguage.is(MediaLanguage.UNKNOWN))
							predicate.andOpt(new AudioLanguageFilter(audioLanguage));
						break;
				}
			}
			
			return comparator.isValid()
						? MediaFilter.of(comparator, predicate.predicate())
						: MediaFilter.none();
		}
		
		public Builder comparisonOrder(ComparisonOrder... comparisonOrders) {
			return comparisonOrder(List.of(comparisonOrders));
		}
		
		public Builder comparisonOrder(List<ComparisonOrder> comparisonOrder) {
			this.comparisonOrder = Objects.requireNonNull(Utils.nonNullContent(comparisonOrder));
			return this;
		}
		
		public Builder formatPriority(MediaFormat... mediaFormats) {
			return formatPriority(List.of(mediaFormats));
		}
		
		public Builder formatPriority(List<MediaFormat> formatPriority) {
			this.formatPriority.priority(Objects.requireNonNull(Utils.nonNullContent(formatPriority)));
			return this;
		}
		
		public Builder formatPriorityReversed(boolean priorityReversed, boolean naturalReversed) {
			this.formatPriority.priorityReversed(priorityReversed).naturalReversed(naturalReversed);
			return this;
		}
		
		public Builder bestQuality(boolean bestQuality) {
			this.bestQuality = bestQuality;
			return this;
		}
		
		public Builder qualityPriority(MediaQuality... mediaQualities) {
			return qualityPriority(List.of(mediaQualities));
		}
		
		public Builder qualityPriority(List<MediaQuality> qualityPriority) {
			this.qualityPriority.priority(Objects.requireNonNull(Utils.nonNullContent(qualityPriority)));
			return this;
		}
		
		public Builder qualityPriorityReversed(boolean priorityReversed, boolean naturalReversed) {
			this.qualityPriority.priorityReversed(priorityReversed).naturalReversed(naturalReversed);
			return this;
		}
		
		/** @since 00.02.09 */
		public Builder preferredQuality(MediaQuality preferredQuality) {
			this.qualityPriority.preferred(preferredQuality);
			return this;
		}
		
		public Builder audioLanguage(MediaLanguage audioLanguage) {
			this.audioLanguage = Objects.requireNonNull(audioLanguage);
			return this;
		}
		
		/** @since 00.02.08 */
		public void filter(Predicate<Media> filter) {
			this.filter = filter;
		}
		
		public static enum ComparisonOrder {
			
			BEST_QUALITY, FORMAT, QUALITY, AUDIO_LANGUAGE;
			
			private static List<ComparisonOrder> defaultOrder;
			
			public static final List<ComparisonOrder> defaultOrder() {
				return defaultOrder == null ? defaultOrder = Collections.unmodifiableList(List.of(values())) : defaultOrder;
			}
		}
		
		private static final class PriorityHolder<T> {
			
			private List<T> priority = List.of();
			private boolean priorityReversed = false;
			private boolean naturalReversed = false;
			/** @since 00.02.09 */
			private T preferred = null;
			
			public PriorityHolder<T> priority(List<T> priority) {
				this.priority = priority;
				return this;
			}
			
			public PriorityHolder<T> priorityReversed(boolean priorityReversed) {
				this.priorityReversed = priorityReversed;
				return this;
			}
			
			public PriorityHolder<T> naturalReversed(boolean naturalReversed) {
				this.naturalReversed = naturalReversed;
				return this;
			}
			
			/** @since 00.02.09 */
			public PriorityHolder<T> preferred(T preferred) {
				this.preferred = preferred;
				return this;
			}
			
			public List<T> priority() {
				return priority;
			}
			
			public boolean priorityReversed() {
				return priorityReversed;
			}
			
			public boolean naturalReversed() {
				return naturalReversed;
			}
			
			/** @since 00.02.09 */
			public T preferred() {
				return preferred;
			}
			
			public boolean isEmpty() {
				return priority.isEmpty() && preferred == null;
			}
		}
	}
	
	private static final class MediaFormatComparator extends PriorityComparator<Media, MediaFormat> {
		
		private static final ArrayIndexBasedComparator<MediaFormat> naturalComparator
			= new ArrayIndexBasedComparator<>(MediaFormat::values);
		
		private MediaFormatComparator(List<MediaFormat> priority, boolean priorityReversed,
				boolean naturalReversed) {
			super(priority, priorityReversed, Media::format, naturalReversed, naturalComparator);
		}
		
		public static final MediaFormatComparator ofPriority(Builder.PriorityHolder<MediaFormat> holder) {
			return new MediaFormatComparator(
				holder.priority(), holder.priorityReversed(), holder.naturalReversed()
			);
		}
	}
	
	private static final class MediaQualityComparator extends PriorityComparatorOfComparable<Media, MediaQuality> {
		
		private MediaQualityComparator(List<MediaQuality> priority, boolean priorityReversed,
				boolean naturalReversed, MediaQuality preferred) {
			super(priority, priorityReversed, Media::quality, naturalReversed, MediaQuality::compareTo, preferred);
		}
		
		public static final MediaQualityComparator ofPriority(Builder.PriorityHolder<MediaQuality> holder) {
			return new MediaQualityComparator(
				holder.priority(), holder.priorityReversed(), holder.naturalReversed(), holder.preferred()
			);
		}
		
		public static final MediaQualityComparator naturalReversed() {
			return new MediaQualityComparator(List.of(), false, true, null);
		}
	}
	
	private static final class ArrayIndexBasedComparator<T> implements BiFunction<T, T, Integer> {
		
		private final Supplier<T[]> supplier;
		
		public ArrayIndexBasedComparator(Supplier<T[]> supplier) {
			this.supplier = Objects.requireNonNull(supplier);
		}
		
		@Override
		public Integer apply(T a, T b) {
			T[] array = supplier.get();
			return Integer.compare(Utils.indexOf(a, array), Utils.indexOf(b, array));
		}
	}
	
	private static class PriorityComparator<T, V> implements Comparator<T> {
		
		protected final Map<V, Integer> priority;
		protected final boolean priorityReversed;
		protected final Function<T, V> mapper;
		protected final boolean naturalReversed;
		protected final BiFunction<V, V, Integer> naturalComparator;
		
		protected PriorityComparator(List<V> priority, boolean priorityReversed, Function<T, V> mapper,
				boolean naturalReversed, BiFunction<V, V, Integer> naturalComparator) {
			this.priority = priorityMap(Objects.requireNonNull(Utils.nonNullContent(priority)));
			this.priorityReversed = priorityReversed;
			this.mapper = Objects.requireNonNull(mapper);
			this.naturalReversed = naturalReversed;
			this.naturalComparator = Objects.requireNonNull(naturalComparator);
		}
		
		protected static final <T> Map<T, Integer> priorityMap(List<T> priority) {
			Map<T, Integer> map = new HashMap<>();
			for(int i = 0, l = priority.size(); i < l; ++i)
				map.put(priority.get(i), i);
			return map;
		}
		
		protected V map(T t) {
			return mapper.apply(t);
		}
		
		protected int cmp(V a, V b) {
			int pa = priority.getOrDefault(a, Integer.MAX_VALUE);
			int pb = priority.getOrDefault(b, Integer.MAX_VALUE);
			return pa == pb
						? (naturalReversed
								? naturalComparator.apply(b, a)
								: naturalComparator.apply(a, b))
						: (priorityReversed
								? Integer.compare(pb, pa)
								: Integer.compare(pa, pb));
		}
		
		@Override
		public int compare(T a, T b) {
			return a == b ? 0 : (a == null ? 1 : (b == null ? -1 : cmp(map(a), map(b))));
		}
	}
	
	/** @since 00.02.09 */
	private static class PriorityComparatorOfComparable<T, V extends Comparable<V>> extends PriorityComparator<T, V> {
		
		protected final V preferred;
		
		protected PriorityComparatorOfComparable(List<V> priority, boolean priorityReversed, Function<T, V> mapper,
				boolean naturalReversed, BiFunction<V, V, Integer> naturalComparator, V preferred) {
			super(priority, priorityReversed, mapper, naturalReversed, naturalComparator);
			this.preferred = preferred;
		}
		
		protected static final int normalize(int cmp) {
			return cmp == 0 ? 0 : cmp > 0 ? +1 : -1;
		}
		
		@Override
		protected int cmp(V a, V b) {
			if(preferred == null) {
				return super.cmp(a, b);
			}
			
			// The final list after all comparisons will look like this:
			// [ ( PREFERRED ), ( < PREFERRED ), ( > PREFERRED ) ]
			// where ( G ) is a group G of items, where for each pair U, V from G
			// cmp(U, P) == cmp(V, P) is true and items in each group G are sorted
			// using whichever comparison is specified in the super class.
			int ca = normalize(a.compareTo(preferred));
			int cb = normalize(b.compareTo(preferred));
			
			if(ca == cb) return super.cmp(a, b);
			if(ca ==  0) return -1; // b is not preferred, but a is
			if(cb ==  0) return +1; // a is not preferred, but b is
			if(ca  <  0) return -1; // a is lower than preferred, b is not
			else         return +1; // b is lower than preferred, a is not
		}
	}
	
	private static final class AudioLanguageFilter implements OptPredicate<Media> {
		
		private final MediaLanguage language;
		
		public AudioLanguageFilter(MediaLanguage language) {
			this.language = Objects.requireNonNull(language);
		}
		
		@Override
		public boolean test(Media value) {
			return Media.findAllOfType(value, MediaType.AUDIO).stream()
					    .anyMatch((a) -> ((AudioMediaBase) a).language().is(language));
		}
	}
}