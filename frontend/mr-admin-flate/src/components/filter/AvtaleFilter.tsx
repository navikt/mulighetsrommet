import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { AvtaleFilter as AvtaleFilterProps, avtaleFilterAccordionAtom } from "@/api/atoms";
import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { addOrRemove } from "@/utils/Utils";
import {
  arrangorOptions,
  AVTALE_STATUS_OPTIONS,
  AVTALE_TYPE_OPTIONS,
  regionOptions,
  tiltakstypeOptions,
} from "@/utils/filterUtils";
import { Accordion, Search, Switch } from "@navikt/ds-react";
import { useAtom, WritableAtom } from "jotai";
import { ArrangorTil } from "mulighetsrommet-api-client";
import { FilterAccordionHeader, FilterSkeleton } from "mulighetsrommet-frontend-common";
import { logEvent } from "@/logging/amplitude";
import { CheckboxList } from "./CheckboxList";

type Filters = "tiltakstype";

interface Props {
  filterAtom: WritableAtom<AvtaleFilterProps, [newValue: AvtaleFilterProps], void>;
  skjulFilter?: Record<Filters, boolean>;
}

function loggBrukAvFilter(filter: string, value: any) {
  logEvent({
    name: "tiltaksadministrasjon.velg-avtale-filter",
    data: {
      filter,
      value,
    },
  });
}

