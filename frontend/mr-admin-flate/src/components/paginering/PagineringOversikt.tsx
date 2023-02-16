import { Heading } from "@navikt/ds-react";
import { PAGE_SIZE } from "../../constants";

interface Props {
  page: number;
  antall: number;
  maksAntall?: number;
  type: string;
}

export function PagineringsOversikt({
  page,
  antall,
  maksAntall = 0,
  type,
}: Props) {
  if (antall === 0) return null;

  return (
    <Heading level="1" size="xsmall" data-testid="antall-tiltak">
      Viser {(page - 1) * PAGE_SIZE + 1}-{antall + (page - 1) * PAGE_SIZE} av{" "}
      {maksAntall} {type}
    </Heading>
  );
}
