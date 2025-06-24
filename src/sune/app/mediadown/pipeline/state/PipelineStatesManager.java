package sune.app.mediadown.pipeline.state;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import sune.app.mediadown.Shared;
import sune.app.mediadown.concurrent.Threads;
import sune.app.mediadown.event.PipelineEvent;
import sune.app.mediadown.event.tracker.TrackerEvent;
import sune.app.mediadown.pipeline.MediaPipelineResult;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineMedia;
import sune.app.mediadown.pipeline.PipelineTask;
import sune.app.mediadown.util.JSON;
import sune.app.mediadown.util.JSON.JSONCollection;
import sune.app.mediadown.util.JSON.JSONObject;
import sune.app.mediadown.util.NIO;

/** @since 00.02.09 */
public class PipelineStatesManager {
	
	private final Path path;
	private final List<ManagedPipeline> managedPipelines;
	private final StateFile file;
	private final Thread thread;
	private final BlockingQueue<TaskHolder> queue;
	
	private int taskPosition;
	private int taskRemovedCount;
	
	public PipelineStatesManager(Path path) throws IOException {
		this.path = Objects.requireNonNull(path);
		managedPipelines = new ArrayList<>();
		thread = Threads.newThread(this::run);
		file = new StateFile(path);
		queue = new LinkedBlockingQueue<>();
		thread.start();
	}
	
	private final void run() {
		for(TaskHolder item;;) {
			try {
				item = queue.take();
				item.run();
			} catch(InterruptedException ex) {
				break;
			} catch(Exception ex) {
				// Ignore
				ex.printStackTrace(); // FIXME: Remove
			}
		}
	}
	
	private final boolean shouldSerialize(Metrics oldMetrics, Metrics newMetrics) {
		return MetricsComparator.compareWithRegistered(oldMetrics, newMetrics);
	}
	
	private final JSONCollection startState(Pipeline pipeline) {
		MediaPipelineResult input = (MediaPipelineResult) pipeline.getResult();
		PipelineMedia media = input.media();
		
		return JSONCollection.ofObject(
			"type", JSONObject.ofString("start"),
			"input", PipelineStates.Serialization.serialize(media)
		);
	}
	
	private final void doAdd(AddTask task) throws IOException {
		ManagedPipeline pipeline = task.pipeline;
		
		Pipeline ref;
		if((ref = pipeline.pipeline.get()) == null) {
			return;
		}
		
		file.save(pipeline, startState(ref));
		Reference.reachabilityFence(ref);
	}
	
	private final void doPipelineUpdate(PipelineUpdateTask task) throws IOException {
		ManagedPipeline pipeline = task.pipeline;
		
		if(!pipeline.isRunning) {
			return;
		}
		
		file.save(pipeline, task.state.serialize());
	}
	
	private final void doStateUpdate(StateUpdateTask task) throws IOException {
		Metrics oldMetrics = task.oldMetrics;
		Metrics newMetrics = task.newMetrics;
		
		if(shouldSerialize(oldMetrics, newMetrics)) {
			ManagedPipeline pipeline = task.pipeline;
			
			if(!pipeline.isRunning) {
				return;
			}
			
			pipeline.taskMetrics.put(pipeline.task, newMetrics);
			file.save(pipeline, task.state.serialize());
		}
	}
	
	private final void doEnd(EndTask task) throws IOException {
		ManagedPipeline pipeline = task.pipeline;
		
		if(!pipeline.isRunning) {
			return;
		}
		
		pipeline.isRunning = false;
		
		// Remove the pipeline's data if and only if it has successfully ended,
		// so that its state may be recovered on subsequent future load.
		if(task.isSuccess) {
			file.remove(pipeline);
		}
		
		pipelineRemoved(pipeline);
	}
	
	private final void enqueueTask(ManagedPipeline pipeline, Task task) {
		TaskHolder holder;
		if((holder = pipeline.setTask(task)).isSet) {
			return; // Exchanged or rejected
		}
		
		queue.add(holder);
	}
	
	private final void enqueueAdd(ManagedPipeline pipeline) {
		AddTask queueTask = new AddTask(pipeline);
		enqueueTask(pipeline, queueTask);
	}
	
	private final void enqueuePipelineUpdate(ManagedPipeline pipeline) {
		PipelineTask task;
		if((task = pipeline.task) == null) {
			return; // Nothing to do
		}
		
		PipelineState state;
		if((state = task.state()) == null) {
			return; // Nothing to do
		}
		
		PipelineUpdateTask queueTask = new PipelineUpdateTask(pipeline, state);
		enqueueTask(pipeline, queueTask);
	}
	
