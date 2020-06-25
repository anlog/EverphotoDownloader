package cc.ifnot.everphotodownloader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import cc.ifnot.libs.utils.Lg;

public class EverphotoDownloader {

    private Options options;
    private boolean verbose;
    private String mobile;
    private String password;
    private String smsCode;
    private String outDir;

    public static void main(String[] args) {
        new EverphotoDownloader().init(args);
    }

    private void init(String[] args) {
        Lg.level(EDConfig.DEBUG);
        options = new Options();
        options.addOption("h", "help", false, "show help");
        options.addOption("V", "version", false, "show version");
        options.addOption("v", "verbose", false, "verbose");

        final Option user = Option.builder("u").longOpt("user")
                .argName("username").desc("username mostly phone num")
                .hasArg(true).optionalArg(false).build();
        options.addOption(user);

        final Option pass = Option.builder("p").longOpt("password")
                .argName("password").desc("login password")
                .hasArg(true).optionalArg(false).build();
        options.addOption(pass);

        final Option sms = Option.builder("s").longOpt("sms")
                .argName("sms code").desc("sms code login mode")
                .hasArg(true).optionalArg(false).build();
        options.addOption(sms);

        final Option out = Option.builder("o").longOpt("output")
                .argName("output").desc("the output dir you may want")
                .hasArg(true).optionalArg(false).build();
        options.addOption(out);

        final DefaultParser parser = new DefaultParser();
        try {
            final CommandLine cmd = parser.parse(options, args);
            if (cmd.getArgs().length > 0) {
                for (String arg : cmd.getArgs()) {
                    Lg.e("%s is not valid");
                }
                printHelp();
            } else {
                if (cmd.hasOption("V")) {
                    printVersion();
                    return;
                } else if (cmd.hasOption("h")) {
                    printHelp();
                    return;
                }

                if (cmd.hasOption("v")) {
                    verbose = true;
                }

                mobile = cmd.getOptionValue("u");
                if (mobile == null || mobile.length() == 0) {
                    Lg.e("user of %s is invalid", user);
                    printHelp();
                    return;
                }
                password = cmd.getOptionValue("p");
                smsCode = cmd.getOptionValue("s");

                if ((password == null || password.length() == 0) &&
                        (smsCode == null || smsCode.length() == 0)) {
                    Lg.e("make sure password or sms code is provided");
                    printHelp();
                    return;
                }
                outDir = cmd.getOptionValue("o");
                if (outDir == null || outDir.length() == 0) {
                    Lg.w("out is not offered, will use current out");
                    outDir = "out";
                }
                // todo job
            }

        } catch (ParseException e) {
//            e.printStackTrace();
            Lg.e(e.getMessage());
        }
    }

    private void printVersion() {
        System.out.println(String.format("%s%s v%s", EDConfig.NAME,
                EDConfig.DEBUG <= Lg.DEBUG ? "-debug" : "",
                EDConfig.VERSION));
        System.out.println("Copyright (C) 2020");
        System.out.println("hello@ifnot.cc");
    }

    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(EDConfig.NAME, options);
        printVersion();
    }
}