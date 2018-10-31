package org.tloss.compiler;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import soot.JimpleClassProvider;
import soot.JimpleClassSource;
import soot.Main;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.parser.JimpleAST;
import soot.jimple.parser.Parse;

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
				"enabled:true", "-p", "jb", "use-original-names:true", "-pp", "-process-dir", result.getClassFolder(), "-f",
				"jimple" };
		System.out.println("process-dir: " + result.getClassPath());
		System.out.println("classpath: " + result.getClassFolder());
		Main.main(args);

	}

	public static void main(String[] args) throws Exception {
		TlossCompiler compiler = new TlossCompiler();
		compiler.init("compile.properties");
		BuildResult buildResult = compiler.buildSource();
		compiler.runSoot(buildResult);

	}
}
