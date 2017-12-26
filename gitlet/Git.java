package gitlet;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.TreeMap;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;


import java.io.FileNotFoundException;
import java.io.FileReader;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Git class that serves as the driver class for
 * performing many operations.
 *
 * @author Tony Hsu
 */
public class Git implements Serializable {

    /**
     * path to blobs.
     */
    private static String blobsDirectory = ".gitlet/blobs/";
    /**
     * path to commits.
     */
    private static String commitsPath = ".gitlet/commits/";
    /**
     * branches.
     */
    private TreeMap<String, Branch> branches;
    /**
     * current branch.
     */
    private Branch curBranch;
    /**
     * message and corresponding IDs.
     */
    private HashMap<String, HashSet<String>> messageToID;

    /**
     * Git constructor.
     */
    Git() {
        branches = new TreeMap<>();
    }

    /**
     * To initialize a new git.
     *
     * @param curCommit Commit.
     * @return Git
     */
    public static Git gitInit(Commit curCommit) {
        Git newGit = new Git();
        Branch initialBranch = new Branch("master");
        newGit.curBranch = initialBranch;
        initialBranch.setStage(new Stages(curCommit));
        initialBranch.setHead(curCommit);
        initialBranch.writeCommitFile(curCommit);
        newGit.branches.put("master", initialBranch);
        return newGit;
    }

    /**
     * deserialize a commit.
     *
     * @param name String
     * @return Commit
     */
    public static Commit deserializeCommit(String name) {
        if (name == null) {
            return null;
        }
        Commit result = null;
        File gitFile = new File(commitsPath + name);
        try {
            ObjectInputStream inp =
                    new ObjectInputStream(new FileInputStream(gitFile));
            result = (Commit) inp.readObject();
            inp.close();
        } catch (IOException e) {
            System.out.println("IOException");
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException");
        }
        return result;
    }

