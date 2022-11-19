package sune.app.mediadown.util;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import sune.app.mediadown.Shared;

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
	
	/**
	 * More forceful method of moving a file from {@code src} to {@code dest}.
	 * It tries to replace the file, if it already exists, and ensure that
	 * the move is atomic. It does not copy the attributes, since it is not
	 * supported on all platforms (e.g. Windows).
	 * Throwing {@linkplain IOException} if the file cannot be moved.
	 * @param src the path to the file to move
	 * @param dest the path to the target file
	 * @see Files#move(Path, Path, java.nio.file.CopyOption...)*/
	public static final void move_force(Path src, Path dest)
			throws IOException {
		Files.move(src, dest, REPLACE_EXISTING, ATOMIC_MOVE);
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
	public static final void unmap(MappedByteBuffer buffer)
			throws NoSuchMethodException,
			       SecurityException,
			       NoSuchFieldException,
			       IllegalArgumentException,
			       IllegalAccessException,
			       InvocationTargetException {
		if((buffer != null && buffer.isDirect())) {
			// Since there is no official way to fully "close" a mapped byte
			// buffer that uses direct byte buffer, it is needed to call
			// Sun's internal methods to clean the leftovers. This ensures
			// that the file can be copied, moved, renamed, deleted, etc.
			// immediately after its file channel is closed.
			Method methodCleaner = buffer.getClass().getMethod("cleaner");
			Reflection.setAccessible(methodCleaner, true);
			// Get the cleaner instance
			Object objectCleaner = methodCleaner.invoke(buffer);
			if((objectCleaner != null)) {
				// Run the cleaner clean() method
				Class<?> classCleaner = objectCleaner.getClass();
				Method   methodClean  = classCleaner .getMethod("clean");
				Reflection.setAccessible(methodClean, true);
				methodClean.invoke(objectCleaner);
			}
		}
	}
	
	public static final void setPosixFilePermissions(Path path, Set<PosixFilePermission> perms)
			throws IOException {
		Files.setPosixFilePermissions(path, perms);
	}
	
	private static final PosixFilePermission[] POSIX_PERMISSIONS = {
		PosixFilePermission.OWNER_EXECUTE,
		PosixFilePermission.GROUP_EXECUTE,
		PosixFilePermission.OTHERS_EXECUTE,
		PosixFilePermission.OWNER_WRITE,
		PosixFilePermission.GROUP_WRITE,
		PosixFilePermission.OTHERS_WRITE,
		PosixFilePermission.OWNER_READ,
		PosixFilePermission.GROUP_READ,
		PosixFilePermission.OTHERS_READ
	};
	
	private static final void addPosixPermission(Set<PosixFilePermission> permissions, int index) {
		if((index >= 0))
			permissions.add(POSIX_PERMISSIONS[index]);
	}
	
	private static final Set<PosixFilePermission> chmodPermissions(int owner, int group, int others) {
		if((owner < 0 || owner > 7 || group < 0 || group > 7 || others < 0 || others > 7))
			throw new IllegalArgumentException();
		Set<PosixFilePermission> permissions = new HashSet<>();
		addPosixPermission(permissions, ((owner  & 1) << 0) - 1);
		addPosixPermission(permissions, ((owner  & 2) << 1) - 1);
		addPosixPermission(permissions, ((owner  & 4) << 1) - 2);
		addPosixPermission(permissions, ((group  & 1) << 1) - 1);
		addPosixPermission(permissions, ((group  & 2) << 2) - 4);
		addPosixPermission(permissions, ((group  & 4) << 1) - 1);
		addPosixPermission(permissions, ((others & 1) << 2) - 2);
		addPosixPermission(permissions, ((others & 2) << 2) - 3);
		addPosixPermission(permissions, ((others & 4) << 2) - 8);
		return permissions;
	}
	
	public static final void chmod(Path path, int owner, int group, int others)
			throws IOException {
		setPosixFilePermissions(path, chmodPermissions(owner, group, others));
	}
}