	private final void enqueueStateUpdate(ManagedPipeline pipeline) {
		PipelineTask task;
		if((task = pipeline.task) == null) {
			return; // Nothing to do
		}
		
		PipelineState state;
		if((state = task.state()) == null) {
			return; // Nothing to do
		}
		
		Metrics oldMetrics = pipeline.taskMetrics.get(task);
		Metrics newMetrics = state.metrics();
		StateUpdateTask queueTask = new StateUpdateTask(pipeline, state, oldMetrics, newMetrics);
		enqueueTask(pipeline, queueTask);
	}
	
	private final void enqueueEnd(ManagedPipeline pipeline) {
		Pipeline ref;
		if((ref = pipeline.pipeline.get()) == null) {
			return; // Nothing to do
		}
		
		boolean isSuccess = ref.isStarted() && !ref.isError() && !ref.isStopped();
		EndTask queueTask = new EndTask(pipeline, isSuccess);
		enqueueTask(pipeline, queueTask);
	}
	
	private final void pipelineRemoved(ManagedPipeline pipeline) {
		synchronized(managedPipelines) {
			managedPipelines.remove(pipeline);
			int position = pipeline.position;
			
			for(ManagedPipeline mp : managedPipelines) {
				mp.pipelineRemoved(position);
			}
			
			++taskRemovedCount;
		}
	}
	
	public void add(Pipeline pipeline) {
		synchronized(managedPipelines) {
			int position = taskPosition++;
			int fence = taskRemovedCount;
			ManagedPipeline managed = new ManagedPipeline(pipeline, position, fence);
			enqueueAdd(managed);
			managedPipelines.add(managed);
		}
	}
	
	public void load() throws IOException {
		file.load();
	}
	
	public void clear() throws IOException {
		file.clear();
	}
	
	public Iterator<JSONCollection> contents() throws IOException {
		return new ContentsIterator(path);
	}
	
	private static final class ContentsIterator implements Iterator<JSONCollection> {
		
		private final JSON.JSONReader reader;
		private boolean eof;
		private JSONCollection item;
		
		public ContentsIterator(Path path) throws IOException {
			this.reader = JSON.newReader(path);
		}
		
		@Override
		public boolean hasNext() {
			if(eof) {
				return false;
			}
			
			if(item != null) {
				return true; // Prefetched
			}
			
			try {
				item = reader.read();
				eof = item == null;
				return !eof;
			} catch(IOException ex) {
				eof = true;
				return false;
			}
		}
		
		@Override
		public JSONCollection next() {
			JSONCollection result = item;
			
			if(result == null) {
				throw new NoSuchElementException(); // Follow the Iterator contract
			}
			
			item = null; // Reset
			return result;
		}
	}
	
	private static interface Task {
		
		void run() throws Exception;
		ManagedPipeline pipeline();
		int typeOrdinal();
		boolean isStarted();
		long creationTime();
	}
	
	private static final class TaskHolder {
		
		private Task task;
		private volatile boolean isStarted;
		private volatile boolean isSet;
		
		public TaskHolder(Task task) {
			this.task = Objects.requireNonNull(task);
		}
		
		public void run() throws Exception {
			isStarted = true;
			task.run();
		}
		
		public void set(Task newTask) {
			task = newTask;
			isSet = true;
		}
	}
	
	private static final class TaskType {
		
		private static final int COUNT = 4;
		private static final int TYPE_ADD = 0;
		private static final int TYPE_PIPELINE_UPDATE = 1;
		private static final int TYPE_STATE_UPDATE = 2;
		private static final int TYPE_END = 3;
		
		private TaskType() {
		}
		
		public static final AtomicReference<TaskHolder>[] newRefArray() {
			@SuppressWarnings("unchecked")
			AtomicReference<TaskHolder>[] refs = new AtomicReference[TaskType.COUNT];
			for(int i = 0, l = refs.length; i < l; ++i) refs[i] = new AtomicReference<>();
			return refs;
		}
	}
	
	private static final class PositionTable {
		
		private static final int DEFAULT_SIZE = 16;
		private static final long UNSET = -1L;
		
		private long[] pos;
		private int cap;
		
		public PositionTable() {
			pos = new long[DEFAULT_SIZE];
			cap = 0; // Empty
			Arrays.fill(pos, UNSET);
			pos[cap] = 0L; // End
		}
		
		private final void resize() {
			int len = pos.length;
			long[] arr = new long[len + (len + 1) / 2];
			System.arraycopy(pos, 0, arr, 0, len);
			pos = arr;
		}
		
