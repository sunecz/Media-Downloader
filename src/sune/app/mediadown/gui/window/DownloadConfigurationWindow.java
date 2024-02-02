package sune.app.mediadown.gui.window;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.gui.CustomChoiceDialog;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.media.MediaFilter;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaLanguage;
import sune.app.mediadown.media.MediaQuality;
import sune.app.mediadown.media.MediaType;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.Opt;
import sune.app.mediadown.util.Utils;

public class DownloadConfigurationWindow extends DraggableWindow<BorderPane> {
	
	public static final String NAME = "download_config";
	
	/** @since 00.02.05 */
	private final Map<Feature, Object> featureResults = new HashMap<>();
	private DownloadConfiguration result;
	
	private GridPane grid;
	private HBox boxBottom;
	private Button btnOK;
	private Button btnCancel;
	
	public DownloadConfigurationWindow() {
		super(NAME, new BorderPane(), 400.0, 150.0);
		initModality(Modality.APPLICATION_MODAL);
		
		grid = new GridPane();
		
		boxBottom = new HBox(5);
		btnOK = new Button(translation.getSingle("button.ok"));
		btnCancel = new Button(translation.getSingle("button.cancel"));
		
		btnOK.setOnAction((e) -> createResultAndClose());
		btnCancel.setOnAction((e) -> close());
		
		btnOK.setMinWidth(80);
		btnCancel.setMinWidth(80);
		boxBottom.setPadding(new Insets(10, 0, 0, 0));
		boxBottom.getChildren().addAll(btnOK, btnCancel);
		boxBottom.setAlignment(Pos.CENTER_RIGHT);
		
		grid.setVgap(5);
		grid.setHgap(5);
		
		content.setCenter(grid);
		content.setBottom(boxBottom);
		content.setPadding(new Insets(10));
		
		FXUtils.onWindowShow(this, () -> {
			Stage parent = (Stage) args.get("parent");
			
			if(parent != null) {
				centerWindow(parent);
			}
		});
		
		centerWindowAfterLayoutUpdate();
	}
	
	/** @since 00.02.05 */
	protected final GridPane grid() {
		return grid;
	}
	
	/** @since 00.02.05 */
	private final void featureResult(Feature feature, Object result) {
		featureResults.put(Objects.requireNonNull(feature), result);
	}
	
	/** @since 00.02.05 */
	@SuppressWarnings("unchecked")
	private final <T> T featureResult(Feature feature) {
		return (T) featureResults.get(feature);
	}
	
	/** @since 00.02.05 */
	private final void clearResult() {
		featureResults.clear();
		result = null;
	}
	
	/** @since 00.02.05 */
	private final void createResult() {
		MediaFilter.Builder mediaFilter = MediaFilter.builder();
		
		boolean bestQuality = Opt.<Boolean>of(featureResult(Feature.BEST_QUALITY)).ifTrue(Objects::nonNull).orElse(false);
		mediaFilter.bestQuality(bestQuality);
		
		MediaQuality preferredQuality;
		if((preferredQuality = featureResult(Feature.PREFERRED_QUALITY)) != null) {
			mediaFilter.preferredQuality(preferredQuality)
			           .qualityPriorityReversed(false, true);
		}
		
		List<MediaFormat> formatsPriority;
		if((formatsPriority = featureResult(Feature.FORMATS_PRIORITY)) != null) {
			mediaFilter.formatPriority(formatsPriority);
		}
		
		MediaLanguage audioLanguage;
		if((audioLanguage = featureResult(Feature.AUDIO_LANGUAGE)) != null) {
			mediaFilter.audioLanguage(audioLanguage);
		}
		
		List<MediaLanguage> subtitlesLanguages;
		if((subtitlesLanguages = featureResult(Feature.SUBTITLES_LANGUAGE)) == null) {
			subtitlesLanguages = List.of();
		}
		
		MediaFormat outputFormat;
		if((outputFormat = featureResult(Feature.OUTPUT_FORMAT)) == null) {
			outputFormat = MediaFormat.outputFormats()[0];
		}
		
		result = new DownloadConfiguration(mediaFilter.build(), outputFormat, subtitlesLanguages);
	}
	
