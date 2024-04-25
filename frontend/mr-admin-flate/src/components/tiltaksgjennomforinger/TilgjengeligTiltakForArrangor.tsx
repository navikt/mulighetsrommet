import { Alert, Heading, Switch, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { useFormContext } from "react-hook-form";
import { formaterDato, formaterDatoSomYYYYMMDD } from "../../utils/Utils";
import { InferredTiltaksgjennomforingSchema } from "../redaksjonelt-innhold/TiltaksgjennomforingSchema";
import { ControlledDateInput } from "../skjema/ControlledDateInput";

interface Props {
  startDato: Date;
  avtaleStartdato: Date;
}

export function TiltakTilgjengeligForArrangor({ startDato, avtaleStartdato }: Props) {
  const [tilgjengeliggjorForArrangor, setTilgjengeliggjorForArrangor] = useState(false);
  const { register, setValue, watch } = useFormContext<InferredTiltaksgjennomforingSchema>();

  const selectedDay = watch("tiltakTilgjengeligForArrangorFraOgMed") || startDato;

  return (
    <Alert variant="info">
      <Heading level="4" size="small">
        Når ser arrangør tiltaket og deltakerlister?
      </Heading>
      <p>
        Tiltaksgjennomføringen vil bli tilgjengelig for veileder den{" "}
        <b>{formaterDato(new Date(selectedDay))}</b>.
      </p>
      <p>
        Ønsker du at arrangør skal ha tilgang til tiltaket før {formaterDato(selectedDay!!)} må du
        legge til en egen dato.
      </p>
      <VStack gap="2">
        <Switch
          checked={tilgjengeliggjorForArrangor}
          size="small"
          onChange={(event) => {
            if (!event.target.checked) {
              setValue("tiltakTilgjengeligForArrangorFraOgMed", formaterDatoSomYYYYMMDD(startDato));
            } else {
              setValue(
                "tiltakTilgjengeligForArrangorFraOgMed",
                formaterDatoSomYYYYMMDD(new Date(selectedDay)),
              );
            }
            setTilgjengeliggjorForArrangor(event.target.checked);
          }}
        >
          Arrangør skal ha tilgang til tiltaket før denne datoen
        </Switch>
        {tilgjengeliggjorForArrangor ? (
          <>
            <ControlledDateInput
              size="small"
              fromDate={avtaleStartdato}
              toDate={startDato}
              label={"Når skal arrangør ha tilgang til tiltaket?"}
              {...register("tiltakTilgjengeligForArrangorFraOgMed")}
              format={"iso-string"}
            />
            {selectedDay && (
              <p>
                Arrangør vil kunne se tiltaket <abbr title="Fra og med">fom.</abbr>{" "}
                <b>{formaterDato(selectedDay!!)}</b>
              </p>
            )}
          </>
        ) : null}
      </VStack>
    </Alert>
  );
}
