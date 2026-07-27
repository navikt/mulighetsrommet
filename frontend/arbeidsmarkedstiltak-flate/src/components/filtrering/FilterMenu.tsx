import { filterAccordionAtom } from "@/core/atoms";
import { useArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { ApentForPamelding } from "@arbeidsmarkedstiltak/api-client";
import { FilterAccordion } from "@mr/frontend-common";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { Accordion, Checkbox, CheckboxGroup, VStack } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { InnsatsgruppeFilter } from "./InnsatsgruppeFilter";
import Sokefelt from "./Sokefelt";
import { TiltakstypeFilter } from "./TiltakstypeFilter";
import { NavEnhetFilter } from "@/components/filtrering/NavEnhetFilter";

interface Props {
  lagredeFilterOversikt?: React.ReactElement;
}

export function FilterMenu({ lagredeFilterOversikt }: Props) {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();
  const [accordionsOpen, setAccordionsOpen] = useAtom(filterAccordionAtom);

  return (
    <VStack gap="space-16" data-testid="filtertabs">
      <Sokefelt
        sokefilter={filter.search}
        setSokefilter={(search: string) => setFilter({ ...filter, search })}
      />
      <Accordion size="small">
        {lagredeFilterOversikt && (
          <FilterAccordion
            tittel="Lagrede filter"
            open={accordionsOpen.includes("lagrede-filter")}
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "lagrede-filter")]);
            }}
          >
            {lagredeFilterOversikt}
          </FilterAccordion>
        )}
        <FilterAccordion
          tittel="Åpent for påmelding"
          open={accordionsOpen.includes("apen-for-pamelding")}
          onClick={() => {
            setAccordionsOpen([...addOrRemove(accordionsOpen, "apen-for-pamelding")]);
          }}
        >
          <CheckboxGroup
            legend="Åpent for påmelding"
            hideLegend
            value={filter.apentForPamelding}
            size="small"
            onChange={(apentForPamelding) => {
              setFilter({ ...filter, apentForPamelding: apentForPamelding });
            }}
          >
            <Checkbox value={ApentForPamelding.APENT}>Åpent</Checkbox>
            <Checkbox value={ApentForPamelding.STENGT}>Stengt</Checkbox>
          </CheckboxGroup>
        </FilterAccordion>
        <InnsatsgruppeFilter />
        <FilterAccordion
          tittel="Tiltakstype"
          antallValgteFilter={filter.tiltakstyper.length}
          open={accordionsOpen.includes("tiltakstyper")}
          onClick={() => {
            setAccordionsOpen([...addOrRemove(accordionsOpen, "tiltakstyper")]);
          }}
        >
          <TiltakstypeFilter
            value={filter.tiltakstyper}
            onChange={(tiltakstyper) => setFilter({ ...filter, tiltakstyper })}
          />
        </FilterAccordion>
        <FilterAccordion
          tittel="Nav-enhet"
          antallValgteFilter={filter.navEnheter.length}
          open={accordionsOpen.includes("brukers-enhet")}
          onClick={() => {
            setAccordionsOpen([...addOrRemove(accordionsOpen, "brukers-enhet")]);
          }}
        >
          <NavEnhetFilter
            value={filter.navEnheter}
            onChange={(navEnheter) => setFilter({ ...filter, navEnheter })}
          />
        </FilterAccordion>
      </Accordion>
    </VStack>
  );
}