	/** @since 00.02.05 */
	private final void createResultAndClose() {
		createResult();
		close();
	}
	
	/** @since 00.02.05 */
	private final void initWithValuesBeforeShow(Stage parent, Set<FeatureValue> featureValues) {
		clearResult();
		grid.getChildren().clear();
		
		int row = 0;
		for(FeatureValue fv : Utils.iterable(featureValues.stream()
	            .sorted((a, b) -> Integer.compare(a.feature().ordinal(), b.feature().ordinal()))
	            .iterator())) {
			row = fv.feature().addTo(this, row, fv.value());
		}
	}
	
	/** @since 00.02.05 */
	private final void initBeforeShow(Stage parent, Set<Feature> features) {
		initWithValuesBeforeShow(parent, features.stream().map(Feature::defaultValue).collect(Collectors.toSet()));
	}
	
	/** @since 00.02.05 */
	public final void showWithFeatures(Stage parent, Set<Feature> features) {
		initBeforeShow(parent, features);
		show(parent);
	}
	
	/** @since 00.02.05 */
	public final void showWithFeatures(Stage parent, Feature... features) {
		showWithFeatures(parent, Set.of(features));
	}
	
	/** @since 00.02.05 */
	public final void showWithFeaturesAndWait(Stage parent, Set<Feature> features) {
		initBeforeShow(parent, features);
		showAndWait(parent);
	}
	
	/** @since 00.02.05 */
	public final void showWithFeaturesAndWait(Stage parent, Feature... features) {
		showWithFeaturesAndWait(parent, Set.of(features));
	}
	
	/** @since 00.02.05 */
	public final void showWithFeatureValues(Stage parent, Set<FeatureValue> featureValue) {
		initWithValuesBeforeShow(parent, featureValue);
		show(parent);
	}
	
	/** @since 00.02.05 */
	public final void showWithFeatureValues(Stage parent, FeatureValue... featureValue) {
		showWithFeatureValues(parent, Set.of(featureValue));
	}
	
	/** @since 00.02.05 */
	public final void showWithFeatureValuesAndWait(Stage parent, Set<FeatureValue> featureValue) {
		initWithValuesBeforeShow(parent, featureValue);
		showAndWait(parent);
	}
	
	/** @since 00.02.05 */
	public final void showWithFeatureValuesAndWait(Stage parent, FeatureValue... featureValue) {
		showWithFeatureValuesAndWait(parent, Set.of(featureValue));
	}
	
	/** @since 00.02.05 */
	public DownloadConfiguration result() {
		return result;
	}
	
	/** @since 00.02.05 */
	public static final class DownloadConfiguration {
		
		private static DownloadConfiguration DEFAULT;
		
		private final MediaFilter mediaFilter;
		private final MediaFormat outputFormat;
		private final List<MediaLanguage> subtitlesLanguages;
		
		private DownloadConfiguration(MediaFilter mediaFilter, MediaFormat outputFormat,
				List<MediaLanguage> subtitlesLanguages) {
			this.mediaFilter = Objects.requireNonNull(mediaFilter);
			this.outputFormat = Objects.requireNonNull(outputFormat);
			this.subtitlesLanguages = Objects.requireNonNull(subtitlesLanguages);
		}
		
		private static final DownloadConfiguration createDefault() {
			return new DownloadConfiguration(MediaFilter.none(), MediaFormat.UNKNOWN, List.of());
		}
		
		public static final DownloadConfiguration ofDefault() {
			return DEFAULT == null ? (DEFAULT = createDefault()) : DEFAULT;
		}
		
		public MediaFilter mediaFilter() {
			return mediaFilter;
		}
		
		public MediaFormat outputFormat() {
			return outputFormat;
		}
		
		public List<MediaLanguage> subtitlesLanguages() {
			return subtitlesLanguages;
		}
	}
	
	/** @since 00.02.05 */
	public static enum Feature {
		
