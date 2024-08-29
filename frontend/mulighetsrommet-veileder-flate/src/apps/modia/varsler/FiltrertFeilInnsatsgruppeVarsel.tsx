import { Alert } from "@navikt/ds-react";
import { useHentBrukerdata } from "@/apps/modia/hooks/useHentBrukerdata";
import { Innsatsgruppe } from "@mr/api-client";
import { ArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";

interface FiltrertFeilInnsatsgruppeVarselProps {
  filter: ArbeidsmarkedstiltakFilter;
}

export function FiltrertFeilInnsatsgruppeVarsel({ filter }: FiltrertFeilInnsatsgruppeVarselProps) {
  const { data: brukerdata } = useHentBrukerdata();
  const innsatsgruppeBruker = brukerdata.innsatsgruppe;
  const innsatsgruppeFiltrert = filter.innsatsgruppe?.nokkel;
  const innsatsgruppeFiltrertNavn = filter.innsatsgruppe?.tittel;

  const standardInnsatsBruker =
    innsatsgruppeBruker === Innsatsgruppe.STANDARD_INNSATS &&
    innsatsgruppeFiltrert !== Innsatsgruppe.STANDARD_INNSATS;
  const situasjonsbestemtInnsatsBruker =
    innsatsgruppeBruker === Innsatsgruppe.SITUASJONSBESTEMT_INNSATS &&
    (innsatsgruppeFiltrert === Innsatsgruppe.SPESIELT_TILPASSET_INNSATS ||
      innsatsgruppeFiltrert === Innsatsgruppe.VARIG_TILPASSET_INNSATS);
  const spesieltTilpassetInnsats =
    innsatsgruppeBruker === Innsatsgruppe.SPESIELT_TILPASSET_INNSATS &&
    innsatsgruppeFiltrert === Innsatsgruppe.VARIG_TILPASSET_INNSATS;

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
