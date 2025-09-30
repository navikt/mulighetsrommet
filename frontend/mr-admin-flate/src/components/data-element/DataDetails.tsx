import { DataDetails as DataDetailsType } from "@tiltaksadministrasjon/api-client";
import { LabeledDataElement } from "./LabeledDataElement";

export interface DataDetailsProps extends DataDetailsType {
  layout?: "stack" | "grid";
}

export function DataDetails({ layout, entries }: DataDetailsProps) {
  const className = resolveClassName(layout ?? "stack");
  return (
    <dl className={className}>
      {entries.map((entry) => (
        <LabeledDataElement key={entry.label} {...entry} />
      ))}
    </dl>
  );
}

function resolveClassName(layout: "stack" | "grid") {
  switch (layout) {
    case "stack":
      return "grid grid-cols-1 gap-4 my-4";
    case "grid":
      return "grid grid-cols-2 gap-12 my-4";
  }
}