		BEST_QUALITY {
			
			@Override
			protected int addTo(DownloadConfigurationWindow window, int row, Object value) {
				Translation translation = window.getTranslation();
				CheckBox chbSelect = new CheckBox(translation.getSingle("label.best_quality"));
				chbSelect.setId("checkbox-best-quality");
				
				chbSelect.selectedProperty().addListener((o, ov, isSelected) -> {
					window.featureResult(this, isSelected);
				});
				chbSelect.setSelected(true);
				
				window.grid().getChildren().addAll(chbSelect);
				GridPane.setConstraints(chbSelect, 0, row, 2, 1);
				return row + 1;
			}
		},
		PREFERRED_QUALITY {
			
			@Override
			protected int addTo(DownloadConfigurationWindow window, int row, Object value) {
				Translation translation = window.getTranslation();
				Label lblPreferredQuality = new Label(translation.getSingle("label.preferred_quality"));
				ComboBox<MediaQuality> cmbPreferredQuality = new ComboBox<>();
				
				Stream<MediaQuality> stream;
				if(value instanceof Set) {
					@SuppressWarnings("unchecked")
					Set<MediaQuality> found = (Set<MediaQuality>) value;
					stream = found.stream();
				} else {
					stream = Stream.of(MediaQuality.validQualities())
							       .filter((q) -> q.mediaType().isAnyOf(MediaType.VIDEO, MediaType.UNKNOWN));
				}
				
				Set<MediaQuality> qualities = Stream.concat(
					Stream.of(MediaQuality.UNKNOWN),
					stream
				).collect(Collectors.toSet());
				
				MediaQuality[] refAll = MediaQuality.values();
				MediaQuality[] sorted = qualities.stream()
						.sorted((a, b) -> Utils.compareIndex(a, b, refAll))
						.toArray(MediaQuality[]::new);
				
				cmbPreferredQuality.getItems().setAll(sorted);
				cmbPreferredQuality.setConverter(new MediaQualityStringConverter(qualities, translation));
				cmbPreferredQuality.getSelectionModel().selectedItemProperty().addListener((o, ov, quality) -> {
					CheckBox chbBestQuality = (CheckBox) window.grid().lookup("#checkbox-best-quality");
					
					if(chbBestQuality != null) {
						boolean shouldBeSelected = quality.is(MediaQuality.UNKNOWN);
						chbBestQuality.setSelected(shouldBeSelected);
					}
					
					window.featureResult(this, quality);
				});
				cmbPreferredQuality.getSelectionModel().select(0);
				
				cmbPreferredQuality.setMaxWidth(Double.MAX_VALUE);
				GridPane.setHgrow(cmbPreferredQuality, Priority.ALWAYS);
				
				window.grid().getChildren().addAll(lblPreferredQuality, cmbPreferredQuality);
				GridPane.setConstraints(lblPreferredQuality, 0, row);
				GridPane.setConstraints(cmbPreferredQuality, 1, row);
				return row + 1;
			}
			
			@Override
			public PreferredQualityFeatureValueFactory valueFactory() {
				return new PreferredQualityFeatureValueFactory(this);
			}
		},
		FORMATS_PRIORITY {
			
			@Override
			protected int addTo(DownloadConfigurationWindow window, int row, Object value) {
				Translation translation = window.getTranslation();
				Label lblFormatsPriority = new Label(translation.getSingle("label.formats_priority"));
				
				Stream<MediaFormat> stream;
				if(value instanceof Set) {
					@SuppressWarnings("unchecked")
					Set<MediaFormat> found = (Set<MediaFormat>) value;
					stream = found.stream();
				} else {
					stream = Stream.of(MediaFormat.values())
				                   .filter((f) -> f.mediaType().isAnyOf(MediaType.VIDEO,
				                                                        MediaType.AUDIO,
				                                                        MediaType.UNKNOWN));
				}
				
				Set<MediaFormat> formats = stream.collect(Collectors.toSet());
				
				MediaFormat[] refAll = MediaFormat.values();
				MediaFormat[] sorted = formats.stream()
						.sorted((a, b) -> Utils.compareIndex(a, b, refAll))
						.toArray(MediaFormat[]::new);
				
				ValueList<MediaFormat> listFormatsPriority = new ValueList<>(window, true, translation, null,
						() -> sorted, (v) -> v[0]);
				
				listFormatsPriority.view().getItems().addListener((ListChangeListener.Change<? extends MediaFormat> change) -> {
					if(!change.next()) return;
					window.featureResult(this, List.copyOf(change.getList()));
				});
				
				listFormatsPriority.setPrefHeight(120.0);
				GridPane.setHgrow(listFormatsPriority, Priority.ALWAYS);
				
				window.grid().getChildren().addAll(lblFormatsPriority, listFormatsPriority);
				GridPane.setConstraints(lblFormatsPriority, 0, row, 2, 1);
				GridPane.setConstraints(listFormatsPriority, 0, row + 1, 2, 1);
				return row + 2;
			}
			
			@Override
			public FormatsPriorityFeatureValueFactory valueFactory() {
				return new FormatsPriorityFeatureValueFactory(this);
			}
		},
		AUDIO_LANGUAGE {
			
			@Override
			protected int addTo(DownloadConfigurationWindow window, int row, Object value) {
				Translation translation = window.getTranslation();
				Label lblAudioLanguage = new Label(translation.getSingle("label.audio_language"));
				ComboBox<MediaLanguage> cmbAudioLanguage = new ComboBox<>();
				
				Stream<MediaLanguage> stream;
				if(value instanceof Set) {
					@SuppressWarnings("unchecked")
					Set<MediaLanguage> found = (Set<MediaLanguage>) value;
					stream = found.stream();
				} else {
					stream = Stream.of(MediaLanguage.determinedLanguages());
				}
				
				Set<MediaLanguage> languages = Stream.concat(
					Stream.of(MediaLanguage.UNKNOWN),
					stream
				).collect(Collectors.toSet());
				
				MediaLanguage[] refAll = MediaLanguage.values();
				MediaLanguage[] sorted = languages.stream()
						.sorted((a, b) -> Utils.compareIndex(a, b, refAll))
						.toArray(MediaLanguage[]::new);
				
				cmbAudioLanguage.getItems().setAll(sorted);
				cmbAudioLanguage.setConverter(new MediaLanguageStringConverter(languages, translation));
				cmbAudioLanguage.getSelectionModel().selectedItemProperty().addListener((o, ov, quality) -> {
					window.featureResult(this, quality);
				});
				cmbAudioLanguage.getSelectionModel().select(0);
				
				cmbAudioLanguage.setMaxWidth(Double.MAX_VALUE);
				GridPane.setHgrow(cmbAudioLanguage, Priority.ALWAYS);
				
				window.grid().getChildren().addAll(lblAudioLanguage, cmbAudioLanguage);
				GridPane.setConstraints(lblAudioLanguage, 0, row);
				GridPane.setConstraints(cmbAudioLanguage, 1, row);
				return row + 1;
			}
			
			@Override
			public AudioLanguageFeatureValueFactory valueFactory() {
				return new AudioLanguageFeatureValueFactory(this);
			}
		},
		SUBTITLES_LANGUAGE {
			
			@Override
			protected int addTo(DownloadConfigurationWindow window, int row, Object value) {
				Translation translation = window.getTranslation();
				Translation languagesTranslation = MediaDownloader.translation().getTranslation("media.language");
				Label lblSubtitlesLanguage = new Label(translation.getSingle("label.subtitles_language"));
				
				Stream<MediaLanguage> stream;
				if(value instanceof Set) {
					@SuppressWarnings("unchecked")
					Set<MediaLanguage> found = (Set<MediaLanguage>) value;
					stream = found.stream();
				} else {
					stream = Stream.of(MediaLanguage.determinedLanguages());
				}
				
				Set<MediaLanguage> languages = stream.collect(Collectors.toSet());
				
				MediaLanguage[] refAll = MediaLanguage.values();
				MediaLanguage[] sorted = languages.stream()
						.sorted((a, b) -> Utils.compareIndex(a, b, refAll))
						.toArray(MediaLanguage[]::new);
				
				ValueList<MediaLanguage> listSubtitlesLanguage = new ValueList<>(window, false, translation,
						languagesTranslation, () -> sorted, (v) -> v[0]);
				
				listSubtitlesLanguage.view().getItems().addListener((ListChangeListener.Change<? extends MediaLanguage> change) -> {
					if(!change.next()) return;
					window.featureResult(this, List.copyOf(change.getList()));
				});
				listSubtitlesLanguage.view().setCellFactory((view) -> new TranslatableListCell<>(languagesTranslation));
				
				listSubtitlesLanguage.setPrefHeight(120.0);
				GridPane.setHgrow(listSubtitlesLanguage, Priority.ALWAYS);
				
				window.grid().getChildren().addAll(lblSubtitlesLanguage, listSubtitlesLanguage);
				GridPane.setConstraints(lblSubtitlesLanguage, 0, row, 2, 1);
				GridPane.setConstraints(listSubtitlesLanguage, 0, row + 1, 2, 1);
				return row + 2;
			}
			
			@Override
			public SubtitlesLanguageFeatureValueFactory valueFactory() {
				return new SubtitlesLanguageFeatureValueFactory(this);
			}
		},
		OUTPUT_FORMAT {
			
			@Override
			protected int addTo(DownloadConfigurationWindow window, int row, Object value) {
				Translation translation = window.getTranslation();
				Label lblOutputFormat = new Label(translation.getSingle("label.output_format"));
				ComboBox<MediaFormat> cmbOutputFormat = new ComboBox<>();
				
				cmbOutputFormat.getItems().setAll(MediaFormat.outputFormats());
				cmbOutputFormat.getSelectionModel().selectedItemProperty().addListener((o, ov, format) -> {
					window.featureResult(this, format);
				});
				cmbOutputFormat.getSelectionModel().select(0);
				
				cmbOutputFormat.setMaxWidth(Double.MAX_VALUE);
				GridPane.setHgrow(cmbOutputFormat, Priority.ALWAYS);
				
				window.grid().getChildren().addAll(lblOutputFormat, cmbOutputFormat);
				GridPane.setConstraints(lblOutputFormat, 0, row);
				GridPane.setConstraints(cmbOutputFormat, 1, row);
				return row + 1;
			}
		};
		
