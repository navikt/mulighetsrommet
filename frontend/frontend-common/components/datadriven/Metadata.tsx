import { BodyLong, HGrid } from "@navikt/ds-react";
import classNames from "classnames";
import { ReactNode } from "react";

export interface MetadataProps {
  label: string | ReactNode;
  value: string | number | undefined | null | ReactNode;
  compact?: boolean;
}

export function Metadata({ label, value }: MetadataProps) {
  return (
    <dl className={`flex flex-col gap-2`}>
      <dt className="font-bold">{label}</dt>
      <dd className="mr-6 whitespace-pre-line">{value ?? "-"}</dd>
    </dl>
  );
}

export function Separator({ style, classname }: { style?: any; classname?: string }) {
  return (
    <hr
      style={style}
      className={classNames("bg-[var(--a-border-divider)] h-px border-0 w-full my-4", classname)}
    />
  );
}

export function MetadataHorisontal({ label, value, compact }: MetadataProps) {
  const gridLayout = compact ? "max-content 1fr" : "0.5fr 1fr";
  return (
    <HGrid as="dl" columns={gridLayout} gap="2" align="start">
      <dt className="font-bold w-max">{label}:</dt>
      <dd className="whitespace-nowrap w-fit">{value ?? "-"}</dd>
    </HGrid>
  );
}

export interface MetadataFritekstfeltProps {
  label: string;
  value: string | number | undefined | null | ReactNode;
}

export function MetadataFritekstfelt({ label, value }: MetadataFritekstfeltProps) {
  return (
    <Metadata
      label={label}
      value={<BodyLong className="whitespace-pre-line">{value ?? "-"}</BodyLong>}
    />
  );
}
