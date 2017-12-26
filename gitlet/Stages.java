package gitlet;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.io.File;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.TreeMap;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Stage class that deals with staging operations.
 * @author Tony Hsu
 */
public class Stages implements Serializable {

    /**
     * staged files. */
    private TreeMap<String, String> stagedFiles;
    /**
     * files that will be checked out. */
    private TreeMap<String, String> mergeCheckOutFiles;
    /**
     * newest commit. */
    private Commit newestCommit;
    /**
     * files that should be untracked. */
    private TreeSet<String> removingFiles;
    /**
     * path to blobs. */
    private String blobsDirectory = ".gitlet/blobs/";

    /**
     * Constructor for stage class.
     * @param latest Commit
     * */
    Stages(Commit latest) {
        newestCommit = latest;
        stagedFiles = new TreeMap<>();
        removingFiles = new TreeSet<>();
    }

    /**
     * initializes the merge checkout process. */
    public void initMerge() {
        mergeCheckOutFiles = new TreeMap<>();
    }

    /**
     * return the merge checkout files.
     * @return TreeMap
     * */
    public TreeMap<String, String> getMergeCheckOutFiles() {
        return mergeCheckOutFiles;
    }

    /**
     * add files to the staging area.
     * @param name String
     * */
    public void add(String name) {
        File file = new File(name);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        if (newestCommit.contains(name)) {
            if (!hasChangedFromLast(name)) {
                if (stagedFiles.containsKey(name)) {
                    stagedFiles.remove(name);
                }
                if (removingFiles.contains(name)) {
                    removingFiles.remove(name);
                }
                return;
            }
        }
        if (removingFiles.contains(name)) {
            removingFiles.remove(name);
        }
        String fileSha = Utils.sha1(Utils.readContents(file), name);
        File gitletFile = new File(blobsDirectory + fileSha);
        try {
            Files.copy(file.toPath(), gitletFile.toPath(),
                    COPY_ATTRIBUTES, REPLACE_EXISTING);
            stagedFiles.put(name, fileSha);
        } catch (IOException e) {
            System.out.println("IOException");
        }
    }

    /**
     * return staged files.
     * @return TreeMap
     * */
    public TreeMap<String, String> getStagedFiles() {
        return stagedFiles;
    }

    /**
     * return the removing files.
     * @return TreeSet
     * */
    public TreeSet<String> getRemovingFiles() {
        return removingFiles;
    }

    /**
     * return the newest commit.
     * @return Commit
     * */
    public Commit getNewestCommit() {
        return newestCommit;
    }

    /**
     * removing files: untracking.
     * @param name String
     * */
    public void remove(String name) {
        if ((!stagedFiles.containsKey(name))
                && (!newestCommit.contains(name))) {
            System.out.println("No reason to remove the file.");
            return;
        }
        if (stagedFiles.containsKey(name)) {
            stagedFiles.remove(name);
        }
        if (newestCommit.contains(name)) {
            File file = new File(name);
            removingFiles.add(name);
            if (file.exists()) {
                Utils.restrictedDelete(file);
            }
        }
    }

    /**
     * Check whether the file has changed since last commit.
     * @param name String
     * @return boolean
     * */
    public boolean hasChangedFromLast(String name) {
        File cur = new File(name);
        String oldLocation = blobsDirectory
                + getNewestCommit().getBlobs().get(name);
        File old = new File(oldLocation);
        byte[] curContents = Utils.readContents(cur);
        byte[] oldContents = Utils.readContents(old);
        return !Arrays.equals(curContents, oldContents);
    }


}
