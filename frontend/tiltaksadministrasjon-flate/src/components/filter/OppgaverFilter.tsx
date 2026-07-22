import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterAccordion } from "@mr/frontend-common";
import { Accordion } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  oppgaverFilterAccordionAtom,
  OppgaverFilterType,
} from "@/pages/oppgaveoversikt/oppgaver/filter";
import { OppgaveType } from "@tiltaksadministrasjon/api-client";
import { OppgaveTypeFilter } from "@/components/filter/OppgaveTypeFilter";
import { GjennomforingTiltakstypeFilter } from "./GjennomforingTiltakstypeFilter";
import { ArrangorerFilter } from "./ArrangorerFilter";
import { KontorstrukturOgKostnadsstedFilter } from "./KontorstrukturOgKostnadsstedFilter";

interface Props {
  filter: OppgaverFilterType;
  updateFilter: (values: Partial<OppgaverFilterType>) => void;
  lagredeFilterOversikt: React.ReactElement;
}

export function OppgaverFilter({ filter, updateFilter, lagredeFilterOversikt }: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(oppgaverFilterAccordionAtom);

  return (
    <div className="bg-ax-bg-default self-start w-80">
      <Accordion>
        <FilterAccordion
          tittel="Lagrede filter"
          onClick={() => {
            setAccordionsOpen([...addOrRemove(accordionsOpen, "lagrede-filter")]);
          }}
          open={accordionsOpen.includes("lagrede-filter")}
        >
          {lagredeFilterOversikt}
        </FilterAccordion>
        <FilterAccordion
          tittel="Oppgave"
          antallValgteFilter={filter.type.length}
          open={accordionsOpen.includes("type")}
          onClick={() => {
            setAccordionsOpen([...addOrRemove(accordionsOpen, "type")]);
          }}
        >
          <OppgaveTypeFilter
            value={filter.type}
            onChange={(value) => updateFilter({ type: value as OppgaveType[] })}
          />
        </FilterAccordion>

        <FilterAccordion
          tittel="Nav-enhet"
          antallValgteFilter={filter.navEnheter.length}
          open={accordionsOpen.includes("navEnhet")}
          onClick={() => {
            setAccordionsOpen([...addOrRemove(accordionsOpen, "navEnhet")]);
          }}
        >
          <KontorstrukturOgKostnadsstedFilter
            value={filter.navEnheter}
            onChange={(navEnheter) => {
              updateFilter({ navEnheter });
            }}
          />
        </FilterAccordion>
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
            onChange={(tiltakstyper) => updateFilter({ tiltakstyper })}
          />
        </FilterAccordion>

        <FilterAccordion
          tittel="Arrangør"
          antallValgteFilter={filter.arrangorer.length}
          open={accordionsOpen.includes("arrangor")}
          onClick={() => {
            setAccordionsOpen([...addOrRemove(accordionsOpen, "arrangor")]);
          }}
        >
          <ArrangorerFilter
            filter={filter.arrangorer}
            updateFilter={(arrangorer) => updateFilter({ arrangorer })}
          />
        </FilterAccordion>
      </Accordion>
    </div>
  );
}
