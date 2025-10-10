import { FileObject, FileUpload, Heading, VStack } from "@navikt/ds-react";
import { RefObject } from "react";

interface VedleggUtlistingProps {
  files: FileObject[];
  fileInputRef: RefObject<HTMLInputElement | null>;
}

export function VedleggUtlisting({ files, fileInputRef }: VedleggUtlistingProps) {
  return (
    <VStack gap="4">
      <Heading level="3" size="medium">
        Vedlegg
      </Heading>
      <VStack gap="2">
        <Heading level="4" size="xsmall">
          {`Vedlegg (${files.length})`}
        </Heading>
        <VStack as="ul" gap="3">
          {files.map((file, index) => (
            <FileUpload.Item as="li" key={index} file={file.file} />
          ))}
          <input
            ref={fileInputRef}
            type="file"
            name="vedlegg"
            multiple
            style={{ display: "none" }}
          />
        </VStack>
      </VStack>
    </VStack>
  );
}
