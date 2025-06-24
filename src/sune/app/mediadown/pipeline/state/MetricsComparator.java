package sune.app.mediadown.pipeline.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** @since 00.02.09 */
public interface MetricsComparator {
	
	boolean compare(Metrics a, Metrics b);
	
	static boolean compareWithRegistered(Metrics a, Metrics b) {
		return Registry.compare(a, b);
	}
	
	static class Registry {
		
		private static final List<MetricsComparator> metricsComparators = new ArrayList<>();
		
		static {
			metricsComparators.add(new NullTransitionMetricsComparator());
			metricsComparators.add(new DifferentTypeMetricsComparator());
		}
		
		private static final boolean compare(Metrics a, Metrics b) {
			if(a == null && b == null) {
				return false; // Refuse to compare
			}
			
			for(MetricsComparator comparator : metricsComparators) {
				if(comparator.compare(a, b)) {
					return true;
				}
			}
			
			return false;
		}
		
		public static void register(MetricsComparator comparator) {
			metricsComparators.add(Objects.requireNonNull(comparator));
		}
		
		public static void unregister(MetricsComparator comparator) {
			metricsComparators.remove(Objects.requireNonNull(comparator));
		}
		
		private static final class NullTransitionMetricsComparator implements MetricsComparator {
			
			@Override
			public boolean compare(Metrics a, Metrics b) {
				// null -> non-null OR non-null -> null
				return (a == null) ^ (b == null);
			}
		}
		
		private static final class DifferentTypeMetricsComparator implements MetricsComparator {
			
			@Override
			public boolean compare(Metrics a, Metrics b) {
				return !Objects.equals(a.type(), b.type()); // Type may be null
			}
		}
	}
}
