import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterAccordionHeader } from "@mr/frontend-common";
import { Accordion, Checkbox, CheckboxGroup } from "@navikt/ds-react";
import { useAtom } from "jotai/index";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { useNavRegioner } from "@/api/enhet/useNavRegioner";
import { useGetOppgavetyper } from "@/api/oppgaver/useGetOppgavetyper";
import {
  oppgaverFilterAccordionAtom,
  OppgaverFilterType,
} from "@/pages/oppgaveoversikt/oppgaver/filter";

interface Props {
  filter: OppgaverFilterType;
  updateFilter: (values: Partial<OppgaverFilterType>) => void;
}

export function OppgaverFilter({ filter, updateFilter }: Props) {
  const { data: oppgavetyper } = useGetOppgavetyper();
  const { data: tiltakstyper } = useTiltakstyper();
  const { data: regioner } = useNavRegioner();

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
                  updateFilter({
                    type: [...value],
                  });
                }}
                hideLegend
              >
                {oppgavetyper.map(({ navn, type }) => (
                  <Checkbox size="small" key={type} value={type}>
                    {navn}
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
                  updateFilter({
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
                  updateFilter({
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
