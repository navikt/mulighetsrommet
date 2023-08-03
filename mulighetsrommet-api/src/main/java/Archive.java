import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class Archive implements Closeable {
    private ZipOutputStream zipStream;

    public Archive(File zip) throws IOException {
        OutputStream stream = new FileOutputStream(zip);
        stream = new BufferedOutputStream(stream);
        zipStream = new ZipOutputStream(stream);
    }

    public void archive(String name, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zipStream.putNextEntry(entry);
        // Missing call to 'write'
        zipStream.closeEntry();
    }

    void writeZipEntry(ZipEntry entry, File destinationDir) throws FileNotFoundException {
        File file = new File(destinationDir, entry.getName());
        FileOutputStream fos = new FileOutputStream(file); // BAD
        // ... write entry to fos ...
    }

    public void close() throws IOException {
        zipStream.close();
    }
}
