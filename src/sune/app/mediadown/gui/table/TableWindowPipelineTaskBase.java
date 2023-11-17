package sune.app.mediadown.gui.table;

import javafx.scene.control.TableView;
import sune.app.mediadown.gui.ProgressWindow.ProgressAction;
import sune.app.mediadown.gui.window.TableWindow;

/** @since 00.01.27 */
public abstract class TableWindowPipelineTaskBase<T> extends ProgressPipelineTaskBase<T, TableWindow> {
	
	public TableWindowPipelineTaskBase(TableWindow window) {
		super(window);
	}
	
	@Override
	protected void submit(ProgressAction action) {
		window.submit(action);
	}
	
	public abstract TableView<T> getTable(TableWindow window);
	public abstract String getTitle(TableWindow window);
	public abstract boolean filter(T item, String text);
	
	// ----- "Default" abstract methods
	
	/** @since 00.02.07 */
	public boolean canReload() { return false; }
	/** @since 00.02.07 */
	public void beforeReload() { /* Do nothing by default */ }
	/** @since 00.02.07 */
	public void afterReload() { /* Do nothing by default */ }
	
	// -----
}