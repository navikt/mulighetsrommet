import { Alert } from "@navikt/ds-react";
import { useBrukerdata } from "@/apps/modia/hooks/useBrukerdata";
import { Innsatsgruppe } from "@api-client";
import { ArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";

interface FiltrertFeilInnsatsgruppeVarselProps {
  filter: ArbeidsmarkedstiltakFilter;
}

export function FiltrertFeilInnsatsgruppeVarsel({ filter }: FiltrertFeilInnsatsgruppeVarselProps) {
  const { data: brukerdata } = useBrukerdata();
  const innsatsgruppeBruker = brukerdata.innsatsgruppe;
  const innsatsgruppeFiltrert = filter.innsatsgruppe?.nokkel;
  const innsatsgruppeFiltrertNavn = filter.innsatsgruppe?.tittel;

  const standardInnsatsBruker =
    innsatsgruppeBruker === Innsatsgruppe.GODE_MULIGHETER &&
    innsatsgruppeFiltrert !== Innsatsgruppe.GODE_MULIGHETER;
  const situasjonsbestemtInnsatsBruker =
    innsatsgruppeBruker === Innsatsgruppe.TRENGER_VEILEDNING &&
    (innsatsgruppeFiltrert === Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE ||
      innsatsgruppeFiltrert === Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE);
  const spesieltTilpassetInnsats =
    innsatsgruppeBruker === Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE &&
    innsatsgruppeFiltrert === Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE;

  return (
    <>
      {(standardInnsatsBruker || situasjonsbestemtInnsatsBruker || spesieltTilpassetInnsats) && (
        <Alert variant="warning">
          Oversikten viser nå tiltak for innsatsgruppen
          <strong> {innsatsgruppeFiltrertNavn}</strong>. Tenker du noen av disse er aktuelle bør du
          gjøre en ny behovsvurdering.
        </Alert>
      )}
    </>
  );
}
