package com.devonfw.tools.ide.locking;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

/**
 * Program to check if eclipse workspace is locked. A successful exit (code {@code 0}) indicates that the workspace is
 * free. Otherwise the exit code {@code 1} is used to indicate that the workspace is locked. In case of an error (e.g.
 * invalid CLI arguments) the exit code {@code -1} is used.
 */
public class EclipseWorkspaceLockChecker {

  /**
   * @param args the command-line arguments. There is currently only one argument defined which is the path to the lock
   *        file.
   */
  public static void main(String[] args) {

    EclipseWorkspaceLockChecker lockChecker = new EclipseWorkspaceLockChecker();
    int exitCode = lockChecker.run(args);
    System.exit(exitCode);
  }

  private int run(String[] args) {

    if (args.length != 1) {
      System.out.println("Usage: " + EclipseWorkspaceLockChecker.class.getSimpleName() + " <path2workspace>");
      return -1;
    }
    File lockfile = new File(args[0]);
    boolean locked = isLocked(lockfile);
    if (locked) {
      return 1;
    }
    return 0;
  }

  /**
   * @param lockfile the {@link File} pointing to the lockfile to check.
   * @return {@code true} if the given {@link File} is locked, {@code false} otherwise.
   */
  public static boolean isLocked(File lockfile) {

    if (lockfile.isFile()) {
      try (RandomAccessFile raFile = new RandomAccessFile(lockfile, "rw")) {
        FileLock fileLock = raFile.getChannel().tryLock(0, 1, false);
        // success, file was not locked so we immediately unlock again...
        fileLock.release();
        return false;
      } catch (Exception e) {
        return true;
      }
    }
    return false;
  }

}
