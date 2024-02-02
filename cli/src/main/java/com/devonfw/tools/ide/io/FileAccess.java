package com.devonfw.tools.ide.io;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

/**
 * Interface that gives access to various operations on files.
 */
public interface FileAccess {

  /**
   * Downloads a file from an arbitrary location.
   *
   * @param url the location of the binary file to download. May also be a local or remote path to copy from.
   * @param targetFile the {@link Path} to the target file to download to. Should not already exists. Missing parent
   *        directories will be created automatically.
   */
  void download(String url, Path targetFile);

  /**
   * Creates the entire {@link Path} as directories if not already existing.
   *
   * @param directory the {@link Path} to
   *        {@link java.nio.file.Files#createDirectories(Path, java.nio.file.attribute.FileAttribute...) create}.
   */
  void mkdirs(Path directory);

  /**
   * @param file the {@link Path} to check.
   * @return {@code true} if the given {@code file} points to an existing file, {@code false} otherwise (the given
   *         {@link Path} does not exist or is a directory).
   */
  boolean isFile(Path file);

  /**
   * @param folder the {@link Path} to check.
   * @return {@code true} if the given {@code folder} points to an existing directory, {@code false} otherwise (a
   *         warning is logged in this case).
   */
  boolean isExpectedFolder(Path folder);

  /**
   * @param file the {@link Path} to compute the checksum of.
   * @return the computed checksum (SHA-266).
   */
  String checksum(Path file);

  /**
   * Moves the given {@link Path} to the backup.
   *
   * @param fileOrFolder the {@link Path} to move to the backup (soft-deletion).
   */
  void backup(Path fileOrFolder);

  /**
   * @param source the source {@link Path file or folder} to move.
   * @param targetDir the {@link Path} with the directory to move {@code source} into.
   */
  void move(Path source, Path targetDir);

  /**
   * Creates a symbolic link. If the given {@code targetLink} already exists and is a symbolic link or a Windows
   * junction, it will be replaced. In case of missing privileges, Windows Junctions may be used as fallback, which must
   * point to absolute paths. Therefore, the created link will be absolute instead of relative.
   *
   * @param source the source {@link Path} to link to, may be relative or absolute.
   * @param targetLink the {@link Path} where the symbolic link shall be created pointing to {@code source}.
   * @param relative - {@code true} if the symbolic link shall be relative, {@code false} if it shall be absolute.
   */
  void symlink(Path source, Path targetLink, boolean relative);

  /**
   * Creates a relative symbolic link. If the given {@code targetLink} already exists and is a symbolic link or a
   * Windows junction, it will be replaced. In case of missing privileges, Windows Junctions may be used as fallback,
   * which must point to absolute paths. Therefore, the created link will be absolute instead of relative.
   *
   * @param source the source {@link Path} to link to, may be relative or absolute.
   * @param targetLink the {@link Path} where the symbolic link shall be created pointing to {@code source}.
   */
  default void symlink(Path source, Path targetLink) {

    symlink(source, targetLink, true);
  }

  /**
   * @param source the source {@link Path file or folder} to copy.
   * @param target the {@link Path} to copy {@code source} to. See {@link #copy(Path, Path, FileCopyMode)} for details.
   *        will always ensure that in the end you will find the same content of {@code source} in {@code target}.
   */
  default void copy(Path source, Path target) {

    copy(source, target, FileCopyMode.COPY_TREE_FAIL_IF_EXISTS);
  }

  /**
   * @param source the source {@link Path file or folder} to copy.
   * @param target the {@link Path} to copy {@code source} to. Unlike the Linux {@code cp} command this method will not
   *        take the filename of {@code source} and copy that to {@code target} in case that is an existing folder.
   *        Instead it will always be simple and stupid and just copy from {@code source} to {@code target}. Therefore
   *        the result is always clear and easy to predict and understand. Also you can easily rename a file to copy.
   *        While {@code cp my-file target} may lead to a different result than {@code cp my-file target/} this method
   *        will always ensure that in the end you will find the same content of {@code source} in {@code target}.
   * @param fileOnly - {@code true} if {@code fileOrFolder} is expected to be a file and an exception shall be thrown if
   *        it is a directory, {@code false} otherwise (copy recursively).
   */
  void copy(Path source, Path target, FileCopyMode fileOnly);

  /**
   * @param file the ZIP file to extract.
   * @param targetDir the {@link Path} with the directory to unzip to.
   */
  void unzip(Path file, Path targetDir);

  /**
   * @param file the ZIP file to extract.
   * @param targetDir the {@link Path} with the directory to unzip to.
   * @param compression the {@link TarCompression} to use.
   */
  void untar(Path file, Path targetDir, TarCompression compression);

  /**
   * @param path the {@link Path} to convert.
   * @return the absolute and physical {@link Path} (without symbolic links).
   */
  Path toRealPath(Path path);

  /**
   * Deletes the given {@link Path} idempotent and recursive.
   *
   * @param path the {@link Path} to delete.
   */
  void delete(Path path);

  /**
   * Creates a new temporary directory. ATTENTION: The user of this method is responsible to do house-keeping and
   * {@link #delete(Path) delete} it after the work is done.
   *
   * @param name the default name of the temporary directory to create. A prefix or suffix may be added to ensure
   *        uniqueness.
   * @return the {@link Path} to the newly created and unique temporary directory.
   */
  Path createTempDir(String name);

  /**
   * @param dir the folder to search.
   * @param filter the {@link Predicate} used to find the {@link Predicate#test(Object) match}.
   * @param recursive - {@code true} to search recursive in all sub-folders, {@code false} otherwise.
   * @return the first child {@link Path} matching the given {@link Predicate} or {@code null} if no match was found.
   */
  Path findFirst(Path dir, Predicate<Path> filter, boolean recursive);

  /**
   * Retrieves a list of file paths from the specified directory that satisfy the given predicate.
   * @param dir the folder to iterate through
   * @param filter the {@link Predicate} that determines whether a file should be included in the list.
   * @return a list of paths that satisfy the provided {@link Predicate}. Will be {@link List#isEmpty() empty} if no match was found but is never {@code null}.
   */
  List<Path> getChildrenInDir(Path dir, Predicate<Path> filter);

  /**
   * Retrieves a list of file paths from the specified directory.
   * @param dir the folder to iterate through
   * @return a list of paths. Will be {@link List#isEmpty() empty} if directory is empty but is never {@code null}.
   */
  List<Path> getChildrenInDir(Path dir);

  /**
   * Checks if a directory is empty.
   * @param dir The path of the directory to check.
   * @return {@code true} if the directory is empty, {@code false} otherwise.
   */
  boolean isEmptyDir(Path dir);

  /**
   * Finds the existing file with the specified name in the given list of directories.
   *
   * @param fileName      The name of the file to find.
   * @param searchDirs    The list of directories to search for the file.
   * @return              The {@code Path} of the existing file, or {@code null} if the file is not found.
   */
  Path findExistingFile(String fileName, List<Path> searchDirs);

}
