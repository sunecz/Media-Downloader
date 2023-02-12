package sune.app.mediadown.gui.table;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.scene.control.TableView;
import sune.app.mediadown.Episode;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.concurrent.ListTask;
import sune.app.mediadown.concurrent.Tasks;
import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.gui.window.DownloadConfigurationWindow;
import sune.app.mediadown.gui.window.DownloadConfigurationWindow.DownloadConfiguration;
import sune.app.mediadown.gui.window.DownloadConfigurationWindow.Feature;
import sune.app.mediadown.gui.window.DownloadConfigurationWindow.FeatureValue;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaFilter;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaLanguage;
import sune.app.mediadown.resource.cache.GlobalCache;
import sune.app.mediadown.util.Choosers;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;

/** @since 00.01.27 */
public final class MultipleEpisodePipelineTask extends MediaEnginePipelineTaskBase<Episode, Pair<Episode, List<Media>>, ResolvedMediaPipelineResult> {
	
	public MultipleEpisodePipelineTask(TableWindow window, MediaEngine engine, List<Episode> items) {
		super(window, engine, items);
	}
	
	@Override
	protected final ListTask<Pair<Episode, List<Media>>> getFunction(Episode item, MediaEngine engine) {
		return Tasks.listOne(() -> {
			List<Media> media = GlobalCache.ofMedia().getChecked(item, () -> {
				ListTask<Media> t = engine.getMedia(item);
				t.startAndWait();
				return t.list();
			});
			
			return new Pair<>(item, media);
		});
	}
	
	@Override
	protected final ResolvedMediaPipelineResult getResult(TableWindow window, MediaEngine engine, List<Pair<Episode, List<Media>>> result) {
		List<ResolvedMedia> resultMedia = new ArrayList<>();
		FXUtils.fxTaskValue(() -> {
			DownloadConfigurationWindow wdc = MediaDownloader.window(DownloadConfigurationWindow.NAME);
			List<Media> allMedia = result.stream().flatMap((p) -> p.b.stream()).collect(Collectors.toList());
			Set<FeatureValue> featureValues = TablePipelineUtils.prepareDownloadConfigurationFeatures(allMedia, Feature.ALL_FEATURES);
			wdc.showWithFeatureValuesAndWait(window, featureValues);
			
			DownloadConfiguration config;
			if((config = wdc.result()) != null) {
				Translation translation = window.getTranslation();
				Choosers.OfDirectory.Chooser chooser = Choosers.OfDirectory.configuredBuilder()
					.parent(window)
					.title(translation.getSingle("dialogs.save_dir"))
					.build();
				MediaFilter filter = config.mediaFilter();
				MediaFormat outputFormat = config.outputFormat();
				MediaLanguage[] subtitlesLanguages = config.subtitlesLanguages().toArray(MediaLanguage[]::new);
				Choosers.SelectedItem dir;
				if((dir = chooser.show()) != null) {
					Map<String, Integer> usedTitles = new HashMap<>();
					int counter;
					for(Pair<Episode, List<Media>> pair : result) {
						Media media = filter.filter(pair.b);
						if(media == null) continue;
						String title = Utils.validateFileName(Utils.getOrDefault(media.metadata().get("title"), pair.a.title()));
						String checkedTitle = title;
						if((counter = usedTitles.getOrDefault(title, 0)) > 0)
							checkedTitle += String.format(" (%d)", counter);
						usedTitles.put(title, counter + 1);
						Path path = dir.path().resolve(Utils.addFormatExtension(checkedTitle, outputFormat));
						resultMedia.add(TablePipelineUtils.resolveSingleMedia(media, path, outputFormat, subtitlesLanguages));
					}
				}
			}
		});
		return new ResolvedMediaPipelineResult(window, resultMedia);
	}
	
	@Override
	protected String getProgressText(TableWindow window) {
		return window.getTranslation().getSingle("progress.media");
	}
	
	@Override
	public final TableView<Pair<Episode, List<Media>>> getTable(TableWindow window) {
		// Notify the handler that nothing should be changed
		return null;
	}
	
	@Override
	public final String getTitle(TableWindow window) {
		// Notify the handler that nothing should be changed
		return null;
	}
	
	@Override
	public final boolean filter(Pair<Episode, List<Media>> item, String text) {
		// Do not filter anything
		return true;
	}
	
	/** @since 00.02.07 */
	public boolean canReload() {
		return true;
	}
	
	/** @since 00.02.07 */
	@Override
	public void beforeReload() {
		items.forEach(GlobalCache.ofMedia()::remove);
	}
}