    /**
     * perform merge.
     *
     * @param name String
     */
    public void merge(String name) {
        if ((!curBranch.getCurStage().getStagedFiles().isEmpty())
                || (!curBranch.getCurStage().getRemovingFiles().isEmpty())) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        if (!branches.containsKey(name)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (name.equals(curBranch.getName())) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        Branch givenBranch = branches.get(name);
        Commit givenCommit = givenBranch.getHead();
        Commit splitPoint = splitPoint(givenBranch);
        Stages temp = new Stages(curBranch.getHead());
        if (splitPoint.getSHA1ID().equals(givenCommit.getSHA1ID())) {
            System.out.println("Given branch is an "
                    +
                    "ancestor of the current branch.");
            return;
        }
        if (splitPoint.getSHA1ID().equals(curBranch.getHead().getSHA1ID())) {
            if (checkUntrackedOverwritten(givenCommit)) {
                System.out.println("There is an untrac"
                        + "ked file in the way; delete it or add it first.");
            } else {
                System.out.println("Current branch fast-forwarded.");
                reset(givenCommit.getSHA1ID());
                curBranch.setHead(givenCommit);
                temp = new Stages(givenCommit);
                curBranch.setStage(temp);
            }
            return;
        }
        HashSet<String> allFiles =
                new HashSet<>(givenBranch.getHead().getBlobs().keySet());
        allFiles.addAll(splitPoint.getBlobs().keySet());
        allFiles.addAll(curBranch.getHead().getBlobs().keySet());
        temp.initMerge();
        boolean mergeConflict = mergeFilesClassifier(splitPoint, givenCommit,
                curBranch.getHead(), temp, allFiles);
        if (checkUntrackedOverwrittenMerge
                (temp.getMergeCheckOutFiles().keySet(),
                        temp.getRemovingFiles())) {
            System.out.println("There is an untracked file "
                    +
                    "in the way; delete it or add it first.");
            return;
        }
        if (mergeConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        curBranch.setStage(temp);
        mergeFilesRestore(temp);
        curBranch.commitMerge(givenCommit.getSHA1ID(), name);
        addNewMesID();
    }

    /**
     * Restore merged files.
     *
     * @param cur Stages
     */
    public void mergeFilesRestore(Stages cur) {
        TreeMap<String, String> newFiles = cur.getMergeCheckOutFiles();
        for (String i : newFiles.keySet()) {
            restoreFileName(i, newFiles.get(i), true);
        }
    }

    /**
     * Check whether files would get overwritten by merge.
     *
     * @param overWriting Set
     * @param removing    Set
     * @return boolean
     */
    public boolean checkUntrackedOverwrittenMerge(
            Set<String> overWriting, Set<String> removing) {
        List<String> allName = Utils.plainFilenamesIn(Paths.get(".").
                toAbsolutePath().normalize().toString());
        ArrayList<String> untrackedFiles = new ArrayList<>();
        for (String i : allName) {
            if (curBranch.getHead().contains(i)) {
                continue;
            }
            if (curBranch.getCurStage().getStagedFiles().containsKey(i)) {
                continue;
            }
            untrackedFiles.add(i);
        }
        for (String i : untrackedFiles) {
            if ((overWriting.contains(i)) || (removing.contains(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * To determine what to do with file at split point.
     *
     * @param name         String
     * @param curID        String
     * @param currentFiles Set
     * @param givenFiles   Set
     * @param givenID      String
     * @param newStage     Stages
     * @param splitPoint   Commit
     * @return boolean
     */
    public boolean splitPointClassifier(Commit splitPoint,
                                        Set<String> currentFiles,
                                        Set<String> givenFiles,
                                        String name, String curID,
                                        String givenID, Stages newStage) {
        boolean conflict = false;
        String splitID = splitPoint.getBlobsID(name);
        if ((currentFiles.contains(name)) && (givenFiles.contains(name))) {
            if (splitID.equals(givenID)) {
                if (splitID.equals(curID)) {
                    newStage.getStagedFiles().put(name, givenID);
                } else {
                    newStage.getStagedFiles().put(name, curID);
                }
            } else {
                if (splitID.equals(curID)) {
                    newStage.getStagedFiles().put(name, givenID);
                    newStage.getMergeCheckOutFiles().put(name, givenID);
                } else {
                    String merged = handleMergeConflict(curID, givenID);
                    newStage.getStagedFiles().put(name, merged);
                    newStage.getMergeCheckOutFiles().put(name, merged);
                    conflict = true;
                }
            }
        } else if (currentFiles.contains(name)) {
            if (splitID.equals(curID)) {
                newStage.getRemovingFiles().add(name);
            } else {
                String merged = handleMergeConflict(curID, givenID);
                newStage.getStagedFiles().put(name, merged);
                newStage.getMergeCheckOutFiles().put(name, merged);
                conflict = true;
            }
        } else if (givenFiles.contains(name)) {
            if (!splitID.equals(givenID)) {
                String merged = handleMergeConflict(curID, givenID);
                newStage.getStagedFiles().put(name, merged);
                newStage.getMergeCheckOutFiles().put(name, merged);
                conflict = true;
            }
        }
        return conflict;
    }

    /**
     * Determine what to do with a file at merge.
     *
     * @param splitPoint Commit
     * @param newStage   Stages
     * @param allFiles   Set
     * @param current    Commit
     * @param given      Commit
     * @return boolean
     */
    public boolean mergeFilesClassifier(Commit splitPoint,
                                        Commit given, Commit current,
                                        Stages newStage, Set<String> allFiles) {
        Set<String> splitPointFiles = splitPoint.getBlobs().keySet();
        Set<String> currentFiles = current.getBlobs().keySet();
        Set<String> givenFiles = given.getBlobs().keySet();
        boolean conflict = false;
        for (String name : allFiles) {
            String curID = current.getBlobsID(name);
            String givenID = given.getBlobsID(name);
            if (splitPointFiles.contains(name)) {
                boolean helper = splitPointClassifier(splitPoint,
                        currentFiles, givenFiles,
                        name, curID, givenID, newStage);
                conflict = helper;

            } else {
                if ((currentFiles.contains(name))
                        &&
                        (givenFiles.contains(name))) {
                    if (curID.equals(givenID)) {
                        newStage.getStagedFiles().put(name, givenID);
                    } else {
                        String newFile = handleMergeConflict(curID, givenID);
                        newStage.getStagedFiles().put(name, newFile);
                        newStage.getMergeCheckOutFiles().put(name, newFile);
                        conflict = true;
                    }
                } else if (currentFiles.contains(name)) {
                    newStage.getStagedFiles().
                            put(name, current.getBlobsID(name));
                } else {
                    newStage.getStagedFiles().put(name, givenID);
                    newStage.getMergeCheckOutFiles().put(name, givenID);
                }
            }
        }
        return conflict;
    }

    /**
     * Make a new file when there's a conflict.
     *
     * @param file1 String
     * @param file2 String
     * @return String
     */
    public String handleMergeConflict(String file1, String file2) {
        String firstCont;
        String secondCont;
        if (file1 == null) {
            firstCont = "";
        } else {
            String firstDes = blobsDirectory + file1;
            File firstFile = new File(firstDes);
            firstCont = Utils.readContentsAsString(firstFile);
        }
        if (file2 == null) {
            secondCont = "";
        } else {
            String secondDes = blobsDirectory + file2;
            File secondFile = new File(secondDes);
            secondCont = Utils.readContentsAsString(secondFile);
        }
        String sha1 = Utils.sha1(firstCont, secondCont);
        String head = "<<<<<<< HEAD\n";
        String separator = "=======\n";
        String tail = ">>>>>>>\n";
        String finalDes = blobsDirectory + sha1;
        File finalFile = new File(finalDes);
        String finalString = head + firstCont
                +
                separator + secondCont + tail;
        Utils.writeContents(finalFile, finalString);
        return sha1;
    }

    /**
     * Return the split point between two branches.
     *
     * @param given Branch
     * @return Commit
     */
    public Commit splitPoint(Branch given) {
        ArrayList<String> curHistory = new ArrayList<>();
        Commit curCommit = curBranch.getHead();
        while (curCommit != null) {
            curHistory.add(curCommit.getSHA1ID());
            curCommit = Git.deserializeCommit(curCommit.getParent());
        }
        curCommit = given.getHead();
        int result;
        while (true) {
            if (curHistory.contains(curCommit.getSHA1ID())) {
                result = curHistory.indexOf(curCommit.getSHA1ID());
                break;
            }
            curCommit = Git.deserializeCommit(curCommit.getParent());
        }
        return Git.deserializeCommit(curHistory.get(result));
    }

    /**
     * Perform the checkout operation.
     *
     * @param input String[]
     */
    public void checkout(String[] input) {
        if (input.length == 1) {
            String branchName = input[0];
            if (!branches.containsKey(branchName)) {
                System.out.println("No such branch exists.");
            } else if (checkUntrackedOverwritten
                    (branches.get(branchName).getHead())) {
                System.out.println("There is an untracked file in the way; "
                        +
                        "delete it or add it first.");
            } else if (curBranch.getName().equals(branchName)) {
                System.out.println("No need to checkout the current branch.");
            } else {
                restoreBranchName(branchName);
            }
        } else if (input.length == 2) {
            if (curBranch.getHead().contains(input[1])) {
                restoreFileName(input[1], null, false);
            } else {
                System.out.println("File does not exist in that commit.");
            }
        } else if (input.length == 3) {
            String commitID = input[0];
            String fileName = input[2];
            commitID = commitIDExists(commitID);
            if (commitID == null) {
                System.out.println("No commit with that id exists.");
                return;
            }
            Commit cur = deserializeCommit(commitID);
            if (!cur.contains(fileName)) {
                System.out.println("File does not exist in that commit.");
                return;
            }
            restoreCommitID(cur, fileName);
        }
    }

    /**
     * Checkout whether a commit ID exists in Git.
     *
     * @param name String
     * @return String
     */
    public String commitIDExists(String name) {
        if (name.length() == Utils.UID_LENGTH) {
            File commitFile = new File(commitsPath + name);
            if (commitFile.exists()) {
                return name;
            } else {
                return null;
            }
        } else {
            List<String> allCommits = Utils.plainFilenamesIn(commitsPath);
            for (String i : allCommits) {
                if (i.contains(name)) {
                    return i;
                }
            }
            return null;
        }
    }

    /**
     * Perform the reset operation.
     *
     * @param name String
     */
    public void reset(String name) {
        name = commitIDExists(name);
        if (name == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit cur = deserializeCommit(name);
        if (checkUntrackedOverwritten(cur)) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it or add it first.");
            return;
        }
        Set<String> allNamesNew = cur.getBlobs().keySet();
        for (String i : allNamesNew) {
            restoreCommitID(cur, i);
        }
        Set<String> allNamesOld = curBranch.getHead().getBlobs().keySet();
        for (String j : allNamesOld) {
            if (!allNamesNew.contains(j)) {
                Utils.restrictedDelete(j);
            }
        }
        curBranch.setHead(cur);
        curBranch.setStage(new Stages(cur));
    }

    /**
     * Check whether a file would get overwritten.
     *
     * @param newCommit Commit
     * @return boolean
     */
    public boolean checkUntrackedOverwritten(Commit newCommit) {
        List<String> allName = Utils.plainFilenamesIn(Paths.get(".").
                toAbsolutePath().normalize().toString());
        ArrayList<String> untrackedFiles = new ArrayList<>();
        for (String i : allName) {
            if (curBranch.getHead().contains(i)) {
                continue;
            }
            if (curBranch.getCurStage().getStagedFiles().containsKey(i)) {
                continue;
            }
            untrackedFiles.add(i);
        }
        Set<String> comingFiles = newCommit.getBlobs().keySet();
        for (String i : untrackedFiles) {
            if (comingFiles.contains(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checkout when a commit ID is give.
     *
     * @param name String
     * @param cur  Commit
     */
    public void restoreCommitID(Commit cur, String name) {
        String old = blobsDirectory + cur.getBlobs().get(name);
        File curFile = new File(name);
        File oldFile = new File(old);
        try {
            Files.copy(oldFile.toPath(), curFile.toPath(),
                    COPY_ATTRIBUTES, REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("Could not restore file.");
        }
    }

    /**
     * Restore a specific file.
     *
     * @param name  String
     * @param iD    String
     * @param merge boolean
     */
    public void restoreFileName(String name, String iD, Boolean merge) {
        String old;
        if (merge) {
            old = blobsDirectory + iD;
        } else {
            Commit cur = curBranch.getHead();
            old = blobsDirectory + cur.getBlobs().get(name);
        }
        File curFile = new File(name);
        File oldFile = new File(old);
        try {
            Files.copy(oldFile.toPath(), curFile.toPath(),
                    COPY_ATTRIBUTES, REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("Could not restore file.");
        }
    }

    /**
     * Checkout with a branch name is given.
     *
     * @param name String
     */
    public void restoreBranchName(String name) {
        Branch setBranch = branches.get(name);
        Commit newCommit = setBranch.getHead();
        HashMap<String, String> newCommitBlobs = newCommit.getBlobs();
        for (String i : newCommitBlobs.keySet()) {
            String old = blobsDirectory
                    + newCommitBlobs.get(i);
            File curFile = new File(i);
            File oldFile = new File(old);
            try {
                Files.copy(oldFile.toPath(),
                        curFile.toPath(), COPY_ATTRIBUTES, REPLACE_EXISTING);
            } catch (IOException e) {
                System.out.println("Could not restore file.");
            }
        }
        Commit oldCommit = curBranch.getHead();
        Set<String> newCommitBlobsNames = newCommitBlobs.keySet();
        for (String i : oldCommit.getBlobs().keySet()) {
            if (!newCommitBlobsNames.contains(i)) {
                Utils.restrictedDelete(i);
            }
        }
        curBranch.setStage(null);
        curBranch = setBranch;
        curBranch.setStage(new Stages(curBranch.getHead()));
    }

    /**
     * Stage a new file.
     *
     * @param adding String
     */
    public void add(String adding) {
        curBranch.addFile(adding);
    }

    /**
     * Add a new branch.
     *
     * @param name String
     */
    public void newBranch(String name) {
        if (branches.containsKey(name)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        Branch newBranch = new Branch(name);
        newBranch.setHead(curBranch.getHead());
        branches.put(name, newBranch);
    }

    /**
     * Remove a branch.
     *
     * @param name String
     */
    public void rmBranch(String name) {
        if (!branches.containsKey(name)) {
            System.out.println("A branch with that name does not exists.");
        } else if (curBranch.getName().equals(name)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            branches.remove(name);
        }
    }

    /**
     * Make a new commit.
     *
     * @param message String
     */
    public void commit(String message) {
        curBranch.commit(message);
        addNewMesID();
    }

    /**
     * Print log from current branch.
     */
    public void printLog() {
        curBranch.printLog();
    }

    /**
     * Print current status.
     */
    public void status() {
        System.out.println("=== Branches ===");
        for (String i : branches.keySet()) {
            if (i.equals(curBranch.getName())) {
                System.out.println("*" + i);
            } else {
                System.out.println(i);
            }
        }
        System.out.println("");
        System.out.println("=== Staged Files ===");
        for (String i : curBranch.getCurStage().
                getStagedFiles().keySet()) {
            System.out.println(i);
        }
        System.out.println("");
        System.out.println("=== Removed Files ===");
        for (String i : curBranch.getCurStage().getRemovingFiles()) {
            System.out.println(i);
        }
        System.out.println("");
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println("");
        System.out.println("=== Untracked Files ===");
    }

    /**
     * Remove a file from current branch.
     *
     * @param deleting String
     */
    public void remove(String deleting) {
        curBranch.removeFile(deleting);
    }

    /**
     * Find the ID with that message.
     *
     * @param message String
     */
    public void find(String message) {
        if (!messageToID.containsKey(message)) {
            System.out.println("Found no commit with that message.");
            return;
        }
        for (String id : messageToID.get(message)) {
            System.out.println(id);
        }
    }

    /**
     * Initialize new hashmap for message to ID.
     */
    public void initMes2ID() {
        messageToID = new HashMap<>();
    }

    /**
     * add message to ID.
     */
    public void addNewMesID() {
        Commit cur = curBranch.getCurStage().getNewestCommit();
        String message = cur.getMessage();
        String iD = cur.getSHA1ID();
        if (messageToID.containsKey(message)) {
            messageToID.get(message).add(iD);
        } else {
            HashSet<String> newMess = new HashSet<>();
            newMess.add(iD);
            messageToID.put(message, newMess);
        }
    }

    /**
     * Print global log.
     */
    public void globalLog() {
        try {
            BufferedReader reader = new BufferedReader(
                    new FileReader(".gitlet/globalLog.txt"));
            String output = "";
            String per = reader.readLine();
            while (per != null) {
                output += per;
                per = reader.readLine();
                if (per == null) {
                    break;
                } else if (per.equals("===")) {
                    output += "\n";
                    output += "\n";
                } else {
                    output += "\n";
                }
            }
            System.out.print(output);
        } catch (FileNotFoundException e) {
            System.out.println("Could not find file");
        } catch (IOException e) {
            System.out.println("Could not read file");
        }

    }


    /**
     * Return a desired branch.
     * @param name String
     * @return Branch.
     */
    public Branch getAnyBranch(String name) {
        return branches.get(name);
    }

}
