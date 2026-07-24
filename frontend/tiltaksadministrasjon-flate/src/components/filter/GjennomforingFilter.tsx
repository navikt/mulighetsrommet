import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { TILTAKSGJENNOMFORING_STATUS_OPTIONS } from "@/utils/filterUtils";
import { FilterAccordion } from "@mr/frontend-common";
import { Accordion, Search, Switch, VStack } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { CheckboxList } from "./CheckboxList";
import {
  gjennomforingFilterAccordionAtom,
  GjennomforingFilterType,
} from "@/pages/gjennomforing/filter";
import {
  ArrangorKobling,
  AvtaleDto,
  FeatureToggle,
  GjennomforingType,
} from "@tiltaksadministrasjon/api-client";
import { GjennomforingTiltakstypeFilter } from "@/components/filter/GjennomforingTiltakstypeFilter";
import { KontorstrukturFilter } from "@/components/filter/KontorstrukturFilter";
import { ArrangorerFilter } from "./ArrangorerFilter";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { gjennomforingTypeToString } from "@/utils/Utils";

type Filters = "tiltakstype";

interface Props {
  filter: GjennomforingFilterType;
  updateFilter: (values: Partial<GjennomforingFilterType>) => void;
  skjulFilter?: Record<Filters, boolean>;
  avtale?: AvtaleDto;
  lagredeFilterOversikt?: React.ReactElement;
}

export function GjennomforingFilter({
  filter,
  updateFilter,
  skjulFilter,
  lagredeFilterOversikt,
}: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(gjennomforingFilterAccordionAtom);
  const { data: enableEnkeltplassFilter } = useFeatureToggle(
    FeatureToggle.TILTAKSADMINISTRASJON_ENKELTPLASS_FILTER,
  );

  const toggleAccordion = (key: string) => {
    setAccordionsOpen([...addOrRemove(accordionsOpen, key)]);
  };

  function selectDeselectAll(checked: boolean, key: string, values: string[]) {
    updateFilter({
      [key]: checked ? values : [],
      page: 1,
    });
  }
  return (
    <VStack gap="space-16">
      <Search
        label="Søk etter tiltaksgjennomføring"
        size="small"
        variant="secondary"
        placeholder="Navn, tiltaksnr., tiltaksarrangør"
        onChange={(search: string) => {
          updateFilter({
            search,
            page: 1,
          });
        }}
        value={filter.search}
      />
      <Switch
        position="left"
        size="small"
        checked={filter.visMineGjennomforinger}
        onChange={(event) => {
          updateFilter({
            visMineGjennomforinger: event.currentTarget.checked,
            page: 1,
          });
        }}
      >
        Vis kun mine gjennomføringer
      </Switch>
      <Accordion size="small">
        {lagredeFilterOversikt && (
          <FilterAccordion
            tittel="Lagrede filter"
            open={accordionsOpen.includes("lagrede-filter")}
            onClick={() => toggleAccordion("lagrede-filter")}
          >
            {lagredeFilterOversikt}
          </FilterAccordion>
        )}
        <FilterAccordion
          tittel="Nav-enhet"
          antallValgteFilter={filter.navEnheter.length}
          open={accordionsOpen.includes("navEnhet")}
          onClick={() => toggleAccordion("navEnhet")}
        >
          <KontorstrukturFilter
            value={filter.navEnheter}
            onChange={(navEnheter) => {
              updateFilter({ navEnheter, page: 1 });
            }}
          />
        </FilterAccordion>
        {!skjulFilter?.tiltakstype && (
          <FilterAccordion
            tittel="Tiltakstype"
            antallValgteFilter={filter.tiltakstyper.length}
            open={accordionsOpen.includes("tiltakstype")}
            onClick={() => toggleAccordion("tiltakstype")}
          >
            <GjennomforingTiltakstypeFilter
              value={filter.tiltakstyper}
              onChange={(tiltakstyper) => {
                updateFilter({ tiltakstyper, page: 1 });
              }}
            />
          </FilterAccordion>
        )}
        {enableEnkeltplassFilter && (
          <FilterAccordion
            tittel="Gjennomføringtype"
            antallValgteFilter={filter.gjennomforingTyper.length}
            open={accordionsOpen.includes("gjennomforingType")}
            onClick={() => toggleAccordion("gjennomforingType")}
          >
            <CheckboxList
              items={[
                {
                  label: gjennomforingTypeToString(GjennomforingType.AVTALE),
                  value: GjennomforingType.AVTALE,
                },
                {
                  label: gjennomforingTypeToString(GjennomforingType.ENKELTPLASS),
                  value: GjennomforingType.ENKELTPLASS,
                },
              ]}
              isChecked={(type) => filter.gjennomforingTyper.includes(type)}
              onChange={(type) => {
                updateFilter({
                  gjennomforingTyper: addOrRemove(filter.gjennomforingTyper, type),
                  page: 1,
                });
              }}
            />
          </FilterAccordion>
        )}
        <FilterAccordion
          tittel="Status"
          antallValgteFilter={filter.statuser.length}
          open={accordionsOpen.includes("status")}
          onClick={() => toggleAccordion("status")}
        >
          <CheckboxList
            onSelectAll={(checked) => {
              selectDeselectAll(
                checked,
                "statuser",
                TILTAKSGJENNOMFORING_STATUS_OPTIONS.map((s) => s.value),
              );
            }}
            items={TILTAKSGJENNOMFORING_STATUS_OPTIONS}
            isChecked={(status) => filter.statuser.includes(status)}
            onChange={(status) => {
              updateFilter({
                statuser: addOrRemove(filter.statuser, status),
                page: 1,
              });
            }}
          />
        </FilterAccordion>

        <FilterAccordion
          tittel="Arrangør"
          antallValgteFilter={filter.arrangorer.length}
          open={accordionsOpen.includes("arrangor")}
          onClick={() => toggleAccordion("arrangor")}
        >
          <ArrangorerFilter
            filter={filter.arrangorer}
            updateFilter={(arrangorer) => updateFilter({ arrangorer, page: 1 })}
            arrangorKobling={ArrangorKobling.TILTAKSGJENNOMFORING}
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
