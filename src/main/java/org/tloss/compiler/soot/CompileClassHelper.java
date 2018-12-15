package org.tloss.compiler.soot;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

public interface CompileClassHelper {
	public void setConfig(Properties config);
	public void compilePreClass(OutputStream outputStream, String className) throws IOException;

	public void compileSubClass(OutputStream outputStream, String className) throws IOException;
}
