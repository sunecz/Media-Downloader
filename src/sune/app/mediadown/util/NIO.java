package sune.app.mediadown.util;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import sune.app.mediadown.Shared;
import sune.app.mediadown.concurrent.Threads;
import sune.app.mediadown.util.unsafe.Reflection;

public final class NIO {
	
	private static final Charset CHARSET = Shared.CHARSET;
	private static final Set<OpenOption> OPTIONS_SAVE = Set.of(CREATE, WRITE);
	
	// Forbid anyone to create an instance of this class
	private NIO() {
	}
	
	public static final void save(Path file, String content)
			throws IOException {
		Files.write(file, content.getBytes(CHARSET));
	}
	
	public static final String read(Path file)
			throws IOException {
		return new String(Files.readAllBytes(file), CHARSET);
	}
	
	public static final Path localPath() {
		return Path.of(PathSystem.getCurrentDirectory());
	}
	
	public static final Path localPath(String path, String... more) {
		return Path.of(PathSystem.getFullPath(path), more);
	}
	
	public static final Path path(String path, String... more) {
		return Path.of(path, more);
	}
	
	public static final Path path(Path path, String child) {
		return path.resolve(child);
	}
	
	/** @since 00.02.05 */
	public static final Path pathOrNull(String path, String... more) {
		return path != null ? Path.of(path, more) : null;
	}
	
	public static final void copy(InputStream src, Path dest)
			throws IOException {
		try(FileChannel         output = FileChannel.open(dest, CREATE, WRITE);
			ReadableByteChannel input  = Channels.newChannel(src)) {
			output.transferFrom(input, 0L, Long.MAX_VALUE);
		}
	}
	
	/** @since 00.02.04 */
	public static final void copyFile(Path src, Path dest) throws IOException {
		Files.copy(src, dest, REPLACE_EXISTING);
	}
	
	/** @since 00.02.04 */
	public static final void copy(Path src, Path dest) throws IOException {
		if(isRegularFile(src)) copyFile(src, dest);
		else                   copyDir (src, dest);
	}
	
	/** @since 00.02.02 */
	private static final <T> Iterable<T> iterable(Iterator<T> iterator) {
		return (() -> iterator);
	}
	
	/** @since 00.02.02 */
	public static final void copyDir(Path src, Path dest) throws IOException {
		for(Path path : iterable(Files.walk(src).iterator())) {
			Path newPath = dest.resolve(src.relativize(path));
			Files.copy(path, newPath, StandardCopyOption.REPLACE_EXISTING);
		}
	}
	
	/** @since 00.02.02 */
	public static final void mergeDirectories(Path src, Path dest) throws IOException {
		mergeDirectories(src, dest, (path, newPath) -> !exists(newPath));
	}
	
