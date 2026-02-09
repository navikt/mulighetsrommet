import { BodyLong, HGrid, HStack, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";

export interface MetadataProps {
  label: string | ReactNode;
  value: string | number | undefined | null | ReactNode;
  compact?: boolean;
}

export function MetadataVStack({ label, value }: MetadataProps) {
  return (
    <VStack as="dl" gap="space-8">
      <dt className="font-bold">{label}</dt>
      <dd className="mr-6 whitespace-pre-line">{value ?? "-"}</dd>
    </VStack>
  );
}

export function Separator() {
  return (
    <hr
      style={{
        color: "var(--ax-border-neutral-subtle)",
        marginBlock: "1rem",
      }}
    />
  );
}

export function MetadataHStack({ label, value }: MetadataProps) {
  return (
    <HStack as="dl" justify="space-between" gap="space-8" align="start">
      <dt className="font-bold w-max">{label}:</dt>
      <dd className="whitespace-nowrap w-fit">{value ?? "-"}</dd>
    </HStack>
  );
}

export function MetadataHGrid({ label, value, compact }: MetadataProps) {
  const gridLayout = compact ? "max-content 1fr" : "200px 1fr";
  return (
    <HGrid as="dl" columns={gridLayout} gap="space-8" align="start">
      <dt className="font-bold">{label}:</dt>
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
    <dl className={`flex flex-col gap-2`}>
      <dt className="font-bold">{label}:</dt>
      <dd>
        <BodyLong className="whitespace-pre-wrap">{value ?? "-"}</BodyLong>
      </dd>
    </dl>
  );
}
