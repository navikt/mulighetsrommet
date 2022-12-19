import { Heading } from "@navikt/ds-react";
import { PAGE_SIZE } from "../../constants";

interface Props {
  page: number;
  antall: number;
  maksAntall?: number;
}

export function PagineringsOversikt({ page, antall, maksAntall = 0 }: Props) {
  return (
    <Heading level="1" size="xsmall" data-testid="antall-tiltak">
      Viser {(page - 1) * PAGE_SIZE + 1}-{antall + (page - 1) * PAGE_SIZE} av{" "}
      {maksAntall} tiltak
    </Heading>
  );
}
