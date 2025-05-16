package sune.app.mediadown.gui.table;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.gui.GUI;
import sune.app.mediadown.gui.Window;
import sune.app.mediadown.gui.clipboard.ClipboardUtils;
import sune.app.mediadown.gui.util.Choosers;
import sune.app.mediadown.gui.util.ExtensionFilters;
import sune.app.mediadown.gui.util.FXUtils;
import sune.app.mediadown.gui.util.Choosers.SelectedItem;
import sune.app.mediadown.gui.window.DownloadConfigurationWindow;
import sune.app.mediadown.gui.window.DownloadConfigurationWindow.DownloadConfiguration;
import sune.app.mediadown.gui.window.DownloadConfigurationWindow.Feature;
import sune.app.mediadown.gui.window.DownloadConfigurationWindow.Feature.AudioLanguageFeatureValueFactory;
import sune.app.mediadown.gui.window.DownloadConfigurationWindow.Feature.FormatsPriorityFeatureValueFactory;
import sune.app.mediadown.gui.window.DownloadConfigurationWindow.Feature.PreferredQualityFeatureValueFactory;
import sune.app.mediadown.gui.window.DownloadConfigurationWindow.Feature.SubtitlesLanguageFeatureValueFactory;
import sune.app.mediadown.gui.window.DownloadConfigurationWindow.FeatureValue;
import sune.app.mediadown.gui.window.MediaInfoWindow;
import sune.app.mediadown.gui.window.ReportWindow;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.media.AudioMedia;
import sune.app.mediadown.media.AudioMediaBase;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaContainer;
import sune.app.mediadown.media.MediaFilter;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaLanguage;
import sune.app.mediadown.media.MediaQuality;
import sune.app.mediadown.media.MediaType;
import sune.app.mediadown.media.ResolvedMedia;
import sune.app.mediadown.media.SubtitlesMediaBase;
import sune.app.mediadown.report.Report;
import sune.app.mediadown.report.Report.Reason;
import sune.app.mediadown.report.ReportContext;
import sune.app.mediadown.util.Opt;
import sune.app.mediadown.util.Opt.OptCondition;
import sune.app.mediadown.util.Opt.OptMapper;
import sune.app.mediadown.util.Opt.OptPredicate;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;

/** @since 00.02.05 */
public final class TablePipelineUtils {
	
	// Forbid anyone to create an instance of this class
	private TablePipelineUtils() {
	}
	
	private static final ContextMenu newMediaTableContextMenu(TableWindow window, TableView<Media> table,
			Supplier<ReportContext> reportContext) {
		Translation translation = window.getTranslation();
		ContextMenu menu = new ContextMenu();
		
		MenuItem itemCopyURL = new MenuItem(translation.getSingle("tables.media.context_menu.copy_url"));
		itemCopyURL.setOnAction((e) -> {
			ClipboardUtils.copy(
				Utils.toString(table.getSelectionModel().getSelectedItems(), (m) -> m.uri().normalize().toString())
			);
		});
		
		MenuItem itemCopySourceURL = new MenuItem(translation.getSingle("tables.media.context_menu.copy_source_url"));
		itemCopySourceURL.setOnAction((e) -> {
			ClipboardUtils.copy(
				Utils.toString(table.getSelectionModel().getSelectedItems(),
				               (m) -> Objects.toString(Optional.ofNullable(m.metadata().sourceURI())
				                                               .map(URI::normalize).orElse(null)))
			);
		});
		
		MenuItem itemMediaInfo = new MenuItem(translation.getSingle("tables.media.context_menu.media_info"));
		itemMediaInfo.setOnAction((e) -> {
			Media media = table.getSelectionModel().getSelectedItem();
			if(media != null) {
				MediaDownloader.window(MediaInfoWindow.NAME)
					.setArgs("parent", window, "media", media)
					.show();
			}
		});
		
		MenuItem itemReportBroken = new MenuItem(translation.getSingle("tables.media.context_menu.report_broken"));
		itemReportBroken.setOnAction((e) -> {
			Media item = table.getSelectionModel().getSelectedItem();
			GUI.showReportWindow(window, Report.Builders.ofMedia(
				item, Reason.BROKEN,
				reportContext.get()
			), ReportWindow.Feature.onlyReasons(Reason.BROKEN));
		});
		
		menu.getItems().addAll(itemCopyURL, itemCopySourceURL, itemMediaInfo, itemReportBroken);
		menu.showingProperty().addListener((o, ov, isShowing) -> {
			if(!isShowing) return;
			int countItems = table.getSelectionModel().getSelectedItems().size();
			boolean noSelectedItems = countItems == 0;
			boolean notOneSelectedItem = countItems != 1;
			itemCopyURL.setDisable(noSelectedItems);
			itemCopySourceURL.setDisable(noSelectedItems);
			itemMediaInfo.setDisable(notOneSelectedItem);
			itemReportBroken.setDisable(notOneSelectedItem);
		});
		
		return menu;
	}
	
