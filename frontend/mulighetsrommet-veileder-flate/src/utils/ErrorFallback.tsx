import { BodyShort, Ingress } from '@navikt/ds-react';
import { Feilmelding, forsokPaNyttLink } from '../components/feilmelding/Feilmelding';

export function ErrorFallback({ error }: any) {
  let feilmelding = (
    <>
      Arbeidsmarkedstiltakene kunne ikke hentes på grunn av en feil hos oss. Vennligst {forsokPaNyttLink()} eller
      ta&nbsp;
      <a href="https://jira.adeo.no/plugins/servlet/desk/portal/541/create/4442">kontakt</a> i Porten dersom du trenger
      mer hjelp.
    </>
  );

  if (error.status === 404) {
    feilmelding = (
      <BodyShort>
        Beklager, siden kan være slettet eller flyttet, eller det var en feil i lenken som førte deg hit.
      </BodyShort>
    );
  }

  if (error.status === 401 || error.status === 403) {
    feilmelding = (
      <BodyShort>
        Det oppstod en feil under behandlingen av forespørselen din. Ta kontakt med admin hvis problemene vedvarer
      </BodyShort>
    );
  }

  return <Feilmelding ikonvariant="error" header={<>Vi beklager, men noe gikk galt</>} beskrivelse={feilmelding} />;
}
