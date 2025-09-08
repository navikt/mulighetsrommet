import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import {
  arrangorOptions,
  AVTALE_STATUS_OPTIONS,
  AVTALE_TYPE_OPTIONS,
  regionOptions,
  tiltakstypeOptions,
} from "@/utils/filterUtils";
import { Accordion, Search, Switch } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { FilterAccordionHeader, FilterSkeleton } from "@mr/frontend-common";
import { CheckboxList } from "./CheckboxList";
import { avtaleFilterAccordionAtom, AvtaleFilterType } from "@/pages/avtaler/filter";
import { ArrangorKobling } from "@tiltaksadministrasjon/api-client";

type Filters = "tiltakstype";

interface Props {
  filter: AvtaleFilterType;
  updateFilter: (values: Partial<AvtaleFilterType>) => void;
  skjulFilter?: Record<Filters, boolean>;
}

export function AvtaleFilter({ filter, updateFilter, skjulFilter }: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(avtaleFilterAccordionAtom);
  const { data: tiltakstyper } = useTiltakstyper();
  const { data: enheter } = useNavEnheter();
  const { data: arrangorData } = useArrangorer(ArrangorKobling.AVTALE, {
    pageSize: 10000,
  });

  if (!arrangorData) {
    return <FilterSkeleton />;
  }

  function selectDeselectAll(checked: boolean, key: string, values: string[]) {
    updateFilter({
      [key]: checked ? values : [],
      page: 1,
    });
  }

  return (
    <>
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
      <div style={{ margin: "0.8rem 0.5rem" }}>
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
          <span style={{ fontWeight: "bold" }}>Vis kun mine avtaler</span>
        </Switch>
      </div>
      <Accordion>
        <Accordion.Item open={accordionsOpen.includes("region")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "region")]);
            }}
          >
            <FilterAccordionHeader tittel="Region" antallValgteFilter={filter.navRegioner.length} />
          </Accordion.Header>
          <Accordion.Content className="ml-[-2rem]">
            <CheckboxList
              items={regionOptions(enheter)}
              isChecked={(region) => filter.navRegioner.includes(region)}
              onChange={(region) => {
                updateFilter({
                  navRegioner: addOrRemove(filter.navRegioner, region),
                  page: 1,
                });
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
        <Accordion.Item open={accordionsOpen.includes("status")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "status")]);
            }}
          >
            <FilterAccordionHeader tittel="Status" antallValgteFilter={filter.statuser.length} />
          </Accordion.Header>
          <Accordion.Content className="ml-[-2rem]">
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
                updateFilter({
                  statuser: addOrRemove(filter.statuser, status),
                  page: 1,
                });
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
            <Accordion.Content className="ml-[-2rem]">
              <CheckboxList
                onSelectAll={(checked) => {
                  selectDeselectAll(
                    checked,
                    "tiltakstyper",
                    tiltakstyper.map((t) => t.id),
                  );
                }}
                items={tiltakstypeOptions(tiltakstyper)}
                isChecked={(tiltakstype) => filter.tiltakstyper.includes(tiltakstype)}
                onChange={(tiltakstype) => {
                  updateFilter({
                    tiltakstyper: addOrRemove(filter.tiltakstyper, tiltakstype),
                    page: 1,
                  });
                }}
              />
            </Accordion.Content>
          </Accordion.Item>
        )}
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
          <Accordion.Content className="ml-[-2rem]">
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
                updateFilter({
                  avtaletyper: addOrRemove(filter.avtaletyper, type),
                  page: 1,
                });
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
          <Accordion.Content className="ml-[-2rem]">
            <CheckboxList
              searchable
              items={arrangorOptions(arrangorData.data)}
              isChecked={(id) => filter.arrangorer.includes(id)}
              onChange={(id) => {
                updateFilter({
                  arrangorer: addOrRemove(filter.arrangorer, id),
                  page: 1,
                });
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
              antallValgteFilter={filter.personvernBekreftet ? 1 : 0}
            />
          </Accordion.Header>
          <Accordion.Content className="ml-[-2rem]">
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
    </>
  );
}
