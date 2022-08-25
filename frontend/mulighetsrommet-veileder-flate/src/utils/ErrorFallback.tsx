import { BodyShort, Ingress } from '@navikt/ds-react';
import { Feilmelding } from '../components/feilmelding/Feilmelding';

export function ErrorFallback({ error }: any) {
  let feilmelding = (
    <BodyShort>
      Vi er ikke helt sikre på hva som gikk galt. Du kan gå tilbake, eller{' '}
      <a href="https://jira.adeo.no/plugins/servlet/desk/portal/541/create/4442">ta kontakt i Porten</a> hvis du trenger
      hjelp.
    </BodyShort>
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

  return (
    <Feilmelding ikonvariant="error">
      <>
        <Ingress>
          Noe gikk galt - Statuskode: {error.status} {error.statusText}
        </Ingress>
        {feilmelding}
        <a href="/">Tilbake til forsiden</a>
      </>
    </Feilmelding>
  );
}
