import { List } from "@navikt/ds-react";
import { Bransje } from "@mr/api-client";
import { useFormContext } from "react-hook-form";
import { bransjeToString } from "../../utils/Utils";
import { InferredAvtaleSchema } from "@/components/redaksjoneltInnhold/AvtaleSchema";
import { ControlledSokeSelect } from "@mr/frontend-common";
import { SertifiseringerSkjema } from "./SertifiseringerSelect";
import { ForerkortSkjema } from "./ForerkortSkjema";
import { InnholdElementerSkjema } from "./InnholdElementerSkjema";

export function AvtaleBransjeSkjema() {
  const { register } = useFormContext<InferredAvtaleSchema>();

  return (
    <>
      <ControlledSokeSelect
        size="small"
        helpText={<BransjeHelpText />}
        label="Bransje"
        {...register("amoKategorisering.bransje")}
        placeholder="Velg bransje"
        options={[
          {
            value: Bransje.INGENIOR_OG_IKT_FAG,
            label: bransjeToString(Bransje.INGENIOR_OG_IKT_FAG),
          },
          {
            value: Bransje.HELSE_PLEIE_OG_OMSORG,
            label: bransjeToString(Bransje.HELSE_PLEIE_OG_OMSORG),
          },
          {
            value: Bransje.BARNE_OG_UNGDOMSARBEID,
            label: bransjeToString(Bransje.BARNE_OG_UNGDOMSARBEID),
          },
          {
            value: Bransje.KONTORARBEID,
            label: bransjeToString(Bransje.KONTORARBEID),
          },
          {
            value: Bransje.BUTIKK_OG_SALGSARBEID,
            label: bransjeToString(Bransje.BUTIKK_OG_SALGSARBEID),
          },
          {
            value: Bransje.BYGG_OG_ANLEGG,
            label: bransjeToString(Bransje.BYGG_OG_ANLEGG),
          },
          {
            value: Bransje.INDUSTRIARBEID,
            label: bransjeToString(Bransje.INDUSTRIARBEID),
          },
          {
            value: Bransje.REISELIV_SERVERING_OG_TRANSPORT,
            label: bransjeToString(Bransje.REISELIV_SERVERING_OG_TRANSPORT),
          },
          {
            value: Bransje.SERVICEYRKER_OG_ANNET_ARBEID,
            label: bransjeToString(Bransje.SERVICEYRKER_OG_ANNET_ARBEID),
          },
          {
            value: Bransje.ANDRE_BRANSJER,
            label: bransjeToString(Bransje.ANDRE_BRANSJER),
          },
        ]}
      />
      <ForerkortSkjema<InferredAvtaleSchema> path="amoKategorisering.forerkort" />
      <SertifiseringerSkjema<InferredAvtaleSchema> path="amoKategorisering.sertifiseringer" />
      <InnholdElementerSkjema<InferredAvtaleSchema> path={"amoKategorisering.innholdElementer"} />
    </>
  );
}

function BransjeHelpText() {
  return (
    <div
      style={{
        maxHeight: "400px",
        overflow: "auto",
      }}
    >
      <List as="ul" size="small" title="Ingeniør- og ikt-fag">
        <List.Item>Andre naturvitenskapelige yrker</List.Item>
        <List.Item>Ikt-yrker</List.Item>
        <List.Item>Ingeniører og teknikere</List.Item>
      </List>
      <List as="ul" size="small" title="Helse, pleie og omsorg">
        <List.Item>Omsorgs- og pleiearbeidere</List.Item>
        <List.Item>Annet helsepersonell</List.Item>
        <List.Item>Mellomledere innen helse, pleie og omsorg</List.Item>
      </List>
      <List as="ul" size="small" title="Barne- og ungdomsarbeid">
        <List.Item>Skoleassistenter</List.Item>
        <List.Item>Barnehage- og skolefritidsassistenter</List.Item>
      </List>
      <List as="ul" size="small" title="Kontorarbeid">
        <List.Item>Lavere saksbehandlere innen offentlig administrasjon</List.Item>
        <List.Item>Sekretærer</List.Item>
        <List.Item>Økonomi- og kontormedarbeidere</List.Item>
        <List.Item>Lager- og transportmedarbeidere</List.Item>
        <List.Item>Resepsjonister og sentralbordoperatører</List.Item>
        <List.Item>Andre funksjonærer</List.Item>
      </List>
      <List as="ul" size="small" title="Butikk- og salgsarbeid">
        <List.Item>Butikkarbeid</List.Item>
        <List.Item>Annet salgsarbeid</List.Item>
      </List>
      <List as="ul" size="small" title="Bygg og anlegg">
        <List.Item>Rørleggere</List.Item>
        <List.Item>Snekkere og tømrere</List.Item>
        <List.Item>Elektrikere</List.Item>
        <List.Item>Andre bygningsarbeidere</List.Item>
        <List.Item>Anleggsarbeidere</List.Item>
        <List.Item>Hjelpearbeidere innen bygg og anlegg</List.Item>
        <List.Item>Mellomledere innen bygg og anlegg</List.Item>
      </List>
      <List as="ul" size="small" title="Industriarbeid">
        <List.Item>Mekanikere</List.Item>
        <List.Item>Prosess- og maskinoperatører</List.Item>
        <List.Item>Næringsmiddelarbeid</List.Item>
        <List.Item>Automatikere og elektriske montører</List.Item>
        <List.Item>Andre håndverkere</List.Item>
        <List.Item>Hjelpearbeid innen industrien</List.Item>
        <List.Item>Mellomledere innen industriarbeid</List.Item>
      </List>
      <List as="ul" size="small" title="Reiseliv, servering og transport">
        <List.Item>Maritime yrker</List.Item>
        <List.Item>Førere av transportmidler</List.Item>
        <List.Item>Reiseledere, guider og reisebyråmedarbeidere</List.Item>
        <List.Item>Konduktører og kabinpersonale</List.Item>
        <List.Item>Kokker</List.Item>
        <List.Item>Hovmestere, servitører og hjelpepersonell</List.Item>
        <List.Item>Mellomledere innen reiseliv og transport</List.Item>
      </List>
      <List as="ul" size="small" title="Serviceyrker og annet arbeid">
        <List.Item>Yrker innen politi, brannvesen, toll og forsvar</List.Item>
        <List.Item>Velvære</List.Item>
        <List.Item>Rengjøring</List.Item>
        <List.Item>Vakthold og vaktmestere</List.Item>
        <List.Item>Annet arbeid</List.Item>
        <List.Item>Yrker innen kunst, sport og kultur</List.Item>
      </List>
    </div>
  );
}
