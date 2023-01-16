import { Alert, BodyShort, Button, Heading, Link } from "@navikt/ds-react";
import { mulighetsrommetClient } from "../../api/clients";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { useTiltaksgjennomforingerByInnloggetAnsatt } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingerByInnloggetAnsatt";
import { Laster } from "../../components/Laster";
import { formaterDato } from "../../utils/Utils";
import styles from "./DetaljerTiltaksgjennomforingPage.module.scss";
import { Tilbakelenke } from "../../components/navigering/Tilbakelenke";

interface Props {
  fagansvarlig?: boolean;
}

export function TiltaksgjennomforingPage({ fagansvarlig = false }: Props) {
  const {
    data,
    isError,
    isFetching,
    refetch: refetchTiltaksgjennomforinger,
  } = useTiltaksgjennomforingById();
  const { data: favoritter, refetch: refetchAnsattsGjennomforinger } =
    useTiltaksgjennomforingerByInnloggetAnsatt();

  const gjennomforingErFavorisert =
    favoritter?.data.find((it) => it.id === data?.id) !== undefined;

  const onLagreFavoritt = async (id: string) => {
    await mulighetsrommetClient.tiltaksgjennomforinger.lagreTilMinListe({
      requestBody: id,
    });
    refetchTiltaksgjennomforinger();
    refetchAnsattsGjennomforinger();
  };

  const onFjernFavoritt = async (id: string) => {
    await mulighetsrommetClient.tiltaksgjennomforinger.fjernFraMinListe({
      requestBody: id,
    });
    refetchTiltaksgjennomforinger();
    refetchAnsattsGjennomforinger();
  };

  if (isError) {
    return (
      <Alert variant="warning">
        Noe gikk galt ved henting av data om tiltaksgjennomføring.
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
      <BodyShort>
        Tiltaksgjennomføringen har startdato:{" "}
        {formaterDato(tiltaksgjennomforing.startDato)} og sluttdato{" "}
        {formaterDato(tiltaksgjennomforing.sluttDato)}
      </BodyShort>
      <dl>
        <dt>Tiltaksnummer</dt>
        <dd>{tiltaksgjennomforing.tiltaksnummer}</dd>
        <dt>Tiltakstype</dt>
        <dd>{tiltaksgjennomforing.tiltakstype.navn}</dd>
        <dt>Kode for tiltakstype:</dt>
        <dd>{tiltaksgjennomforing.tiltakstype.arenaKode}</dd>
        <dt>Virksomhetsnummer</dt>
        <dd>{tiltaksgjennomforing.virksomhetsnummer}</dd>
        <dt>Startdato</dt>
        <dd>{formaterDato(tiltaksgjennomforing.startDato)} </dd>
        <dt>Sluttdato</dt>
        <dd>{formaterDato(tiltaksgjennomforing.sluttDato)} </dd>
      </dl>

      {!fagansvarlig ? (
        gjennomforingErFavorisert ? (
          <Button
            variant="secondary"
            onClick={() => onFjernFavoritt(tiltaksgjennomforing.id)}
            data-testid="fjern-favoritt"
          >
            Fjern fra min liste
          </Button>
        ) : (
          <Button
            variant="primary"
            onClick={() => onLagreFavoritt(tiltaksgjennomforing.id)}
            data-testid="legg-til-favoritt"
          >
            Legg til i min liste
          </Button>
        )
      ) : (
        <></>
      )}

      {/**
       * TODO Implementere skjema for opprettelse av tiltaksgjennomføring
       */}
      {/* <BodyShort>Her kan du opprette en gjennomføring</BodyShort>
      // <Formik<Values>
      //   initialValues={{
      //     tiltakgjennomforingId: "",
      //     sakId: "",
      //   }}
      //   validationSchema={toFormikValidationSchema(Schema)}
      //   onSubmit={(values, actions) => {
      //     setTimeout(() => {
      //       alert(JSON.stringify(values, null, 2));
      //       actions.setSubmitting(false);
      //     }, 1000);
      //   }}
      // >
      //   {() => (
      //     <Form>
      //       <Tekstfelt
      //         name="tiltakgjennomforingId"
      //         type="text"
      //         label="ID for tiltaksgjennomføring"
      //       />
      //       <Tekstfelt name="sakId" type="text" label="ID for sak" />
      //       <button type="submit">Opprett</button>
      //     </Form>
      //   )}
      // </Formik> */}
    </div>
  );
}
