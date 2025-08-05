package com.devonfw.tools.ide.io;

import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Interface that gives access to various operations on files.
 */
public interface FileAccess {

  /** {@link PosixFilePermission}s for "rwxr-xr-x" or 0755. */
  Set<PosixFilePermission> RWX_RX_RX = Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
      PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE);

  /**
   * Downloads a file from an arbitrary location.
   *
   * @param url the location of the binary file to download. May also be a local or remote path to copy from.
   * @param targetFile the {@link Path} to the target file to download to. Should not already exist. Missing parent directories will be created
   *     automatically.
   */
  void download(String url, Path targetFile);

  /**
   * Creates the entire {@link Path} as directories if not already existing.
   *
   * @param directory the {@link Path} to {@link java.nio.file.Files#createDirectories(Path, java.nio.file.attribute.FileAttribute...) create}.
   */
  void mkdirs(Path directory);

  /**
   * @param file the {@link Path} to check.
   * @return {@code true} if the given {@code file} points to an existing file, {@code false} otherwise (the given {@link Path} does not exist or is a
   *     directory).
   */
  boolean isFile(Path file);

  /**
   * @param folder the {@link Path} to check.
   * @return {@code true} if the given {@code folder} points to an existing directory, {@code false} otherwise (a warning is logged in this case).
   */
  boolean isExpectedFolder(Path folder);

  /**
   * @param file the {@link Path} to compute the checksum of.
   * @param hashAlgorithm the hash algorithm (e.g. SHA-266).
   * @return the computed hash checksum as hex {@link String}.
   */
  String checksum(Path file, String hashAlgorithm);

  /**
   * Moves the given {@link Path} to the backup.
   *
   * @param fileOrFolder the {@link Path} to move to the backup (soft-deletion).
   * @return the {@link Path} in the backup where the given {@link Path} was moved to.
   */
  Path backup(Path fileOrFolder);

  /**
   * @param source the source {@link Path file or folder} to move.
   * @param targetDir the {@link Path} with the directory to move {@code source} into.
   * @param copyOptions the {@link java.nio.file.CopyOption} which specify how the move should be done
   */
  void move(Path source, Path targetDir, StandardCopyOption... copyOptions);

  /**
   * Creates a symbolic link. If the given {@code targetLink} already exists and is a symbolic link or a Windows junction, it will be replaced. In case of
   * missing privileges, Windows Junctions may be used as fallback, which must point to absolute paths. Therefore, the created link will be absolute instead of
   * relative.
   *
   * @param source the source {@link Path} to link to, may be relative or absolute.
   * @param targetLink the {@link Path} where the symbolic link shall be created pointing to {@code source}.
   * @param relative - {@code true} if the symbolic link shall be relative, {@code false} if it shall be absolute.
   */
  void symlink(Path source, Path targetLink, boolean relative);

  /**
   * Creates a relative symbolic link. If the given {@code targetLink} already exists and is a symbolic link or a Windows junction, it will be replaced. In case
   * of missing privileges, Windows Junctions may be used as fallback, which must point to absolute paths. Therefore, the created link will be absolute instead
   * of relative.
   *
   * @param source the source {@link Path} to link to, may be relative or absolute.
   * @param targetLink the {@link Path} where the symbolic link shall be created pointing to {@code source}.
   */
  default void symlink(Path source, Path targetLink) {

    symlink(source, targetLink, true);
  }

  /**
   * @param source the source {@link Path file or folder} to copy.
   * @param target the {@link Path} to copy {@code source} to. See {@link #copy(Path, Path, FileCopyMode)} for details. Will always ensure that in the end
   *     you will find the same content of {@code source} in {@code target}.
   */
  default void copy(Path source, Path target) {

    copy(source, target, FileCopyMode.COPY_TREE_FAIL_IF_EXISTS);
  }

  /**
   * @param source the source {@link Path file or folder} to copy.
   * @param target the {@link Path} to copy {@code source} to. Unlike the Linux {@code cp} command this method will not take the filename of {@code source}
   *     and copy that to {@code target} in case that is an existing folder. Instead it will always be simple and stupid and just copy from {@code source} to
   *     {@code target}. Therefore the result is always clear and easy to predict and understand. Also you can easily rename a file to copy. While
   *     {@code cp my-file target} may lead to a different result than {@code cp my-file target/} this method will always ensure that in the end you will find
   *     the same content of {@code source} in {@code target}.
   * @param mode the {@link FileCopyMode}.
   */
  default void copy(Path source, Path target, FileCopyMode mode) {

    copy(source, target, mode, PathCopyListener.NONE);
  }

  /**
   * @param source the source {@link Path file or folder} to copy.
   * @param target the {@link Path} to copy {@code source} to. Unlike the Linux {@code cp} command this method will not take the filename of {@code source}
   *     and copy that to {@code target} in case that is an existing folder. Instead it will always be simple and stupid and just copy from {@code source} to
   *     {@code target}. Therefore the result is always clear and easy to predict and understand. Also you can easily rename a file to copy. While
   *     {@code cp my-file target} may lead to a different result than {@code cp my-file target/} this method will always ensure that in the end you will find
   *     the same content of {@code source} in {@code target}.
   * @param mode the {@link FileCopyMode}.
   * @param listener the {@link PathCopyListener} that will be called for each copied {@link Path}.
   */
  void copy(Path source, Path target, FileCopyMode mode, PathCopyListener listener);

  /**
   * @param archiveFile the {@link Path} to the file to extract.
   * @param targetDir the {@link Path} to the directory where to extract the {@code archiveFile} to.
   */
  default void extract(Path archiveFile, Path targetDir) {

    extract(archiveFile, targetDir, null);
  }

  /**
   * @param archiveFile the {@link Path} to the archive file to extract.
   * @param targetDir the {@link Path} to the directory where to extract the {@code archiveFile}.
   * @param postExtractHook the {@link Consumer} to be called after the extraction on the final folder before it is moved to {@code targetDir}.
   */
  default void extract(Path archiveFile, Path targetDir, Consumer<Path> postExtractHook) {

    extract(archiveFile, targetDir, postExtractHook, true);
  }

  /**
   * @param archiveFile the {@link Path} to the archive file to extract.
   * @param targetDir the {@link Path} to the directory where to extract the {@code archiveFile}.
   * @param postExtractHook the {@link Consumer} to be called after the extraction on the final folder before it is moved to {@code targetDir}.
   * @param extract {@code true} if the {@code archiveFile} should be extracted (default), {@code false} otherwise.
   */
  void extract(Path archiveFile, Path targetDir, Consumer<Path> postExtractHook, boolean extract);

  /**
   * Extracts a ZIP file what is the common archive format on Windows. Initially invented by PKZIP for MS-DOS and also famous from WinZIP software for Windows.
   *
   * @param file the ZIP file to extract.
   * @param targetDir the {@link Path} with the directory to unzip to.
   */
  void extractZip(Path file, Path targetDir);

  /**
   * @param file the ZIP file to extract.
   * @param targetDir the {@link Path} with the directory to unzip to.
   * @param compression the {@link TarCompression} to use.
   */
  void extractTar(Path file, Path targetDir, TarCompression compression);

  /**
   * @param file the JAR file to extract.
   * @param targetDir the {@link Path} with the directory to extract to.
   */
  void extractJar(Path file, Path targetDir);

  /**
   * Extracts an Apple DMG (Disk Image) file that is similar to an ISO image. DMG files are commonly used for software releases on MacOS. Double-clicking such
   * files on MacOS mounts them and show the application together with a symbolic link to the central applications folder and some help instructions. The user
   * then copies the application to the applications folder via drag and drop in order to perform the installation.
   *
   * @param file the DMG file to extract.
   * @param targetDir the target directory where to extract the contents to.
   */
  void extractDmg(Path file, Path targetDir);

  /**
   * Extracts an MSI (Microsoft Installer) file. MSI files are commonly used for software releases on Windows that allow an installation wizard and easy later
   * uninstallation.
   *
   * @param file the MSI file to extract.
   * @param targetDir the target directory where to extract the contents to.
   */
  void extractMsi(Path file, Path targetDir);

  /**
   * Extracts an Apple PKG (Package) file. PKG files are used instead of {@link #extractDmg(Path, Path) DMG files} if additional changes have to be performed
   * like drivers to be installed. Similar to what {@link #extractMsi(Path, Path) MSI} is on Windows. PKG files are internally a xar based archive with a
   * specific structure.
   *
   * @param file the PKG file to extract.
   * @param targetDir the target directory where to extract the contents to.
   */
  void extractPkg(Path file, Path targetDir);

  /**
   * @param dir the {@link Path directory} to compress.
   * @param out the {@link OutputStream} to write the compressed data to.
   * @param format the path, filename or extension to derive the archive format from (e.g. "tgz", "tar.gz", "zip", etc.).
   */
  void compress(Path dir, OutputStream out, String format);

  /**
   * @param dir the {@link Path directory} to compress as TAR with given {@link TarCompression}.
   * @param out the {@link OutputStream} to write the compressed data to.
   * @param tarCompression the {@link TarCompression} to use for the TAR archive.
   */
  void compressTar(Path dir, OutputStream out, TarCompression tarCompression);

  /**
   * @param dir the {@link Path directory} to compress as TAR.
   * @param out the {@link OutputStream} to write the compressed data to.
   */
  void compressTar(Path dir, OutputStream out);

  /**
   * @param dir the {@link Path directory} to compress as TGZ.
   * @param out the {@link OutputStream} to write the compressed data to.
   */
  void compressTarGz(Path dir, OutputStream out);

  /**
   * @param dir the {@link Path directory} to compress as TBZ2.
   * @param out the {@link OutputStream} to write the compressed data to.
   */
  void compressTarBzip2(Path dir, OutputStream out);

  /**
   * @param dir the {@link Path directory} to compress as ZIP.
   * @param out the {@link OutputStream} to write the compressed data to.
   */
  void compressZip(Path dir, OutputStream out);

  /**
   * @param path the {@link Path} to convert.
   * @return the absolute and physical {@link Path} (without symbolic links).
   */
  Path toRealPath(Path path);

  /**
   * @param path the {@link Path} to convert.
   * @return the absolute and physical {@link Path}.
   */
  Path toCanonicalPath(Path path);

  /**
   * Deletes the given {@link Path} idempotent and recursive.
   * <p>
   * ATTENTION: In most cases we want to use {@link #backup(Path)} instead to prevent the user from data loss.
   * </p>
   *
   * @param path the {@link Path} to delete.
   */
  void delete(Path path);

  /**
   * Creates a new temporary directory. ATTENTION: The user of this method is responsible to do house-keeping and {@link #delete(Path) delete} it after the work
   * is done.
   *
   * @param name the default name of the temporary directory to create. A prefix or suffix may be added to ensure uniqueness.
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
   * @param dir the {@link Path} to the directory where to list the children.
   * @param filter the {@link Predicate} used to {@link Predicate#test(Object) decide} which children to include (if {@code true} is returned).
   * @return all children of the given {@link Path} that match the given {@link Predicate}. Will be the empty list of the given {@link Path} is not an existing
   *     directory.
   */
  default List<Path> listChildren(Path dir, Predicate<Path> filter) {

    return listChildrenMapped(dir, child -> (filter.test(child)) ? child : null);
  }

  /**
   * @param dir the {@link Path} to the directory where to list the children.
   * @param filter the filter {@link Function} used to {@link Function#apply(Object) filter and transform} children to include. If the {@link Function}
   *     returns  {@code null}, the child will be filtered, otherwise the returned {@link Path} will be included in the resulting {@link List}.
   * @return all children of the given {@link Path} returned by the given {@link Function}. Will be the empty list if the given {@link Path} is not an existing
   *     directory.
   */
  List<Path> listChildrenMapped(Path dir, Function<Path, Path> filter);

  /**
   * Finds the existing file with the specified name in the given list of directories.
   *
   * @param fileName The name of the file to find.
   * @param searchDirs The list of directories to search for the file.
   * @return The {@code Path} of the existing file, or {@code null} if the file is not found.
   */
  Path findExistingFile(String fileName, List<Path> searchDirs);

  /**
   * Checks if the given directory is empty.
   *
   * @param dir The {@link Path} object representing the directory to check.
   * @return {@code true} if the directory is empty, {@code false} otherwise.
   */
  boolean isEmptyDir(Path dir);

  /**
   * Sets or unsets the writable permission for the specified file path.
   *
   * @param file {@link Path} to the file.
   * @param writable {@code true} to make the file writable, {@code false} to make it read-only
   * @return {@code true} if the operation was successful or supported, {@code false} otherwise
   */
  boolean setWritable(Path file, boolean writable);

  /**
   * Makes a file executable (analog to 'chmod a+x').
   *
   * @param file {@link Path} to the file.
   */
  default void makeExecutable(Path file) {

    makeExecutable(file, false);
  }

  /**
   * Makes a file executable (analog to 'chmod a+x').
   *
   * @param file {@link Path} to the file.
   * @param confirm - {@code true} to get user confirmation before adding missing executable flags, {@code false} otherwise (always set missing flags).
   */
  void makeExecutable(Path file, boolean confirm);

  /**
   * Like the linux touch command this method will update the modification time of the given {@link Path} to the current
   * {@link System#currentTimeMillis() system time}. In case the file does not exist, it will be created as empty file. If already the
   * {@link Path#getParent() parent folder} does not exist, the operation will fail.
   *
   * @param file the {@link Path} to the file or folder.
   */
  void touch(Path file);

  /**
   * @param file the {@link Path} to the file to read.
   * @return the content of the specified file (in UTF-8 encoding), or null if the file doesn't exist
   * @see java.nio.file.Files#readString(Path)
   */
  String readFileContent(Path file);

  /**
   * @param content the {@link String} with the text to write to a file.
   * @param file the {@link Path} to the file where to save.
   */
  default void writeFileContent(String content, Path file) {

    writeFileContent(content, file, false);
  }

  /**
   * @param content the {@link String} with the text to write to a file.
   * @param file the {@link Path} to the file where to save.
   * @param createParentDir if {@code true}, the parent directory will be created if it does not already exist, {@code false} otherwise (fail if parent does
   *     not exist).
   */
  void writeFileContent(String content, Path file, boolean createParentDir);

  /**
   * Like {@link #readFileContent(Path)} but giving one {@link String} per line of text. It will not allow to preserve line endings (CRLF vs. LF).
   *
   * @param file the {@link Path} to the file to read.
   * @return the content of the specified file (in UTF-8 encoding) as {@link List} of {@link String}s per line of text.
   */
  List<String> readFileLines(Path file);

  /**
   * Like {@link #writeFileContent(String, Path)} but taking a {@link List} with one {@link String} per line of text. It will always use LF as newline character
   * independent of the operating system.
   *
   * @param lines the {@link List} of {@link String}s per line of text.
   * @param file the {@link Path} to the file where to save.
   */
  default void writeFileLines(List<String> lines, Path file) {
    writeFileLines(lines, file, false);
  }

  /**
   * Like {@link #writeFileContent(String, Path, boolean)} but taking a {@link List} with one {@link String} per line of text. It will always use LF as newline
   * character independent of the operating system.
   *
   * @param lines the {@link List} of {@link String}s per line of text.
   * @param file the {@link Path} to the file where to save.
   * @param createParentDir if {@code true}, the parent directory will be created if it does not already exist, {@code false} otherwise (fail if parent does
   *     not exist).
   */
  void writeFileLines(List<String> lines, Path file, boolean createParentDir);

  /**
   * @param path that is checked whether it is a junction or not.
   * @return {@code true} if the given {@link Path} is a junction, false otherwise.
   */
  boolean isJunction(Path path);

  /**
   * @param file the {@link Path} to the {@link Properties} file to read.
   * @return the parsed {@link Properties}.
   */
  default Properties readProperties(Path file) {
    Properties properties = new Properties();
    readProperties(file, properties);
    return properties;
  }

  /**
   * @param file the {@link Path} to the {@link Properties} file to read.
   * @param properties the existing {@link Properties} to {@link Properties#load(Reader) load} into.
   */
  void readProperties(Path file, Properties properties);

  /**
   * @param properties the {@link Properties} to save.
   * @param file the {@link Path} to the file where to save the properties.
   */
  default void writeProperties(Properties properties, Path file) {

    writeProperties(properties, file, false);
  }


  /**
   * @param properties the {@link Properties} to save.
   * @param file the {@link Path} to the file where to save the properties.
   * @param createParentDir if {@code true}, the parent directory will created if it does not already exist, {@code false} otherwise (fail if parent does
   *     not exist).
   */
  void writeProperties(Properties properties, Path file, boolean createParentDir);

  /**
   * @param file the {@link Path} to read from
   * @return {@link IniFile}
   */
  default IniFile readIniFile(Path file) {
    IniFile iniFile = new IniFileImpl();
    readIniFile(file, iniFile);
    return iniFile;
  }

  /**
   * @param file the {@link Path} to read from
   * @param iniFile the {@link IniFile} object the data is loaded into
   */
  void readIniFile(Path file, IniFile iniFile);

  /**
   * @param iniFile the {@link IniFile} object
   * @param file the {@link Path} to write to
   */
  default void writeIniFile(IniFile iniFile, Path file) {
    writeIniFile(iniFile, file, false);
  }

  /**
   * @param iniFile the {@link IniFile} object
   * @param file the {@link Path} to write to
   * @param createParentDir whether to create missing parent directories
   */
  void writeIniFile(IniFile iniFile, Path file, boolean createParentDir);

  /**
   * @param path the {@link Path} to get the age from the modification time.
   * @return the age of the file as {@link Duration} from now to the modification time of the file.
   */
  public Duration getFileAge(Path path);

  /**
   * @param path the {@link Path} to check.
   * @param cacheDuration the {@link Duration} to consider as recent.
   * @return {@code true} if the given {@link Path} exists and is recent enough (its {@link #getFileAge(Path) age} is not greater than the given
   *     {@link Duration}), {@code false} otherwise.
   */
  boolean isFileAgeRecent(Path path, Duration cacheDuration);
}
