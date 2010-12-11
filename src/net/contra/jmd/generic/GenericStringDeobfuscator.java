/*
	TODO: Write a dynamic string decryptor like the one in SAE; Pattern is LDC INVOKESTATIC,                     
	grab the IL from the invokestatic method and put it in a new methodgen and run the LDC through that and replace it with the result
	Please see http://www.java-tips.org/java-se-tips/java.lang.reflect/invoke-method-using-reflection.html

	Also ask somebody how you can trace the stack for stringfixer, foreigncallremover
	AND to get the key, check the method arguments. If it is just string pass the string, otherwise grab the integer above it.
	If there is more leave it be or attempt to grab the values
	
	*/
package net.contra.jmd.generic;

import net.contra.jmd.util.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.jar.*;

/**
 * Created by IntelliJ IDEA.
 * User: Eric
 * Date: Nov 30, 2010
 * Time: 4:52:48 AM
 */
public class GenericStringDeobfuscator {
	private static LogHandler logger = new LogHandler("GenericStringDeobfuscator");
	private Map<String, ClassGen> cgs = new HashMap<String, ClassGen>();
	String JAR_NAME;

	public GenericStringDeobfuscator(String jarfile) throws Exception {
		File jar = new File(jarfile);
		JAR_NAME = jarfile;
		JarFile jf = new JarFile(jar);
		Enumeration<JarEntry> entries = jf.entries();
		while(entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			if(entry == null) {
				break;
			}
			if(entry.isDirectory()) {
			}
			if(entry.getName().endsWith(".class")) {
				ClassGen cg = new ClassGen(new ClassParser(jf.getInputStream(entry), entry.getName()).parse());
				cgs.put(cg.getClassName(), cg);
			} else {
				NonClassEntries.add(entry, jf.getInputStream(entry));
			}
		}
	}

	public void replaceStrings() {
		for(ClassGen cg : cgs.values()) {
			int replaced = 0;
			for(Method method : cg.getMethods()) {
				if(method.isAbstract() || method.isNative()) {
					continue;
				}
				MethodGen mg = new MethodGen(method, cg.getClassName(), cg.getConstantPool());
				InstructionList list = mg.getInstructionList();
				if(list == null) {
					continue;
				}
				InstructionHandle[] handles = list.getInstructionHandles();
				for(int i = 1; i < handles.length; i++) {
					//java.lang.reflect.Method
					if(GenericMethods.isCall(handles[i].getInstruction()) && handles[i - 1].getInstruction() instanceof LDC) {
						String methodCallClass = GenericMethods.getCallClassName(handles[i].getInstruction(), cg.getConstantPool());
						String methodCallMethod = GenericMethods.getCallMethodName(handles[i].getInstruction(), cg.getConstantPool());
						String methodCallSig = GenericMethods.getCallSignature(handles[i].getInstruction(), cg.getConstantPool());
						if(GenericMethods.getCallArgTypes(handles[i].getInstruction(), cg.getConstantPool()).length == 1
								&& methodCallClass != null && methodCallMethod != null) {
							//Begin classloader bullshit
							GenericClassLoader loader = new GenericClassLoader(GenericStringDeobfuscator.class.getClassLoader());
							Class cl = null;
							if(cgs.containsKey(methodCallClass)
									&& cgs.get(methodCallClass).containsMethod(methodCallMethod, methodCallSig) != null) {
								if(!cgs.get(methodCallClass).isAbstract() && !cgs.get(methodCallClass).isNative()) {
									byte[] bit = cgs.get(methodCallClass).getJavaClass().getBytes();
									cl = loader.loadClass(methodCallClass, bit);
									//TODO: Getting a classnotfound here. FIX IT!!!
								} else {
									continue;
								}
							} else {
								continue;
							}
							if(cl == null) {
								continue;
							}
							java.lang.reflect.Method mthd = null;
							try {
								//mthd = cl.getMethod(methodCallSig, String.class);
								//TODO: Figure out a better way to find the method (method signature?) because method names can be the same
								mthd = cl.getMethod(methodCallMethod, String.class);
							} catch(NoSuchMethodException e) {
								//e.printStackTrace();
								//logger.error("Tried to access a method that wasn't loaded");
								continue;
							}

							LDC encryptedLDC = (LDC) handles[i - 1].getInstruction();
							String encryptedString = encryptedLDC.getValue(cg.getConstantPool()).toString();
							String decryptedString = null;
							try {
								decryptedString = mthd.invoke(null, encryptedString).toString();
							} catch(IllegalAccessException e) {
								//e.printStackTrace();
								continue;
							} catch(InvocationTargetException e) {
								//e.printStackTrace();
								continue;
							}
							logger.debug(encryptedString + " -> " + decryptedString + " in " + cg.getClassName() + "." + method.getName());
							int stringRef = cg.getConstantPool().addString(decryptedString);
							LDC lc = new LDC(stringRef);
							NOP nop = new NOP();
							handles[i].setInstruction(lc);
							handles[i - 1].setInstruction(nop);
							replaced++;
						}
					}
				}
				mg.setInstructionList(list);
				mg.setMaxLocals();
				mg.setMaxStack();
				cg.replaceMethod(method, mg.getMethod());
			}
			if(replaced > 0) {
				logger.debug("decrypted " + replaced + " strings in class " + cg.getClassName());
			}
		}
	}

	public void transform() {
		logger.log("Generic String Deobfuscator");
		logger.log("Still basic (and very buggy)");
		replaceStrings();
		logger.log("Deobfuscation finished! Dumping jar...");
		GenericMethods.dumpJar(JAR_NAME, cgs.values());
		logger.log("Operation Completed.");

	}
}