	private static final <T, C extends Media > T mapMedia(Media media, OptPredicate<Media> isTrueSingle,
			OptPredicate<Media> isTrueContainer, Function<C, T> mapperMedia,
			Function<MediaContainer, Media> mapperMediaContainer, Supplier<T> defaultValueSupplier) {
		return Opt.of(media).ifTrue(OptCondition.of(Media::isSingle).andOpt(isTrueSingle))
				  .<Media>or((opt) -> opt.ifTrue(OptCondition.of(Media::isContainer).andOpt(isTrueContainer))
				                         .map(OptMapper.of(Media::asContainer)
				                                       .then(mapperMediaContainer)
				                         ))
				  .<C>cast().map(mapperMedia)
				  .orElseGet(defaultValueSupplier);
	}
	
	private static final <T, C extends Media > T mapMedia(Media media, OptPredicate<Media> isTrueSingle,
			OptPredicate<Media> isTrueContainer, Function<C, T> mapperMedia, Function<MediaContainer,
			Media> mapperMediaContainer, T defaultValue) {
		return mapMedia(media, isTrueSingle, isTrueContainer, mapperMedia, mapperMediaContainer,
		                (Supplier<T>) () -> defaultValue);
	}
	
	private static final MediaFormat videoFormat(Media media) {
		return mapMedia(media, OptCondition.of((m) -> m.type().is(MediaType.VIDEO)), OptCondition::returnTrue,
		                Media::format, MediaContainer::video, MediaFormat.UNKNOWN);
	}
	
	private static final MediaFormat audioFormat(Media media) {
		return mapMedia(media, OptCondition.of((m) -> m.type().is(MediaType.AUDIO)), OptCondition::returnTrue,
		                Media::format, MediaContainer::audio, MediaFormat.UNKNOWN);
	}
	
	private static final MediaQuality videoQuality(Media media) {
		return mapMedia(media, OptCondition.of((m) -> m.type().is(MediaType.VIDEO)), OptCondition::returnTrue,
		                Media::quality, MediaContainer::video, MediaQuality.UNKNOWN);
	}
	
	private static final MediaQuality audioQuality(Media media) {
		return mapMedia(media, OptCondition.of((m) -> m.type().is(MediaType.AUDIO)), OptCondition::returnTrue,
		                Media::quality, MediaContainer::audio, MediaQuality.UNKNOWN);
	}
	
	private static final MediaLanguage audioLanguage(Media media) {
		return mapMedia(media, OptCondition.of((m) -> m.type().is(MediaType.AUDIO)), OptCondition::returnTrue,
		                AudioMedia::language, MediaContainer::audio, MediaLanguage.UNKNOWN);
	}
	
	/** @since 00.02.08 */
	private static final MediaFormat selectedMediaFormat(SelectedItem item) {
		return item.extension().getExtensions().stream()
					.map((s) -> s.replaceFirst("^\\*\\.", ""))
					.map(MediaFormat::fromExtension)
					.filter((f) -> !f.is(MediaFormat.UNKNOWN))
					.findFirst().orElse(MediaFormat.UNKNOWN);
	}
	
