import { HGrid, Heading } from "@navikt/ds-react";
import { Avtale, Avtaletype } from "mulighetsrommet-api-client";
import { DeepPartial, useFormContext } from "react-hook-form";
import { addYear } from "../../../utils/Utils";
import { avtaletekster } from "../../ledetekster/avtaleLedetekster";
import { InferredAvtaleSchema } from "../../redaksjonelt-innhold/AvtaleSchema";
import { ControlledDateInput } from "../../skjema/ControlledDateInput";
import { FormGroup } from "../../skjema/FormGroup";
import { AvtaleVarighet } from "./AvtaleVarighet";

const MIN_START_DATO = new Date(2000, 0, 1);
const MAKS_AAR = 35;

interface Props {
  avtale?: Avtale;
  arenaOpphavOgIngenEierskap: boolean;
}

export function AvtaleDatoContainer({ avtale, arenaOpphavOgIngenEierskap }: Props) {
  const { register, watch } = useFormContext<DeepPartial<InferredAvtaleSchema>>();
  const avtaletype = watch("avtaletype");
  const { startDato } = watch("startOgSluttDato") ?? {};
  const sluttDatoFraDato = startDato ? new Date(startDato) : MIN_START_DATO;
  const sluttDatoTilDato = addYear(startDato ? new Date(startDato) : new Date(), MAKS_AAR);

  function erForhandsgodkjent(avtaletype: Avtaletype): boolean {
    return [Avtaletype.FORHAANDSGODKJENT].includes(avtaletype);
  }

  if (!avtaletype) return null;

  if (avtaletype && erForhandsgodkjent(avtaletype)) {
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
          avtale={avtale}
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
