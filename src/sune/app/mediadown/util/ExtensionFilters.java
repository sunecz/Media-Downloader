package sune.app.mediadown.util;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import javafx.stage.FileChooser.ExtensionFilter;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.conversion.ConversionFormat;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaType;

public final class ExtensionFilters {
	
	private static ExtensionFilter[] outputMediaFormats;
	private static final BiFunction<MediaFormat[], MediaType[], Comparator<MediaFormat>> comparatorSupplier;
	
	/** @since 00.02.09 */
	private static final Regex REGEX_FORMAT_NAME = Regex.of("(?i)-(?:video|audio)$");
	
	static {
		comparatorSupplier = ((mediaFormats, mediaTypes) -> {
			return ComparatorCombiner
						.<MediaFormat>of((a, b) -> Utils.compareIndex(a.mediaType(), b.mediaType(), mediaTypes))
						.combine((a, b) -> Utils.compareIndex(a, b, mediaFormats));
		});
	}
	
	// Forbid anyone to create an instance of this class
	private ExtensionFilters() {
	}
	
	/** @since 00.02.09 */
	private static final MediaFormat[] supportedOutputFormats() {
		return MediaDownloader.configuration().conversionProvider().formats().stream()
					.map(ConversionFormat::format)
					.filter(MediaFormat::isOutputFormat)
					.toArray(MediaFormat[]::new);
	}
	
	private static final String[] fixExtensions(List<String> extensions) {
		return extensions.stream().map((extension) -> {
			return (!extension.startsWith("*")
						? !extension.startsWith(".")
							? "*."
							: "*"
						: "") + extension;
		}).toArray(String[]::new);
	}
	
	public static final ExtensionFilter extensionFilter(MediaFormat format) {
		return new ExtensionFilter(
			String.format(
				"%s %s",
				REGEX_FORMAT_NAME.replaceFirst(format.toString(), ""),
				format.mediaType().toString().toLowerCase()
			),
			fixExtensions(format.fileExtensions())
		);
	}
	
	public static final ExtensionFilter[] outputMediaFormats() {
		if(outputMediaFormats == null) {
			MediaFormat[] ref = supportedOutputFormats();
			
			outputMediaFormats = Stream.of(ref)
				.sorted(comparatorSupplier.apply(ref, MediaType.values()))
				.map(ExtensionFilters::extensionFilter)
				.toArray(ExtensionFilter[]::new);
		}
		
		return outputMediaFormats;
	}
}