import {
  FileObject,
  FileRejected,
  FileRejectionReason,
  FileUpload,
  Heading,
  VStack,
} from "@navikt/ds-react";
import { useRef, useState } from "react";

interface Props {
  maxFiles: number;
  maxSizeMB: number;
  maxSizeBytes: number;
  id?: string;
}

export function FileUploader({ maxFiles, maxSizeMB, maxSizeBytes, id }: Props) {
  const [files, setFiles] = useState<FileObject[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  function removeFile(fileToRemove: FileObject) {
    setFiles(files.filter((file) => file !== fileToRemove));
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
          maxSizeInBytes={maxSizeBytes}
          fileLimit={{ max: maxFiles, current: acceptedFiles.length }}
          onSelect={(newFiles: FileObject[]) => {
            setFiles([...files, ...newFiles]);
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
            <Heading level="3" size="xsmall">
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
            <Heading level="3" size="xsmall">
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
