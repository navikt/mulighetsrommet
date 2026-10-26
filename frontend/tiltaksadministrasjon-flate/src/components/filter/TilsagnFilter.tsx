import { FilterAccordion, FilterSkeleton } from "@mr/frontend-common";
import { InnsendingFilterAccordionAtom } from "../../pages/innsendinger/filter";
import { useAtom } from "jotai";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { Accordion } from "@navikt/ds-react";
import { ArrangorKobling } from "@tiltaksadministrasjon/api-client";
import { TilsagnFilterType } from "@/pages/gjennomforing/tilsagn/filter";

type Filters = "tiltakstype" | "navEnhet" | "sortering";

interface Props {
  filter: TilsagnFilterType;
  updateFilter: (values: Partial<TilsagnFilterType>) => void;
  skjulFilter?: Record<Filters, boolean>;
  lagredeFilterOversikt: React.ReactElement;
}

export function TilsagnFilter({ lagredeFilterOversikt }: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(InnsendingFilterAccordionAtom);

  const { data: arrangorer } = useArrangorer(ArrangorKobling.TILTAKSGJENNOMFORING, {
    pageSize: 10000,
  });

  if (!arrangorer) {
    return <FilterSkeleton />;
  }

  return (
    <Accordion size="small">
      <FilterAccordion
        tittel="Lagrede filter"
        open={accordionsOpen.includes("lagrede-filter")}
        onClick={() => {
          setAccordionsOpen([...addOrRemove(accordionsOpen, "lagrede-filter")]);
        }}
      >
        {lagredeFilterOversikt}
      </FilterAccordion>
    </Accordion>
  );
}
