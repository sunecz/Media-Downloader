package sune.app.mediadown.gui.util;

import sune.app.mediadown.media.ResolvedMedia;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineInfo;
import sune.app.mediadown.util.Property;

/** @since 00.02.09 */
public class GUIPipelineInfo extends PipelineInfo {
	
	public GUIPipelineInfo(Pipeline pipeline, ResolvedMedia resolvedMedia) {
		super(pipeline, resolvedMedia);
	}
	
	@Override
	public Property<String> sourceProperty() {
		return sourceProperty == null
					? sourceProperty = new SimpleGUIProperty<>(source())
					: sourceProperty;
	}
	
	@Override
	public Property<String> titleProperty() {
		return titleProperty == null
					? titleProperty = new SimpleGUIProperty<>(title())
					: titleProperty;
	}
	
	@Override
	public Property<String> destinationProperty() {
		return destinationProperty == null
					? destinationProperty = new SimpleGUIProperty<>(destination())
					: destinationProperty;
	}
	
	@Override
	public Property<Double> progressProperty() {
		return progressProperty == null
					? progressProperty = new SimpleGUIProperty<>()
					: progressProperty;
	}
	
	@Override
	public Property<String> stateProperty() {
		return stateProperty == null
					? stateProperty = new SimpleGUIProperty<>()
					: stateProperty;
	}
	
	@Override
	public Property<String> currentProperty() {
		return currentProperty == null
					? currentProperty = new SimpleGUIProperty<>()
					: currentProperty;
	}
	
	@Override
	public Property<String> totalProperty() {
		return totalProperty == null
					? totalProperty = new SimpleGUIProperty<>()
					: totalProperty;
	}
	
	@Override
	public Property<String> speedProperty() {
		return speedProperty == null
					? speedProperty = new SimpleGUIProperty<>()
					: speedProperty;
	}
	
	@Override
	public Property<String> timeLeftProperty() {
		return timeLeftProperty == null
					? timeLeftProperty = new SimpleGUIProperty<>()
					: timeLeftProperty;
	}
	
	@Override
	public Property<String> informationProperty() {
		return informationProperty == null
					? informationProperty = new SimpleGUIProperty<>()
					: informationProperty;
	}
}
