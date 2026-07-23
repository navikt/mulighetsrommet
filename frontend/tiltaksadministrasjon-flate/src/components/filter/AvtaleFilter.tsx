import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { AVTALE_STATUS_OPTIONS, AVTALE_TYPE_OPTIONS } from "@/utils/filterUtils";
import { Accordion, Search, Switch, VStack } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { FilterAccordion } from "@mr/frontend-common";
import { CheckboxList } from "./CheckboxList";
import { avtaleFilterAccordionAtom, AvtaleFilterType } from "@/pages/avtaler/filter";
import { ArrangorKobling } from "@tiltaksadministrasjon/api-client";
import { AvtaleTiltakstypeFilter } from "@/components/filter/AvtaleTiltakstypeFilter";
import { ArrangorerFilter } from "./ArrangorerFilter";
import { KontorstrukturFilter } from "@/components/filter/KontorstrukturFilter";

interface Props {
  filter: AvtaleFilterType;
  updateFilter: (values: Partial<AvtaleFilterType>) => void;
  lagredeFilterOversikt: React.ReactElement;
}

export function AvtaleFilter({ filter, updateFilter, lagredeFilterOversikt }: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(avtaleFilterAccordionAtom);

  const toggleAccordion = (key: string) => {
    setAccordionsOpen([...addOrRemove(accordionsOpen, key)]);
  };

  const updateArrayFilter = (key: keyof AvtaleFilterType, value: any) => {
    updateFilter({
      [key]: addOrRemove(filter[key] as any[], value),
      page: 1,
    });
  };

  return (
    <VStack gap="space-8">
      <Search
        label="Søk etter tiltaksgjennomføring"
        hideLabel
        size="small"
        variant="simple"
        placeholder="Navn, tiltaksnr., tiltaksarrangør"
        onBlur={() => {}}
        onChange={(search: string) => {
          updateFilter({
            sok: search,
            page: 1,
          });
        }}
        value={filter.sok}
        aria-label="Søk etter tiltaksgjennomføring"
      />
      <Switch
        position="left"
        size="small"
        checked={filter.visMineAvtaler}
        onChange={(event) => {
          updateFilter({
            visMineAvtaler: event.currentTarget.checked,
            page: 1,
          });
        }}
      >
        Vis kun mine avtaler
      </Switch>
      <Accordion>
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
        <FilterAccordion
          tittel="Status"
          antallValgteFilter={filter.statuser.length}
          open={accordionsOpen.includes("status")}
          onClick={() => toggleAccordion("status")}
        >
          <CheckboxList
            onSelectAll={(checked) =>
              updateFilter({
                statuser: checked ? AVTALE_STATUS_OPTIONS.map((s) => s.value) : [],
                page: 1,
              })
            }
            items={AVTALE_STATUS_OPTIONS}
            isChecked={(status) => filter.statuser.includes(status)}
            onChange={(status) => updateArrayFilter("statuser", status)}
          />
        </FilterAccordion>
        <FilterAccordion
          tittel="Tiltakstype"
          antallValgteFilter={filter.tiltakstyper.length}
          open={accordionsOpen.includes("tiltakstype")}
          onClick={() => toggleAccordion("tiltakstype")}
        >
          <AvtaleTiltakstypeFilter
            value={filter.tiltakstyper}
            onChange={(tiltakstyper) => {
              updateFilter({ tiltakstyper, page: 1 });
            }}
          />
        </FilterAccordion>
        <FilterAccordion
          tittel="Avtaletype"
          antallValgteFilter={filter.avtaletyper.length}
          open={accordionsOpen.includes("avtaletype")}
          onClick={() => toggleAccordion("avtaletype")}
        >
          <CheckboxList
            onSelectAll={(checked) =>
              updateFilter({
                avtaletyper: checked ? AVTALE_TYPE_OPTIONS.map((a) => a.value) : [],
                page: 1,
              })
            }
            items={AVTALE_TYPE_OPTIONS}
            isChecked={(type) => filter.avtaletyper.includes(type)}
            onChange={(type) => updateArrayFilter("avtaletyper", type)}
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
            arrangorKobling={ArrangorKobling.AVTALE}
          />
        </FilterAccordion>
        <FilterAccordion
          tittel="Personvern"
          antallValgteFilter={filter.personvernBekreftet ? 1 : 0}
          open={accordionsOpen.includes("personvern")}
          onClick={() => toggleAccordion("personvern")}
        >
          <CheckboxList
            items={[
              { label: "Bekreftet", value: true },
              { label: "Ikke bekreftet", value: false },
            ]}
            isChecked={(b) => filter.personvernBekreftet === b}
            onChange={(bekreftet) => {
              updateFilter({
                personvernBekreftet: bekreftet,
                page: 1,
              });
            }}
          />
        </FilterAccordion>
      </Accordion>
    </VStack>
  );
}
