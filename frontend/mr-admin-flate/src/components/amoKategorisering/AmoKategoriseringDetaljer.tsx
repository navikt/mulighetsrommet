import {
  AmoKategorisering,
  AmoKategoriseringInnholdElement,
} from "@tiltaksadministrasjon/api-client";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import {
  forerkortKlasseToString,
  innholdElementToString,
  kurstypeToString,
  bransjeToString,
} from "@/utils/Utils";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";

interface Props {
  amoKategorisering: AmoKategorisering;
}

export function AmoKategoriseringDetaljer({ amoKategorisering }: Props) {
  function innholdElementer(elementer: AmoKategoriseringInnholdElement[]) {
    return {
      key: gjennomforingTekster.innholdElementerLabel,
      value: (
        <ul>
          {elementer.map((element) => (
            <li key={element} className="list-disc list-inside">
              {innholdElementToString(element)}
            </li>
          ))}
        </ul>
      ),
    };
  }

  switch (amoKategorisering.kurstype) {
    case "BRANSJE_OG_YRKESRETTET":
      return (
        <Definisjonsliste
          title="Kursdetaljer"
          definitions={[
            {
              key: gjennomforingTekster.kurstypeLabel,
              value: `${kurstypeToString(amoKategorisering.kurstype)} - ${bransjeToString(amoKategorisering.bransje)}`,
            },
            ...(amoKategorisering.forerkort.length > 0
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
            ...(amoKategorisering.sertifiseringer.length > 0
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
            innholdElementer(amoKategorisering.innholdElementer),
          ]}
        />
      );
    case "FORBEREDENDE_OPPLAERING_FOR_VOKSNE":
    case "GRUNNLEGGENDE_FERDIGHETER":
      return (
        <Definisjonsliste
          title="Kursdetaljer"
          definitions={[
            {
              key: gjennomforingTekster.kurstypeLabel,
              value: `${kurstypeToString(amoKategorisering.kurstype)}`,
            },
            innholdElementer(amoKategorisering.innholdElementer),
          ]}
        />
      );
    case "NORSKOPPLAERING":
      return (
        <Definisjonsliste
          title="Kursdetaljer"
          definitions={[
            {
              key: gjennomforingTekster.kurstypeLabel,
              value: `${kurstypeToString(amoKategorisering.kurstype)}`,
            },
            {
              key: gjennomforingTekster.norskproveLabel,
              value: amoKategorisering.norskprove ? "Ja" : "Nei",
            },
            innholdElementer(amoKategorisering.innholdElementer),
          ]}
        />
      );
    case "STUDIESPESIALISERING":
      return (
        <Definisjonsliste
          title="Kursdetaljer"
          definitions={[
            {
              key: gjennomforingTekster.kurstypeLabel,
              value: `${kurstypeToString(amoKategorisering.kurstype)}`,
            },
          ]}
        />
      );
    case undefined:
      throw new Error("Not implemented yet: undefined case");
  }
}
