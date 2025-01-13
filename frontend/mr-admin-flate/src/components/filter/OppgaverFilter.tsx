import { useAtom, WritableAtom } from "jotai/index";
import { OppgaverFilter as OppgaverFilterProps, oppgaverFilterAccordionAtom } from "@/api/atoms";
import { Accordion, Checkbox, CheckboxGroup, Radio, RadioGroup } from "@navikt/ds-react";
import { addOrRemove } from "@/utils/Utils";
import { FilterAccordionHeader } from "@mr/frontend-common";
import styles from "./OppgaverFilter.module.scss";
import { TiltakstypeDto } from "@mr/api-client";

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
            <FilterAccordionHeader tittel="Oppgave" antallValgteFilter={5} />
          </Accordion.Header>
          <Accordion.Content>
            <div style={{ marginLeft: "-2rem" }}>
              <RadioGroup
                value={filter.type}
                legend="Registrer opsjon"
                onChange={(value) => {
                  setFilter({
                    ...filter,
                    type: value,
                  });
                }}
                hideLegend
              >
                <Radio value="alle">Alle</Radio>
                <Radio value="avtale">Avtale</Radio>
                <Radio value="gjennomforing">Gjennomføring</Radio>
                <Radio value="tilsagn">Tilsagn</Radio>
                <Radio value="stikkprove">Stikkprøve</Radio>
              </RadioGroup>
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
                      {t.navn} {t.tiltakskode}
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
