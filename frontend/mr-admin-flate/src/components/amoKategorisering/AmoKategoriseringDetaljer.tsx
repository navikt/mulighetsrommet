import { AmoKategoriseringDto } from "@tiltaksadministrasjon/api-client";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import {
  forerkortKlasseToString,
  innholdElementToString,
  kurstypeToString,
  bransjeToString,
} from "@/utils/Utils";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";

interface Props {
  amoKategorisering: AmoKategoriseringDto;
}

export function AmoKategoriseringDetaljer({ amoKategorisering }: Props) {
  return (
    <Definisjonsliste
      title="Kursdetaljer"
      definitions={[
        amoKategorisering.kurstype && {
          key: gjennomforingTekster.kurstypeLabel,
          value: `${kurstypeToString(amoKategorisering.kurstype)}${amoKategorisering.bransje ? `- ${bransjeToString(amoKategorisering.bransje)}` : ""}`,
        },
        ...(amoKategorisering.forerkort && amoKategorisering.forerkort.length > 0
          ? [
              {
                key: gjennomforingTekster.forerkortLabel,
                value: (
                  <ul>
                    {amoKategorisering.forerkort.map((klasse) => (
                      <li key={klasse}>{forerkortKlasseToString(klasse)}</li>
                    ))}
                  </ul>
                ),
              },
            ]
          : []),
        ...(amoKategorisering.sertifiseringer && amoKategorisering.sertifiseringer.length > 0
          ? [
              {
                key: gjennomforingTekster.sertifiseringerLabel,
                value: (
                  <ul>
                    {amoKategorisering.sertifiseringer.map((s) => (
                      <li key={s.konseptId}>{s.label}</li>
                    ))}
                  </ul>
                ),
              },
            ]
          : []),
        amoKategorisering.norskprove !== null && {
          key: gjennomforingTekster.norskproveLabel,
          value: amoKategorisering.norskprove ? "Ja" : "Nei",
        },
        amoKategorisering.innholdElementer && {
          key: gjennomforingTekster.innholdElementerLabel,
          value: (
            <ul>
              {amoKategorisering.innholdElementer.map((element) => (
                <li key={element} className="list-disc list-inside">
                  {innholdElementToString(element)}
                </li>
              ))}
            </ul>
          ),
        },
      ].filter((definition) => !!definition)}
    />
  );
}