		public static final Set<Feature> ALL_FEATURES = Set.of(values());
		
		private Feature() {
			// Do nothing
		}
		
		protected abstract int addTo(DownloadConfigurationWindow window, int row, Object value);
		
		// Default implementation, may be overriden if needed
		public FeatureValueFactory valueFactory() {
			return new NullFeatureValueFactory(this);
		}
		
		// Default implementation, may be overriden if needed
		public FeatureValue defaultValue() {
			return valueFactory().create();
		}
		
		private static final class NullFeatureValueFactory extends FeatureValueFactory {
			
			NullFeatureValueFactory(Feature feature) {
				super(feature);
			}
			
			@Override
			public FeatureValue create() {
				return new FeatureValue(feature, null);
			}
		}
		
		public static final class PreferredQualityFeatureValueFactory extends SetOfValuesFeatureValueFactory<MediaQuality> {
			
			PreferredQualityFeatureValueFactory(Feature feature) {
				super(feature);
			}
			
			public PreferredQualityFeatureValueFactory qualities(Set<MediaQuality> qualities) {
				return (PreferredQualityFeatureValueFactory) values(qualities);
			}
			
			public PreferredQualityFeatureValueFactory qualities(MediaQuality... qualities) {
				return (PreferredQualityFeatureValueFactory) values(qualities);
			}
			
