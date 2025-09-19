import { bransjeToString, kurstypeToString } from "@/utils/Utils";
import { AvtaleDto } from "@tiltaksadministrasjon/api-client";
import { Alert, HGrid, Select } from "@navikt/ds-react";
import { gjennomforingTekster } from "../ledetekster/gjennomforingLedetekster";
import { ForerkortForm } from "./ForerkortForm";
import { InnholdElementerForm } from "./InnholdElementerForm";
import { NorksopplaeringForm } from "./NorskopplaeringForm";
import { SertifiseringerSkjema } from "./SertifiseringerSelect";

interface Props {
  avtale: AvtaleDto;
}

export function GjennomforingAmoKategoriseringForm(props: Props) {
  const { avtale } = props;

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
          <InnholdElementerForm path="amoKategorisering.innholdElementer" />
        </>
      )}
      {avtaleAmo.kurstype == "NORSKOPPLAERING" && (
        <NorksopplaeringForm
          norskprovePath="amoKategorisering.norskprove"
          innholdElementerPath="amoKategorisering.innholdElementer"
        />
      )}
    </HGrid>
  );
}
