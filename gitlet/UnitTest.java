package gitlet;

import ucb.junit.textui;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.sql.Timestamp;



/** The suite of all JUnit tests for the gitlet package.
 *  @author Tony Hsu
 */
public class UnitTest {


    private static final String GIT_DIRECTORY = ".gitlet";

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    public static boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDirectory(children[i]);
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }



    /** A dummy test to avoid complaint. */
    @Test
    public void placeholderTest() {
    }

    @Test
    public void initTest() {
        File file = new File(GIT_DIRECTORY);
        if (!file.exists()) {
            Main.init();
            assertTrue(file.exists());
            deleteDirectory(file);
            assertTrue(!file.exists());
        }
    }

    @Test
    public void splitTest() {
        Commit first = new Commit("initial commit", new Timestamp(0), null);
        Git main = Git.gitInit(first);
        main.initMes2ID();
        main.addNewMesID();
        main.newBranch("first");
        main.add("test.txt");
        main.commit("hello");
        assertEquals(main.splitPoint(main.getAnyBranch("first")).getSHA1ID(),
                main.getAnyBranch("first").getHead().getSHA1ID());
    }


    @Test
    public void split2Test() {
        Commit first = new Commit("initial commit", new Timestamp(0), null);
        Git main = Git.gitInit(first);
        main.initMes2ID();
        main.addNewMesID();
        main.newBranch("first");
        main.checkout(new String[] {"first"});
        assertEquals(main.splitPoint(main.getAnyBranch("first")).getSHA1ID(),
                main.getAnyBranch("master").getHead().getSHA1ID());
        assertEquals(main.splitPoint(main.getAnyBranch("first")).getSHA1ID(),
                main.getAnyBranch("first").getHead().getSHA1ID());
        main.add("test.txt");
        main.commit("hello");
        assertEquals(main.splitPoint(main.getAnyBranch("master")).getSHA1ID(),
                main.getAnyBranch("master").getHead().getSHA1ID());
    }

    @Test
    public void split3Test() {
        Commit first = new Commit("initial commit", new Timestamp(0), null);
        String result = first.getSHA1ID();
        Git main = Git.gitInit(first);
        main.initMes2ID();
        main.addNewMesID();
        main.newBranch("first");
        main.checkout(new String[] {"first"});
        main.add("test.txt");
        main.commit("hello1");
        main.add("test2.txt");
        main.commit("hello2");
        main.checkout(new String[] {"master"});
        main.add("test1.txt");
        main.commit("hello3");
        main.add("test3.txt");
        main.commit("hello4");
        Commit output = main.splitPoint(main.getAnyBranch("first"));
        assertEquals(result,
                output.getSHA1ID());
    }

    @Test
    public void split4Test() {
        Commit first = new Commit("initial commit", new Timestamp(0), null);
        String result = first.getSHA1ID();
        Git main = Git.gitInit(first);
        main.initMes2ID();
        main.addNewMesID();
        main.newBranch("first");
        main.checkout(new String[] {"first"});
        main.add("test.txt");
        main.commit("hello1");
        main.checkout(new String[] {"master"});
        main.add("test1.txt");
        main.commit("hello3");
        main.add("test2.txt");
        main.commit("hello2");
        main.add("test3.txt");
        main.commit("hello4");
        Commit output = main.splitPoint(main.getAnyBranch("first"));
        assertEquals(result,
                output.getSHA1ID());
    }

    @Test
    public void split5Test() {
        Commit first = new Commit("initial commit", new Timestamp(0), null);
        String result = first.getSHA1ID();
        Git main = Git.gitInit(first);
        main.initMes2ID();
        main.addNewMesID();
        main.newBranch("first");
        main.checkout(new String[] {"first"});
        main.add("test.txt");
        main.commit("hello1");
        main.add("test1.txt");
        main.commit("hello3");
        main.add("test2.txt");
        main.commit("hello2");
        main.checkout(new String[] {"master"});
        main.add("test3.txt");
        main.commit("hello4");
        Commit output = main.splitPoint(main.getAnyBranch("first"));
        assertEquals(result,
                output.getSHA1ID());
    }


    @Test
    public void timeTest() {
        Commit first = new Commit("initial commit", new Timestamp(0), null);
        Git main = Git.gitInit(first);
        main.initMes2ID();
        main.addNewMesID();
        main.newBranch("first");
        main.add("test.txt");
        main.commit("hello");
        main.printLog();
        main.globalLog();
    }

    @Test
    public void logTest() {
        Commit first = new Commit("initial commit", new Timestamp(0), null);
        System.out.println(first.toString());
    }


    @Test
    public void branchTest() {
        Commit first = new Commit("initial commit", new Timestamp(0), null);
        Git main = Git.gitInit(first);
        main.newBranch("coolBeans");
        main.newBranch("coolBeans");
        main.newBranch("master");
    }

    @Test
    public void uncheckedTest() {
        Commit first = new Commit("initial commit", new Timestamp(0), null);
        Git main = Git.gitInit(first);
        main.initMes2ID();
        main.addNewMesID();
        main.add("test.txt");
        main.commit("hello");
        main.add("test1.txt");
    }


    @Test
    public void statusTest() {
        Commit first = new Commit("initial commit", new Timestamp(0), null);
        Git main = Git.gitInit(first);
        main.newBranch("coolBeans");
        main.newBranch("a");
        main.newBranch("r");
        main.newBranch("b");
        main.add("test.txt");
        main.add("test1.txt");
        main.remove("test.txt");
        main.status();
    }

    @Test
    public void addTest() {
        Commit first = new Commit("initial commit", new Timestamp(0), null);
        Git main = Git.gitInit(first);
        main.initMes2ID();
        main.addNewMesID();
        main.add("test.txt");
        main.commit("hello");
        main.add("test.txt");
        main.commit("hello");
    }





}


