import { AmoKategorisering } from "mulighetsrommet-api-client";
import { Metadata } from "@/components/detaljside/Metadata";
import { Bolk } from "@/components/detaljside/Bolk";
import { tiltaktekster } from "@/components/ledetekster/tiltaksgjennomforingLedetekster";
import {
  forerkortKlasseToString,
  innholdElementToString,
  kurstypeToString,
  spesifiseringToString,
} from "@/utils/Utils";

interface Props {
  amoKategorisering: AmoKategorisering;
}

export function AmoKategoriseringDetaljer({ amoKategorisering }: Props) {
  return (
    <>
      <Bolk>
        <Metadata
          header={tiltaktekster.kurstypeLabel}
          verdi={
            amoKategorisering.spesifisering
              ? `${kurstypeToString(amoKategorisering.kurstype)} - ${spesifiseringToString(amoKategorisering.spesifisering)}`
              : kurstypeToString(amoKategorisering.kurstype)
          }
        />
      </Bolk>
      <Bolk>
        {amoKategorisering.forerkort && amoKategorisering.forerkort.length > 0 && (
          <Metadata
            header={tiltaktekster.forerkortLabel}
            verdi={
              <ul>
                {amoKategorisering.forerkort.map((klasse) => (
                  <li key={klasse}>{forerkortKlasseToString(klasse)}</li>
                ))}
              </ul>
            }
          />
        )}
        {amoKategorisering.sertifiseringer && amoKategorisering.sertifiseringer.length > 0 && (
          <Metadata
            header={tiltaktekster.sertifiseringerLabel}
            verdi={
              <ul>
                {amoKategorisering.sertifiseringer.map((s) => (
                  <li key={s.konseptId}>{s.label}</li>
                ))}
              </ul>
            }
          />
        )}
        {amoKategorisering.norskprove && (
          <Metadata header={tiltaktekster.norskproveLabel} verdi="Ja" />
        )}
      </Bolk>
      <Bolk>
        {amoKategorisering.innholdElementer && amoKategorisering.innholdElementer.length > 0 && (
          <Metadata
            header={tiltaktekster.innholdElementerLabel}
            verdi={
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