	public static final TableView<Media> newMediaTable(TableWindow window, Supplier<ReportContext> reportContext) {
		Translation translation = window.getTranslation();
		Translation translationLanguage = MediaDownloader.translation().getTranslation("media.language");
		Translation translationFormat = MediaDownloader.translation().getTranslation("media.format");
		Translation translationQuality = MediaDownloader.translation().getTranslation("media.quality");
		Translation translationProtected = MediaDownloader.translation().getTranslation("media.protected");
		Function<MediaQuality, String> qualityConverter = ((q) -> q.toString().replaceAll("\\$.*$", ""));
		TableView<Media> table = new TableView<>();
		String titleTitle        = translation.getSingle("tables.media.columns.title");
		String titleFormat       = translation.getSingle("tables.media.columns.format");
		String titleVideoQuality = translation.getSingle("tables.media.columns.video_quality");
		String titleAudioQuality = translation.getSingle("tables.media.columns.audio_quality");
		String titleLanguage     = translation.getSingle("tables.media.columns.language");
		String titleProtected    = translation.getSingle("tables.media.columns.protected");
		String titleVideoFormat  = translation.getSingle("tables.media.columns.video_format");
		String titleAudioFormat  = translation.getSingle("tables.media.columns.audio_format");
		TableColumn<Media, String>        columnTitle        = new TableColumn<>(titleTitle);
		TableColumn<Media, MediaFormat>   columnFormat       = new TranslatableTableColumn<>(titleFormat, translationFormat);
		TableColumn<Media, MediaQuality>  columnVideoQuality = new TranslatableTableColumn<>(titleVideoQuality, translationQuality, qualityConverter);
		TableColumn<Media, MediaQuality>  columnAudioQuality = new TranslatableTableColumn<>(titleAudioQuality, translationQuality, qualityConverter);
		TableColumn<Media, MediaLanguage> columnLanguage     = new TranslatableTableColumn<>(titleLanguage, translationLanguage, true);
		TableColumn<Media, Boolean>       columnProtected    = new TranslatableTableColumn<>(titleProtected, translationProtected);
		TableColumn<Media, MediaFormat>   columnVideoFormat  = new TranslatableTableColumn<>(titleVideoFormat, translationFormat);
		TableColumn<Media, MediaFormat>   columnAudioFormat  = new TranslatableTableColumn<>(titleAudioFormat, translationFormat);
		columnTitle       .setCellValueFactory((c) -> new SimpleStringProperty(c.getValue().metadata().title()));
		columnFormat      .setCellValueFactory((c) -> new SimpleObjectProperty<>(c.getValue().format()));
		columnVideoQuality.setCellValueFactory((c) -> new SimpleObjectProperty<>(videoQuality(c.getValue())));
		columnAudioQuality.setCellValueFactory((c) -> new SimpleObjectProperty<>(audioQuality(c.getValue())));
		columnLanguage    .setCellValueFactory((c) -> new SimpleObjectProperty<>(audioLanguage(c.getValue())));
		columnProtected   .setCellValueFactory((c) -> new SimpleBooleanProperty(c.getValue().metadata().isProtected()));
		columnVideoFormat .setCellValueFactory((c) -> new SimpleObjectProperty<>(videoFormat(c.getValue())));
		columnAudioFormat .setCellValueFactory((c) -> new SimpleObjectProperty<>(audioFormat(c.getValue())));
		columnTitle       .setPrefWidth(150);
		columnFormat      .setPrefWidth(70);
		columnVideoQuality.setPrefWidth(100);
		columnAudioQuality.setPrefWidth(100);
		columnLanguage    .setPrefWidth(70);
		columnProtected   .setPrefWidth(70);
		columnVideoFormat .setPrefWidth(100);
		columnAudioFormat .setPrefWidth(100);
		columnTitle       .setReorderable(false);
		columnFormat      .setReorderable(false);
		columnVideoQuality.setReorderable(false);
		columnAudioQuality.setReorderable(false);
		columnLanguage    .setReorderable(false);
		columnProtected   .setReorderable(false);
		columnVideoFormat .setReorderable(false);
		columnAudioFormat .setReorderable(false);
		table.getColumns().add(columnTitle);
		table.getColumns().add(columnFormat);
		table.getColumns().add(columnVideoQuality);
		table.getColumns().add(columnAudioQuality);
		table.getColumns().add(columnLanguage);
		table.getColumns().add(columnProtected);
		table.getColumns().add(columnVideoFormat);
		table.getColumns().add(columnAudioFormat);
		table.setPlaceholder(new Label(translation.getSingle("tables.media.placeholder")));
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		table.setContextMenu(newMediaTableContextMenu(window, table, reportContext));
		columnVideoQuality.setSortType(SortType.DESCENDING);
		columnAudioQuality.setSortType(SortType.DESCENDING);
		table.getSortOrder().add(columnVideoQuality);
		table.getSortOrder().add(columnAudioQuality);
		return table;
	}
	
