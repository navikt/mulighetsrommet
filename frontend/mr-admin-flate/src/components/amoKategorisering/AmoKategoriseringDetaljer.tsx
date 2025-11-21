import { AmoKategorisering } from "@tiltaksadministrasjon/api-client";
import { Bolk } from "@/components/detaljside/Bolk";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import {
  forerkortKlasseToString,
  innholdElementToString,
  kurstypeToString,
  bransjeToString,
} from "@/utils/Utils";
import { Metadata } from "@mr/frontend-common/components/datadriven/Metadata";

interface Props {
  amoKategorisering: AmoKategorisering;
}

export function AmoKategoriseringDetaljer({ amoKategorisering }: Props) {
  return (
    <>
      <Bolk>
        <Metadata
          label={gjennomforingTekster.kurstypeLabel}
          value={
            amoKategorisering.kurstype === "BRANSJE_OG_YRKESRETTET"
              ? `${kurstypeToString(amoKategorisering.kurstype)} - ${bransjeToString(amoKategorisering.bransje)}`
              : kurstypeToString(amoKategorisering.kurstype)
          }
        />
      </Bolk>
      <Bolk>
        {amoKategorisering.kurstype === "BRANSJE_OG_YRKESRETTET" && (
          <>
            {amoKategorisering.forerkort.length > 0 && (
              <Metadata
                label={gjennomforingTekster.forerkortLabel}
                value={
                  <ul>
                    {amoKategorisering.forerkort.map((klasse) => (
                      <li key={klasse}>{forerkortKlasseToString(klasse)}</li>
                    ))}
                  </ul>
                }
              />
            )}
            {amoKategorisering.sertifiseringer.length > 0 && (
              <Metadata
                label={gjennomforingTekster.sertifiseringerLabel}
                value={
                  <ul>
                    {amoKategorisering.sertifiseringer.map((s) => (
                      <li key={s.konseptId}>{s.label}</li>
                    ))}
                  </ul>
                }
              />
            )}
          </>
        )}
        {amoKategorisering.kurstype === "NORSKOPPLAERING" && amoKategorisering.norskprove && (
          <Metadata label={gjennomforingTekster.norskproveLabel} value="Ja" />
        )}
      </Bolk>
      <Bolk>
        {(amoKategorisering.kurstype === "NORSKOPPLAERING" ||
          amoKategorisering.kurstype === "GRUNNLEGGENDE_FERDIGHETER" ||
          amoKategorisering.kurstype === "BRANSJE_OG_YRKESRETTET") && (
          <Metadata
            label={gjennomforingTekster.innholdElementerLabel}
            value={
              <ul>
                {amoKategorisering.innholdElementer.map((element) => (
                  <li key={element}>{innholdElementToString(element)}</li>
                ))}
              </ul>
            }
          />
        )}
      </Bolk>
    </>
  );
}
