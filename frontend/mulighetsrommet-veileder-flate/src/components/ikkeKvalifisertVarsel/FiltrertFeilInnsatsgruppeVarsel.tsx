import { Alert } from "@navikt/ds-react";
import { useHentBrukerdata } from "../../core/api/queries/useHentBrukerdata";
import appStyles from "../../App.module.scss";
import styles from "./BrukerKvalifisererIkkeVarsel.module.scss";
import { Innsatsgruppe } from "mulighetsrommet-api-client";
import { ArbeidsmarkedstiltakFilter } from "../../hooks/useArbeidsmarkedstiltakFilter";

interface FiltrertFeilInnsatsgruppeVarselProps {
  filter: ArbeidsmarkedstiltakFilter;
}

export function FiltrertFeilInnsatsgruppeVarsel({ filter }: FiltrertFeilInnsatsgruppeVarselProps) {
  const brukerdata = useHentBrukerdata();
  const innsatsgruppeBruker = brukerdata.data?.innsatsgruppe;
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
        <Alert variant="warning" className={styles.varsel}>
          Oversikten viser nå tiltak for innsatsgruppen
          <strong className={appStyles.lowercase}> {innsatsgruppeFiltrertNavn}</strong>. Tenker du
          noen av disse er aktuelle bør du gjøre en ny behovsvurdering.
        </Alert>
      )}
    </>
  );
}
