module sune.app.mediadown {
	// Internal modules
	requires java.desktop;
	requires java.scripting;
	requires transitive jdk.unsupported;
	requires transitive javafx.controls;
	requires javafx.base;
	requires javafx.graphics;
	requires javafx.web;
	requires transitive java.logging;
	requires java.datatransfer;
	// External modules
	requires sune.util.load;
	requires transitive sune.memory;
	requires transitive ssdf2;
	requires transitive sune.api.process;
	requires infomas.asl;
	requires transitive org.jsoup;
	// Exports
	exports sune.app.mediadown;
	exports sune.app.mediadown.configuration;
	exports sune.app.mediadown.convert;
	exports sune.app.mediadown.download;
	exports sune.app.mediadown.download.segment;
	exports sune.app.mediadown.engine;
	exports sune.app.mediadown.event;
	exports sune.app.mediadown.event.tracker;
	exports sune.app.mediadown.ffmpeg;
	exports sune.app.mediadown.gui;
	exports sune.app.mediadown.gui.control;
	exports sune.app.mediadown.gui.form;
	exports sune.app.mediadown.gui.form.field;
	exports sune.app.mediadown.gui.table;
	exports sune.app.mediadown.gui.window;
	exports sune.app.mediadown.initialization;
	exports sune.app.mediadown.language;
	exports sune.app.mediadown.library;
	exports sune.app.mediadown.logging;
	exports sune.app.mediadown.manager;
	exports sune.app.mediadown.media;
	exports sune.app.mediadown.media.format;
	exports sune.app.mediadown.media.type;
	exports sune.app.mediadown.message;
	exports sune.app.mediadown.pipeline;
	exports sune.app.mediadown.plugin;
	exports sune.app.mediadown.registry;
	exports sune.app.mediadown.resource;
	exports sune.app.mediadown.resource.cache;
	exports sune.app.mediadown.search;
	exports sune.app.mediadown.server;
	exports sune.app.mediadown.theme;
	exports sune.app.mediadown.update;
	exports sune.app.mediadown.util;
}