import { DataDetails as DataDetailsProps } from "@tiltaksadministrasjon/api-client";
import { VStack } from "@navikt/ds-react";
import { LabeledDataElement } from "./LabeledDataElement";

export function DataDetails({ entries }: DataDetailsProps) {
  return (
    <VStack gap="4">
      {entries.map((entry) => (
        <LabeledDataElement key={entry.label} {...entry} />
      ))}
    </VStack>
  );
}