		private final void shift(int idx, long size) {
			long shift = size - (pos[idx + 1] - pos[idx]);
			
			for(int i = idx + 1; i <= cap; ++i) {
				pos[i] += shift;
			}
		}
		
		public void set(int idx, long size) {
			if(idx < 0 || idx >= cap) {
				return; // Ignore
			}
			
			shift(idx, size);
		}
		
		public void del(int idx) {
			if(idx < 0 || idx >= cap) {
				return; // Ignore
			}
			
			shift(idx, 0L);
			System.arraycopy(pos, idx + 1, pos, idx, cap - idx);
			--cap;
		}
		
		public long add(long size) {
			if(size < 0L) {
				return UNSET;
			}
			
			if(cap + 1 >= pos.length) {
				resize();
			}
			
			long val = pos[cap++];
			pos[cap] = val + size;
			return val;
		}
		
		public long get(int idx) {
			if(idx < 0 || idx >= cap) {
				return UNSET;
			}
			
			return pos[idx];
		}
		
		public long getSize(int idx) {
			if(idx < 0 || idx >= cap) {
				return UNSET;
			}
			
			return pos[idx + 1] - pos[idx];
		}
		
		public void clear() {
			Arrays.fill(pos, UNSET);
			cap = 0; // Empty
			pos[cap] = 0L; // End
		}
	}
	
	private static final class StateFile {
		
		private static final OpenOption[] OPEN_OPTIONS = {
			StandardOpenOption.READ,
			StandardOpenOption.WRITE,
			StandardOpenOption.CREATE,
		};
		
		private final Path path;
		private final FileChannel channel;
		private final PositionTable table;
		
		public StateFile(Path path) throws IOException {
			this.path = Objects.requireNonNull(path);
			this.channel = FileChannel.open(path, OPEN_OPTIONS);
			this.table = new PositionTable();
		}
		
		public void load() throws IOException {
			if(table.cap > 0) {
				// Clear beforehand so that the table is synchronized with the file content,
				// i.e. the first line should be the first entry in the table.
				table.clear();
			}
			
			ByteBuffer buf = NIO.newBuffer(path);
			int num, size = 0;
			
			for(long pos = 0L;
					(num = channel.read(buf, pos)) >= 0;
					 pos += num) {
				buf.flip();
				int i = 0, l = buf.limit(), p = 0;
				
				for(int c; i < l; ++i) {
					c = buf.get(i) & 0xff;
					
					if(c == '\n') {
						table.add(size + (i + 1 - p));
						size = 0; p = i + 1;
					}
				}
				
				size += l - p;
				buf.clear();
			}
			
			if(size > 0) {
				table.add(size);
			}
		}
		
		public void save(ManagedPipeline pipeline, JSONCollection state) throws IOException {
			int position = pipeline.savePosition();
			
			StringBuilder sb = new StringBuilder();
			state.toString(sb, true);
			sb.append('\n');
			byte[] bytes = sb.toString().getBytes(Shared.CHARSET);
			
			ByteBuffer buf = ByteBuffer.wrap(bytes);
			long pos = table.get(position);
			long newSize = buf.limit();
			
			if(pos == PositionTable.UNSET) {
				pos = table.add(newSize);
			}
			
			long oldSize = table.getSize(position);
			table.set(position, newSize);
			NIO.replace(channel, pos, oldSize, buf);
			channel.force(false);
		}
		
		public void remove(ManagedPipeline pipeline) throws IOException {
			int position = pipeline.savePosition();
			long pos = table.get(position);
			
			if(pos == PositionTable.UNSET) {
				return; // Does not exist
			}
			
			long size = table.getSize(position);
			table.del(position);
			NIO.truncate(channel, pos, size);
			channel.force(false);
		}
		
		public void clear() throws IOException {
			channel.truncate(0L);
			channel.force(false);
		}
	}
	
	private final class AddTask implements Task {
		
		private final long creationTime;
		private final ManagedPipeline pipeline;
		private volatile boolean isStarted;
		
		public AddTask(ManagedPipeline pipeline) {
			this.creationTime = System.nanoTime();
			this.pipeline = Objects.requireNonNull(pipeline);
		}
		
		@Override
		public void run() throws Exception {
			isStarted = true;
			doAdd(this);
		}
		
		@Override public ManagedPipeline pipeline() { return pipeline; }
		@Override public int typeOrdinal() { return TaskType.TYPE_ADD; }
		@Override public boolean isStarted() { return isStarted; }
		@Override public long creationTime() { return creationTime; }
	}
	
	private final class PipelineUpdateTask implements Task {
		
		private final long creationTime;
		private final ManagedPipeline pipeline;
		private final PipelineState state;
		private volatile boolean isStarted;
		