			@Override
			public Value create() {
				return new Value(feature, values);
			}
			
			public static final class Value extends SetOfValuesFeatureValueFactory.Value<MediaQuality> {
				
				private Value(Feature feature, Set<MediaQuality> value) {
					super(feature, value);
				}
			}
		}
		
		public static final class FormatsPriorityFeatureValueFactory extends SetOfValuesFeatureValueFactory<MediaFormat> {
			
			FormatsPriorityFeatureValueFactory(Feature feature) {
				super(feature);
			}
			
			public FormatsPriorityFeatureValueFactory formats(Set<MediaFormat> formats) {
				return (FormatsPriorityFeatureValueFactory) values(formats);
			}
			
			public FormatsPriorityFeatureValueFactory formats(MediaFormat... formats) {
				return (FormatsPriorityFeatureValueFactory) values(formats);
			}
			
			@Override
			public Value create() {
				return new Value(feature, values);
			}
			
			public static final class Value extends SetOfValuesFeatureValueFactory.Value<MediaFormat> {
				
				private Value(Feature feature, Set<MediaFormat> value) {
					super(feature, value);
				}
			}
		}
		
		public static final class AudioLanguageFeatureValueFactory extends SetOfValuesFeatureValueFactory<MediaLanguage> {
			
			AudioLanguageFeatureValueFactory(Feature feature) {
				super(feature);
			}
			
