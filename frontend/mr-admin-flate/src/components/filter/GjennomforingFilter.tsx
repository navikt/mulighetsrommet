import { useNavRegioner } from "@/api/enhet/useNavRegioner";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { TILTAKSGJENNOMFORING_STATUS_OPTIONS } from "@/utils/filterUtils";
import { FilterAccordionHeader } from "@mr/frontend-common";
import { Accordion, Search, Switch } from "@navikt/ds-react";
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
import { NavEnhetFilter } from "@/components/filter/NavEnhetFilter";
import { GjennomforingTiltakstypeFilter } from "@/components/filter/GjennomforingTiltakstypeFilter";
import { ArrangorerFilter } from "./ArrangorerFilter";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";

type Filters = "tiltakstype";

interface Props {
  filter: GjennomforingFilterType;
  updateFilter: (values: Partial<GjennomforingFilterType>) => void;
  skjulFilter?: Record<Filters, boolean>;
  avtale?: AvtaleDto;
}

export function GjennomforingFilter({ filter, updateFilter, skjulFilter }: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(gjennomforingFilterAccordionAtom);
  const { data: regioner } = useNavRegioner();
  const { data: enableEnkeltplassFilter } = useFeatureToggle(
    FeatureToggle.TILTAKSADMINISTRASJON_ENKELTPLASS_FILTER,
  );

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
        onChange={(search: string) => {
          updateFilter({
            search,
            page: 1,
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
            });
          }}
        >
          <span style={{ fontWeight: "bold" }}>Vis kun mine gjennomføringer</span>
        </Switch>
      </div>
      {enableEnkeltplassFilter && (
        <Accordion>
          <Accordion.Item open={accordionsOpen.includes("gjennomforingType")}>
            <Accordion.Header
              onClick={() => {
                setAccordionsOpen([...addOrRemove(accordionsOpen, "gjennomforingType")]);
              }}
            >
              <FilterAccordionHeader
                tittel="Gjennomføringtype"
                antallValgteFilter={filter.gjennomforingTyper.length}
              />
            </Accordion.Header>
            <Accordion.Content>
              <CheckboxList
                items={[
                  {
                    label: "Gruppe",
                    value: GjennomforingType.AVTALE,
                  },
                  {
                    label: "Enkeltplass",
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
            </Accordion.Content>
          </Accordion.Item>
        </Accordion>
      )}
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
                updateFilter({
                  statuser: addOrRemove(filter.statuser, status),
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
          <Accordion.Content>
            <ArrangorerFilter
              filter={filter.arrangorer}
              updateFilter={(arrangorer) => updateFilter({ arrangorer, page: 1 })}
              arrangorKobling={ArrangorKobling.TILTAKSGJENNOMFORING}
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
              <GjennomforingTiltakstypeFilter
                value={filter.tiltakstyper}
                onChange={(tiltakstyper) => {
                  updateFilter({ tiltakstyper, page: 1 });
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
          <Accordion.Content>
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
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
    </>
  );
}
