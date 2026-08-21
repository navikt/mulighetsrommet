import {
  FileObject,
  FileRejected,
  FileRejectionReason,
  FileUpload,
  Heading,
  VStack,
} from "@navikt/ds-react";

const MAX_FILES = 10;
const MAX_SIZE_MB = 10;
const MAX_SIZE_BYTES = MAX_SIZE_MB * 1024 * 1024;

const fileErrors: Record<FileRejectionReason, string> = {
  fileType: "Filformatet støttes ikke",
  fileSize: `Filen er større enn ${MAX_SIZE_MB} MB`,
};

interface VedleggUploadProps {
  files: FileObject[];
  onFilesChange: (files: FileObject[]) => void;
  description: string;
  error?: string;
}

export function VedleggUpload({ files, onFilesChange, description, error }: VedleggUploadProps) {
  const acceptedFiles = files.filter((file) => !file.error);
  const rejectedFiles = files.filter((f): f is FileRejected => f.error);

  const removeFile = (fileToRemove: FileObject) => {
    onFilesChange(files.filter((file) => file !== fileToRemove));
  };

  return (
    <VStack gap="space-24">
      <FileUpload.Dropzone
        id="vedlegg"
        label="Last opp vedlegg"
        description={description}
        accept=".pdf"
        error={error}
        maxSizeInBytes={MAX_SIZE_BYTES}
        fileLimit={{ max: MAX_FILES, current: acceptedFiles.length }}
        onSelect={(newFiles: FileObject[]) => onFilesChange([...files, ...newFiles])}
      />

      {acceptedFiles.length > 0 && (
        <VStack gap="space-8">
          <Heading level="4" size="xsmall">
            {`Vedlegg (${acceptedFiles.length})`}
          </Heading>
          <VStack as="ul" gap="space-8" align="start">
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
        <VStack gap="space-8">
          <Heading level="4" size="xsmall">
            Vedlegg med feil
          </Heading>
          <VStack as="ul" gap="space-8" align="start">
            {rejectedFiles.map((rejected, index) => (
              <FileUpload.Item
                as="li"
                key={index}
                file={rejected.file}
                error={fileErrors[rejected.reasons[0] as FileRejectionReason]}
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
  );
}