			public AudioLanguageFeatureValueFactory languages(Set<MediaLanguage> languages) {
				return (AudioLanguageFeatureValueFactory) values(languages);
			}
			
			public AudioLanguageFeatureValueFactory languages(MediaLanguage... languages) {
				return (AudioLanguageFeatureValueFactory) values(languages);
			}
			
			@Override
			public Value create() {
				return new Value(feature, values);
			}
			
			public static final class Value extends SetOfValuesFeatureValueFactory.Value<MediaLanguage> {
				
				private Value(Feature feature, Set<MediaLanguage> value) {
					super(feature, value);
				}
			}
		}
		
		public static final class SubtitlesLanguageFeatureValueFactory extends SetOfValuesFeatureValueFactory<MediaLanguage> {
			
			SubtitlesLanguageFeatureValueFactory(Feature feature) {
				super(feature);
			}
			
			public SubtitlesLanguageFeatureValueFactory languages(Set<MediaLanguage> languages) {
				return (SubtitlesLanguageFeatureValueFactory) values(languages);
			}
			
			public SubtitlesLanguageFeatureValueFactory languages(MediaLanguage... languages) {
				return (SubtitlesLanguageFeatureValueFactory) values(languages);
			}
			
			@Override
			public Value create() {
				return new Value(feature, values);
			}
			
			public static final class Value extends SetOfValuesFeatureValueFactory.Value<MediaLanguage> {
				
				private Value(Feature feature, Set<MediaLanguage> value) {
					super(feature, value);
				}
			}
		}
		
		protected static class SetOfValuesFeatureValueFactory<T> extends FeatureValueFactory {
			
			protected Set<T> values = Set.of();
			
			protected SetOfValuesFeatureValueFactory(Feature feature) {
				super(feature);
			}
			
			protected final SetOfValuesFeatureValueFactory<T> values(Set<T> values) {
				this.values = Objects.requireNonNull(values);
				return this;
			}
			
			@SafeVarargs
			protected final SetOfValuesFeatureValueFactory<T> values(T... values) {
				return values(Set.of(values));
			}
			
			@Override
			public Value<T> create() {
				return new Value<>(feature, values);
			}
			
			protected static class Value<T> extends FeatureValue {
				
				protected Value(Feature feature, Set<T> value) {
					super(feature, value);
				}
				
				@Override
				public Set<T> value() {
					return valueCasted();
				}
			}
		}
	}
	
	public static abstract class FeatureValueFactory {
		
		protected final Feature feature;
		
		protected FeatureValueFactory(Feature feature) {
			this.feature = Objects.requireNonNull(feature);
		}
		
		public abstract FeatureValue create();
		
		public Feature feature() {
			return feature;
		}
	}
	
	public static class FeatureValue {
		
		protected final Feature feature;
		protected final Object value;
		
		FeatureValue(Feature feature, Object value) {
			this.feature = Objects.requireNonNull(feature);
			this.value = value;
		}
		
