import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { arrangorOptions, AVTALE_STATUS_OPTIONS, AVTALE_TYPE_OPTIONS } from "@/utils/filterUtils";
import { Accordion, Search, Switch, VStack } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { FilterAccordionHeader, FilterSkeleton } from "@mr/frontend-common";
import { CheckboxList } from "./CheckboxList";
import { avtaleFilterAccordionAtom, AvtaleFilterType } from "@/pages/avtaler/filter";
import { ArrangorKobling } from "@tiltaksadministrasjon/api-client";
import { NavEnhetFilter } from "@/components/filter/NavEnhetFilter";
import { useNavRegioner } from "@/api/enhet/useNavRegioner";
import { AvtaleTiltakstypeFilter } from "@/components/filter/AvtaleTiltakstypeFilter";

interface Props {
  filter: AvtaleFilterType;
  updateFilter: (values: Partial<AvtaleFilterType>) => void;
}

export function AvtaleFilter({ filter, updateFilter }: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(avtaleFilterAccordionAtom);

  const { data: regioner } = useNavRegioner();
  const { data: arrangorData } = useArrangorer(ArrangorKobling.AVTALE, {
    pageSize: 10000,
  });

  if (!arrangorData) {
    return <FilterSkeleton />;
  }

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
        <Accordion.Item open={accordionsOpen.includes("navEnhet")}>
          <Accordion.Header onClick={() => toggleAccordion("navEnhet")}>
            <FilterAccordionHeader
              tittel="Nav-enhet"
              antallValgteFilter={filter.navEnheter.length}
            />
          </Accordion.Header>
          <Accordion.Content>
            <NavEnhetFilter
              value={filter.navEnheter}
              onChange={(navEnheter) => {
                updateFilter({ navEnheter, page: 1 });
              }}
              regioner={regioner}
            />
          </Accordion.Content>
        </Accordion.Item>
        <Accordion.Item open={accordionsOpen.includes("status")}>
          <Accordion.Header onClick={() => toggleAccordion("status")}>
            <FilterAccordionHeader tittel="Status" antallValgteFilter={filter.statuser.length} />
          </Accordion.Header>
          <Accordion.Content>
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
          </Accordion.Content>
        </Accordion.Item>
        <Accordion.Item open={accordionsOpen.includes("tiltakstype")}>
          <Accordion.Header onClick={() => toggleAccordion("tiltakstype")}>
            <FilterAccordionHeader
              tittel="Tiltakstype"
              antallValgteFilter={filter.tiltakstyper.length}
            />
          </Accordion.Header>
          <Accordion.Content>
            <AvtaleTiltakstypeFilter
              value={filter.tiltakstyper}
              onChange={(tiltakstyper) => {
                updateFilter({ tiltakstyper, page: 1 });
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
        <Accordion.Item open={accordionsOpen.includes("avtaletype")}>
          <Accordion.Header onClick={() => toggleAccordion("avtaletype")}>
            <FilterAccordionHeader
              tittel="Avtaletype"
              antallValgteFilter={filter.avtaletyper.length}
            />
          </Accordion.Header>
          <Accordion.Content>
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
          </Accordion.Content>
        </Accordion.Item>
        <Accordion.Item open={accordionsOpen.includes("arrangor")}>
          <Accordion.Header onClick={() => toggleAccordion("arrangor")}>
            <FilterAccordionHeader
              tittel="Arrangør"
              antallValgteFilter={filter.arrangorer.length}
            />
          </Accordion.Header>
          <Accordion.Content>
            <CheckboxList
              searchable
              items={arrangorOptions(arrangorData.data)}
              isChecked={(id) => filter.arrangorer.includes(id)}
              onChange={(id) => updateArrayFilter("arrangorer", id)}
            />
          </Accordion.Content>
        </Accordion.Item>
        <Accordion.Item open={accordionsOpen.includes("personvern")}>
          <Accordion.Header onClick={() => toggleAccordion("personvern")}>
            <FilterAccordionHeader
              tittel="Personvern"
              antallValgteFilter={filter.personvernBekreftet ? 1 : 0}
            />
          </Accordion.Header>
          <Accordion.Content>
            <CheckboxList
              items={[
                {
                  label: "Bekreftet",
                  value: true,
                },
                {
                  label: "Ikke bekreftet",
                  value: false,
                },
              ]}
              isChecked={(b) => filter.personvernBekreftet === b}
              onChange={(bekreftet) => {
                updateFilter({
                  personvernBekreftet: bekreftet,
                  page: 1,
                });
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
    </VStack>
  );
}
