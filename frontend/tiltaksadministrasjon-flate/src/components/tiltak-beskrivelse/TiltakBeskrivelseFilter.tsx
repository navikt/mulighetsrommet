import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterAccordion } from "@mr/frontend-common";
import { Accordion, Switch, VStack } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  tiltakBeskrivelseFilterAccordionAtom,
  TiltakBeskrivelseFilterType,
} from "@/pages/tiltak-beskrivelse/filter";
import { KontorstrukturFilter } from "@/components/filter/KontorstrukturFilter";
import {
  SortDirection,
  Tiltakskode,
  TiltakstypeEgenskap,
  TiltakstypeSortField,
} from "@tiltaksadministrasjon/api-client";
import { CheckboxList } from "@/components/filter/CheckboxList";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { TiltakskodeFilter } from "../filter/TiltakskodeFilter";

interface Props {
  filter: TiltakBeskrivelseFilterType;
  updateFilter: (values: Partial<TiltakBeskrivelseFilterType>) => void;
}

export function TiltakBeskrivelseFilter({ filter, updateFilter }: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(tiltakBeskrivelseFilterAccordionAtom);

  const toggleAccordion = (key: string) => {
    setAccordionsOpen([...addOrRemove(accordionsOpen, key)]);
  };

  return (
    <VStack gap="space-16">
      <Switch
        position="left"
        size="small"
        checked={filter.visMineTiltakBeskrivelser}
        onChange={(event) => {
          updateFilter({
            visMineTiltakBeskrivelser: event.currentTarget.checked,
            page: 1,
          });
        }}
      >
        Vis mine tiltaksbeskrivelser
      </Switch>
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
          <TiltakBeskrivelseTiltakstypeFilter
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
    </VStack>
  );
}

interface TiltakBeskrivelseTiltakstypeFilterProps {
  value: Tiltakskode[];
  onChange: (tiltakstyper: Tiltakskode[]) => void;
}

function TiltakBeskrivelseTiltakstypeFilter({
  value,
  onChange,
}: TiltakBeskrivelseTiltakstypeFilterProps) {
  const tiltakstyper = useTiltakstyper({
    sort: { field: TiltakstypeSortField.NAVN, direction: SortDirection.ASC },
    egenskaper: [TiltakstypeEgenskap.STOTTER_TILTAK_BESKRIVELSE],
  });
  return <TiltakskodeFilter tiltakstyper={tiltakstyper} value={value} onChange={onChange} />;
}
