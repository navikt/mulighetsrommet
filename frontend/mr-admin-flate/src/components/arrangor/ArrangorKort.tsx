import { BodyShort, CopyButton, List, Tabs } from "@navikt/ds-react";
import { Arrangor } from "mulighetsrommet-api-client";
import { ArrangorKontaktpersonOversikt } from "./ArrangorKontaktpersonerOversikt";
import styles from "./ArrangorKort.module.scss";

interface Props {
  arrangor: Arrangor;
}

export function ArrangorKort({ arrangor }: Props) {
  return (
    <div>
      <BodyShort className={styles.orgnr}>
        Organisasjonsnummer: {arrangor.organisasjonsnummer}
        <CopyButton size="small" copyText={arrangor.organisasjonsnummer} />{" "}
      </BodyShort>
      <Tabs defaultValue="kontaktpersoner" className={styles.tabs}>
        <Tabs.List>
          <Tabs.Tab value="kontaktpersoner" label="Kontaktpersoner" />
          <Tabs.Tab
            value="underenheter"
            label={`Underenheter (${arrangor.underenheter?.length ?? 0})`}
          />
        </Tabs.List>
        <Tabs.Panel value="kontaktpersoner">
          <ArrangorKontaktpersonOversikt arrangor={arrangor} />
        </Tabs.Panel>
        <Tabs.Panel value="underenheter">
          <div>
            <List>
              {arrangor.underenheter?.map((underenhet) => (
                <List.Item key={underenhet.id}>
                  {underenhet.navn} ({underenhet.organisasjonsnummer})
                </List.Item>
              ))}
            </List>
          </div>
        </Tabs.Panel>
      </Tabs>
    </div>
  );
}
