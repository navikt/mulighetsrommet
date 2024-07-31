import { Accordion, Search, Switch } from "@navikt/ds-react";
import { useAtom, WritableAtom } from "jotai";
import { ArrangorTil, NavEnhet } from "mulighetsrommet-api-client";
import { useEffect } from "react";
import {
  gjennomforingFilterAccordionAtom,
  TiltaksgjennomforingFilter as TiltaksgjennomforingFilterProps,
} from "@/api/atoms";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { addOrRemove } from "@/utils/Utils";
import {
  arrangorOptions,
  TILTAKSGJENNOMFORING_STATUS_OPTIONS,
  tiltakstypeOptions,
} from "@/utils/filterUtils";
import {
  FilterAccordionHeader,
  FilterSkeleton,
  NavEnhetFilter,
} from "mulighetsrommet-frontend-common";
import { useRegioner } from "@/api/enhet/useRegioner";
import { CheckboxList } from "./CheckboxList";
import { logEvent } from "@/logging/amplitude";

type Filters = "tiltakstype";

interface Props {
  filterAtom: WritableAtom<
    TiltaksgjennomforingFilterProps,
    [newValue: TiltaksgjennomforingFilterProps],
    void
  >;
  skjulFilter?: Record<Filters, boolean>;
}

function loggBrukAvFilter(filter: string, value: any) {
  logEvent({
    name: "tiltaksadministrasjon.velg-tiltaksgjennomforing-filter",
    data: {
      filter,
      value,
    },
  });
}

export function TiltaksgjennomforingFilter({ filterAtom, skjulFilter }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const [accordionsOpen, setAccordionsOpen] = useAtom(gjennomforingFilterAccordionAtom);
  const { data: avtale } = useAvtale();
  const { data: enheter, isLoading: isLoadingEnheter } = useNavEnheter();
  const { data: regioner, isLoading: isLoadingRegioner } = useRegioner();
  const { data: arrangorer, isLoading: isLoadingArrangorer } = useArrangorer(
    ArrangorTil.TILTAKSGJENNOMFORING,
    {
      pageSize: 10000,
    },
  );
  const { data: tiltakstyper, isLoading: isLoadingTiltakstyper } = useTiltakstyper();

  useEffect(() => {
    setFilter({
      ...filter,
      avtale: avtale?.id ?? "",
    });
  }, [avtale]);

  if (
    !enheter ||
    isLoadingEnheter ||
    !regioner ||
    isLoadingRegioner ||
    !arrangorer ||
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
        onChange={(search: string) => {
          setFilter({
            ...filter,
            page: 1,
            lagretFilterIdValgt: undefined,
            search,
          });
        }}
        onBlur={() => {
          loggBrukAvFilter("sok", "REDACTED");
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
            loggBrukAvFilter("visMineGjennomforinger", event.currentTarget.checked);
          }}
        >
          <span style={{ fontWeight: "bold" }}>Vis kun mine gjennomføringer</span>
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
                loggBrukAvFilter(
                  "status",
                  TILTAKSGJENNOMFORING_STATUS_OPTIONS.find((s) => s.value === status)?.label,
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
                    lagretFilterIdValgt: undefined,
                    tiltakstyper: addOrRemove(filter.tiltakstyper, tiltakstype),
                  });
                  loggBrukAvFilter(
                    "status",
                    tiltakstyper.data.find((s) => s.id === tiltakstype)?.navn,
                  );
                }}
              />
            </Accordion.Content>
          </Accordion.Item>
        )}
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
          <Accordion.Content>
            <div style={{ marginLeft: "-2rem" }}>
              <NavEnhetFilter
                navEnheter={filter.navEnheter}
                setNavEnheter={(navEnheter: NavEnhet[]) => {
                  setFilter({
                    ...filter,
                    page: 1,
                    lagretFilterIdValgt: undefined,
                    navEnheter,
                  });
                  loggBrukAvFilter(
                    "navEnheter",
                    navEnheter.map((n) => n.navn),
                  );
                }}
                regioner={regioner}
              />
            </div>
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
              items={arrangorOptions(arrangorer.data)}
              isChecked={(id) => filter.arrangorer.includes(id)}
              onChange={(id) => {
                setFilter({
                  ...filter,
                  page: 1,
                  lagretFilterIdValgt: undefined,
                  arrangorer: addOrRemove(filter.arrangorer, id),
                });
                loggBrukAvFilter("arrangorer", arrangorer.data.find((a) => a.id === id)?.navn);
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
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
          <Accordion.Content>
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
                loggBrukAvFilter("publisert", id);
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
    </>
  );
}
