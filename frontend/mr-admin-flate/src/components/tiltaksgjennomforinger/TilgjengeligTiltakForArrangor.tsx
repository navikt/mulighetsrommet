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
  gjennomforingStartdato: Date;
  lagretDatoForTilgjengeligForArrangor?: String | null;
}

export function TiltakTilgjengeligForArrangor({
  gjennomforingStartdato,
  lagretDatoForTilgjengeligForArrangor,
}: Props) {
  const [tilgjengeliggjorForArrangor, setTilgjengeliggjorForArrangor] = useState(
    !!lagretDatoForTilgjengeligForArrangor,
  );
  const { register, setValue, watch } = useFormContext<InferredTiltaksgjennomforingSchema>();
  const selectedDay = watch("tilgjengeligForArrangorFraOgMedDato") || gjennomforingStartdato;

  function subtractDate(type: "month" | "days", date: Date, value: number) {
    switch (type) {
      case "days":
        setValue(
          "tilgjengeligForArrangorFraOgMedDato",
          formaterDatoSomYYYYMMDD(subtractDays(date, value)),
        );
        return;
      case "month":
        setValue(
          "tilgjengeligForArrangorFraOgMedDato",
          formaterDatoSomYYYYMMDD(subtractMonths(date, value)),
        );
    }
  }

  return (
    <Alert variant="info">
      <Heading level="4" size="small">
        Når ser arrangør tiltaket?
      </Heading>
      <p>
        Tiltaket blir automatisk tilgjengelig for arrangør i Deltakeroversikten på nav.no den{" "}
        <b>{formaterDato(gjennomforingStartdato)}</b>.
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
            <HStack gap="2" align={"end"}>
              <Button
                variant="primary"
                size="xsmall"
                onClick={() => subtractDate("month", new Date(gjennomforingStartdato), 1)}
                type="button"
              >
                1 måned før
              </Button>
              <Button
                variant="primary"
                size="xsmall"
                onClick={() => subtractDate("days", new Date(gjennomforingStartdato), 14)}
                type="button"
              >
                2 uker før
              </Button>
              <ControlledDateInput
                label="Annen dato"
                size="small"
                fromDate={subtractMonths(gjennomforingStartdato, 1)}
                toDate={gjennomforingStartdato}
                {...register("tilgjengeligForArrangorFraOgMedDato")}
                format="iso-string"
              />
            </HStack>
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