		public Feature feature() {
			return feature;
		}
		
		public Object value() {
			return value;
		}
		
		public <T> T valueCasted() {
			@SuppressWarnings("unchecked")
			T casted = (T) value;
			return casted;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(feature); // Do not hash value
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			FeatureValue other = (FeatureValue) obj;
			return feature == other.feature; // Do not compare value
		}
	}
	
	/** @since 00.02.05 */
	private static final class MediaQualityStringConverter extends StringConverter<MediaQuality> {
		
		private final Translation qualitiesTranslation;
		private final String textNoQuality;
		private final Map<String, MediaQuality> mapping;
		
		private MediaQualityStringConverter(Set<MediaQuality> allQualities, Translation translation) {
			qualitiesTranslation = MediaDownloader.translation().getTranslation("media.quality");
			textNoQuality = translation.getSingle("etc.no_quality");
			mapping = allQualities.stream().collect(Collectors.toMap((q) -> mapToString(q), Function.identity()));
		}
		
		private final String mapToString(MediaQuality quality) {
			return qualitiesTranslation.getSingle(quality.toString());
		}
		
		private final MediaQuality mapToQuality(String string) {
			return mapping.get(string);
		}
		
		@Override
		public String toString(MediaQuality quality) {
			return quality.is(MediaQuality.UNKNOWN) ? textNoQuality : mapToString(quality);
		}
		
		@Override
		public MediaQuality fromString(String string) {
			return string.equals(textNoQuality) ? MediaQuality.UNKNOWN : mapToQuality(string);
		}
	}
	
	/** @since 00.02.05 */
	private static final class MediaLanguageStringConverter extends StringConverter<MediaLanguage> {
		
		private final Translation languagesTranslation;
		private final String textNoLanguage;
		private final Map<String, MediaLanguage> mapping;
		
		private MediaLanguageStringConverter(Set<MediaLanguage> allLanguages, Translation translation) {
			languagesTranslation = MediaDownloader.translation().getTranslation("media.language");
			textNoLanguage = translation.getSingle("etc.no_language");
			mapping = allLanguages.stream().collect(Collectors.toMap((q) -> mapToString(q), Function.identity()));
		}
		
		private final String mapToString(MediaLanguage language) {
			String string = language.toString();
			return languagesTranslation.hasSingle(string)
						? languagesTranslation.getSingle(string)
						: Utils.titlize(string);
		}
		
		private final MediaLanguage mapToLanguage(String string) {
			return mapping.get(string);
		}
		
		@Override
		public String toString(MediaLanguage language) {
			return language.is(MediaLanguage.UNKNOWN) ? textNoLanguage : mapToString(language);
		}
		
		@Override
		public MediaLanguage fromString(String string) {
			return string.equals(textNoLanguage) ? MediaLanguage.UNKNOWN : mapToLanguage(string);
		}
	}
	
	/** @since 00.02.05 */
	private static final class TranslatableListCell<T> extends ListCell<T> {
		
		private final Translation translation;
		
		public TranslatableListCell(Translation translation) {
			this.translation = Objects.requireNonNull(translation);
		}
		
		@Override
		protected void updateItem(T item, boolean empty) {
			super.updateItem(item, empty);
			
			if(empty || item == null) {
				setText(null);
			} else {
				String string = item.toString();
				setText(translation.hasSingle(string)
							? translation.getSingle(string)
							: Utils.titlize(string));
			}
		}
	}
	
	/** @since 00.02.05 */
	private static final class TranslatableTitlizableStringConverter<T> extends StringConverter<T> {
		
		private final Translation translation;
		private final Map<String, T> mapping;
		
		private TranslatableTitlizableStringConverter(Translation translation, T[] values) {
			this.translation = translation;
			this.mapping = Stream.of(values).collect(Collectors.toMap((q) -> toString(q), Function.identity()));
		}
		
		@Override
		public String toString(T value) {
			String string = value.toString();
			return translation.hasSingle(string) ? translation.getSingle(string) : Utils.titlize(string);
		}
		
