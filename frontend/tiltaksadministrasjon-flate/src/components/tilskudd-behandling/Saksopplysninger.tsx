import {
  ArrangorReferanse,
  Betalingsinformasjon,
  KostnadsstedDto,
  OpplaeringtilskuddKode,
  Periode,
  TilskuddMottaker,
  Valuta,
} from "@tiltaksadministrasjon/api-client";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import {
  Definisjonsliste,
  Definition,
} from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { formaterNavEnhet } from "@/utils/nav-enhet";
import { opplaeringTilskuddToString, tilskuddMottakerToString } from "@/utils/Utils";
import { formaterValuta } from "@mr/frontend-common/utils/utils";

interface SaksopplysningerProps {
  journalpostId: string | null;
  soknadsdato: string | null;
  periode: Periode | null;
  kostnadssted: KostnadsstedDto | null;
  tilskuddOpplaeringType: OpplaeringtilskuddKode | null;
  utbetalingMottaker: TilskuddMottaker | null;
  belop: number | null;
  betalingsinformasjon: Betalingsinformasjon | null;
}

export function Saksopplysninger({
  journalpostId,
  soknadsdato,
  periode,
  kostnadssted,
  tilskuddOpplaeringType,
  utbetalingMottaker,
  belop,
  betalingsinformasjon,
}: SaksopplysningerProps) {
  return (
    <Definisjonsliste
      definitions={[
        { key: "Journalpost-ID i Gosys", value: journalpostId },
        {
          key: "Søknadsdato",
          value: soknadsdato ? formaterDato(soknadsdato) : null,
        },
        {
          key: "Tilskuddsperiode",
          value: periode ? formaterPeriode(periode) : null,
        },
        {
          key: "Kostnadssted",
          value: kostnadssted ? formaterNavEnhet(kostnadssted) : null,
        },
        {
          key: "Tilskuddstype",
          value: tilskuddOpplaeringType ? opplaeringTilskuddToString(tilskuddOpplaeringType) : "-",
        },
        {
          key: "Beløp fra faktura",
          value: formaterValuta(belop || 0, Valuta.NOK),
        },
        {
          key: "Hvem skal motta utbetalingen?",
          value: utbetalingMottaker ? tilskuddMottakerToString(utbetalingMottaker) : "-",
          spanColumns: true,
        },
        ...(utbetalingMottaker
          ? tilskuddMottakerOpplysninger(utbetalingMottaker, betalingsinformasjon)
          : []),
      ]}
    />
  );
}

function tilskuddMottakerOpplysninger(
  mottaker: TilskuddMottaker,
  betalingsinformasjon: Betalingsinformasjon | null,
): Definition[] {
  switch (mottaker.type) {
    case "BRUKER":
      return [];
    case "ARRANGOR": {
      const kontoDetaljer =
        betalingsinformasjon?.type === "BBan"
          ? [
              {
                key: "Kontonummer",
                value: betalingsinformasjon.kontonummer,
              },
            ]
          : [];
      return [...kontoDetaljer, referanseOpplysning(mottaker.referanse)];
    }
  }
}

function referanseOpplysning(referanse: ArrangorReferanse) {
  switch (referanse.type) {
    case "FAKTURANUMMER":
      return {
        key: "Fakturanummer",
        value: referanse.fakturanummer,
      };
    case "KID":
      return {
        key: "KID",
        value: referanse.kid,
      };
  }
}
