package amidst.mojangapi.world.versionfeatures;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import amidst.documentation.Immutable;
import amidst.documentation.ThreadSafe;
import amidst.mojangapi.minecraftinterface.RecognisedVersion;

@Immutable
@ThreadSafe
public class VersionFeatures {
	private final RecognisedVersion version;
	private final Map<FeatureKey<?>, Entry<?>> featureEntries;

	private VersionFeatures(RecognisedVersion version, Map<FeatureKey<?>, Entry<?>> featureEntries) {
		this.version = version;
		this.featureEntries = featureEntries;
	}

	public<T> T get(FeatureKey<T> key) {
		if (!featureEntries.containsKey(key)) {
			throw new IllegalArgumentException("unknown feature " + key);
		}
		@SuppressWarnings("unchecked")
		Entry<T> entry = (Entry<T>) featureEntries.get(key);
		return entry.getOrCompute(this);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder implements Cloneable {
		private final Map<FeatureKey<?>, Entry<?>> features;

		private Builder() {
			features = new IdentityHashMap<>();
		}

		private Builder(Builder builder) {
			features = new IdentityHashMap<>(builder.features);
		}

		private<T> Builder addEntry(FeatureKey<T> key, Entry<T> entry) {
			Objects.requireNonNull(key);
			if (features.containsKey(key)) {
				throw new IllegalArgumentException("the feature " + key + " was already set");
			}
			features.put(key, entry);
			return this;
		}

		public<T> Builder with(FeatureKey<T> key, VersionFeature<T> feature) {
			return addEntry(key, new Entry<>(Objects.requireNonNull(feature)));
		}

		public<T> Builder withValue(FeatureKey<T> key, T value) {
			return addEntry(key, new Entry<>(Objects.requireNonNull(value)));
		}

		@Override
		public Builder clone() {
			return new Builder(this);
		}

		public VersionFeatures create(RecognisedVersion version) {
			return new VersionFeatures(
				version,
				features.entrySet().stream().collect(Collectors.toMap(
					e -> e.getKey(),
					e -> new Entry<>(e.getValue()),
					(v1, v2) -> {
						throw new IllegalStateException("duplicate key");
					},
					IdentityHashMap::new
				))
			);
		}
	}

	private static class Entry<T> {

		// Three possible states :
		// - value == null, feature != null: the value isn't computed yet
		// - value == null, feature == null: the value is currently being computed
		// - value != null, feature == null: the value is computed
		private T value;
		private VersionFeature<T> feature;

		public Entry(VersionFeature<T> feature) {
			this.value = null;
			this.feature = feature;
		}

		public Entry(T value) {
			this.value = value;
			this.feature = null;
		}

		public Entry(Entry<T> other) {
			this.value = other.value;
			this.feature = other.feature;
		}

		public synchronized T getOrCompute(VersionFeatures features) {
			if (value != null) {
				return value;
			}

			VersionFeature<T> tmpFeature = feature;
			if (tmpFeature == null) {
				throw new IllegalStateException("Circular feature loading");
			}

			feature = null;
			value = tmpFeature.getValue(features.version, features);
			if (value == null) {
				feature = tmpFeature;
				throw new NullPointerException("Feature value can't be null");
			}
			return value;
		}
	}
}
