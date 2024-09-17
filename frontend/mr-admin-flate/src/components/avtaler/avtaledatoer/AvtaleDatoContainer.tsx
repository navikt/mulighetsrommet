import { MAKS_AAR_FOR_AVTALER, MIN_START_DATO_FOR_AVTALER } from "@/constants";
import { addYear } from "@/utils/Utils";
import { AvtaleDto } from "@mr/api-client";
import { useMemo } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { InferredAvtaleSchema } from "../../redaksjoneltInnhold/AvtaleSchema";
import { FormGroup } from "../../skjema/FormGroup";
import { Opsjonsmodell } from "../opsjoner/opsjonsmodeller";
import { AvtaleVarighet } from "./AvtaleVarighet";

interface Props {
  avtale?: AvtaleDto;
  arenaOpphavOgIngenEierskap: boolean;
  opsjonsmodell?: Opsjonsmodell;
}

export function AvtaleDatoContainer({ avtale, arenaOpphavOgIngenEierskap, opsjonsmodell }: Props) {
  const { watch } = useFormContext<DeepPartial<InferredAvtaleSchema>>();
  const avtaletype = watch("avtaletype");
  const { startDato } = watch("startOgSluttDato") ?? {};
  // Uten useMemo for sluttDatoFraDato så trigges rerendering av children hver gang sluttdato kalkuleres på nytt ved endring av startdato
  const sluttDatoFraDato = useMemo(
    () => (startDato ? new Date(startDato) : MIN_START_DATO_FOR_AVTALER),
    [startDato],
  );
  const sluttDatoTilDato = useMemo(
    () => addYear(startDato ? new Date(startDato) : new Date(), MAKS_AAR_FOR_AVTALER),
    [startDato],
  );

  if (!avtaletype) return null;

  return (
    <FormGroup>
      <AvtaleVarighet
        avtale={avtale}
        avtaletype={avtaletype}
        opsjonsmodell={opsjonsmodell}
        arenaOpphavOgIngenEierskap={arenaOpphavOgIngenEierskap}
        minStartDato={MIN_START_DATO_FOR_AVTALER}
        sluttDatoFraDato={sluttDatoFraDato}
        sluttDatoTilDato={sluttDatoTilDato}
        maksAar={MAKS_AAR_FOR_AVTALER}
      />
    </FormGroup>
  );
}