	/** @since 00.02.05 */
	public static final void mergeDirectories(Path src, Path dest, BiPredicate<Path, Path> filter) throws IOException {
		if(src == null || dest == null || filter == null)
			throw new IllegalArgumentException();
		for(Path path : iterable(Files.walk(src).iterator())) {
			Path newPath = dest.resolve(src.relativize(path));
			if(filter.test(path, newPath)) {
				createDir(newPath.getParent());
				Files.copy(path, newPath, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}
	
	public static final void createFile(Path file)
			throws IOException {
		if((exists(file)))
			// Do not try to create already-existing file, since it throws IOException
			return;
		Files.createFile(file);
	}
	
	public static final void createDir(Path dir)
			throws IOException {
		if((exists(dir)))
			// Do not try to create already-existing directory, since it throws IOException
			return;
		Files.createDirectories(dir);
	}
	
	public static final boolean exists(Path path) {
		return Files.exists(path);
	}
	
	public static final boolean isRegularFile(Path path) {
		return Files.isRegularFile(path);
	}
	
	public static final boolean isDirectory(Path path) {
		return Files.isDirectory(path);
	}
	
	public static final boolean isEmptyDirectory(Path path) throws IOException {
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			return !stream.iterator().hasNext();
		}
	}
	
	public static final long size(Path path) throws IOException {
		return Files.size(path);
	}
	
	private static SimpleFileVisitor<Path> FILE_VISITOR_DELETE_DIR;
	private static final SimpleFileVisitor<Path> fileVisitorDeleteDir() {
		if((FILE_VISITOR_DELETE_DIR == null)) {
			FILE_VISITOR_DELETE_DIR = new SimpleFileVisitor<Path>() {
				
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
						throws IOException {
					// Delete the current file
					deleteFile(file);
					// Continue in walking
					return FileVisitResult.CONTINUE;
				}
				
				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc)
						throws IOException {
					// Delete the current directory
					deleteFile(dir);
					// Continue in walking
					return FileVisitResult.CONTINUE;
				}
			};
		}
		return FILE_VISITOR_DELETE_DIR;
	}
	
	public static final void deleteDir(Path dir)
			throws IOException {
		if(!exists(dir))
			// Do not try to delete non-existent directory, since it throws IOException
			return;
		// Delete all the directories and files in the directory
		Files.walkFileTree(dir, fileVisitorDeleteDir());
	}
	
	public static final void deleteFile(Path file)
			throws IOException {
		Files.deleteIfExists(file);
	}
	
	public static final void delete(Path path)
			throws IOException {
		if((isRegularFile(path))) deleteFile(path);
		else                      deleteDir (path);
	}
	
	public static final void move(Path src, Path dest)
			throws IOException {
		Files.move(src, dest);
	}
	
	/** @since 00.02.09 */
	public static final void moveForce(Path src, Path dest) throws IOException {
		try {
			Files.move(src, dest, REPLACE_EXISTING, ATOMIC_MOVE);
		} catch(AtomicMoveNotSupportedException ex) {
			Files.move(src, dest, REPLACE_EXISTING);
		}
	}
	
	public static final void download(String url, Path dest)
			throws IOException {
		URL urlObj = new URL(url);
		try(ReadableByteChannel in  = Channels.newChannel(urlObj.openStream());
			FileChannel         out = FileChannel.open(dest, OPTIONS_SAVE)) {
			out.transferFrom(in, 0L, Long.MAX_VALUE);
		}
	}
	
	private static final String fileSeparatorSys = File.separator;
	private static final String fileSeparatorOth = fileSeparatorSys.equals("\\") ? "/" : "\\";
	
	public static final String fixPath(String path) {
		return path.replace(fileSeparatorOth, fileSeparatorSys).trim();
	}
	
	public static final String unixSlashes(String path) {
		return path.replace('\\', '/');
	}
	
	/**
	 * Only for Windows operating system.*/
	public static final String getDriveName(String path) {
		path = fixPath(path);
		int index = path.indexOf(':');
		if((index > -1))
			return path.substring(0, index);
		return path;
	}
	
	public static final String getDirectory(String path) {
		path = fixPath(path);
		int index = path.lastIndexOf(fileSeparatorSys);
		if((index > -1))
			return path.substring(0, index);
		return path;
	}
	
	public static final String getFile(String path) {
		path = fixPath(path);
		int index = path.lastIndexOf(fileSeparatorSys);
		if((index > -1))
			return path.substring(index+1);
		return path;
	}
	
	public static final String getType(String path) {
		String name = getFile(path);
		int index = name.lastIndexOf('.');
		if((index > -1))
			return name.substring(index+1);
		return name;
	}
	
	public static final String getFileNoType(String path) {
		String name = getFile(path);
		int index = name.lastIndexOf('.');
		if((index > -1))
			return name.substring(0, index);
		return name;
	}
	
	private static List<Path> deleteOnExit;
	/** @since 00.02.08 */
	private static final List<Path> deleteOnExit() {
		if(deleteOnExit == null) {
			deleteOnExit = new ArrayList<>();
			Runtime.getRuntime().addShutdownHook(Threads.newThreadUnmanaged(NIO::runDeleteOnExit));
		}
		
		return deleteOnExit;
	}
	
	private static final void runDeleteOnExit() {
		for(Path path : deleteOnExit) {
			if((Files.isDirectory(path))) {
				try {
					deleteDir(path);
				} catch(IOException ex) {
				}
			} else {
				try {
					Files.deleteIfExists(path);
				} catch(IOException ex) {
				}
			}
		}
	}
	
	private static final Path deleteOnExit(Path path) {
		deleteOnExit().add(path);
		return path;
	}
	
	/** @since 00.02.08 */
	public static final Path uniqueFile(String prefix, String suffix) throws IOException {
		return uniqueFile(null, prefix, suffix);
	}
	
	/** @since 00.02.08 */
	public static final Path uniqueFile(Path dir, String prefix, String suffix) throws IOException {
		return dir == null ? Files.createTempFile(prefix, suffix) : Files.createTempFile(dir, prefix, suffix);
	}
	
	/** @since 00.02.08 */
	public static final Path uniqueDir(String prefix) throws IOException {
		return uniqueDir(null, prefix);
	}
	
	/** @since 00.02.08 */
	public static final Path uniqueDir(Path dir, String prefix) throws IOException {
		return dir == null ? Files.createTempDirectory(prefix) : Files.createTempDirectory(dir, prefix);
	}
	
	public static final Path tempFile(String prefix, String suffix) throws IOException {
		return tempFile(null, prefix, suffix);
	}
	
	public static final Path tempFile(Path dir, String prefix, String suffix) throws IOException {
		return deleteOnExit(uniqueFile(dir, prefix, suffix));
	}
	
	public static final Path tempDir(String prefix) throws IOException {
		return tempDir(null, prefix);
	}
	
	public static final Path tempDir(Path dir, String prefix) throws IOException {
		return deleteOnExit(uniqueDir(dir, prefix));
	}
	
	/** @since 00.02.00 */
	public static final void unmap(MappedByteBuffer buffer) throws IOException {
		if(buffer == null || !buffer.isDirect()) {
			return; // Nothing to unmap
		}
		
		try {
			// Since there is no official way to fully "close" a mapped byte
			// buffer that uses direct byte buffer, it is needed to call
			// Sun's internal methods to clean the leftovers. This ensures
			// that the file can be copied, moved, renamed, deleted, etc.
			// immediately after its file channel is closed.
			Method methodCleaner = Reflection.getMethod(buffer.getClass(), "cleaner");
			Object cleaner = Reflection.invokeMethod(methodCleaner, buffer);
			
			if(cleaner != null) {
				// Run the cleaner's clean() method
				Method methodClean = Reflection.getMethod(cleaner.getClass(), "clean");
				Reflection.invokeMethod(methodClean, cleaner);
			}
		} catch(Throwable th) {
			// Should not be done, but since it's an IO operation, it should throw IOException.
			throw new IOException(th);
		}
	}
	
	public static final void setPosixFilePermissions(Path path, Set<PosixFilePermission> perms)
			throws IOException {
		Files.setPosixFilePermissions(path, perms);
	}
	
	/** @since 00.02.08 */
	public static final void makeExecutable(Path path) throws IOException {
		File file = path.toFile();
		
		if(file.canExecute()) {
			return;
		}
		
		file.setExecutable(true, true);
	}
	
	/** @since 00.02.09 */
	public static final void transferTo(FileChannel ch, long position, long count, WritableByteChannel target)
			throws IOException {
		for(long num;
			count > 0L && (num = ch.transferTo(position, count, target)) >= 0L;
			count -= num, position += num)
		;
	}
	
	/** @since 00.02.09 */
	public static final void transferFrom(ReadableByteChannel src, FileChannel ch, long position, long count)
			throws IOException {
		for(long num;
			count > 0L && (num = ch.transferFrom(src, position, count)) >= 0L;
			count -= num, position += num)
		;
	}
	
	/** @since 00.02.09 */
	public static final ByteBuffer read(FileChannel ch, long position, long count) throws IOException {
		try(ByteBufferWritableByteChannel target = new ByteBufferWritableByteChannel((int) count)) {
			transferTo(ch, position, count, target);
			return target.toByteBuffer();
		}
	}
	
	/** @since 00.02.09 */
	private static final class ByteBufferWritableByteChannel
			extends ByteArrayOutputStream
			implements WritableByteChannel {
		
		private final byte[] temp = new byte[8192];
		private boolean open = true;
		
		public ByteBufferWritableByteChannel(int size) {
			super(size);
		}
		
		@Override
		public int write(ByteBuffer b) throws IOException {
			int num = 0;
			
			for(int len; (len = Math.min(b.remaining(), temp.length)) > 0; num += len) {
				b.get(temp, 0, len);
				write(temp, 0, len);
			}
			
			return num;
		}
		
		@Override
		public boolean isOpen() {
			return open;
		}
		
		@Override
		public void close() throws IOException {
			super.close();
			open = false;
		}
		
		public ByteBuffer toByteBuffer() {
			return ByteBuffer.wrap(buf, 0, count);
		}
	}
	
	/** @since 00.02.09 */
	public static final void write(FileChannel ch, long position, ByteBuffer buf) throws IOException {
		for(int num;
			buf.hasRemaining() && (num = ch.write(buf, position)) >= 0;
			position += num)
		;
	}
	
	/** @since 00.02.09 */
	private static final OpenOption[] OPEN_OPTIONS_TEMPORARY_CHANNEL = {
		StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE,
		StandardOpenOption.TRUNCATE_EXISTING
	};
	
	/** @since 00.02.09 */
	private static final void temporaryFileChannel(CheckedConsumer<FileChannel> action) throws IOException {
		Path temp = Files.createTempFile(null, null);
		
		try(FileChannel tempCh = FileChannel.open(temp, OPEN_OPTIONS_TEMPORARY_CHANNEL)) {
			action.accept(tempCh);
		} catch(IOException ex) {
			throw ex;
		} catch(Exception ex) {
			throw new IOException(ex);
		} finally {
			Files.deleteIfExists(temp);
		}
	}
	
	/** @since 00.02.09 */
	public static final void truncate(FileChannel ch, long position, long count) throws IOException {
		temporaryFileChannel((tempCh) -> {
			long copySize = ch.size() - position - count;
			transferTo(ch, position + count, copySize, tempCh);
			ch.truncate(position);
			tempCh.position(0L);
			transferFrom(tempCh, ch, position, copySize);
		});
	}
	
	/** @since 00.02.09 */
	public static final void replace(FileChannel ch, long position, long count, ByteBuffer buf) throws IOException {
		temporaryFileChannel((tempCh) -> {
			long copySize = ch.size() - position - count;
			transferTo(ch, position + count, copySize, tempCh);
			ch.truncate(position);
			tempCh.position(0L);
			int replaceSize = buf.remaining();
			write(ch, position, buf);
			transferFrom(tempCh, ch, position + replaceSize, copySize);
		});
	}
}