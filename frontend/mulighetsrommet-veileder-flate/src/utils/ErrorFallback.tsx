import { Feilmelding, forsokPaNyttLink } from '../components/feilmelding/Feilmelding';
import { porten } from 'mulighetsrommet-frontend-common/constants';

export function ErrorFallback({ error }: any) {
  let feilmelding = (
    <>
      Arbeidsmarkedstiltakene kunne ikke hentes på grunn av en feil hos oss. Vennligst {forsokPaNyttLink()} eller
      ta&nbsp;
      <a href={porten}>kontakt</a> i Porten dersom du trenger mer hjelp.
    </>
  );

  if (error.status === 404) {
    feilmelding = (
      <>Beklager, siden kan være slettet eller flyttet, eller det var en feil i lenken som førte deg hit.</>
    );
  }

  if (error.status === 401 || error.status === 403) {
    feilmelding = (
      <>Det oppstod en feil under behandlingen av forespørselen din. Ta kontakt med admin hvis problemene vedvarer</>
    );
  }

  return <Feilmelding ikonvariant="error" header={<>Vi beklager, men noe gikk galt</>} beskrivelse={feilmelding} />;
}
