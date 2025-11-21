import { VStack } from "@navikt/ds-react";
import { type LabeledDataElement, LabeledDataElementType } from "./types";
import { MetadataFritekstfelt, MetadataHorisontal } from "./Metadata";
import { getDataElement } from "./DataElement";

export interface DataElementProps {
  entries: LabeledDataElement[];
}

export function DataDetails({ entries }: DataElementProps) {
  return (
    <VStack gap="4">
      {entries.map((entry) => (
        <LabeledDataElement key={entry.label} {...entry} />
      ))}
    </VStack>
  );
}

function LabeledDataElement(props: LabeledDataElement) {
  const value = props.value ? getDataElement(props.value) : null;
  const valueOrFallback = value || "-";
  switch (props.type) {
    case LabeledDataElementType.INLINE:
      return <MetadataHorisontal label={props.label} value={valueOrFallback} />;
    case LabeledDataElementType.MULTILINE:
      return <MetadataFritekstfelt label={props.label} value={valueOrFallback} />;
  }
}
