import { oppgaverFilterAccordionAtom, OppgaverFilter as OppgaverFilterProps } from "@/api/atoms";
import { addOrRemove } from "@/utils/Utils";
import { OppgaveType, TiltakstypeDto } from "@mr/api-client-v2";
import { FilterAccordionHeader } from "@mr/frontend-common";
import { Accordion, Checkbox, CheckboxGroup } from "@navikt/ds-react";
import { useAtom, WritableAtom } from "jotai/index";

interface Props {
  filterAtom: WritableAtom<OppgaverFilterProps, [newValue: OppgaverFilterProps], void>;
  tiltakstyper: TiltakstypeDto[];
}

export function OppgaverFilter({ filterAtom, tiltakstyper }: Props) {
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
                <Checkbox size="small" value={OppgaveType.TILSAGN_TIL_ANNULLERING}>
                  Tilsagn til annullering
                </Checkbox>
                <Checkbox size="small" value={OppgaveType.TILSAGN_TIL_BESLUTNING}>
                  Tilsagn til beslutning
                </Checkbox>
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
