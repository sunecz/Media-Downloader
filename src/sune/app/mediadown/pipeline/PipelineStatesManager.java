package sune.app.mediadown.pipeline;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import sune.app.mediadown.util.JSON.JSONCollection;
import sune.app.mediadown.util.NIO;

/** @since 00.02.09 */
public class PipelineStatesManager {
	
	private final List<ManagedPipeline> managedPipelines;
	private final StateFile file;
	private final Thread thread;
	private final BlockingQueue<TaskHolder> queue;
	
	private int taskPosition;
	private int taskRemovedCount;
	
	public PipelineStatesManager(Path path) throws IOException {
		Objects.requireNonNull(path);
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
			}
		}
	}
	
	public void add(Pipeline pipeline) {
		synchronized(managedPipelines) {
			int position = taskPosition++;
			int fence = taskRemovedCount;
			ManagedPipeline managed = new ManagedPipeline(position, fence, pipeline);
			enqueueAdd(managed);
			managedPipelines.add(managed);
		}
	}
	
	private final boolean shouldSerialize(Metrics oldMetrics, Metrics newMetrics) {
		// TODO: Implement
		return true;
	}
	
	private final JSONCollection startState(Pipeline pipeline) {
		// TODO: Implement
		return JSONCollection.ofObject(
			"state", "start",
			"time", System.nanoTime()
		);
	}
	
	private final void doAdd(AddTask task) throws IOException {
		ManagedPipeline pipeline = task.pipeline;
		
		Pipeline ref;
		if((ref = pipeline.pipeline.get()) == null) {
			return;
		}
		
		file.save(pipeline, startState(ref));
	}
	
	private final void doUpdate(UpdateTask task) throws IOException {
		PipelineState state = task.state;
		Metrics oldMetrics = task.oldMetrics;
		Metrics newMetrics = state.metrics();
		
		if(shouldSerialize(oldMetrics, newMetrics)) {
			ManagedPipeline pipeline = task.pipeline;
			
			if(!pipeline.isRunning) {
				return;
			}
			
			file.save(pipeline, state.serialize());
			pipeline.taskMetrics.put(pipeline.task, newMetrics);
		}
	}
	
	private final void doEnd(EndTask task) throws IOException {
		ManagedPipeline pipeline = task.pipeline;
		
		if(!pipeline.isRunning) {
			return;
		}
		
		pipeline.isRunning = false;
		file.remove(pipeline);
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
	
	private final void enqueueUpdate(ManagedPipeline pipeline) {
		PipelineTask task;
		if((task = pipeline.task) == null) {
			return; // Nothing to do
		}
		
		PipelineState state;
		if((state = task.state()) == null) {
			return; // Nothing to do
		}
		
		Metrics oldMetrics = pipeline.taskMetrics.get(task);
		UpdateTask queueTask = new UpdateTask(pipeline, state, oldMetrics);
		enqueueTask(pipeline, queueTask);
	}
	
	private final void enqueueEnd(ManagedPipeline pipeline) {
		EndTask queueTask = new EndTask(pipeline);
		enqueueTask(pipeline, queueTask);
	}
	
	private void pipelineRemoved(ManagedPipeline pipeline) {
		synchronized(managedPipelines) {
			managedPipelines.remove(pipeline);
			int position = pipeline.position;
			
			for(ManagedPipeline mp : managedPipelines) {
				mp.pipelineRemoved(position);
			}
			
			++taskRemovedCount;
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
		
		private static final int COUNT = 3;
		private static final int TYPE_ADD = 0;
		private static final int TYPE_UPDATE = 1;
		private static final int TYPE_END = 2;
		
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
	}
	
	private static final class StateFile {
		
		private static final OpenOption[] OPEN_OPTIONS = {
			StandardOpenOption.READ,
			StandardOpenOption.WRITE,
			StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING
		};
		
		private final FileChannel channel;
		private final PositionTable posTable;
		
		public StateFile(Path path) throws IOException {
			Objects.requireNonNull(path);
			channel = FileChannel.open(path, OPEN_OPTIONS);
			posTable = new PositionTable();
		}
		
		public void save(ManagedPipeline pipeline, JSONCollection state) throws IOException {
			int position = pipeline.savePosition();
			
			StringBuilder sb = new StringBuilder();
			state.toString(sb, true);
			sb.append('\n');
			byte[] bytes = sb.toString().getBytes(Shared.CHARSET);
			
			ByteBuffer buf = ByteBuffer.wrap(bytes);
			long pos = posTable.get(position);
			long size = buf.limit();
			
			if(pos == PositionTable.UNSET) {
				pos = posTable.add(size);
			}
			
			channel.write(buf, pos);
			posTable.set(position, size);
			channel.force(false);
		}
		
		public void remove(ManagedPipeline pipeline) throws IOException {
			int position = pipeline.savePosition();
			long pos = posTable.get(position);
			
			if(pos == PositionTable.UNSET) {
				return; // Does not exist
			}
			
			long size = posTable.getSize(position);
			posTable.del(position);
			NIO.truncate(channel, pos, size);
			channel.force(false);
		}
	}
	
	private final class AddTask implements Task {
		
		private final long creationTime;
		private final ManagedPipeline pipeline;
		private volatile boolean isStarted;
		
		public AddTask(ManagedPipeline pipeline) {
			this.creationTime = System.nanoTime();
			this.pipeline = pipeline;
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
	
	private final class UpdateTask implements Task {
		
		private final long creationTime;
		private final ManagedPipeline pipeline;
		private final PipelineState state;
		private final Metrics oldMetrics;
		private volatile boolean isStarted;
		
		public UpdateTask(ManagedPipeline pipeline, PipelineState state, Metrics oldMetrics) {
			this.creationTime = System.nanoTime();
			this.pipeline = pipeline;
			this.state = state;
			this.oldMetrics = oldMetrics;
		}
		
		@Override
		public void run() throws Exception {
			isStarted = true;
			doUpdate(this);
		}
		
		@Override public ManagedPipeline pipeline() { return pipeline; }
		@Override public int typeOrdinal() { return TaskType.TYPE_UPDATE; }
		@Override public boolean isStarted() { return isStarted; }
		@Override public long creationTime() { return creationTime; }
	}
	
	private final class EndTask implements Task {
		
		private final long creationTime;
		private final ManagedPipeline pipeline;
		private volatile boolean isStarted;
		
		public EndTask(ManagedPipeline pipeline) {
			this.creationTime = System.nanoTime();
			this.pipeline = pipeline;
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
		
		public ManagedPipeline(int position, int lowerFence, Pipeline pipeline) {
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
			});
			
			pipeline.addEventListener(TrackerEvent.UPDATE, (tracker) -> {
				enqueueUpdate(this);
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
