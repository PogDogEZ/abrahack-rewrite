package ez.pogdog.yescom.handlers;

import ez.pogdog.yescom.YesCom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SaveHandler implements IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    public SaveHandler() {
        createDirectories();
    }

    @Override
    public void onTick() {
    }

    @Override
    public void onExit() {
    }

    /* ------------------------ Uncompressed Saving & Private Methods ------------------------ */

    /**
     * TODO: This can probably be removed later, but is useful for testing and shit
     * @param data Any string you want to store uncompressed
     */
    public void saveUncompressed(String data, String filename) {
        String directory = "data/uncompressed/" + filename + ".txt";
        File file = new File(directory);
        try {
            FileWriter writer = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(writer);
            bw.write(data);
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createDirectories() {
        new File(yesCom.configHandler.MAIN_DIRECTORY).mkdirs();
        new File(yesCom.configHandler.RAW_DIRECTORY).mkdirs();
        new File(yesCom.configHandler.PLAYER_DIRECTORY).mkdirs();
        new File(yesCom.configHandler.TRACKERS_DIRECTORY).mkdirs();
        new File("data/uncompressed").mkdirs();
    }
}
