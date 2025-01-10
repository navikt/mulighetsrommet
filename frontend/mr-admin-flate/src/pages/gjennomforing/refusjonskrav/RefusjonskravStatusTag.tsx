import { RefusjonskravStatus } from "@mr/api-client";
import { Tag } from "@navikt/ds-react";
import { ReactNode } from "react";

export function RefusjonskravStatusTag({ status }: { status: RefusjonskravStatus }): ReactNode {
  switch (status) {
    case RefusjonskravStatus.GODKJENT_AV_ARRANGOR:
      return <Tag variant="neutral">Godkjent</Tag>;
    case RefusjonskravStatus.KLAR_FOR_GODKJENNING:
      return <Tag variant="alt1">Klar for innsending</Tag>;
    case RefusjonskravStatus.NARMER_SEG_FRIST:
      return <Tag variant="warning">Nærmer seg frist</Tag>;
  }
}
