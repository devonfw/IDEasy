package com.devonfw.tools.ide.io;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.jline.utils.Log;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.cli.CliOfflineException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.util.DateTimeUtil;
import com.devonfw.tools.ide.util.FilenameUtil;
import com.devonfw.tools.ide.util.HexUtil;

/**
 * Implementation of {@link FileAccess}.
 */
public class FileAccessImpl implements FileAccess {

  private static final String WINDOWS_FILE_LOCK_DOCUMENTATION_PAGE = "https://github.com/devonfw/IDEasy/blob/main/documentation/windows-file-lock.adoc";

  private static final String WINDOWS_FILE_LOCK_WARNING =
      "On Windows, file operations could fail due to file locks. Please ensure the files in the moved directory are not in use. For further details, see: \n"
          + WINDOWS_FILE_LOCK_DOCUMENTATION_PAGE;

  private static final Map<String, String> FS_ENV = Map.of("encoding", "UTF-8");

  private final IdeContext context;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext} to use.
   */
  public FileAccessImpl(IdeContext context) {

    super();
    this.context = context;
  }

  private HttpClient createHttpClient(String url) {

    HttpClient.Builder builder = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS);
    return builder.build();
  }

  @Override
  public void download(String url, Path target) {

    this.context.info("Trying to download {} from {}", target.getFileName(), url);
    mkdirs(target.getParent());
    try {
      if (this.context.isOffline()) {
        throw CliOfflineException.ofDownloadViaUrl(url);
      }
      if (url.startsWith("http")) {

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpClient client = createHttpClient(url);
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        int statusCode = response.statusCode();
        if (statusCode == 200) {
          downloadFileWithProgressBar(url, target, response);
        } else {
          throw new IllegalStateException("Download failed with status code " + statusCode);
        }
      } else if (url.startsWith("ftp") || url.startsWith("sftp")) {
        throw new IllegalArgumentException("Unsupported download URL: " + url);
      } else {
        Path source = Path.of(url);
        if (isFile(source)) {
          // network drive

          copyFileWithProgressBar(source, target);
        } else {
          throw new IllegalArgumentException("Download path does not point to a downloadable file: " + url);
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to download file from URL " + url + " to " + target, e);
    }
  }

  /**
   * Downloads a file while showing a {@link IdeProgressBar}.
   *
   * @param url the url to download.
   * @param target Path of the target directory.
   * @param response the {@link HttpResponse} to use.
   */
  private void downloadFileWithProgressBar(String url, Path target, HttpResponse<InputStream> response) {

    long contentLength = response.headers().firstValueAsLong("content-length").orElse(-1);
    informAboutMissingContentLength(contentLength, url);

    byte[] data = new byte[1024];
    boolean fileComplete = false;
    int count;

    try (InputStream body = response.body();
        FileOutputStream fileOutput = new FileOutputStream(target.toFile());
        BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOutput, data.length);
        IdeProgressBar pb = this.context.newProgressBarForDownload(contentLength)) {
      while (!fileComplete) {
        count = body.read(data);
        if (count <= 0) {
          fileComplete = true;
        } else {
          bufferedOut.write(data, 0, count);
          pb.stepBy(count);
        }
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Copies a file while displaying a progress bar.
   *
   * @param source Path of file to copy.
   * @param target Path of target directory.
   */
  private void copyFileWithProgressBar(Path source, Path target) throws IOException {

    try (InputStream in = new FileInputStream(source.toFile()); OutputStream out = new FileOutputStream(target.toFile())) {
      long size = getFileSize(source);
      byte[] buf = new byte[1024];
      try (IdeProgressBar pb = this.context.newProgressbarForCopying(size)) {
        int readBytes;
        while ((readBytes = in.read(buf)) > 0) {
          out.write(buf, 0, readBytes);
          if (size > 0) {
            pb.stepBy(readBytes);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void informAboutMissingContentLength(long contentLength, String url) {

    if (contentLength < 0) {
      this.context.warning("Content-Length was not provided by download from {}", url);
    }
  }

  @Override
  public void mkdirs(Path directory) {

    if (Files.isDirectory(directory)) {
      return;
    }
    this.context.trace("Creating directory {}", directory);
    try {
      Files.createDirectories(directory);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create directory " + directory, e);
    }
  }

  @Override
  public boolean isFile(Path file) {

    if (!Files.exists(file)) {
      this.context.trace("File {} does not exist", file);
      return false;
    }
    if (Files.isDirectory(file)) {
      this.context.trace("Path {} is a directory but a regular file was expected", file);
      return false;
    }
    return true;
  }

  @Override
  public boolean isExpectedFolder(Path folder) {

    if (Files.isDirectory(folder)) {
      return true;
    }
    this.context.warning("Expected folder was not found at {}", folder);
    return false;
  }

  @Override
  public String checksum(Path file, String hashAlgorithm) {

    MessageDigest md;
    try {
      md = MessageDigest.getInstance(hashAlgorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("No such hash algorithm " + hashAlgorithm, e);
    }
    byte[] buffer = new byte[1024];
    try (InputStream is = Files.newInputStream(file); DigestInputStream dis = new DigestInputStream(is, md)) {
      int read = 0;
      while (read >= 0) {
        read = dis.read(buffer);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read and hash file " + file, e);
    }
    byte[] digestBytes = md.digest();
    return HexUtil.toHexString(digestBytes);
  }

  public boolean isJunction(Path path) {

    if (!SystemInfoImpl.INSTANCE.isWindows()) {
      return false;
    }

    try {
      BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      return attr.isOther() && attr.isDirectory();
    } catch (NoSuchFileException e) {
      return false; // file doesn't exist
    } catch (IOException e) {
      // errors in reading the attributes of the file
      throw new IllegalStateException("An unexpected error occurred whilst checking if the file: " + path + " is a junction", e);
    }
  }

  @Override
  public Path backup(Path fileOrFolder) {

    if ((fileOrFolder != null) && (Files.isSymbolicLink(fileOrFolder) || isJunction(fileOrFolder))) {
      delete(fileOrFolder);
    } else if ((fileOrFolder != null) && Files.exists(fileOrFolder)) {
      LocalDateTime now = LocalDateTime.now();
      String date = DateTimeUtil.formatDate(now, true);
      String time = DateTimeUtil.formatTime(now);
      String filename = fileOrFolder.getFileName().toString();
      Path backupPath = this.context.getIdeHome().resolve(IdeContext.FOLDER_BACKUPS).resolve(date).resolve(time + "_" + filename);
      backupPath = appendParentPath(backupPath, fileOrFolder.getParent(), 2);
      mkdirs(backupPath);
      Path target = backupPath.resolve(filename);
      this.context.info("Creating backup by moving {} to {}", fileOrFolder, target);
      move(fileOrFolder, target);
      return target;
    } else {
      this.context.trace("Backup of {} skipped as the path does not exist.", fileOrFolder);
    }
    return fileOrFolder;
  }

  private static Path appendParentPath(Path path, Path parent, int max) {

    if ((parent == null) || (max <= 0)) {
      return path;
    }
    return appendParentPath(path, parent.getParent(), max - 1).resolve(parent.getFileName());
  }

  @Override
  public void move(Path source, Path targetDir, StandardCopyOption... copyOptions) {

    this.context.trace("Moving {} to {}", source, targetDir);
    try {
      Files.move(source, targetDir, copyOptions);
    } catch (IOException e) {
      String fileType = Files.isSymbolicLink(source) ? "symlink" : isJunction(source) ? "junction" : Files.isDirectory(source) ? "directory" : "file";
      String message = "Failed to move " + fileType + ": " + source + " to " + targetDir + ".";
      if (this.context.getSystemInfo().isWindows()) {
        message = message + "\n" + WINDOWS_FILE_LOCK_WARNING;
      }
      throw new IllegalStateException(message, e);
    }
  }

  @Override
  public void copy(Path source, Path target, FileCopyMode mode, PathCopyListener listener) {

    if (mode != FileCopyMode.COPY_TREE_CONTENT) {
      // if we want to copy the file or folder "source" to the existing folder "target" in a shell this will copy
      // source into that folder so that we as a result have a copy in "target/source".
      // With Java NIO the raw copy method will fail as we cannot copy "source" to the path of the "target" folder.
      // For folders we want the same behavior as the linux "cp -r" command so that the "source" folder is copied
      // and not only its content what also makes it consistent with the move method that also behaves this way.
      // Therefore we need to add the filename (foldername) of "source" to the "target" path before.
      // For the rare cases, where we want to copy the content of a folder (cp -r source/* target) we support
      // it via the COPY_TREE_CONTENT mode.
      Path fileName = source.getFileName();
      if (fileName != null) { // if filename is null, we are copying the root of a (virtual filesystem)
        target = target.resolve(fileName.toString());
      }
    }
    boolean fileOnly = mode.isFileOnly();
    String operation = mode.getOperation();
    if (mode.isExtract()) {
      this.context.debug("Starting to {} to {}", operation, target);
    } else {
      if (fileOnly) {
        this.context.debug("Starting to {} file {} to {}", operation, source, target);
      } else {
        this.context.debug("Starting to {} {} recursively to {}", operation, source, target);
      }
    }
    if (fileOnly && Files.isDirectory(source)) {
      throw new IllegalStateException("Expected file but found a directory to copy at " + source);
    }
    if (mode.isFailIfExists()) {
      if (Files.exists(target)) {
        throw new IllegalStateException("Failed to " + operation + " " + source + " to already existing target " + target);
      }
    } else if (mode == FileCopyMode.COPY_TREE_OVERRIDE_TREE) {
      delete(target);
    }
    try {
      copyRecursive(source, target, mode, listener);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to " + operation + " " + source + " to " + target, e);
    }
  }

  private void copyRecursive(Path source, Path target, FileCopyMode mode, PathCopyListener listener) throws IOException {

    if (Files.isDirectory(source)) {
      mkdirs(target);
      try (Stream<Path> childStream = Files.list(source)) {
        Iterator<Path> iterator = childStream.iterator();
        while (iterator.hasNext()) {
          Path child = iterator.next();
          copyRecursive(child, target.resolve(child.getFileName().toString()), mode, listener);
        }
      }
      listener.onCopy(source, target, true);
    } else if (Files.exists(source)) {
      if (mode.isOverrideFile()) {
        delete(target);
      }
      this.context.trace("Starting to {} {} to {}", mode.getOperation(), source, target);
      Files.copy(source, target);
      listener.onCopy(source, target, false);
    } else {
      throw new IOException("Path " + source + " does not exist.");
    }
  }

  /**
   * Deletes the given {@link Path} if it is a symbolic link or a Windows junction. And throws an {@link IllegalStateException} if there is a file at the given
   * {@link Path} that is neither a symbolic link nor a Windows junction.
   *
   * @param path the {@link Path} to delete.
   * @throws IOException if the actual {@link Files#delete(Path) deletion} fails.
   */
  private void deleteLinkIfExists(Path path) throws IOException {

    boolean isJunction = isJunction(path); // since broken junctions are not detected by Files.exists()
    boolean isSymlink = Files.exists(path) && Files.isSymbolicLink(path);

    assert !(isSymlink && isJunction);

    if (isJunction || isSymlink) {
      this.context.info("Deleting previous " + (isJunction ? "junction" : "symlink") + " at " + path);
      Files.delete(path);
    }
  }

  /**
   * Adapts the given {@link Path} to be relative or absolute depending on the given {@code relative} flag. Additionally, {@link Path#toRealPath(LinkOption...)}
   * is applied to {@code source}.
   *
   * @param source the {@link Path} to adapt.
   * @param targetLink the {@link Path} used to calculate the relative path to the {@code source} if {@code relative} is set to {@code true}.
   * @param relative the {@code relative} flag.
   * @return the adapted {@link Path}.
   * @see FileAccessImpl#symlink(Path, Path, boolean)
   */
  private Path adaptPath(Path source, Path targetLink, boolean relative) throws IOException {

    if (source.isAbsolute()) {
      try {
        source = source.toRealPath(LinkOption.NOFOLLOW_LINKS); // to transform ../d1/../d2 to ../d2
      } catch (IOException e) {
        throw new IOException("Calling toRealPath() on the source (" + source + ") in method FileAccessImpl.adaptPath() failed.", e);
      }
      if (relative) {
        source = targetLink.getParent().relativize(source);
        // to make relative links like this work: dir/link -> dir
        source = (source.toString().isEmpty()) ? Path.of(".") : source;
      }
    } else { // source is relative
      if (relative) {
        // even though the source is already relative, toRealPath should be called to transform paths like
        // this ../d1/../d2 to ../d2
        source = targetLink.getParent().relativize(targetLink.resolveSibling(source).toRealPath(LinkOption.NOFOLLOW_LINKS));
        source = (source.toString().isEmpty()) ? Path.of(".") : source;
      } else { // !relative
        try {
          source = targetLink.resolveSibling(source).toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
          throw new IOException("Calling toRealPath() on " + targetLink + ".resolveSibling(" + source + ") in method FileAccessImpl.adaptPath() failed.", e);
        }
      }
    }
    return source;
  }

  /**
   * Creates a Windows junction at {@code targetLink} pointing to {@code source}.
   *
   * @param source must be another Windows junction or a directory.
   * @param targetLink the location of the Windows junction.
   */
  private void createWindowsJunction(Path source, Path targetLink) {

    this.context.trace("Creating a Windows junction at " + targetLink + " with " + source + " as source.");
    Path fallbackPath;
    if (!source.isAbsolute()) {
      this.context.warning("You are on Windows and you do not have permissions to create symbolic links. Junctions are used as an "
          + "alternative, however, these can not point to relative paths. So the source (" + source + ") is interpreted as an absolute path.");
      try {
        fallbackPath = targetLink.resolveSibling(source).toRealPath(LinkOption.NOFOLLOW_LINKS);
      } catch (IOException e) {
        throw new IllegalStateException(
            "Since Windows junctions are used, the source must be an absolute path. The transformation of the passed " + "source (" + source
                + ") to an absolute path failed.", e);
      }

    } else {
      fallbackPath = source;
    }
    if (!Files.isDirectory(fallbackPath)) { // if source is a junction. This returns true as well.
      throw new IllegalStateException(
          "These junctions can only point to directories or other junctions. Please make sure that the source (" + fallbackPath + ") is one of these.");
    }
    this.context.newProcess().executable("cmd").addArgs("/c", "mklink", "/d", "/j", targetLink.toString(), fallbackPath.toString()).run();
  }

  @Override
  public void symlink(Path source, Path targetLink, boolean relative) {

    Path adaptedSource = null;
    try {
      adaptedSource = adaptPath(source, targetLink, relative);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to adapt source for source (" + source + ") target (" + targetLink + ") and relative (" + relative + ")", e);
    }
    this.context.debug("Creating {} symbolic link {} pointing to {}", adaptedSource.isAbsolute() ? "" : "relative", targetLink, adaptedSource);

    try {
      deleteLinkIfExists(targetLink);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to delete previous symlink or Windows junction at " + targetLink, e);
    }

    try {
      Files.createSymbolicLink(targetLink, adaptedSource);
    } catch (FileSystemException e) {
      if (SystemInfoImpl.INSTANCE.isWindows()) {
        this.context.info("Due to lack of permissions, Microsoft's mklink with junction had to be used to create "
            + "a Symlink. See https://github.com/devonfw/IDEasy/blob/main/documentation/symlink.adoc for " + "further details. Error was: "
            + e.getMessage());
        createWindowsJunction(adaptedSource, targetLink);
      } else {
        throw new RuntimeException(e);
      }
    } catch (IOException e) {
      throw new IllegalStateException(
          "Failed to create a " + (adaptedSource.isAbsolute() ? "" : "relative") + "symbolic link " + targetLink + " pointing to " + source, e);
    }
  }

  @Override
  public Path toRealPath(Path path) {

    return toRealPath(path, true);
  }

  @Override
  public Path toCanonicalPath(Path path) {

    return toRealPath(path, false);
  }

  private Path toRealPath(Path path, boolean resolveLinks) {

    try {
      Path realPath;
      if (resolveLinks) {
        realPath = path.toRealPath();
      } else {
        realPath = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
      }
      if (!realPath.equals(path)) {
        this.context.trace("Resolved path {} to {}", path, realPath);
      }
      return realPath;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to get real path for " + path, e);
    }
  }

  @Override
  public Path createTempDir(String name) {

    try {
      Path tmp = this.context.getTempPath();
      Path tempDir = tmp.resolve(name);
      int tries = 1;
      while (Files.exists(tempDir)) {
        long id = System.nanoTime() & 0xFFFF;
        tempDir = tmp.resolve(name + "-" + id);
        tries++;
        if (tries > 200) {
          throw new IOException("Unable to create unique name!");
        }
      }
      return Files.createDirectory(tempDir);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create temporary directory with prefix '" + name + "'!", e);
    }
  }

  @Override
  public void extract(Path archiveFile, Path targetDir, Consumer<Path> postExtractHook, boolean extract) {

    if (Files.isDirectory(archiveFile)) {
      // TODO: check this case
      Path properInstallDir = archiveFile; // getProperInstallationSubDirOf(archiveFile, archiveFile);
      this.context.warning("Found directory for download at {} hence copying without extraction!", archiveFile);
      copy(properInstallDir, targetDir, FileCopyMode.COPY_TREE_CONTENT);
      postExtractHook(postExtractHook, targetDir);
      return;
    } else if (!extract) {
      mkdirs(targetDir);
      move(archiveFile, targetDir.resolve(archiveFile.getFileName()));
      return;
    }
    Path tmpDir = createTempDir("extract-" + archiveFile.getFileName());
    this.context.trace("Trying to extract the downloaded file {} to {} and move it to {}.", archiveFile, tmpDir, targetDir);
    String filename = archiveFile.getFileName().toString();
    TarCompression tarCompression = TarCompression.of(filename);
    if (tarCompression != null) {
      extractTar(archiveFile, tmpDir, tarCompression);
    } else {
      String extension = FilenameUtil.getExtension(filename);
      if (extension == null) {
        throw new IllegalStateException("Unknown archive format without extension - can not extract " + archiveFile);
      } else {
        this.context.trace("Determined file extension {}", extension);
      }
      switch (extension) {
        case "zip" -> extractZip(archiveFile, tmpDir);
        case "jar" -> extractJar(archiveFile, tmpDir);
        case "dmg" -> extractDmg(archiveFile, tmpDir);
        case "msi" -> extractMsi(archiveFile, tmpDir);
        case "pkg" -> extractPkg(archiveFile, tmpDir);
        default -> throw new IllegalStateException("Unknown archive format " + extension + ". Can not extract " + archiveFile);
      }
    }
    Path properInstallDir = getProperInstallationSubDirOf(tmpDir, archiveFile);
    postExtractHook(postExtractHook, properInstallDir);
    move(properInstallDir, targetDir);
    delete(tmpDir);
  }

  private void postExtractHook(Consumer<Path> postExtractHook, Path properInstallDir) {

    if (postExtractHook != null) {
      postExtractHook.accept(properInstallDir);
    }
  }

  /**
   * @param path the {@link Path} to start the recursive search from.
   * @return the deepest subdir {@code s} of the passed path such that all directories between {@code s} and the passed path (including {@code s}) are the sole
   *     item in their respective directory and {@code s} is not named "bin".
   */
  private Path getProperInstallationSubDirOf(Path path, Path archiveFile) {

    try (Stream<Path> stream = Files.list(path)) {
      Path[] subFiles = stream.toArray(Path[]::new);
      if (subFiles.length == 0) {
        throw new CliException("The downloaded package " + archiveFile + " seems to be empty as you can check in the extracted folder " + path);
      } else if (subFiles.length == 1) {
        String filename = subFiles[0].getFileName().toString();
        if (!filename.equals(IdeContext.FOLDER_BIN) && !filename.equals(IdeContext.FOLDER_CONTENTS) && !filename.endsWith(".app") && Files.isDirectory(
            subFiles[0])) {
          return getProperInstallationSubDirOf(subFiles[0], archiveFile);
        }
      }
      return path;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to get sub-files of " + path);
    }
  }

  @Override
  public void extractZip(Path file, Path targetDir) {

    this.context.info("Extracting ZIP file {} to {}", file, targetDir);
    URI uri = URI.create("jar:" + file.toUri());
    try (FileSystem fs = FileSystems.newFileSystem(uri, FS_ENV)) {
      long size = 0;
      for (Path root : fs.getRootDirectories()) {
        size += getFileSizeRecursive(root);
      }
      try (final IdeProgressBar progressBar = this.context.newProgressbarForExtracting(size)) {
        for (Path root : fs.getRootDirectories()) {
          copy(root, targetDir, FileCopyMode.EXTRACT, (s, t, d) -> onFileCopiedFromZip(s, t, d, progressBar));
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to extract " + file + " to " + targetDir, e);
    }
  }

  @SuppressWarnings("unchecked")
  private void onFileCopiedFromZip(Path source, Path target, boolean directory, IdeProgressBar progressBar) {

    if (directory) {
      return;
    }
    if (!context.getSystemInfo().isWindows()) {
      try {
        Object attribute = Files.getAttribute(source, "zip:permissions");
        if (attribute instanceof Set<?> permissionSet) {
          Files.setPosixFilePermissions(target, (Set<PosixFilePermission>) permissionSet);
        }
      } catch (Exception e) {
        context.error(e, "Failed to transfer zip permissions for {}", target);
      }
    }
    progressBar.stepBy(getFileSize(target));
  }

  @Override
  public void extractTar(Path file, Path targetDir, TarCompression compression) {

    extractArchive(file, targetDir, in -> new TarArchiveInputStream(compression.unpack(in)));
  }

  @Override
  public void extractJar(Path file, Path targetDir) {

    extractZip(file, targetDir);
  }

  /**
   * @param permissions The integer as returned by {@link TarArchiveEntry#getMode()} that represents the file permissions of a file on a Unix file system.
   * @return A String representing the file permissions. E.g. "rwxrwxr-x" or "rw-rw-r--"
   */
  public static String generatePermissionString(int permissions) {

    // Ensure that only the last 9 bits are considered
    permissions &= 0b111111111;

    StringBuilder permissionStringBuilder = new StringBuilder("rwxrwxrwx");
    for (int i = 0; i < 9; i++) {
      int mask = 1 << i;
      char currentChar = ((permissions & mask) != 0) ? permissionStringBuilder.charAt(8 - i) : '-';
      permissionStringBuilder.setCharAt(8 - i, currentChar);
    }

    return permissionStringBuilder.toString();
  }

  private void extractArchive(Path file, Path targetDir, Function<InputStream, ArchiveInputStream<?>> unpacker) {

    this.context.info("Extracting TAR file {} to {}", file, targetDir);
    try (InputStream is = Files.newInputStream(file);
        ArchiveInputStream<?> ais = unpacker.apply(is);
        IdeProgressBar pb = this.context.newProgressbarForExtracting(getFileSize(file))) {

      ArchiveEntry entry = ais.getNextEntry();
      boolean isTar = ais instanceof TarArchiveInputStream;
      while (entry != null) {
        String permissionStr = null;
        if (isTar) {
          int tarMode = ((TarArchiveEntry) entry).getMode();
          permissionStr = generatePermissionString(tarMode);
        }
        Path entryName = Path.of(entry.getName());
        Path entryPath = targetDir.resolve(entryName).toAbsolutePath();
        if (!entryPath.startsWith(targetDir)) {
          throw new IOException("Preventing path traversal attack from " + entryName + " to " + entryPath);
        }
        if (entry.isDirectory()) {
          mkdirs(entryPath);
        } else {
          // ensure the file can also be created if directory entry was missing or out of order...
          mkdirs(entryPath.getParent());
          Files.copy(ais, entryPath);
        }
        if (isTar && !this.context.getSystemInfo().isWindows()) {
          Set<PosixFilePermission> permissions = PosixFilePermissions.fromString(permissionStr);
          Files.setPosixFilePermissions(entryPath, permissions);
        }
        pb.stepBy(entry.getSize());
        entry = ais.getNextEntry();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to extract " + file + " to " + targetDir, e);
    }
  }

  @Override
  public void extractDmg(Path file, Path targetDir) {

    this.context.info("Extracting DMG file {} to {}", file, targetDir);
    assert this.context.getSystemInfo().isMac();

    Path mountPath = this.context.getIdeHome().resolve(IdeContext.FOLDER_UPDATES).resolve(IdeContext.FOLDER_VOLUME);
    mkdirs(mountPath);
    ProcessContext pc = this.context.newProcess();
    pc.executable("hdiutil");
    pc.addArgs("attach", "-quiet", "-nobrowse", "-mountpoint", mountPath, file);
    pc.run();
    Path appPath = findFirst(mountPath, p -> p.getFileName().toString().endsWith(".app"), false);
    if (appPath == null) {
      throw new IllegalStateException("Failed to unpack DMG as no MacOS *.app was found in file " + file);
    }

    copy(appPath, targetDir, FileCopyMode.COPY_TREE_OVERRIDE_TREE);
    pc.addArgs("detach", "-force", mountPath);
    pc.run();
  }

  @Override
  public void extractMsi(Path file, Path targetDir) {

    this.context.info("Extracting MSI file {} to {}", file, targetDir);
    this.context.newProcess().executable("msiexec").addArgs("/a", file, "/qn", "TARGETDIR=" + targetDir).run();
    // msiexec also creates a copy of the MSI
    Path msiCopy = targetDir.resolve(file.getFileName());
    delete(msiCopy);
  }

  @Override
  public void extractPkg(Path file, Path targetDir) {

    this.context.info("Extracting PKG file {} to {}", file, targetDir);
    Path tmpDirPkg = createTempDir("ide-pkg-");
    ProcessContext pc = this.context.newProcess();
    // we might also be able to use cpio from commons-compression instead of external xar...
    pc.executable("xar").addArgs("-C", tmpDirPkg, "-xf", file).run();
    Path contentPath = findFirst(tmpDirPkg, p -> p.getFileName().toString().equals("Payload"), true);
    extractTar(contentPath, targetDir, TarCompression.GZ);
    delete(tmpDirPkg);
  }

  @Override
  public void delete(Path path) {

    if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
      this.context.trace("Deleting {} skipped as the path does not exist.", path);
      return;
    }
    this.context.debug("Deleting {} ...", path);
    try {
      if (Files.isSymbolicLink(path) || isJunction(path)) {
        Files.delete(path);
      } else {
        deleteRecursive(path);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to delete " + path, e);
    }
  }

  private void deleteRecursive(Path path) throws IOException {

    if (Files.isDirectory(path)) {
      try (Stream<Path> childStream = Files.list(path)) {
        Iterator<Path> iterator = childStream.iterator();
        while (iterator.hasNext()) {
          Path child = iterator.next();
          deleteRecursive(child);
        }
      }
    }
    this.context.trace("Deleting {} ...", path);
    Files.delete(path);
  }

  @Override
  public Path findFirst(Path dir, Predicate<Path> filter, boolean recursive) {

    try {
      if (!Files.isDirectory(dir)) {
        return null;
      }
      return findFirstRecursive(dir, filter, recursive);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to search for file in " + dir, e);
    }
  }

  private Path findFirstRecursive(Path dir, Predicate<Path> filter, boolean recursive) throws IOException {

    List<Path> folders = null;
    try (Stream<Path> childStream = Files.list(dir)) {
      Iterator<Path> iterator = childStream.iterator();
      while (iterator.hasNext()) {
        Path child = iterator.next();
        if (filter.test(child)) {
          return child;
        } else if (recursive && Files.isDirectory(child)) {
          if (folders == null) {
            folders = new ArrayList<>();
          }
          folders.add(child);
        }
      }
    }
    if (folders != null) {
      for (Path child : folders) {
        Path match = findFirstRecursive(child, filter, recursive);
        if (match != null) {
          return match;
        }
      }
    }
    return null;
  }

  @Override
  public List<Path> listChildrenMapped(Path dir, Function<Path, Path> filter) {

    if (!Files.isDirectory(dir)) {
      return List.of();
    }
    List<Path> children = new ArrayList<>();
    try (Stream<Path> childStream = Files.list(dir)) {
      Iterator<Path> iterator = childStream.iterator();
      while (iterator.hasNext()) {
        Path child = iterator.next();
        Path filteredChild = filter.apply(child);
        if (filteredChild != null) {
          if (filteredChild == child) {
            this.context.trace("Accepted file {}", child);
          } else {
            this.context.trace("Accepted file {} and mapped to {}", child, filteredChild);
          }
          children.add(filteredChild);
        } else {
          this.context.trace("Ignoring file {} according to filter", child);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to find children of directory " + dir, e);
    }
    return children;
  }

  @Override
  public boolean isEmptyDir(Path dir) {

    return listChildren(dir, f -> true).isEmpty();
  }

  private long getFileSize(Path file) {

    try {
      return Files.size(file);
    } catch (IOException e) {
      this.context.warning(e.getMessage(), e);
      return 0;
    }
  }

  private long getFileSizeRecursive(Path path) {

    long size = 0;
    if (Files.isDirectory(path)) {
      try (Stream<Path> childStream = Files.list(path)) {
        Iterator<Path> iterator = childStream.iterator();
        while (iterator.hasNext()) {
          Path child = iterator.next();
          size += getFileSizeRecursive(child);
        }
      } catch (IOException e) {
        throw new RuntimeException("Failed to iterate children of folder " + path, e);
      }
    } else {
      size += getFileSize(path);
    }
    return size;
  }

  @Override
  public Path findExistingFile(String fileName, List<Path> searchDirs) {

    for (Path dir : searchDirs) {
      Path filePath = dir.resolve(fileName);
      try {
        if (Files.exists(filePath)) {
          return filePath;
        }
      } catch (Exception e) {
        throw new IllegalStateException("Unexpected error while checking existence of file " + filePath + " .", e);
      }
    }
    return null;
  }

  @Override
  public void makeExecutable(Path file, boolean confirm) {

    if (Files.exists(file)) {
      if (SystemInfoImpl.INSTANCE.isWindows()) {
        this.context.trace("Windows does not have executable flags hence omitting for file {}", file);
        return;
      }
      try {
        // Read the current file permissions
        Set<PosixFilePermission> existingPermissions = Files.getPosixFilePermissions(file);

        // Add execute permission for all users
        Set<PosixFilePermission> executablePermissions = new HashSet<>(existingPermissions);
        boolean update = false;
        update |= executablePermissions.add(PosixFilePermission.OWNER_EXECUTE);
        update |= executablePermissions.add(PosixFilePermission.GROUP_EXECUTE);
        update |= executablePermissions.add(PosixFilePermission.OTHERS_EXECUTE);

        if (update) {
          if (confirm) {
            boolean yesContinue = this.context.question(
                "We want to execute " + file.getFileName() + " but this command seems to lack executable permissions!\n"
                    + "Most probably the tool vendor did forgot to add x-flags in the binary release package.\n"
                    + "Before running the command, we suggest to set executable permissions to the file:\n"
                    + file + "\n"
                    + "For security reasons we ask for your confirmation so please check this request.\n"
                    + "Changing permissions from " + PosixFilePermissions.toString(existingPermissions) + " to " + PosixFilePermissions.toString(
                    executablePermissions) + ".\n"
                    + "Do you confirm to make the command executable before running it?");
            if (!yesContinue) {
              return;
            }
          }
          this.context.debug("Setting executable flags for file {}", file);
          // Set the new permissions
          Files.setPosixFilePermissions(file, executablePermissions);
        } else {
          this.context.trace("Executable flags already present so no need to set them for file {}", file);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      this.context.warning("Cannot set executable flag on file that does not exist: {}", file);
    }
  }

  @Override
  public void touch(Path file) {

    if (Files.exists(file)) {
      try {
        Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
      } catch (IOException e) {
        throw new IllegalStateException("Could not update modification-time of " + file, e);
      }
    } else {
      try {
        Files.createFile(file);
      } catch (IOException e) {
        throw new IllegalStateException("Could not create empty file " + file, e);
      }
    }
  }

  @Override
  public String readFileContent(Path file) {

    this.context.trace("Reading content of file from {}", file);
    if (!Files.exists((file))) {
      this.context.debug("File {} does not exist", file);
      return null;
    }
    try {
      String content = Files.readString(file);
      this.context.trace("Completed reading {} character(s) from file {}", content.length(), file);
      return content;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read file " + file, e);
    }
  }

  @Override
  public void writeFileContent(String content, Path file, boolean createParentDir) {

    if (createParentDir) {
      mkdirs(file.getParent());
    }
    if (content == null) {
      content = "";
    }
    this.context.trace("Writing content with {} character(s) to file {}", content.length(), file);
    if (Files.exists(file)) {
      this.context.info("Overriding content of file {}", file);
    }
    try {
      Files.writeString(file, content);
      this.context.trace("Wrote content to file {}", file);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write file " + file, e);
    }
  }

  @Override
  public List<String> readFileLines(Path file) {

    this.context.trace("Reading content of file from {}", file);
    if (!Files.exists(file)) {
      this.context.warning("File {} does not exist", file);
      return null;
    }
    try {
      List<String> content = Files.readAllLines(file);
      this.context.trace("Completed reading {} lines from file {}", content.size(), file);
      return content;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read file " + file, e);
    }
  }

  @Override
  public void writeFileLines(List<String> content, Path file, boolean createParentDir) {

    if (createParentDir) {
      mkdirs(file.getParent());
    }
    if (content == null) {
      content = List.of();
    }
    this.context.trace("Writing content with {} lines to file {}", content.size(), file);
    if (Files.exists(file)) {
      this.context.debug("Overriding content of file {}", file);
    }
    try {
      Files.write(file, content);
      this.context.trace("Wrote content to file {}", file);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write file " + file, e);
    }
  }

  @Override
  public void readProperties(Path file, Properties properties) {

    try (Reader reader = Files.newBufferedReader(file)) {
      properties.load(reader);
      this.context.debug("Successfully loaded {} properties from {}", properties.size(), file);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read properties file: " + file, e);
    }
  }

  @Override
  public void writeProperties(Properties properties, Path file, boolean createParentDir) {

    if (createParentDir) {
      mkdirs(file.getParent());
    }
    try (Writer writer = Files.newBufferedWriter(file)) {
      properties.store(writer, null); // do not get confused - Java still writes a date/time header that cannot be omitted
      this.context.debug("Successfully saved {} properties to {}", properties.size(), file);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to save properties file during tests.", e);
    }
  }

  @Override
  public void readIniFile(Path file, IniFile iniFile) {
    List<String> iniLines = readFileLines(file);
    IniSection currentIniSection = null;
    for (String line : iniLines) {
      if (line.isEmpty()) {
        continue;
      }
      if (line.startsWith("[")) {
        String sectionName = line.replace("[", "").replace("]", "");
        currentIniSection = iniFile.getOrCreateSection(sectionName);
      } else {
        String[] parts = line.split("=");
        String propertyName = parts[0].trim();
        String propertyValue = parts[1].trim();
        if (currentIniSection == null) {
          Log.warn("Invalid ini-file with property {} before section", propertyName);
        } else {
          currentIniSection.getProperties().put(propertyName, propertyValue);
        }
      }
    }
  }

  @Override
  public void writeIniFile(IniFile iniFile, Path file, boolean createParentDir) {
    String iniString = iniFile.toString();
    writeFileContent(iniString, file, createParentDir);
  }

  @Override
  public Duration getFileAge(Path path) {
    if (Files.exists(path)) {
      try {
        long currentTime = System.currentTimeMillis();
        long fileModifiedTime = Files.getLastModifiedTime(path).toMillis();
        return Duration.ofMillis(currentTime - fileModifiedTime);
      } catch (IOException e) {
        this.context.warning().log(e, "Could not get modification-time of {}.", path);
      }
    } else {
      this.context.debug("Path {} is missing - skipping modification-time and file age check.", path);
    }
    return null;
  }

  @Override
  public boolean isFileAgeRecent(Path path, Duration cacheDuration) {

    Duration age = getFileAge(path);
    if (age == null) {
      return false;
    }
    context.debug("The path {} was last updated {} ago and caching duration is {}.", path, age, cacheDuration);
    return (age.toMillis() <= cacheDuration.toMillis());
  }
}
