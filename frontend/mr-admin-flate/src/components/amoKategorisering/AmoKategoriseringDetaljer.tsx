import { AmoKategorisering, Kurstype } from "@mr/api-client-v2";
import { Metadata } from "@/components/detaljside/Metadata";
import { Bolk } from "@/components/detaljside/Bolk";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import {
  forerkortKlasseToString,
  innholdElementToString,
  kurstypeToString,
  bransjeToString,
} from "@/utils/Utils";

interface Props {
  amoKategorisering: AmoKategorisering;
}

export function AmoKategoriseringDetaljer({ amoKategorisering }: Props) {
  return (
    <>
      <Bolk>
        <Metadata
          header={gjennomforingTekster.kurstypeLabel}
          value={
            amoKategorisering.kurstype == Kurstype.BRANSJE_OG_YRKESRETTET
              ? `${kurstypeToString(amoKategorisering.kurstype as Kurstype)} - ${bransjeToString(amoKategorisering.bransje)}`
              : kurstypeToString(amoKategorisering.kurstype as Kurstype)
          }
        />
      </Bolk>
      <Bolk>
        {amoKategorisering.kurstype == Kurstype.BRANSJE_OG_YRKESRETTET && (
          <>
            {amoKategorisering.forerkort.length > 0 && (
              <Metadata
                header={gjennomforingTekster.forerkortLabel}
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
                header={gjennomforingTekster.sertifiseringerLabel}
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
        {amoKategorisering.kurstype == Kurstype.NORSKOPPLAERING && amoKategorisering.norskprove && (
          <Metadata header={gjennomforingTekster.norskproveLabel} value="Ja" />
        )}
      </Bolk>
      <Bolk>
        {(amoKategorisering.kurstype == Kurstype.NORSKOPPLAERING ||
          amoKategorisering.kurstype == Kurstype.GRUNNLEGGENDE_FERDIGHETER ||
          amoKategorisering.kurstype == Kurstype.BRANSJE_OG_YRKESRETTET) && (
          <Metadata
            header={gjennomforingTekster.innholdElementerLabel}
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