	public static final ResolvedMedia resolveSingleMedia(Media media, Path dest, MediaFormat outputFormat,
			MediaLanguage[] subtitlesLanguages) {
		MediaDownloadConfiguration mediaConfig;
		
		if(subtitlesLanguages.length > 0) {
			List<Media> subtitles = Media.findAllOfType(media, MediaType.SUBTITLES).stream()
					.filter((m) -> ((SubtitlesMediaBase) m).language().isAnyOf(subtitlesLanguages))
					.collect(Collectors.toList());
			Map<MediaType, List<Media>> selectedMedia = Map.of(MediaType.SUBTITLES, subtitles);
			mediaConfig = MediaDownloadConfiguration.of(outputFormat, selectedMedia);
		} else {
			mediaConfig = MediaDownloadConfiguration.of(outputFormat);
		}
		
		return new ResolvedMedia(media, dest, mediaConfig);
	}
	
	public static final <T> List<ResolvedMedia> resolveMedia(TableWindow window,
			List<Pair<T, Media>> result, Function<Pair<T, Media>, String> fTitle) {
		List<ResolvedMedia> resolved = new ArrayList<>();
		Translation translation = window.getTranslation();
		FXUtils.fxTaskValue(() -> {
			Set<Feature> allowedFeatures = new HashSet<>();
			boolean isSingle = result.size() == 1;
			if(!isSingle) allowedFeatures.add(Feature.OUTPUT_FORMAT);
			
			// Check for possible subtitles so that we can possibly add subtitles selector
			boolean hasSubtitles = result.stream()
				.filter((p) -> !Media.findAllOfType(p.b, MediaType.SUBTITLES).isEmpty())
				.findAny().isPresent();
			if(hasSubtitles) allowedFeatures.add(Feature.SUBTITLES_LANGUAGE);
			
			DownloadConfiguration config;
			if(!allowedFeatures.isEmpty()) {
				DownloadConfigurationWindow wdc = MediaDownloader.window(DownloadConfigurationWindow.NAME);
				List<Media> allMedia = result.stream().map((p) -> p.b).collect(Collectors.toList());
				Set<FeatureValue> featureValues = TablePipelineUtils.prepareDownloadConfigurationFeatures(allMedia, allowedFeatures);
				wdc.showWithFeatureValuesAndWait(window, featureValues);
				config = wdc.result();
				if(config == null) return; // Configuration window closed
			} else {
				config = DownloadConfiguration.ofDefault();
			}
			
			MediaLanguage[] subtitlesLanguages = config.subtitlesLanguages().toArray(MediaLanguage[]::new);
			
			if(isSingle) {
				Choosers.OfFile.Chooser chooser = Choosers.OfFile.configuredBuilder()
					.parent(window)
					.title(translation.getSingle("dialogs.save_file"))
					.filters(ExtensionFilters.outputMediaFormats())
					.build();
				Pair<T, Media> pair = result.get(0);
				String title = Utils.validateFileName(Utils.getOrDefault(pair.b.metadata().title(), fTitle.apply(pair)));
				Choosers.SelectedItem path;
				if((path = chooser.fileName(title).showSave()) != null) {
					MediaFormat outputFormat = selectedMediaFormat(path);
					
					if(outputFormat.is(MediaFormat.UNKNOWN)) {
						throw new IllegalStateException("Unknown format");
					}
					
					resolved.add(resolveSingleMedia(pair.b, path.pathWithExtension(), outputFormat, subtitlesLanguages));
				}
			} else {
				Choosers.OfDirectory.Chooser chooser = Choosers.OfDirectory.configuredBuilder()
					.parent(window)
					.title(translation.getSingle("dialogs.save_dir"))
					.build();
				MediaFormat outputFormat = config.outputFormat();
				Choosers.SelectedItem dir;
				if((dir = chooser.show()) != null) {
					Map<String, Integer> usedTitles = new HashMap<>();
					int counter;
					for(Pair<T, Media> pair : result) {
						String title = Utils.validateFileName(Utils.getOrDefault(pair.b.metadata().title(), fTitle.apply(pair)));
						String checkedTitle = title;
						if((counter = usedTitles.getOrDefault(title, 0)) > 0)
							checkedTitle += String.format(" (%d)", counter);
						usedTitles.put(title, counter + 1);
						Path path = dir.path().resolve(Utils.addFormatExtension(checkedTitle, outputFormat));
						resolved.add(resolveSingleMedia(pair.b, path, outputFormat, subtitlesLanguages));
					}
				}
			}
		});
		return resolved;
	}
	
