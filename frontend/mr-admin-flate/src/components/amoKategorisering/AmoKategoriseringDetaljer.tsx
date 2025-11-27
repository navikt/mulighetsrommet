import { AmoKategorisering } from "@tiltaksadministrasjon/api-client";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import {
  forerkortKlasseToString,
  innholdElementToString,
  kurstypeToString,
  bransjeToString,
} from "@/utils/Utils";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";

interface Props {
  amoKategorisering: AmoKategorisering;
}

export function AmoKategoriseringDetaljer({ amoKategorisering }: Props) {
  const innholdElementer =
    amoKategorisering.kurstype === "NORSKOPPLAERING" ||
    amoKategorisering.kurstype === "GRUNNLEGGENDE_FERDIGHETER" ||
    amoKategorisering.kurstype === "BRANSJE_OG_YRKESRETTET"
      ? [
          {
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
        ]
      : [];

  const bransjeOgYrke =
    amoKategorisering.kurstype === "BRANSJE_OG_YRKESRETTET"
      ? [
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
          ...innholdElementer,
        ]
      : [];

  const norskopplaering = amoKategorisering.kurstype === "NORSKOPPLAERING" && [
    {
      key: gjennomforingTekster.kurstypeLabel,
      value: kurstypeToString(amoKategorisering.kurstype),
    },
    {
      key: gjennomforingTekster.norskproveLabel,
      value: amoKategorisering.norskprove ? "Ja" : "Nei",
    },
    ...innholdElementer,
  ];

  const kursMeta = (() => {
    switch (amoKategorisering.kurstype) {
      case "NORSKOPPLAERING":
        return norskopplaering;
      case "BRANSJE_OG_YRKESRETTET":
        return bransjeOgYrke;
      case "GRUNNLEGGENDE_FERDIGHETER":
        return [
          {
            key: gjennomforingTekster.kurstypeLabel,
            value: kurstypeToString(amoKategorisering.kurstype),
          },
          ...innholdElementer,
        ];
      case "STUDIESPESIALISERING":
      case "FORBEREDENDE_OPPLAERING_FOR_VOKSNE":
      case undefined:
        return [
          {
            key: gjennomforingTekster.kurstypeLabel,
            value: kurstypeToString(amoKategorisering.kurstype),
          },
        ];
      default:
        return [];
    }
  })();

  return (
    <>
      <Separator />
      <Definisjonsliste title="Kursdetaljer" definitions={kursMeta || []} />
    </>
  );
}
