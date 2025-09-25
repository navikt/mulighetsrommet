import {
  FileObject,
  FileRejected,
  FileRejectionReason,
  FileUpload,
  Heading,
  VStack,
} from "@navikt/ds-react";
import { RefObject, SetStateAction, useEffect, useRef, useState } from "react";
import { FileStorage, useFileStorage } from "~/hooks/useFileStorage";

interface Props {
  maxFiles: number;
  maxSizeMB: number;
  maxSizeBytes: number;
  id?: string;
  error?: string;
  fileStorage?: boolean;
}

export const addFilesTo = async (
  fileInputRef: RefObject<HTMLInputElement | null>,
  setFiles: (value: SetStateAction<FileObject[]>) => void,
  storage: FileStorage,
): Promise<void> => {
  storage
    .getAll()
    .then((files) => {
      if (!fileInputRef.current) {
        return storage.deleteDatabase();
      }

      const fileObjects = files.map<FileObject>((file) => ({
        file: file.data as File,
        error: false,
      }));
      setFiles(fileObjects);

      const dataTransfer = new DataTransfer();
      fileObjects.forEach((file) => {
        dataTransfer.items.add(file.file);
      });
      fileInputRef.current.files = dataTransfer.files;

      return;
    })
    .catch((err) =>
      // eslint-disable-next-line no-console
      console.log(err),
    );
};

export function FileUploader({
  maxFiles,
  maxSizeMB,
  maxSizeBytes,
  id,
  error,
  fileStorage = false,
}: Props) {
  const storage = useFileStorage();
  const [files, setFiles] = useState<FileObject[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    // Ved last
    if (fileStorage && !files.length) {
      addFilesTo(fileInputRef, setFiles, storage);
    }
  });

  function removeFile(fileToRemove: FileObject) {
    const remainingFiles = files.filter((file) => file !== fileToRemove);
    setFiles(remainingFiles);
    if (fileStorage) {
      storage.store(
        remainingFiles.filter((file) => file.error === false).map((file) => file.file),
        { clearStore: true },
      );
    }
  }

  const acceptedFiles = files.filter((file) => !file.error);
  const rejectedFiles = files.filter((f): f is FileRejected => f.error);

  const errors: Record<FileRejectionReason, string> = {
    fileType: "Filformatet støttes ikke",
    fileSize: `Filen er større enn ${maxSizeMB} MB`,
  };
  return (
    <>
      <VStack gap="6">
        <FileUpload.Dropzone
          label="Last opp vedlegg"
          description={`Du kan laste opp PDF-filer. Maks ${maxFiles} filer. Maks størrelse ${maxSizeMB} MB per fil.`}
          accept=".pdf"
          id={id}
          error={error}
          maxSizeInBytes={maxSizeBytes}
          fileLimit={{ max: maxFiles, current: acceptedFiles.length }}
          onSelect={(newFiles: FileObject[]) => {
            setFiles([...files, ...newFiles]);
            if (fileStorage) {
              storage.store(
                newFiles.filter((file) => file.error === false).map((file) => file.file),
                { clearStore: false },
              );
            }
            if (fileInputRef.current) {
              const dataTransfer = new DataTransfer();
              [...files, ...newFiles].forEach((file) => {
                dataTransfer.items.add(file.file);
              });
              fileInputRef.current.files = dataTransfer.files;
            }
          }}
        />

        {acceptedFiles.length > 0 && (
          <VStack gap="2">
            <Heading level="4" size="xsmall">
              {`Vedlegg (${acceptedFiles.length})`}
            </Heading>
            <VStack as="ul" gap="3">
              {acceptedFiles.map((file, index) => (
                <FileUpload.Item
                  as="li"
                  key={index}
                  file={file.file}
                  button={{
                    action: "delete",
                    onClick: () => removeFile(file),
                  }}
                />
              ))}
            </VStack>
          </VStack>
        )}
        {rejectedFiles.length > 0 && (
          <VStack gap="2">
            <Heading level="4" size="xsmall">
              Vedlegg med feil
            </Heading>
            <VStack as="ul" gap="3">
              {rejectedFiles.map((rejected, index) => (
                <FileUpload.Item
                  as="li"
                  key={index}
                  file={rejected.file}
                  error={errors[rejected.reasons[0] as FileRejectionReason]}
                  button={{
                    action: "delete",
                    onClick: () => removeFile(rejected),
                  }}
                />
              ))}
            </VStack>
          </VStack>
        )}
      </VStack>
      <input ref={fileInputRef} type="file" name="vedlegg" multiple style={{ display: "none" }} />
    </>
  );
}
