package sune.app.mediadown.gui.control;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.skin.TreeViewSkin;
import javafx.util.Callback;
import sune.app.mediadown.util.FXUtils;

/** @since 00.02.09 */
public class FixedWidthTreeView<T> extends TreeView<T> {
	
	private ListChangeListener<TreeItem<T>> rootChildrenListener;
	
	public FixedWidthTreeView() {
		setSkin(new FixedWidthTreeView.FixedWidthTreeViewSkin<>(this));
		rootProperty().addListener((o, oldRoot, newRoot) -> {
			unbindRoot(oldRoot);
			bindRoot(newRoot);
		});
	}
	
	private final void onRootChildrenChanged(Change<? extends TreeItem<T>> change) {
		List<TreeItem<T>> added = null;
		List<TreeItem<T>> removed = null;
		
		while(change.next()) {
			if(change.wasAdded()) {
				if(added == null) {
					added = new ArrayList<>(change.getAddedSize());
				}
				
				added.addAll(change.getAddedSubList());
			}
			
			if(change.wasRemoved()) {
				if(removed == null) {
					removed = new ArrayList<>(change.getRemovedSize());
				}
				
				removed.addAll(change.getRemoved());
			}
		}
	}
	
	private final ListChangeListener<TreeItem<T>> rootChildrenListener() {
		if(rootChildrenListener == null) {
			rootChildrenListener = this::onRootChildrenChanged;
		}
		
		return rootChildrenListener;
	}
	
	private final void unbindRoot(TreeItem<T> root) {
		if(root == null) {
			return;
		}
		
		root.getChildren().removeListener(rootChildrenListener());
	}
	
	private final void bindRoot(TreeItem<T> root) {
		if(root == null) {
			return;
		}
		
		root.getChildren().addListener(rootChildrenListener());
		
		ObservableList<TreeItem<T>> children = root.getChildren();
		FXUtils.once(
			children::addListener, children::removeListener,
			(Change<? extends TreeItem<T>> change) -> notifyVisible()
		);
	}
	
	@SuppressWarnings("unchecked")
	private final FixedWidthTreeView.FixedWidthTreeViewSkin<T> skin() {
		return (FixedWidthTreeView.FixedWidthTreeViewSkin<T>) getSkin();
	}
	
	private final void notifyVisible() {
		skin().prepareForWidthComputation();
	}
	
	private static final class FixedWidthTreeViewSkin<T> extends TreeViewSkin<T> {
		
		private static final double ARROW_DEFAULT_WIDTH = 12.0;
		private static final double INDENT_PER_LEVEL = 12.0;
		
		private final Map<TreeItem<T>, WeakReference<FixedWidthTreeCell>> cells = new WeakHashMap<>();
		private volatile List<TreeItem<T>> expandedItems = null;
		private final Set<TreeItem<T>> computedItems = new HashSet<>();
		
		public FixedWidthTreeViewSkin(TreeView<T> control) {
			super(control);
			control.setCellFactory(cellFactory());
		}
		
		private final void expandChildren(TreeItem<T> parent, List<TreeItem<T>> expanded) {
			for(TreeItem<T> child : parent.getChildren()) {
				if(!child.isExpanded() && !child.getChildren().isEmpty()) {
					child.setExpanded(true);
					expanded.add(child);
				}
				
				expandChildren(child, expanded);
			}
		}
		
		private final void collapseItems(List<TreeItem<T>> items) {
			for(TreeItem<T> item : items) {
				item.setExpanded(false);
			}
		}
		
		@Override
		protected void layoutChildren(double x, double y, double w, double h) {
			super.layoutChildren(x, y, w, h);
			
			double maxWidth = computePrefWidth(-1.0, 0.0, 0.0, 0.0, 0.0);
			TreeView<T> view = getSkinnable();
			double prefWidth = view.getPrefWidth();
			
			if(prefWidth < 0.0 || prefWidth != maxWidth) {
				// Must be done in the next FX pulse to take the correct effect
				FXUtils.enqueue(() -> {
					view.setPrefWidth(maxWidth);
					
					List<TreeItem<T>> expanded = expandedItems;
					int count = computedItems.size();
					
					// Collapse all items only when all cells have their expanded width set
					if(expanded != null && count == getItemCount()) {
						collapseItems(expanded);
						expandedItems = null;
						computedItems.clear();
					}
				});
			}
		}
		
		protected double computeItemWidth(TreeItem<T> item, int level) {
			double width = -1.0;
			WeakReference<FixedWidthTreeCell> ref = cells.get(item);
			FixedWidthTreeCell cell;
			
			if(ref != null && (cell = ref.get()) != null) {
				boolean isExpanded = item.isExpanded() || item.getChildren().isEmpty();
				width = cell.expandedWidth();
				
				if(width < 0.0 && isExpanded) {
					width = (level - 1) * INDENT_PER_LEVEL + cell.computePrefWidth();
					
					for(TreeItem<T> child : item.getChildren()) {
						width = Math.max(width, computeItemWidth(child, level + 1));
					}
					
					cell.expandedWidth(width);
					computedItems.add(item);
				}
			}
			
			return width;
		}
		
		@Override
		protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset,
		        double leftInset) {
			double maxWidth = -1.0;
			TreeItem<T> root = getSkinnable().getRoot();
			
			if(root != null) {
				for(TreeItem<T> item : root.getChildren()) {
					maxWidth = Math.max(maxWidth, computeItemWidth(item, 1));
				}
			}
			
			if(maxWidth < 0.0) {
				return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
			}
			
			return maxWidth;
		}
		
		protected final Callback<TreeView<T>, TreeCell<T>> cellFactory() {
			return (t) -> new FixedWidthTreeCell();
		}
		
		protected final void prepareForWidthComputation() {
			FXUtils.enqueue(() -> {
				List<TreeItem<T>> expanded = new ArrayList<>();
				expandChildren(getSkinnable().getRoot(), expanded);
				expandedItems = expanded;
			});
		}
		
		private final class FixedWidthTreeCell extends TreeCell<T> {
			
			private volatile double expandedWidth = -1.0;
			
			@Override
			protected void updateItem(T item, boolean empty) {
				super.updateItem(item, empty);
				
				if(item == null || empty) {
					setGraphic(null);
					setText(null);
				} else {
					setText(item.toString());
					cells.compute(getTreeItem(), (it, ref) -> {
						FixedWidthTreeCell cell;
						
						if(ref != null && (cell = ref.get()) != null) {
							// Do not update the expanded width since the cell was actually not updated
							if(cell == this) {
								return ref;
							}
							
							double width = cell.expandedWidth;
							expandedWidth = width;
							// Must reset the previous cell's expanded width
							cell.expandedWidth = -1.0;
						}
						
						return new WeakReference<>(this);
					});
				}
			}
			
			public double computePrefWidth() {
				double width = computePrefWidth(-1.0);
				
				if(width < 0.0) {
					return width;
				}
				
				double gap = getGraphicTextGap();
				double graphicWidth = 2.0 * ARROW_DEFAULT_WIDTH + gap;
				Node graphic = getGraphic();
				
				if(graphic != null) {
					graphicWidth = graphic.prefWidth(-1.0);
				}
				
				return width + 2.0 * gap + graphicWidth;
			}
			
			public void expandedWidth(double expandedWidth) {
				this.expandedWidth = expandedWidth;
			}
			
			public double expandedWidth() {
				return expandedWidth;
			}
		}
	}
}