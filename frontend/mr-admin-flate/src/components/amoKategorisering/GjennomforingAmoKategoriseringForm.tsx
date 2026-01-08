import { bransjeToString, kurstypeToString } from "@/utils/Utils";
import { AvtaleDto, Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { Alert, HGrid, Select } from "@navikt/ds-react";
import { gjennomforingTekster } from "../ledetekster/gjennomforingLedetekster";
import { ForerkortForm } from "./ForerkortForm";
import { InnholdElementerForm } from "./InnholdElementerForm";
import { NorksopplaeringForm } from "./NorskopplaeringForm";
import { SertifiseringerSkjema } from "./SertifiseringerSelect";

interface Props {
  avtale: AvtaleDto;
}

export function GjennomforingAmoKategoriseringForm({ avtale }: Props) {
  switch (avtale.tiltakstype.tiltakskode) {
    case Tiltakskode.ARBEIDSFORBEREDENDE_TRENING:
    case Tiltakskode.ARBEIDSRETTET_REHABILITERING:
    case Tiltakskode.AVKLARING:
    case Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK:
    case Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING:
    case Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING:
    case Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING:
    case Tiltakskode.HOYERE_UTDANNING:
    case Tiltakskode.JOBBKLUBB:
    case Tiltakskode.OPPFOLGING:
    case Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET:
    case Tiltakskode.FAG_OG_YRKESOPPLAERING:
    case Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING:
      return null;
    case Tiltakskode.ARBEIDSMARKEDSOPPLAERING:
    case Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING:
    case Tiltakskode.STUDIESPESIALISERING:
    case Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV:
  }

  if (!avtale.amoKategorisering) {
    return <Alert variant="warning">{gjennomforingTekster.amoKategoriseringMangler}</Alert>;
  }

  const avtaleAmo = avtale.amoKategorisering;

  return (
    <HGrid gap="4" columns={1}>
      <Select readOnly size="small" label={gjennomforingTekster.kurstypeLabel}>
        <option>{kurstypeToString(avtaleAmo.kurstype)}</option>
      </Select>
      {avtaleAmo.kurstype === "BRANSJE_OG_YRKESRETTET" && (
        <>
          <Select readOnly size="small" label="Bransje">
            <option>{bransjeToString(avtaleAmo.bransje)}</option>
          </Select>
          {avtaleAmo.forerkort.length > 0 && (
            <ForerkortForm path="amoKategorisering.forerkort" options={avtaleAmo.forerkort} />
          )}
          {avtaleAmo.sertifiseringer.length > 0 && (
            <SertifiseringerSkjema
              path="amoKategorisering.sertifiseringer"
              options={avtaleAmo.sertifiseringer}
            />
          )}
          <InnholdElementerForm
            path="amoKategorisering.innholdElementer"
            tiltakskode={avtale.tiltakstype.tiltakskode}
          />
        </>
      )}
      {avtaleAmo.kurstype === "NORSKOPPLAERING" && (
        <NorksopplaeringForm
          norskprovePath="amoKategorisering.norskprove"
          innholdElementerPath="amoKategorisering.innholdElementer"
          tiltakskode={avtale.tiltakstype.tiltakskode}
        />
      )}
    </HGrid>
  );
}
