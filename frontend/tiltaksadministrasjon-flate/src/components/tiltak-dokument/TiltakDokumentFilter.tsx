import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterAccordion } from "@mr/frontend-common";
import { Accordion } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  tiltakDokumentFilterAccordionAtom,
  TiltakDokumentFilterType,
} from "@/pages/tiltak-dokument/filter";
import { GjennomforingTiltakstypeFilter } from "@/components/filter/GjennomforingTiltakstypeFilter";
import { KontorstrukturFilter } from "@/components/filter/KontorstrukturFilter";
import { Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { CheckboxList } from "@/components/filter/CheckboxList";

interface Props {
  filter: TiltakDokumentFilterType;
  updateFilter: (values: Partial<TiltakDokumentFilterType>) => void;
}

export function TiltakDokumentFilter({ filter, updateFilter }: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(tiltakDokumentFilterAccordionAtom);

  const toggleAccordion = (key: string) => {
    setAccordionsOpen([...addOrRemove(accordionsOpen, key)]);
  };

  return (
    <Accordion size="small">
      <FilterAccordion
        tittel="Nav-enhet"
        antallValgteFilter={filter.navEnheter.length}
        open={accordionsOpen.includes("navEnhet")}
        onClick={() => toggleAccordion("navEnhet")}
      >
        <KontorstrukturFilter
          value={filter.navEnheter}
          onChange={(navEnheter) => updateFilter({ navEnheter, page: 1 })}
        />
      </FilterAccordion>

      <FilterAccordion
        tittel="Tiltakstype"
        antallValgteFilter={filter.tiltakstyper.length}
        open={accordionsOpen.includes("tiltakstype")}
        onClick={() => toggleAccordion("tiltakstype")}
      >
        <GjennomforingTiltakstypeFilter
          value={filter.tiltakstyper as Tiltakskode[]}
          onChange={(tiltakstyper) => updateFilter({ tiltakstyper, page: 1 })}
        />
      </FilterAccordion>

      <FilterAccordion
        tittel="Publisert"
        antallValgteFilter={filter.publisert.length}
        open={accordionsOpen.includes("publiserteStatuser")}
        onClick={() => toggleAccordion("publiserteStatuser")}
      >
        <CheckboxList
          items={[
            { value: "publisert", label: "Publisert" },
            { value: "ikke-publisert", label: "Ikke publisert" },
          ]}
          isChecked={(id) => filter.publisert.includes(id)}
          onChange={(id) => {
            updateFilter({
              publisert: addOrRemove(filter.publisert, id),
              page: 1,
            });
          }}
        />
      </FilterAccordion>
    </Accordion>
  );
}
