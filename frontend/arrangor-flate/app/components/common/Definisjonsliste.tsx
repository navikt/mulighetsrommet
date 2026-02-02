import { Heading, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import { LabeledDataElement } from "@api-client";
import { getDataElement } from "@mr/frontend-common";
import { MetadataHGrid } from "@mr/frontend-common/components/datadriven/Metadata";

interface LabeledDataElementListProps {
  title?: string;
  className?: string;
  entries: LabeledDataElement[];
}

export function LabeledDataElementList({ title, entries, className }: LabeledDataElementListProps) {
  return (
    <Definisjonsliste
      className={className}
      title={title}
      definitions={entries.map((entry) => ({
        key: entry.label,
        value: entry.value ? getDataElement(entry.value) : null,
      }))}
    />
  );
}

export interface Definition {
  key: string;
  value: string | ReactNode;
}

interface Props {
  title?: string;
  className?: string;
  definitions: Definition[];
}

export function Definisjonsliste({ title, definitions, className }: Props) {
  return (
    <VStack gap="space-12" className={className}>
      {title && (
        <Heading size="medium" level="3">
          {title}
        </Heading>
      )}
      <VStack gap="space-4">
        {definitions.map((definition, index) => (
          <MetadataHGrid key={index} label={definition.key} value={definition.value} />
        ))}
      </VStack>
    </VStack>
  );
}
