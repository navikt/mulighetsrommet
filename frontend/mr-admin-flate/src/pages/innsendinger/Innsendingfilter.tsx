import { FilterAccordionHeader, FilterSkeleton, NavEnhetFilter } from "@mr/frontend-common";
import { InnsendingFilterAccordionAtom, InnsendingFilterType } from "./filter";
import { useAtom } from "jotai";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { CheckboxList } from "@/components/filter/CheckboxList";
import { tiltakstypeOptions } from "@/utils/filterUtils";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { Accordion, Checkbox, CheckboxGroup } from "@navikt/ds-react";
import { ArrangorKobling } from "@tiltaksadministrasjon/api-client";
import { useKostnadssted } from "@/api/enhet/useKostnadssted";
import { useKostnadsstedFiltre } from "@/api/enhet/useKostnadsstedFiltre";

type Filters = "tiltakstype" | "navEnhet" | "sortering";

interface Props {
  filter: InnsendingFilterType;
  updateFilter: (values: Partial<InnsendingFilterType>) => void;
  skjulFilter?: Record<Filters, boolean>;
}

export function InnsendingFilter({ filter, updateFilter, skjulFilter }: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(InnsendingFilterAccordionAtom);
  const { data: tiltakstyper } = useTiltakstyper();
  const { data: regioner } = useKostnadsstedFiltre();
  const { data: kostnadssteder } = useKostnadssted([]);
  const { data: arrangorer } = useArrangorer(ArrangorKobling.TILTAKSGJENNOMFORING, {
    pageSize: 10000,
  });

  const kostnadsstedRegioner = regioner.filter((region) => region.enheter.length > 1);
  const enkeltKostnadssteder = kostnadssteder.filter((enhet) => !enhet.overordnetEnhet);

  if (!arrangorer) {
    return <FilterSkeleton />;
  }
  function selectDeselectAll(checked: boolean, key: string, values: string[]) {
    updateFilter({
      [key]: checked ? values : [],
    });
  }

  return (
    <>
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
              value={filter.navEnheter}
              onChange={(navEnheter: string[]) => {
                updateFilter({
                  navEnheter: kostnadssteder.filter((enhet) =>
                    navEnheter.includes(enhet.enhetsnummer),
                  ),
                });
              }}
              regioner={kostnadsstedRegioner}
            />
            <CheckboxGroup legend="Enkeltstående kostnadssteder" size="small" className="mt-2">
              {enkeltKostnadssteder.map((kostnadssted) => (
                <Checkbox
                  key={kostnadssted.enhetsnummer}
                  value={kostnadssted}
                  size="small"
                  onChange={() =>
                    updateFilter({
                      navEnheter: addOrRemove(filter.navEnheter, kostnadssted),
                    })
                  }
                >
                  {kostnadssted.navn}
                </Checkbox>
              ))}
            </CheckboxGroup>
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
                  });
                }}
              />
            </Accordion.Content>
          </Accordion.Item>
        )}
      </Accordion>
    </>
  );
}
