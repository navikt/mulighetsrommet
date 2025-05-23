import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { gjennomforingFilterAccordionAtom, GjennomforingFilterType } from "@/api/atoms";
import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import { useNavRegioner } from "@/api/enhet/useNavRegioner";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import {
  arrangorOptions,
  TILTAKSGJENNOMFORING_STATUS_OPTIONS,
  tiltakstypeOptions,
} from "@/utils/filterUtils";
import { ArrangorTil, AvtaleDto } from "@mr/api-client-v2";
import { FilterAccordionHeader, FilterSkeleton, NavEnhetFilter } from "@mr/frontend-common";
import { Accordion, Search, Switch } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { CheckboxList } from "./CheckboxList";

type Filters = "tiltakstype";

interface Props {
  filter: GjennomforingFilterType;
  updateFilter: (values: Partial<GjennomforingFilterType>) => void;
  skjulFilter?: Record<Filters, boolean>;
  avtale?: AvtaleDto;
}

export function GjennomforingFilter({ filter, updateFilter, skjulFilter }: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(gjennomforingFilterAccordionAtom);
  const { data: tiltakstyper } = useTiltakstyper();
  const { data: enheter } = useNavEnheter();
  const { data: regioner } = useNavRegioner();
  const { data: arrangorer, isLoading: isLoadingArrangorer } = useArrangorer(
    ArrangorTil.TILTAKSGJENNOMFORING,
    {
      pageSize: 10000,
    },
  );

  if (!arrangorer || isLoadingArrangorer) {
    return <FilterSkeleton />;
  }

  function selectDeselectAll(checked: boolean, key: string, values: string[]) {
    updateFilter({
      [key]: checked ? values : [],
      page: 1,
      lagretFilterIdValgt: undefined,
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
        onChange={(search: string) => {
          updateFilter({
            search,
            page: 1,
            lagretFilterIdValgt: undefined,
          });
        }}
        value={filter.search}
        aria-label="Søk etter tiltaksgjennomføring"
      />
      <div style={{ margin: "0.8rem 0.5rem" }}>
        <Switch
          position="left"
          size="small"
          checked={filter.visMineGjennomforinger}
          onChange={(event) => {
            updateFilter({
              visMineGjennomforinger: event.currentTarget.checked,
              page: 1,
              lagretFilterIdValgt: undefined,
            });
          }}
        >
          <span style={{ fontWeight: "bold" }}>Vis kun mine gjennomføringer</span>
        </Switch>
      </div>
      <Accordion>
        <Accordion.Item open={accordionsOpen.includes("navEnhet")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "navEnhet")]);
            }}
          >
            <FilterAccordionHeader
              tittel="Nav-enhet"
              antallValgteFilter={filter.navEnheter.length}
            />
          </Accordion.Header>
          <Accordion.Content className="ml-[-2rem]">
            <NavEnhetFilter
              navEnheter={filter.navEnheter}
              setNavEnheter={(navEnheter: string[]) => {
                updateFilter({
                  navEnheter: enheter.filter((enhet) => navEnheter.includes(enhet.enhetsnummer)),
                  page: 1,
                  lagretFilterIdValgt: undefined,
                });
              }}
              regioner={regioner}
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
                  TILTAKSGJENNOMFORING_STATUS_OPTIONS.map((s) => s.value),
                );
              }}
              items={TILTAKSGJENNOMFORING_STATUS_OPTIONS}
              isChecked={(status) => filter.statuser.includes(status)}
              onChange={(status) => {
                updateFilter({
                  statuser: addOrRemove(filter.statuser, status),
                  page: 1,
                  lagretFilterIdValgt: undefined,
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
              items={arrangorOptions(arrangorer.data)}
              isChecked={(id) => filter.arrangorer.includes(id)}
              onChange={(id) => {
                updateFilter({
                  arrangorer: addOrRemove(filter.arrangorer, id),
                  page: 1,
                  lagretFilterIdValgt: undefined,
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
                    lagretFilterIdValgt: undefined,
                  });
                }}
              />
            </Accordion.Content>
          </Accordion.Item>
        )}

        <Accordion.Item open={accordionsOpen.includes("publiserteStatuser")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "publiserteStatuser")]);
            }}
          >
            <FilterAccordionHeader
              tittel="Publisert"
              antallValgteFilter={filter.publisert.length}
            />
          </Accordion.Header>
          <Accordion.Content className="ml-[-2rem]">
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
                  lagretFilterIdValgt: undefined,
                });
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
    </>
  );
}
