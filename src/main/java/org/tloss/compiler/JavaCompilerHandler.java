package org.tloss.compiler;

import java.util.Properties;

public abstract interface JavaCompilerHandler {

	public abstract void init(Properties properties) throws Exception;

	public abstract BuildResult compile() throws Exception;
}
