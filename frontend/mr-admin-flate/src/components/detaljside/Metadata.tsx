import { BodyLong, HGrid } from "@navikt/ds-react";
import { ReactNode } from "react";

export interface MetadataProps {
  header: string | ReactNode;
  value: string | number | undefined | null | ReactNode;
  compact?: boolean;
}

export function Metadata({ header, value }: MetadataProps) {
  return (
    <div className={`flex flex-col gap-2`}>
      <dt className="font-bold">{header}</dt>
      <dd className="mr-6 whitespace-pre-line">{value ?? "-"}</dd>
    </div>
  );
}

export function Separator() {
  return <hr className="bg-(--a-border-divider) h-px border-0 w-full my-4" />;
}

export function MetadataHorisontal({ header, value, compact }: MetadataProps) {
  const gridLayout = compact ? "max-content 1fr" : "0.5fr 1fr";
  return (
    <HGrid columns={gridLayout} gap="2" align="start">
      <dt className="font-bold w-max">{header}:</dt>
      <dd className="whitespace-nowrap w-fit">{value ?? "-"}</dd>
    </HGrid>
  );
}

export interface MetadataFritekstfeltProps {
  header: string;
  value: string | undefined | null;
}

export function MetadataFritekstfelt({ header, value }: MetadataFritekstfeltProps) {
  return (
    <Metadata
      header={header}
      value={<BodyLong className="whitespace-pre-line">{value ?? "-"}</BodyLong>}
    />
  );
}
