import { FilterAccordionHeader, FilterSkeleton, NavEnhetFilter } from "@mr/frontend-common";
import { InnsendingFilterAccordionAtom, InnsendingFilterType } from "./filter";
import { useAtom } from "jotai";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { CheckboxList } from "@/components/filter/CheckboxList";
import { tiltakstypeOptions } from "@/utils/filterUtils";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { Accordion, Checkbox } from "@navikt/ds-react";
import { ArrangorKobling } from "@tiltaksadministrasjon/api-client";
import { useKostnadsstedFiltre } from "@/api/enhet/useKostnadsstedFiltre";
import { useNavEnheter } from "@/api/enhet/useNavEnheter";

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
  const { data: enheter } = useNavEnheter();
  const { data: arrangorer } = useArrangorer(ArrangorKobling.TILTAKSGJENNOMFORING, {
    pageSize: 10000,
  });

  if (!arrangorer) {
    return <FilterSkeleton />;
  }
  function selectDeselectAll(checked: boolean, key: string, values: string[]) {
    updateFilter({
      [key]: checked ? values : [],
    });
  }

  const enkleKostnadssteder = regioner
    .filter((region) => region.enheter.length <= 1)
    .map((r) => {
      const enkel = r.enheter.length > 0;
      const enhet = enkel
        ? enheter.find((e) => e.enhetsnummer === r.enheter[0].enhetsnummer)
        : enheter.find((e) => e.enhetsnummer === r.enhetsnummer);
      return enhet;
    });

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
                  navEnheter: enheter.filter((enhet) => navEnheter.includes(enhet.enhetsnummer)),
                });
              }}
              regioner={regioner.filter((region) => region.enheter.length > 1)}
            />
            {enkleKostnadssteder.map((kostnadssted) => (
              <>
                {kostnadssted ? (
                  <Checkbox
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
                ) : null}
              </>
            ))}
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
