import { AvtaleFormValues, toUtdanningslopDbo } from "@/pages/avtaler/form/validation";
import {
  AvtaleArrangorKontaktperson,
  AvtaleDto,
  NavAnsattDto,
} from "@tiltaksadministrasjon/api-client";
import { DeepPartial } from "react-hook-form";
import { splitNavEnheterByType } from "@/api/enhet/helpers";
import { toAmoKategoriseringRequest } from "@/pages/avtaler/form/mappers";

export function defaultAvtaleData(
  ansatt: NavAnsattDto,
  avtale?: Partial<AvtaleDto>,
): DeepPartial<AvtaleFormValues> {
  const navRegioner = avtale?.kontorstruktur?.map((struktur) => struktur.region.enhetsnummer) ?? [];

  const navEnheter = avtale?.kontorstruktur?.flatMap((struktur) => struktur.kontorer);
  const { navKontorEnheter, navAndreEnheter } = splitNavEnheterByType(navEnheter || []);

  return {
    detaljer: {
      administratorer: avtale?.administratorer?.map((admin) => admin.navIdent) || [ansatt.navIdent],
      navn: avtale?.navn,
      avtaletype: avtale?.avtaletype,
      arrangor: {
        hovedenhet: avtale?.arrangor?.organisasjonsnummer ?? "",
        underenheter: !avtale?.arrangor?.underenheter
          ? []
          : avtale.arrangor.underenheter.map((underenhet) => underenhet.organisasjonsnummer),
        kontaktpersoner:
          avtale?.arrangor?.kontaktpersoner.map((p: AvtaleArrangorKontaktperson) => p.id) ?? [],
      },
      startDato: avtale?.startDato,
      sluttDato: avtale?.sluttDato ?? null,
      sakarkivNummer: avtale?.sakarkivNummer ?? null,
      tiltakskode: avtale?.tiltakstype?.tiltakskode,
      amoKategorisering: toAmoKategoriseringRequest(avtale?.amoKategorisering ?? null),
      opsjonsmodell: {
        type: avtale?.opsjonsmodell?.type,
        opsjonMaksVarighet: avtale?.opsjonsmodell?.opsjonMaksVarighet,
        customOpsjonsmodellNavn: avtale?.opsjonsmodell?.customOpsjonsmodellNavn,
      },
      utdanningslop: avtale?.utdanningslop ? toUtdanningslopDbo(avtale.utdanningslop) : undefined,
    },
    veilederinformasjon: {
      navRegioner: navRegioner,
      navKontorer: navKontorEnheter.map((enhet) => enhet.enhetsnummer),
      navAndreEnheter: navAndreEnheter.map((enhet) => enhet.enhetsnummer),
      beskrivelse: avtale?.beskrivelse ?? null,
      faneinnhold: avtale?.faneinnhold ?? null,
    },
    personvern: {
      personvernBekreftet: avtale?.personvernBekreftet,
      personopplysninger: avtale?.personopplysninger?.map((p) => p.type) ?? [],
    },
    prismodeller:
      avtale?.prismodeller?.map((prismodell) => ({
        id: prismodell.id,
        type: prismodell.type,
        valuta: prismodell.valuta,
        satser:
          prismodell.satser?.map((sats) => ({
            gjelderFra: sats.gjelderFra,
            gjelderTil: sats.gjelderTil,
            pris: sats.pris.belop,
          })) ?? null,
        prisbetingelser: prismodell.prisbetingelser || null,
      })) ?? [],
  };
}
