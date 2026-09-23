import {
  KostnadsstedDto,
  OpplaeringtilskuddKode,
  Periode,
  TilskuddMottaker,
  Valuta,
} from "@tiltaksadministrasjon/api-client";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
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
}

export function Saksopplysninger({
  journalpostId,
  soknadsdato,
  periode,
  kostnadssted,
  tilskuddOpplaeringType,
  utbetalingMottaker,
  belop,
}: SaksopplysningerProps) {
  return (
    <Definisjonsliste
      title="Saksopplysninger"
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
          key: "Beløp fra søknad",
          value: formaterValuta(belop || 0, Valuta.NOK),
        },
        {
          key: "Hvem skal motta utbetalingen?",
          value: utbetalingMottaker ? tilskuddMottakerToString(utbetalingMottaker) : "-",
        },
      ]}
    />
  );
}
