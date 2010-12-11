package net.contra.jmd.jshrink;

import net.contra.jmd.util.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * Created by IntelliJ IDEA.
 * User: Eric
 * Date: Nov 28, 2010
 * Time: 2:43:03 PM
 */
public class JShrinkTransformer {
	private static LogHandler logger = new LogHandler("JShrinkTransformer");
	private Map<String, ClassGen> cgs = new HashMap<String, ClassGen>();
	private Map<Integer, String> jStrings = new HashMap<Integer, String>();
	String JAR_NAME;
	ClassGen LoaderClass;

	public String getJString(int index) {
		return jStrings.get(index);
	}

	public boolean isLoader(ClassGen cg) {
		if(cg.getMethods().length == 3 && cg.getMethods()[1].isStatic()
				&& cg.getMethods()[1].isFinal()
				&& cg.getMethods()[1].isPublic()
				&& cg.getMethods()[1].isSynchronized()
				&& cg.getMethods()[1].getReturnType().toString().equals("java.lang.String")) {
			return true;
		}
		return false;
	}

	public JShrinkTransformer(String jarfile) throws Exception {
		File jar = new File(jarfile);
		JAR_NAME = jarfile;
		JarFile jf = new JarFile(jar);
		Enumeration<JarEntry> entries = jf.entries();
		while(entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			if(entry == null) {
				break;
			}
			if(entry.getName().endsWith(".class")) {
				ClassGen cg = new ClassGen(new ClassParser(jf.getInputStream(entry), entry.getName()).parse());
				String className = entry.getName().replace(".class", "").replace("\\", "/").replace("/", ".");
				if(isLoader(cg)) {
					logger.debug("Found JShrink Loader! " + cg.getClassName());
					LoaderClass = cg;
				} else {
					cgs.put(className, cg);
				}
			} else {
				if(!entry.isDirectory()) {
					NonClassEntries.add(entry, jf.getInputStream(entry));
				}
			}
		}
		logger.debug("Classes loaded from JAR");
	}

	public void replaceStrings() throws TargetLostException {
		for(ClassGen cg : cgs.values()) {
			int replaced = 0;
			for(Method method : cg.getMethods()) {
				MethodGen mg = new MethodGen(method, cg.getClassName(), cg.getConstantPool());
				InstructionList list = mg.getInstructionList();
				if(list == null) {
					continue;
				}
				InstructionHandle[] handles = list.getInstructionHandles();
				for(int i = 1; i < handles.length; i++) {
					if((handles[i].getInstruction() instanceof INVOKESTATIC)
							&& ((handles[i - 1].getInstruction() instanceof BIPUSH)
							|| (handles[i - 1].getInstruction() instanceof SIPUSH)
							|| (handles[i - 1].getInstruction() instanceof ICONST)
							|| (handles[i - 1].getInstruction() instanceof LDC_W))) {
						INVOKESTATIC methodCall = (INVOKESTATIC) handles[i].getInstruction();
						if(methodCall.getClassName(cg.getConstantPool()).contains(LoaderClass.getClassName())) {
							int push;
							if(handles[i - 1].getInstruction() instanceof BIPUSH) {
								push = ((BIPUSH) handles[i - 1].getInstruction()).getValue().intValue();
							} else if(handles[i - 1].getInstruction() instanceof SIPUSH) {
								push = ((SIPUSH) handles[i - 1].getInstruction()).getValue().intValue();
							} else if(handles[i - 1].getInstruction() instanceof LDC_W) {
								push = Integer.parseInt(((LDC_W) handles[i - 1].getInstruction()).getValue(cg.getConstantPool()).toString());
							} else {
								push = ((ICONST) handles[i - 1].getInstruction()).getValue().intValue();
							}

							String decryptedString = StoreHandler.I(push);
							if(decryptedString != null) {
								int stringRef = cg.getConstantPool().addString(decryptedString);
								LDC lc = new LDC(stringRef);
								NOP nop = new NOP();
								handles[i].setInstruction(lc);
								handles[i - 1].setInstruction(nop);
								replaced++;
							}
						}
					}
				}
				mg.setInstructionList(list);
				mg.removeNOPs();
				mg.setMaxLocals();
				mg.setMaxStack();
				cg.replaceMethod(method, mg.getMethod());
			}
			if(replaced > 0) {
				logger.debug("replaced " + replaced + " calls in class " + cg.getClassName());
			}
		}
	}


	public void transform() {
		try {
			logger.log("Starting String Replacer...");
			replaceStrings();
		} catch(TargetLostException e) {
			e.printStackTrace();
		}
		logger.log("Deobfuscation Finished! Dumping jar...");
		GenericMethods.dumpJar(JAR_NAME, cgs.values());
		logger.log("Operation Completed.");
	}
}