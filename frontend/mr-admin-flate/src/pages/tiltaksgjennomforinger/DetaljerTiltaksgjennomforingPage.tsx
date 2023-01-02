import { Alert, Button, Heading, Link } from "@navikt/ds-react";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { useTiltaksgjennomforingerByInnloggetAnsatt } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingerByInnloggetAnsatt";
import { Laster } from "../../components/Laster";
import { Tilbakelenke } from "../../components/navigering/Tilbakelenke";
import { formaterDato } from "../../utils/Utils";
import styles from "./DetaljerTiltaksgjennomforingPage.module.scss";

export function TiltaksgjennomforingPage() {
  const { data, isError, isFetching } = useTiltaksgjennomforingById();
  const { data: favoritter } = useTiltaksgjennomforingerByInnloggetAnsatt();

  const gjennomforingErFavorisert =
    favoritter?.data.find((it) => it.id === data?.id) !== undefined;

  if (isError) {
    return (
      <Alert variant="warning">
        <div>Noe gikk galt ved henting av data om tiltaksgjennomføring</div>
        <Link href="/">Til forside</Link>
      </Alert>
    );
  }

  if (isFetching) {
    return <Laster tekst="Laster data om tiltaksgjennomføring" />;
  }

  if (!data) {
    return (
      <Alert variant="warning">Klarte ikke finne tiltaksgjennomføring</Alert>
    );
  }

  const tiltaksgjennomforing = data;
  return (
    <div className={styles.container}>
      <Tilbakelenke>Tilbake</Tilbakelenke>

      <Heading size="large" level="1">
        {tiltaksgjennomforing.tiltaksnummer} - {tiltaksgjennomforing.navn}
      </Heading>
      <p>
        Tiltaksgjennomføringen har startdato:{" "}
        {formaterDato(tiltaksgjennomforing.fraDato)} og sluttdato{" "}
        {formaterDato(tiltaksgjennomforing.tilDato)}
      </p>
      <dl>
        <dt>Tiltaksnummer</dt>
        <dd>{tiltaksgjennomforing.tiltaksnummer}</dd>
        <dt>Tiltakstype</dt>
        <dd>{tiltaksgjennomforing.tiltakstype.navn}</dd>
        <dt>Kode for tiltakstype:</dt>
        <dd>{tiltaksgjennomforing.tiltakstype.kode}</dd>
        <dt>Virksomhetsnummer</dt>
        <dd>{tiltaksgjennomforing.virksomhetsnummer}</dd>
        <dt>Startdato</dt>
        <dd>{formaterDato(tiltaksgjennomforing.fraDato)} </dd>
        <dt>Sluttdato</dt>
        <dd>{formaterDato(tiltaksgjennomforing.tilDato)} </dd>
      </dl>

      {/** TODO Koble opp knapper */}
      {gjennomforingErFavorisert ? (
        <Button
          variant="secondary"
          onClick={() => {
            alert(
              "Fjerning av tiltaksgjennomføring fra min liste er ikke implementert enda"
            );
          }}
        >
          Fjern fra favoritter
        </Button>
      ) : (
        <Button
          variant="primary"
          onClick={() => {
            alert("Legge til i min liste er ikke implementert enda");
          }}
        >
          Legg til i min liste
        </Button>
      )}

      {/**
       * TODO Implementere skjema for opprettelse av tiltaksgjennomføring
       */}
      {/* <p>Her kan du opprette en gjennomføring</p>
      <Formik<Values>
        initialValues={{
          tiltakgjennomforingId: "",
          sakId: "",
        }}
        validationSchema={toFormikValidationSchema(Schema)}
        onSubmit={(values, actions) => {
          setTimeout(() => {
            alert(JSON.stringify(values, null, 2));
            actions.setSubmitting(false);
          }, 1000);
        }}
      >
        {() => (
          <Form>
            <Tekstfelt
              name="tiltakgjennomforingId"
              type="text"
              label="ID for tiltaksgjennomføring"
            />
            <Tekstfelt name="sakId" type="text" label="ID for sak" />
            <button type="submit">Opprett</button>
          </Form>
        )}
      </Formik> */}
    </div>
  );
}
