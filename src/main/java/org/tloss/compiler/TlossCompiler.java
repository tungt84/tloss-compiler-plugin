package org.tloss.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.Properties;

import org.tloss.compiler.soot.CompileClassHelper;
import org.tloss.compiler.soot.TransformHelper;

import soot.Main;

public class TlossCompiler {
	public final String JAVA_COMPILER_HANDLER_CLASS = "JAVA_COMPILER_HANDLER_CLASS";
	protected Properties config;
	protected JavaCompilerHandler defaultHandler;

	public Properties init(String configFile) throws Exception {
		config = new Properties();
		FileInputStream fileInputStream = new FileInputStream(new File(configFile));
		config.load(fileInputStream);
		if (config.containsKey(JAVA_COMPILER_HANDLER_CLASS)) {
			Class<?> c = Class.forName(config.getProperty(JAVA_COMPILER_HANDLER_CLASS));
			defaultHandler = (JavaCompilerHandler) c.newInstance();
			defaultHandler.init(config);
		}
		fileInputStream.close();
		return config;
	}

	public Properties getConfig() {
		return config;
	}

	public JavaCompilerHandler getDefaultHandler() {
		return defaultHandler;
	}

	public BuildResult buildSource() throws Exception {
		return buildSource(defaultHandler);
	}

	public BuildResult buildSource(JavaCompilerHandler handler) throws Exception {
		return handler.compile();
	}

	public void runSoot(BuildResult result) throws Exception {
		// Scene.v().addBasicClass("org.Test4",SootClass.HIERARCHY);
		String[] args = new String[] { "-cp", result.getClassPath(), "--plugin", "plugin.xml", "-p", "jap.foo",
				"enabled:true", "-p", "jb", "use-original-names:true", "-pp", "-process-dir", result.getClassFolder(),
				"-f", "jimple" };
		System.out.println("process-dir: " + result.getClassPath());
		System.out.println("classpath: " + result.getClassFolder());
		Main.main(args);
		for (java.util.Map.Entry<String, Path> entry : TransformHelper.classNameMapping.entrySet()) {
			File dir = entry.getValue().toFile();
			
			final FileOutputStream fileOutputStream = new FileOutputStream(
					config.getProperty("COMPILE_OUT_FOLDER","")+
					entry.getKey() + "." + config.getProperty("COMPILE_EXT_FILE", "tmp"));
			CompileClassHelper classHelper = (CompileClassHelper)Class.forName(config.getProperty("COMPILE_CLASS_HELPER_CLASS")).newInstance();
			classHelper.setConfig(config);
			classHelper.compilePreClass(fileOutputStream,entry.getKey());
			dir.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					if (pathname.isFile()) {
						BufferedReader br = null;
						FileReader fr = null;
						try {

							String sCurrentLine;
							fr = new FileReader(pathname);
							br = new BufferedReader(fr);
							while ((sCurrentLine = br.readLine()) != null) {
								fileOutputStream.write((sCurrentLine + "\n").getBytes("utf-8"));
							}

						} catch (Exception e) {

						} finally {
							try {
								if (fr != null)
									fr.close();
							} catch (Exception e) {

							}
							try {
								if (br != null)
									br.close();
							} catch (Exception e) {

							}

						}

					}
					return false;
				}
			});
			classHelper.compileSubClass(fileOutputStream, entry.getKey());
			fileOutputStream.flush();
			fileOutputStream.close();
			dir.delete();
		}
	}

	public static void main(String[] args) throws Exception {
		TlossCompiler compiler = new TlossCompiler();
		compiler.init("compile.properties");
		BuildResult buildResult = compiler.buildSource();
		compiler.runSoot(buildResult);

	}
}
