import { HGrid, Heading } from "@navikt/ds-react";
import { FormGroup } from "../../skjema/FormGroup";
import { avtaletekster } from "../../ledetekster/avtaleLedetekster";
import { ControlledDateInput } from "../../skjema/ControlledDateInput";
import { useFormContext, DeepPartial } from "react-hook-form";
import { addYear } from "../../../utils/Utils";
import { InferredAvtaleSchema } from "../../redaksjonelt-innhold/AvtaleSchema";
import { Tiltakskode } from "mulighetsrommet-api-client";
import { AvtaleVarighet } from "./AvtaleVarighet";

const MIN_START_DATO = new Date(2000, 0, 1);
const MAKS_AAR = 35;

interface Props {
  arenaOpphavOgIngenEierskap: boolean;
}

export function AvtaleDatoContainer({ arenaOpphavOgIngenEierskap }: Props) {
  const { register, watch } = useFormContext<DeepPartial<InferredAvtaleSchema>>();
  const tiltakstype = watch("tiltakstype");
  const { startDato } = watch("startOgSluttDato") ?? {};
  const sluttDatoFraDato = startDato ? new Date(startDato) : MIN_START_DATO;
  const sluttDatoTilDato = addYear(startDato ? new Date(startDato) : new Date(), MAKS_AAR);

  function erForhandsgodkjent(tiltakskode: Tiltakskode): boolean {
    return [
      Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
      Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
    ].includes(tiltakskode);
  }

  if (tiltakstype?.tiltakskode && erForhandsgodkjent(tiltakstype.tiltakskode)) {
    return (
      <FormGroup>
        <Heading size="small" as="h3">
          Avtalens varighet
        </Heading>
        <HGrid columns={2}>
          <ControlledDateInput
            size="small"
            label={avtaletekster.startdatoLabel}
            readOnly={arenaOpphavOgIngenEierskap}
            fromDate={MIN_START_DATO}
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
            invalidDatoEtterPeriode={`Avtaleperioden kan ikke vare lenger enn ${MAKS_AAR} år`}
          />
        </HGrid>
      </FormGroup>
    );
  } else {
    return (
      <FormGroup>
        <AvtaleVarighet
          arenaOpphavOgIngenEierskap={arenaOpphavOgIngenEierskap}
          minStartDato={MIN_START_DATO}
          sluttDatoFraDato={sluttDatoFraDato}
          sluttDatoTilDato={sluttDatoTilDato}
          maksAar={MAKS_AAR}
        />
      </FormGroup>
    );
  }
}
