import { useAtom, WritableAtom } from "jotai/index";
import { OppgaverFilter as OppgaverFilterProps, oppgaverFilterAccordionAtom } from "@/api/atoms";
import { Accordion, Checkbox, CheckboxGroup } from "@navikt/ds-react";
import { addOrRemove } from "@/utils/Utils";
import { FilterAccordionHeader } from "@mr/frontend-common";
import styles from "./OppgaverFilter.module.scss";
import { OppgaveType, TiltakstypeDto } from "@mr/api-client";

interface Props {
  filterAtom: WritableAtom<OppgaverFilterProps, [newValue: OppgaverFilterProps], void>;
  tiltakstyper: TiltakstypeDto[];
}

export function OppgaverFilter({ filterAtom, tiltakstyper }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const [accordionsOpen, setAccordionsOpen] = useAtom(oppgaverFilterAccordionAtom);

  return (
    <div className={styles.container}>
      <Accordion>
        <Accordion.Item open={accordionsOpen.includes("oppgaveType")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "oppgaveType")]);
            }}
          >
            <FilterAccordionHeader tittel="Oppgave" antallValgteFilter={2} />
          </Accordion.Header>
          <Accordion.Content>
            <div style={{ marginLeft: "-2rem" }}>
              <CheckboxGroup
                value={filter.type}
                legend="Registrer opsjon"
                onChange={(value) => {
                  setFilter({
                    ...filter,
                    type: [...value],
                  });
                }}
                hideLegend
              >
                <Checkbox value={OppgaveType.TILSAGN_TIL_ANNULLERING}>
                  Tilsagn til annullering
                </Checkbox>
                <Checkbox value={OppgaveType.TILSAGN_TIL_BESLUTNING}>
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
            <FilterAccordionHeader tittel="Tiltakstype" antallValgteFilter={tiltakstyper.length} />
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
                    <Checkbox key={t.tiltakskode} value={t.tiltakskode}>
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
