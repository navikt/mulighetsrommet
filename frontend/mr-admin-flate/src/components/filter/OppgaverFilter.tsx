import { oppgaverFilterAccordionAtom, OppgaverFilter as OppgaverFilterProps } from "@/api/atoms";
import { OPPGAVER_TYPE_STATUS } from "@/utils/filterUtils";
import { addOrRemove } from "@/utils/Utils";
import { NavRegion, TiltakstypeDto } from "@mr/api-client-v2";
import { FilterAccordionHeader } from "@mr/frontend-common";
import { Accordion, Checkbox, CheckboxGroup } from "@navikt/ds-react";
import { useAtom, WritableAtom } from "jotai/index";

interface Props {
  filterAtom: WritableAtom<OppgaverFilterProps, [newValue: OppgaverFilterProps], void>;
  tiltakstyper: TiltakstypeDto[];
  regioner: NavRegion[];
}

export function OppgaverFilter({ filterAtom, tiltakstyper, regioner }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const [accordionsOpen, setAccordionsOpen] = useAtom(oppgaverFilterAccordionAtom);

  return (
    <div className="bg-white self-start w-80">
      <Accordion>
        <Accordion.Item open={accordionsOpen.includes("type")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "type")]);
            }}
          >
            <FilterAccordionHeader tittel="Oppgave" antallValgteFilter={filter.type.length} />
          </Accordion.Header>
          <Accordion.Content>
            <div style={{ marginLeft: "-2rem" }}>
              <CheckboxGroup
                value={filter.type}
                legend="Velg tilsagn du vil se"
                onChange={(value) => {
                  setFilter({
                    ...filter,
                    type: [...value],
                  });
                }}
                hideLegend
              >
                {OPPGAVER_TYPE_STATUS.map(({ label, value }) => (
                  <Checkbox size="small" key={value} value={value}>
                    {label}
                  </Checkbox>
                ))}
              </CheckboxGroup>
            </div>
          </Accordion.Content>
        </Accordion.Item>

        <Accordion.Item open={accordionsOpen.includes("regioner")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "regioner")]);
            }}
          >
            <FilterAccordionHeader tittel="Region" antallValgteFilter={filter.regioner.length} />
          </Accordion.Header>
          <Accordion.Content>
            <div style={{ marginLeft: "-2rem" }}>
              <CheckboxGroup
                value={filter.regioner}
                legend="Velg regioner"
                onChange={(value) => {
                  setFilter({
                    ...filter,
                    regioner: [...value],
                  });
                }}
                hideLegend
              >
                {regioner.map((region) => {
                  return (
                    <Checkbox size="small" key={region.enhetsnummer} value={region.enhetsnummer}>
                      {region.navn}
                    </Checkbox>
                  );
                })}
              </CheckboxGroup>
            </div>
          </Accordion.Content>
        </Accordion.Item>
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
            <div style={{ marginLeft: "-2rem" }}>
              <CheckboxGroup
                value={filter.tiltakstyper}
                legend="Velg tiltakstype"
                onChange={(value) => {
                  setFilter({
                    ...filter,
                    tiltakstyper: [...value],
                  });
                }}
                hideLegend
              >
                {tiltakstyper.map((t) => {
                  return (
                    <Checkbox size="small" key={t.tiltakskode} value={t.tiltakskode}>
                      {t.navn}
                    </Checkbox>
                  );
                })}
              </CheckboxGroup>
            </div>
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
    </div>
  );
}
