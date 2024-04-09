package sune.app.mediadown.tor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

import sune.app.mediadown.util.Regex;

/** @since 00.02.10 */
public final class TorCircuit {
	
	private static final Regex regex = Regex.of("^(?<id>\\d+) (?<status>[^ ]+) (?<path>[^ ]+).*$");
	
	private final int id;
	private final String status;
	private final List<TorCircuit.Node> nodes;
	
	private TorCircuit(int id, String status, List<TorCircuit.Node> nodes) {
		this.id = id;
		this.status = status;
		this.nodes = nodes;
	}
	
	public static final TorCircuit parse(String line) {
		Matcher matcher = regex.matcher(line);
		
		if(!matcher.matches()) {
			return null;
		}
		
		int id = Integer.valueOf(matcher.group("id"));
		String status = matcher.group("status");
		String path = matcher.group("path");
		
		List<TorCircuit.Node> nodes = new ArrayList<>();
		StringTokenizer tokenizer = new StringTokenizer(path, ",");
		
		while(tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			nodes.add(Node.parse(token));
		}
		
		return new TorCircuit(id, status, List.copyOf(nodes));
	}
	
	public int id() {
		return id;
	}
	
	public String status() {
		return status;
	}
	
	public List<TorCircuit.Node> nodes() {
		return nodes;
	}
	
	public static final class Node {
		
		private final String identity;
		private final String nickname;
		
		private Node(String identity, String nickname) {
			this.identity = Objects.requireNonNull(identity);
			this.nickname = nickname;
		}
		
		public static final TorCircuit.Node parse(String string) {
			String identity = string;
			String nickname = null;
			
			int index;
			if((index = string.indexOf('~')) > 0) {
				identity = string.substring(0, index);
				nickname = string.substring(index + 1);
			}
			
			return new Node(identity, nickname);
		}
		
		public String identity() {
			return identity;
		}
		
		public String nickname() {
			return nickname;
		}
	}
}