import { Alert, HStack, Heading, Switch, VStack } from "@navikt/ds-react";
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
  const selectedDay =
    watch("tilgjengeligForArrangorFraOgMedDato") || subtractDays(gjennomforingStartdato, 14);

  return (
    <Alert variant="info">
      <Heading level="4" size="small">
        Når ser arrangør tiltaket?
      </Heading>
      <p>
        Tiltaket blir automatisk tilgjengelig for arrangør i Deltakeroversikten på nav.no den{" "}
        <b>{formaterDato(selectedDay)}</b>.
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
          Arrangør skal ha tilgang fra en annen dato
        </Switch>
        {tilgjengeliggjorForArrangor ? (
          <>
            <HStack gap="2" align={"end"}>
              <ControlledDateInput
                label="Når skal arrangør ha tilgang?"
                size="small"
                fromDate={subtractMonths(gjennomforingStartdato, 2)}
                toDate={gjennomforingStartdato}
                {...register("tilgjengeligForArrangorFraOgMedDato")}
                format="iso-string"
                invalidDatoEtterPeriode="Du må skrive inn en dato som er før oppstartsdato"
                invalidDatoForTidlig="Du må skrive en dato som er tidligst to måneder før oppstartsdato"
              />
            </HStack>
            {selectedDay && (
              <Alert variant="success" inline style={{ marginTop: "1rem" }}>
                Arrangøren kan se tiltaket og eventuelle deltakere fra{" "}
                <b>{formaterDato(selectedDay!!)}</b>
              </Alert>
            )}
          </>
        ) : null}
      </VStack>
    </Alert>
  );
}
