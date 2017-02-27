package janala.instrument;

import janala.config.Config;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;

public class SnoopInstructionTransformer implements ClassFileTransformer {
  private boolean writeInstrumentedClasses = true;
  private String instDir = "instrumented";
  public SnoopInstructionTransformer() {
    writeInstrumentedClasses = Config.instance.writeInstrumentedClasses;
    instDir = "instrumented";
  }
  
  private static Collection<String> banned;
  private static Collection<String> excludes;
  private static Collection<String> includes;
  
  public static void premain(String agentArgs, Instrumentation inst) {
    Coverage.read(Config.instance.coverage);
    banned = Arrays.asList("java/lang", "com/sun", "janala", "org/objectweb/asm", "sun", "jdk");
    excludes = Arrays.asList(Config.instance.excludeInst);
    includes = Arrays.asList(Config.instance.includeInst);
    inst.addTransformer(new SnoopInstructionTransformer());
  }

  /** packages that should be exluded from the instrumentation */
  private static boolean shouldExclude(String cname) {
    for (String e : banned) {
      if (cname.startsWith(e)) {
        return true;
      }
    }
    for (String e : includes) {
      if (cname.startsWith(e)) {
        return false;
      }
    }
    for (String e : excludes) {
      if (cname.startsWith(e)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public byte[] transform(ClassLoader loader, String cname, Class<?> classBeingRedefined,
      ProtectionDomain d, byte[] cbuf)
    throws IllegalClassFormatException {

    if (classBeingRedefined != null) {
      return cbuf;
    }

    boolean toInstrument = !shouldExclude(cname);

    if (toInstrument) {
      // System.out.println("Instrumenting: " + cname);
      GlobalStateForInstrumentation.instance.setCid(Coverage.instance.getCid(cname));
      ClassReader cr = new ClassReader(cbuf);
      ClassWriter cw = new SafeClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
      ClassVisitor cv = new SnoopInstructionClassAdapter(cw);

      try {
        cr.accept(cv, 0);
      } catch (Exception e) {
        e.printStackTrace();
      }

      byte[] ret = cw.toByteArray();
      if (writeInstrumentedClasses) {
        try {
          File file = new File(instDir + "/" + cname + ".class");
          File parent = new File(file.getParent());
          parent.mkdirs();
          FileOutputStream out = new FileOutputStream(file);
          out.write(ret);
          out.close();
        } catch(Exception e) {
          e.printStackTrace();
        }
      }
      return ret;
    } else {
      return cbuf;
    }
  }
}
