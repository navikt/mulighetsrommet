import { Alert, Button, HStack, Heading, Switch, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { useFormContext } from "react-hook-form";
import {
  formaterDato,
  formaterDatoSomYYYYMMDD,
  subtractDays,
  subtractMonths,
} from "../../utils/Utils";
import { InferredTiltaksgjennomforingSchema } from "../redaksjonelt-innhold/TiltaksgjennomforingSchema";
import { ControlledDateInput } from "../skjema/ControlledDateInput";

interface Props {
  avtaleStartdato: Date;
  lagretDatoForTilgjengeligForArrangor?: String | null;
}

export function TiltakTilgjengeligForArrangor({
  avtaleStartdato,
  lagretDatoForTilgjengeligForArrangor,
}: Props) {
  const [tilgjengeliggjorForArrangor, setTilgjengeliggjorForArrangor] = useState(
    !!lagretDatoForTilgjengeligForArrangor,
  );
  const { register, setValue, watch } = useFormContext<InferredTiltaksgjennomforingSchema>();
  const selectedDay = watch("tilgjengeligForArrangorFraOgMedDato") || avtaleStartdato;

  function subtractDate(type: "month" | "days", date: Date, value: number) {
    const tilgjengeliggjorPaaDato =
      type === "days" ? subtractDays(date, value) : subtractMonths(date, value);

    setValue(
      "tilgjengeligForArrangorFraOgMedDato",
      formaterDatoSomYYYYMMDD(tilgjengeliggjorPaaDato),
    );
  }

  return (
    <Alert variant="info">
      <Heading level="4" size="small">
        Når ser arrangør tiltaket?
      </Heading>
      <p>
        Tiltaket blir automatisk tilgjengelig for arrangør i Deltakeroversikten på nav.no den{" "}
        <b>{formaterDato(avtaleStartdato)}</b>.
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
            <VStack gap="2">
              <HStack gap="2" align={"center"}>
                <Button
                  variant="primary"
                  size="xsmall"
                  onClick={() => subtractDate("month", new Date(avtaleStartdato), 1)}
                  type="button"
                >
                  1 måned før
                </Button>
                <Button
                  variant="primary"
                  size="xsmall"
                  onClick={() => subtractDate("days", new Date(avtaleStartdato), 14)}
                  type="button"
                >
                  2 uker før
                </Button>
              </HStack>
              <ControlledDateInput
                label="Annen dato"
                size="small"
                fromDate={subtractMonths(avtaleStartdato, 1)}
                toDate={avtaleStartdato}
                {...register("tilgjengeligForArrangorFraOgMedDato")}
                format="iso-string"
              />
            </VStack>
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
