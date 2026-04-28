import { Tabs } from "@navikt/ds-react";
import { ReactNode } from "react";
import { LinkIcon, PaperplaneIcon } from "@navikt/aksel-icons";
import { RedaksjoneltInnholdTabTittel } from "./RedaksjoneltInnholdTabTittel";

interface Props {
  forHvem: ReactNode;
  detaljerOgInnhold: ReactNode;
  pameldingOgVarighet: ReactNode;
  kontaktinfo: ReactNode;
  lenker: ReactNode;
  delMedBruker: ReactNode;
}

export function RedaksjoneltInnholdTabs({
  forHvem,
  detaljerOgInnhold,
  pameldingOgVarighet,
  kontaktinfo,
  lenker,
  delMedBruker,
}: Props) {
  return (
    <Tabs size="small" defaultValue="for_hvem">
      <Tabs.List>
        <Tabs.Tab
          value="for_hvem"
          label={<RedaksjoneltInnholdTabTittel>For hvem</RedaksjoneltInnholdTabTittel>}
        />
        <Tabs.Tab
          value="detaljer_og_innhold"
          label={<RedaksjoneltInnholdTabTittel>Detaljer og innhold</RedaksjoneltInnholdTabTittel>}
        />
        <Tabs.Tab
          value="pamelding_og_varighet"
          label={<RedaksjoneltInnholdTabTittel>Påmelding og varighet</RedaksjoneltInnholdTabTittel>}
        />
        <Tabs.Tab
          value="kontaktinfo"
          label={<RedaksjoneltInnholdTabTittel>Kontaktinfo</RedaksjoneltInnholdTabTittel>}
        />
        <Tabs.Tab
          value="lenker"
          label={
            <RedaksjoneltInnholdTabTittel>
              <LinkIcon style={{ fontSize: "1.5rem" }} /> Lenker
            </RedaksjoneltInnholdTabTittel>
          }
        />
        <Tabs.Tab
          value="del_med_bruker"
          label={
            <RedaksjoneltInnholdTabTittel>
              <PaperplaneIcon style={{ fontSize: "1.5rem" }} /> Del med bruker
            </RedaksjoneltInnholdTabTittel>
          }
        />
      </Tabs.List>
      <Tabs.Panel value="for_hvem">{forHvem}</Tabs.Panel>
      <Tabs.Panel value="detaljer_og_innhold">{detaljerOgInnhold}</Tabs.Panel>
      <Tabs.Panel value="pamelding_og_varighet">{pameldingOgVarighet}</Tabs.Panel>
      <Tabs.Panel value="kontaktinfo">{kontaktinfo}</Tabs.Panel>
      <Tabs.Panel value="lenker">{lenker}</Tabs.Panel>
      <Tabs.Panel value="del_med_bruker">{delMedBruker}</Tabs.Panel>
    </Tabs>
  );
}
