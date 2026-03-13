import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterAccordionHeader } from "@mr/frontend-common";
import { Accordion, Checkbox, CheckboxGroup } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { useKontorstruktur } from "@/api/enhet/useKontorstruktur";
import { useGetOppgavetyper } from "@/api/oppgaver/useGetOppgavetyper";
import {
  oppgaverFilterAccordionAtom,
  OppgaverFilterType,
} from "@/pages/oppgaveoversikt/oppgaver/filter";
import { TiltakskodeFilter } from "@/components/filter/TiltakskodeFilter";

interface Props {
  filter: OppgaverFilterType;
  updateFilter: (values: Partial<OppgaverFilterType>) => void;
}

export function OppgaverFilter({ filter, updateFilter }: Props) {
  const { data: oppgavetyper } = useGetOppgavetyper();
  const { data: regioner } = useKontorstruktur();

  const [accordionsOpen, setAccordionsOpen] = useAtom(oppgaverFilterAccordionAtom);

  return (
    <div className="bg-ax-bg-default self-start w-80">
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
              {regioner.map(({ region }) => {
                return (
                  <Checkbox size="small" key={region.enhetsnummer} value={region.enhetsnummer}>
                    {region.navn}
                  </Checkbox>
                );
              })}
            </CheckboxGroup>
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
            <TiltakskodeFilter
              value={filter.tiltakstyper}
              onChange={(tiltakstyper) => {
                updateFilter({ tiltakstyper });
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
    </div>
  );
}
