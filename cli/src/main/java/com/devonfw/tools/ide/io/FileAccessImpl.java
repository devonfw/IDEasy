package com.devonfw.tools.ide.io;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.url.model.file.UrlChecksum;
import com.devonfw.tools.ide.util.DateTimeUtil;
import com.devonfw.tools.ide.util.HexUtil;

/**
 * Implementation of {@link FileAccess}.
 */
public class FileAccessImpl implements FileAccess {

  private final IdeContext context;

  /** The {@link HttpClient} for HTTP requests. */
  private final HttpClient client;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext} to use.
   */
  public FileAccessImpl(IdeContext context) {

    super();
    this.context = context;
    this.client = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();
  }

  @Override
  public void download(String url, Path target) {

    this.context.info("Trying to download {} from {}", target.getFileName(), url);
    mkdirs(target.getParent());
    try {
      if (url.startsWith("http")) {

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<InputStream> response = this.client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() == 200) {
          downloadFileWithProgressBar(url, target, response);
        }
      } else if (url.startsWith("ftp") || url.startsWith("sftp")) {
        throw new IllegalArgumentException("Unsupported download URL: " + url);
      } else {
        Path source = Paths.get(url);
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
  private void downloadFileWithProgressBar(String url, Path target, HttpResponse<InputStream> response)
      throws IOException {

    long contentLength = response.headers().firstValueAsLong("content-length").orElse(0);
    if (contentLength == 0) {
      this.context.warning(
          "Content-Length was not provided by download source : {} using fallback for the progress bar which will be inaccurate.",
          url);
      contentLength = 10000000;
    }

    byte[] data = new byte[1024];
    boolean fileComplete = false;
    int count;

    try (InputStream body = response.body();
        FileOutputStream fileOutput = new FileOutputStream(target.toFile());
        BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOutput, data.length);
        IdeProgressBar pb = this.context.prepareProgressBar("Downloading", contentLength)) {
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
   * Copies a file while displaying a progress bar
   *
   * @param source Path of file to copy
   * @param target Path of target directory
   */
  private void copyFileWithProgressBar(Path source, Path target) throws IOException {

    try (InputStream in = new FileInputStream(source.toFile());
        OutputStream out = new FileOutputStream(target.toFile())) {

      long size = source.toFile().length();
      byte[] buf = new byte[1024];
      int readBytes;

      try (IdeProgressBar pb = this.context.prepareProgressBar("Copying", size)) {
        while ((readBytes = in.read(buf)) > 0) {
          out.write(buf, 0, readBytes);
          pb.stepByOne();
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
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
  public String checksum(Path file) {

    try {
      MessageDigest md = MessageDigest.getInstance(UrlChecksum.HASH_ALGORITHM);
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
      String checksum = HexUtil.toHexString(digestBytes);
      return checksum;
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("No such hash algorithm " + UrlChecksum.HASH_ALGORITHM, e);
    }
  }

  @Override
  public void backup(Path fileOrFolder) {

    if (Files.isSymbolicLink(fileOrFolder)) {
      delete(fileOrFolder);
    }
    Path backupPath = this.context.getIdeHome().resolve(IdeContext.FOLDER_UPDATES).resolve(IdeContext.FOLDER_BACKUPS);
    LocalDateTime now = LocalDateTime.now();
    String date = DateTimeUtil.formatDate(now);
    String time = DateTimeUtil.formatTime(now);
    Path backupDatePath = backupPath.resolve(date);
    mkdirs(backupDatePath);
    Path target = backupDatePath.resolve(fileOrFolder.getFileName().toString() + "_" + time);
    this.context.info("Creating backup by moving {} to {}", fileOrFolder, target);
    move(fileOrFolder, target);
  }

  @Override
  public void move(Path source, Path targetDir) {

    this.context.trace("Moving {} to {}", source, targetDir);
    try {
      Files.move(source, targetDir);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to move " + source + " to " + targetDir, e);
    }
  }

  @Override
  public void copy(Path source, Path target, FileCopyMode mode) {

    boolean fileOnly = mode.isFileOnly();
    if (fileOnly) {
      this.context.debug("Copying file {} to {}", source, target);
    } else {
      this.context.debug("Copying {} recursively to {}", source, target);
    }
    if (fileOnly && Files.isDirectory(source)) {
      throw new IllegalStateException("Expected file but found a directory to copy at " + source);
    }
    if (mode.isFailIfExists()) {
      if (Files.exists(target)) {
        throw new IllegalStateException("Failed to copy " + source + " to already existing target " + target);
      }
    } else if (mode == FileCopyMode.COPY_TREE_OVERRIDE_TREE) {
      delete(target);
    }
    try {
      copyRecursive(source, target, mode);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to copy " + source + " to " + target, e);
    }
  }

  private void copyRecursive(Path source, Path target, FileCopyMode mode) throws IOException {

    if (Files.isDirectory(source)) {
      mkdirs(target);
      try (Stream<Path> childStream = Files.list(source)) {
        Iterator<Path> iterator = childStream.iterator();
        while (iterator.hasNext()) {
          Path child = iterator.next();
          copyRecursive(child, target.resolve(child.getFileName()), mode);
        }
      }
    } else if (Files.exists(source)) {
      if (mode == FileCopyMode.COPY_TREE_OVERRIDE_FILES) {
        delete(target);
      }
      this.context.trace("Copying {} to {}", source, target);
      Files.copy(source, target);
    } else {
      throw new IOException("Path " + source + " does not exist.");
    }
  }

  @Override
  public void symlink(Path source, Path targetLink) {

    this.context.trace("Creating symbolic link {} pointing to {}", targetLink, source);
    try {
      if (Files.exists(targetLink) && Files.isSymbolicLink(targetLink)) {
        this.context.debug("Deleting symbolic link to be re-created at {}", targetLink);
        Files.delete(targetLink);
      }
      Files.createSymbolicLink(targetLink, source);
    } catch (FileSystemException e) {
      if (this.context.getSystemInfo().isWindows()) {
        this.context.info(
            "Due to lack of permissions, Microsofts mklink with junction has to be used to create a Symlink. See https://github.com/devonfw/IDEasy/blob/main/documentation/symlinks.asciidoc for further details. Error was: "
                + e.getMessage());
        this.context.newProcess().executable("cmd")
            .addArgs("/c", "mklink", "/d", "/j", targetLink.toString(), source.toString()).run();
      } else {
        throw new RuntimeException(e);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create a symbolic link " + targetLink + " pointing to " + source, e);
    }
  }

  @Override
  public Path toRealPath(Path path) {

    try {
      Path realPath = path.toRealPath();
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
  public void unzip(Path file, Path targetDir) {

    unpack(file, targetDir, in -> new ZipArchiveInputStream(in));
  }

  @Override
  public void untar(Path file, Path targetDir, TarCompression compression) {

    unpack(file, targetDir, in -> new TarArchiveInputStream(compression.unpack(in)));
  }

  private void unpack(Path file, Path targetDir, Function<InputStream, ArchiveInputStream> unpacker) {

    this.context.trace("Unpacking archive {} to {}", file, targetDir);
    try (InputStream is = Files.newInputStream(file); ArchiveInputStream ais = unpacker.apply(is)) {
      ArchiveEntry entry = ais.getNextEntry();
      while (entry != null) {
        Path entryName = Paths.get(entry.getName());
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
        entry = ais.getNextEntry();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to extract " + file + " to " + targetDir, e);
    }
  }

  @Override
  public void delete(Path path) {

    if (!Files.exists(path)) {
      this.context.trace("Deleting {} skipped as the path does not exist.", path);
      return;
    }
    this.context.debug("Deleting {} ...", path);
    try {
      if (Files.isSymbolicLink(path)) {
        Files.delete(path);
      }
      deleteRecursive(path);
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

}
