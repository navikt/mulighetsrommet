import { Alert, Heading, Switch, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { useFormContext } from "react-hook-form";
import { formaterDato, formaterDatoSomYYYYMMDD } from "../../utils/Utils";
import { InferredTiltaksgjennomforingSchema } from "../redaksjonelt-innhold/TiltaksgjennomforingSchema";
import { ControlledDateInput } from "../skjema/ControlledDateInput";

interface Props {
  startDato: Date;
  avtaleStartdato: Date;
  lagretDatoForTilgjengeligForArrangor?: String | null;
}

export function TiltakTilgjengeligForArrangor({
  startDato,
  avtaleStartdato,
  lagretDatoForTilgjengeligForArrangor,
}: Props) {
  const [tilgjengeliggjorForArrangor, setTilgjengeliggjorForArrangor] = useState(
    !!lagretDatoForTilgjengeligForArrangor,
  );
  const { register, setValue, watch } = useFormContext<InferredTiltaksgjennomforingSchema>();

  const selectedDay = watch("tilgjengeligForArrangorFraOgMedDato") || startDato;

  return (
    <Alert variant="info">
      <Heading level="4" size="small">
        Når ser arrangør tiltaket?
      </Heading>
      <p>
        Tiltaket blir automatisk tilgjengelig for arrangør i Deltakeroversikten på nav.no den{" "}
        <b>{formaterDato(startDato)}</b>.
      </p>
      <p>
        Hvis arrangør har behov for å se opplysninger om deltakere før oppstartdato, kan du endre
        dette.
      </p>
      <VStack gap="2">
        <Switch
          checked={tilgjengeliggjorForArrangor}
          size="small"
          onChange={(event) => {
            if (!event.target.checked) {
              setValue("tilgjengeligForArrangorFraOgMedDato", null);
            } else {
              setValue(
                "tilgjengeligForArrangorFraOgMedDato",
                formaterDatoSomYYYYMMDD(new Date(selectedDay)),
              );
            }
            setTilgjengeliggjorForArrangor(event.target.checked);
          }}
        >
          Ja, arrangør må få tilgang til tiltaket før oppstartsdato
        </Switch>
        {tilgjengeliggjorForArrangor ? (
          <>
            <ControlledDateInput
              size="small"
              fromDate={avtaleStartdato}
              toDate={startDato}
              label="Når skal arrangør ha tilgang til tiltaket?"
              {...register("tilgjengeligForArrangorFraOgMedDato")}
              format="iso-string"
            />
            {selectedDay && (
              <Alert variant="success" inline style={{ marginTop: "1rem" }}>
                Arrangør vil ha tilgang til tiltaket <abbr title="Fra og med">fom.</abbr>{" "}
                <b>{formaterDato(selectedDay!!)}</b> i Deltakeroversikten på nav.no
              </Alert>
            )}
          </>
        ) : null}
      </VStack>
    </Alert>
  );
}
