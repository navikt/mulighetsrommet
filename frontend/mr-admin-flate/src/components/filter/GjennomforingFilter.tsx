import { useArrangorer } from "@/api/arrangor/useArrangorer";
import {
  GjennomforingFilterType as GjennomforingFilterProps,
  gjennomforingFilterAccordionAtom,
} from "@/api/atoms";
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
import { useAtom, WritableAtom } from "jotai";
import { useEffect } from "react";
import { CheckboxList } from "./CheckboxList";

type Filters = "tiltakstype";

interface Props {
  filterAtom: WritableAtom<GjennomforingFilterProps, [newValue: GjennomforingFilterProps], void>;
  skjulFilter?: Record<Filters, boolean>;
  avtale?: AvtaleDto;
}

export function GjennomforingFilter({ filterAtom, skjulFilter, avtale }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
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

  useEffect(() => {
    setFilter({
      ...filter,
      avtale: avtale?.id ?? "",
    });
  }, [avtale, filter, setFilter]);

  if (!arrangorer || isLoadingArrangorer) {
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
    } else {
      setFilter({
        ...filter,
        page: 1,
        [key]: [],
        lagretFilterIdValgt: undefined,
      });
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
        onChange={(search: string) => {
          setFilter({
            ...filter,
            page: 1,
            lagretFilterIdValgt: undefined,
            search,
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
            setFilter({
              ...filter,
              page: 1,
              lagretFilterIdValgt: undefined,
              visMineGjennomforinger: event.currentTarget.checked,
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
                setFilter({
                  ...filter,
                  page: 1,
                  lagretFilterIdValgt: undefined,
                  navEnheter: enheter.filter((enhet) => navEnheter.includes(enhet.enhetsnummer)),
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
                setFilter({
                  ...filter,
                  page: 1,
                  lagretFilterIdValgt: undefined,
                  statuser: addOrRemove(filter.statuser, status),
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
                setFilter({
                  ...filter,
                  page: 1,
                  lagretFilterIdValgt: undefined,
                  arrangorer: addOrRemove(filter.arrangorer, id),
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
                  setFilter({
                    ...filter,
                    page: 1,
                    lagretFilterIdValgt: undefined,
                    tiltakstyper: addOrRemove(filter.tiltakstyper, tiltakstype),
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
                setFilter({
                  ...filter,
                  page: 1,
                  lagretFilterIdValgt: undefined,
                  publisert: addOrRemove(filter.publisert, id),
                });
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
    </>
  );
}
