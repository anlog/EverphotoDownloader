package cc.ifnot.everphotodownloader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.concurrent.CountDownLatch;

import cc.ifnot.libs.everphoto.EverPhoto;
import cc.ifnot.libs.utils.Lg;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class EverphotoDownloader {

    private Options options;

    public static void main(String[] args) {
        new EverphotoDownloader().init(args);
    }

    private void init(String[] args) {
        Lg.level(EDConfig.DEBUG);
        options = new Options();
        options.addOption("h", "help", false, "show help");
        options.addOption("V", "version", false, "show version");
        options.addOption("v", "verbose", false, "verbose");

        options.addOption("c", "check", false, "check local md5 or not");
        options.addOption("e", "error", false, "download error only, file list in out/.err");

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

        final Option threadsCount = Option.builder("t").longOpt("thread-count")
                .argName("thread count").desc("the thread amount to download")
                .hasArg(true).optionalArg(false).build();
        options.addOption(threadsCount);

        final DefaultParser parser = new DefaultParser();
        try {
            final CommandLine cmd = parser.parse(options, args);
            if (cmd.getArgs().length > 0) {
                for (String arg : cmd.getArgs()) {
                    Lg.e("%s is not valid");
                }
                printHelp();
            } else {

                if (cmd.getOptions().length == 0) {
                    printHelp();
                    return;
                }
                if (cmd.hasOption("V")) {
                    printVersion();
                    return;
                } else if (cmd.hasOption("h")) {
                    printHelp();
                    return;
                }

                boolean verbose = false;
                String mobile;
                String password;
                String smsCode;
                String outDir;
                int threads;

                if (cmd.hasOption("v")) {
                    verbose = true;
                }

                boolean c = cmd.hasOption("c");
                boolean e = cmd.hasOption("e");

                mobile = cmd.getOptionValue("u");
                if (mobile == null || mobile.length() == 0) {
                    if (!e) {
                        Lg.e("user of %s is invalid", mobile);
                        printHelp();
                        return;
                    }
                }
                password = cmd.getOptionValue("p");
                smsCode = cmd.getOptionValue("s");

                if ((password == null || password.length() == 0) &&
                        (smsCode == null || smsCode.length() == 0)) {
                    if (!e) {
                        Lg.e("make sure password or sms code is provided");
                        printHelp();
                        return;
                    }
                }
                outDir = cmd.getOptionValue("o");
                if (outDir == null || outDir.length() == 0) {
                    Lg.w("out is not offered, will use current out");
                    outDir = "out";
                }

                String threadsValue = cmd.getOptionValue("o");
                threads = Runtime.getRuntime()
                        .availableProcessors() + 1;
                if (threadsValue == null || threadsValue.length() == 0) {
                    Lg.w("threads is not offered, will use default %d", threads);
                } else {
                    try {
                        threads = Integer.parseInt(threadsValue);
                    } catch (Exception ignored) {
                    }
                }

                CountDownLatch latch = new CountDownLatch(1);
                @NonNull final Disposable disposable = EverPhoto.INSTANCE.setVerbose(verbose)
                        .setMobile(mobile)
                        .setPassword(password)
                        .setOut(outDir)
                        .setThreadCount(threads)
                        .setSmsCode(smsCode)
                        .setCheckLocal(c)
                        .setDownlocalErrOnly(e)
                        .doDownload().observeOn(Schedulers.io())
                        .subscribe(s -> {
                                    Lg.d("ED: %s", s);
                                    latch.countDown();
                                }
                                , throwable -> {
                                    Lg.d(throwable);
                                    latch.countDown();
                                });
                while (!disposable.isDisposed()) {
                    try {
                        latch.await();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    } finally {
                        if (!disposable.isDisposed()) {
                            ThreadGroup currentGroup =
                                    Thread.currentThread().getThreadGroup();
                            int noThreads = currentGroup.activeCount();
                            Thread[] lstThreads = new Thread[noThreads];
                            currentGroup.enumerate(lstThreads);
                            for (int i = 0; i < noThreads; i++) {
                                if (lstThreads[i] != null &&
                                        lstThreads[i].getName().contains("pool")) {
                                    try {
                                        lstThreads[i].join();
                                    } catch (InterruptedException ex) {
                                    }
                                }
                            }
                        }
                    }
                    //wait
                }
            }

        } catch (ParseException e) {
//            e.printStackTrace();
            Lg.e(e.getMessage());
        }
    }

    private void printVersion() {
        System.out.println();
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