package gitlet;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.io.File;
import java.io.IOException;


/**
 * Driver class for Gitlet, the tiny version-control
 * system.
 * @author Tony Hsu
 */
public class Main {

    /**
     * Gitlet repository path.
     */
    private static final String GITLETREPO = ".gitlet/";


    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        String[] inputs = new String[args.length - 1];
        System.arraycopy(args, 1, inputs, 0, args.length - 1);
        if (incorrectOperands(args)) {
            return;
        }
        Git currentGit = loadGit();
        if ((!args[0].equals("init")) && (currentGit == null)) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        switch (args[0]) {
        case "init":
            currentGit = init();
            break;
        case "commit":
            currentGit.commit(inputs[0]);
            break;
        case "add":
            currentGit.add(inputs[0]);
            break;
        case "rm":
            currentGit.remove(inputs[0]);
            break;
        case "log":
            currentGit.printLog();
            break;
        case "global-log":
            currentGit.globalLog();
            break;
        case "status":
            currentGit.status();
            break;
        case "find":
            currentGit.find(inputs[0]);
            break;
        case "checkout":
            currentGit.checkout(inputs);
            break;
        case "branch":
            currentGit.newBranch(inputs[0]);
            break;
        case "rm-branch":
            currentGit.rmBranch(inputs[0]);
            break;
        case "reset":
            currentGit.reset(inputs[0]);
            break;
        case "merge":
            currentGit.merge(inputs[0]);
            break;
        default:
        }
        saveProgress(currentGit);
    }

    /**
     * Gitlet command check.
     * @param args String[]
     * @return boolean
     */
    public static boolean incorrectOperands(String[] args) {
        String[] inputs = new String[args.length - 1];
        System.arraycopy(args, 1, inputs, 0, args.length - 1);
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return true;
        }
        switch (args[0]) {
        case "init":
            return incorrectOperandsHelper2(inputs);
        case "commit":
            return incorrectOperandsHelper3(inputs);
        case "add":
            return incorrectOperandsHelper(inputs);
        case "rm":
            return incorrectOperandsHelper(inputs);
        case "log":
            return incorrectOperandsHelper2(inputs);
        case "global-log":
            return incorrectOperandsHelper2(inputs);
        case "status":
            return incorrectOperandsHelper2(inputs);
        case "find":
            return incorrectOperandsHelper(inputs);
        case "checkout":
            return incorrectOperandsHelper4(inputs);
        case "branch":
            return incorrectOperandsHelper(inputs);
        case "rm-branch":
            return incorrectOperandsHelper(inputs);
        case "reset":
            return incorrectOperandsHelper(inputs);
        case "merge":
            return incorrectOperandsHelper(inputs);
        default:
            System.out.println("No command with that name exists.");
            return true;
        }
    }

    /**
     * Gitlet command check helper.
     * @param args String[]
     * @return boolean
     */
    public static boolean incorrectOperandsHelper(String[] args) {
        if (args.length != 1) {
            System.out.println("Incorrect operands.");
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gitlet command check helper.
     * @param args String[]
     * @return boolean
     */
    public static boolean incorrectOperandsHelper2(String[] args) {
        if (args.length != 0) {
            System.out.println("Incorrect operands.");
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gitlet command check helper.
     * @param args String[]
     * @return boolean
     */
    public static boolean incorrectOperandsHelper3(String[] args) {
        if ((args.length == 0) || (args[0].equals(""))) {
            System.out.println("Please enter a commit message.");
            return true;
        }
        if (args.length != 1)  {
            System.out.println("Incorrect operands.");
            return true;
        }
        return false;
    }

    /**
     * Gitlet command check helper.
     * @param args String[]
     * @return boolean
     */
    public static boolean incorrectOperandsHelper4(String[] args) {
        if ((args.length > 3) || (args.length == 0)) {
            System.out.println("Incorrect operands.");
            return true;
        }
        if ((args.length == 2) && (!args[0].equals("--"))) {
            System.out.println("Incorrect operands.");
            return true;
        }
        if ((args.length == 3) && (!args[1].equals("--"))) {
            System.out.println("Incorrect operands.");
            return true;
        }
        return false;
    }


    /**
     * Gitlet repository path.
     * @return Git
     */
    public static Git loadGit() {
        Git result = null;
        File gitFile = new File(GITLETREPO + "mainControl.file");
        if (gitFile.exists()) {
            try {
                ObjectInputStream inp =
                        new ObjectInputStream(new FileInputStream(gitFile));
                result = (Git) inp.readObject();
                inp.close();
            } catch (IOException e) {
                System.out.println("IOException");
            } catch (ClassNotFoundException e) {
                System.out.println("ClassNotFoundException");
            }
        }
        return result;
    }

    /**
     * Saving process for Git object.
     * @param git Git
     */
    public static void saveProgress(Git git) {
        if (git == null) {
            return;
        }
        try {
            File gitFile = new File(GITLETREPO + "mainControl.file");
            ObjectOutputStream out =
                    new ObjectOutputStream(new FileOutputStream(gitFile));
            out.writeObject(git);
            out.close();
        } catch (IOException e) {
            System.out.println("IOException");
        }
    }

    /**
     * Initializes Git object.
     * @return Git
     */
    public static Git init() {
        File cur = new File(".gitlet");
        if (cur.exists()) {
            System.out.println("A Gitlet version-control "
                    + "system already exists in"
                    + "the current directory.");
            return null;
        } else {
            cur.mkdirs();
        }
        Commit first = new Commit("initial commit", new Date(0), null);
        Git main = Git.gitInit(first);
        main.initMes2ID();
        main.addNewMesID();
        return main;
    }

}
