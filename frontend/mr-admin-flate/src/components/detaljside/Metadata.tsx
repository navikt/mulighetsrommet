import { Box, HGrid, HStack } from "@navikt/ds-react";
import classNames from "classnames";
import { ReactNode } from "react";

export interface MetadataProps {
  header: string | ReactNode;
  verdi: string | number | undefined | null | ReactNode;
}

export function Metadata({ header, verdi }: MetadataProps) {
  return (
    <div className={`flex flex-col gap-2`}>
      <dt className="font-bold">{header}</dt>
      <dd className="mr-6 whitespace-pre-line">{verdi ?? "-"}</dd>
    </div>
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

export function MetadataHorisontal({ header, verdi }: MetadataProps) {
  return (
    <HGrid columns="0.5fr 1fr" gap="2" align="center">
      <Box>
        <dt>{header}:</dt>
      </Box>
      <Box>
        <dd className="font-bold text-wrap whitespace-break-spaces">{verdi ?? "-"}</dd>
      </Box>
    </HGrid>
  );
}
