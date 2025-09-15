import { BodyShort, CopyButton, List, Tabs } from "@navikt/ds-react";
import { ArrangorDto } from "@tiltaksadministrasjon/api-client";
import { ArrangorKontaktpersonOversikt } from "./ArrangorKontaktpersonerOversikt";

interface Props {
  arrangor: ArrangorDto;
}

export function ArrangorKort({ arrangor }: Props) {
  return (
    <div>
      <BodyShort className="flex items-baseline">
        Organisasjonsnummer: {arrangor.organisasjonsnummer}
        <CopyButton size="small" copyText={arrangor.organisasjonsnummer} />{" "}
      </BodyShort>
      <Tabs defaultValue="kontaktpersoner" className="mt-8">
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
