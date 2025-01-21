import { RefusjonskravStatus } from "@mr/api-client-v2";
import { Tag } from "@navikt/ds-react";
import { ReactNode } from "react";

export function RefusjonskravStatusTag({ status }: { status: RefusjonskravStatus }): ReactNode {
  const baseTagClasses = "w-[140px] text-center whitespace-nowrap";

  switch (status) {
    case RefusjonskravStatus.GODKJENT_AV_ARRANGOR:
      return (
        <Tag size="small" variant="neutral" className={baseTagClasses}>
          Godkjent
        </Tag>
      );
    case RefusjonskravStatus.KLAR_FOR_GODKJENNING:
      return (
        <Tag size="small" variant="alt1" className={baseTagClasses}>
          Klar for innsending
        </Tag>
      );
    case RefusjonskravStatus.NARMER_SEG_FRIST:
      return (
        <Tag size="small" variant="warning" className={baseTagClasses}>
          Nærmer seg frist
        </Tag>
      );
  }
}
