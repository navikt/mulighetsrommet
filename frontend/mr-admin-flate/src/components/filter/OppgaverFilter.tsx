import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterAccordionHeader } from "@mr/frontend-common";
import { Accordion } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  oppgaverFilterAccordionAtom,
  OppgaverFilterType,
} from "@/pages/oppgaveoversikt/oppgaver/filter";
import { TiltakskodeFilter } from "@/components/filter/TiltakskodeFilter";
import { OppgaveType } from "@tiltaksadministrasjon/api-client";
import { OppgaveTypeFilter } from "@/components/filter/OppgaveTypeFilter";
import { NavRegionFilter } from "@/components/filter/NavRegionFilter";

interface Props {
  filter: OppgaverFilterType;
  updateFilter: (values: Partial<OppgaverFilterType>) => void;
}

export function OppgaverFilter({ filter, updateFilter }: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(oppgaverFilterAccordionAtom);

  return (
    <div className="bg-ax-bg-default self-start w-80">
      <Accordion>
        <Accordion.Item open={accordionsOpen.includes("type")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "type")]);
            }}
          >
            <FilterAccordionHeader tittel="Oppgave" antallValgteFilter={filter.type.length} />
          </Accordion.Header>
          <Accordion.Content>
            <OppgaveTypeFilter
              value={filter.type}
              onChange={(value) => updateFilter({ type: value as OppgaveType[] })}
            />
          </Accordion.Content>
        </Accordion.Item>
        <Accordion.Item open={accordionsOpen.includes("regioner")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "regioner")]);
            }}
          >
            <FilterAccordionHeader tittel="Region" antallValgteFilter={filter.regioner.length} />
          </Accordion.Header>
          <Accordion.Content>
            <NavRegionFilter
              value={filter.regioner}
              onChange={(regioner) => updateFilter({ regioner })}
            />
          </Accordion.Content>
        </Accordion.Item>
        <Accordion.Item open={accordionsOpen.includes("tiltakstype")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "tiltakstype")]);
            }}
          >
            <FilterAccordionHeader
              tittel="Tiltakstype"
              antallValgteFilter={filter.tiltakstyper.length}
            />
          </Accordion.Header>
          <Accordion.Content>
            <TiltakskodeFilter
              value={filter.tiltakstyper}
              onChange={(tiltakstyper) => {
                updateFilter({ tiltakstyper });
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
    </div>
  );
}
