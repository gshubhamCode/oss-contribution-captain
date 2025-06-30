package org.fa.oss.contribution.helper.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.config.CacheProperties;

@Slf4j
public abstract class JsonFileCache<T> implements CacheService<T> {

  private static final long DEFAULT_MAX_AGE_MILLIS = 120 * 60 * 1000; // 120 minutes
  private final ObjectMapper mapper;
  private final File file;
  private final TypeReference<T> typeRef;

  private long maxAgeMillis;

  protected boolean firstLoadDone = false;

  public JsonFileCache(
      ObjectMapper mapper,
      CacheProperties properties,
      String fileName,
      TypeReference<T> typeRef,
      long maxAgeMillis) {
    String baseDir = properties.getBaseDir();
    this.file = new File(baseDir, fileName);
    this.typeRef = typeRef;
    this.mapper = mapper;
    this.maxAgeMillis = maxAgeMillis;

    this.file.getParentFile().mkdirs();
  }

  public JsonFileCache(
      ObjectMapper mapper, CacheProperties properties, String fileName, TypeReference<T> typeRef) {
    this(mapper, properties, fileName, typeRef, DEFAULT_MAX_AGE_MILLIS);
  }

  public long getMaxAgeMillis() {
    return maxAgeMillis;
  }

  public void setMaxAgeMillis(long maxAgeMillis) {
    this.maxAgeMillis = maxAgeMillis;
  }

  public File getFile() {
    return file;
  }

  @Override
  public T load() {
    if (file.exists()) {
      try {
        firstLoadDone = true;
        return mapper.readValue(file, typeRef);
      } catch (IOException e) {
        throw new RuntimeException("Failed to load cache from " + file.getAbsolutePath(), e);
      }
    }
    return null;
  }

  public synchronized void save(T data) {
    file.getParentFile().mkdirs();

    File tmpFile = new File(file.getAbsolutePath() + ".tmp");

    FileOutputStream fos = null;
    FileLock lock = null;

    try {
      fos = new FileOutputStream(tmpFile);
      FileChannel channel = fos.getChannel();

      lock = channel.lock();

      mapper.writerWithDefaultPrettyPrinter().writeValue(fos, data);
      fos.flush();

    } catch (IOException e) {
      throw new RuntimeException("Failed to save cache atomically to " + file.getAbsolutePath(), e);
    } finally {
      try {
        if (lock != null && lock.isValid()) lock.release();
      } catch (IOException e) {
        log.warn("Failed to release file lock: " + tmpFile.getAbsolutePath(), e);
      }
    }

    if (!tmpFile.renameTo(file)) {
      throw new RuntimeException(
          "Failed to rename temp file to cache file: " + file.getAbsolutePath());
    }
  }

  @Override
  public boolean isCacheValid() {
    if (!file.exists()) return false;

    if (!firstLoadDone) {
      return true;
    }

    long lastModified = file.lastModified();
    long age = System.currentTimeMillis() - lastModified;

    return age <= maxAgeMillis;
  }

  @Override
  public long getCacheAgeMillis() {
    if (!file.exists()) return -1;
    return System.currentTimeMillis() - file.lastModified();
  }
}
