import {
  LabeledDataElement as LabeledDataElementProps,
  LabeledDataElementType,
} from "@tiltaksadministrasjon/api-client";
import { BodyLong } from "@navikt/ds-react";
import { Metadata, MetadataHorisontal } from "../detaljside/Metadata";
import { getDataElement } from "./DataElement";

export function LabeledDataElement(props: LabeledDataElementProps) {
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
