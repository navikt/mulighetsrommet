import { FileUpload, Heading, VStack } from "@navikt/ds-react";

interface VedleggSummaryProps {
  vedlegg: File[];
}

export function VedleggSummary({ vedlegg }: VedleggSummaryProps) {
  return (
    <>
      <Heading level="4" size="xsmall">
        {`Vedlegg (${vedlegg.length})`}
      </Heading>
      {vedlegg.length > 0 && (
        <VStack gap="space-8" marginBlock="space-4" align="start">
          <VStack as="ul" gap="space-8">
            {vedlegg.map((file, index) => (
              <FileUpload.Item as="li" key={index} file={file} />
            ))}
          </VStack>
        </VStack>
      )}
    </>
  );
}
