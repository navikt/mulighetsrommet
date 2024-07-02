import { HGrid } from "@navikt/ds-react";
import { Tiltakskode } from "mulighetsrommet-api-client";
import { useFormContext, DeepPartial } from "react-hook-form";
import { addYear } from "../../../utils/Utils";
import { avtaletekster } from "../../ledetekster/avtaleLedetekster";
import { InferredAvtaleSchema } from "../../redaksjonelt-innhold/AvtaleSchema";
import { ControlledDateInput } from "../../skjema/ControlledDateInput";
import { AvtaleVarighet } from "./AvtaleVarighet";
import { FormGroup } from "../../skjema/FormGroup";

interface Props {
  arenaOpphavOgIngenEierskap: boolean;
}

const MIN_STARTDATO = new Date(2000, 0, 1);
const MAKS_SLUTTDATO_AAR = 35;

export function AvtaleDatoContainer({ arenaOpphavOgIngenEierskap }: Props) {
  const { register, watch } = useFormContext<DeepPartial<InferredAvtaleSchema>>();
  const watchedTiltakstype = watch("tiltakstype");
  const tiltakskode = watchedTiltakstype?.tiltakskode;

  const { startDato } = watch("startOgSluttDato") ?? {};
  const sluttDatoFraDato = startDato ? new Date(startDato) : MIN_STARTDATO;
  const sluttDatoTilDato = addYear(
    startDato ? new Date(startDato) : new Date(),
    MAKS_SLUTTDATO_AAR,
  );

  function erForhandsgodkjent(tiltakskode: Tiltakskode): boolean {
    return [
      Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
      Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
    ].includes(tiltakskode);
  }

  if (!tiltakskode) return null;

  return tiltakskode && erForhandsgodkjent(tiltakskode) ? (
    <FormGroup>
      <HGrid columns={2}>
        <ControlledDateInput
          size="small"
          label={avtaletekster.startdatoLabel}
          readOnly={arenaOpphavOgIngenEierskap}
          fromDate={MIN_STARTDATO}
          toDate={sluttDatoTilDato}
          {...register("startOgSluttDato.startDato")}
          format={"iso-string"}
        />
        <ControlledDateInput
          size="small"
          label={avtaletekster.sluttdatoLabel}
          readOnly={arenaOpphavOgIngenEierskap}
          fromDate={sluttDatoFraDato}
          toDate={sluttDatoTilDato}
          {...register("startOgSluttDato.sluttDato")}
          format={"iso-string"}
          invalidDatoEtterPeriode={`Avtaleperioden kan ikke vare lenger enn ${MAKS_SLUTTDATO_AAR} år`}
        />
      </HGrid>
    </FormGroup>
  ) : (
    <FormGroup>
      <AvtaleVarighet
        arenaOpphavOgIngenEierskap={arenaOpphavOgIngenEierskap}
        startDato={startDato}
        sluttDatoFraDato={sluttDatoFraDato}
        sluttDatoTilDato={sluttDatoTilDato}
        minStartDato={MIN_STARTDATO}
        maksSluttDato={MAKS_SLUTTDATO_AAR}
      />
    </FormGroup>
  );
}
