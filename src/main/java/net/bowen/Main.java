package net.bowen;

import net.bowen.gui.Window;
import org.apache.commons.cli.*;

public class Main {
    public static void main(String[] args) {
        Options options = getOptions();

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
            return;
        }

        String resolution = cmd.getOptionValue("resolution", "500:300");
        String[] res = resolution.split(":");
        int width = Integer.parseInt(res[0]);
        int height = Integer.parseInt(res[1]);

        int sceneID = Integer.parseInt(cmd.getOptionValue("scene", "0"));
        int samplePerPixel = Integer.parseInt(cmd.getOptionValue("sample-per-pixel", "20"));
        int maxDepth = Integer.parseInt(cmd.getOptionValue("max-depth", "5"));
        String outputFile = cmd.getOptionValue("output", null);

        new Window("Raytracing", sceneID, width, height, samplePerPixel, maxDepth, outputFile);
    }

    private static Options getOptions() {
        Options options = new Options();

        Option sceneOption = new Option("s", "scene", true, "scene ID");
        sceneOption.setRequired(false);
        options.addOption(sceneOption);

        Option resolutionOption = new Option("r", "resolution", true, "screen resolution");
        resolutionOption.setRequired(false);
        options.addOption(resolutionOption);

        Option samplePerPixelOption = new Option("spp", "sample-per-pixel", true, "sample per pixel");
        samplePerPixelOption.setRequired(false);
        options.addOption(samplePerPixelOption);

        Option maxDepthOption = new Option("md", "max-depth", true, "max depth");
        maxDepthOption.setRequired(false);
        options.addOption(maxDepthOption);

        Option outputOption = new Option("o", "output", true, "output file (must be a .png file)");
        outputOption.setRequired(false);
        options.addOption(outputOption);
        return options;
    }
}