	/** @since 00.02.07 */
	public static final <T> List<ResolvedMedia> resolveMediaMultiple(Window<?> window, List<Pair<T, List<Media>>> result) {
		List<ResolvedMedia> resolved = new ArrayList<>();
		Translation translation = window.getTranslation();
		FXUtils.fxTaskValue(() -> {
			DownloadConfigurationWindow wdc = MediaDownloader.window(DownloadConfigurationWindow.NAME);
			List<Media> allMedia = result.stream().flatMap((p) -> p.b.stream()).collect(Collectors.toList());
			Set<FeatureValue> featureValues = TablePipelineUtils.prepareDownloadConfigurationFeatures(allMedia, Feature.ALL_FEATURES);
			wdc.showWithFeatureValuesAndWait(window, featureValues);
			DownloadConfiguration config = wdc.result();
			if(config == null) return; // Configuration window closed
			
			MediaLanguage[] subtitlesLanguages = config.subtitlesLanguages().toArray(MediaLanguage[]::new);
			
			if(result.size() == 1) {
				Choosers.OfFile.Chooser chooser = Choosers.OfFile.configuredBuilder()
					.parent(window)
					.title(translation.getSingle("dialogs.save_file"))
					.filters(ExtensionFilters.outputMediaFormats())
					.build();
				Pair<T, List<Media>> pair = result.get(0);
				Media media = config.mediaFilter().filter(pair.b);
				if(media == null) return;
				String title = Utils.validateFileName(media.metadata().title());
				Choosers.SelectedItem path;
				if((path = chooser.fileName(title).showSave()) != null) {
					MediaFormat outputFormat = selectedMediaFormat(path);
					
					if(outputFormat.is(MediaFormat.UNKNOWN)) {
						throw new IllegalStateException("Unknown format");
					}
					
					resolved.add(resolveSingleMedia(media, path.pathWithExtension(), outputFormat, subtitlesLanguages));
				}
			} else {
				Choosers.OfDirectory.Chooser chooser = Choosers.OfDirectory.configuredBuilder()
					.parent(window)
					.title(translation.getSingle("dialogs.save_dir"))
					.build();
				MediaFilter mediaFilter = config.mediaFilter();
				MediaFormat outputFormat = config.outputFormat();
				Choosers.SelectedItem dir;
				if((dir = chooser.show()) != null) {
					Map<String, Integer> usedTitles = new HashMap<>();
					int counter;
					for(Pair<T, List<Media>> pair : result) {
						Media media = mediaFilter.filter(pair.b);
						if(media == null) return;
						String title = Utils.validateFileName(media.metadata().title());
						String checkedTitle = title;
						if((counter = usedTitles.getOrDefault(title, 0)) > 0)
							checkedTitle += String.format(" (%d)", counter);
						usedTitles.put(title, counter + 1);
						Path path = dir.path().resolve(Utils.addFormatExtension(checkedTitle, outputFormat));
						resolved.add(resolveSingleMedia(media, path, outputFormat, subtitlesLanguages));
					}
				}
			}
		});
		return resolved;
	}
	
	public static final Set<FeatureValue> prepareDownloadConfigurationFeatures(List<Media> media, Feature... allowedFeatures) {
		return prepareDownloadConfigurationFeatures(media, Set.of(allowedFeatures));
	}
	
