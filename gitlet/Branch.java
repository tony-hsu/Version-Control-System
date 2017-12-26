package gitlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.IOException;



/**
 * Branch class that is used to track branches and stages.
 *
 * @author Tony Hsu
 */
public class Branch implements Serializable {

    /**
     * path to commit.
     */
    private static String commitsPath = ".gitlet/commits/";
    /**
     * head commit.
     */
    private String head;
    /**
     * current stage.
     */
    private Stages curStage;
    /**
     * name of branch.
     */
    private String name;

    /**
     * Branch constructor.
     *
     * @param branchName String
     */
    Branch(String branchName) {
        name = branchName;
    }

    /**
     * Get name of branch.
     *
     * @return String.
     */
    public String getName() {
        return name;
    }

    /**
     * Get head commit of branch.
     *
     * @return Commit.
     */
    public Commit getHead() {
        return Git.deserializeCommit(head);
    }

    /**
     * Change head of commit.
     *
     * @param cur Commits
     */
    public void setHead(Commit cur) {
        head = cur.getSHA1ID();
    }

    /**
     * change the stage.
     *
     * @param cur Stage
     */
    public void setStage(Stages cur) {
        curStage = cur;
    }

    /**
     * print the current log.
     */
    public void printLog() {
        Commit cur = Git.deserializeCommit(head);
        while (true) {
            System.out.println(cur.toString());
            String curParent = cur.getParent();
            cur = Git.deserializeCommit(curParent);
            if (cur == null) {
                break;
            }
            System.out.println("");
        }
    }

    /**
     * make a new commit for merge.
     *
     * @param secondBranch String
     * @param secondParent String
     */
    public void commitMerge(String secondParent, String secondBranch) {
        Commit temp = new Commit(curStage, secondParent, name, secondBranch);
        head = temp.getSHA1ID();
        curStage = new Stages(temp);
        writeCommitFile(temp);
    }


    /**
     * add a new file.
     *
     * @param newname String
     */
    public void addFile(String newname) {
        curStage.add(newname);
    }

    /**
     * remove a file.
     *
     * @param newname String
     */
    public void removeFile(String newname) {
        curStage.remove(newname);
    }

    /**
     * make a new commit.
     * @param message String
     */
    public void commit(String message) {
        if ((curStage.getStagedFiles().isEmpty())
                && (curStage.getRemovingFiles().isEmpty())) {
            System.out.println("No changes added to the commit.");
            return;
        }
        Commit temp = new Commit(curStage, message);
        head = temp.getSHA1ID();
        curStage = new Stages(temp);
        writeCommitFile(temp);
    }

    /**
     * return the newest stage.
     *
     * @return Stage
     */
    public Stages getCurStage() {
        return curStage;
    }


    /**
     * serialize the newest commit.
     *
     * @param cur Commit
     */
    public void writeCommitFile(Commit cur) {
        try {
            File gitFile = new File(commitsPath + cur.getSHA1ID());
            ObjectOutputStream out =
                    new ObjectOutputStream(new FileOutputStream(gitFile));
            out.writeObject(cur);
            out.close();
        } catch (IOException e) {
            System.out.println("IOException");
        }
    }


}
