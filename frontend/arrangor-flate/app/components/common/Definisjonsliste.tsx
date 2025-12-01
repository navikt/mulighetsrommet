import { Heading, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import { formaterNOK, formaterTall } from "@mr/frontend-common/utils/utils";
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
        format: null,
      }))}
    />
  );
}

export interface Definition {
  key: string;
  value: string | ReactNode;
  format?: DefinitionFormat | null;
}

export type DefinitionFormat = "NOK" | "NUMBER";

interface Props {
  title?: string;
  className?: string;
  definitions: Definition[];
}

export function Definisjonsliste({ title, definitions, className }: Props) {
  return (
    <VStack gap="3" className={className}>
      {title && (
        <Heading size="medium" level="3">
          {title}
        </Heading>
      )}
      <VStack gap="1">
        {definitions.map((definition, index) => (
          <MetadataHGrid key={index} label={definition.key} value={getFormattedValue(definition)} />
        ))}
      </VStack>
    </VStack>
  );
}

function getFormattedValue(definition: Definition) {
  return definition.format && typeof definition.value === "string"
    ? formatValue(definition.value, definition.format)
    : definition.value;
}

function formatValue(value: string, format: DefinitionFormat) {
  switch (format) {
    case "NOK":
      return formaterNOK(Number(value));
    case "NUMBER":
      return formaterTall(Number(value));
  }
}
