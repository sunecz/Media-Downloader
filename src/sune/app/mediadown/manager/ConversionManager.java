package sune.app.mediadown.manager;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import sune.app.mediadown.Disposables;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.convert.ConversionConfiguration;
import sune.app.mediadown.convert.Converter;
import sune.app.mediadown.convert.FFMpegConverter;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.util.Threads;
import sune.app.mediadown.util.Utils;

/** @since 00.01.26 */
public class ConversionManager {
	
	private static final ExecutorService EXECUTOR;
	
	static {
		int numOfThreads = MediaDownloader.configuration().parallelConversions();
		EXECUTOR = Threads.Pools.newFixed(numOfThreads);
		Disposables.add(ConversionManager::dispose);
	}
	
	private static final Converter createConverter(ConversionConfiguration configuration, MediaFormat formatInput,
			MediaFormat formatOutput, Path fileOutput, Path... filesInput) {
		return new FFMpegConverter(configuration, formatInput, formatOutput, fileOutput, filesInput);
	}
	
	private static final Callable<Void> createCallable(Converter converter) {
		return Utils.callable(converter::start);
	}
	
	public static final ManagerSubmitResult<Converter, Void> submit(ConversionConfiguration configuration,
			MediaFormat formatInput, MediaFormat formatOutput, Path fileOutput, Path... filesInput) {
		if((configuration == null || formatInput == null || formatOutput == null || fileOutput == null
				|| filesInput == null || filesInput.length <= 0))
			throw new IllegalArgumentException();
		Converter converter = createConverter(configuration, formatInput, formatOutput, fileOutput, filesInput);
		Future<Void> future = EXECUTOR.submit(createCallable(converter));
		return new ManagerSubmitResult<>(converter, future);
	}
	
	public static final void dispose() {
		EXECUTOR.shutdownNow();
	}
	
	public static final boolean isRunning() {
		return !EXECUTOR.isShutdown();
	}
}