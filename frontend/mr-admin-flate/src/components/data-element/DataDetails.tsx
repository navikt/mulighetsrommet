import { type LabeledDataElement, LabeledDataElementType } from "@mr/api-client-v2";
import { BodyLong, VStack } from "@navikt/ds-react";
import { Metadata, MetadataHorisontal } from "../detaljside/Metadata";
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

export function LabeledDataElement(props: LabeledDataElement) {
  const value = props.value ? getDataElement(props.value) : null;
  const valueOrFallback = value || "-";
  switch (props.type) {
    case LabeledDataElementType.INLINE:
      return <MetadataHorisontal header={props.label} value={valueOrFallback} />;
    case LabeledDataElementType.MULTILINE:
      return (
        <Metadata
          header={props.label}
          value={<BodyLong className="whitespace-pre-line">{valueOrFallback}</BodyLong>}
        />
      );
  }
}
