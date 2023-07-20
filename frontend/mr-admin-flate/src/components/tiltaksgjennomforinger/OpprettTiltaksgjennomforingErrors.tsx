import { PORTEN } from "mulighetsrommet-frontend-common/constants";
import { Link } from "react-router-dom";

export const tekniskFeilError = () => (
  <>
    Gjennomføringen kunne ikke opprettes på grunn av en teknisk feil hos oss.
    Forsøk på nytt eller ta <a href={PORTEN}>kontakt</a> i Porten dersom du
    trenger mer hjelp.
  </>
);

export const avtaleManglerNavRegionError = (avtaleId?: string) => (
  <>
    Avtalen mangler NAV region. Du må oppdatere avtalens NAV region for å kunne
    opprette en gjennomføring.
    {avtaleId ? (
      <>
        <br />
        <br />
        <Link reloadDocument to={`/avtaler/${avtaleId}`}>
          Klikk her for å fikse avtalen
        </Link>
        <br />
        <br />
      </>
    ) : null}
    Ta <a href={PORTEN}>kontakt</a> i Porten dersom du trenger mer hjelp.
  </>
);

export const avtaleFinnesIkke = () => (
  <>
    Det finnes ingen avtale koblet til tiltaksgjennomføringen. Hvis
    gjennomføringen er en AFT- eller VTA-gjennomføring kan du koble
    gjennomføringen til riktig avtale.
    <br />
    <br />
    <Link to={`/avtaler`}>Gå til avtaler her</Link>
    <br />
    <br />
    Ta <a href={PORTEN}>kontakt</a> i Porten dersom du trenger mer hjelp.
  </>
);

export const avtalenErAvsluttet = (erRedigeringsmodus: boolean) => (
  <>
    Kan ikke {erRedigeringsmodus ? "redigere" : "opprette"} gjennomføring fordi
    avtalens sluttdato har passert.
    <br />
    <br />
    Ta <a href={PORTEN}>kontakt</a> i Porten dersom du trenger mer hjelp.
  </>
);
