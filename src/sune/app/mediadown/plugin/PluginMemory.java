package sune.app.mediadown.plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import sune.util.memory.ManagedMemory;
import sune.util.memory.Memory;
import sune.util.memory.MemoryPointer;

public class PluginMemory extends ManagedMemory {
	
	public static class MemoryFile {
		
		private final MemoryPointer pointer;
		private final String path;
		private final String name;
		
		public MemoryFile(MemoryPointer pointer, String path, String name) {
			this.pointer = pointer;
			this.path    = path;
			this.name    = name;
		}
		
		public MemoryPointer getPointer() {
			return pointer;
		}
		
		public String getPath() {
			return path;
		}
		
		public String getName() {
			return name;
		}
		
		public int getSize() {
			return pointer.length;
		}
	}
	
	private final Map<Long, MemoryFile> files;
	
	public PluginMemory(Memory memory) {
		super(memory);
		this.files = new HashMap<>();
	}
	
	public MemoryFile getFile(long address) {
		return files.get(address);
	}
	
	public MemoryFile getFile(String path) {
		for(MemoryFile file : files.values()) {
			if((file.getPath().equals(path)))
				return file;
		}
		return null;
	}
	
	public void addFile(MemoryFile file) {
		MemoryPointer pointer = file.getPointer();
		files.put(pointer.address, file);
		addPointer(pointer);
	}
	
	public void getFileContent(MemoryFile file, OutputStream os) throws IOException {
		long ppos = getPosition();
		MemoryPointer pointer = file.getPointer();
		long addr = pointer.address;
		int  size = pointer.length;
		int  clen = 0, rlen;
		byte[] buf = new byte[8192];
		position(addr);
		while((clen < size)) {
			rlen = Math.min(size - clen, 8192);
			get(buf, 0, rlen);
			os.write(buf, 0, rlen);
			clen += rlen;
		}
		position(ppos);
	}
	
	public Collection<MemoryFile> files() {
		return Collections.unmodifiableCollection(files.values());
	}
}