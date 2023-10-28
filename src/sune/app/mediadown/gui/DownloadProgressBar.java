package sune.app.mediadown.gui;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import sune.app.mediadown.download.DownloadState;
import sune.app.mediadown.download.DownloadState.DownloadedRange;
import sune.app.mediadown.event.DownloadStateEvent;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.util.FXUtils;

/** @since 00.02.09 */
public class DownloadProgressBar extends ResizableCanvas {
	
	private final AtomicBoolean updatePending = new AtomicBoolean();
	private final Listener<DownloadState> listenerUpdate = this::onStateUpdated;
	
	private DownloadState state;
	private Text text;
	
	public DownloadProgressBar() {
	}
	
	private final void onStateUpdated(DownloadState state) {
		redraw();
	}
	
	private final void bindState(DownloadState state) {
		if(state == null) {
			return;
		}
		
		state.addEventListener(DownloadStateEvent.UPDATE, listenerUpdate);
	}
	
	private final void unbindState(DownloadState state) {
		if(state == null) {
			return;
		}
		
		state.removeEventListener(DownloadStateEvent.UPDATE, listenerUpdate);
	}
	
	protected void doDraw() {
		updatePending.set(false);
		draw();
	}
	
	protected void redraw() {
		if(!updatePending.compareAndSet(false, true)) {
			return;
		}
		
		FXUtils.thread(this::doDraw);
	}
	
	@Override
	protected void draw() {
		GraphicsContext gc = getGraphicsContext2D();
		double w = getWidth();
		double h = getHeight();
		
		gc.setFill(Color.BEIGE);
		gc.fillRect(0.0, 0.0, w, h);
		
		drawState();
	}
	
	protected void drawState() {
		if(state == null) {
			return;
		}
		
		GraphicsContext gc = getGraphicsContext2D();
		double w = getWidth();
		double h = getHeight();
		
		int current = state.current();
		int total = state.total();
		
		double t = w / total;
		
		List<DownloadedRange> ranges = state.downloaded();
		
		for(DownloadedRange range : ranges) {
			double x = range.from() * t;
			double s = (range.to() - range.from()) * t;
			gc.setFill(Color.LIGHTSALMON);
			gc.fillRect(x, 0.0, s, h);
		}
		
		String string = String.format(Locale.US, "%.2f%%", current * 100.0 / total);
		
		if(text == null) {
			text = new Text(string);
		} else {
			text.setText(string);
		}
		
		Bounds bounds = text.getLayoutBounds();
		double tx = (w - bounds.getWidth()) * 0.5;
		double ty = (h - bounds.getHeight()) * 0.5;
		
		gc.setGlobalBlendMode(BlendMode.DIFFERENCE);
		gc.setFill(Color.WHITE);
		gc.setTextBaseline(VPos.TOP);
		gc.fillText(string, tx, ty);
		gc.setGlobalBlendMode(BlendMode.SRC_OVER);
	}
	
	public void setState(DownloadState state) {
		unbindState(this.state);
		this.state = state;
		bindState(this.state);
		redraw();
	}
}