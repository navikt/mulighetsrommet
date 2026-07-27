import { FilterAccordion, FilterSkeleton } from "@mr/frontend-common";
import {
  InnsendingFilterAccordionAtom,
  InnsendingFilterType,
} from "../../pages/innsendinger/filter";
import { useAtom } from "jotai";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { Accordion } from "@navikt/ds-react";
import { ArrangorKobling } from "@tiltaksadministrasjon/api-client";
import { GjennomforingTiltakstypeFilter } from "@/components/filter/GjennomforingTiltakstypeFilter";
import { KostnadsstedFilter } from "@/components/filter/KostnadsstedFilter";

type Filters = "tiltakstype" | "navEnhet" | "sortering";

interface Props {
  filter: InnsendingFilterType;
  updateFilter: (values: Partial<InnsendingFilterType>) => void;
  skjulFilter?: Record<Filters, boolean>;
  lagredeFilterOversikt: React.ReactElement;
}

export function InnsendingFilter({
  filter,
  updateFilter,
  skjulFilter,
  lagredeFilterOversikt,
}: Props) {
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
      <FilterAccordion
        tittel="Kostnadssted"
        antallValgteFilter={filter.kostnadssteder.length}
        open={accordionsOpen.includes("navEnhet")}
        onClick={() => {
          setAccordionsOpen([...addOrRemove(accordionsOpen, "navEnhet")]);
        }}
      >
        <KostnadsstedFilter
          value={filter.kostnadssteder}
          onChange={(kostnadssteder) => {
            updateFilter({ kostnadssteder });
          }}
        />
      </FilterAccordion>
      {!skjulFilter?.tiltakstype && (
        <FilterAccordion
          tittel="Tiltakstype"
          antallValgteFilter={filter.tiltakstyper.length}
          open={accordionsOpen.includes("tiltakstype")}
          onClick={() => {
            setAccordionsOpen([...addOrRemove(accordionsOpen, "tiltakstype")]);
          }}
        >
          <GjennomforingTiltakstypeFilter
            value={filter.tiltakstyper}
            onChange={(tiltakstyper) => {
              updateFilter({ tiltakstyper });
            }}
          />
        </FilterAccordion>
      )}
    </Accordion>
  );
}
