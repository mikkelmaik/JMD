package net.contra.jmd;

import net.contra.jmd.allatori.AllatoriTransformer;
import net.contra.jmd.dasho.DashOTransformer;
import net.contra.jmd.generic.*;
import net.contra.jmd.jshrink.JShrinkTransformer;
import net.contra.jmd.smokescreen.SmokeScreenTransformer;
import net.contra.jmd.zkm.ZKMTransformer;
import net.contra.jmd.util.*;

import java.util.Scanner;

/**
 * Created by IntelliJ IDEA.
 * User: Eric
 * Date: Nov 24, 2010
 * Time: 9:21:10 PM     
 */
public class Deobfuscator {
    public static boolean Debug = false;
    private static double version = 1.5;
    private static String credits = "skoalman, super_, ollie, popcorn89, the prophecy, and saevion";
    private static LogHandler logger = new LogHandler("Deobfuscator");
    //TODO: Load single class files... herpa derp
    public static void main(String[] argv) throws Exception {
        logger.message("Java Multi-Purpose Deobfuscator");
	    logger.message("Please Visit RSCBUnlocked.net for updates and info");
        logger.message("Version " + version);
        logger.message("Created by Contra");
        logger.message("Tons of code from " + credits);
        if (!(argv.length >= 3)) {
            logger.error("java -jar JMD.jar <file location> <type> <debug> <optional args>");
            logger.error("Example ZKM: java -jar JMD.jar \"C:/Files/Magic.jar\" zkm true");
            logger.error("Example StringScan: java -jar JMD.jar \"C:/Files/Magic.jar\" stringscanner \"rscheata.net\" false");
            logger.error("Example StringReplace: java -jar JMD.jar \"C:/Files/Magic.jar\" stringreplacer \"rscheata.net\" true \"rscbunlocked.net\"");
            return;
        }
        if (argv[2].equals("true")) {
            Debug = true;
        }
        if (argv[1].toLowerCase().equals("zkm")) {
            ZKMTransformer zt = new ZKMTransformer(argv[0]);
            zt.transform();
        } else if (argv[1].toLowerCase().equals("allatori")) {
            AllatoriTransformer at = new AllatoriTransformer(argv[0]);
            at.transform();
        } else if (argv[1].toLowerCase().equals("jshrink")) {
            JShrinkTransformer jt = new JShrinkTransformer(argv[0]);
            jt.transform();
	    } else if (argv[1].toLowerCase().equals("dasho")) {
            DashOTransformer dt = new DashOTransformer(argv[0]);
            dt.transform();
	    } else if (argv[1].toLowerCase().equals("smokescreen")) {
            SmokeScreenTransformer st = new SmokeScreenTransformer(argv[0]);
            st.transform();
	    } else if (argv[1].toLowerCase().equals("genericstringdeobfuscator")) {
            GenericStringDeobfuscator gsd = new GenericStringDeobfuscator(argv[0]);
            gsd.transform();
	    } else if (argv[1].toLowerCase().equals("stringfixer")) {
            StringFixer sf = new StringFixer(argv[0]);
            sf.transform();
	    } else if (argv[1].toLowerCase().equals("foreigncallremover")) {
            ForeignCallRemover fc = new ForeignCallRemover(argv[0]);
            fc.transform();
	    } else if (argv[1].toLowerCase().equals("stringscanner")) {
            StringScanner us = new StringScanner(argv[0], argv[3], false, "");
            us.scan();
        } else if (argv[1].toLowerCase().equals("stringreplacer")) {
            StringScanner us = new StringScanner(argv[0], argv[3], true, argv[4]);
            us.scan();
        } else {
            logger.error("Types are: ZKM, Allatori, JShrink, DashO, StringFixer, StringScanner, and StringReplacer (not case sensitive)");
        }
        Scanner in = new Scanner(System.in);
        logger.message("Press enter to exit...");
        in.nextLine();
    }
}