	public static final Set<FeatureValue> prepareDownloadConfigurationFeatures(List<Media> media, Set<Feature> allowedFeatures) {
		Set<FeatureValue> featureValues = new HashSet<>();
		// Static features that do not require a feature value
		if(allowedFeatures.contains(Feature.BEST_QUALITY))
			featureValues.add(Feature.BEST_QUALITY.defaultValue());
		if(allowedFeatures.contains(Feature.OUTPUT_FORMAT))
			featureValues.add(Feature.OUTPUT_FORMAT.defaultValue());
		
		if(allowedFeatures.contains(Feature.PREFERRED_QUALITY)) {
			Set<MediaQuality> qualities = media.stream().map(Media::quality).collect(Collectors.toSet());
			if(qualities.size() > 1) {
				featureValues.add(((PreferredQualityFeatureValueFactory) Feature.PREFERRED_QUALITY.valueFactory())
				                        .qualities(qualities).create());
			}
		}
		
		if(allowedFeatures.contains(Feature.FORMATS_PRIORITY)) {
			Set<MediaFormat> formats = media.stream().map(Media::format).collect(Collectors.toSet());
			if(formats.size() > 1) {
				featureValues.add(((FormatsPriorityFeatureValueFactory) Feature.FORMATS_PRIORITY.valueFactory())
				                        .formats(formats).create());
			}
		}
		
		if(allowedFeatures.contains(Feature.AUDIO_LANGUAGE)) {
			Set<MediaLanguage> audioLanguages = media.stream()
				.flatMap((m) -> Media.findAllOfType(m, MediaType.AUDIO).stream())
				.map((m) -> ((AudioMediaBase) m).language())
				.collect(Collectors.toSet());
			if(audioLanguages.size() > 1) {
				featureValues.add(((AudioLanguageFeatureValueFactory) Feature.AUDIO_LANGUAGE.valueFactory())
				                        .languages(audioLanguages).create());
			}
		}
		
		if(allowedFeatures.contains(Feature.SUBTITLES_LANGUAGE)) {
			Set<MediaLanguage> subtitlesLanguages = media.stream()
				.flatMap((m) -> Media.findAllOfType(m, MediaType.SUBTITLES).stream())
				.map((m) -> ((SubtitlesMediaBase) m).language())
				.collect(Collectors.toSet());
			if(!subtitlesLanguages.isEmpty()) {
				featureValues.add(((SubtitlesLanguageFeatureValueFactory) Feature.SUBTITLES_LANGUAGE.valueFactory())
				                        .languages(subtitlesLanguages).create());
			}
		}
		
		return featureValues;
	}
	
	private static final class TranslatableTableColumn<S, T> extends TableColumn<S, T> {
		
		private final Translation translation;
		private final Function<T, String> converter;
		private final boolean titlize;
		
		public TranslatableTableColumn(String text, Translation translation) {
			this(text, translation, false);
		}
		
		public TranslatableTableColumn(String text, Translation translation, boolean titlize) {
			this(text, translation, T::toString, titlize);
		}
		
		public TranslatableTableColumn(String text, Translation translation, Function<T, String> converter) {
			this(text, translation, converter, false);
		}
		
		public TranslatableTableColumn(String text, Translation translation, Function<T, String> converter, boolean titlize) {
			super(text);
			this.translation = translation;
			this.converter = Objects.requireNonNull(converter);
			this.titlize = titlize;
			setCellFactory(this::cellFactory);
		}
		
		private final TableCell<S, T> cellFactory(TableColumn<S, T> column) {
			return new TranslatableTableCell();
		}
		
		private final class TranslatableTableCell extends TableCell<S, T> {
			
			@Override
			protected void updateItem(T item, boolean empty) {
				if(item == getItem()) return;
				super.updateItem(item, empty);
				
				if(item == null) {
					super.setText(null);
					super.setGraphic(null);
				} else if(item instanceof Node) {
					super.setText(null);
					super.setGraphic((Node)item);
				} else {
					String string = converter.apply(item);
					boolean doTitlize = true;
					if(translation != null && translation.hasSingle(string)) {
						string = translation.getSingle(string);
						doTitlize = false;
					}
					if(titlize && doTitlize) {
						string = Utils.titlize(string);
					}
					super.setText(string);
					super.setGraphic(null);
				}
			}
		}
	}
}