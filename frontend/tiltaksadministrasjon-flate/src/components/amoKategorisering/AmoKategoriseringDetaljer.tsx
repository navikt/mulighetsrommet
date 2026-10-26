import {
  Kurstype,
  KurstypeKode,
  OpplaringKategoriseringDetaljer,
} from "@tiltaksadministrasjon/api-client";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";

interface Props {
  opplaring: OpplaringKategoriseringDetaljer;
  erEnkeltplass?: boolean;
}

export function AmoKategoriseringDetaljer({ opplaring, erEnkeltplass }: Props) {
  return (
    <Definisjonsliste
      title="Kursdetaljer"
      definitions={[
        opplaring.kurstype && {
          key: gjennomforingTekster.kurstypeLabel,
          value: kurstypeToVisningsnavn(opplaring.kurstype, opplaring),
        },
        ...(opplaring.forerkort.length > 0
          ? [
              {
                key: gjennomforingTekster.forerkortLabel,
                value: (
                  <ul>
                    {opplaring.forerkort.map((klasse) => (
                      <li key={klasse.id}>{klasse.navn}</li>
                    ))}
                  </ul>
                ),
              },
            ]
          : []),
        ...(opplaring.sertifiseringer.length > 0
          ? [
              {
                key: gjennomforingTekster.sertifiseringerLabel,
                value: (
                  <ul>
                    {opplaring.sertifiseringer.map((s) => (
                      <li key={s.konseptId}>{s.label}</li>
                    ))}
                  </ul>
                ),
              },
            ]
          : []),
        !erEnkeltplass &&
          opplaring.norskprove !== null && {
            key: gjennomforingTekster.norskproveLabel,
            value: opplaring.norskprove ? "Ja" : "Nei",
          },
        !erEnkeltplass && {
          key: gjennomforingTekster.innholdElementerLabel,
          value: (
            <ul>
              {opplaring.innholdElementer.map((element) => (
                <li key={element.id} className="list-disc list-inside">
                  {element.navn}
                </li>
              ))}
            </ul>
          ),
        },
      ].filter((definition) => !!definition)}
    />
  );
}

const kurstypeToVisningsnavn = (kurstype: Kurstype, opplaring: OpplaringKategoriseringDetaljer) => {
  switch (kurstype.kode) {
    case KurstypeKode.BRANSJE_OG_YRKESRETTET:
      return `Bransje: ${opplaring.bransje ? opplaring.bransje.navn : "Ukjent"}`;
    case KurstypeKode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE:
    case KurstypeKode.NORSKOPPLAERING:
    case KurstypeKode.GRUNNLEGGENDE_FERDIGHETER:
    case KurstypeKode.STUDIESPESIALISERING:
      return kurstype.navn;
    default:
      return kurstype.navn;
  }
};