export function AvtaleFilter({ filterAtom, skjulFilter }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const [accordionsOpen, setAccordionsOpen] = useAtom(avtaleFilterAccordionAtom);
  const { data: enheter, isLoading: isLoadingEnheter } = useNavEnheter();
  const { data: arrangorData, isLoading: isLoadingArrangorer } = useArrangorer(ArrangorTil.AVTALE, {
    pageSize: 10000,
  });
  const { data: tiltakstyper, isLoading: isLoadingTiltakstyper } = useTiltakstyper();

  if (
    !enheter ||
    isLoadingEnheter ||
    !arrangorData ||
    isLoadingArrangorer ||
    !tiltakstyper ||
    isLoadingTiltakstyper
  ) {
    return <FilterSkeleton />;
  }

  function selectDeselectAll(checked: boolean, key: string, values: string[]) {
    if (checked) {
      setFilter({
        ...filter,
        page: 1,
        [key]: values,
        lagretFilterIdValgt: undefined,
      });
      loggBrukAvFilter(key, "Velg alle");
    } else {
      setFilter({
        ...filter,
        page: 1,
        [key]: [],
        lagretFilterIdValgt: undefined,
      });
      loggBrukAvFilter(key, "Fjern alle");
    }
  }

  return (
    <>
      <Search
        label="Søk etter tiltaksgjennomføring"
        hideLabel
        size="small"
        variant="simple"
        placeholder="Navn, tiltaksnr., tiltaksarrangør"
        onBlur={() => {
          loggBrukAvFilter("sok", "REDACTED");
        }}
        onChange={(search: string) => {
          setFilter({
            ...filter,
            page: 1,
            sok: search,
            lagretFilterIdValgt: undefined,
          });
        }}
        value={filter.sok}
        aria-label="Søk etter tiltaksgjennomføring"
      />
      <div style={{ margin: "0.8rem 0.5rem" }}>
        <Switch
          position="left"
          size="small"
          checked={filter.visMineAvtaler}
          onChange={(event) => {
            setFilter({
              ...filter,
              page: 1,
              visMineAvtaler: event.currentTarget.checked,
              lagretFilterIdValgt: undefined,
            });
            loggBrukAvFilter("visMineAvtaler", event.currentTarget.checked);
          }}
        >
          <span style={{ fontWeight: "bold" }}>Vis kun mine avtaler</span>
        </Switch>
      </div>
      <Accordion>
        <Accordion.Item open={accordionsOpen.includes("status")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "status")]);
            }}
          >
            <FilterAccordionHeader tittel="Status" antallValgteFilter={filter.statuser.length} />
          </Accordion.Header>
          <Accordion.Content>
            <CheckboxList
              onSelectAll={(checked) => {
                selectDeselectAll(
                  checked,
                  "statuser",
                  AVTALE_STATUS_OPTIONS.map((s) => s.value),
                );
              }}
              items={AVTALE_STATUS_OPTIONS}
              isChecked={(status) => filter.statuser.includes(status)}
              onChange={(status) => {
                setFilter({
                  ...filter,
                  page: 1,
                  statuser: addOrRemove(filter.statuser, status),
                  lagretFilterIdValgt: undefined,
                });
                loggBrukAvFilter(
                  "statuser",
                  AVTALE_STATUS_OPTIONS.find((s) => s.value === status)?.label,
                );
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
        <Accordion.Item open={accordionsOpen.includes("avtaletype")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "avtaletype")]);
            }}
          >
            <FilterAccordionHeader
              tittel="Avtaletype"
              antallValgteFilter={filter.avtaletyper.length}
            />
          </Accordion.Header>
          <Accordion.Content>
            <CheckboxList
              onSelectAll={(checked) => {
                selectDeselectAll(
                  checked,
                  "avtaletyper",
                  AVTALE_TYPE_OPTIONS.map((a) => a.value),
                );
              }}
              items={AVTALE_TYPE_OPTIONS}
              isChecked={(type) => filter.avtaletyper.includes(type)}
              onChange={(type) => {
                setFilter({
                  ...filter,
                  page: 1,
                  avtaletyper: addOrRemove(filter.avtaletyper, type),
                  lagretFilterIdValgt: undefined,
                });
                loggBrukAvFilter(
                  "avtaletyper",
                  AVTALE_TYPE_OPTIONS.find((a) => a.value === type)?.label,
                );
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
        {!skjulFilter?.tiltakstype && (
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
              <CheckboxList
                onSelectAll={(checked) => {
                  selectDeselectAll(
                    checked,
                    "tiltakstyper",
                    tiltakstyper.data.map((t) => t.id),
                  );
                }}
                items={tiltakstypeOptions(tiltakstyper.data)}
                isChecked={(tiltakstype) => filter.tiltakstyper.includes(tiltakstype)}
                onChange={(tiltakstype) => {
                  setFilter({
                    ...filter,
                    page: 1,
                    tiltakstyper: addOrRemove(filter.tiltakstyper, tiltakstype),
                    lagretFilterIdValgt: undefined,
                  });
                  loggBrukAvFilter(
                    "tiltakstyper",
                    tiltakstyper.data.find((t) => t.id === tiltakstype)?.navn,
                  );
                }}
              />
            </Accordion.Content>
          </Accordion.Item>
        )}
        <Accordion.Item open={accordionsOpen.includes("region")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "region")]);
            }}
          >
            <FilterAccordionHeader tittel="Region" antallValgteFilter={filter.navRegioner.length} />
          </Accordion.Header>
          <Accordion.Content>
            <CheckboxList
              items={regionOptions(enheter)}
              isChecked={(region) => filter.navRegioner.includes(region)}
              onChange={(region) => {
                setFilter({
                  ...filter,
                  page: 1,
                  navRegioner: addOrRemove(filter.navRegioner, region),
                  lagretFilterIdValgt: undefined,
                });
                loggBrukAvFilter(
                  "navRegioner",
                  enheter.find((e) => e.enhetsnummer === region)?.navn,
                );
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
        <Accordion.Item open={accordionsOpen.includes("arrangor")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "arrangor")]);
            }}
          >
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
              onChange={(id) => {
                setFilter({
                  ...filter,
                  page: 1,
                  arrangorer: addOrRemove(filter.arrangorer, id),
                  lagretFilterIdValgt: undefined,
                });
                loggBrukAvFilter("arrangorer", arrangorData.data.find((a) => a.id === id)?.navn);
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
        <Accordion.Item open={accordionsOpen.includes("personvern")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "personvern")]);
            }}
          >
            <FilterAccordionHeader
              tittel="Personvern"
              antallValgteFilter={filter.personvernBekreftet.length}
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
              isChecked={(b) => filter.personvernBekreftet.includes(b)}
              onChange={(bekreftet) => {
                setFilter({
                  ...filter,
                  page: 1,
                  personvernBekreftet: addOrRemove(filter.personvernBekreftet, bekreftet),
                  lagretFilterIdValgt: undefined,
                });
                loggBrukAvFilter("personvernBekreftet", bekreftet);
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
    </>
  );
}
