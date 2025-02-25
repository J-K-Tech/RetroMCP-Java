package org.mcphackers.mcp.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tools.FileUtil;

public class PluginManager {
	private final Map<String, MCPPlugin> loadedPlugins = new HashMap<>();

	public Map<String, MCPPlugin> getLoadedPlugins() {
		return this.loadedPlugins;
	}

	public void discoverPlugins(MCP mcp) {
		ClassLoader classLoader = mcp.getClass().getClassLoader();
		try (PluginClassLoader pluginClassLoader = new PluginClassLoader(classLoader)) {
			Path workingDir = mcp.getWorkingDir();
			if (workingDir == null) {
				workingDir = Paths.get(".");
			}
			Path pluginsDir = workingDir.resolve("plugins");
			if (Files.exists(pluginsDir) && Files.isDirectory(pluginsDir)) {
				List<Path> pluginCandidates = FileUtil.getPathsOfType(pluginsDir, ".jar", ".zip");
				for (Path pluginCandidate : pluginCandidates) {
					System.out.println("Adding " + pluginCandidate.getFileName() + " to classpath!");
					pluginClassLoader.addURL(pluginCandidate.toUri().toURL());
				}
			}

			ServiceLoader<MCPPlugin> serviceLoader = ServiceLoader.load(MCPPlugin.class, pluginClassLoader);
			for (MCPPlugin plugin : serviceLoader) {
				plugin.init(mcp);
				this.loadedPlugins.put(plugin.pluginId(), plugin);

				// Load translations from plugins
				MCP.TRANSLATOR.readTranslation(plugin.getClass());
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
