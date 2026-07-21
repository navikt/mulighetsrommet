import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterAccordionHeader } from "@mr/frontend-common";
import { Accordion } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  tiltakDokumentFilterAccordionAtom,
  TiltakDokumentFilterType,
} from "@/pages/tiltak-dokument/filter";
import { GjennomforingTiltakstypeFilter } from "@/components/filter/GjennomforingTiltakstypeFilter";
import { KontorstrukturFilter } from "@/components/filter/KontorstrukturFilter";
import { Tiltakskode } from "@tiltaksadministrasjon/api-client";

interface Props {
  filter: TiltakDokumentFilterType;
  updateFilter: (values: Partial<TiltakDokumentFilterType>) => void;
}

export function TiltakDokumentFilter({ filter, updateFilter }: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(tiltakDokumentFilterAccordionAtom);

  return (
    <Accordion>
      <Accordion.Item open={accordionsOpen.includes("navEnhet")}>
        <Accordion.Header
          onClick={() => setAccordionsOpen([...addOrRemove(accordionsOpen, "navEnhet")])}
        >
          <FilterAccordionHeader tittel="Nav-enhet" antallValgteFilter={filter.navEnheter.length} />
        </Accordion.Header>
        <Accordion.Content>
          <KontorstrukturFilter
            value={filter.navEnheter}
            onChange={(navEnheter) => updateFilter({ navEnheter, page: 1 })}
          />
        </Accordion.Content>
      </Accordion.Item>

      <Accordion.Item open={accordionsOpen.includes("tiltakstype")}>
        <Accordion.Header
          onClick={() => setAccordionsOpen([...addOrRemove(accordionsOpen, "tiltakstype")])}
        >
          <FilterAccordionHeader
            tittel="Tiltakstype"
            antallValgteFilter={filter.tiltakstyper.length}
          />
        </Accordion.Header>
        <Accordion.Content>
          <GjennomforingTiltakstypeFilter
            value={filter.tiltakstyper as Tiltakskode[]}
            onChange={(tiltakstyper) => updateFilter({ tiltakstyper, page: 1 })}
          />
        </Accordion.Content>
      </Accordion.Item>
    </Accordion>
  );
}
