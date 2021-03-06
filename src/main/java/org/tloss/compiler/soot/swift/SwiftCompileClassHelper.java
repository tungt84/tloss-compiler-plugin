package org.tloss.compiler.soot.swift;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.tloss.compiler.soot.CompileClassHelper;

public class SwiftCompileClassHelper implements CompileClassHelper {

	public void compilePreClass(OutputStream outputStream, String className) throws IOException {
		if(SwiftTransformHelper.classDeclare.get(className)!=null) {
			if(config.getProperty("SWIFT_IMPORT")!=null) {
				String[] imports = config.getProperty("SWIFT_IMPORT").split(",");
				for(int i=0;i<imports.length;i++) {
					outputStream.write(("import "+imports[i]+"\n").getBytes("utf-8"));
				}
			}
			outputStream.write(SwiftTransformHelper.classDeclare.get(className).getBytes("utf-8"));
		}
		//outputStream.write(("class " + className.replace('.', '_') + "{\n").getBytes("utf-8"));
	}

	public void compileSubClass(OutputStream outputStream, String className) throws IOException {
		outputStream.write("}\n".getBytes("utf-8"));

	}
	Properties config;
	public void setConfig(Properties config) {
		this.config =  config;
		
	}

}