		public PipelineUpdateTask(ManagedPipeline pipeline, PipelineState state) {
			this.creationTime = System.nanoTime();
			this.pipeline = Objects.requireNonNull(pipeline);
			this.state = Objects.requireNonNull(state);
		}
		
		@Override
		public void run() throws Exception {
			isStarted = true;
			doPipelineUpdate(this);
		}
		
		@Override public ManagedPipeline pipeline() { return pipeline; }
		@Override public int typeOrdinal() { return TaskType.TYPE_PIPELINE_UPDATE; }
		@Override public boolean isStarted() { return isStarted; }
		@Override public long creationTime() { return creationTime; }
	}
	
	private final class StateUpdateTask implements Task {
		
		private final long creationTime;
		private final ManagedPipeline pipeline;
		private final PipelineState state;
		private final Metrics oldMetrics;
		private final Metrics newMetrics;
		private volatile boolean isStarted;
		
		public StateUpdateTask(
			ManagedPipeline pipeline,
			PipelineState state,
			Metrics oldMetrics,
			Metrics newMetrics
		) {
			this.creationTime = System.nanoTime();
			this.pipeline = Objects.requireNonNull(pipeline);
			this.state = Objects.requireNonNull(state);
			this.oldMetrics = oldMetrics;
			this.newMetrics = newMetrics;
		}
		
		@Override
		public void run() throws Exception {
			isStarted = true;
			doStateUpdate(this);
		}
		
		@Override public ManagedPipeline pipeline() { return pipeline; }
		@Override public int typeOrdinal() { return TaskType.TYPE_STATE_UPDATE; }
		@Override public boolean isStarted() { return isStarted; }
		@Override public long creationTime() { return creationTime; }
	}
	
	private final class EndTask implements Task {
		
		private final long creationTime;
		private final ManagedPipeline pipeline;
		private final boolean isSuccess;
		private volatile boolean isStarted;
		
		public EndTask(ManagedPipeline pipeline, boolean isSuccess) {
			this.creationTime = System.nanoTime();
			this.pipeline = Objects.requireNonNull(pipeline);
			this.isSuccess = isSuccess;
		}
		
		@Override
		public void run() throws Exception {
			isStarted = true;
			doEnd(this);
		}
		
		@Override public ManagedPipeline pipeline() { return pipeline; }
		@Override public int typeOrdinal() { return TaskType.TYPE_END; }
		@Override public boolean isStarted() { return isStarted; }
		@Override public long creationTime() { return creationTime; }
	}
	
	private final class ManagedPipeline {
		
		private final WeakReference<Pipeline> pipeline;
		private final int position;
		private final AtomicReference<TaskHolder>[] lastTasks;
		private final Map<PipelineTask, Metrics> taskMetrics;
		private volatile int lowerFence;
		private volatile PipelineTask task;
		private volatile boolean isRunning;
		
		public ManagedPipeline(Pipeline pipeline, int position, int lowerFence) {
			Objects.requireNonNull(pipeline);
			this.pipeline = new WeakReference<>(pipeline);
			this.position = position;
			this.lowerFence = lowerFence;
			this.lastTasks = TaskType.newRefArray();
			this.taskMetrics = new WeakHashMap<>();
			
			pipeline.addEventListener(PipelineEvent.BEGIN, (o) -> {
				isRunning = true;
			});
			
			pipeline.addEventListener(PipelineEvent.END, (o) -> {
				enqueueEnd(this);
			});
			
			pipeline.addEventListener(PipelineEvent.UPDATE, (p) -> {
				task = p.b;
				enqueuePipelineUpdate(this);
			});
			
			pipeline.addEventListener(TrackerEvent.UPDATE, (tracker) -> {
				enqueueStateUpdate(this);
			});
		}
		
		private final UnaryOperator<TaskHolder> fnLatestTask(Task task) {
			return (current) -> {
				// No task has been started yet
				if(current == null) {
					return new TaskHolder(task);
				}
				
				// The latest task already started
				if(current.isStarted) {
					return new TaskHolder(task);
				}
				
				// Our task is newer
				if(current.task.creationTime() < task.creationTime()) {
					current.set(task); // Keep only ours
				}
				
				return current;
			};
		}
		
		protected void pipelineRemoved(int otherPosition) {
			if(otherPosition < position) {
				++lowerFence;
			}
		}
		
		public TaskHolder setTask(Task task) {
			AtomicReference<TaskHolder> ref = lastTasks[task.typeOrdinal()];
			return ref.updateAndGet(fnLatestTask(task));
		}
		
		public int savePosition() {
			return position - lowerFence;
		}
	}
}
