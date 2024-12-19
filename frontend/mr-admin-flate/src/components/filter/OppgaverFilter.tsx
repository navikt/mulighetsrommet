import { useAtom, WritableAtom } from "jotai/index";
import {
  avtaleFilterAccordionAtom,
  OppgaverFilter as OppgaverFilterProps,
  oppgaverFilterAccordionAtom,
} from "@/api/atoms";
import { Accordion, Radio, RadioGroup } from "@navikt/ds-react";
import { addOrRemove } from "@/utils/Utils";
import { FilterAccordionHeader } from "@mr/frontend-common";
import { Tiltakstypefilter } from "mulighetsrommet-veileder-flate/src/components/filtrering/Tiltakstypefilter";
import styles from "./OppgaverFilter.module.scss";

interface Props {
  filterAtom: WritableAtom<OppgaverFilterProps, [newValue: OppgaverFilterProps], void>;
}

export function OppgaverFilter({ filterAtom }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const [accordionsOpen, setAccordionsOpen] = useAtom(oppgaverFilterAccordionAtom);

  return (
    <div className={styles.container}>
      <Accordion>
        <Accordion.Item open={accordionsOpen.includes("navEnhet")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "navEnhet")]);
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
                {/*...register("opsjonsvalg")*/}
                <Radio value="alle">Alle</Radio>
                <Radio value="avtale">Avtale</Radio>
                <Radio value="gjennomforing">Gjennomføring</Radio>
                <Radio value="tilsagn">Tilsagn</Radio>
                <Radio value="stikkprove">Stikkprøve</Radio>
              </RadioGroup>
              Hello
            </div>
          </Accordion.Content>
        </Accordion.Item>
        <Tiltakstypefilter antallValgteTiltakstyper={5} />
      </Accordion>
    </div>
  );
}