		@Override
		public T fromString(String string) {
			return mapping.get(string);
		}
	}
	
	/** @since 00.02.05 */
	private static final class ValueList<T> extends HBox {
		
		private final ListView<T> listView;
		private final Set<T> set = new HashSet<>();
		
		public ValueList(DownloadConfigurationWindow parent, boolean ordered, Translation translation,
				Translation valuesTranslation, Supplier<T[]> supplierValues, Function<T[], T> obtainerDefaultValue) {
			super(5);
			
			listView = new ListView<>();
			VBox boxButtons = new VBox(5);
			Button btnAdd = new Button(translation.getSingle("button.add"));
			Button btnRemove = new Button(translation.getSingle("button.remove"));

			btnAdd.setOnAction((e) -> {
				String title = translation.getSingle("etc.window_select.title");
				T[] allValues = supplierValues.get();
				T selected = showSelectionWindow(parent, valuesTranslation, title,
					obtainerDefaultValue.apply(allValues), allValues);
				if(selected != null && !set.contains(selected)) {
					listView.getItems().add(selected);
					set.add(selected);
				}
			});
			btnRemove.setOnAction((e) -> {
				for(int index : Utils.iterable(listView.getSelectionModel().getSelectedIndices().stream()
						.sorted(Comparator.reverseOrder()).iterator())) {
					T removed = listView.getItems().remove(index);
					set.remove(removed);
				}
			});
			
			boxButtons.getChildren().addAll(btnAdd, btnRemove);
			btnAdd.setMinWidth(80);
			btnRemove.setMinWidth(80);
			
			if(ordered) {
				Button btnUp = new Button(translation.getSingle("button.up"));
				Button btnDown = new Button(translation.getSingle("button.down"));
				
				btnUp.setOnAction((e) -> {
					int index = listView.getSelectionModel().getSelectedIndex();
					if(moveItem(listView.getItems(), index, -1))
						listView.getSelectionModel().clearAndSelect(index - 1);
				});
				btnDown.setOnAction((e) -> {
					int index = listView.getSelectionModel().getSelectedIndex();
					if(moveItem(listView.getItems(), index, 1))
						listView.getSelectionModel().clearAndSelect(index + 1);
				});
				
				boxButtons.getChildren().addAll(btnUp, btnDown);
				btnUp.setMinWidth(80);
				btnDown.setMinWidth(80);
			}
			
			listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
			
			getChildren().addAll(listView, boxButtons);
			HBox.setHgrow(listView, Priority.ALWAYS);
		}
		
		private static final <T> boolean moveItem(List<T> list, int index, int offset) {
			int size = list.size(), src = index, dst = index + offset;
			if(src < 0 || src >= size || dst < 0 || dst >= size) return false;
			Collections.swap(list, index, index + offset);
			return true;
		}
		
		@SafeVarargs
		private static final <T> T showSelectionWindow(DownloadConfigurationWindow parent, Translation translation,
				String title, T defaultChoice, T... choices) {
			StringConverter<T> converter = null;
			if(translation != null) converter = new TranslatableTitlizableStringConverter<>(translation, choices);
			ChoiceDialog<T> dialog = new CustomChoiceDialog<>(converter, defaultChoice, choices);
			dialog.initOwner(parent);
			dialog.setTitle(title);
			dialog.setHeaderText(null);
			dialog.setContentText(null);
			dialog.setGraphic(null);
			FXUtils.setDialogIcon(dialog, MediaDownloader.ICON);
			// Fix sizing and positioning of the dialog window
			FXUtils.onDialogShow(dialog, () -> {
				DialogPane pane = dialog.getDialogPane();
				Region content = (Region) pane.getContent();
				content.setPrefWidth(content.minWidth(-1.0));
				Stage window = (Stage) pane.getScene().getWindow();
				window.sizeToScene();
				FXUtils.centerWindow(window, parent);
			});
			return dialog.showAndWait().orElse(null);
		}
		
		public ListView<T> view() {
			return listView;
		}
	}
}