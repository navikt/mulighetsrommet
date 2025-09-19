import {
  LabeledDataElement as LabeledDataElementProps,
  LabeledDataElementType,
} from "@tiltaksadministrasjon/api-client";
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
          value={<div className="whitespace-pre-line">{valueOrFallback}</div>}
        />
      );
  }
}
