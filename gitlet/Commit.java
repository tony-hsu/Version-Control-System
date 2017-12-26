package gitlet;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Set;
import java.util.Formatter;

/**
 * Commit object class that stores many meta data.
 * @author Tony Hsu
 */
public class Commit implements Serializable {

    /**
     * commits object directory.
     */
    private String commitsPath = ".gitlet/commits/";
    /**
     * blobs object directory.
     */
    private String blobsDirectory = ".gitlet/blobs/";
    /**
     * blobs object directory.
     */
    private String message;
    /**
     * Date when this commit was created.
     */
    private Date time;
    /**
     * Second parent if any.
     */
    private String mergeParent;
    /**
     * Parent commit.
     */
    private String parent;
    /**
     * Files and their sha1ID.
     */
    private HashMap<String, String> blobs;
    /**
     * Sha1ID of this commit.
     */
    private String sHA1ID;
    /**
     * Files that will be removed.
     */
    private TreeSet<String> removeFiles;

    /**
     * Initial Commits constructor.
     *
     * @param mess  String
     * @param time1 Date
     * @param par   String
     */
    Commit(String mess, Date time1, String par) {
        message = mess;
        time = time1;
        parent = par;
        blobs = new HashMap<>();
        sHA1ID = Utils.sha1(message, time.toString(), "", blobs.toString());
        File blobsDir = new File(blobsDirectory);
        File commitsDir = new File(commitsPath);
        File globalLog = new File(".gitlet/globalLog.txt");
        if (!blobsDir.exists()) {
            blobsDir.mkdirs();
            commitsDir.mkdirs();
            try {
                globalLog.createNewFile();
            } catch (IOException e) {
                System.out.println("Cannot create new File.");
            }
        }
        writeGlobalLog();
    }

    /**
     * Normal Commits constructor.
     *
     * @param mess  String
     * @param stage String
     */
    Commit(Stages stage, String mess) {
        message = mess;
        time = new Date();
        blobs = new HashMap<>(stage.getNewestCommit().blobs);
        removeFiles = stage.getRemovingFiles();
        removeFiles();
        parent = stage.getNewestCommit().sHA1ID;
        sHA1ID = Utils.sha1(message, time.toString(), parent, blobs.toString());
        addBlobs(stage.getStagedFiles());
        writeGlobalLog();
    }

    /**
     * Merge Commits constructor.
     *
     * @param stage   Stage
     * @param secondP String
     * @param firstB  String
     * @param secondB String
     */
    Commit(Stages stage, String secondP, String firstB, String secondB) {
        message = "Merged " + secondB + " into " + firstB + ".";
        time = new Date();
        blobs = new HashMap<>(stage.getStagedFiles());
        removeFiles(stage.getRemovingFiles());
        parent = stage.getNewestCommit().sHA1ID;
        mergeParent = secondP;
        sHA1ID = Utils.sha1(message, time.toString(), parent, blobs.toString());
        writeGlobalLog();
    }

    /**
     * Commits constructor.
     *
     * @param adding Map
     */
    public void addBlobs(Map<String, String> adding) {
        for (String i : adding.keySet()) {
            if (blobs.containsKey(i)) {
                blobs.replace(i, adding.get(i));
            } else {
                blobs.put(i, adding.get(i));
            }
        }
    }

    /**
     * Commits constructor.
     *
     * @return Map
     */
    public HashMap<String, String> getBlobs() {
        return blobs;
    }

    /**
     * Get the blobs ID.
     *
     * @param name String
     * @return String
     */
    public String getBlobsID(String name) {
        return blobs.get(name);
    }

    /**
     * Writing global log.
     */
    public void writeGlobalLog() {
        File globalLog = new File(".gitlet/globalLog.txt");
        try {
            FileWriter globalLogWriter = new FileWriter(globalLog, true);
            globalLogWriter.write(this.toString());
            globalLogWriter.write("\n");
            globalLogWriter.flush();
        } catch (IOException e) {
            System.out.println("Cannot write log.");
        }
    }

    /**
     * Removing track files.
     */
    public void removeFiles() {
        if (removeFiles == null) {
            return;
        }
        for (String filename : removeFiles) {
            blobs.remove(filename);
        }
    }

    /**
     * Removing files from working directory
     * when merging.
     *
     * @param removing Set
     */
    public void removeFiles(Set<String> removing) {
        for (String filename : removing) {
            Utils.restrictedDelete(filename);
        }
    }

    /**
     public void filesCopy() {
     if (newFiles == null) {
     return;
     }
     for (String fileName : newFiles) {
     File file = new File(fileName);
     String fileSha = Utils.sha1(Utils.readContents(file), fileName);
     blobs.put(fileName, fileSha);
     File gitletFile = new File(blobsDirectory + fileSha + ".file" );
     try {
     Files.copy(file.toPath(), gitletFile.toPath());
     } catch (IOException e) {
     System.out.println("IOException");
     }
     }
     }
     */

    /**
     * Check whether this commit tracks this file.
     *
     * @param name String
     * @return boolean
     */
    public boolean contains(String name) {
        return blobs.containsKey(name);
    }

    /**
     * Get the message of this commit.
     *
     * @return String
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the SHA1 of this commit.
     *
     * @return String
     */
    public String getSHA1ID() {
        return sHA1ID;
    }

    /**
     * Get the parent of this commit.
     *
     * @return String
     */
    public String getParent() {
        return parent;
    }

    /**
     * Put the string version of this commit.
     *
     * @return String
     */
    @Override
    public String toString() {
        StringBuilder sbuf = new StringBuilder();
        Formatter fmt = new Formatter(sbuf);
        fmt.format("Date: %ta %tb %te %tT %tY %tz",
                time.getTime(), time.getTime(),
                time.getTime(), time.getTime(),
                time.getTime(), time.getTime());

        StringBuilder output = new StringBuilder();
        output.append("===\n");
        output.append("commit " + sHA1ID + "\n");
        if (mergeParent != null) {
            output.append("Merge: " + parent.substring(0, 7) + " "
                    + mergeParent.substring(0, 7) + "\n");
        }
        output.append(fmt.toString() + "\n");
        output.append(message);
        String result = output.toString();
        return result;
    }


}
